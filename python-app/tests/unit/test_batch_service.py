import logging
from dataclasses import replace
from pathlib import Path
from threading import Event
from types import SimpleNamespace

import pytest

from sportsnet.adapters.sqlite_store import SQLiteEventStore
from sportsnet.application.batch import BatchService
from sportsnet.application.management import ManagementService
from sportsnet.application.training import TrainingOutcome, TrainingRequest
from sportsnet.domain.errors import RepositoryError, TrainingCancelled, ValidationError
from sportsnet.domain.events import ManagementEventType
from sportsnet.domain.metrics import calculate_metrics

_METRICS = calculate_metrics([0, 1], [[1, 0], [1, 0]], (0, 1))


class _BlockingTraining:
    def __init__(self) -> None:
        self.started = Event()
        self.release = Event()
        self.finished = Event()
        self.correlations: list[str] = []

    def train(self, request, cancellation=None, *, correlation_id=None, batch=False):
        self.correlations.append(correlation_id)
        self.started.set()
        self.release.wait(timeout=3)
        self.finished.set()
        return TrainingOutcome(
            "completed_below_threshold",
            "model-id",
            None,
            None,
            1,
            "threshold",
            (1, 2, 2),
            _METRICS,
            0.25,
            (0, 1),
        )


def _request() -> TrainingRequest:
    return TrainingRequest(
        sport_id="synthetic",
        training_years=(2020,),
        evaluation_years=(2021,),
        hidden_layer_count=1,
        max_epochs=1,
        evaluation_interval=1,
        max_network_error=0.1,
        performance_threshold=70,
        normalizer="NORMALIZER_STANDARDIZE",
        seed=3,
    )


def test_batch_id_is_assigned_before_enqueue_and_capacity_is_bounded(
    tmp_path: Path,
) -> None:
    training = _BlockingTraining()
    identifiers = iter(("batch-1", "batch-2"))
    service = BatchService(
        training=training,
        management=ManagementService(SQLiteEventStore(tmp_path / "events.sqlite")),
        max_workers=1,
        queue_capacity=0,
        max_iterations=5,
        id_factory=lambda: next(identifiers),
    )
    accepted = service.submit(_request(), number_of_iterations=1)
    assert accepted["batchTrainingRunId"] == "batch-1"
    assert training.started.wait(timeout=1)
    with pytest.raises(ValidationError, match="capacity"):
        service.submit(_request(), number_of_iterations=1)
    training.release.set()
    service.shutdown()
    assert training.correlations == ["batch-1"]


def test_shutdown_respects_deadline_and_signals_active_training(
    tmp_path: Path,
) -> None:
    training = _BlockingTraining()
    service = BatchService(
        training=training,
        management=ManagementService(SQLiteEventStore(tmp_path / "events.sqlite")),
    )
    service.submit(_request(), number_of_iterations=1)
    assert training.started.wait(timeout=1)

    stopped = service.shutdown(timeout_seconds=0.01)

    assert stopped is False
    training.release.set()
    assert training.finished.wait(timeout=1)


def test_failed_iteration_logs_exception_chain_and_batch_context(
    tmp_path: Path, caplog: pytest.LogCaptureFixture
) -> None:
    class _FailingTraining:
        def train(
            self, request, cancellation=None, *, correlation_id=None, batch=False
        ):
            try:
                raise OSError("filesystem does not support hard links")
            except OSError as error:
                raise RepositoryError("model file publication failed") from error

    management = ManagementService(SQLiteEventStore(tmp_path / "events.sqlite"))
    service = BatchService(training=_FailingTraining(), management=management)
    service._capacity.acquire()
    with caplog.at_level(logging.ERROR, logger="sportsnet.application.batch"):
        service._run("run-123", _request(), 1, Event())
    service.shutdown()

    assert "run_id=run-123 sport=synthetic iteration=1 seed=4" in caplog.text
    assert "RepositoryError: model file publication failed" in caplog.text
    assert "OSError: filesystem does not support hard links" in caplog.text
    end = next(
        event
        for event in management.list_all()
        if event.management_event_type == ManagementEventType.TRAIN_NETWORK_BATCH_END
    )
    assert end.additional_context["failed"] == 1
    assert end.additional_context["cancelled"] == 0


