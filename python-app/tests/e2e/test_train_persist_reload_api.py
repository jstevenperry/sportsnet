from pathlib import Path

from fastapi.testclient import TestClient

from sportsnet.adapters.keras_backend import KerasBackend
from sportsnet.adapters.model_repository import FilesystemModelRepository
from sportsnet.adapters.sqlite_store import SQLiteEventStore
from sportsnet.api.sports import create_sport_router
from sportsnet.application.dependencies import ApplicationDependencies
from sportsnet.application.evaluation import EvaluationService
from sportsnet.application.management import ManagementService
from sportsnet.application.tournament import TournamentPlugin, TournamentService
from sportsnet.application.training import TrainingService
from sportsnet.config import AppSettings
from sportsnet.domain.datasets import TrainingDataset
from sportsnet.domain.models import SeasonData
from sportsnet.domain.schemas import SportSchema
from sportsnet.main import create_app
from sportsnet.sports.registry import SportRegistry


class _SyntheticData:
    def build(self, sport: str, years: tuple[int, ...]) -> TrainingDataset:
        return TrainingDataset(((0.0, 0.0), (1.0, 1.0)), (0, 1))

    def load_tournament_season(self, sport: str, year: int):
        return {
            "Alpha": SeasonData(sport, year, "Alpha", (0.0, 0.0)),
            "Beta": SeasonData(sport, year, "Beta", (1.0, 1.0)),
        }


def test_synthetic_training_persists_and_reloads_after_application_restart(
    tmp_path: Path,
) -> None:
    schema = SportSchema(
        sport_id="synthetic",
        display_name="Synthetic",
        feature_schema_version="synthetic-e2e-v1",
        feature_names=("left", "right"),
        output_classes=(0, 1),
        season_years=(2020, 2021),
        training_years=(2020, 2021),
    )
    registry = SportRegistry((schema,))
    backend = KerasBackend(deterministic=True)
    model_root = tmp_path / "Networks"
    event_path = tmp_path / "events.sqlite"
    models = FilesystemModelRepository(model_root, backend)
    management = ManagementService(SQLiteEventStore(event_path))
    data = _SyntheticData()
    training = TrainingService(
        registry=registry,
        data=data,
        backend=backend,
        models=models,
        management=management,
    )
    evaluation = EvaluationService(
        registry=registry,
        data=data,
        models=models,
        backend=backend,
        management=management,
    )
    tournaments = TournamentService(
        registry=registry,
        data=data,
        models=models,
        backend=backend,
        management=management,
        plugins=(
            TournamentPlugin("synthetic", 2021, "Synthetic ranking", "TEAM_RANKING"),
        ),
    )
    dependencies = ApplicationDependencies(
        registry=registry,
        management=management,
        training=training,
        models=models,
        evaluation=evaluation,
        tournaments=tournaments,
    )
    settings = AppSettings(resource_root=tmp_path, event_database=event_path)
    app = create_app(settings, dependencies)
    app.include_router(create_sport_router("synthetic", dependencies, settings))
    client = TestClient(app)
    with client:
        response = client.post(
            "/synthetic/networks",
            json={
                "yearsToTrainNetwork": [2020],
                "yearsToEvaluateNetwork": [2021],
                "networkStructureAlgorithm": "USER_DEFINED",
                "networkLayout": "2x3x2",
                "numberOfHiddenLayers": 1,
                "maxNumberOfTrainingEpochs": 1,
                "networkEvaluationIndex": 1,
                "maxNetworkError": 1.0,
                "networkPerformanceThreshold": 0,
                "normalizer": "NORMALIZER_STANDARDIZE",
                "seed": 7,
            },
        )
        descriptor = response.json()
        simulation = client.post(
            "/synthetic/simulations",
            json={"networkIds": [descriptor["networkId"]], "tournamentYear": 2021},
        )
    assert response.status_code == 200
    assert descriptor["networkSaved"] is True
    assert simulation.status_code == 200
    assert simulation.text.startswith("Rank,Team Name,Tournament Score\n")
    assert "Alpha" in simulation.text and "Beta" in simulation.text
    model_id = descriptor["networkId"]

    restarted_models = FilesystemModelRepository(model_root, KerasBackend())
    restarted_management = ManagementService(SQLiteEventStore(event_path))
    loaded = restarted_models.load(model_id, "synthetic", schema)
    predictions = KerasBackend().predict_scores(
        loaded.model, loaded.preprocessor.transform([[0.5, 0.5]])
    )
    assert predictions.shape == (1, 2)
    assert restarted_models.get_descriptor(model_id, "synthetic") is not None
    assert len(restarted_management.list_all()) >= 2
    restarted_evaluation = EvaluationService(
        registry=registry,
        data=data,
        models=restarted_models,
        backend=KerasBackend(),
        management=restarted_management,
    )
    result = restarted_evaluation.evaluate("synthetic", model_id, 2021)
    assert result["networkSaved"] is True
