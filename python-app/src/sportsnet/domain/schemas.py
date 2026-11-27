"""Immutable sport schema declarations used by all generic workflows."""

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class SportSchema:
    sport_id: str
    display_name: str
    feature_schema_version: str
    feature_names: tuple[str, ...]
    output_classes: tuple[int, ...]
    season_years: tuple[int, ...]
    training_years: tuple[int, ...]
    smoothing_multiplicities: tuple[int, ...] | None = None

    def __post_init__(self) -> None:
        if not self.sport_id or not self.feature_schema_version:
            raise ValueError("sport identifier and schema version are required")
        if not self.feature_names or len(set(self.feature_names)) != len(
            self.feature_names
        ):
            raise ValueError("feature names must be non-empty and unique")
        if not self.output_classes or len(set(self.output_classes)) != len(
            self.output_classes
        ):
            raise ValueError("output classes must be non-empty and unique")
        if tuple(sorted(set(self.season_years))) != self.season_years:
            raise ValueError("season years must be unique and ascending")
        if tuple(sorted(set(self.training_years))) != self.training_years:
            raise ValueError("training years must be unique and ascending")
        if not set(self.training_years).issubset(self.season_years):
            raise ValueError("training years must also have season data")
        if self.smoothing_multiplicities is not None and (
            len(self.smoothing_multiplicities) != len(self.output_classes)
            or any(value < 1 for value in self.smoothing_multiplicities)
        ):
            raise ValueError("smoothing multiplicities must match output classes")

    @property
    def input_width(self) -> int:
        return len(self.feature_names)

    @property
    def output_width(self) -> int:
        return len(self.output_classes)
