import pytest

from sportsnet.domain.errors import ValidationError
from sportsnet.domain.layouts import (
    hourglass_widths,
    resolve_layout,
    tapered_tree_widths,
)


def _resolve(**overrides: object) -> tuple[int, ...]:
    options: dict[str, object] = {
        "input_width": 64,
        "output_width": 7,
        "hidden_layer_count": 5,
        "algorithm": "HOUR_GLASS",
        "seed": 912,
    }
    options.update(overrides)
    return resolve_layout(**options)  # type: ignore[arg-type]


@pytest.mark.parametrize(
    ("hidden", "expected"),
    [
        (3, [700, 8, 700]),
        (4, [700, 8, 8, 700]),
        (5, [400, 700, 8, 700, 12]),
        (7, [300, 500, 700, 8, 700, 356, 12]),
        (
            20,
            [166, 233, 300, 366, 433, 500, 566, 633, 700, 8]
            + [8, 700, 614, 528, 442, 356, 270, 184, 98, 12],
        ),
    ],
)
def test_hourglass_shape_matches_worked_examples(
    hidden: int, expected: list[int]
) -> None:
    assert hourglass_widths(100, hidden, peak=700, neck=8, final=12) == expected


@pytest.mark.parametrize(
    ("hidden", "expected"),
    [
        (3, [700, 356, 12]),
        (7, [400, 700, 562, 424, 287, 149, 12]),
        (
            20,
            [220, 340, 460, 580, 700, 654, 608, 562, 516, 470]
            + [424, 378, 333, 287, 241, 195, 149, 103, 57, 12],
        ),
    ],
)
def test_tapered_tree_shape_matches_worked_examples(
    hidden: int, expected: list[int]
) -> None:
    assert tapered_tree_widths(100, hidden, peak=700, final=12) == expected


@pytest.mark.parametrize("algorithm", ["HOUR_GLASS", "TAPERED_TREE"])
@pytest.mark.parametrize("hidden", [3, 4, 5, 7, 20])
def test_generated_layouts_are_seeded_and_within_drawn_bounds(
    algorithm: str, hidden: int
) -> None:
    first = _resolve(algorithm=algorithm, hidden_layer_count=hidden)
    assert first == _resolve(algorithm=algorithm, hidden_layer_count=hidden)
    assert len(first) == hidden + 2
    assert first[0] == 64 and first[-1] == 7
    assert 4 * 64 <= max(first[1:-1]) <= 10 * 64
    assert min(first[1:-1]) >= 2 * 7


def test_hourglass_has_two_peaks_around_a_narrow_neck() -> None:
    hidden = _resolve(algorithm="HOUR_GLASS", hidden_layer_count=9)[1:-1]
    neck = min(hidden)
    assert 2 * 7 <= neck <= 4 * 7
    assert hidden[4] == neck
    assert hidden[3] == hidden[5] == max(hidden)


def test_tapered_tree_peaks_at_ceil_quarter_and_never_widens_after() -> None:
    hidden = _resolve(algorithm="TAPERED_TREE", hidden_layer_count=20)[1:-1]
    peak_index = hidden.index(max(hidden))
    assert peak_index == 4
    tail = hidden[peak_index:]
    assert all(a >= b for a, b in zip(tail, tail[1:], strict=False))
    assert 2 * 7 <= hidden[-1] <= 4 * 7


@pytest.mark.parametrize("algorithm", ["HOUR_GLASS", "TAPERED_TREE"])
def test_generated_shapes_require_at_least_three_hidden_layers(
    algorithm: str,
) -> None:
    for count in (None, 0, 1, 2):
        with pytest.raises(ValidationError, match="at least 3"):
            _resolve(algorithm=algorithm, hidden_layer_count=count)


@pytest.mark.parametrize("algorithm", ["HOUR_GLASS", "TAPERED_TREE"])
def test_generated_shapes_ignore_network_layout_and_require_input_ge_output(
    algorithm: str,
) -> None:
    assert _resolve(algorithm=algorithm, user_layout="64x9x7") == _resolve(
        algorithm=algorithm
    )
    with pytest.raises(ValidationError, match="input width"):
        _resolve(algorithm=algorithm, input_width=3, output_width=7)


def test_algorithm_is_required_and_legacy_identifiers_are_rejected() -> None:
    with pytest.raises(ValidationError, match="required"):
        _resolve(algorithm=None)
    for name in (
        "DEFAULT",
        "DEFAULT_RAND",
        "DEFAULT_SMALL",
        "DEFAULT_RAND_SMALL",
        "HOUR_GLASS_RAND",
        "TAPERED_TREE_RAND",
        "UNKNOWN",
    ):
        with pytest.raises(ValidationError, match="unsupported"):
            _resolve(algorithm=name)


def test_user_layout_derives_hidden_count_and_ignores_number_of_layers() -> None:
    for count in (None, 1, 9):
        assert _resolve(
            input_width=3,
            output_width=2,
            hidden_layer_count=count,
            algorithm="USER_DEFINED",
            user_layout="3x5x4x2",
        ) == (3, 5, 4, 2)


def test_user_layout_is_required_and_dimensions_must_match_schema() -> None:
    with pytest.raises(ValidationError, match="layout is required"):
        _resolve(algorithm="USER_DEFINED", user_layout=None)
    with pytest.raises(ValidationError, match="input and output"):
        _resolve(
            input_width=3,
            output_width=2,
            algorithm="USER_DEFINED",
            user_layout="4x5x2",
        )
    with pytest.raises(ValidationError, match="positive"):
        _resolve(
            input_width=3,
            output_width=2,
            algorithm="USER_DEFINED",
            user_layout="3x0x2",
        )


def test_user_defined_rand_keeps_layer_count_and_is_seeded() -> None:
    options = {
        "input_width": 3,
        "output_width": 2,
        "algorithm": "USER_DEFINED_RAND",
        "user_layout": "3x50x40x30x2",
        "seed": 5,
    }
    first = _resolve(**options)
    assert first == _resolve(**options)
    assert len(first) == 5 and first[0] == 3 and first[-1] == 2
    assert first[1] < 75 and first[2] < 60 and first[3] < 45


def test_parameter_limit_is_enforced() -> None:
    with pytest.raises(ValidationError, match="parameter limit"):
        _resolve(
            input_width=100,
            output_width=10,
            algorithm="USER_DEFINED",
            user_layout="100x100000x100000x10",
            max_parameters=1_000,
        )
