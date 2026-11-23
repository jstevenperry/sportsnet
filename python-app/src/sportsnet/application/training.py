"""Sport-neutral single-model training orchestration."""

import logging
import time
import uuid
from collections.abc import Callable, Mapping
from dataclasses import dataclass, field
from threading import Event
from typing import Any, Protocol

import numpy as np
from numpy.typing import NDArray

from sportsnet.application.management import ManagementService
from sportsnet.domain.datasets import TrainingDataset, validate_year_splits
from sportsnet.domain.errors import TrainingCancelled, ValidationError
from sportsnet.domain.events import ManagementEventType
from sportsnet.domain.layouts import resolve_layout
from sportsnet.domain.metrics import (
    ClassificationMetrics,
    calculate_metrics,
    confusion_matrix_table,
    early_stop_reached,
    evaluation_report_fields,
    should_persist,
    training_report_fields,
)
from sportsnet.domain.models import ModelSpec, TrainingEpochResult
from sportsnet.domain.preprocessing import Preprocessor
from sportsnet.domain.schemas import SportSchema
from sportsnet.sports.registry import SportRegistry

logger = logging.getLogger(__name__)


class DataProvider(Protocol):
    def build(self, sport: str, years: tuple[int, ...]) -> TrainingDataset: ...


class ModelBackend(Protocol):
    def build(self, spec: ModelSpec) -> object: ...

    def train_one_epoch(
        self, handle: object, features: NDArray[Any], labels: NDArray[Any]
    ) -> TrainingEpochResult: ...

    def predict_scores(
        self, handle: object, features: NDArray[Any]
    ) -> NDArray[Any]: ...


class ModelRepository(Protocol):
    def persist_model(
        self,
        *,
        model_id: str,
        sport: str,
        schema: SportSchema,
        model: object,
        model_spec: ModelSpec,
        preprocessor: Preprocessor,
        metrics: ClassificationMetrics,
        training_years: tuple[int, ...],
        evaluation_years: tuple[int, ...],
        seed: int,
        normalizer: str,
        stop_reason: str,
        threshold: float,
        epochs: int,
        algorithm: str,
        max_network_error: float,
        max_epochs: int,
        evaluation_interval: int,
        shuffle: bool,
        elapsed_time_millis: int,
        correlation_id: str,
        probe_features: NDArray[Any],
        training_error: float,
        training_metrics: ClassificationMetrics,
    ) -> Mapping[str, object]: ...


@dataclass(frozen=True, slots=True)
class TrainingRequest:
    sport_id: str
    training_years: tuple[int, ...]
    evaluation_years: tuple[int, ...]
    hidden_layer_count: int | None
    max_epochs: int
    evaluation_interval: int
    max_network_error: float
    performance_threshold: float
    normalizer: str
    seed: int
    algorithm: str | None = None
    user_layout: str | None = None
    hidden_activation: str = "RELU"
    output_activation: str = "SOFTMAX"
    loss: str = "SPARSE_MCXENT"
    optimizer: str = "ADAM"
    initializer: str = "XAVIER_UNIFORM"
    learning_rate: float = 0.001
    l2: float = 0.0
    shuffle: bool = False
    reporting_interval: int | None = None


@dataclass(frozen=True, slots=True)
class TrainingOutcome:
    status: str
    model_id: str
    descriptor: Mapping[str, object] | None
    metrics: ClassificationMetrics | None
    epochs: int
    stop_reason: str
    layout: tuple[int, ...]
    training_metrics: ClassificationMetrics
    training_loss: float
    classes: tuple[int, ...]
    report: Mapping[str, object] = field(default_factory=dict)


