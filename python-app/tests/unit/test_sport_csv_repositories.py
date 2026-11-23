from __future__ import annotations

import csv
import logging
from pathlib import Path

import pytest

from sportsnet.adapters.filesystem_resources import FilesystemDatasetProvider
from sportsnet.domain.datasets import assemble_dataset
from sportsnet.domain.errors import ValidationError
from sportsnet.sports.mlb.csv_repository import (
    load_mlb_participants,
    load_mlb_season,
)
from sportsnet.sports.ncaabb.csv_repository import (
    load_aliases,
    load_ncaabb_participants,
    load_ncaabb_season,
)
from sportsnet.sports.ncaabb.module import NCAABB
from sportsnet.sports.registry import SportRegistry


def _write(path: Path, rows: list[list[str]]) -> None:
    with path.open("w", encoding="utf-8", newline="") as stream:
        csv.writer(stream).writerows(rows)


def _ncaa_row(width: int, team: str) -> list[str]:
    row = ["0"] * width
    row[1] = team
    for column in (8, 11, 14, 17, 20):
        row[column] = ""
    return row


def _ncaabb_headers(filename: str, width: int) -> list[list[str]]:
    return [[f"{filename} group"] * width, ["arbitrary name"] * width]


_MLB_HEADERS = tuple(["arbitrary name"] * width for width in (29, 36, 19))


def test_ncaabb_joins_four_inputs_and_keeps_legacy_duplicate_feature(
    tmp_path: Path,
) -> None:
    basic = _ncaa_row(38, "Synthetic State")
    basic[5:8] = ["0.75", "10.5", "2.5"]
    basic[19] = "71"
    opponent = _ncaa_row(38, "Synthetic State")
    opponent[22:38] = [str(value) for value in range(1, 17)]
    advanced = _ncaa_row(34, "Synthetic State")
    advanced[21:34] = [str(value) for value in range(21, 34)]
    advanced_opponent = _ncaa_row(34, "Synthetic State")
    advanced_opponent[21:34] = [str(value) for value in range(31, 44)]
    _write(
        tmp_path / "BasicSeasonData.csv",
        _ncaabb_headers("BasicSeasonData.csv", 38) + [basic],
    )
    _write(
        tmp_path / "OpponentSeasonData.csv",
        _ncaabb_headers("OpponentSeasonData.csv", 38) + [opponent],
    )
    _write(
        tmp_path / "AdvancedSeasonData.csv",
        _ncaabb_headers("AdvancedSeasonData.csv", 34) + [advanced],
    )
    _write(
        tmp_path / "AdvancedOpponentSeasonData.csv",
        _ncaabb_headers("AdvancedOpponentSeasonData.csv", 34) + [advanced_opponent],
    )

    records = load_ncaabb_season(tmp_path, 2024)
    record = records["Synthetic State"]
    assert len(record.features) == 64
    assert record.features[0:5] == (0.75, 10.5, 2.5, 0.0, 71.0)
    assert record.features[21] == record.features[4]
    assert record.features[22:38] == tuple(float(value) for value in range(1, 17))
    assert record.features[38:51] == tuple(float(value) for value in range(21, 34))
    assert record.features[51:64] == tuple(float(value) for value in range(31, 44))


def test_ncaabb_blank_statistics_become_zero_in_every_input_file(
    tmp_path: Path,
) -> None:
    for filename, width in (
        ("BasicSeasonData.csv", 38),
        ("OpponentSeasonData.csv", 38),
        ("AdvancedSeasonData.csv", 34),
        ("AdvancedOpponentSeasonData.csv", 34),
    ):
        row = [" "] * width
        row[1] = "Synthetic State"
        _write(tmp_path / filename, _ncaabb_headers(filename, width) + [row])

    record = load_ncaabb_season(tmp_path, 2023)["Synthetic State"]
    assert record.features == (0.0,) * 64


def test_ncaabb_blank_opponent_fg_in_row_34_is_zero(tmp_path: Path) -> None:
    for filename, width in (
        ("BasicSeasonData.csv", 38),
        ("OpponentSeasonData.csv", 38),
        ("AdvancedSeasonData.csv", 34),
        ("AdvancedOpponentSeasonData.csv", 34),
    ):
        rows = [_ncaa_row(width, f"Team {index}") for index in range(32)]
        if filename == "OpponentSeasonData.csv":
            rows[31][22] = ""
        _write(tmp_path / filename, _ncaabb_headers(filename, width) + rows)
    record = load_ncaabb_season(tmp_path, 2023)["Team 31"]
    assert record.features[22] == 0.0


