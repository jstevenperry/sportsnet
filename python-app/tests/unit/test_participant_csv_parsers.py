import csv

import pytest

from sportsnet.domain.errors import ValidationError
from sportsnet.sports.mlb.csv_repository import load_mlb_participants
from sportsnet.sports.ncaabb.csv_repository import load_ncaabb_participants


def _write(path, rows):
    with path.open("w", encoding="utf-8", newline="") as stream:
        csv.writer(stream).writerows(rows)


def test_ncaabb_participants_trim_names_and_preserve_integer_labels(tmp_path) -> None:
    path = tmp_path / "participants.csv"
    _write(path, [["team_name", "number_of_victories"], [" Alpha* ", "2"]])
    assert load_ncaabb_participants(path, 2023)[0].team_name == "Alpha"
    assert load_ncaabb_participants(path, 2023)[0].victories == 2


def test_mlb_participants_apply_alias_and_reject_bad_label(tmp_path) -> None:
    path = tmp_path / "participants.csv"
    aliases = {"Legacy Club": "Current Club"}
    _write(path, [["Team_name", "Distance"], ["Legacy Club", "3"]])
    assert load_mlb_participants(path, 2022, aliases)[0].team_name == "Current Club"
    _write(path, [["Team_name", "Distance"], ["Legacy Club", "6"]])
    with pytest.raises(ValidationError, match="label"):
        load_mlb_participants(path, 2022, aliases)


def test_participant_parser_rejects_duplicate_team_rows(tmp_path) -> None:
    path = tmp_path / "participants.csv"
    _write(
        path,
        [["team_name", "number_of_victories"], ["Alpha", "1"], ["Alpha", "1"]],
    )
    with pytest.raises(ValidationError, match="duplicate"):
        load_ncaabb_participants(path, 2023)