class TrainingService:
    def __init__(
        self,
        *,
        registry: SportRegistry,
        data: DataProvider,
        backend: ModelBackend,
        models: ModelRepository,
        management: ManagementService,
        id_factory: Callable[[], str] | None = None,
    ) -> None:
        self.registry = registry
        self.data = data
        self.backend = backend
        self.models = models
        self.management = management
        self.id_factory = id_factory or (lambda: str(uuid.uuid4()))

    def train(
        self,
        request: TrainingRequest,
        cancellation: Event | None = None,
        *,
        correlation_id: str | None = None,
        batch: bool = False,
    ) -> TrainingOutcome:
        schema = self._validate_request(request)
        correlation_id = correlation_id or self.id_factory()
        model_id = self.id_factory()
        start_time = time.monotonic()
        self.management.create(
            management_event_type=ManagementEventType.TRAIN_NETWORK_START,
            sport=schema.sport_id,
            correlation_id=correlation_id,
            network_descriptor={"networkId": model_id},
            message="training started",
        )
        try:
            outcome = self._run(
                schema,
                request,
                model_id,
                correlation_id,
                start_time,
                cancellation,
                batch,
            )
        except TrainingCancelled:
            self.management.create(
                management_event_type=ManagementEventType.TRAIN_NETWORK_END,
                sport=schema.sport_id,
                correlation_id=correlation_id,
                network_descriptor={"networkId": model_id},
                message="training cancelled",
                elapsed_time_millis=_elapsed(start_time),
            )
            raise
        except Exception:
            try:
                self.management.create(
                    management_event_type=ManagementEventType.EXCEPTION,
                    sport=schema.sport_id,
                    correlation_id=correlation_id,
                    network_descriptor={"networkId": model_id},
                    message="training failed",
                    elapsed_time_millis=_elapsed(start_time),
                )
            except Exception:
                logger.exception("failed to persist training failure event")
            raise
        self.management.create(
            management_event_type=ManagementEventType.TRAIN_NETWORK_END,
            sport=schema.sport_id,
            correlation_id=correlation_id,
            network_descriptor=dict(outcome.descriptor) if outcome.descriptor else None,
            message=outcome.status,
            elapsed_time_millis=_elapsed(start_time),
            additional_context={
                "status": outcome.status,
                "epochs": outcome.epochs,
                "stopReason": outcome.stop_reason,
            },
        )
        return outcome

    def _run(
        self,
        schema: SportSchema,
        request: TrainingRequest,
        model_id: str,
        correlation_id: str,
        start_time: float,
        cancellation: Event | None,
        batch: bool,
    ) -> TrainingOutcome:
        training = self.data.build(schema.sport_id, request.training_years)
        evaluation = self.data.build(schema.sport_id, request.evaluation_years)
        train_x, train_y = _validate_dataset(training, schema, "training")
        eval_x, eval_y = _validate_dataset(evaluation, schema, "evaluation")
        class_indexes = {
            label: index for index, label in enumerate(schema.output_classes)
        }
        train_indexes = np.asarray([class_indexes[label] for label in train_y])
        evaluation_preprocessor = Preprocessor.fit(train_x.tolist(), request.normalizer)
        train_values = evaluation_preprocessor.transform(train_x.tolist())
        eval_values = evaluation_preprocessor.transform(eval_x.tolist())
        layout = resolve_layout(
            input_width=schema.input_width,
            output_width=schema.output_width,
            hidden_layer_count=request.hidden_layer_count,
            algorithm=request.algorithm,
            user_layout=request.user_layout,
            seed=request.seed,
        )
        spec = ModelSpec(
            layout=layout,
            output_classes=schema.output_classes,
            hidden_activation=request.hidden_activation,
            output_activation=request.output_activation,
            loss=request.loss,
            optimizer=request.optimizer,
            initializer=request.initializer,
            learning_rate=request.learning_rate,
            l2=request.l2,
            seed=request.seed,
        )
        handle = self.backend.build(spec)
        rng = np.random.default_rng(request.seed)
        metrics: ClassificationMetrics | None = None
        training_error = 1.0
        last_training_metrics: ClassificationMetrics | None = None
        epochs = 0
        stop_reason = "maximum_epochs_reached"
        should_save = False
        for epoch in range(1, request.max_epochs + 1):
            if cancellation is not None and cancellation.is_set():
                raise TrainingCancelled("training cancelled before epoch")
            if request.shuffle:
                order = rng.permutation(len(train_values))
                epoch_x, epoch_y = train_values[order], train_indexes[order]
            else:
                epoch_x, epoch_y = train_values, train_indexes
            epoch_result = self.backend.train_one_epoch(handle, epoch_x, epoch_y)
            epochs = epoch
            is_check = (
                epoch % request.evaluation_interval == 0 or epoch == request.max_epochs
            )
            if request.reporting_interval is None:
                is_milestone = (epoch * 10 // request.max_epochs) > (
                    (epoch - 1) * 10 // request.max_epochs
                )
            else:
                is_milestone = epoch % request.reporting_interval == 0
            if not is_check and not is_milestone:
                continue
            training_metrics = calculate_metrics(
                train_y,
                self.backend.predict_scores(handle, train_values).tolist(),
                schema.output_classes,
            )
            training_error = 1 - training_metrics.recall
            last_training_metrics = training_metrics
            early_stopped = is_check and early_stop_reached(
                training_metrics.recall, request.max_network_error
            )
            if is_milestone or early_stopped or epoch == request.max_epochs:
                decision = (
                    "early_stop"
                    if early_stopped
                    else "maximum_epochs_reached"
                    if epoch == request.max_epochs
                    else "continue"
                    if is_check
                    else "monitoring_only"
                )
                logger.debug(
                    "Training progress sport=%s model_id=%s correlation_id=%s "
                    "epoch=%d/%d training_loss=%.6f training_error=%.4f "
                    "training_accuracy=%.4f max_network_error=%.4f decision=%s",
                    schema.sport_id,
                    model_id,
                    correlation_id,
                    epoch,
                    request.max_epochs,
                    epoch_result.loss,
                    training_error,
                    training_metrics.accuracy,
                    request.max_network_error,
                    decision,
                )
            if early_stopped:
                stop_reason = "max_network_error_reached"
                break
        if cancellation is not None and cancellation.is_set():
            raise TrainingCancelled("training cancelled before evaluation")
        if stop_reason == "max_network_error_reached":
            metrics = calculate_metrics(
                eval_y,
                self.backend.predict_scores(handle, eval_values).tolist(),
                schema.output_classes,
            )
            should_save = should_persist(
                early_stopped=True,
                accuracy=metrics.accuracy,
                threshold=request.performance_threshold,
            )
        if cancellation is not None and cancellation.is_set():
            raise TrainingCancelled("training cancelled before persistence")
        if not batch:
            _log_training_summary(
                logger=logger,
                sport=schema.sport_id,
                model_id=model_id,
                correlation_id=correlation_id,
                epochs=epochs,
                max_epochs=request.max_epochs,
                training_loss=epoch_result.loss,
                training_error=training_error,
                max_network_error=request.max_network_error,
                stop_reason=stop_reason,
                training_metrics=last_training_metrics,
                evaluation_metrics=metrics,
                classes=schema.output_classes,
                performance_threshold=request.performance_threshold,
                qualifies_for_persistence=should_save,
            )
        if last_training_metrics is None:
            raise ValidationError("training ended without training metrics")
        if not should_save:
            return TrainingOutcome(
                "completed_below_threshold",
                model_id,
                None,
                metrics,
                epochs,
                stop_reason,
                layout,
                last_training_metrics,
                epoch_result.loss,
                schema.output_classes,
                _unsaved_report(
                    model_id=model_id,
                    schema=schema,
                    request=request,
                    stop_reason=stop_reason,
                    epochs=epochs,
                    training_error=training_error,
                    training_metrics=last_training_metrics,
                    metrics=metrics,
                    elapsed_time_millis=_elapsed(start_time),
                ),
            )
        if metrics is None:
            raise ValidationError("training ended without evaluation metrics")
        descriptor = self.models.persist_model(
            model_id=model_id,
            sport=schema.sport_id,
            schema=schema,
            model=handle,
            model_spec=spec,
            preprocessor=evaluation_preprocessor,
            metrics=metrics,
            training_years=request.training_years,
            evaluation_years=request.evaluation_years,
            seed=request.seed,
            normalizer=request.normalizer,
            stop_reason=stop_reason,
            threshold=request.performance_threshold,
            epochs=epochs,
            algorithm=request.algorithm or "",
            max_network_error=request.max_network_error,
            max_epochs=request.max_epochs,
            evaluation_interval=request.evaluation_interval,
            shuffle=request.shuffle,
            elapsed_time_millis=_elapsed(start_time),
            correlation_id=correlation_id,
            probe_features=eval_values[:1],
            training_error=training_error,
            training_metrics=last_training_metrics,
        )
        return TrainingOutcome(
            "persisted",
            model_id,
            descriptor,
            metrics,
            epochs,
            stop_reason,
            layout,
            last_training_metrics,
            epoch_result.loss,
            schema.output_classes,
        )

    def _validate_request(self, request: TrainingRequest) -> SportSchema:
        schema = self.registry.get(request.sport_id)
        validate_year_splits(request.training_years, request.evaluation_years)
        if set(request.training_years).difference(schema.training_years):
            raise ValidationError("training years are unsupported for this sport")
        if set(request.evaluation_years).difference(schema.training_years):
            raise ValidationError("evaluation years are unsupported for this sport")
        if request.max_epochs <= 0 or request.max_epochs > 10_000:
            raise ValidationError("max epochs must be in [1, 10000]")
        if request.evaluation_interval <= 0:
            raise ValidationError("evaluation interval must be positive")
        if request.reporting_interval is not None and request.reporting_interval <= 0:
            raise ValidationError("reporting interval must be positive")
        if not 0 <= request.max_network_error <= 1:
            raise ValidationError("max network error must be in [0, 1]")
        if not 0 <= request.performance_threshold <= 100:
            raise ValidationError("performance threshold must be in [0, 100]")
        if not np.isfinite(request.learning_rate) or request.learning_rate <= 0:
            raise ValidationError("learning rate must be positive and finite")
        if not np.isfinite(request.l2) or request.l2 < 0:
            raise ValidationError("L2 regularization must be finite and non-negative")
        return schema


