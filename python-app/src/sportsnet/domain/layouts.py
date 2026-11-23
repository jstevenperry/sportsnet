"""Validated, deterministic dense-network architecture resolution."""

import random

from sportsnet.domain.errors import ValidationError

_GENERATED = {"HOUR_GLASS", "TAPERED_TREE"}
_USER_DEFINED = {"USER_DEFINED", "USER_DEFINED_RAND"}
_MIN_GENERATED_HIDDEN_LAYERS = 3
_MAX_HIDDEN_LAYERS = 256


def resolve_layout(
    *,
    input_width: int,
    output_width: int,
    hidden_layer_count: int | None,
    algorithm: str | None,
    seed: int,
    user_layout: str | None = None,
    max_width: int = 100_000,
    max_parameters: int = 10_000_000,
) -> tuple[int, ...]:
    if input_width <= 0 or output_width <= 0:
        raise ValidationError("input and output widths must be positive")
    if max_width <= 0 or max_parameters <= 0:
        raise ValidationError("layout limits must be positive")
    if not algorithm:
        raise ValidationError("networkStructureAlgorithm is required")
    if algorithm not in _GENERATED | _USER_DEFINED:
        raise ValidationError(f"unsupported network structure algorithm: {algorithm}")

    generator = random.Random(seed)
    if algorithm in _USER_DEFINED:
        hidden = _parse_user_layout(user_layout, input_width, output_width)
        if algorithm == "USER_DEFINED_RAND":
            hidden = _randomize_user_layout(hidden, input_width, generator)
    else:
        if (
            hidden_layer_count is None
            or hidden_layer_count < _MIN_GENERATED_HIDDEN_LAYERS
        ):
            raise ValidationError(
                f"{algorithm} requires numberOfHiddenLayers of at least 3"
            )
        if hidden_layer_count > _MAX_HIDDEN_LAYERS:
            raise ValidationError(
                f"hidden-layer count must be at most {_MAX_HIDDEN_LAYERS}"
            )
        if input_width < output_width:
            raise ValidationError(
                f"{algorithm} requires the input width to be at least the output width"
            )
        peak = generator.randint(4 * input_width, 10 * input_width)
        if algorithm == "HOUR_GLASS":
            neck = generator.randint(2 * output_width, 4 * output_width)
            final = generator.randint(2 * output_width, 4 * output_width)
            hidden = hourglass_widths(
                input_width, hidden_layer_count, peak=peak, neck=neck, final=final
            )
        else:
            final = generator.randint(2 * output_width, 4 * output_width)
            hidden = tapered_tree_widths(
                input_width, hidden_layer_count, peak=peak, final=final
            )

    layout = (input_width, *hidden, output_width)
    if any(width <= 0 or width > max_width for width in layout):
        raise ValidationError(f"layout widths must be in [1, {max_width}]")
    parameter_count = sum(
        previous * current + current
        for previous, current in zip(layout, layout[1:], strict=False)
    )
    if parameter_count > max_parameters:
        raise ValidationError(
            f"layout exceeds configured parameter limit ({max_parameters})"
        )
    return layout


def hourglass_widths(
    input_width: int, hidden_layers: int, *, peak: int, neck: int, final: int
) -> list[int]:
    """Two humps around a narrow neck, smoothly rising and falling."""
    if hidden_layers % 2:
        middle = (hidden_layers + 1) // 2
        first_peak, second_peak = middle - 1, middle + 1
        neck_layers = [middle]
    else:
        first_peak, second_peak = hidden_layers // 2 - 1, hidden_layers // 2 + 2
        neck_layers = [hidden_layers // 2, hidden_layers // 2 + 1]
    widths = {layer: neck for layer in neck_layers}
    widths[first_peak] = peak
    widths[second_peak] = peak
    for layer in range(1, first_peak):
        widths[layer] = _interpolate(input_width, peak, layer, first_peak)
    for layer in range(second_peak + 1, hidden_layers + 1):
        widths[layer] = _interpolate(
            peak, final, layer - second_peak, hidden_layers - second_peak
        )
    return [widths[layer] for layer in range(1, hidden_layers + 1)]


def tapered_tree_widths(
    input_width: int, hidden_layers: int, *, peak: int, final: int
) -> list[int]:
    """One hump that rises quickly and tapers slowly to the output side."""
    peak_layer = -(-hidden_layers // 4)
    widths = {peak_layer: peak}
    for layer in range(1, peak_layer):
        widths[layer] = _interpolate(input_width, peak, layer, peak_layer)
    for layer in range(peak_layer + 1, hidden_layers + 1):
        widths[layer] = _interpolate(
            peak, final, layer - peak_layer, hidden_layers - peak_layer
        )
    return [widths[layer] for layer in range(1, hidden_layers + 1)]


def _interpolate(start: int, end: int, step: int, steps: int) -> int:
    return start + ((end - start) * step) // steps


def _randomize_user_layout(
    hidden: list[int], input_width: int, generator: random.Random
) -> list[int]:
    resolved: list[int] = []
    previous = input_width
    for width in hidden:
        lower = max(1, previous // 2)
        upper = max(lower + 1, (width * 3) // 2)
        value = generator.randrange(lower, upper)
        resolved.append(value)
        previous = value
    return resolved


def _parse_user_layout(
    value: str | None, input_width: int, output_width: int
) -> list[int]:
    if not value:
        raise ValidationError("user-defined layout is required")
    try:
        widths = [int(item) for item in value.split("x")]
    except ValueError as error:
        raise ValidationError(
            "layout must contain positive integers separated by x"
        ) from error
    if any(width <= 0 for width in widths):
        raise ValidationError("layout widths must be positive")
    if widths[0] != input_width or widths[-1] != output_width:
        raise ValidationError("layout input and output widths must match the schema")
    if not 3 <= len(widths) <= _MAX_HIDDEN_LAYERS + 2:
        raise ValidationError("layout must have between 1 and 256 hidden layers")
    return widths[1:-1]