@pytest.mark.parametrize("value", ["not-a-number", "%", "NaN", "inf"])
def test_ncaabb_rejects_invalid_nonblank_statistics(tmp_path: Path, value: str) -> None:
    for filename, width in (
        ("BasicSeasonData.csv", 38),
        ("OpponentSeasonData.csv", 38),
        ("AdvancedSeasonData.csv", 34),
        ("AdvancedOpponentSeasonData.csv", 34),
    ):
        row = _ncaa_row(width, "Synthetic State")
        if filename == "OpponentSeasonData.csv":
            row[22] = value
        _write(tmp_path / filename, _ncaabb_headers(filename, width) + [row])

    with pytest.raises(
        ValidationError, match=r"OpponentSeasonData.csv: row 3, column 23"
    ):
        load_ncaabb_season(tmp_path, 2023)


def test_ncaabb_rejects_missing_join_rows_with_source_context(tmp_path: Path) -> None:
    for filename, width in (
        ("BasicSeasonData.csv", 38),
        ("OpponentSeasonData.csv", 38),
        ("AdvancedSeasonData.csv", 34),
        ("AdvancedOpponentSeasonData.csv", 34),
    ):
        header_rows = _ncaabb_headers(filename, width)
        rows = header_rows + (
            [_ncaa_row(width, "Only Basic")]
            if filename == "BasicSeasonData.csv"
            else [_ncaa_row(width, "Different Team")]
        )
        _write(tmp_path / filename, rows)

    with pytest.raises(ValidationError) as error:
        load_ncaabb_season(tmp_path, 2024)
    message = str(error.value)
    assert str(tmp_path) in message
    assert "year 2024" in message
    assert "possible missing aliases or missing data rows" in message
    assert "'Different Team': missing from BasicSeasonData.csv" in message
    assert (
        "'Only Basic': missing from OpponentSeasonData.csv, "
        "AdvancedSeasonData.csv, AdvancedOpponentSeasonData.csv"
    ) in message
    assert str(tmp_path.parent / "SeasonData-Alias.csv") in message


def test_training_excludes_unmatched_names_only_for_affected_year(
    tmp_path: Path, caplog: pytest.LogCaptureFixture
) -> None:
    root = tmp_path / "NCAABB" / "data"
    root.mkdir(parents=True)
    _write(root / "SeasonData-Alias.csv", [["source", "alias"]])
    for year in (2022, 2023):
        directory = root / str(year)
        directory.mkdir()
        for filename, width in (
            ("BasicSeasonData.csv", 38),
            ("OpponentSeasonData.csv", 38),
            ("AdvancedSeasonData.csv", 34),
            ("AdvancedOpponentSeasonData.csv", 34),
        ):
            team = (
                "Alternate"
                if year == 2022 and filename == "OpponentSeasonData.csv"
                else "Beta"
            )
            rows = [_ncaa_row(width, "Alpha"), _ncaa_row(width, team)]
            _write(directory / filename, _ncaabb_headers(filename, width) + rows)
        _write(
            directory / "tournament_participant_all.csv",
            [
                ["team_name", "number_of_victories"],
                ["Alpha", "0"],
                ["Beta", "1"],
                *([["Unknown", "2"]] if year == 2022 else []),
            ],
        )
    provider = FilesystemDatasetProvider(tmp_path, SportRegistry((NCAABB,)))
    with caplog.at_level(logging.WARNING):
        dataset = provider.build("NCAABB", (2022, 2023))
    assert dataset.labels == (0, 0, 1)
    output = caplog.text
    assert "year 2022" in output
    assert "'Beta': missing from OpponentSeasonData.csv" in output
    assert "'Alternate': missing" not in output
    assert "'Unknown': missing from BasicSeasonData.csv" in output
    assert "Unmatched tournament participants are excluded for this year" in output
    assert "possible missing aliases or missing data rows" in output
    assert str(root / "SeasonData-Alias.csv") in output
    assert "year 2023" not in output
    with pytest.raises(ValidationError, match="incomplete NCAA season joins"):
        provider.load_tournament_season("NCAABB", 2022)
    _write(root / "SeasonData-Alias.csv", [["source", "alias"], ["Beta", "Alternate"]])
    caplog.clear()
    with caplog.at_level(logging.WARNING):
        repaired = provider.build("NCAABB", (2022, 2023))
    assert repaired.labels == (0, 1, 0, 1)
    assert "'Beta': missing" not in caplog.text


