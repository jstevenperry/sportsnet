from dataclasses import replace
from pathlib import Path

import numpy as np
import pytest

from sportsnet.adapters.keras_backend import KerasBackend, ModelSpec
from sportsnet.domain.errors import ValidationError

pytestmark = pytest.mark.keras


def _spec() -> ModelSpec:
    return ModelSpec(
        layout=(2, 4, 3),
        output_classes=(0, 1, 2),
        hidden_activation="RELU",
        output_activation="SOFTMAX",
        loss="SPARSE_MCXENT",
        optimizer="ADAM",
        initializer="XAVIER_UNIFORM",
        learning_rate=0.01,
        l2=0.0,
        seed=42,
    )


def test_keras_one_epoch_predictions_and_fresh_instance_roundtrip(
    tmp_path: Path,
) -> None:
    backend = KerasBackend(deterministic=True)
    spec = _spec()
    handle = backend.build(spec)
    features = np.asarray([[0.0, 1.0], [1.0, 0.0], [0.5, 0.5]], dtype=np.float32)
    labels = np.asarray([0, 1, 2], dtype=np.int64)
    result = backend.train_one_epoch(handle, features, labels)
    assert np.isfinite(result.loss)
    predictions = backend.predict_scores(handle, features)
    assert predictions.shape == (3, 3)
    assert np.isfinite(predictions).all()

    artifact = tmp_path / "model.keras"
    backend.save(handle, artifact)
    loaded = KerasBackend(deterministic=True).load(artifact, spec)
    np.testing.assert_allclose(
        backend.predict_scores(handle, features),
        KerasBackend(deterministic=True).predict_scores(loaded, features),
        rtol=1e-5,
        atol=1e-6,
    )


def test_keras_backend_rejects_incompatible_classification_options() -> None:
    backend = KerasBackend()
    invalid = replace(_spec(), output_activation="RELU")
    with pytest.raises(ValidationError, match="softmax"):
        backend.build(invalid)
