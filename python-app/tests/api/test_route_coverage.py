from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
from threading import Event
from types import SimpleNamespace
from uuid import uuid4

from fastapi.testclient import TestClient

from sportsnet.adapters.sqlite_store import SQLiteEventStore
from sportsnet.application.batch import BatchService
from sportsnet.application.dependencies import ApplicationDependencies
from sportsnet.application.management import ManagementService
from sportsnet.application.training import TrainingOutcome
from sportsnet.config import AppSettings
from sportsnet.domain.metrics import calculate_metrics
from sportsnet.domain.schemas import SportSchema
from sportsnet.main import create_app
from sportsnet.sports.registry import SportRegistry


class _Models:
    def list_descriptors(self, sport: str):
        return ({"networkId": "saved", "networkSaved": True},)

    def get_descriptor(self, model_id: str, sport: str):
        return None


class _Training:
    def train(self, request):
        return SimpleNamespace(status="completed_below_threshold", descriptor=None)


class _Batch:
    def submit(self, request, *, number_of_iterations: int):
        return {
            "batchTrainingRunId": "batch-1",
            "numberOfBatchIterations": number_of_iterations,
        }

    def shutdown(self, timeout_seconds: float = 30.0) -> bool:
        return True


class _Tournaments:
    def years(self, sport: str):
        return ({"tournamentYear": 2024, "outputType": "TEAM_RANKING"},)

    def run(self, sport: str, request):
        return "Rank,Team Name,Tournament Score\n1,Alpha,1.0000\n"


class _Evaluation:
    def evaluate(self, sport: str, model_id: str, evaluation_year: int):
        return {"networkId": model_id, "evaluationYear": evaluation_year}


def _client(tmp_path: Path, training=None) -> TestClient:
    schemas = tuple(
        SportSchema(
            sport_id=sport,
            display_name=sport,
            feature_schema_version=f"{sport}-v1",
            feature_names=("x",),
            output_classes=(0, 1),
            season_years=(2020, 2021, 2024),
            training_years=(2020, 2021),
        )
        for sport in ("NCAABB", "mlb")
    )
    registry = SportRegistry(schemas)
    store = SQLiteEventStore(tmp_path / "events.sqlite")
    management = ManagementService(store)
    dependencies = ApplicationDependencies(
        registry=registry,
        management=management,
        training=_Training(),
        models=_Models(),
        batch=BatchService(training=training, management=management)
        if training is not None
        else _Batch(),
        tournaments=_Tournaments(),
        evaluation=_Evaluation(),
    )
    settings = AppSettings(
        resource_root=tmp_path, event_database=tmp_path / "events.sqlite"
    )
    return TestClient(create_app(settings, dependencies))


def test_sport_routes_cover_years_batch_simulation_and_evaluation(
    tmp_path: Path,
) -> None:
    client = _client(tmp_path)
    network_id = str(uuid4())
    with client:
        assert client.get("/ncaabb/trainingYears").json() == [2020, 2021]
        assert client.get("/mlb/tournamentYears").json()[0]["tournamentYear"] == 2024
        assert client.get(f"/ncaabb/networks/{network_id}").json() is None

        batch = client.post(
            "/ncaabb/networks/batch",
            json={
                "yearsToTrainNetwork": [2020],
                "yearsToEvaluateNetwork": [2021],
                "networkStructureAlgorithm": "HOUR_GLASS",
                "numberOfHiddenLayers": 3,
                "numberOfBatchIterations": 2,
            },
        )
        assert batch.status_code == 202
        assert batch.json()["batchTrainingRunId"] == "batch-1"

        simulation = client.post(
            "/mlb/simulations",
            json={"networkIds": [network_id], "tournamentYear": 2024},
        )
        assert simulation.status_code == 200
        assert simulation.headers["content-type"] == "text/plain; charset=utf-8"
        assert simulation.text.endswith("1,Alpha,1.0000\n")

        evaluation = client.get(f"/ncaabb/evaluate/networks/{network_id}/2021")
        assert evaluation.json() == {
            "networkId": network_id,
            "evaluationYear": 2021,
        }


def test_batch_endpoint_accepts_before_training_finishes(tmp_path: Path) -> None:
    class _BlockingTraining:
        def __init__(self) -> None:
            self.started = Event()
            self.release = Event()

        def train(
            self, request, cancellation=None, *, correlation_id=None, batch=False
        ):
            self.started.set()
            if not self.release.wait(timeout=10):
                raise TimeoutError("test training was not released")
            return TrainingOutcome(
                "completed_below_threshold",
                str(uuid4()),
                None,
                None,
                1,
                "maximum_epochs_reached",
                (1, 2, 2),
                calculate_metrics([0, 1], [[1, 0], [0, 1]], (0, 1)),
                0.25,
                (0, 1),
            )

    training = _BlockingTraining()
    client = _client(tmp_path, training)
    with client, ThreadPoolExecutor(max_workers=1) as executor:
        try:
            response_future = executor.submit(
                client.post,
                "/ncaabb/networks/batch",
                json={
                    "yearsToTrainNetwork": [2020],
                    "yearsToEvaluateNetwork": [2021],
                    "networkStructureAlgorithm": "USER_DEFINED",
                    "networkLayout": "1x2x2",
                    "numberOfBatchIterations": 1,
                },
            )
            assert training.started.wait(timeout=3)
            response = response_future.result(timeout=3)
            assert response.status_code == 202
            assert set(response.json()) == {
                "networkType",
                "numberOfBatchIterations",
                "batchTrainingRunId",
            }
        finally:
            training.release.set()


def test_management_event_create_read_search_and_stats_routes(tmp_path: Path) -> None:
    client = _client(tmp_path)
    with client:
        created = client.post(
            "/management/events",
            json={
                "managementEventType": "PING",
                "sport": "NCAABB",
                "hostName": "test-host",
                "message": "healthy",
                "additionalContext": {"source": "test"},
            },
        )
        assert created.status_code == 201
        event = created.json()
        event_id = event["eventId"]
        assert client.get("/management/events").json()[0]["eventId"] == event_id
        assert client.get(f"/management/events/{event_id}").json() == event
        search = client.get("/management/searchEvents?sport=NCAABB")
        assert [item["eventId"] for item in search.json()] == [event_id]
        stats = client.get("/management/stats?sport=NCAABB")
        assert stats.json()[0]["numberOfEvents"] == 1
