import logging
from dataclasses import dataclass, field, replace
from pathlib import Path
from threading import Event

import numpy as np
import pytest

from sportsnet.adapters.sqlite_store import SQLiteEventStore
from sportsnet.application.batch import BatchService
from sportsnet.application.management import ManagementService
from sportsnet.application.training import TrainingRequest, TrainingService
from sportsnet.domain.datasets import TrainingDataset
from sportsnet.domain.errors import ValidationError
from sportsnet.domain.schemas import SportSchema
from sportsnet.sports.registry import SportRegistry


@dataclass
class _FakeBackend:
    perfect: bool
    train_calls: int = 0
    predicted_sizes: list[int] = field(default_factory=list)

    def build(self, spec: object) -> object:
        return object()

    def train_one_epoch(
        self, handle: object, features: np.ndarray, labels: np.ndarray
    ) -> object:
        self.train_calls += 1
        return type("Epoch", (), {"loss": 0.25})()

    def predict_scores(self, handle: object, features: np.ndarray) -> np.ndarray:
        self.predicted_sizes.append(len(features))
        if self.perfect:
            return np.asarray(
                [[0.9, 0.1] if row[0] < 0 else [0.1, 0.9] for row in features]
            )
        return np.tile(np.asarray([[0.9, 0.1]]), (len(features), 1))


class _Data:
    def build(self, sport: str, years: tuple[int, ...]) -> TrainingDataset:
        if years == (2020,):
            return TrainingDataset(((0.0,), (1.0,)), (0, 1))
        return TrainingDataset(((0.0,), (1.0,)), (0, 1))


class _Models:
    def __init__(self) -> None:
        self.saved = 0
        self.last: dict[str, object] = {}

    def persist_model(self, **kwargs: object) -> dict[str, object]:
        self.saved += 1
        self.last = kwargs
        return {"networkId": kwargs["model_id"], "networkSaved": True}


def _service(
    tmp_path: Path, *, perfect: bool
) -> tuple[TrainingService, _Models, _FakeBackend]:
    schema = SportSchema(
        sport_id="synthetic",
        display_name="Synthetic",
        feature_schema_version="synthetic-v1",
        feature_names=("x",),
        output_classes=(0, 1),
        season_years=(2020, 2021),
        training_years=(2020, 2021),
    )
    backend = _FakeBackend(perfect)
    models = _Models()
    management = ManagementService(SQLiteEventStore(tmp_path / "events.sqlite"))
    service = TrainingService(
        registry=SportRegistry((schema,)),
        data=_Data(),
        backend=backend,
        models=models,
        management=management,
    )
    return service, models, backend


def test_training_persists_only_after_early_stop_and_threshold(tmp_path: Path) -> None:
    service, models, backend = _service(tmp_path, perfect=True)
    outcome = service.train(
        TrainingRequest(
            sport_id="synthetic",
            training_years=(2020,),
            evaluation_years=(2021,),
            algorithm="USER_DEFINED",
            user_layout="1x2x2",
            hidden_layer_count=1,
            max_epochs=4,
            evaluation_interval=1,
            max_network_error=0.1,
            performance_threshold=70,
            normalizer="NORMALIZER_STANDARDIZE",
            seed=4,
        )
    )
    assert outcome.status == "persisted"
    assert models.saved == 1
    assert backend.train_calls == 1
    assert outcome.layout == (1, 2, 2)


@pytest.mark.parametrize("perfect", [True, False])
def test_training_outcome_uses_actual_randomized_layout(
    tmp_path: Path, perfect: bool
) -> None:
    service, _, backend = _service(tmp_path, perfect=perfect)
    original_build = backend.build
    layouts = []

    def build(spec):
        layouts.append(spec.layout)
        return original_build(spec)

    backend.build = build
    outcome = service.train(
        _split_request(2, algorithm="USER_DEFINED_RAND", user_layout="1x10x4x2")
    )

    assert outcome.status == ("persisted" if perfect else "completed_below_threshold")
    assert outcome.layout == layouts[0]
    assert outcome.layout[0] == 1
    assert outcome.layout[-1] == 2
    assert len(outcome.layout) == 4