def test_training_rejects_dataset_with_no_matched_participants(
    tmp_path: Path, caplog: pytest.LogCaptureFixture
) -> None:
    root = tmp_path / "NCAABB" / "data"
    directory = root / "2022"
    directory.mkdir(parents=True)
    _write(root / "SeasonData-Alias.csv", [["source", "alias"]])
    for filename, width in (
        ("BasicSeasonData.csv", 38),
        ("OpponentSeasonData.csv", 38),
        ("AdvancedSeasonData.csv", 34),
        ("AdvancedOpponentSeasonData.csv", 34),
    ):
        _write(
            directory / filename,
            _ncaabb_headers(filename, width) + [_ncaa_row(width, "Alpha")],
        )
    _write(
        directory / "tournament_participant_all.csv",
        [["team_name", "number_of_victories"], ["Unknown", "0"]],
    )
    provider = FilesystemDatasetProvider(tmp_path, SportRegistry((NCAABB,)))
    with (
        caplog.at_level(logging.WARNING),
        pytest.raises(ValidationError, match="dataset has no participant examples"),
    ):
        provider.build("NCAABB", (2022,))
    assert "'Unknown': missing from" in caplog.text
    assert "year 2022" in caplog.text


def test_training_ignores_nonparticipant_rows_before_parsing_statistics(
    tmp_path: Path, caplog: pytest.LogCaptureFixture
) -> None:
    root = tmp_path / "NCAABB" / "data"
    directory = root / "2022"
    directory.mkdir(parents=True)
    _write(root / "SeasonData-Alias.csv", [["source", "alias"], ["Alpha", "A"]])
    for filename, width in (
        ("BasicSeasonData.csv", 38),
        ("OpponentSeasonData.csv", 38),
        ("AdvancedSeasonData.csv", 34),
        ("AdvancedOpponentSeasonData.csv", 34),
    ):
        participant = _ncaa_row(width, "A")
        nonparticipant = _ncaa_row(width, "Other")
        nonparticipant[22] = "not-a-number"
        rows = [participant, nonparticipant]
        if filename == "OpponentSeasonData.csv":
            rows.append(_ncaa_row(width, "Only Opponent"))
        _write(directory / filename, _ncaabb_headers(filename, width) + rows)
    _write(
        directory / "tournament_participant_all.csv",
        [["team_name", "number_of_victories"], ["Alpha", "1"]],
    )
    provider = FilesystemDatasetProvider(tmp_path, SportRegistry((NCAABB,)))
    with caplog.at_level(logging.WARNING):
        dataset = provider.build("NCAABB", (2022,))
    assert dataset.labels == (1,)
    assert len(dataset.features) == 1
    assert not caplog.records
    with pytest.raises(ValidationError, match="expected numeric value"):
        provider.load_tournament_season("NCAABB", 2022)


def test_ncaabb_aliases_join_season_files_and_participants(tmp_path: Path) -> None:
    alias_path = tmp_path / "aliases.csv"
    _write(
        alias_path,
        [
            ["source", "alias"],
            ["Synthetic State"],
            ["Synthetic State", "Synthetic St"],
            ["Synthetic State NCAA", "Synthetic St"],
            ["Synthetic State NCAA", "Synthetic State"],
        ],
    )
    aliases = load_aliases(alias_path)
    for (filename, width), team in zip(
        (
            ("BasicSeasonData.csv", 38),
            ("OpponentSeasonData.csv", 38),
            ("AdvancedSeasonData.csv", 34),
            ("AdvancedOpponentSeasonData.csv", 34),
        ),
        ("Synthetic State", "Synthetic St", "Synthetic State NCAA", "Synthetic St"),
        strict=True,
    ):
        _write(
            tmp_path / filename,
            _ncaabb_headers(filename, width) + [_ncaa_row(width, team)],
        )
    participants_path = tmp_path / "participants.csv"
    _write(
        participants_path, [["team_name", "number_of_victories"], ["Synthetic St", "2"]]
    )
    season = load_ncaabb_season(tmp_path, 2023, aliases)
    participants = load_ncaabb_participants(participants_path, 2023, aliases)
    dataset = assemble_dataset(NCAABB, {2023: season}, participants, (2023,))
    assert len(season) == 1
    assert participants[0].team_name in season
    assert dataset.labels == (2,)


def test_ncaabb_rejects_distinct_season_rows_with_shared_alias(tmp_path: Path) -> None:
    alias_path = tmp_path / "aliases.csv"
    _write(alias_path, [["source", "alias"], ["Alpha", "Shared"], ["Beta", "Shared"]])
    aliases = load_aliases(alias_path)
    _write(
        tmp_path / "BasicSeasonData.csv",
        _ncaabb_headers("BasicSeasonData.csv", 38)
        + [_ncaa_row(38, "Alpha"), _ncaa_row(38, "Beta")],
    )
    with pytest.raises(ValidationError, match="duplicate team"):
        load_ncaabb_season(tmp_path, 2023, aliases)


