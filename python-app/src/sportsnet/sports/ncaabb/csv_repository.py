"""CSV parser and deterministic join for NCAA season statistics."""

import csv
import logging
import math
from collections import defaultdict
from collections.abc import Mapping, Set
from pathlib import Path

from sportsnet.domain.errors import ValidationError
from sportsnet.domain.models import SeasonData, TournamentParticipant
from sportsnet.sports.csv_values import parse_statistic
from sportsnet.sports.ncaabb.module import NCAABB

logger = logging.getLogger(__name__)

_FILES = (
    ("BasicSeasonData.csv", 38),
    ("OpponentSeasonData.csv", 38),
    ("AdvancedSeasonData.csv", 34),
    ("AdvancedOpponentSeasonData.csv", 34),
)
_SEPARATOR_COLUMNS = (8, 11, 14, 17, 20)
_FEATURE_COLUMNS = (
    (5, 6, 7, 18, 19, *range(22, 38)),
    tuple(range(22, 38)),
    tuple(range(21, 34)),
    tuple(range(21, 34)),
)


def canonicalize_team(name: str, aliases: Mapping[str, str] | None = None) -> str:
    normalized = name.strip().replace("*", "").strip().rstrip(".").strip()
    if not normalized:
        raise ValidationError("team name cannot be empty")
    alias_map = aliases or {}
    seen: set[str] = set()
    while normalized in alias_map:
        if normalized in seen:
            raise ValidationError(f"cyclic team alias: {normalized}")
        seen.add(normalized)
        normalized = alias_map[normalized].strip()
    return normalized


def load_ncaabb_participants(
    path: Path,
    year: int,
    aliases: Mapping[str, str] | None = None,
) -> tuple[TournamentParticipant, ...]:
    if year not in NCAABB.training_years:
        raise ValidationError(f"unsupported NCAA participant year: {year}")
    return _load_participants(path, year, NCAABB.output_classes, aliases)


def load_aliases(path: Path) -> dict[str, str]:
    neighbors: dict[str, set[str]] = defaultdict(set)
    preferred: set[str] = set()
    try:
        with path.open(encoding="utf-8-sig", newline="") as stream:
            rows = csv.reader(stream)
            header_seen = False
            for line_number, row in enumerate(rows, start=1):
                if not row or not row[0].strip() or row[0].lstrip().startswith("#"):
                    continue
                if not header_seen:
                    header_seen = True
                    continue
                if len(row) not in (1, 2):
                    raise ValidationError(
                        f"{path}: row {line_number}: expected one or two alias fields"
                    )
                source = canonicalize_team(row[0])
                neighbors[source]
                if len(row) == 1 or not row[1].strip():
                    preferred.add(source)
                    continue
                target = canonicalize_team(row[1])
                neighbors[source].add(target)
                neighbors[target].add(source)
            if not header_seen:
                raise ValidationError(f"{path}: missing alias header")
    except OSError as error:
        raise ValidationError(f"cannot read aliases {path}: {error}") from error

    aliases: dict[str, str] = {}
    remaining = set(neighbors)
    while remaining:
        component: set[str] = set()
        pending = [min(remaining)]
        while pending:
            name = pending.pop()
            if name in component:
                continue
            component.add(name)
            pending.extend(neighbors[name] - component)
        remaining.difference_update(component)
        canonical = min(component & preferred or component)
        aliases.update((name, canonical) for name in component if name != canonical)
    return aliases


