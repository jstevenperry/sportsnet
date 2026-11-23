import pytest

from sportsnet.domain.datasets import assemble_dataset
from sportsnet.domain.errors import ValidationError
from sportsnet.domain.models import SeasonData, TournamentParticipant
from sportsnet.domain.schemas import SportSchema
from sportsnet.sports.mlb.module import MLB
from sportsnet.sports.ncaabb.module import NCAABB
from sportsnet.sports.registry import SportRegistry


def test_registry_accepts_both_production_schemas_and_synthetic_extension() -> None:
    synthetic = SportSchema(
        sport_id="synthetic",
        display_name="Synthetic",
        feature_schema_version="synthetic-v1",
        feature_names=("x", "y", "z"),
        output_classes=(2, 4, 8),
        season_years=(2020,),
        training_years=(2020,),
    )
    registry = SportRegistry((NCAABB, MLB, synthetic))
    assert registry.get("NCAABB") is NCAABB
    assert registry.get("mlb") is MLB
    assert registry.get("synthetic") is synthetic


def test_registry_rejects_duplicate_sport_ids() -> None:
    with pytest.raises(ValidationError, match="duplicate"):
        SportRegistry((NCAABB, NCAABB))


def test_dataset_is_stable_validates_joins_and_applies_ncaabb_smoothing() -> None:
    schema = SportSchema(
        sport_id="synthetic",
        display_name="Synthetic",
        feature_schema_version="synthetic-v1",
        feature_names=("x",),
        output_classes=(0, 1, 2),
        season_years=(2020, 2021),
        training_years=(2020, 2021),
        smoothing_multiplicities=(1, 2, 4),
    )
    seasons = {
        year: {
            team: SeasonData("synthetic", year, team, (float(year),))
            for team in ("Alpha", "Beta")
        }
        for year in (2020, 2021)
    }
    participants = (
        TournamentParticipant(2021, "Beta", 2),
        TournamentParticipant(2020, "Alpha", 0),
        TournamentParticipant(2021, "Alpha", 1),
    )
    dataset = assemble_dataset(schema, seasons, participants, (2020, 2021))
    assert dataset.features == ((2020.0,), (2021.0,), (2021.0,))
    assert dataset.labels == (0, 1, 2)

    smoothed = assemble_dataset(
        schema, seasons, participants, (2020, 2021), smoothing=True
    )
    assert smoothed.labels == (0, 1, 1, 2, 2, 2, 2)


def test_dataset_rejects_overlap_missing_rows_and_invalid_labels() -> None:
    with pytest.raises(ValidationError, match="unique"):
        assemble_dataset(
            NCAABB,
            {},
            (),
            (2018, 2018),
        )
    schema = SportSchema(
        sport_id="synthetic",
        display_name="Synthetic",
        feature_schema_version="synthetic-v1",
        feature_names=("x",),
        output_classes=(0, 1),
        season_years=(2020,),
        training_years=(2020,),
    )
    with pytest.raises(ValidationError, match="missing season"):
        assemble_dataset(
            schema,
            {},
            (TournamentParticipant(2020, "Missing", 0),),
            (2020,),
        )
    with pytest.raises(ValidationError, match="label"):
        assemble_dataset(
            schema,
            {2020: {"Alpha": SeasonData("synthetic", 2020, "Alpha", (1.0,))}},
            (TournamentParticipant(2020, "Alpha", 2),),
            (2020,),
        )