def test_below_threshold_discards_model_and_releases_no_descriptor(
    tmp_path: Path,
) -> None:
    service, models, _ = _service(tmp_path, perfect=False)
    outcome = service.train(
        TrainingRequest(
            sport_id="synthetic",
            training_years=(2020,),
            evaluation_years=(2021,),
            algorithm="USER_DEFINED",
            user_layout="1x2x2",
            hidden_layer_count=1,
            max_epochs=4,
            evaluation_interval=1,
            max_network_error=0.6,
            performance_threshold=100,
            normalizer="NORMALIZER_STANDARDIZE",
            seed=4,
        )
    )
    assert outcome.status == "completed_below_threshold"
    assert outcome.descriptor is None
    assert models.saved == 0
    assert outcome.layout == (1, 2, 2)
    report = outcome.report
    assert report["confusionMatrixCsv"].startswith("Actual\\Predicted,")
    assert report["trainingConfusionMatrixCsv"].startswith("Actual\\Predicted,")
    assert report["networkId"] == outcome.model_id
    assert report["networkSaved"] is False
    assert report["stopReason"] == "max_network_error_reached"
    for key in (
        "trainingError",
        "networkAccuracy",
        "networkPrecision",
        "networkRecall",
        "networkF1Score",
        "realNetworkAccuracy",
        "numberOfEpochs",
        "elapsedTimeMillis",
    ):
        assert key in report


def test_training_rejects_overlapping_years_before_backend(tmp_path: Path) -> None:
    service, _, backend = _service(tmp_path, perfect=True)
    with pytest.raises(ValidationError, match="disjoint"):
        service.train(
            TrainingRequest(
                sport_id="synthetic",
                training_years=(2020,),
                evaluation_years=(2020,),
                algorithm="USER_DEFINED",
                user_layout="1x2x2",
                hidden_layer_count=1,
                max_epochs=1,
                evaluation_interval=1,
                max_network_error=0.1,
                performance_threshold=70,
                normalizer="NORMALIZER_STANDARDIZE",
                seed=4,
            )
        )
    assert backend.train_calls == 0


@dataclass
class _SizeAwareBackend(_FakeBackend):
    """Predicts perfectly only for inputs with `perfect_rows` rows."""

    perfect_rows: int = 2

    def predict_scores(self, handle: object, features: np.ndarray) -> np.ndarray:
        self.perfect = len(features) == self.perfect_rows
        return super().predict_scores(handle, features)


_TRAIN_ROWS = 2
_EVAL_ROWS = 4


class _SplitData:
    def build(self, sport: str, years: tuple[int, ...]) -> TrainingDataset:
        if years == (2020,):
            return TrainingDataset(((0.0,), (1.0,)), (0, 1))
        return TrainingDataset(((0.0,), (1.0,), (0.0,), (1.0,)), (0, 1, 0, 1))


def _split_service(
    tmp_path: Path, *, trains_well: bool
) -> tuple[TrainingService, _Models, _SizeAwareBackend]:
    """Training data (2 rows) is fit perfectly iff `trains_well`.

    The evaluation data (4 rows) is always predicted perfectly when training is
    poor, and with a 0.5 error otherwise, so the two sets are distinguishable.
    """
    service, models, _ = _service(tmp_path, perfect=False)
    backend = _SizeAwareBackend(
        False, perfect_rows=_TRAIN_ROWS if trains_well else _EVAL_ROWS
    )
    service.backend = backend
    service.data = _SplitData()
    return service, models, backend


def _split_request(max_epochs: int, **overrides: object) -> TrainingRequest:
    return replace(
        TrainingRequest(
            sport_id="synthetic",
            training_years=(2020,),
            evaluation_years=(2021,),
            algorithm="USER_DEFINED",
            user_layout="1x2x2",
            hidden_layer_count=1,
            max_epochs=max_epochs,
            evaluation_interval=1,
            max_network_error=0.1,
            performance_threshold=0,
            normalizer="NORMALIZER_STANDARDIZE",
            seed=4,
        ),
        **overrides,
    )


def _progress(output: str) -> list[str]:
    return [
        record.message
        for record in output
        if record.message.startswith("Training progress")
    ]


def _evaluation(output: str) -> list[str]:
    return [
        line
        for record in output
        for line in record.message.splitlines()
        if line.startswith("Evaluation:")
    ]


