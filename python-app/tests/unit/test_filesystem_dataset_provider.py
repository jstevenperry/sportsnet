import csv

import pytest

from sportsnet.adapters.filesystem_resources import FilesystemDatasetProvider
from sportsnet.domain.errors import ValidationError
from sportsnet.sports.mlb.module import MLB
from sportsnet.sports.ncaabb.module import NCAABB
from sportsnet.sports.registry import SportRegistry


def _write(path, rows) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as stream:
        csv.writer(stream).writerows(rows)


def test_training_years_require_every_season_file_and_participants(tmp_path) -> None:
    root = tmp_path / "NCAABB" / "data"
    directory = root / "2023"
    _write(
        directory / "tournament_participant_all.csv",
        [["team_name", "number_of_victories"], ["Alpha", "1"]],
    )
    provider = FilesystemDatasetProvider(
        tmp_path, SportRegistry((NCAABB, MLB)), require_ncaabb_alias_file=False
    )
    assert 2023 not in provider.available_training_years("NCAABB")


def test_training_years_validate_layout_of_available_resources(tmp_path) -> None:
    root = tmp_path / "NCAABB" / "data"
    directory = root / "2023"
    _write(root / "SeasonData-Alias.csv", [["source", "canonical"]])
    _write(
        directory / "tournament_participant_all.csv",
        [["team_name", "number_of_victories"], ["Alpha", "1"]],
    )
    for filename, width in (
        ("BasicSeasonData.csv", 38),
        ("OpponentSeasonData.csv", 38),
        ("AdvancedSeasonData.csv", 34),
        ("AdvancedOpponentSeasonData.csv", 34),
    ):
        _write(directory / filename, [["invalid"] * width, ["header"] * width])
    provider = FilesystemDatasetProvider(tmp_path, SportRegistry((NCAABB, MLB)))
    with pytest.raises(ValidationError, match="dataset has no participant examples"):
        provider.available_training_years("NCAABB")