def _log_training_summary(
    *,
    logger: logging.Logger,
    sport: str,
    model_id: str,
    correlation_id: str,
    epochs: int,
    max_epochs: int,
    training_loss: float,
    training_error: float,
    max_network_error: float,
    stop_reason: str,
    training_metrics: ClassificationMetrics | None,
    evaluation_metrics: ClassificationMetrics | None,
    classes: tuple[int, ...],
    performance_threshold: float,
    qualifies_for_persistence: bool,
) -> None:
    if training_metrics is None:
        raise ValidationError("training ended without training metrics")
    if stop_reason == "maximum_epochs_reached":
        logger.info(
            "Network training did not reach the error target sport=%s model_id=%s "
            "correlation_id=%s epoch=%d/%d training_error=%.6f "
            "max_network_error=%.6f\nTraining confusion matrix:\n%s",
            sport,
            model_id,
            correlation_id,
            epochs,
            max_epochs,
            training_error,
            max_network_error,
            confusion_matrix_table(training_metrics, classes),
        )
    if not logger.isEnabledFor(logging.DEBUG):
        return
    lines = [
        f"Network training completed sport={sport} model_id={model_id} "
        f"correlation_id={correlation_id} epoch={epochs}/{max_epochs}",
    ]
    if evaluation_metrics is None:
        lines.extend(
            (
                f"Training: loss={training_loss:.6f} error={training_error:.4f} "
                f"accuracy={training_metrics.accuracy:.4f} "
                f"precision={training_metrics.precision:.4f} "
                f"recall={training_metrics.recall:.4f} "
                f"f1={training_metrics.f1:.4f} "
                f"composite_percent={training_metrics.composite * 100:.4f}",
                "Training confusion matrix:\n"
                + confusion_matrix_table(training_metrics, classes),
                "Evaluation: not run (training did not reach the error target)",
            )
        )
    else:
        lines.extend(
            (
                f"Evaluation: error={1 - evaluation_metrics.recall:.4f} "
                f"accuracy={evaluation_metrics.accuracy:.4f} "
                f"precision={evaluation_metrics.precision:.4f} "
                f"recall={evaluation_metrics.recall:.4f} "
                f"f1={evaluation_metrics.f1:.4f} "
                f"composite_percent={evaluation_metrics.composite * 100:.4f} "
                f"accuracy_percent={evaluation_metrics.accuracy * 100:.4f} "
                f"performance_threshold={performance_threshold:.4f} "
                f"qualifies_for_persistence={qualifies_for_persistence}",
                "Evaluation confusion matrix:\n"
                + confusion_matrix_table(evaluation_metrics, classes),
            )
        )
    logger.debug("%s", "\n".join(lines))


