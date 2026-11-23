"""Immutable records shared by sport modules and application services."""

from collections.abc import Mapping
from dataclasses import dataclass

from sportsnet.domain.preprocessing import Preprocessor


@dataclass(frozen=True, slots=True)
class ModelSpec:
    layout: tuple[int, ...]
    output_classes: tuple[int, ...]
    hidden_activation: str
    output_activation: str
    loss: str
    optimizer: str
    initializer: str
    learning_rate: float
    l2: float
    seed: int


@dataclass(frozen=True, slots=True)
class TrainingEpochResult:
    loss: float


@dataclass(frozen=True, slots=True)
class ModelHandle:
    descriptor: Mapping[str, object]
    manifest: Mapping[str, object]
    model: object
    preprocessor: Preprocessor
    model_spec: ModelSpec


@dataclass(frozen=True, slots=True)
class SeasonData:
    sport: str
    year: int
    team_name: str
    features: tuple[float, ...]


@dataclass(frozen=True, slots=True)
class TournamentParticipant:
    year: int
    team_name: str
    victories: int
