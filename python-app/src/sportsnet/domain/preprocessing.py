"""Training-fitted feature transforms shared by training and inference."""

from dataclasses import dataclass

import numpy as np
from numpy.typing import ArrayLike, NDArray

from sportsnet.domain.errors import ValidationError


@dataclass(frozen=True, slots=True)
class Preprocessor:
    method: str
    offsets: tuple[float, ...]
    scales: tuple[float, ...]
    constant_columns: tuple[bool, ...]

    @classmethod
    def fit(cls, values: ArrayLike, method: str) -> "Preprocessor":
        data = _validate_matrix(values)
        if method not in {
            "NORMALIZER_STANDARDIZE",
            "NORMALIZER_ZERO_TO_ONE",
            "NORMALIZER_NEGATIVE_ONE_TO_ONE",
        }:
            raise ValidationError(f"unsupported normalizer: {method}")
        if method == "NORMALIZER_STANDARDIZE":
            offsets = data.mean(axis=0)
            scales = data.std(axis=0, ddof=0)
        else:
            offsets = data.min(axis=0)
            scales = data.max(axis=0) - offsets
        constants = scales == 0
        safe_scales = np.where(constants, 1.0, scales)
        return cls(
            method=method,
            offsets=tuple(float(value) for value in offsets),
            scales=tuple(float(value) for value in safe_scales),
            constant_columns=tuple(bool(value) for value in constants),
        )

    def transform(self, values: ArrayLike) -> NDArray[np.float64]:
        data = _validate_matrix(values)
        if data.shape[1] != len(self.offsets):
            raise ValidationError(
                f"expected {len(self.offsets)} features, got {data.shape[1]}"
            )
        result = (data - np.asarray(self.offsets)) / np.asarray(self.scales)
        if self.method == "NORMALIZER_NEGATIVE_ONE_TO_ONE":
            result = 2 * result - 1
        result = np.where(np.asarray(self.constant_columns), 0.0, result)
        if not np.isfinite(result).all():
            raise ValidationError("transformed features must be finite")
        return result


def _validate_matrix(values: ArrayLike) -> NDArray[np.float64]:
    try:
        data = np.asarray(values, dtype=np.float64)
    except (TypeError, ValueError) as error:
        raise ValidationError(
            "features must be a rectangular numeric matrix"
        ) from error
    if data.ndim != 2 or data.shape[0] == 0 or data.shape[1] == 0:
        raise ValidationError("features must have shape (n, features), with n > 0")
    if not np.isfinite(data).all():
        raise ValidationError("features must be finite")
    return data
