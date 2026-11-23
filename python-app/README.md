# Sportsnet Python

Combined NCAA basketball and MLB API backed by FastAPI and Keras. Run all
commands from `python-app/`. Python 3.12 and `uv` are required.

## Install and run

```sh
uv sync --frozen --extra dev
uv run sportsnet
```

The server listens on `127.0.0.1:8287`. Configure runtime paths and limits
with `SPORTSNET_` environment variables; for example,
`SPORTSNET_RESOURCE_ROOT` and `SPORTSNET_EVENT_DATABASE`. Models are stored in
`Networks` under the resource root, alongside the season and participant CSV
resources. Each saved network has `<UUID>.model.keras` and `<UUID>.json` files
under `Networks/<SPORT>` (for example, `Networks/NCAABB` or `Networks/MLB`);
the JSON contains its descriptor and manifest. `Networks/descriptor-index.sqlite`
provides a rebuildable search index.
When training reaches its maximum epochs without bringing training error below
`maxNetworkError`, the server logs the final training error, target, and
training confusion matrix at INFO. Evaluation is not run in this case.
Each batch iteration logs the outcome and evaluation metrics and confusion
matrix at INFO when evaluation ran; otherwise it logs training metrics and the
training confusion matrix. Set `SPORTSNET_TRAINING_LOG_LEVEL=DEBUG` in `.env`
for progress at the configured training reporting intervals and detailed
single-network completion summaries. The setting accepts `INFO` (the default)
or `DEBUG`.
Blank numeric season statistics are filled with zero for NCAA and MLB.
NCAA training and evaluation use only each year's tournament participants.
Unmatched participants are excluded for the affected year with a warning about
possible missing aliases or season rows. NCAA tournament inference still
rejects incomplete joins. Missing participant
labels and malformed or non-finite statistics remain errors.
For each batch iteration, the server logs its position and remaining count
before training, then its outcome, model ID, epoch count, and stop reason when
it finishes, along with the resolved network layout, iteration elapsed time,
and cumulative training time. Both times include data loading, evaluation,
and model persistence where applicable; cumulative time sums attempted
iteration durations and excludes time spent waiting in the batch queue. The
iteration completion log includes the metrics and matrices. At the end, the
server logs a multiline batch summary and records the counts in the batch
completion event. `trained` counts returned results that reached the training error
target, including saved networks and those rejected by evaluation.
`training_target_not_met` counts networks that reached the epoch limit without
meeting the target; it does not mean they can never be trained.
`evaluation_below_threshold` counts trained networks not saved because their
evaluation accuracy was below the threshold. The summary also reports saved,
failed, and cancelled counts, requested and started iterations, iterations
not started, training and evaluation years, cumulative training time, and
average time per started iteration. The average includes failed and cancelled
attempts and is unavailable if no iteration started. Cancellation is counted even when
detected before an iteration starts. An error after a training result is
returned may count as a failure as well as a trained or saved result.
The summary also lists the structure algorithm, distinct hidden-layer counts
from completed iterations, updater, hidden and output activations, L2
regularization lambda, and learning rate. Hidden-layer counts are unavailable
if no iteration completed; user-defined layouts determine their own counts.
Routine HTTP access logs are suppressed by default. Set
`SPORTSNET_ACCESS_LOG_LEVEL=DEBUG` in `.env` to show them at DEBUG level;
`INFO` is the default. Endpoint warnings and errors remain enabled.

## Checks

```sh
uv run pytest -m unit
uv run pytest -m "repository or api"
uv run pytest -m keras
uv run pytest -m e2e
uv run pytest -m integration
uv run ruff check .
uv run ruff format --check .
uv run mypy src
```

The test suite uses synthetic datasets and controlled backends for training,
evaluation, tournaments, and persistence.

## Live training requests

The automated integration tests use temporary synthetic data and do not require
a running server. The clients under `tests/integration/` send real HTTP
requests to a server that you start separately. Configure
`SPORTSNET_RESOURCE_ROOT` to a directory with this layout before starting the
server:

```text
<resource-root>/
  NCAABB/data/<year>/
    BasicSeasonData.csv
    OpponentSeasonData.csv
    AdvancedSeasonData.csv
    AdvancedOpponentSeasonData.csv
    tournament_participant_all.csv
  NCAABB/data/SeasonData-Alias.csv
  MLB/data/<year>/
    <year>-Team-Batting.csv
    <year>-Team-Pitching.csv
    <year>-Team-Fielding.csv
    playoff-results.csv
```

Provide files for every requested training and evaluation year. The sample
requests use NCAA training year 2023/evaluation year 2022 and MLB training
year 2022/evaluation year 2021. Supply the corresponding production resources
under the configured resource root.

Start the app in one terminal:

```sh
SPORTSNET_RESOURCE_ROOT=/path/to/resources uv run sportsnet
```

