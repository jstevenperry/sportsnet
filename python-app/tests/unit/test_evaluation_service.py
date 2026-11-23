from pathlib import Path
from types import SimpleNamespace

import numpy as np

from sportsnet.adapters.sqlite_store import SQLiteEventStore
from sportsnet.application.evaluation import EvaluationService
from sportsnet.application.management import ManagementService
from sportsnet.domain.datasets import TrainingDataset
from sportsnet.domain.preprocessing import Preprocessor
from sportsnet.domain.schemas import SportSchema
from sportsnet.sports.registry import SportRegistry


class _Data:
    def build(self, sport: str, years: tuple[int, ...]) -> TrainingDataset:
        return TrainingDataset(((1.0,), (2.0,)), (0, 1))


class _Models:
    def load(self, model_id: str, sport: str, schema: SportSchema):
        return SimpleNamespace(
            descriptor={"networkId": model_id, "networkAccuracy": 0.1},
            manifest={"sport": sport},
            model=object(),
            preprocessor=Preprocessor.fit([[1.0], [2.0]], "NORMALIZER_STANDARDIZE"),
        )


class _Backend:
    def predict_scores(self, model: object, features: np.ndarray) -> np.ndarray:
        return np.asarray([[0.9, 0.1], [0.1, 0.9]])


def test_evaluation_uses_saved_preprocessor_and_does_not_mutate_descriptor(
    tmp_path: Path,
) -> None:
    schema = SportSchema(
        sport_id="synthetic",
        display_name="Synthetic",
        feature_schema_version="test-v1",
        feature_names=("x",),
        output_classes=(0, 1),
        season_years=(2020, 2021),
        training_years=(2020, 2021),
    )
    service = EvaluationService(
        registry=SportRegistry((schema,)),
        data=_Data(),
        models=_Models(),
        backend=_Backend(),
        management=ManagementService(SQLiteEventStore(tmp_path / "events.sqlite")),
    )
    result = service.evaluate("synthetic", "saved-id", 2021)
    assert result["networkAccuracy"] == 1.0
    assert result["networkSaved"] is True
    assert result["networkId"] == "saved-id"
