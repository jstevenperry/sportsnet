"""Submit a batch request on an already-running Sportsnet server."""

import argparse
import json
import sys
import time
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
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


def _request_json(request: Request, timeout: float) -> tuple[int, object]:
    with urlopen(request, timeout=timeout) as response:
        return response.status, json.loads(response.read().decode("utf-8"))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--base-url",
        default="http://127.0.0.1:8287",
        help="Sportsnet server origin (default: %(default)s)",
    )
    parser.add_argument("--sport", choices=("ncaabb",), required=True)
    parser.add_argument(
        "--request", type=Path, required=True, help="Path to a JSON batch request body"
    )
    parser.add_argument(
        "--training-years",
        help="Optional comma-separated training-year override, e.g. 2022,2023",
    )
    parser.add_argument(
        "--evaluation-years",
        help="Optional comma-separated evaluation-year override, e.g. 2021",
    )
    parser.add_argument(
        "--timeout",
        type=_positive_float,
        default=3600.0,
        help="Deadline in seconds for submission and optional monitoring "
        "(default: %(default)s)",
    )
    parser.add_argument(
        "--poll-interval",
        type=_positive_float,
        default=2.0,
        help="Seconds between event checks (default: %(default)s)",
    )
    wait_options = parser.add_mutually_exclusive_group()
    wait_options.add_argument(
        "--wait",
        action="store_true",
        help="Monitor management events until the batch finishes",
    )
    wait_options.add_argument(
        "--no-wait",
        dest="wait",
        action="store_false",
        help="Return after acceptance (default)",
    )
    parser.set_defaults(wait=False)
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

    base_url = args.base_url.rstrip("/")
    url = f"{base_url}/{args.sport}/networks/batch"
    request = Request(
        url,
        data=json.dumps(body).encode("utf-8"),
        headers={"Content-Type": "application/json", "Accept": "application/json"},
        method="POST",
    )
    deadline = time.monotonic() + args.timeout
    try:
        status, result = _request_json(request, args.timeout)
        print(f"HTTP {status}")
        print(json.dumps(result, indent=2, sort_keys=True))
        if status != 202 or not isinstance(result, dict):
            print("Unexpected batch submission response.", file=sys.stderr)
            return 1
        run_id = result.get("batchTrainingRunId")
        try:
            if not isinstance(run_id, str):
                raise ValueError("missing batchTrainingRunId")
            UUID(run_id)
        except ValueError:
            print("Batch response has an invalid batchTrainingRunId.", file=sys.stderr)
            return 1
        print(f"Batch training run ID: {run_id}", flush=True)
        if not args.wait:
            return 0

        query = urlencode({"correlationId": run_id})
        events_url = f"{base_url}/management/searchEvents?{query}"
        seen: set[str] = set()
        while True:
            remaining = deadline - time.monotonic()
            if remaining <= 0:
                print(f"Timed out waiting for batch {run_id}.", file=sys.stderr)
                return 1
            _, events = _request_json(
                Request(events_url, headers={"Accept": "application/json"}), remaining
            )
            if not isinstance(events, list) or not all(
                isinstance(event, dict) for event in events
            ):
                print("Unexpected management events response.", file=sys.stderr)
                return 1
            completion = None
            for event in events:
                event_id = event.get("eventId")
                if not isinstance(event_id, str):
                    print("Management event is missing its ID.", file=sys.stderr)
                    return 1
                if event_id not in seen:
                    seen.add(event_id)
                    print(json.dumps(event, sort_keys=True), flush=True)
                if event.get("managementEventType") == "TRAIN_NETWORK_BATCH_END":
                    completion = event
            if completion is not None:
                context = completion.get("additionalContext")
                if not isinstance(context, dict) or any(
                    type(context.get(key)) is not int or context[key] < 0
                    for key in (
                        "persisted",
                        "completedBelowThreshold",
                        "failed",
                        "cancelled",
                    )
                ):
                    print("Invalid batch completion event.", file=sys.stderr)
                    return 1
                if context["failed"] or context["cancelled"]:
                    print(
                        f"Batch {run_id} did not complete successfully: "
                        f"failed={context['failed']}, "
                        f"cancelled={context['cancelled']}, "
                        f"persisted={context['persisted']}, "
                        f"completedBelowThreshold="
                        f"{context['completedBelowThreshold']}. "
                        "Check server logs using the batch run ID for details.",
                        file=sys.stderr,
                    )
                    return 1
                print("Batch training completed.")
                return 0
            time.sleep(min(args.poll_interval, max(0, deadline - time.monotonic())))
    except HTTPError as error:
        response_text = error.read().decode("utf-8", errors="replace")
        print(f"HTTP {error.code} {error.reason}\n{response_text}", file=sys.stderr)
    except (TimeoutError, URLError) as error:
        print(f"Batch request failed: {error}", file=sys.stderr)
    except (UnicodeDecodeError, json.JSONDecodeError) as error:
        print(f"Server returned invalid JSON: {error}", file=sys.stderr)
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
