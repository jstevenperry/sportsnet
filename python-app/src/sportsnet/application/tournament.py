"""Persisted-model tournament inference with plugin-owned scoring parameters."""

import csv
import io
import time
import uuid
from collections.abc import Mapping, Sequence
from dataclasses import dataclass
from typing import Any, Protocol

import numpy as np
from numpy.typing import NDArray

from sportsnet.application.management import ManagementService
from sportsnet.domain.errors import NotFoundError, RepositoryError, ValidationError
from sportsnet.domain.events import ManagementEventType
from sportsnet.domain.models import ModelHandle, SeasonData
from sportsnet.domain.schemas import SportSchema
from sportsnet.sports.registry import SportRegistry


class TournamentData(Protocol):
    def load_tournament_season(
        self, sport: str, year: int
    ) -> Mapping[str, SeasonData]: ...


class TournamentModels(Protocol):
    def load(self, model_id: str, sport: str, schema: SportSchema) -> ModelHandle: ...


class TournamentBackend(Protocol):
    def predict_scores(self, model: object, features: NDArray[Any]) -> NDArray[Any]: ...


@dataclass(frozen=True, slots=True)
class TournamentPlugin:
    sport_id: str
    tournament_year: int
    description: str
    output_type: str


class TournamentService:
    def __init__(
        self,
        *,
        registry: SportRegistry,
        data: TournamentData,
        models: TournamentModels,
        backend: TournamentBackend,
        management: ManagementService,
        plugins: Sequence[TournamentPlugin],
        distance_factor: float = 0.0,
    ) -> None:
        if not np.isfinite(distance_factor):
            raise ValidationError("tournament distance factor must be finite")
        self.registry = registry
        self.data = data
        self.models = models
        self.backend = backend
        self.management = management
        self.plugins = tuple(plugins)
        self.distance_factor = distance_factor
        if any(
            plugin.output_type not in {"BRACKET", "TEAM_RANKING"} for plugin in plugins
        ):
            raise ValidationError("unsupported tournament plugin output type")
        keys = [
            (plugin.sport_id.casefold(), plugin.tournament_year) for plugin in plugins
        ]
        if len(set(keys)) != len(keys):
            raise ValidationError("duplicate tournament plugin")

    def years(self, sport: str) -> tuple[Mapping[str, object], ...]:
        return tuple(
            {
                "tournamentYear": plugin.tournament_year,
                "pluginDescription": plugin.description,
                "outputType": plugin.output_type,
            }
            for plugin in sorted(
                self.plugins,
                key=lambda item: (item.tournament_year, item.description),
            )
            if plugin.sport_id.casefold() == sport.casefold()
        )

    def run(self, sport: str, request: Mapping[str, object]) -> str:
        try:
            year_value = request["tournamentYear"]
            raw_ids = request["networkIds"]
        except KeyError as error:
            raise ValidationError(
                "simulation requires networkIds and tournamentYear"
            ) from error
        if not isinstance(year_value, int) or isinstance(year_value, bool):
            raise ValidationError("tournamentYear must be an integer")
        year = year_value
        plugin = next(
            (
                item
                for item in self.plugins
                if item.sport_id.casefold() == sport.casefold()
                and item.tournament_year == year
            ),
            None,
        )
        if plugin is None:
            raise ValidationError(f"no tournament plugin for year {year}")
        if not isinstance(raw_ids, (list, tuple)) or not raw_ids:
            raise ValidationError("networkIds must be a non-empty list")
        model_ids = tuple(sorted({str(model_id) for model_id in raw_ids}))
        if any(not model_id.strip() for model_id in model_ids):
            raise ValidationError("network IDs must not be empty")
        schema = self.registry.get(sport)
        correlation_id = str(uuid.uuid4())
        started = time.monotonic()
        self.management.create(
            management_event_type=ManagementEventType.RUN_SIMULATION_START,
            sport=schema.sport_id,
            correlation_id=correlation_id,
            message=f"simulation started for {year}",
        )
        try:
            # Validate all inputs before running model inference.
            season = self.data.load_tournament_season(schema.sport_id, year)
            teams = _validate_season(season, schema, year)
            aggregated = {team: 0.0 for team in teams}
            valid_models = 0
            for model_id in model_ids:
                try:
                    loaded = self.models.load(model_id, schema.sport_id, schema)
                except (NotFoundError, RepositoryError) as error:
                    self._warn(correlation_id, schema.sport_id, model_id, str(error))
                    continue
                model = loaded.model
                preprocessor = loaded.preprocessor
                manifest = loaded.manifest
                if (
                    str(manifest.get("sport", "")).casefold()
                    != schema.sport_id.casefold()
                ):
                    self._warn(
                        correlation_id, schema.sport_id, model_id, "sport mismatch"
                    )
                    continue
                matrix = np.asarray([teams[name].features for name in sorted(teams)])
                prepared = preprocessor.transform(matrix.tolist())
                predictions = np.asarray(
                    self.backend.predict_scores(model, prepared), dtype=np.float64
                )
                expected_shape = (len(teams), schema.output_width)
                if (
                    predictions.shape != expected_shape
                    or not np.isfinite(predictions).all()
                ):
                    self._warn(
                        correlation_id,
                        schema.sport_id,
                        model_id,
                        "invalid prediction shape or non-finite score",
                    )
                    continue
                if (
                    np.any(predictions < 0)
                    or np.any(predictions > 1)
                    or not np.allclose(predictions.sum(axis=1), 1.0, atol=1e-5)
                ):
                    self._warn(
                        correlation_id,
                        schema.sport_id,
                        model_id,
                        "predictions are not valid class probabilities",
                    )
                    continue
                for index, team in enumerate(sorted(teams)):
                    distance = sum(
                        float(probability) * (class_index + self.distance_factor)
                        for class_index, probability in enumerate(predictions[index])
                    )
                    aggregated[team] += distance
                valid_models += 1
            if valid_models == 0:
                raise ValidationError("no valid persisted models remain for simulation")
            output = _ranking_csv(aggregated)
        except Exception:
            try:
                self.management.create(
                    management_event_type=ManagementEventType.EXCEPTION,
                    sport=schema.sport_id,
                    correlation_id=correlation_id,
                    message=f"simulation failed for {year}",
                )
            finally:
                raise
        self.management.create(
            management_event_type=ManagementEventType.RUN_SIMULATION_END,
            sport=schema.sport_id,
            correlation_id=correlation_id,
            message=f"simulation completed for {year}",
            elapsed_time_millis=max(0, int((time.monotonic() - started) * 1000)),
            additional_context={"modelsUsed": valid_models},
        )
        return output

    def _warn(
        self, correlation_id: str, sport: str, model_id: str, reason: str
    ) -> None:
        self.management.create(
            management_event_type=ManagementEventType.EXCEPTION,
            sport=sport,
            correlation_id=correlation_id,
            message="tournament model skipped",
            additional_context={"networkId": model_id, "reason": reason},
        )