def _summaries(output: list[logging.LogRecord]) -> list[str]:
    return [
        record.message
        for record in output
        if record.message.startswith("Network training completed")
    ]


def test_evaluation_data_is_never_scored_when_error_target_is_not_reached(
    tmp_path: Path, caplog: pytest.LogCaptureFixture
) -> None:
    service, models, backend = _split_service(tmp_path, trains_well=False)
    with caplog.at_level(logging.DEBUG, logger="sportsnet.application.training"):
        outcome = service.train(_split_request(max_epochs=5))
    assert backend.train_calls == outcome.epochs == 5
    assert set(backend.predicted_sizes) == {_TRAIN_ROWS}
    (summary,) = _summaries(caplog.records)
    assert "Evaluation: not run" in summary
    assert "Training confusion matrix:" in summary
    assert (
        "Actual \\ Predicted\n       | 0 | 1\n-------+---+--\n"
        "     0 | 1 | 0\n     1 | 1 | 0"
    ) in summary
    assert outcome.status == "completed_below_threshold"
    assert outcome.metrics is None
    assert outcome.stop_reason == "maximum_epochs_reached"
    assert models.saved == 0
    report = outcome.report
    assert report["networkSaved"] is False
    assert report["stopReason"] == "maximum_epochs_reached"
    assert report["numberOfEpochs"] == 5
    assert report["trainingError"] > 0
    for key in (
        "trainingAccuracy",
        "trainingPrecision",
        "trainingRecall",
        "trainingF1Score",
        "realTrainingAccuracy",
        "trainingConfusionMatrixCsv",
    ):
        assert key in report
    assert "networkAccuracy" not in report
    assert "confusionMatrixCsv" not in report


def test_evaluation_data_is_scored_exactly_once_after_error_target_is_reached(
    tmp_path: Path, caplog: pytest.LogCaptureFixture
) -> None:
    service, models, backend = _split_service(tmp_path, trains_well=True)
    with caplog.at_level(logging.DEBUG, logger="sportsnet.application.training"):
        outcome = service.train(_split_request(max_epochs=50))
    assert backend.train_calls == outcome.epochs == 1
    assert backend.predicted_sizes == [_TRAIN_ROWS, _EVAL_ROWS]
    (summary,) = _summaries(caplog.records)
    assert "Evaluation: error=0.5000 accuracy=0.5000" in summary
    assert "accuracy_percent=50.0000 performance_threshold=0.0000" in summary
    assert "qualifies_for_persistence=True" in summary
    assert "Evaluation confusion matrix:" in summary
    assert "Training:" not in summary
    assert "Training confusion matrix:" not in summary
    assert outcome.status == "persisted"
    assert outcome.metrics is not None
    assert models.saved == 1


def test_evaluation_below_performance_threshold_discards_the_model(
    tmp_path: Path, caplog: pytest.LogCaptureFixture
) -> None:
    service, models, backend = _split_service(tmp_path, trains_well=True)
    with caplog.at_level(logging.DEBUG, logger="sportsnet.application.training"):
        outcome = service.train(
            _split_request(max_epochs=50, performance_threshold=100)
        )
    (summary,) = _summaries(caplog.records)
    assert "qualifies_for_persistence=False" in summary
    assert backend.predicted_sizes.count(_EVAL_ROWS) == 1
    assert outcome.status == "completed_below_threshold"
    assert outcome.metrics is not None
    assert outcome.stop_reason == "max_network_error_reached"
    assert models.saved == 0


@pytest.mark.parametrize(
    ("threshold", "expected"),
    [(50, "persisted"), (50.01, "completed_below_threshold")],
)
def test_persistence_uses_evaluation_accuracy_not_composite(
    tmp_path: Path, threshold: float, expected: str
) -> None:
    service, models, _ = _split_service(tmp_path, trains_well=True)
    outcome = service.train(
        _split_request(max_epochs=1, performance_threshold=threshold)
    )
    assert outcome.metrics is not None
    assert outcome.metrics.accuracy == 0.5
    assert outcome.metrics.composite * 100 < 50
    assert outcome.status == expected
    assert models.saved == (1 if expected == "persisted" else 0)


