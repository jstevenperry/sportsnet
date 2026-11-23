import csv
import json
import socket
import subprocess
import sys
import threading
import time
from pathlib import Path
from uuid import UUID

import pytest
import uvicorn
from fastapi.testclient import TestClient

from sportsnet.main import create_application

REQUEST_PATH = Path(__file__).parent / "requests" / "ncaabb-batch-training.json"
CLIENT_PATH = Path(__file__).parent / "batch_training_client.py"
ENDPOINT = "/ncaabb/networks/batch"


def _write_csv(path: Path, rows: list[list[str]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as stream:
        csv.writer(stream).writerows(rows)


def _create_ncaabb_data(root: Path) -> None:
    data_root = root / "NCAABB" / "data"
    _write_csv(data_root / "SeasonData-Alias.csv", [["source", "alias"]])
    files = (
        ("BasicSeasonData.csv", 38),
        ("OpponentSeasonData.csv", 38),
        ("AdvancedSeasonData.csv", 34),
        ("AdvancedOpponentSeasonData.csv", 34),
    )
    for year in (2022, 2023):
        directory = data_root / str(year)
        _write_csv(
            directory / "tournament_participant_all.csv",
            [["team_name", "number_of_victories"]]
            + [[f"Team {label}", str(label)] for label in range(7)],
        )
        for filename, width in files:
            groups, fields = [""] * width, [""] * width
            rows = []
            for label in range(7):
                row = ["0"] * width
                row[1] = f"Team {label}"
                for column in (8, 11, 14, 17, 20):
                    row[column] = ""
                if filename == "OpponentSeasonData.csv" and label == 0:
                    row[22] = ""
                rows.append(row)
            _write_csv(directory / filename, [groups, fields, *rows])


@pytest.fixture
def sample_request() -> dict:
    request = json.loads(REQUEST_PATH.read_text(encoding="utf-8"))
    request.update(
        yearsToTrainNetwork=[2023],
        yearsToEvaluateNetwork=[2022],
        numberOfBatchIterations=2,
        networkStructureAlgorithm="USER_DEFINED",
        networkLayout="64x8x7",
        maxNumberOfTrainingEpochs=1,
        maxNetworkError=1.0,
        networkPerformanceThreshold=0,
        updater="ADA_MAX",
        activationFunction="RELU",
        outputLayerActivation="SOFTMAX",
        l2RegularizationLambda=0.001,
        learningRuleLearningRate=0.005,
    )
    return request


@pytest.fixture
def client(tmp_path: Path, monkeypatch: pytest.MonkeyPatch):
    root = tmp_path / "resources"
    _create_ncaabb_data(root)
    monkeypatch.setenv("SPORTSNET_RESOURCE_ROOT", str(root))
    monkeypatch.setenv("SPORTSNET_EVENT_DATABASE", str(tmp_path / "events.sqlite"))
    monkeypatch.setenv("SPORTSNET_MAX_BATCH_ITERATIONS", "2")
    with TestClient(create_application()) as test_client:
        yield test_client


def _wait_for_batch(client: TestClient, run_id: str) -> list[dict]:
    deadline = time.monotonic() + 60
    while time.monotonic() < deadline:
        response = client.get(
            "/management/searchEvents", params={"correlationId": run_id}
        )
        assert response.status_code == 200
        events = response.json()
        if any(
            event["managementEventType"] == "TRAIN_NETWORK_BATCH_END"
            for event in events
        ):
            return events
        time.sleep(0.05)
    pytest.fail(f"Batch {run_id} did not finish within 60 seconds")


def test_batch_trains_and_persists_each_ncaabb_network(
    client: TestClient,
    sample_request: dict,
    tmp_path: Path,
    caplog: pytest.LogCaptureFixture,
) -> None:
    response = client.post(ENDPOINT, json=sample_request)

    assert response.status_code == 202
    result = response.json()
    assert result["networkType"] == "NCAABB"
    assert result["numberOfBatchIterations"] == 2
    run_id = result["batchTrainingRunId"]
    UUID(run_id)
    events = _wait_for_batch(client, run_id)
    assert all(event["correlationId"] == run_id for event in events)
    assert all(event["sport"] == "NCAABB" for event in events)
    batch_start = [
        event
        for event in events
        if event["managementEventType"] == "TRAIN_NETWORK_BATCH_START"
    ]
    assert len(batch_start) == 1
    assert batch_start[0]["additionalContext"] == {"iterations": 2}
    iterations = sorted(
        (
            event
            for event in events
            if event["managementEventType"] == "TRAIN_NETWORK_BATCH_ITERATION_END"
        ),
        key=lambda event: event["additionalContext"]["iteration"],
    )
    assert len(iterations) == 2
    assert [event["message"] for event in iterations] == ["persisted", "persisted"]
    assert [event["additionalContext"]["iteration"] for event in iterations] == [1, 2]
    assert [event["additionalContext"]["seed"] for event in iterations] == [18, 19]
    results = [
        record.getMessage()
        for record in caplog.records
        if record.getMessage().startswith("Batch iteration completed")
        and run_id in record.getMessage()
    ]
    assert len(results) == 2
    assert all(
        "status=persisted" in line
        and "layout=64x8x7" in line
        and "iteration_elapsed_ms=" in line
        and "total_training_elapsed_ms=" in line
        and "it was saved" in line
        and "Training confusion matrix:" not in line
        and "Training: error=" not in line
        and "Evaluation confusion matrix:" in line
        for line in results
    )
    end = next(
        event
        for event in events
        if event["managementEventType"] == "TRAIN_NETWORK_BATCH_END"
    )
    context = end["additionalContext"]
    assert {
        key: context[key]
        for key in (
            "persisted",
            "completedBelowThreshold",
            "failed",
            "cancelled",
            "requestedIterations",
            "startedIterations",
            "notStarted",
            "trained",
            "trainingTargetNotMet",
            "evaluationBelowThreshold",
        )
    } == {
        "persisted": 2,
        "completedBelowThreshold": 0,
        "failed": 0,
        "cancelled": 0,
        "requestedIterations": 2,
        "startedIterations": 2,
        "notStarted": 0,
        "trained": 2,
        "trainingTargetNotMet": 0,
        "evaluationBelowThreshold": 0,
    }
    assert context["totalTrainingElapsedMillis"] >= 0
    assert context["trainingYears"] == [2023]
    assert context["evaluationYears"] == [2022]
    assert context["averageIterationElapsedMillis"] >= 0
    assert context["networkStructureAlgorithm"] == "USER_DEFINED"
    assert context["hiddenLayerCounts"] == [1]
    assert context["updater"] == sample_request["updater"]
    assert context["hiddenActivation"] == sample_request["activationFunction"]
    assert context["outputActivation"] == sample_request["outputLayerActivation"]
    assert context["l2"] == sample_request["l2RegularizationLambda"]
    assert context["learningRate"] == sample_request["learningRuleLearningRate"]

    model_ids = {event["additionalContext"]["networkId"] for event in iterations}
    assert len(model_ids) == 2
    listed = client.get("/ncaabb/networks")
    assert listed.status_code == 200
    assert {descriptor["networkId"] for descriptor in listed.json()} == model_ids
    for iteration in iterations:
        context = iteration["additionalContext"]
        model_id = context["networkId"]
        UUID(model_id)
        directory = tmp_path / "resources" / "Networks" / "NCAABB"
        assert (directory / f"{model_id}.model.keras").is_file()
        manifest = json.loads(
            (directory / f"{model_id}.json").read_text(encoding="utf-8")
        )
        assert manifest["modelId"] == model_id
        assert manifest["seed"] == context["seed"]
        assert manifest["correlationId"] == run_id
        assert manifest["trainingYears"] == [2023]
        assert manifest["evaluationYears"] == [2022]


@pytest.mark.parametrize(
    ("iterations", "code"),
    [
        (0, "invalid_request"),
        (-1, "invalid_request"),
        (3, "validation_error"),
        (None, "invalid_request"),
    ],
)
def test_batch_rejects_invalid_iteration_counts(
    client: TestClient, sample_request: dict, iterations: int | None, code: str
) -> None:
    if iterations is None:
        del sample_request["numberOfBatchIterations"]
    else:
        sample_request["numberOfBatchIterations"] = iterations
    response = client.post(ENDPOINT, json=sample_request)
    assert response.status_code == 400
    assert response.json()["code"] == code
    assert client.get("/management/events").json() == []


def test_batch_records_unsaved_networks_below_threshold(
    client: TestClient, sample_request: dict, caplog: pytest.LogCaptureFixture
) -> None:
    sample_request["maxNetworkError"] = 0
    response = client.post(ENDPOINT, json=sample_request)
    assert response.status_code == 202
    events = _wait_for_batch(client, response.json()["batchTrainingRunId"])
    iterations = [
        event
        for event in events
        if event["managementEventType"] == "TRAIN_NETWORK_BATCH_ITERATION_END"
    ]
    assert len(iterations) == 2
    assert all(event["message"] == "completed_below_threshold" for event in iterations)
    results = [
        record.getMessage()
        for record in caplog.records
        if record.getMessage().startswith("Batch iteration completed")
        and response.json()["batchTrainingRunId"] in record.getMessage()
    ]
    assert len(results) == 2
    assert all(
        "status=completed_below_threshold" in line
        and "layout=64x8x7" in line
        and "iteration_elapsed_ms=" in line
        and "total_training_elapsed_ms=" in line
        and "did not reach its training error target" in line
        and "Training confusion matrix:" in line
        and "Training: error=" in line
        and "Evaluation confusion matrix:" not in line
        and "Evaluation: not run." in line
        for line in results
    )
    end = next(
        event
        for event in events
        if event["managementEventType"] == "TRAIN_NETWORK_BATCH_END"
    )
    context = end["additionalContext"]
    assert {
        key: context[key]
        for key in (
            "persisted",
            "completedBelowThreshold",
            "failed",
            "cancelled",
            "requestedIterations",
            "startedIterations",
            "notStarted",
            "trained",
            "trainingTargetNotMet",
            "evaluationBelowThreshold",
        )
    } == {
        "persisted": 0,
        "completedBelowThreshold": 2,
        "failed": 0,
        "cancelled": 0,
        "requestedIterations": 2,
        "startedIterations": 2,
        "notStarted": 0,
        "trained": 0,
        "trainingTargetNotMet": 2,
        "evaluationBelowThreshold": 0,
    }
    assert context["totalTrainingElapsedMillis"] >= 0
    assert client.get("/ncaabb/networks").json() == []


def test_batch_reports_asynchronous_training_failure(
    client: TestClient, sample_request: dict
) -> None:
    sample_request["yearsToEvaluateNetwork"] = sample_request["yearsToTrainNetwork"]
    response = client.post(ENDPOINT, json=sample_request)
    assert response.status_code == 202
    events = _wait_for_batch(client, response.json()["batchTrainingRunId"])
    iterations = [
        event
        for event in events
        if event["managementEventType"] == "TRAIN_NETWORK_BATCH_ITERATION_END"
    ]
    assert len(iterations) == 1
    assert iterations[0]["message"] == "failed"
    assert iterations[0]["additionalContext"]["iteration"] == 1
    end = next(
        event
        for event in events
        if event["managementEventType"] == "TRAIN_NETWORK_BATCH_END"
    )
    context = end["additionalContext"]
    assert {
        key: context[key]
        for key in (
            "persisted",
            "completedBelowThreshold",
            "failed",
            "cancelled",
            "requestedIterations",
            "startedIterations",
            "notStarted",
            "trained",
            "trainingTargetNotMet",
            "evaluationBelowThreshold",
        )
    } == {
        "persisted": 0,
        "completedBelowThreshold": 0,
        "failed": 1,
        "cancelled": 0,
        "requestedIterations": 2,
        "startedIterations": 1,
        "notStarted": 1,
        "trained": 0,
        "trainingTargetNotMet": 0,
        "evaluationBelowThreshold": 0,
    }
    assert context["totalTrainingElapsedMillis"] >= 0
    assert client.get("/ncaabb/networks").json() == []


@pytest.mark.parametrize("failure", [False, True])
def test_batch_client_against_live_server(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch, sample_request: dict, failure: bool
) -> None:
    root = tmp_path / "resources"
    _create_ncaabb_data(root)
    monkeypatch.setenv("SPORTSNET_RESOURCE_ROOT", str(root))
    monkeypatch.setenv("SPORTSNET_EVENT_DATABASE", str(tmp_path / "events.sqlite"))
    if failure:
        sample_request["yearsToEvaluateNetwork"] = sample_request["yearsToTrainNetwork"]
    request_path = tmp_path / "request.json"
    request_path.write_text(json.dumps(sample_request), encoding="utf-8")
    app = create_application()
    server = uvicorn.Server(uvicorn.Config(app, log_level="error"))
    with socket.socket() as listener:
        listener.bind(("127.0.0.1", 0))
        listener.listen()
        port = listener.getsockname()[1]
        thread = threading.Thread(target=server.run, kwargs={"sockets": [listener]})
        thread.start()
        try:
            deadline = time.monotonic() + 10
            while (
                not server.started and thread.is_alive() and time.monotonic() < deadline
            ):
                time.sleep(0.01)
            assert server.started, "Local test server failed to start"
            result = subprocess.run(
                [
                    sys.executable,
                    str(CLIENT_PATH),
                    "--base-url",
                    f"http://127.0.0.1:{port}",
                    "--sport",
                    "ncaabb",
                    "--request",
                    str(request_path),
                    "--wait",
                    "--timeout",
                    "30",
                    "--poll-interval",
                    "0.05",
                ],
                capture_output=True,
                text=True,
                timeout=40,
                check=False,
            )
            assert result.returncode == (1 if failure else 0), result.stderr
            assert "HTTP 202" in result.stdout
            assert "Batch training run ID:" in result.stdout
            assert '"TRAIN_NETWORK_BATCH_ITERATION_END"' in result.stdout
            assert '"TRAIN_NETWORK_BATCH_END"' in result.stdout
            if failure:
                assert "failed=1, cancelled=0" in result.stderr
                assert "Check server logs" in result.stderr
            else:
                assert "Batch training completed." in result.stdout
                models = list((root / "Networks" / "NCAABB").glob("*.model.keras"))
                assert len(models) == 2
        finally:
            server.should_exit = True
            thread.join(timeout=10)
            assert not thread.is_alive(), "Local test server failed to stop"
