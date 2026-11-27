import os
import socket
import subprocess
import sys
import time
from pathlib import Path
from urllib.error import URLError
from urllib.request import urlopen

import pytest


@pytest.mark.parametrize("level", [None, "DEBUG"])
def test_live_server_access_log_level(tmp_path: Path, level: str | None) -> None:
    with socket.socket() as listener:
        listener.bind(("127.0.0.1", 0))
        port = listener.getsockname()[1]
    env = os.environ.copy()
    env.pop("SPORTSNET_ACCESS_LOG_LEVEL", None)
    env["SPORTSNET_RESOURCE_ROOT"] = str(tmp_path / "resources")
    env["SPORTSNET_EVENT_DATABASE"] = str(tmp_path / "events.sqlite")
    if level is not None:
        env["SPORTSNET_ACCESS_LOG_LEVEL"] = level
    server = subprocess.Popen(
        [
            sys.executable,
            "-m",
            "uvicorn",
            "sportsnet.main:create_application",
            "--factory",
            "--host",
            "127.0.0.1",
            "--port",
            str(port),
        ],
        cwd=tmp_path,
        env=env,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
    )
    try:
        deadline = time.monotonic() + 30
        while True:
            if server.poll() is not None:
                pytest.fail("Uvicorn exited before becoming ready")
            try:
                with urlopen(f"http://127.0.0.1:{port}/health/live", timeout=1) as r:
                    assert r.status == 200
                break
            except URLError:
                if time.monotonic() >= deadline:
                    pytest.fail("Uvicorn did not become ready")
                time.sleep(0.1)
    finally:
        server.terminate()
        stdout, stderr = server.communicate(timeout=15)
    if level == "DEBUG":
        assert 'GET /health/live HTTP/1.1" 200' in stdout
        assert "DEBUG:" in stdout
    else:
        assert "/health/live" not in stdout + stderr