def test_mlb_merge_uses_errors_column_and_synthetic_alias(tmp_path: Path) -> None:
    batting = ["0"] * 29
    batting[0] = "Legacy Club"
    pitching = ["0"] * 36
    pitching[0] = "Current Club"
    fielding = ["0"] * 19
    fielding[0] = "Current Club"
    fielding[1] = "57"
    fielding[11] = "86"
    _write(tmp_path / "batting.csv", [_MLB_HEADERS[0], batting])
    _write(tmp_path / "pitching.csv", [_MLB_HEADERS[1], pitching])
    _write(tmp_path / "fielding.csv", [_MLB_HEADERS[2], fielding])
    records = load_mlb_season(
        tmp_path,
        2022,
        aliases={"Legacy Club": "Current Club"},
        batting_file="batting.csv",
        pitching_file="pitching.csv",
        fielding_file="fielding.csv",
    )
    assert list(records) == ["Current Club"]
    assert len(records["Current Club"].features) == 78
    assert records["Current Club"].features[63:78] == (
        (57.0,) + (0.0,) * 6 + (86.0,) + (0.0,) * 7
    )


def test_mlb_blank_statistics_become_zero_in_every_input_file(tmp_path: Path) -> None:
    for filename, header in zip(
        ("batting.csv", "pitching.csv", "fielding.csv"), _MLB_HEADERS, strict=True
    ):
        row = [" "] * len(header)
        row[0] = "Synthetic Club"
        _write(tmp_path / filename, [header, row])

    record = load_mlb_season(
        tmp_path,
        2022,
        batting_file="batting.csv",
        pitching_file="pitching.csv",
        fielding_file="fielding.csv",
    )["Synthetic Club"]
    assert record.features == (0.0,) * 78


@pytest.mark.parametrize("value", ["not-a-number", "%", "NaN", "inf"])
def test_mlb_rejects_invalid_nonblank_statistics(tmp_path: Path, value: str) -> None:
    row = ["0"] * 29
    row[0], row[1] = "Synthetic Club", value
    _write(tmp_path / "batting.csv", [_MLB_HEADERS[0], row])
    with pytest.raises(ValidationError, match=r"batting.csv: row 2, column 2"):
        load_mlb_season(tmp_path, 2022, batting_file="batting.csv")


@pytest.mark.parametrize("sport", ["ncaabb", "mlb"])
def test_blank_participant_labels_are_rejected(tmp_path: Path, sport: str) -> None:
    path = tmp_path / "participants.csv"
    if sport == "ncaabb":
        _write(path, [["team_name", "number_of_victories"], ["Synthetic", " "]])
        load = load_ncaabb_participants
    else:
        _write(path, [["Team_name", "Distance"], ["Synthetic", " "]])
        load = load_mlb_participants
    with pytest.raises(ValidationError, match="expected integer label"):
        load(path, 2022)


def test_ncaabb_accepts_changed_header_names_before_parsing_rows(
    tmp_path: Path,
) -> None:
    for filename, width in (
        ("BasicSeasonData.csv", 38),
        ("OpponentSeasonData.csv", 38),
        ("AdvancedSeasonData.csv", 34),
        ("AdvancedOpponentSeasonData.csv", 34),
    ):
        _write(
            tmp_path / filename,
            _ncaabb_headers(filename, width) + [_ncaa_row(width, "Alpha")],
        )
    headers = _ncaabb_headers("BasicSeasonData.csv", 38)
    headers[0] = ["A short header"]
    headers[1][22] = "Changed"
    _write(
        tmp_path / "BasicSeasonData.csv",
        headers + [_ncaa_row(38, "Alpha")],
    )
    opponent_headers = _ncaabb_headers("AdvancedOpponentSeasonData.csv", 34)
    opponent_headers[0][21] = "School Advanced"
    _write(
        tmp_path / "AdvancedOpponentSeasonData.csv",
        opponent_headers + [_ncaa_row(34, "Alpha")],
    )
    assert "Alpha" in load_ncaabb_season(tmp_path, 2024)


def test_mlb_accepts_changed_header_names_before_parsing_rows(
    tmp_path: Path,
) -> None:
    for (filename, width), source_header in zip(
        (("batting.csv", 29), ("pitching.csv", 36), ("fielding.csv", 19)),
        _MLB_HEADERS,
        strict=True,
    ):
        header = list(source_header)
        if filename == "batting.csv":
            header[3] = "Changed"
        _write(tmp_path / filename, [header, ["0"] * width])
    assert "0" in load_mlb_season(
        tmp_path,
        2022,
        batting_file="batting.csv",
        pitching_file="pitching.csv",
        fielding_file="fielding.csv",
    )


@pytest.mark.parametrize("sport", ["ncaabb", "mlb"])
def test_participant_headers_are_not_validated(tmp_path: Path, sport: str) -> None:
    path = tmp_path / "participants.csv"
    _write(path, [["anything", "anything else"], ["Synthetic", "1"]])
    load = load_ncaabb_participants if sport == "ncaabb" else load_mlb_participants
    participants = load(path, 2022)
    assert participants[0].team_name == "Synthetic"
    assert participants[0].victories == 1