def _validate_season(
    season: Mapping[str, SeasonData], schema: SportSchema, year: int
) -> dict[str, SeasonData]:
    if not season:
        raise ValidationError(f"tournament season {year} has no team rows")
    result: dict[str, SeasonData] = {}
    for team, data in season.items():
        if (
            not team.strip()
            or data.team_name != team
            or data.year != year
            or data.sport.casefold() != schema.sport_id.casefold()
            or len(data.features) != schema.input_width
            or not np.isfinite(np.asarray(data.features, dtype=np.float64)).all()
        ):
            raise ValidationError(f"invalid tournament season row for {team!r}")
        if team in result:
            raise ValidationError(f"duplicate tournament team key: {team}")
        result[team] = data
    return dict(sorted(result.items()))


def _ranking_csv(scores: Mapping[str, float]) -> str:
    stream = io.StringIO(newline="")
    writer = csv.writer(stream, lineterminator="\n")
    writer.writerow(("Rank", "Team Name", "Tournament Score"))
    ranked = sorted(scores.items(), key=lambda item: (-item[1], item[0]))
    for rank, (team, score) in enumerate(ranked, start=1):
        if not np.isfinite(score):
            raise ValidationError(f"non-finite tournament score for {team}")
        writer.writerow((rank, team, f"{score:.4f}"))
    return stream.getvalue()
