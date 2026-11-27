from pathlib import Path
from types import SimpleNamespace

import numpy as np
import pytest

from sportsnet.adapters.sqlite_store import SQLiteEventStore
from sportsnet.application.management import ManagementService
from sportsnet.application.tournament import TournamentPlugin, TournamentService
from sportsnet.domain.errors import ValidationError
from sportsnet.domain.models import SeasonData
from sportsnet.domain.schemas import SportSchema
from sportsnet.sports.registry import SportRegistry


class _Data:
    def __init__(self) -> None:
        self.validated = False

    def load_tournament_season(self, sport: str, year: int):
        self.validated = True
        return {
            "Alpha": SeasonData("synthetic", year, "Alpha", (0.0,)),
            "Beta, Club": SeasonData("synthetic", year, "Beta, Club", (1.0,)),
        }


class _Models:
    def __init__(self) -> None:
        self.calls = 0

    def load(self, model_id: str, sport: str, schema: SportSchema):
        self.calls += 1
        return SimpleNamespace(
            descriptor={"networkId": model_id},
            manifest={"sport": sport},
            model=object(),
            preprocessor=SimpleNamespace(transform=lambda rows: np.asarray(rows)),
        )


class _Backend:
    def predict_scores(self, model: object, features: np.ndarray) -> np.ndarray:
        return np.asarray([[1, 0, 0], [0, 0, 1]], dtype=float)


def _service(tmp_path: Path):
    schema = SportSchema(
        sport_id="synthetic",
        display_name="Synthetic",
        feature_schema_version="test-v1",
        feature_names=("x",),
        output_classes=(0, 1, 2),
        season_years=(2024,),
        training_years=(2024,),
    )
    data = _Data()
    models = _Models()
    service = TournamentService(
        registry=SportRegistry((schema,)),
        data=data,
        models=models,
        backend=_Backend(),
        management=ManagementService(SQLiteEventStore(tmp_path / "events.sqlite")),
        plugins=(TournamentPlugin("synthetic", 2024, "2024 Ranking", "TEAM_RANKING"),),
        distance_factor=0,
    )
    return service, data, models


def test_tournament_deduplicates_models_and_renders_stable_csv(tmp_path: Path) -> None:
    service, data, models = _service(tmp_path)
    result = service.run(
        "synthetic",
        {"networkIds": ["model-b", "model-a", "model-a"], "tournamentYear": 2024},
    )
    assert data.validated
    assert models.calls == 2
    assert result == (
        'Rank,Team Name,Tournament Score\n1,"Beta, Club",4.0000\n2,Alpha,0.0000\n'
    )


def test_tournament_rejects_missing_plugin_before_model_inference(
    tmp_path: Path,
) -> None:
    service, _, models = _service(tmp_path)
    with pytest.raises(ValidationError, match="plugin"):
        service.run("synthetic", {"networkIds": ["model-a"], "tournamentYear": 2020})
    assert models.calls == 0
