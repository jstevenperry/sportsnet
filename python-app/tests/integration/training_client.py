"""Send a saved JSON training request to an already-running Sportsnet server."""

import argparse
import json
import sys
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--base-url",
        default="http://127.0.0.1:8287",
        help="Sportsnet server origin (default: %(default)s)",
    )
    parser.add_argument("--sport", choices=("ncaabb", "mlb"), required=True)
    parser.add_argument(
        "--request",
        type=Path,
        required=True,
        help="Path to a JSON training request body",
    )
    parser.add_argument(
        "--training-years",
        help="Optional comma-separated training-year override, e.g. 2022,2023",
    )
    parser.add_argument(
        "--evaluation-years",
        help="Optional comma-separated evaluation-year override, e.g. 2021",
    )
    parser.add_argument("--timeout", type=float, default=3600.0)
    args = parser.parse_args()

    try:
        body = json.loads(args.request.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        print(f"Cannot read JSON request {args.request}: {error}", file=sys.stderr)
        return 2
    if not isinstance(body, dict):
        print("Request JSON must be an object.", file=sys.stderr)
        return 2
    if args.training_years is not None:
        body["yearsToTrainNetwork"] = args.training_years
    if args.evaluation_years is not None:
        body["yearsToEvaluateNetwork"] = args.evaluation_years

    url = f"{args.base_url.rstrip('/')}/{args.sport}/networks"
    request = Request(
        url,
        data=json.dumps(body).encode("utf-8"),
        headers={"Content-Type": "application/json", "Accept": "application/json"},
        method="POST",
    )
    try:
        with urlopen(request, timeout=args.timeout) as response:
            status = response.status
            content_type = response.headers.get("Content-Type", "")
            response_text = response.read().decode("utf-8")
    except HTTPError as error:
        response_text = error.read().decode("utf-8", errors="replace")
        print(f"HTTP {error.code} {error.reason}\n{response_text}", file=sys.stderr)
        return 1
    except (TimeoutError, URLError) as error:
        print(f"Request to {url} failed: {error}", file=sys.stderr)
        return 1

    print(f"HTTP {status} {content_type}")
    try:
        result = json.loads(response_text)
    except json.JSONDecodeError:
        print(response_text, file=sys.stderr)
        print("Server returned a non-JSON success response.", file=sys.stderr)
        return 1

    print(json.dumps(result, indent=2, sort_keys=True))
    if not isinstance(result, dict) or not isinstance(result.get("networkSaved"), bool):
        print("Unexpected training response; expected a training result object.")
        return 1
    if result["networkSaved"] is False:
        print("Training completed without saving a qualifying model.")
        return 0
    print(f"Saved model ID: {result.get('networkId')}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