In another terminal, run one of the sample requests:

```sh
uv run python tests/integration/training_client.py \
  --sport ncaabb \
  --request tests/integration/requests/ncaabb-training.json

uv run python tests/integration/training_client.py \
  --sport mlb \
  --request tests/integration/requests/mlb-training.json
```

For an NCAABB batch request, run the batch client:

```sh
uv run python tests/integration/batch_training_client.py \
  --sport ncaabb \
  --request tests/integration/requests/ncaabb-batch-training.json
```

To run a tournament simulation using every saved network for the selected
sport, specify a tournament year listed by `/{sport}/tournamentYears`:

```sh
uv run python tests/integration/simulation_client.py \
  --sport ncaabb \
  --tournament-year 2024
```

The client gets saved network IDs from `/{sport}/networks`, then posts them with
the selected year to `/{sport}/simulations`. It prints the returned ranking CSV
to standard output; pass `--base-url` to use a different server.

The batch endpoint responds with `202 Accepted` and a `batchTrainingRunId`
after the work is accepted; training continues in the server. By default, the
batch client prints the response and returns immediately. Use `--wait` to poll
management events until the run finishes; in this mode it exits unsuccessfully
if the batch fails, is cancelled, or the timeout expires. `--timeout` sets the
deadline in seconds, and `--poll-interval` controls event checks while waiting.
Adjust the sample request for the intended workload.

Both clients default to `http://127.0.0.1:8287`; pass `--base-url` to target
another server. Both support `--training-years` and `--evaluation-years`
overrides. The year fields accept either JSON integer arrays or
comma-separated strings. To try several training years, for example:

```sh
uv run python tests/integration/training_client.py \
  --sport ncaabb \
  --request tests/integration/requests/ncaabb-training.json \
  --training-years "2022,2023" \
  --evaluation-years "2021"
```

Training and evaluation year sets must be non-empty, disjoint, supported by
the selected sport, and present in the configured resource tree. The client
prints a persisted descriptor when a model qualifies, or the unsaved training
report (`networkSaved: false`, with all of the run's metrics and confusion
matrices) when training completes below the persistence threshold.

Training and evaluation are separate phases, as in the Java application.

**Network layout.** `networkStructureAlgorithm` is required and is one of
`HOUR_GLASS`, `TAPERED_TREE`, `USER_DEFINED`, or `USER_DEFINED_RAND`.
`HOUR_GLASS` and `TAPERED_TREE` generate a seeded layout and require
`numberOfHiddenLayers` of at least 3 (`networkLayout` is ignored).
`USER_DEFINED` and `USER_DEFINED_RAND` require `networkLayout`, for example
`64x256x32x7` (`numberOfHiddenLayers` is ignored). Missing or invalid values
return `400`.

**Training phase.** Every `networkEvaluationIndex` epochs (and at the final
epoch) the network is scored on the **training data only**. Training stops as
soon as `training_error` (`1 − macro recall` on the training data) is below
`maxNetworkError`, or after `maxNumberOfTrainingEpochs`. The evaluation years
are not read or scored during this phase. The server terminal prints a
`Training progress` line every `reportingInterval` epochs and when training
stops. `reportingInterval` is an optional positive integer; if omitted, a line
prints at every 10% of `maxNumberOfTrainingEpochs` (every epoch when there are
fewer than ten). It only controls printing, never stopping. Each line shows:

- `training_loss`: Keras's cross-entropy loss for the latest epoch (not a
  percentage).
- `training_error` and `training_accuracy`: the network re-scored on the
  training data. `training_error` is compared with `max_network_error`.
- `decision`: `continue`, `monitoring_only` (a report between checks),
  `early_stop`, or `maximum_epochs_reached`.

**Evaluation phase.** Only if training stopped because `maxNetworkError` was
reached, the network is scored once on the evaluation years and a
`Network evaluation` line prints `eval_error`, `eval_accuracy`,
`eval_precision`, `eval_recall`, `eval_f1`, and `eval_composite_percent`. The
model is saved only if evaluation accuracy multiplied by 100 is at least
`networkPerformanceThreshold` (`qualifies_for_persistence=True`). If the epoch
limit is reached first, no evaluation runs and the model is discarded.

Rate values are fractions from 0 to 1; percentage fields and `training_loss`
are not. A low training error with a high evaluation error indicates
overfitting. The model and correlation IDs distinguish concurrent requests.

NCAA alias CSV rows may list several aliases for a source name, and names may
share aliases across rows. The loader groups linked names deterministically:
it chooses the alphabetically first source with a blank alias, or the
alphabetically first name if none has a blank alias. Matching is case-sensitive
after trimming whitespace, removing `*`, and trimming trailing periods. If
multiple season rows resolve to the same team, the parser rejects the duplicate.