def test_batch_reports_real_evaluation_metrics_when_performance_is_below_threshold(
    tmp_path: Path, caplog: pytest.LogCaptureFixture
) -> None:
    training, models, backend = _split_service(tmp_path, trains_well=True)
    batch = BatchService(training=training, management=training.management)
    batch._capacity.acquire()
    with caplog.at_level(logging.INFO):
        batch._run(
            "batch-id",
            _split_request(max_epochs=50, performance_threshold=100),
            1,
            Event(),
        )
    batch.shutdown()
    message = next(
        record.getMessage()
        for record in caplog.records
        if record.getMessage().startswith("Batch iteration completed")
    )
    assert "reached its training error target but did not meet" in message
    assert "evaluation performance threshold; it was not saved" in message
    assert "Training: error=" not in message
    assert "Evaluation: error=0.500000" in message
    assert "accuracy_percent=50.000000 performance_threshold=100.000000" in message
    assert "Training confusion matrix:" not in message
    assert "Evaluation confusion matrix:" in message
    assert models.saved == 0
    assert backend.predicted_sizes == [_TRAIN_ROWS, _EVAL_ROWS]


def test_batch_reports_accuracy_threshold_when_model_is_saved(
    tmp_path: Path, caplog: pytest.LogCaptureFixture
) -> None:
    training, models, _ = _split_service(tmp_path, trains_well=True)
    batch = BatchService(training=training, management=training.management)
    batch._capacity.acquire()
    with caplog.at_level(logging.INFO):
        batch._run(
            "batch-id",
            _split_request(max_epochs=1, performance_threshold=50),
            1,
            Event(),
        )
    batch.shutdown()
    message = next(
        record.getMessage()
        for record in caplog.records
        if record.getMessage().startswith("Batch iteration completed")
    )
    assert "evaluation performance threshold; it was saved" in message
    assert "accuracy_percent=50.000000 performance_threshold=50.000000" in message
    assert "Evaluation confusion matrix:" in message
    assert "Training confusion matrix:" not in message
    assert models.saved == 1


def test_batch_debug_does_not_duplicate_completion_summary(
    tmp_path: Path, caplog: pytest.LogCaptureFixture
) -> None:
    training, _, _ = _split_service(tmp_path, trains_well=True)
    batch = BatchService(training=training, management=training.management)
    batch._capacity.acquire()
    with caplog.at_level(logging.DEBUG):
        batch._run("batch-id", _split_request(max_epochs=1), 1, Event())
    batch.shutdown()
    assert _summaries(caplog.records) == []
    message = next(
        record.getMessage()
        for record in caplog.records
        if record.getMessage().startswith("Batch iteration completed")
    )
    assert "Evaluation confusion matrix:" in message
    assert "Training confusion matrix:" not in message


@pytest.mark.parametrize("threshold", [0.1, 0.5])
def test_info_logs_error_and_matrix_when_training_error_target_is_not_reached(
    tmp_path: Path, caplog: pytest.LogCaptureFixture, threshold: float
) -> None:
    service, _, backend = _split_service(tmp_path, trains_well=False)
    with caplog.at_level(logging.INFO, logger="sportsnet.application.training"):
        outcome = service.train(
            _split_request(max_epochs=3, max_network_error=threshold)
        )
    records = [
        record
        for record in caplog.records
        if record.name == "sportsnet.application.training"
    ]
    assert outcome.stop_reason == "maximum_epochs_reached"
    assert outcome.metrics is None
    assert set(backend.predicted_sizes) == {_TRAIN_ROWS}
    assert len(records) == 1
    assert records[0].levelno == logging.INFO
    message = records[0].getMessage()
    assert "training_error=0.500000" in message
    assert f"max_network_error={threshold:.6f}" in message
    assert f"model_id={outcome.model_id}" in message
    assert "epoch=3/3" in message
    assert (
        "Training confusion matrix:\nActual \\ Predicted\n"
        "       | 0 | 1\n-------+---+--\n     0 | 1 | 0\n     1 | 1 | 0"
    ) in message
    assert _summaries(caplog.records) == []
    assert _progress(caplog.records) == []


