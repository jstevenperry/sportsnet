"""Keras 3 backend adapter; framework imports are isolated to this module."""

from dataclasses import dataclass
from pathlib import Path
from typing import TYPE_CHECKING, Any, cast

import numpy as np
from numpy.typing import NDArray

from sportsnet.domain.errors import ValidationError
from sportsnet.domain.models import ModelSpec, TrainingEpochResult

if TYPE_CHECKING:
    import keras


@dataclass(slots=True)
class KerasHandle:
    model: object
    spec: ModelSpec


_ACTIVATIONS = {
    "ELU": "elu",
    "GELU": "gelu",
    "HARDSIGMOID": "hard_sigmoid",
    "HARDTANH": "hard_sigmoid",
    "IDENTITY": "linear",
    "LEAKYRELU": "leaky_relu",
    "MISH": "mish",
    "RELU": "relu",
    "RELU6": "relu6",
    "SELU": "selu",
    "SIGMOID": "sigmoid",
    "SOFTMAX": "softmax",
    "SOFTPLUS": "softplus",
    "SOFTSIGN": "softsign",
    "SWISH": "swish",
    "TANH": "tanh",
}
_INITIALIZERS = {
    "LECUN_NORMAL": "lecun_normal",
    "LECUN_UNIFORM": "lecun_uniform",
    "NORMAL": "random_normal",
    "ONES": "ones",
    "RELU": "he_normal",
    "RELU_UNIFORM": "he_uniform",
    "UNIFORM": "random_uniform",
    "XAVIER": "glorot_normal",
    "XAVIER_UNIFORM": "glorot_uniform",
    "ZERO": "zeros",
}


class KerasBackend:
    def __init__(self, *, deterministic: bool = True) -> None:
        self.deterministic = deterministic

    @property
    def identity(self) -> dict[str, str]:
        import keras
        import tensorflow as tf

        return {
            "backend": "keras",
            "keras": keras.__version__,
            "tensorflow": tf.__version__,
        }

    def build(self, spec: ModelSpec) -> KerasHandle:
        import keras
        import tensorflow as tf

        self._validate_spec(spec)
        keras.utils.set_random_seed(spec.seed)
        if self.deterministic:
            try:
                tf.config.experimental.enable_op_determinism()
            except (AttributeError, RuntimeError):
                pass

        regularizer = keras.regularizers.L2(spec.l2) if spec.l2 > 0 else None
        initializer = _INITIALIZERS[spec.initializer]
        layers: list[object] = [keras.Input(shape=(spec.layout[0],))]
        for width in spec.layout[1:-1]:
            layers.append(
                keras.layers.Dense(
                    width,
                    activation=_ACTIVATIONS[spec.hidden_activation],
                    kernel_initializer=initializer,
                    kernel_regularizer=regularizer,
                )
            )
        layers.append(
            keras.layers.Dense(
                len(spec.output_classes),
                activation="softmax",
                kernel_initializer=initializer,
                kernel_regularizer=regularizer,
            )
        )
        model = keras.Sequential(layers)
        optimizer = _make_optimizer(keras, spec)
        model.compile(
            optimizer=optimizer,
            loss="sparse_categorical_crossentropy",
        )
        return KerasHandle(model=model, spec=spec)

    def train_one_epoch(
        self, handle: object, features: NDArray[Any], labels: NDArray[Any]
    ) -> TrainingEpochResult:
        typed_handle = _require_handle(handle)
        model = self._model(typed_handle)
        x, y = _validate_training_arrays(features, labels, typed_handle.spec)
        history = model.fit(
            x,
            y,
            epochs=1,
            batch_size=len(x),
            shuffle=False,
            verbose=0,
        )
        values = history.history.get("loss")
        if not values or not np.isfinite(values[-1]):
            raise ValidationError("Keras returned a missing or non-finite epoch loss")
        return TrainingEpochResult(float(values[-1]))

    def predict_scores(self, handle: object, features: NDArray[Any]) -> NDArray[Any]:
        typed_handle = _require_handle(handle)
        model = self._model(typed_handle)
        values = np.asarray(features, dtype=np.float32)
        if (
            values.ndim != 2
            or values.shape[1] != typed_handle.spec.layout[0]
            or not np.isfinite(values).all()
        ):
            raise ValidationError("prediction features have invalid shape or values")
        predictions = np.asarray(model.predict(values, verbose=0), dtype=np.float64)
        expected = (values.shape[0], len(typed_handle.spec.output_classes))
        if predictions.shape != expected or not np.isfinite(predictions).all():
            raise ValidationError("Keras returned invalid prediction shape or values")
        return predictions

    def save(self, handle: object, path: Path) -> None:
        if path.suffix != ".keras":
            raise ValidationError("Keras artifacts must use the .keras extension")
        self._model(_require_handle(handle)).save(path)

    def load(self, path: Path, expected_spec: ModelSpec) -> KerasHandle:
        import keras

        self._validate_spec(expected_spec)
        if not path.is_file() or path.suffix != ".keras":
            raise ValidationError(f"invalid Keras artifact path: {path.name}")
        model = keras.models.load_model(path)
        expected_units = list(expected_spec.layout[1:])
        actual_units = [
            int(layer.units)
            for layer in model.layers
            if isinstance(layer, keras.layers.Dense)
        ]
        if (
            tuple(model.input_shape[1:]) != (expected_spec.layout[0],)
            or tuple(model.output_shape[1:]) != (len(expected_spec.output_classes),)
            or actual_units != expected_units
        ):
            raise ValidationError(
                "loaded Keras model does not match expected architecture"
            )
        return KerasHandle(model=model, spec=expected_spec)

    @staticmethod
    def _model(handle: KerasHandle) -> "keras.Model":
        import keras

        return cast(keras.Model, handle.model)

    @staticmethod
    def _validate_spec(spec: ModelSpec) -> None:
        if len(spec.layout) < 3 or any(width <= 0 for width in spec.layout):
            raise ValidationError(
                "model layout must contain positive input, hidden, output widths"
            )
        if spec.layout[-1] != len(spec.output_classes):
            raise ValidationError("model output width does not match class count")
        if len(set(spec.output_classes)) != len(spec.output_classes):
            raise ValidationError("model output classes must be unique")
        if spec.output_activation != "SOFTMAX":
            raise ValidationError(
                "sparse categorical classification requires softmax output"
            )
        if spec.loss != "SPARSE_MCXENT":
            raise ValidationError("only SPARSE_MCXENT is supported for classification")
        if spec.hidden_activation not in _ACTIVATIONS:
            raise ValidationError(
                f"unsupported hidden activation: {spec.hidden_activation}"
            )
        if spec.initializer not in _INITIALIZERS:
            raise ValidationError(f"unsupported initializer: {spec.initializer}")
        if spec.optimizer not in {
            "ADA_DELTA",
            "ADA_GRAD",
            "ADAM",
            "ADA_MAX",
            "AMS_GRAD",
            "NADAM",
            "RMS_PROP",
            "SGD",
        }:
            raise ValidationError(f"unsupported optimizer: {spec.optimizer}")
        if (
            not np.isfinite(spec.learning_rate)
            or spec.learning_rate <= 0
            or not np.isfinite(spec.l2)
            or spec.l2 < 0
        ):
            raise ValidationError("learning rate must be positive and L2 non-negative")