def load_ncaabb_season(
    directory: Path,
    year: int,
    aliases: Mapping[str, str] | None = None,
    *,
    participant_names: Set[str] | None = None,
) -> dict[str, SeasonData]:
    if year not in NCAABB.season_years:
        raise ValidationError(f"unsupported NCAA season year: {year}")
    maps: list[dict[str, tuple[float, ...]]] = []
    for (filename, width), columns in zip(_FILES, _FEATURE_COLUMNS, strict=True):
        path = directory / filename
        maps.append(
            _read_stats(
                path,
                width,
                header_count=2,
                team_column=1,
                feature_columns=columns,
                team_aliases=aliases,
                participant_names=participant_names,
                structural_columns=_SEPARATOR_COLUMNS,
            )
        )
    names = set(maps[0])
    all_names = (
        set(participant_names)
        if participant_names is not None
        else names.union(*(set(records) for records in maps[1:]))
    )
    incomplete = all_names - names.intersection(*(set(records) for records in maps[1:]))
    if incomplete:
        missing_by_team = [
            f"{team!r}: missing from "
            + ", ".join(
                filename
                for (filename, _), records in zip(_FILES, maps, strict=True)
                if team not in records
            )
            for team in sorted(incomplete)
        ]
        message = (
            f"{directory}: incomplete NCAA season joins for year {year}; "
            f"possible missing aliases or missing data rows: "
            f"{'; '.join(missing_by_team)}. Check the team names and add matching "
            f"aliases to {directory.parent / 'SeasonData-Alias.csv'} if appropriate, "
            "or restore missing rows in the season files."
        )
        if participant_names is None:
            raise ValidationError(message)
        logger.warning(
            "%s Unmatched tournament participants are excluded for this year.",
            message,
        )
        names.difference_update(incomplete)
    result: dict[str, SeasonData] = {}
    for team in sorted(names):
        basic_features = maps[0][team]
        features = (
            basic_features
            + (basic_features[4],)
            + maps[1][team]
            + maps[2][team]
            + maps[3][team]
        )
        if len(features) != NCAABB.input_width or not all(
            math.isfinite(value) for value in features
        ):
            raise ValidationError(
                f"{directory}: invalid NCAA feature vector for {team}"
            )
        result[team] = SeasonData("NCAABB", year, team, features)
    return result


def _read_stats(
    path: Path,
    width: int,
    *,
    header_count: int,
    team_column: int,
    feature_columns: tuple[int, ...],
    team_aliases: Mapping[str, str] | None,
    participant_names: Set[str] | None,
    structural_columns: tuple[int, ...] = (),
) -> dict[str, tuple[float, ...]]:
    try:
        with path.open(encoding="utf-8-sig", newline="") as stream:
            rows = list(csv.reader(stream))
    except OSError as error:
        raise ValidationError(f"cannot read {path}: {error}") from error
    if len(rows) < header_count:
        raise ValidationError(f"{path}: expected {header_count} header rows")
    result: dict[str, tuple[float, ...]] = {}
    for row_number, row in enumerate(rows[header_count:], start=header_count + 1):
        if not row or not any(field.strip() for field in row):
            continue
        if participant_names is not None and len(row) > team_column:
            team = canonicalize_team(row[team_column], team_aliases)
            if team not in participant_names:
                continue
        if len(row) != width:
            raise ValidationError(
                f"{path}: row {row_number}: expected {width} fields, got {len(row)}"
            )
        for column in structural_columns:
            if row[column].strip():
                raise ValidationError(
                    f"{path}: row {row_number}, column {column + 1}: "
                    "expected blank structural field"
                )
        team = canonicalize_team(row[team_column], team_aliases)
        if team in result:
            raise ValidationError(f"{path}: row {row_number}: duplicate team {team}")
        values: list[float] = []
        for column in feature_columns:
            values.append(parse_statistic(row[column], path, row_number, column + 1))
        result[team] = tuple(values)
    if not result and participant_names is None:
        raise ValidationError(f"{path}: no season rows")
    return result


def _load_participants(
    path: Path,
    year: int,
    classes: tuple[int, ...],
    aliases: Mapping[str, str] | None,
) -> tuple[TournamentParticipant, ...]:
    try:
        with path.open(encoding="utf-8-sig", newline="") as stream:
            rows = list(csv.reader(stream))
    except OSError as error:
        raise ValidationError(f"cannot read {path} for year {year}: {error}") from error
    if not rows:
        raise ValidationError(f"{path} for year {year}: missing header")
    participants: list[TournamentParticipant] = []
    seen: set[str] = set()
    for row_number, row in enumerate(rows[1:], start=2):
        if not row or not any(field.strip() for field in row):
            continue
        if len(row) != 2:
            raise ValidationError(
                f"{path} for year {year}: row {row_number}: expected 2 fields, "
                f"got {len(row)}"
            )
        team = canonicalize_team(row[0], aliases)
        if team in seen:
            raise ValidationError(
                f"{path} for year {year}: row {row_number}: "
                f"duplicate participant {team}"
            )
        try:
            victories = int(row[1].strip())
        except ValueError as error:
            raise ValidationError(
                f"{path} for year {year}: row {row_number}, column 2: "
                f"expected integer label, got {row[1]!r}"
            ) from error
        if victories not in classes:
            raise ValidationError(
                f"{path} for year {year}: row {row_number}, column 2: "
                f"label {victories} is outside declared classes"
            )
        seen.add(team)
        participants.append(TournamentParticipant(year, team, victories))
    if not participants:
        raise ValidationError(f"{path} for year {year}: no participant rows")
    return tuple(sorted(participants, key=lambda item: item.team_name))
