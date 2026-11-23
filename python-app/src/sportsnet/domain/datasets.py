"""Deterministic supervised dataset assembly from sport-neutral records."""

from collections.abc import Iterable, Mapping, Sequence
from dataclasses import dataclass

from sportsnet.domain.errors import ValidationError
from sportsnet.domain.models import SeasonData, TournamentParticipant
from sportsnet.domain.schemas import SportSchema


@dataclass(frozen=True, slots=True)
class TrainingDataset:
    features: tuple[tuple[float, ...], ...]
    labels: tuple[int, ...]


def validate_year_splits(
    training_years: Sequence[int], evaluation_years: Sequence[int]
) -> None:
    training = tuple(training_years)
    evaluation = tuple(evaluation_years)
    if not training or not evaluation:
        raise ValidationError("training and evaluation years must be non-empty")
    if len(set(training)) != len(training) or len(set(evaluation)) != len(evaluation):
        raise ValidationError("training and evaluation years must be unique")
    if set(training).intersection(evaluation):
        raise ValidationError("training and evaluation years must be disjoint")


def assemble_dataset(
    schema: SportSchema,
    seasons_by_year: Mapping[int, Mapping[str, SeasonData]],
    participants: Iterable[TournamentParticipant],
    years: Sequence[int],
    *,
    smoothing: bool = False,
) -> TrainingDataset:
    selected_years = tuple(years)
    if not selected_years or len(set(selected_years)) != len(selected_years):
        raise ValidationError("selected years must be non-empty and unique")
    unsupported = set(selected_years).difference(schema.training_years)
    if unsupported:
        raise ValidationError(f"unsupported training years: {sorted(unsupported)}")
    class_index = {label: index for index, label in enumerate(schema.output_classes)}
    participant_map: dict[tuple[int, str], TournamentParticipant] = {}
    for participant in participants:
        key = (participant.year, participant.team_name)
        if key in participant_map:
            raise ValidationError(
                f"duplicate participant row for "
                f"{participant.year}/{participant.team_name}"
            )
        participant_map[key] = participant

    rows: list[tuple[int, str, tuple[float, ...], int]] = []
    for year in sorted(selected_years):
        season = seasons_by_year.get(year)
        if season is None:
            raise ValidationError(f"missing season data for year {year}")
        for (participant_year, team), participant in participant_map.items():
            if participant_year != year:
                continue
            data = season.get(team)
            if data is None:
                raise ValidationError(f"missing season data for {year}/{team}")
            if data.sport.casefold() != schema.sport_id.casefold():
                raise ValidationError(
                    f"sport mismatch for {year}/{team}: expected {schema.sport_id}"
                )
            if data.year != year or data.team_name != team:
                raise ValidationError(f"season identity mismatch for {year}/{team}")
            if len(data.features) != schema.input_width:
                raise ValidationError(
                    f"feature width mismatch for {year}/{team}: "
                    f"expected {schema.input_width}, got {len(data.features)}"
                )
            if participant.victories not in class_index:
                raise ValidationError(
                    f"invalid label {participant.victories} for {year}/{team}"
                )
            rows.append((year, team, data.features, participant.victories))
    rows.sort(key=lambda row: (row[0], row[1]))
    if not rows:
        raise ValidationError("dataset has no participant examples")
    if smoothing and schema.smoothing_multiplicities is None:
        raise ValidationError(f"smoothing is not configured for {schema.sport_id}")

    features: list[tuple[float, ...]] = []
    labels: list[int] = []
    for _, _, row_features, label in rows:
        multiplicity = (
            schema.smoothing_multiplicities[class_index[label]]
            if smoothing and schema.smoothing_multiplicities is not None
            else 1
        )
        features.extend([row_features] * multiplicity)
        labels.extend([label] * multiplicity)
    return TrainingDataset(tuple(features), tuple(labels))