@pytest.mark.parametrize("performance_threshold", [0, 100])
def test_info_level_suppresses_metrics_when_training_error_target_is_reached(
    tmp_path: Path, caplog: pytest.LogCaptureFixture, performance_threshold: int
) -> None:
    service, _, _ = _split_service(tmp_path, trains_well=True)
    with caplog.at_level(logging.INFO, logger="sportsnet.application.training"):
        outcome = service.train(
            _split_request(max_epochs=3, performance_threshold=performance_threshold)
        )
    assert outcome.stop_reason == "max_network_error_reached"
    assert not [
        record
        for record in caplog.records
        if record.name == "sportsnet.application.training"
    ]
    assert _summaries(caplog.records) == []
    assert _progress(caplog.records) == []


def test_debug_logs_one_completion_summary_with_metrics(
    tmp_path: Path, caplog: pytest.LogCaptureFixture
) -> None:
    service, _, _ = _split_service(tmp_path, trains_well=False)
    with caplog.at_level(logging.DEBUG, logger="sportsnet.application.training"):
        service.train(_split_request(max_epochs=3))
    (summary,) = _summaries(caplog.records)
    assert "epoch=3/3" in summary
    assert "Training: loss=0.250000 error=0.5000 accuracy=0.5000" in summary
    assert "Evaluation: not run" in summary
    assert (
        next(record for record in caplog.records if record.message == summary).levelno
        == logging.DEBUG
    )


@pytest.mark.parametrize(
    ("max_epochs", "reporting_interval", "expected_epochs"),
    [
        (1, None, [1]),
        (10, None, list(range(1, 11))),
        (25, None, [3, 5, 8, 10, 13, 15, 18, 20, 23, 25]),
        (100, None, list(range(10, 101, 10))),
        (20, 5, [5, 10, 15, 20]),
        (20, 1, list(range(1, 21))),
        (10, 4, [4, 8, 10]),
        (10, 50, [10]),
    ],
)
def test_reporting_interval_controls_progress_lines_in_epochs(
    tmp_path: Path,
    caplog: pytest.LogCaptureFixture,
    max_epochs: int,
    reporting_interval: int | None,
    expected_epochs: list[int],
) -> None:
    service, _, backend = _split_service(tmp_path, trains_well=False)
    request = _split_request(
        max_epochs,
        evaluation_interval=max_epochs + 1,
        reporting_interval=reporting_interval,
    )
    with caplog.at_level(logging.DEBUG, logger="sportsnet.application.training"):
        outcome = service.train(request)
    progress = _progress(caplog.records)
    assert [
        int(line.split("epoch=")[1].split("/")[0]) for line in progress
    ] == expected_epochs
    assert len(_summaries(caplog.records)) == 1
    assert backend.train_calls == outcome.epochs == max_epochs


@pytest.mark.parametrize("reporting_interval", [None, 20])
def test_reporting_interval_does_not_change_when_training_stops(
    tmp_path: Path,
    caplog: pytest.LogCaptureFixture,
    reporting_interval: int | None,
) -> None:
    service, models, backend = _split_service(tmp_path, trains_well=True)
    request = _split_request(
        100, evaluation_interval=3, reporting_interval=reporting_interval
    )
    with caplog.at_level(logging.DEBUG, logger="sportsnet.application.training"):
        outcome = service.train(request)
    progress = _progress(caplog.records)
    assert len(progress) == 1
    assert "epoch=3/100" in progress[0]
    assert "decision=early_stop" in progress[0]
    assert len(_evaluation(caplog.records)) == 1
    assert len(_summaries(caplog.records)) == 1
    assert backend.train_calls == outcome.epochs == 3
    assert models.saved == 1


def test_non_positive_reporting_interval_is_rejected(tmp_path: Path) -> None:
    service, _, backend = _service(tmp_path, perfect=True)
    with pytest.raises(ValidationError, match="reporting interval"):
        service.train(_split_request(5, reporting_interval=0))
    assert backend.train_calls == 0


def test_saved_training_error_is_the_training_data_error_not_evaluation(
    tmp_path: Path, caplog: pytest.LogCaptureFixture
) -> None:
    service, models, _ = _split_service(tmp_path, trains_well=True)
    with caplog.at_level(logging.DEBUG, logger="sportsnet.application.training"):
        service.train(_split_request(max_epochs=50))
    progress = _progress(caplog.records)[0]
    logged = float(progress.split("error=")[1].split()[0])
    assert models.last["training_error"] == pytest.approx(logged, abs=1e-4)
    assert models.last["training_metrics"].recall == pytest.approx(
        1 - models.last["training_error"]
    )
