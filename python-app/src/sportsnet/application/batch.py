"""Bounded in-process batch training executor."""

import logging
import threading
import time
import uuid
from collections.abc import Callable
from concurrent.futures import Future, ThreadPoolExecutor, wait
from dataclasses import replace

from sportsnet.application.management import ManagementService
from sportsnet.application.training import (
    TrainingOutcome,
    TrainingRequest,
    TrainingService,
)
from sportsnet.domain.errors import TrainingCancelled, ValidationError
from sportsnet.domain.events import ManagementEventType
from sportsnet.domain.metrics import ClassificationMetrics, confusion_matrix_table

logger = logging.getLogger(__name__)


class BatchService:
    def __init__(
        self,
        *,
        training: TrainingService,
        management: ManagementService,
        max_workers: int = 1,
        queue_capacity: int = 10,
        max_iterations: int = 100,
        continue_on_failure: bool = False,
        id_factory: Callable[[], str] | None = None,
    ) -> None:
        if max_workers < 1 or queue_capacity < 0 or max_iterations < 1:
            raise ValidationError(
                "batch worker, queue, and iteration limits are invalid"
            )
        self.training = training
        self.management = management
        self.max_iterations = max_iterations
        self.continue_on_failure = continue_on_failure
        self.id_factory = id_factory or (lambda: str(uuid.uuid4()))
        self._executor = ThreadPoolExecutor(
            max_workers=max_workers, thread_name_prefix="sportsnet-training"
        )
        self._capacity = threading.BoundedSemaphore(max_workers + queue_capacity)
        self._active: dict[str, threading.Event] = {}
        self._futures: set[Future[None]] = set()
        self._lock = threading.Lock()
        self._accepting = True

    def submit(
        self, request: TrainingRequest, *, number_of_iterations: int
    ) -> dict[str, object]:
        if not 1 <= number_of_iterations <= self.max_iterations:
            raise ValidationError(
                f"batch iterations must be in [1, {self.max_iterations}]"
            )
        with self._lock:
            if not self._accepting:
                raise ValidationError("batch service is shutting down")
            if not self._capacity.acquire(blocking=False):
                raise ValidationError("batch queue capacity is full")
            run_id = self.id_factory()
            cancellation = threading.Event()
            try:
                self.management.create(
                    management_event_type=ManagementEventType.TRAIN_NETWORK_BATCH_START,
                    sport=request.sport_id,
                    correlation_id=run_id,
                    message="batch training accepted",
                    additional_context={"iterations": number_of_iterations},
                )
                self._active[run_id] = cancellation
                future = self._executor.submit(
                    self._run, run_id, request, number_of_iterations, cancellation
                )
                self._futures.add(future)
            except Exception:
                self._active.pop(run_id, None)
                self._capacity.release()
                raise
        future.add_done_callback(self._forget_future)
        return {
            "networkType": request.sport_id,
            "numberOfBatchIterations": number_of_iterations,
            "batchTrainingRunId": run_id,
        }

    def shutdown(self, timeout_seconds: float = 30.0) -> bool:
        if timeout_seconds <= 0:
            raise ValidationError("shutdown timeout must be positive")
        with self._lock:
            self._accepting = False
            for cancellation in self._active.values():
                cancellation.set()
            futures = tuple(self._futures)
            self._executor.shutdown(wait=False, cancel_futures=False)
        _, pending = wait(futures, timeout=timeout_seconds)
        return not pending

    def _forget_future(self, future: Future[None]) -> None:
        with self._lock:
            self._futures.discard(future)

    def _run(
        self,
        run_id: str,
        request: TrainingRequest,
        iterations: int,
        cancellation: threading.Event,
    ) -> None:
        persisted = below_threshold = failed = cancelled = 0
        started = trained = training_target_not_met = evaluation_below_threshold = 0
        hidden_layer_counts: set[int] = set()
        total_iteration_seconds = 0.0
        try:
            for iteration in range(1, iterations + 1):
                if cancellation.is_set():
                    cancelled += 1
                    logger.warning(
                        "Batch training cancelled before iteration "
                        "run_id=%s sport=%s iteration=%d",
                        run_id,
                        request.sport_id,
                        iteration,
                    )
                    break
                started += 1
                iteration_started = time.monotonic()
                logger.info(
                    "Batch iteration started run_id=%s sport=%s iteration=%d/%d "
                    "remaining_after=%d seed=%d",
                    run_id,
                    request.sport_id,
                    iteration,
                    iterations,
                    iterations - iteration,
                    request.seed + iteration,
                )
                iteration_seconds: float | None = None
                try:
                    outcome = self.training.train(
                        replace(request, seed=request.seed + iteration),
                        cancellation,
                        correlation_id=run_id,
                        batch=True,
                    )
                    if outcome.status == "persisted":
                        persisted += 1
                    elif outcome.status == "completed_below_threshold":
                        below_threshold += 1
                    else:
                        failed += 1
                        logger.error(
                            "Batch training returned unexpected outcome "
                            "run_id=%s sport=%s iteration=%d status=%s",
                            run_id,
                            request.sport_id,
                            iteration,
                            outcome.status,
                        )
                    if outcome.status in ("persisted", "completed_below_threshold"):
                        hidden_layer_counts.add(len(outcome.layout) - 2)
                        if outcome.stop_reason == "max_network_error_reached":
                            trained += 1
                            if outcome.status == "completed_below_threshold":
                                evaluation_below_threshold += 1
                        elif outcome.stop_reason == "maximum_epochs_reached":
                            training_target_not_met += 1
                    iteration_seconds = max(0.0, time.monotonic() - iteration_started)
                    total_iteration_seconds += iteration_seconds
                    elapsed_time_millis = int(iteration_seconds * 1000)
                    self.management.create(
                        management_event_type=ManagementEventType.TRAIN_NETWORK_BATCH_ITERATION_END,
                        sport=request.sport_id,
                        correlation_id=run_id,
                        message=outcome.status,
                        additional_context={
                            "iteration": iteration,
                            "networkId": outcome.model_id,
                            "seed": request.seed + iteration,
                        },
                        elapsed_time_millis=elapsed_time_millis,
                    )
                    logger.info(
                        "Batch iteration completed run_id=%s sport=%s "
                        "iteration=%d/%d remaining=%d status=%s model_id=%s "
                        "epochs=%d stop_reason=%s layout=%s "
                        "iteration_elapsed_ms=%d total_training_elapsed_ms=%d\n%s",
                        run_id,
                        request.sport_id,
                        iteration,
                        iterations,
                        iterations - iteration,
                        outcome.status,
                        outcome.model_id,
                        outcome.epochs,
                        outcome.stop_reason,
                        "x".join(map(str, outcome.layout)),
                        elapsed_time_millis,
                        int(total_iteration_seconds * 1000),
                        _iteration_metrics(outcome, request),
                    )
                except TrainingCancelled:
                    if iteration_seconds is None:
                        iteration_seconds = max(
                            0.0, time.monotonic() - iteration_started
                        )
                        total_iteration_seconds += iteration_seconds
                    cancelled += 1
                    logger.warning(
                        "Batch training cancelled run_id=%s sport=%s iteration=%d "
                        "iteration_elapsed_ms=%d total_training_elapsed_ms=%d",
                        run_id,
                        request.sport_id,
                        iteration,
                        int(iteration_seconds * 1000),
                        int(total_iteration_seconds * 1000),
                        exc_info=True,
                    )
                    break
                except Exception:
                    if iteration_seconds is None:
                        iteration_seconds = max(
                            0.0, time.monotonic() - iteration_started
                        )
                        total_iteration_seconds += iteration_seconds
                    failed += 1
                    logger.exception(
                        "Batch training iteration failed "
                        "run_id=%s sport=%s iteration=%d seed=%d "
                        "iteration_elapsed_ms=%d total_training_elapsed_ms=%d",
                        run_id,
                        request.sport_id,
                        iteration,
                        request.seed + iteration,
                        int(iteration_seconds * 1000),
                        int(total_iteration_seconds * 1000),
                    )
                    self.management.create(
                        management_event_type=ManagementEventType.TRAIN_NETWORK_BATCH_ITERATION_END,
                        sport=request.sport_id,
                        correlation_id=run_id,
                        message="failed",
                        additional_context={"iteration": iteration},
                        elapsed_time_millis=int(iteration_seconds * 1000),
                    )
                    if not self.continue_on_failure:
                        break
        finally:
            average_iteration_millis = (
                total_iteration_seconds * 1000 / started if started else None
            )
            logger.info(
                "Batch training summary run_id=%s sport=%s\n"
                "  Training years:                   %s\n"
                "  Evaluation years:                 %s\n"
                "  Structure algorithm:              %s\n"
                "  Hidden layers (completed):        %s\n"
                "  Updater (optimizer):              %s\n"
                "  Hidden activation:                %s\n"
                "  Output activation:                %s\n"
                "  L2 regularization lambda:         %s\n"
                "  Learning rate:                    %s\n"
                "  Iterations requested:             %d\n"
                "  Iterations started:               %d\n"
                "  Iterations not started:           %d\n"
                "  Networks trained:                 %d\n"
                "  Networks saved:                   %d\n"
                "  Training target not met:          %d\n"
                "  Below evaluation threshold:       %d\n"
                "  Failed:                           %d\n"
                "  Cancelled:                        %d\n"
                "  Total training time (ms):         %d\n"
                "  Average time per iteration (ms):  %s",
                run_id,
                request.sport_id,
                ", ".join(map(str, request.training_years)),
                ", ".join(map(str, request.evaluation_years)),
                request.algorithm if request.algorithm is not None else "not specified",
                (
                    ", ".join(map(str, sorted(hidden_layer_counts)))
                    if hidden_layer_counts
                    else "N/A (no completed iterations)"
                ),
                request.optimizer,
                request.hidden_activation,
                request.output_activation,
                request.l2,
                request.learning_rate,
                iterations,
                started,
                iterations - started,
                trained,
                persisted,
                training_target_not_met,
                evaluation_below_threshold,
                failed,
                cancelled,
                int(total_iteration_seconds * 1000),
                (
                    f"{average_iteration_millis:.2f}"
                    if average_iteration_millis is not None
                    else "N/A (no iterations started)"
                ),
            )
            try:
                self.management.create(
                    management_event_type=ManagementEventType.TRAIN_NETWORK_BATCH_END,
                    sport=request.sport_id,
                    correlation_id=run_id,
                    message="batch training completed",
                    additional_context={
                        "persisted": persisted,
                        "completedBelowThreshold": below_threshold,
                        "failed": failed,
                        "cancelled": cancelled,
                        "requestedIterations": iterations,
                        "startedIterations": started,
                        "notStarted": iterations - started,
                        "trained": trained,
                        "trainingTargetNotMet": training_target_not_met,
                        "evaluationBelowThreshold": evaluation_below_threshold,
                        "totalTrainingElapsedMillis": int(
                            total_iteration_seconds * 1000
                        ),
                        "averageIterationElapsedMillis": average_iteration_millis,
                        "trainingYears": list(request.training_years),
                        "evaluationYears": list(request.evaluation_years),
                        "networkStructureAlgorithm": request.algorithm,
                        "hiddenLayerCounts": sorted(hidden_layer_counts),
                        "updater": request.optimizer,
                        "hiddenActivation": request.hidden_activation,
                        "outputActivation": request.output_activation,
                        "l2": request.l2,
                        "learningRate": request.learning_rate,
                    },
                )
            finally:
                with self._lock:
                    self._active.pop(run_id, None)
                    self._capacity.release()