def _make_optimizer(keras: Any, spec: ModelSpec) -> object:
    optimizers = keras.optimizers
    optimizer_types = {
        "ADA_DELTA": "Adadelta",
        "ADA_GRAD": "Adagrad",
        "ADAM": "Adam",
        "ADA_MAX": "Adamax",
        "AMS_GRAD": "Adam",
        "NADAM": "Nadam",
        "RMS_PROP": "RMSprop",
        "SGD": "SGD",
    }
    optimizer_type = getattr(optimizers, optimizer_types[spec.optimizer])
    kwargs: dict[str, object] = {"learning_rate": spec.learning_rate}
    if spec.optimizer == "AMS_GRAD":
        kwargs["amsgrad"] = True
    if spec.optimizer in {"ADAM", "ADA_MAX", "AMS_GRAD", "NADAM"}:
        kwargs["beta_1"] = 0.9
        kwargs["beta_2"] = 0.999
    return optimizer_type(**kwargs)


def _validate_training_arrays(
    features: NDArray[Any],
    labels: NDArray[Any],
    spec: ModelSpec,
) -> tuple[NDArray[Any], NDArray[Any]]:
    x = np.asarray(features, dtype=np.float32)
    y = np.asarray(labels)
    if (
        x.ndim != 2
        or x.shape[0] == 0
        or x.shape[1] != spec.layout[0]
        or y.ndim != 1
        or y.shape[0] != x.shape[0]
        or not np.issubdtype(y.dtype, np.integer)
        or np.any(y < 0)
        or np.any(y >= len(spec.output_classes))
        or not np.isfinite(x).all()
    ):
        raise ValidationError("training arrays do not match model input/class schema")
    return x, y.astype(np.int64)


def _require_handle(handle: object) -> KerasHandle:
    if not isinstance(handle, KerasHandle):
        raise ValidationError("backend handle was not created by this Keras adapter")
    return handle
