"""Deterministic classification metrics independent of the ML backend."""

from collections.abc import Sequence
from dataclasses import dataclass
from typing import Any

import numpy as np
from numpy.typing import NDArray

from sportsnet.domain.errors import ValidationError


@dataclass(frozen=True, slots=True)
class ClassificationMetrics:
    accuracy: float
    precision: float
    recall: float
    f1: float
    composite: float
    confusion_matrix: tuple[tuple[int, ...], ...]
    predictions: tuple[int, ...]


def calculate_metrics(
    actual: Sequence[int] | NDArray[Any],
    scores: Sequence[Sequence[float]] | NDArray[Any],
    classes: Sequence[int],
) -> ClassificationMetrics:
    ordered_classes = tuple(classes)
    if not ordered_classes or len(set(ordered_classes)) != len(ordered_classes):
        raise ValidationError("classes must be a non-empty unique ordered sequence")
    labels = np.asarray(actual)
    values = np.asarray(scores, dtype=np.float64)
    if (
        labels.ndim != 1
        or values.ndim != 2
        or values.shape
        != (
            len(labels),
            len(ordered_classes),
        )
    ):
        raise ValidationError("labels and score matrix shapes do not match classes")
    if len(labels) == 0:
        raise ValidationError("metrics require at least one example")
    if not np.issubdtype(labels.dtype, np.integer):
        raise ValidationError("labels must be integers")
    if not np.isfinite(values).all():
        raise ValidationError("scores must be finite")
    if (
        np.any(values < 0)
        or np.any(values > 1)
        or not np.allclose(values.sum(axis=1), 1.0, atol=1e-5)
    ):
        raise ValidationError("scores must be valid class probabilities")
    class_to_index = {label: index for index, label in enumerate(ordered_classes)}
    if any(int(label) not in class_to_index for label in labels):
        raise ValidationError("label is not in the declared class order")
    predicted_indexes = np.argmax(values, axis=1)
    predictions = tuple(ordered_classes[int(index)] for index in predicted_indexes)
    matrix = [[0 for _ in ordered_classes] for _ in ordered_classes]
    for truth, predicted in zip(labels, predictions, strict=True):
        matrix[class_to_index[int(truth)]][class_to_index[predicted]] += 1
    total = int(labels.size)
    accuracy = (
        sum(matrix[index][index] for index in range(len(ordered_classes))) / total
    )
    precision_values: list[float] = []
    recall_values: list[float] = []
    f1_values: list[float] = []
    for index in range(len(ordered_classes)):
        true_positive = matrix[index][index]
        false_positive = (
            sum(matrix[row][index] for row in range(len(ordered_classes)))
            - true_positive
        )
        false_negative = sum(matrix[index]) - true_positive
        precision = (
            true_positive / (true_positive + false_positive)
            if true_positive + false_positive
            else 0.0
        )
        recall = (
            true_positive / (true_positive + false_negative)
            if true_positive + false_negative
            else 0.0
        )
        f1 = (
            2 * precision * recall / (precision + recall) if precision + recall else 0.0
        )
        precision_values.append(precision)
        recall_values.append(recall)
        f1_values.append(f1)
    precision = sum(precision_values) / len(ordered_classes)
    recall = sum(recall_values) / len(ordered_classes)
    f1 = sum(f1_values) / len(ordered_classes)
    composite = (accuracy + precision + recall + f1) / 4
    return ClassificationMetrics(
        accuracy=accuracy,
        precision=precision,
        recall=recall,
        f1=f1,
        composite=composite,
        confusion_matrix=tuple(tuple(row) for row in matrix),
        predictions=predictions,
    )


def confusion_matrix_csv(metrics: ClassificationMetrics, classes: Sequence[int]) -> str:
    rows = ["Actual\\Predicted," + ",".join(map(str, classes))]
    rows.extend(
        f"{label}," + ",".join(map(str, row))
        for label, row in zip(classes, metrics.confusion_matrix, strict=True)
    )
    return "\n".join(rows) + "\n"


def confusion_matrix_table(
    metrics: ClassificationMetrics, classes: Sequence[int]
) -> str:
    labels = tuple(map(str, classes))
    row_width = max(len("Actual"), *(len(label) for label in labels))
    column_widths = tuple(
        max(len(label), *(len(str(row[index])) for row in metrics.confusion_matrix))
        for index, label in enumerate(labels)
    )
    header = f"{'':>{row_width}} | " + " | ".join(
        f"{label:>{width}}" for label, width in zip(labels, column_widths, strict=True)
    )
    separator = (
        "-" * row_width + "-+-" + "-+-".join("-" * width for width in column_widths)
    )
    rows = [
        f"{label:>{row_width}} | "
        + " | ".join(
            f"{count:>{width}}"
            for count, width in zip(values, column_widths, strict=True)
        )
        for label, values in zip(labels, metrics.confusion_matrix, strict=True)
    ]
    return "Actual \\ Predicted\n" + "\n".join((header, separator, *rows))


def evaluation_report_fields(
    metrics: ClassificationMetrics, classes: Sequence[int]
) -> dict[str, object]:
    return {
        "networkAccuracy": metrics.accuracy,
        "networkPrecision": metrics.precision,
        "networkRecall": metrics.recall,
        "networkF1Score": metrics.f1,
        "realNetworkAccuracy": metrics.composite,
        "confusionMatrixCsv": confusion_matrix_csv(metrics, classes),
    }


def training_report_fields(
    metrics: ClassificationMetrics, classes: Sequence[int]
) -> dict[str, object]:
    return {
        "trainingAccuracy": metrics.accuracy,
        "trainingPrecision": metrics.precision,
        "trainingRecall": metrics.recall,
        "trainingF1Score": metrics.f1,
        "realTrainingAccuracy": metrics.composite,
        "trainingConfusionMatrixCsv": confusion_matrix_csv(metrics, classes),
    }


def should_persist(*, early_stopped: bool, accuracy: float, threshold: float) -> bool:
    if not 0 <= threshold <= 100:
        raise ValidationError("performance threshold must be in [0, 100]")
    if not np.isfinite(accuracy) or not 0 <= accuracy <= 1:
        raise ValidationError("accuracy must be finite and in [0, 1]")
    return early_stopped and accuracy >= threshold / 100


def early_stop_reached(recall: float, max_network_error: float) -> bool:
    if not 0 <= recall <= 1 or not 0 <= max_network_error <= 1:
        raise ValidationError("recall and max network error must be in [0, 1]")
    return 1 - recall < max_network_error
