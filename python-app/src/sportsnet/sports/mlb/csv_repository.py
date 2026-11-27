"""CSV parser and deterministic merge for MLB season statistics."""

import csv
import math
from collections.abc import Mapping
from pathlib import Path

from sportsnet.domain.errors import ValidationError
from sportsnet.domain.models import SeasonData, TournamentParticipant
from sportsnet.sports.csv_values import parse_statistic
from sportsnet.sports.mlb.module import MLB

_DEFAULT_FILES = (
    ("Team-Batting.csv", 29),
    ("Team-Pitching.csv", 36),
    ("Team-Fielding.csv", 19),
)
_FIELD_NAMES = (
    (
        "numBat",
        "batAge",
        "rPerG",
        "gamesBat",
        "plateAppear",
        "atBat",
        "runs",
        "hits",
        "doubles",
        "triples",
        "hr",
        "rbis",
        "sb",
        "caughtStealing",
        "bb",
        "so",
        "ba",
        "obp",
        "slg",
        "ops",
        "ops_plus",
        "tb",
        "gdp",
        "hbp",
        "sh",
        "sf",
        "ibb",
        "lob",
    ),
    (
        "numPitchers",
        "pAge",
        "rallPerG",
        "win",
        "loss",
        "wlpct",
        "era",
        "gamesPitch",
        "gs",
        "gf",
        "cg",
        "tSho",
        "cSho",
        "sv",
        "ip",
        "ha",
        "ra",
        "er",
        "hra",
        "walks",
        "intWalks",
        "soP",
        "hbpP",
        "bk",
        "wp",
        "bf",
        "era_plus",
        "fip",
        "whip",
        "h9",
        "hr9",
        "bb9",
        "so9",
        "soPerW",
        "lobP",
    ),
    (
        "numFld",
        "rapg",
        "defEff",
        "inn",
        "ch",
        "po",
        "assist",
        "errors",
        "dp",
        "fldPct",
        "rTot",
        "rTotYr",
        "rdrs",
        "rdrsYr",
        "rgood",
    ),
)
_FIELD_COLUMNS = (
    tuple(range(1, 29)),
    tuple(range(1, 36)),
    (1, 2, 3, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18),
)
_ALIASES = {
    "Cleveland Indians": "Cleveland Guardians",
    "Los Angeles Angels of Anaheim": "Los Angeles Angels",
}


def canonicalize_team(name: str, aliases: Mapping[str, str] | None = None) -> str:
    normalized = name.strip().replace("*", "").replace(".", "").strip()
    mapping = _ALIASES | dict(aliases or {})
    seen: set[str] = set()
    while normalized in mapping:
        if normalized in seen:
            raise ValidationError(f"cyclic MLB team alias: {normalized}")
        seen.add(normalized)
        normalized = mapping[normalized]
    if not normalized:
        raise ValidationError("team name cannot be empty")
    return normalized


def load_mlb_season(
    directory: Path,
    year: int,
    aliases: Mapping[str, str] | None = None,
    *,
    batting_file: str | None = None,
    pitching_file: str | None = None,
    fielding_file: str | None = None,
) -> dict[str, SeasonData]:
    if year not in MLB.season_years:
        raise ValidationError(f"unsupported MLB season year: {year}")
    filenames = (
        batting_file or f"{year}-{_DEFAULT_FILES[0][0]}",
        pitching_file or f"{year}-{_DEFAULT_FILES[1][0]}",
        fielding_file or f"{year}-{_DEFAULT_FILES[2][0]}",
    )
    joined: dict[str, dict[str, float]] = {}
    for filename, (_, width), names, columns in zip(
        filenames,
        _DEFAULT_FILES,
        _FIELD_NAMES,
        _FIELD_COLUMNS,
        strict=True,
    ):
        path = directory / filename
        rows = _read_rows(path, width, 1)
        parsed: dict[str, dict[str, float]] = {}
        for row_number, row in enumerate(rows, start=2):
            raw_name = row[0].strip()
            if raw_name == "League Average":
                continue
            team = canonicalize_team(raw_name, aliases)
            if team in parsed:
                raise ValidationError(
                    f"{path}: row {row_number}: duplicate team {team}"
                )
            parsed[team] = _parse_fields(path, row_number, row, names, columns)
        if not parsed:
            raise ValidationError(f"{path}: no team rows")
        if not joined:
            joined = parsed
        else:
            if set(parsed) != set(joined):
                raise ValidationError(f"{path}: incomplete MLB team join")
            for team, values in parsed.items():
                joined[team].update(values)
    result: dict[str, SeasonData] = {}
    for team, values in sorted(joined.items()):
        features = tuple(values[name] for name in MLB.feature_names)
        if len(features) != MLB.input_width or not all(map(math.isfinite, features)):
            raise ValidationError(f"{directory}: invalid MLB feature vector for {team}")
        result[team] = SeasonData("mlb", year, team, features)
    return result


def load_mlb_participants(
    path: Path,
    year: int,
    aliases: Mapping[str, str] | None = None,
) -> tuple[TournamentParticipant, ...]:
    if year not in MLB.training_years:
        raise ValidationError(f"unsupported MLB participant year: {year}")
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
        if victories not in MLB.output_classes:
            raise ValidationError(
                f"{path} for year {year}: row {row_number}, column 2: "
                f"label {victories} is outside declared classes"
            )
        seen.add(team)
        participants.append(TournamentParticipant(year, team, victories))
    if not participants:
        raise ValidationError(f"{path} for year {year}: no participant rows")
    return tuple(sorted(participants, key=lambda item: item.team_name))


def _read_rows(
    path: Path,
    width: int,
    header_count: int,
) -> list[list[str]]:
    try:
        with path.open(encoding="utf-8-sig", newline="") as stream:
            rows = list(csv.reader(stream))
    except OSError as error:
        raise ValidationError(f"cannot read {path}: {error}") from error
    if len(rows) < header_count:
        raise ValidationError(f"{path}: missing header")
    data = rows[header_count:]
    for row_number, row in enumerate(data, start=header_count + 1):
        if not row or not any(field.strip() for field in row):
            continue
        if len(row) != width:
            raise ValidationError(
                f"{path}: row {row_number}: expected {width} fields, got {len(row)}"
            )
    return [row for row in data if row and any(field.strip() for field in row)]


def _parse_fields(
    path: Path,
    row_number: int,
    row: list[str],
    names: tuple[str, ...],
    columns: tuple[int, ...],
) -> dict[str, float]:
    values = {}
    for name, index in zip(names, columns, strict=True):
        values[name] = parse_statistic(row[index], path, row_number, index + 1)
    return values