def test_cancelled_iteration_logs_reason(
    tmp_path: Path, caplog: pytest.LogCaptureFixture
) -> None:
    class _CancelledTraining:
        def train(
            self, request, cancellation=None, *, correlation_id=None, batch=False
        ):
            raise TrainingCancelled("training cancelled before persistence")

    management = ManagementService(SQLiteEventStore(tmp_path / "events.sqlite"))
    service = BatchService(training=_CancelledTraining(), management=management)
    service._capacity.acquire()
    with caplog.at_level(logging.WARNING, logger="sportsnet.application.batch"):
        service._run("run-456", _request(), 1, Event())
    service.shutdown()

    assert "cancelled run_id=run-456 sport=synthetic iteration=1" in caplog.text
    assert "training cancelled before persistence" in caplog.text
    end = next(
        event
        for event in management.list_all()
        if event.management_event_type == ManagementEventType.TRAIN_NETWORK_BATCH_END
    )
    assert end.additional_context["failed"] == 0
    assert end.additional_context["cancelled"] == 1


def test_batch_logs_iteration_position_and_results(
    tmp_path: Path, caplog: pytest.LogCaptureFixture, monkeypatch: pytest.MonkeyPatch
) -> None:
    class _Training:
        def __init__(self) -> None:
            self.iteration = 0

        def train(
            self, request, cancellation=None, *, correlation_id=None, batch=False
        ):
            self.iteration += 1
            status = "persisted" if self.iteration == 1 else "completed_below_threshold"
            return TrainingOutcome(
                status,
                f"model-{self.iteration}",
                None,
                _METRICS if self.iteration == 1 else None,
                3,
                "max_network_error_reached"
                if self.iteration == 1
                else "maximum_epochs_reached",
                (1, self.iteration + 2, 2),
                _METRICS,
                0.25,
                (0, 1),
            )

    times = iter((10.0, 11.25, 30.0, 32.5))
    monkeypatch.setattr(
        "sportsnet.application.batch.time",
        SimpleNamespace(monotonic=lambda: next(times)),
    )
    management = ManagementService(SQLiteEventStore(tmp_path / "events.sqlite"))
    service = BatchService(training=_Training(), management=management)
    service._capacity.acquire()
    with caplog.at_level(logging.INFO, logger="sportsnet.application.batch"):
        service._run(
            "run-789",
            replace(
                _request(),
                training_years=(2019, 2020),
                evaluation_years=(2021,),
                algorithm="USER_DEFINED",
                hidden_layer_count=None,
                user_layout="1x3x2",
                optimizer="SGD",
                hidden_activation="TANH",
                output_activation="SOFTMAX",
                l2=0.003,
                learning_rate=0.02,
            ),
            2,
            Event(),
        )
    service.shutdown()

    messages = [record.getMessage() for record in caplog.records]
    assert len(messages) == 5
    assert "started run_id=run-789 sport=synthetic iteration=1/2" in messages[0]
    assert "remaining_after=1 seed=4" in messages[0]
    assert "iteration=1/2 remaining=1 status=persisted model_id=model-1" in messages[1]
    assert "epochs=3 stop_reason=max_network_error_reached" in messages[1]
    assert "evaluation performance threshold; it was saved" in messages[1]
    assert "Training: error=" not in messages[1]
    assert "Training loss=" not in messages[1]
    assert "Evaluation: error=0.500000" in messages[1]
    assert "Training confusion matrix:" not in messages[1]
    assert "Evaluation confusion matrix:" in messages[1]
    assert "layout=1x3x2 iteration_elapsed_ms=1250" in messages[1]
    assert "total_training_elapsed_ms=1250" in messages[1]
    assert "started run_id=run-789 sport=synthetic iteration=2/2" in messages[2]
    assert "remaining_after=0 seed=5" in messages[2]
    assert (
        "iteration=2/2 remaining=0 status=completed_below_threshold model_id=model-2"
        in messages[3]
    )
    assert "layout=1x4x2 iteration_elapsed_ms=2500" in messages[3]
    assert "total_training_elapsed_ms=3750" in messages[3]
    assert "did not reach its training error target" in messages[3]
    assert "Evaluation: not run." in messages[3]
    assert "Training confusion matrix:" in messages[3]
    assert "Training: error=0.500000" in messages[3]
    assert "Training loss=0.250000" in messages[3]
    assert "Evaluation confusion matrix:" not in messages[3]
    assert messages[4] == (
        "Batch training summary run_id=run-789 sport=synthetic\n"
        "  Training years:                   2019, 2020\n"
        "  Evaluation years:                 2021\n"
        "  Structure algorithm:              USER_DEFINED\n"
        "  Hidden layers (completed):        1\n"
        "  Updater (optimizer):              SGD\n"
        "  Hidden activation:                TANH\n"
        "  Output activation:                SOFTMAX\n"
        "  L2 regularization lambda:         0.003\n"
        "  Learning rate:                    0.02\n"
        "  Iterations requested:             2\n"
        "  Iterations started:               2\n"
        "  Iterations not started:           0\n"
        "  Networks trained:                 1\n"
        "  Networks saved:                   1\n"
        "  Training target not met:          1\n"
        "  Below evaluation threshold:       0\n"
        "  Failed:                           0\n"
        "  Cancelled:                        0\n"
        "  Total training time (ms):         3750\n"
        "  Average time per iteration (ms):  1875.00"
    )
    end = next(
        event
        for event in management.list_all()
        if event.management_event_type == ManagementEventType.TRAIN_NETWORK_BATCH_END
    )
    assert end.additional_context == {
        "persisted": 1,
        "completedBelowThreshold": 1,
        "failed": 0,
        "cancelled": 0,
        "requestedIterations": 2,
        "startedIterations": 2,
        "notStarted": 0,
        "trained": 1,
        "trainingTargetNotMet": 1,
        "evaluationBelowThreshold": 0,
        "totalTrainingElapsedMillis": 3750,
        "averageIterationElapsedMillis": 1875.0,
        "trainingYears": [2019, 2020],
        "evaluationYears": [2021],
        "networkStructureAlgorithm": "USER_DEFINED",
        "hiddenLayerCounts": [1],
        "updater": "SGD",
        "hiddenActivation": "TANH",
        "outputActivation": "SOFTMAX",
        "l2": 0.003,
        "learningRate": 0.02,
    }


