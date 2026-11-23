"""Explicit registry for production and test sport schemas."""

from collections.abc import Iterable

from sportsnet.domain.errors import ValidationError
from sportsnet.domain.schemas import SportSchema


class SportRegistry:
    def __init__(self, modules: Iterable[SportSchema]) -> None:
        self._modules: dict[str, SportSchema] = {}
        for module in modules:
            key = module.sport_id.casefold()
            if key in self._modules:
                raise ValidationError(f"duplicate sport identifier: {module.sport_id}")
            self._validate(module)
            self._modules[key] = module

    @staticmethod
    def _validate(module: SportSchema) -> None:
        if not module.display_name.strip():
            raise ValidationError("sport display name is required")
        if not module.feature_names or any(
            not name.strip() for name in module.feature_names
        ):
            raise ValidationError(f"{module.sport_id}: feature names are required")
        if len(set(module.feature_names)) != len(module.feature_names):
            raise ValidationError(f"{module.sport_id}: feature names must be unique")
        if not module.output_classes or len(set(module.output_classes)) != len(
            module.output_classes
        ):
            raise ValidationError(f"{module.sport_id}: output classes must be unique")

    def get(self, sport_id: str) -> SportSchema:
        try:
            return self._modules[sport_id.casefold()]
        except KeyError as error:
            raise ValidationError(f"unknown sport: {sport_id}") from error

    def all(self) -> tuple[SportSchema, ...]:
        return tuple(self._modules[key] for key in sorted(self._modules))
