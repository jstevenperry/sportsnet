from __future__ import annotations

import numpy as np
import pytest

from sportsnet.domain.errors import ValidationError
from sportsnet.domain.metrics import (
    calculate_metrics,
    evaluation_report_fields,
    should_persist,
    training_report_fields,
)
from sportsnet.domain.preprocessing import Preprocessor
from sportsnet.sports import MLB, NCAABB


def test_production_sport_schemas_and_explicit_year_manifests() -> None:
    assert len(NCAABB.feature_names) == 64
    assert NCAABB.feature_names[21] == "opponent_points_per_game_legacy_duplicate"
    assert NCAABB.feature_names[21] != NCAABB.feature_names[4]
    assert NCAABB.output_classes == tuple(range(7))
    assert NCAABB.season_years == tuple(range(2009, 2019)) + tuple(range(2020, 2025))
    assert NCAABB.training_years == tuple(range(2010, 2019)) + tuple(range(2020, 2024))

    assert len(MLB.feature_names) == 78
    assert MLB.output_classes == tuple(range(6))
    assert MLB.season_years == tuple(range(2009, 2024))
    assert MLB.training_years == tuple(range(2010, 2023))


def test_metrics_use_full_class_order_and_lowest_index_ties() -> None:
    metrics = calculate_metrics(
        actual=[0, 1, 2, 2],
        scores=[
            [0.8, 0.2, 0.0],
            [0.5, 0.5, 0.0],
            [0.1, 0.2, 0.7],
            [0.2, 0.7, 0.1],
        ],
        classes=(0, 1, 2),
    )
    assert metrics.confusion_matrix == (
        (1, 0, 0),
        (1, 0, 0),
        (0, 1, 1),
    )
    assert metrics.accuracy == pytest.approx(0.5)
    assert metrics.precision == pytest.approx(0.5)
    assert metrics.recall == pytest.approx(0.5)
    assert metrics.f1 == pytest.approx(4 / 9)
    assert metrics.composite == pytest.approx(35 / 72)


def test_preprocessor_fits_training_only_and_handles_constant_columns() -> None:
    training = np.asarray([[1.0, 5.0], [3.0, 5.0]])
    fitted = Preprocessor.fit(training, "NORMALIZER_STANDARDIZE")
    transformed = fitted.transform(np.asarray([[5.0, 100.0]]))
    assert transformed.tolist() == [[3.0, 0.0]]
    assert fitted.constant_columns == (False, True)


def test_persistence_requires_both_early_stop_and_accuracy_threshold() -> None:
    assert should_persist(early_stopped=True, accuracy=0.7, threshold=70.0)
    assert should_persist(early_stopped=True, accuracy=29 / 100, threshold=29.0)
    assert not should_persist(early_stopped=False, accuracy=1.0, threshold=70.0)
    assert not should_persist(early_stopped=True, accuracy=0.6999, threshold=70.0)
    assert should_persist(early_stopped=True, accuracy=0.0, threshold=0.0)
    assert should_persist(early_stopped=True, accuracy=1.0, threshold=100.0)
    for accuracy in (float("nan"), float("inf"), -0.01, 1.01):
        with pytest.raises(ValidationError, match="accuracy"):
            should_persist(early_stopped=True, accuracy=accuracy, threshold=70.0)
    for threshold in (float("nan"), float("inf"), -0.01, 100.01):
        with pytest.raises(ValidationError, match="threshold"):
            should_persist(early_stopped=True, accuracy=0.7, threshold=threshold)


def test_report_fields_carry_every_metric_and_the_confusion_matrix() -> None:
    metrics = calculate_metrics([0, 1, 1], [[0.9, 0.1], [0.2, 0.8], [0.7, 0.3]], (0, 1))
    evaluation = evaluation_report_fields(metrics, (0, 1))
    assert evaluation["networkAccuracy"] == metrics.accuracy
    assert evaluation["realNetworkAccuracy"] == metrics.composite
    assert evaluation["confusionMatrixCsv"] == ("Actual\\Predicted,0,1\n0,1,0\n1,1,1\n")
    training = training_report_fields(metrics, (0, 1))
    assert training["trainingAccuracy"] == metrics.accuracy
    assert training["trainingPrecision"] == metrics.precision
    assert training["trainingRecall"] == metrics.recall
    assert training["trainingF1Score"] == metrics.f1
    assert training["realTrainingAccuracy"] == metrics.composite
    assert training["trainingConfusionMatrixCsv"] == evaluation["confusionMatrixCsv"]
