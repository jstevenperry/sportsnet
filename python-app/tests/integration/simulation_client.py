"""Run a tournament simulation with every persisted model for a sport."""

import argparse
import json
import sys
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen
from uuid import UUID


def _positive_float(value: str) -> float:
    try:
        result = float(value)
    except ValueError as error:
        raise argparse.ArgumentTypeError("must be a positive number") from error
    if not 0 < result < float("inf"):
        raise argparse.ArgumentTypeError("must be a positive finite number")
    return result


def _get_json(url: str, timeout: float) -> object:
    request = Request(url, headers={"Accept": "application/json"})
    with urlopen(request, timeout=timeout) as response:
        return json.loads(response.read().decode("utf-8"))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--base-url",
        default="http://127.0.0.1:8287",
        help="Sportsnet server origin (default: %(default)s)",
    )
    parser.add_argument("--sport", choices=("ncaabb", "mlb"), required=True)
    parser.add_argument(
        "--tournament-year",
        type=int,
        required=True,
        help="Tournament year to simulate (see the sport's /tournamentYears endpoint)",
    )
    parser.add_argument(
        "--timeout",
        type=_positive_float,
        default=3600.0,
        help="Request deadline in seconds (default: %(default)s)",
    )
    args = parser.parse_args()

    base_url = args.base_url.rstrip("/")
    sport_url = f"{base_url}/{args.sport}"
    try:
        years = _get_json(f"{sport_url}/tournamentYears", args.timeout)
        if not isinstance(years, list) or not all(
            isinstance(year, dict) and type(year.get("tournamentYear")) is int
            for year in years
        ):
            print("Unexpected tournament years response.", file=sys.stderr)
            return 1
        available_years = [year["tournamentYear"] for year in years]
        if args.tournament_year not in available_years:
            print(
                f"No tournament plugin for {args.tournament_year}. "
                f"Available years: {', '.join(map(str, available_years)) or 'none'}.",
                file=sys.stderr,
            )
            return 1

        descriptors = _get_json(f"{sport_url}/networks", args.timeout)
        if not isinstance(descriptors, list) or not all(
            isinstance(descriptor, dict) for descriptor in descriptors
        ):
            print("Unexpected network descriptors response.", file=sys.stderr)
            return 1
        network_ids: list[str] = []
        for descriptor in descriptors:
            network_id = descriptor.get("networkId")
            try:
                if descriptor.get("networkSaved") is not True or not isinstance(
                    network_id, str
                ):
                    raise ValueError("descriptor is not a persisted network")
                network_id = str(UUID(network_id))
            except ValueError as error:
                print(f"Invalid network descriptor: {error}.", file=sys.stderr)
                return 1
            if network_id not in network_ids:
                network_ids.append(network_id)
        if not network_ids:
            print(f"No trained networks found for {args.sport}.", file=sys.stderr)
            return 1
        network_ids.sort()

        url = f"{sport_url}/simulations"
        request = Request(
            url,
            data=json.dumps(
                {"networkIds": network_ids, "tournamentYear": args.tournament_year}
            ).encode("utf-8"),
            headers={
                "Content-Type": "application/json",
                "Accept": "text/plain",
            },
            method="POST",
        )
        with urlopen(request, timeout=args.timeout) as response:
            result = response.read().decode("utf-8")
            content_type = response.headers.get("Content-Type", "")
        if not content_type.startswith("text/plain") or not result.startswith(
            "Rank,Team Name,Tournament Score\n"
        ):
            print(
                "Unexpected simulation response; expected a ranking CSV.",
                file=sys.stderr,
            )
            return 1
        print(
            f"Simulation completed for {args.sport} {args.tournament_year} "
            f"using {len(network_ids)} trained networks.",
            file=sys.stderr,
        )
        sys.stdout.write(result)
        return 0
    except HTTPError as error:
        response_text = error.read().decode("utf-8", errors="replace")
        print(f"HTTP {error.code} {error.reason}\n{response_text}", file=sys.stderr)
    except (TimeoutError, URLError) as error:
        print(f"Simulation request failed: {error}", file=sys.stderr)
    except (UnicodeDecodeError, json.JSONDecodeError) as error:
        print(f"Server returned invalid JSON or text: {error}", file=sys.stderr)
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
