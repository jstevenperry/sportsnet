"""Path-safe production CSV dataset provider."""

from collections.abc import Mapping
from pathlib import Path

from sportsnet.domain.datasets import TrainingDataset, assemble_dataset
from sportsnet.domain.errors import ValidationError
from sportsnet.domain.models import SeasonData, TournamentParticipant
from sportsnet.domain.schemas import SportSchema
from sportsnet.sports.mlb.csv_repository import (
    load_mlb_participants,
    load_mlb_season,
)
from sportsnet.sports.ncaabb.csv_repository import (
    load_aliases,
    load_ncaabb_participants,
    load_ncaabb_season,
)
from sportsnet.sports.registry import SportRegistry


class FilesystemDatasetProvider:
    def __init__(
        self,
        resource_root: Path,
        registry: SportRegistry,
        *,
        require_ncaabb_alias_file: bool = True,
    ) -> None:
        self.resource_root = resource_root.resolve()
        self.registry = registry
        self.require_ncaabb_alias_file = require_ncaabb_alias_file

    def build(self, sport: str, years: tuple[int, ...]) -> TrainingDataset:
        schema = self.registry.get(sport)
        if not years or len(set(years)) != len(years):
            raise ValidationError("dataset years must be non-empty and unique")
        unsupported = set(years).difference(schema.training_years)
        if unsupported:
            raise ValidationError(f"unsupported training years: {sorted(unsupported)}")
        aliases: Mapping[str, str] | None = None
        sport_root = self._sport_root(schema)
        if schema.sport_id.casefold() == "ncaabb":
            alias_path = sport_root / "data" / "SeasonData-Alias.csv"
            if alias_path.exists():
                aliases = load_aliases(alias_path)
            elif self.require_ncaabb_alias_file:
                raise ValidationError(
                    f"required alias file is missing: {alias_path.name}"
                )

        seasons: dict[int, Mapping[str, SeasonData]] = {}
        participants: list[TournamentParticipant] = []
        for year in years:
            directory = sport_root / "data" / str(year)
            self._assert_beneath_root(directory)
            if schema.sport_id.casefold() == "ncaabb":
                year_participants = load_ncaabb_participants(
                    directory / "tournament_participant_all.csv", year, aliases
                )
                season = load_ncaabb_season(
                    directory,
                    year,
                    aliases,
                    participant_names={item.team_name for item in year_participants},
                )
                seasons[year] = season
                participants.extend(
                    participant
                    for participant in year_participants
                    if participant.team_name in season
                )
            elif schema.sport_id.casefold() == "mlb":
                seasons[year] = load_mlb_season(directory, year)
                participants.extend(
                    load_mlb_participants(directory / "playoff-results.csv", year)
                )
            else:
                raise ValidationError(
                    f"no filesystem CSV adapter is configured for {schema.sport_id}"
                )
        return assemble_dataset(schema, seasons, participants, years)

    def available_training_years(self, sport: str) -> tuple[int, ...]:
        schema = self.registry.get(sport)
        root = self._sport_root(schema)
        sport_name = schema.sport_id.casefold()
        available: list[int] = []
        for year in schema.training_years:
            directory = root / "data" / str(year)
            required: tuple[str, ...]
            if sport_name == "ncaabb":
                required = (
                    "BasicSeasonData.csv",
                    "OpponentSeasonData.csv",
                    "AdvancedSeasonData.csv",
                    "AdvancedOpponentSeasonData.csv",
                    "tournament_participant_all.csv",
                )
            elif sport_name == "mlb":
                required = (
                    f"{year}-Team-Batting.csv",
                    f"{year}-Team-Pitching.csv",
                    f"{year}-Team-Fielding.csv",
                    "playoff-results.csv",
                )
            else:
                raise ValidationError(
                    f"no filesystem CSV adapter is configured for {schema.sport_id}"
                )
            if not directory.is_dir() or any(
                not (directory / filename).is_file() for filename in required
            ):
                continue
            self.build(schema.sport_id, (year,))
            available.append(year)
        return tuple(available)

    def load_tournament_season(self, sport: str, year: int) -> Mapping[str, SeasonData]:
        schema = self.registry.get(sport)
        if year not in schema.season_years:
            raise ValidationError(f"unsupported tournament season year: {year}")
        sport_root = self._sport_root(schema)
        directory = sport_root / "data" / str(year)
        self._assert_beneath_root(directory)
        if schema.sport_id.casefold() == "ncaabb":
            alias_path = sport_root / "data" / "SeasonData-Alias.csv"
            if not alias_path.is_file() and self.require_ncaabb_alias_file:
                raise ValidationError(
                    f"required alias file is missing: {alias_path.name}"
                )
            aliases = load_aliases(alias_path) if alias_path.is_file() else None
            return load_ncaabb_season(directory, year, aliases)
        if schema.sport_id.casefold() == "mlb":
            return load_mlb_season(directory, year)
        raise ValidationError(f"no tournament CSV adapter for {schema.sport_id}")

    def _sport_root(self, schema: SportSchema) -> Path:
        root = self.resource_root / schema.sport_id.upper()
        self._assert_beneath_root(root)
        return root

    def _assert_beneath_root(self, path: Path) -> None:
        resolved = path.resolve()
        if (
            resolved != self.resource_root
            and self.resource_root not in resolved.parents
        ):
            raise ValidationError("resource path escapes configured read root")
