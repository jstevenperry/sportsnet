"""Shared parsing for numeric season statistics."""

import math
from pathlib import Path

from sportsnet.domain.errors import ValidationError


def parse_statistic(value: str, path: Path, row: int, column: int) -> float:
    if not value.strip():
        return 0.0
    raw = value.strip().replace("%", "").replace(",", "")
    try:
        result = float(raw)
    except ValueError as error:
        raise ValidationError(
            f"{path}: row {row}, column {column}: expected numeric value, got {value!r}"
        ) from error
    if not math.isfinite(result):
        raise ValidationError(
            f"{path}: row {row}, column {column}: value must be finite"
        )
    return result