def test_batch_summary_distinguishes_evaluation_rejection_and_failure(
    tmp_path: Path, caplog: pytest.LogCaptureFixture
) -> None:
    class _Training:
        def __init__(self) -> None:
            self.iteration = 0

        def train(
            self, request, cancellation=None, *, correlation_id=None, batch=False
        ):
            self.iteration += 1
            if self.iteration == 3:
                raise RepositoryError("training failed")
            return TrainingOutcome(
                "persisted" if self.iteration == 1 else "completed_below_threshold",
                f"model-{self.iteration}",
                None,
                _METRICS,
                1,
                "max_network_error_reached",
                (1, 2, 2),
                _METRICS,
                0.25,
                (0, 1),
            )

    management = ManagementService(SQLiteEventStore(tmp_path / "events.sqlite"))
    service = BatchService(training=_Training(), management=management)
    service._capacity.acquire()
    with caplog.at_level(logging.INFO, logger="sportsnet.application.batch"):
        service._run("run-mixed", _request(), 4, Event())
    service.shutdown()

    summary = next(
        record.getMessage()
        for record in caplog.records
        if record.getMessage().startswith("Batch training summary")
    )
    assert "Iterations requested:             4" in summary
    assert "Iterations started:               3" in summary
    assert "Iterations not started:           1" in summary
    assert "Networks trained:                 2" in summary
    assert "Networks saved:                   1" in summary
    assert "Training target not met:          0" in summary
    assert "Below evaluation threshold:       1" in summary
    assert "Failed:                           1" in summary
    assert "Cancelled:                        0" in summary
    end = next(
        event
        for event in management.list_all()
        if event.management_event_type == ManagementEventType.TRAIN_NETWORK_BATCH_END
    )
    assert end.additional_context["trained"] == 2
    assert end.additional_context["persisted"] == 1
    assert end.additional_context["evaluationBelowThreshold"] == 1
    assert end.additional_context["notStarted"] == 1


@pytest.mark.parametrize("before_start", [True, False])
def test_batch_summary_reports_cancellation(
    tmp_path: Path, caplog: pytest.LogCaptureFixture, before_start: bool
) -> None:
    class _Training:
        def train(
            self, request, cancellation=None, *, correlation_id=None, batch=False
        ):
            raise TrainingCancelled("cancelled during training")

    management = ManagementService(SQLiteEventStore(tmp_path / "events.sqlite"))
    service = BatchService(training=_Training(), management=management)
    cancellation = Event()
    if before_start:
        cancellation.set()
    service._capacity.acquire()
    with caplog.at_level(logging.INFO, logger="sportsnet.application.batch"):
        service._run("run-cancelled", _request(), 3, cancellation)
    service.shutdown()

    summary = next(
        record.getMessage()
        for record in caplog.records
        if record.getMessage().startswith("Batch training summary")
    )
    started = 0 if before_start else 1
    assert f"Iterations started:               {started}" in summary
    assert f"Iterations not started:           {3 - started}" in summary
    assert "Networks trained:                 0" in summary
    assert "Networks saved:                   0" in summary
    assert "Training target not met:          0" in summary
    assert "Failed:                           0" in summary
    assert "Cancelled:                        1" in summary
    assert "Hidden layers (completed):        N/A (no completed iterations)" in summary
    end = next(
        event
        for event in management.list_all()
        if event.management_event_type == ManagementEventType.TRAIN_NETWORK_BATCH_END
    )
    if before_start:
        assert (
            "Average time per iteration (ms):  N/A (no iterations started)" in summary
        )
        assert end.additional_context["averageIterationElapsedMillis"] is None
    else:
        assert end.additional_context["averageIterationElapsedMillis"] is not None
    assert end.additional_context["hiddenLayerCounts"] == []


