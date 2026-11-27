import json
import subprocess
import sys
import threading
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from uuid import uuid4

import pytest

CLIENT = Path(__file__).parent / "simulation_client.py"
NETWORK_IDS = [str(uuid4()), str(uuid4())]


@pytest.fixture
def server():
    class Handler(BaseHTTPRequestHandler):
        requests: list[tuple[str, object]] = []

        def log_message(self, format, *args):
            pass

        def do_GET(self):
            self.requests.append((self.path, None))
            if self.path.endswith("/tournamentYears"):
                self._send_json(
                    [
                        {
                            "tournamentYear": 2024,
                            "pluginDescription": "2024 Ranking",
                            "outputType": "TEAM_RANKING",
                        }
                    ]
                )
            elif self.path.endswith("/networks"):
                self._send_json(
                    [
                        {"networkId": network_id, "networkSaved": True}
                        for network_id in NETWORK_IDS
                    ]
                )
            else:
                self._send_json({"message": "not found"}, status=404)

        def do_POST(self):
            body = json.loads(self.rfile.read(int(self.headers["Content-Length"])))
            self.requests.append((self.path, body))
            content = b"Rank,Team Name,Tournament Score\n1,Alpha,1.0000\n"
            self.send_response(200)
            self.send_header("Content-Type", "text/plain; charset=utf-8")
            self.send_header("Content-Length", str(len(content)))
            self.end_headers()
            self.wfile.write(content)

        def _send_json(self, body, status=200):
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


def _run_client(httpd, *args: str) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        [
            sys.executable,
            str(CLIENT),
            "--base-url",
            f"http://127.0.0.1:{httpd.server_address[1]}",
            "--sport",
            "ncaabb",
            "--tournament-year",
            "2024",
            *args,
        ],
        capture_output=True,
        text=True,
        timeout=10,
        check=False,
    )


def test_simulation_uses_all_saved_networks(server) -> None:
    httpd, handler = server
    result = _run_client(httpd)

    assert result.returncode == 0, result.stderr
    assert result.stdout == "Rank,Team Name,Tournament Score\n1,Alpha,1.0000\n"
    assert "using 2 trained networks" in result.stderr
    assert [path for path, _ in handler.requests] == [
        "/ncaabb/tournamentYears",
        "/ncaabb/networks",
        "/ncaabb/simulations",
    ]
    assert handler.requests[-1][1] == {
        "networkIds": sorted(NETWORK_IDS),
        "tournamentYear": 2024,
    }


def test_unavailable_tournament_year_is_reported(server) -> None:
    httpd, handler = server
    result = _run_client(httpd, "--tournament-year", "2020")

    assert result.returncode == 1
    assert "Available years: 2024" in result.stderr
    assert [path for path, _ in handler.requests] == ["/ncaabb/tournamentYears"]
