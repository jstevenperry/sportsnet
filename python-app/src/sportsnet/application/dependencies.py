"""Typed application dependency bundle for the application factory."""

from collections.abc import Callable, Mapping
from dataclasses import dataclass
from typing import Protocol

from sportsnet.application.management import ManagementService
from sportsnet.application.training import (
    ModelRepository,
    TrainingRequest,
    TrainingService,
)
from sportsnet.domain.models import ModelHandle
from sportsnet.domain.schemas import SportSchema
from sportsnet.sports.registry import SportRegistry


class ApiModelRepository(ModelRepository, Protocol):
    def list_descriptors(self, sport: str) -> tuple[Mapping[str, object], ...]: ...

    def get_descriptor(
        self, model_id: str, sport: str
    ) -> Mapping[str, object] | None: ...

    def load(self, model_id: str, sport: str, schema: SportSchema) -> ModelHandle: ...


class BatchRunner(Protocol):
    def submit(
        self, request: TrainingRequest, *, number_of_iterations: int
    ) -> Mapping[str, object]: ...

    def shutdown(self, timeout_seconds: float = 30.0) -> bool: ...


class TournamentRunner(Protocol):
    def years(self, sport: str) -> tuple[Mapping[str, object], ...]: ...

    def run(self, sport: str, request: Mapping[str, object]) -> str: ...


class EvaluationRunner(Protocol):
    def evaluate(
        self, sport: str, model_id: str, evaluation_year: int
    ) -> Mapping[str, object]: ...


@dataclass(frozen=True, slots=True)
class ApplicationDependencies:
    registry: SportRegistry
    management: ManagementService
    training: TrainingService
    models: ApiModelRepository
    batch: BatchRunner | None = None
    tournaments: TournamentRunner | None = None
    evaluation: EvaluationRunner | None = None
    available_training_years: Callable[[str], tuple[int, ...]] | None = None