def test_batch_summary_counts_cancellation_between_iterations(
    tmp_path: Path, caplog: pytest.LogCaptureFixture
) -> None:
    class _Training:
        def train(
            self, request, cancellation=None, *, correlation_id=None, batch=False
        ):
            cancellation.set()
            return TrainingOutcome(
                "persisted",
                "model-id",
                None,
                _METRICS,
                1,
                "max_network_error_reached",
                (1, 2, 2),
                _METRICS,
                0.25,
                (0, 1),
            )

    management = ManagementService(SQLiteEventStore(tmp_path / "events.sqlite"))
    service = BatchService(training=_Training(), management=management)
    service._capacity.acquire()
    with caplog.at_level(logging.INFO, logger="sportsnet.application.batch"):
        service._run("run-cancelled", _request(), 3, Event())
    service.shutdown()

    summary = next(
        record.getMessage()
        for record in caplog.records
        if record.getMessage().startswith("Batch training summary")
    )
    assert "Iterations started:               1" in summary
    assert "Iterations not started:           2" in summary
    assert "Networks trained:                 1" in summary
    assert "Networks saved:                   1" in summary
    assert "Training target not met:          0" in summary
    assert "Failed:                           0" in summary
    assert "Cancelled:                        1" in summary


def test_batch_total_includes_failed_iterations(
    tmp_path: Path, caplog: pytest.LogCaptureFixture, monkeypatch: pytest.MonkeyPatch
) -> None:
    class _Training:
        def __init__(self) -> None:
            self.iteration = 0

        def train(
            self, request, cancellation=None, *, correlation_id=None, batch=False
        ):
            self.iteration += 1
            if self.iteration == 1:
                raise RepositoryError("training failed")
            return TrainingOutcome(
                "persisted",
                "model-id",
                None,
                _METRICS,
                1,
                "max_network_error_reached",
                (1, 2, 2),
                _METRICS,
                0.25,
                (0, 1),
            )

    times = iter((10.0, 11.0, 20.0, 22.0))
    monkeypatch.setattr(
        "sportsnet.application.batch.time",
        SimpleNamespace(monotonic=lambda: next(times)),
    )
    service = BatchService(
        training=_Training(),
        management=ManagementService(SQLiteEventStore(tmp_path / "events.sqlite")),
        continue_on_failure=True,
    )
    service._capacity.acquire()
    with caplog.at_level(logging.INFO, logger="sportsnet.application.batch"):
        service._run("run-123", _request(), 2, Event())
    service.shutdown()

    messages = [record.getMessage() for record in caplog.records]
    assert any("total_training_elapsed_ms=1000" in message for message in messages)
    assert any(
        "iteration_elapsed_ms=2000 total_training_elapsed_ms=3000" in message
        for message in messages
    )
    assert any(
        "Iterations started:               2" in message
        and "Networks saved:                   1" in message
        and "Failed:                           1" in message
        and "Total training time (ms):         3000" in message
        and "Average time per iteration (ms):  1500.00" in message
        for message in messages
    )


def test_batch_logs_evaluation_threshold_rejection(
    tmp_path: Path, caplog: pytest.LogCaptureFixture
) -> None:
    class _Training:
        def train(
            self, request, cancellation=None, *, correlation_id=None, batch=False
        ):
            return TrainingOutcome(
                "completed_below_threshold",
                "model-id",
                None,
                _METRICS,
                1,
                "max_network_error_reached",
                (1, 2, 2),
                _METRICS,
                0.25,
                (0, 1),
            )

    service = BatchService(
        training=_Training(),
        management=ManagementService(SQLiteEventStore(tmp_path / "events.sqlite")),
    )
    service._capacity.acquire()
    with caplog.at_level(logging.INFO, logger="sportsnet.application.batch"):
        service._run("run-123", _request(), 1, Event())
    service.shutdown()
    message = next(
        record.getMessage()
        for record in caplog.records
        if record.getMessage().startswith("Batch iteration completed")
    )
    assert "reached its training error target but did not meet" in message
    assert "evaluation performance threshold; it was not saved" in message
    assert (
        "Evaluation accuracy_percent=50.000000 performance_threshold=70.000000"
        in message
    )
    assert "Training confusion matrix:" not in message
    assert "Training: error=" not in message
    assert "Training loss=" not in message
    assert "Evaluation confusion matrix:" in message
