import json
import subprocess
import sys
import threading
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

import pytest

CLIENT = Path(__file__).parent / "batch_training_client.py"
SAMPLE = Path(__file__).parent / "requests" / "ncaabb-batch-training.json"
RUN_ID = "c9be1814-79c2-42f3-aa44-0b3de1cd4380"


@pytest.fixture
def server():
    class Handler(BaseHTTPRequestHandler):
        requests: list[tuple[str, object]] = []
        submission_status = 202
        submission_body: object = {"batchTrainingRunId": RUN_ID}
        events: object = []

        def log_message(self, format, *args):
            pass

        def do_POST(self):
            body = json.loads(self.rfile.read(int(self.headers["Content-Length"])))
            self.requests.append((self.path, body))
            self._send(self.submission_status, self.submission_body)

        def do_GET(self):
            self.requests.append((self.path, None))
            self._send(200, self.events)

        def _send(self, status, body):
            content = json.dumps(body).encode()
            self.send_response(status)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(content)))
            self.end_headers()
            self.wfile.write(content)

    httpd = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
    thread = threading.Thread(target=httpd.serve_forever)
    thread.start()
    try:
        yield httpd, Handler
    finally:
        httpd.shutdown()
        httpd.server_close()
        thread.join()


def _run_client(server, *args: str) -> subprocess.CompletedProcess[str]:
    port = server.server_address[1]
    return subprocess.run(
        [
            sys.executable,
            str(CLIENT),
            "--base-url",
            f"http://127.0.0.1:{port}",
            "--sport",
            "ncaabb",
            "--request",
            str(SAMPLE),
            *args,
        ],
        capture_output=True,
        text=True,
        timeout=10,
        check=False,
    )


@pytest.mark.parametrize("wait_option", [(), ("--no-wait",)])
def test_submission_returns_immediately_and_applies_year_overrides(
    server, wait_option
) -> None:
    httpd, handler = server
    result = _run_client(
        httpd,
        "--training-years",
        "2021,2022",
        "--evaluation-years",
        "2023",
        *wait_option,
    )
    assert result.returncode == 0, result.stderr
    assert RUN_ID in result.stdout
    assert len(handler.requests) == 1
    path, body = handler.requests[0]
    assert path == "/ncaabb/networks/batch"
    assert body["yearsToTrainNetwork"] == "2021,2022"
    assert body["yearsToEvaluateNetwork"] == "2023"


def test_submission_error_is_reported(server) -> None:
    httpd, handler = server
    handler.submission_status = 400
    handler.submission_body = {"message": "invalid batch"}
    result = _run_client(httpd)
    assert result.returncode == 1
    assert "HTTP 400" in result.stderr
    assert "invalid batch" in result.stderr


def test_invalid_submission_response_is_rejected(server) -> None:
    httpd, handler = server
    handler.submission_body = {"batchTrainingRunId": "not-a-uuid"}
    result = _run_client(httpd)
    assert result.returncode == 1
    assert "invalid batchTrainingRunId" in result.stderr


def test_monitoring_timeout_is_reported(server) -> None:
    httpd, handler = server
    result = _run_client(httpd, "--wait", "--timeout", "0.2", "--poll-interval", "0.05")
    assert result.returncode == 1
    assert "Timed out waiting for batch" in result.stderr
    assert any(
        path.startswith("/management/searchEvents?") for path, _ in handler.requests
    )


@pytest.mark.parametrize(("failed", "cancelled"), [(1, 0), (0, 1), (1, 1)])
def test_batch_failure_reports_completion_counts(server, failed, cancelled) -> None:
    httpd, handler = server
    handler.events = [
        {
            "eventId": "end",
            "managementEventType": "TRAIN_NETWORK_BATCH_END",
            "additionalContext": {
                "failed": failed,
                "cancelled": cancelled,
                "persisted": 2,
                "completedBelowThreshold": 3,
            },
        }
    ]
    result = _run_client(httpd, "--wait")
    assert result.returncode == 1
    assert RUN_ID in result.stderr
    assert f"failed={failed}, cancelled={cancelled}" in result.stderr
    assert "persisted=2, completedBelowThreshold=3" in result.stderr
    assert "Check server logs" in result.stderr