def _validate_dataset(
    dataset: TrainingDataset, schema: SportSchema, label: str
) -> tuple[NDArray[Any], tuple[int, ...]]:
    try:
        features = np.asarray(dataset.features, dtype=np.float64)
        labels = np.asarray(dataset.labels)
    except (TypeError, ValueError) as error:
        raise ValidationError(
            f"{label} dataset is not rectangular numeric data"
        ) from error
    if (
        features.ndim != 2
        or features.shape[0] == 0
        or features.shape[1] != schema.input_width
        or labels.ndim != 1
        or labels.shape[0] != features.shape[0]
        or not np.isfinite(features).all()
    ):
        raise ValidationError(
            f"{label} dataset has invalid shapes or non-finite features"
        )
    if not np.issubdtype(labels.dtype, np.integer):
        raise ValidationError(f"{label} labels must be integers")
    if any(int(value) not in schema.output_classes for value in labels):
        raise ValidationError(f"{label} labels are outside the declared class order")
    return features, tuple(int(value) for value in labels)


def _elapsed(start_time: float) -> int:
    return max(0, int((time.monotonic() - start_time) * 1000))


def _unsaved_report(
    *,
    model_id: str,
    schema: SportSchema,
    request: TrainingRequest,
    stop_reason: str,
    epochs: int,
    training_error: float,
    training_metrics: ClassificationMetrics | None,
    metrics: ClassificationMetrics | None,
    elapsed_time_millis: int,
) -> dict[str, object]:
    classes = schema.output_classes
    report: dict[str, object] = {
        "networkId": model_id,
        "networkType": schema.sport_id,
        "networkSaved": False,
        "stopReason": stop_reason,
        "numberOfEpochs": epochs,
        "maxNumberOfTrainingEpochs": request.max_epochs,
        "maxNetworkError": request.max_network_error,
        "networkPerformanceThreshold": request.performance_threshold,
        "yearsToTrainNetwork": list(request.training_years),
        "yearsToEvaluateNetwork": list(request.evaluation_years),
        "elapsedTimeMillis": elapsed_time_millis,
        "trainingError": training_error,
    }
    if training_metrics is not None:
        report.update(training_report_fields(training_metrics, classes))
    if metrics is not None:
        report.update(evaluation_report_fields(metrics, classes))
    return report