def _format_metrics(
    label: str, metrics: ClassificationMetrics, classes: tuple[int, ...]
) -> str:
    return (
        f"{label}: error={1 - metrics.recall:.6f} accuracy={metrics.accuracy:.6f} "
        f"precision={metrics.precision:.6f} recall={metrics.recall:.6f} "
        f"f1={metrics.f1:.6f} composite_percent={metrics.composite * 100:.6f}\n"
        f"{label} confusion matrix:\n{confusion_matrix_table(metrics, classes)}"
    )


def _iteration_metrics(outcome: TrainingOutcome, request: TrainingRequest) -> str:
    if outcome.stop_reason == "maximum_epochs_reached":
        decision = (
            "The network did not reach its training error target; "
            "evaluation was not run and the network was not saved."
        )
    elif (
        outcome.stop_reason == "max_network_error_reached"
        and outcome.status == "persisted"
    ):
        decision = (
            "The network reached its training error target and met the "
            "evaluation performance threshold; it was saved."
        )
    elif (
        outcome.stop_reason == "max_network_error_reached"
        and outcome.status == "completed_below_threshold"
    ):
        decision = (
            "The network reached its training error target but did not meet the "
            "evaluation performance threshold; it was not saved."
        )
    else:
        decision = (
            "Training returned an unexpected outcome; see status and stop reason above."
        )
    lines = [decision]
    if outcome.metrics is None:
        lines.extend(
            (
                f"Training loss={outcome.training_loss:.6f} "
                f"max_network_error={request.max_network_error:.6f}",
                _format_metrics("Training", outcome.training_metrics, outcome.classes),
                "Evaluation: not run.",
            )
        )
    else:
        lines.extend(
            (
                f"Evaluation accuracy_percent={outcome.metrics.accuracy * 100:.6f} "
                f"performance_threshold={request.performance_threshold:.6f}",
                _format_metrics("Evaluation", outcome.metrics, outcome.classes),
            )
        )
    return "\n".join(lines)
