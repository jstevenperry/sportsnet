# Python and Keras Application Design Specification

## 1. Purpose, authority, and scope

This document specifies a new Sportsnet application implemented in Python
with Keras as its deep-learning backend. It is an implementation profile for
the requirements in:

- [the language- and framework-independent design](design-spec.md);
- [system and domain behavior](system-and-domain-spec.md);
- [interfaces, persistence, and operations](interfaces-storage-operations-spec.md);
- [verification and migration](testing-and-migration-spec.md).

Those documents remain unchanged and define the domain and compatibility
contracts. This document selects the Python libraries, package boundaries,
runtime behavior, and test-first implementation sequence. If a Python
implementation choice conflicts with a domain or wire contract in those
documents, the domain or wire contract wins unless this document explicitly
defines a safer, versioned replacement behavior. The implementation is a
new application, not a port that imports or wraps the Java implementation.

The application MUST:

- run as one deployable application containing the shared framework, NCAA
  men's basketball and MLB modules, repositories, management API, and model
  backend;
- use Keras for new model construction, training, inference, and persistence;
- use in-process repositories for resource and model data access;
- expose the specified NCAA, MLB, and management HTTP contracts;
- store and return models through the persistent model repository;
- be implemented test-first with deterministic, hermetic tests and synthetic
  fixtures before production workflows are added.

The production application includes NCAA basketball (`NCAABB`) and MLB.
Generic workflows MUST remain sport-neutral. Tests MUST also define a small
fake sport module with a different schema and class count, then run the same
extension-contract tests against it. This fake module exists only to test
extensibility: do not ship it or register it as a production sport.

The normative terms **MUST**, **MUST NOT**, **SHOULD**, and **MAY** have their
usual requirements meanings.

Identifiers chosen at code-generation time (types, functions, parameters,
variables, fields, and persisted keys) MUST NOT collide with, shadow, or be
ambiguous with similarly named members of the selected backend or its
libraries, and MUST name what they actually hold. For example, a repository
method that persists a model MUST NOT be named like a backend method that
saves model weights, and a value scored on training data MUST NOT carry a name
that suggests evaluation data.

## 2. Selected Python technology

Use the following stack unless a concrete incompatibility is demonstrated and
documented in an architecture decision before implementation:

| Concern | Selection | Required use |
|---|---|---|
| Runtime | Python 3.12 | Pin an exact supported patch release in CI/container configuration. |
| REST/API | FastAPI with Pydantic 2 | Typed request/response validation, OpenAPI generation, and in-process API tests. |
| ASGI server | Uvicorn | Production entry point; no development reload in production. |
| ML API | Keras 3 with the TensorFlow backend | Keras owns model layers, optimizer integration, `.keras` model serialization, and inference. |
| Numeric arrays | NumPy | Typed, finite arrays at the domain/backend boundary. |
| CSV | Python standard-library `csv` | Preserve quoted-field behavior and produce source-located parser errors. Do not rely on implicit pandas type inference. |
| Durable storage | SQLite via Python `sqlite3` | Transactional event store and rebuildable descriptor index; connection-per-operation, WAL mode, foreign keys, and a configured busy timeout. |
| Configuration | Pydantic Settings (or an equivalent typed settings layer) | Load environment/configuration, validate once at startup, and expose redacted diagnostics. |
| Tests | pytest | Unit, integration, API, and end-to-end suites; no production credentials or network access. |
| Packaging/lint | `pyproject.toml`, a lockfile, Ruff, and a static type checker | Reproducible install, lint, formatting, and type-check commands documented in the repository. |

Use a maintained FastAPI/Pydantic release compatible with the selected Python
version and lock exact dependency versions for each release. Pin compatible
Keras 3 and TensorFlow versions together; never allow an unconstrained
TensorFlow upgrade to alter artifact behavior in a release. Record Python,
Keras, TensorFlow, NumPy, and application versions in each model manifest.
Do not install or use a second ML framework.

The Python implementation MUST NOT add pandas, a task broker, a cache server,
or a second database unless a measured requirement cannot be met by this
stack. In particular, bounded in-process batch execution is sufficient for
v1; no Redis/Celery dependency is required.

## 3. Package layout and dependency rules

Use a `src/` layout and keep framework-independent domain code importable
without importing FastAPI, TensorFlow, or storage implementations:

```text
pyproject.toml
src/sportsnet/
  __init__.py
  main.py                    # FastAPI construction and lifespan wiring
  api/
    common.py                # error response and shared API dependencies
    ncaabb.py                # NCAA routes and wire DTOs
    mlb.py                   # MLB routes and wire DTOs
    management.py            # management routes and wire DTOs
  application/
    training.py
    batch.py
    evaluation.py
    tournament.py
    management.py
  domain/
    errors.py
    models.py                # sport-neutral domain records and outcomes
    metrics.py
    layouts.py
    preprocessing.py
    ports.py                 # repository, module, and backend protocols
  sports/
    registry.py
    ncaabb/
      module.py
      schemas.py
      csv_repository.py
      tournament.py
    mlb/
      module.py
      schemas.py
      csv_repository.py
      tournament.py
  adapters/
    keras_backend.py
    filesystem_resources.py
    sqlite_store.py
    configuration.py
  resources/                 # only test/example resources; no implicit production data
tests/
  unit/
  contract/
  integration/
  api/
  e2e/
```

Names MAY vary, but responsibilities and dependency directions MUST remain:

```text
FastAPI routers -> application services -> domain contracts
                                      |-> repository ports -> adapters
                                      |-> Keras backend port -> Keras adapter
sport modules ----------------------> domain contracts
```

Rules:

- `domain/` MUST NOT import FastAPI, Pydantic transport DTOs, TensorFlow,
  Keras, `sqlite3`, or filesystem adapters.
- `application/` MUST depend on domain protocols and injected services, not
  concrete router or Keras classes.
- `sports/` owns each sport's ordered schema, parsing/encoding, aliases,
  supported-year manifest, and tournament plugin. Generic workflow modules
  MUST NOT branch on sport name.
- `api/` converts wire DTOs into domain requests and maps domain errors to
  stable HTTP responses. It MUST NOT parse season CSVs, build Keras models,
  compute tournament scores, or contain training orchestration.
- `adapters/keras_backend.py` is the only module that imports Keras or
  TensorFlow. A backend protocol and fake backend MUST permit application
  tests without either library initialized.
- Construct all dependencies in one application factory. Tests MUST be able
  to inject temporary repositories, a fake backend, a fake clock, and a
  deterministic ID/seed source without global monkeypatching.
- Importing a router or domain module MUST NOT open databases, load season
  files, initialize a model, or start a worker.

## 4. Domain and sport-module contracts

### 4.1 Domain records and typed outcomes

Define typed domain records/protocols for at least:

- `SeasonData` (matching the Java domain type): stable sport identifier, year,
  canonical team name, and a
  finite ordered feature vector;
- `TournamentParticipant`: year, canonicalizable team name, and an integer
  target label/victory count;
- `SportModule`: stable ID/display name, versioned feature schema, output
  class order, parsers/repositories, explicit supported-year manifests,
  validators, aliases, and optional tournament plugins;
- `NetworkDescriptor`: ID, sport, resolved dimensions/layout, year splits,
  preprocessing and training settings, metrics, outcome, and timestamps;
- `ModelManifest`: artifact format/version, model ID, sport/schema identities,
  Keras/TensorFlow/runtime versions, architecture, preprocessing parameters,
  class order, seed, resolved configuration, metrics, and eligibility fields;
- `TrainingOutcome`: exactly one of persisted success, completed below
  threshold, or failure. A model handle is present only for a successfully
  persisted model;
- `ModelHandle`: a successfully loaded persisted model plus its descriptor,
  manifest, and fitted preprocessor;
- `ManagementEvent` and `ManagementEventSummary` (matching the Java domain
  types): typed, validated domain values.

Use immutable dataclasses (or equivalent immutable models) for records that
cross service boundaries. Convert NumPy values to ordinary finite Python
numbers before JSON serialization or storage.

### 4.2 Sport registry and extension contract

The registry MUST be built from explicit application configuration or
explicit module registration. It MUST validate at startup that each module's
identifier is unique, its feature names are unique and ordered, output classes
are unique and ordered, declared dimensions agree with encoders, and enabled
plugins have all required data/templates. Registration MUST NOT require
editing generic training, evaluation, persistence, event, or backend code.

Both production modules MUST pass the same contract tests. A synthetic test
module MUST use dimensions and class order different from both production
modules, register without tournament support, and complete generic training,
evaluation, persistence, reload, and model/sport mismatch rejection.

Exact NCAA and MLB feature lists, feature indices, labels, aliases, input
widths, output ordering, CSV widths, year manifests, smoothing multiplicities,
and tournament contracts are defined in the linked system/domain spec and
MUST be implemented verbatim. In particular:

- NCAA uses `ncaabb-season-v1`, 64 ordered inputs, and classes `[0, 1, 2, 3,
  4, 5, 6]`; retain the intentional duplicate feature at index 21.
- MLB uses `mlb-season-v1`, 78 ordered inputs, and six declared outputs;
  validate actual label range/order against fixtures and data. Map the
  `errors` feature from CSV `E`, not `#Fld`.
- Season years and participant/training years come from explicit manifests,
  not inferred continuous ranges. A year is trainable only when required
  labels and season features are both available.
- Production datasets, aliases, participants, tournament templates, model
  artifacts, and runtime properties are external deployment inputs. Keep
  required resources explicit, use synthetic fixtures for parser and alias
  behavior, and never treat an absent production file as an empty resource.
- Apply canonical aliases consistently before joins. A team may have multiple
  aliases and aliases may be shared across rows; treat all linked names as one
  team resolved deterministically to a single canonical name, independent of
  row order. NCAA training and evaluation exclude unmatched teams and
  participants only in the affected year and report possible missing aliases
  or rows. NCAA season statistics are selected by each year's tournament
  participants before parsing features or checking joins; nonparticipants do
  not affect training or evaluation. Tournament inference rejects incomplete
  joins. Duplicate year/team rows and distinct season records resolving to one
  team remain errors.
- Use standard-library CSV parsing with `newline=""`, explicit encoding,
  explicit header-row handling, and exact field-count validation. Every parse
  error MUST identify file/resource key, year where applicable, row, column,
  expected value/shape, and actual value/shape.
- Skip CSV header rows without validating their names or contents. Data columns
  retain their documented positional order.
- Fill blank or whitespace-only numeric season statistics with zero for NCAA
  and MLB, including training, evaluation, and inference data. Reject
  malformed or non-finite nonblank statistics, missing team names,
  missing participant labels, and out-of-range values. Missing team rows
  follow the sport and operation-specific join policy above.
- Provider-defined aggregate rows MUST be excluded from team records, joins,
  training, and inference; validate participant joins against actual teams.
- Do not assume production data is checked in. Synthetic fixtures are
  required; production roots are explicit configuration.

### 4.3 Dataset construction

Build examples by iterating participant rows in deterministic
year/team order, resolving each participant to a complete season record for
the same year, and pairing the exact ordered features with its integer class.
Split by explicitly requested disjoint years; do not randomly split records
across years. Do not introduce cross-year feature mappings unless a future
domain specification explicitly requires them.

Only training examples may be shuffled, using a seed derived from and recorded
with the request. Evaluation and inference ordering MUST remain stable.
Smoothing is disabled by default. When explicitly enabled for NCAA, add the
specified class-dependent duplicate rows without mutating source records, and
record the setting in the manifest. Do not replace smoothing with implicit
class weighting or another resampling strategy.

Validate array contracts at every boundary:

- features have shape `(n, feature_count)` and finite numeric values;
- labels have shape `(n,)`, integer dtype/values, and belong to the module's
  output class order;
- `n` is nonzero for operations requiring training/evaluation;
- reject ragged arrays, NaN, infinity, unknown class values, and dimension
  mismatches with typed domain errors.

## 5. Keras backend and model lifecycle

### 5.1 Keras adapter contract

Define a framework-neutral backend protocol with operations equivalent to:

- `build(resolved_spec) -> backend model handle`;
- `train_one_epoch(handle, x_train, y_train, seed/context) -> training info`;
- `predict_scores(handle, x) -> float array`;
- `save(handle, path)` and `load(path, expected_spec)`;
- backend identity/version and input/output shape inspection.

The fake backend MUST implement the same protocol. The application service
owns epoch scheduling, stopping and threshold rules, metric computation,
cancellation, event lifecycle, and persistence decisions. Keras callbacks or
Keras metric defaults MUST NOT silently become the application's contract.

### 5.2 Model construction and training

Implement a Keras Functional or Sequential model using an input tensor of the
module's exact feature width, the resolved ordered hidden dense layers, and a
final dense layer whose units equal the module's class count. Generic code
MUST derive both widths and output class order from the active sport/task
contract.

The standard categorical-classification profile MUST use:

- integer labels in the module's declared class order;
- a softmax output;
- sparse categorical cross-entropy, unless an explicitly validated,
  supported compatibility option requires a different loss;
- an explicitly configured Keras optimizer and initializer;
- L2 regularization only when resolved configuration requests it.

Validate activation/loss/output combinations before constructing a model.
Reject unsupported legacy enum values rather than silently mapping them to a
default. Provide an explicit, unit-tested compatibility mapping for every
legacy option the API claims to accept.

Training MUST be controlled one epoch at a time by the application service.
Each epoch call MUST use the complete training set exactly once, with Keras
internal shuffling disabled; deterministic shuffling, if enabled, is prepared
by the application using the recorded seed.

Training and evaluation are two distinct phases, matching the legacy
implementation:

1. **Training phase.** At each configured `networkEvaluationIndex` interval (and
   at the final epoch), the service scores the network on the **training data
   only** and computes the training error as `1 - macro recall` on that data.
   If the training error is strictly below `maxNetworkError`, training stops
   early. The evaluation years MUST NOT be read, predicted on, or scored during
   this phase.
2. **Evaluation phase.** If and only if the training phase stopped because
   `maxNetworkError` was reached, the service scores the network exactly once on
   the evaluation data, computes the canonical metrics, and applies the
   evaluation accuracy threshold to decide whether to persist.

If `maxNumberOfTrainingEpochs` is exhausted without reaching `maxNetworkError`,
the evaluation phase MUST NOT run; the model is discarded as
completed-below-target and no evaluation metrics exist. Do not fit on
evaluation rows or fit preprocessing again for evaluation or inference.

Progress reporting is independent of stopping. An optional `reportingInterval`
request property (positive integer, in epochs) controls how often a progress
line is written to the server console; the default is every 10% of
`maxNumberOfTrainingEpochs`, or every epoch when fewer than ten. Progress lines
report only training-data values (Keras training loss, training error, and
training accuracy). A single additional line reports the evaluation metrics when
the evaluation phase runs. Reporting MUST NOT change when training stops.
When the maximum epoch count is reached without training error falling strictly
below `maxNetworkError`, log the final training error, target, and readable
training confusion matrix at INFO. Evaluation is not run in this case. Other
detailed metrics remain DEBUG.
Every completed batch iteration logs evaluation metrics and its confusion
matrix at INFO if evaluation ran; otherwise it logs training metrics and the
training confusion matrix.
The outcome explains whether the training error target and evaluation
performance threshold were met and whether the network was saved.
The final batch summary reports requested, started, and not-started iterations;
trained and saved networks; networks that did not meet the training target;
trained networks below the evaluation accuracy threshold; failures,
cancellations, training and evaluation years, total attempted-iteration time,
and average time per started iteration (including unsuccessful attempts, or
unavailable if none started). Trained includes both saved
and evaluation-rejected networks. These counts also appear in the batch end
event as specified in the interfaces and operations specification.
The summary also reports the structure algorithm, distinct hidden-layer counts
from completed layouts, updater, hidden and output activation functions,
L2 regularization lambda, and learning rate. If no iteration completed, the
hidden-layer count is unavailable.

**Locked terminology.** These terms MUST NOT be conflated in code, logs, JSON,
or docs. "Held-out" is not used; the data not trained on is the *evaluation
data*.

| Term | Definition | Data | Used for |
| --- | --- | --- | --- |
| `training_loss` | Keras per-epoch cross-entropy loss, the quantity backpropagation minimizes. Not a rate; not comparable to `maxNetworkError`. | training | Diagnostics only |
| `training_error` | `1 - macro recall` of the network's predictions on the training data (the legacy `computeNetworkError`). 0 means every outcome category is right on the training games. | training | Early stop: stop when strictly `< maxNetworkError` |
| `training_accuracy` | Fraction of training rows predicted correctly. | training | Diagnostics |
| `eval_error`, `eval_accuracy`, `eval_precision`, `eval_recall`, `eval_f1` | Canonical metrics on the evaluation data; `eval_error = 1 - eval_recall`. | evaluation | Persist when `eval_accuracy * 100 >= networkPerformanceThreshold` |
| `eval_composite_percent` | Mean of eval accuracy, precision, recall, F1, times 100. | evaluation | Diagnostic |

Rate-style values are fractions in 0-1; `training_loss` and percentage fields
are not. A very low `training_error` with a high
`eval_error` indicates overfitting, not a defect; the remedy is tuning
(regularization, layer sizes, epochs, `maxNetworkError`), not a code change. A
`training_error` can jump directly from above to far below `maxNetworkError`
(including to 0) between checks, particularly with `networkEvaluationIndex` of
1, so the stop point can undershoot the target.

Console output (server terminal) MUST use these exact field names. Progress
lines (`Training progress ...`) carry `sport`, `model_id`, `correlation_id`,
`epoch=<n>/<max>`, `training_loss`, `training_error`, `training_accuracy`,
`max_network_error`, and `decision` (`continue`, `monitoring_only`,
`early_stop`, or `maximum_epochs_reached`). The single `Network evaluation ...`
line carries the `eval_*` fields, `performance_threshold`, and
`qualifies_for_persistence`. Only milestone epochs, the stopping epoch, and the
final epoch print; `networkEvaluationIndex` schedules checks, never printing.

Cancellation MUST be checked before training starts, at every epoch boundary,
before persistence, and between batch iterations. An individual epoch is the
maximum cancellation latency for synchronous training. The service MUST emit
finish/failure/cancellation events and release model/array references when
done.

Seed Python's `random`, NumPy, and TensorFlow/Keras before model construction.
Enable TensorFlow deterministic operations where supported and configured.
Record the seed and resolved deterministic settings. Bitwise equality across
hardware/runtime versions is not promised; tests MUST use documented numeric
tolerances and must never equate a seed with guaranteed cross-version
identical weights.

### 5.3 Preprocessing and metrics

Implement versioned preprocessing in the domain layer, not by fitting an
untracked Keras or scikit-learn transform. Fit only on training features.
Persist the training-fitted state, including constant-column markers, and
reuse it unchanged for evaluation, tournament, and single-record inference.
Implement the three formulas, zero-denominator behavior, out-of-range
handling, and missing/non-finite rejection exactly as defined in the
system/domain specification.

Implement metrics in pure Python/NumPy, not Keras defaults:

- prediction class is argmax; a tie selects the lowest class index;
- confusion matrix rows are actual classes and columns are predicted classes;
- calculate accuracy and unweighted macro precision, recall, and F1 over the
  module's complete ordered class list;
- zero denominators yield zero for the affected metric;
- `realNetworkAccuracy` is the arithmetic mean of accuracy, precision,
  recall, and F1;
- the early-stop condition is `1 - recall < maxNetworkError`, where recall is
  the macro recall of the network scored on the **training data**, as in the
  legacy implementation. It is kept separate from the accuracy save threshold,
  which is computed from the **evaluation data**.

The threshold is a percent in `[0,100]`; persist when and only when early-stop
criteria have been reached and the evaluation-data `networkAccuracy * 100 >=
threshold`.
Use strict `<` for the early-stop comparison as specified. Record actual
stopping epoch, the training error at that epoch, the evaluation values, stop
reason, and threshold decision.

### 5.4 Configuration mapping for Keras

Load one immutable, fully validated configuration snapshot at startup and
copy model-affecting values into each training request and manifest. Support
sport-scoped overrides while keeping generic defaults. Fail startup with
field-specific diagnostics for missing required values or invalid
cross-field combinations. Never index a possibly empty list.

The settings model MUST include bind/body limits, resource/model/database
paths, sport/plugin registry, cache policies, job limits, seeds, layout
options, activations, loss, optimizer, initializer, normalizer, L2
regularization, learning rate, epoch and evaluation intervals, stopping and
performance thresholds, smoothing, evaluation-year prediction policy,
logging, health checks, and shutdown deadline.

Keras mappings MUST be explicit and tested. Unsupported/semantically
incompatible choices such as a loss/activation pair invalid for the selected
classification schema MUST fail validation before allocating a model. Store
the canonical resolved Keras identifiers and relevant numeric values rather
than only legacy strings.

## 6. Training, persistence, and batch behavior

### 6.1 Single training

The training service MUST execute this sequence:

1. Validate sport, descriptor fields, supported/disjoint years, labels,
   options, request limits, and resolved architecture.
2. Allocate a model UUID and correlation ID; record a start event.
3. Snapshot validated immutable configuration and resolve the module's schema.
4. Build training and evaluation arrays; fit preprocessing only on
   training features.
5. Build and train a new Keras model, checking cancellation at each epoch.
6. At configured intervals, score the network on the training data only and
   apply the early-stop rule. Stop when `maxNetworkError` is reached or
   `maxNumberOfTrainingEpochs` is exhausted. Do not touch the evaluation data
   in this step.
7. Only if training stopped by reaching `maxNetworkError`, score the network
   once on the evaluation data with the domain metrics.
8. Persist atomically only if early stopping and evaluation accuracy threshold both
   qualify. Otherwise record `completed_below_target`, return HTTP `200` with
   the unsaved training report from the single-training route, and discard all
   model state.
9. Emit terminal events and release transient state on every success,
   below-threshold, cancellation, and failure path.

Failures MUST be distinct from valid below-threshold completion. Repository
or backend failures MUST NOT be converted to an empty success. Model state
that is not persisted MUST be released when the training request completes.
Persisted models may be loaded for an operation and released afterward; an
optional bounded cache of loaded persisted models is out of scope for v1.

### 6.2 Keras artifact and atomic repository

Store each persisted model and its JSON descriptor/manifest under its
uppercase sport directory, without UUID subdirectories:

```text
<resource-root>/Networks/
  descriptor-index.sqlite
  <SPORT>/
    <model-uuid>.model.keras
    <model-uuid>.json
```

The JSON file is both the per-model index and the backend-neutral manifest.
It MUST preserve the complete returned descriptor and include the full contract in
the interfaces/storage spec: artifact/schema versions, UUID and sport,
ordered feature names/hash, output classes, exact architecture, backend and
runtime versions, preprocessing type/state, years, seed, resolved optimizer
and other hyperparameters, metrics, stop reason, threshold decision,
timestamps, and app version. JSON serialization MUST reject NaN/infinity and
be stable enough for deterministic tests.

Saving MUST be reader-atomic:

1. create a uniquely named temporary directory on the same filesystem;
2. save the Keras artifact and JSON manifest there;
3. flush/fsync files and directory where supported;
4. reload the temporary artifact through the Keras adapter and validate
   identity, shapes, preprocessor, and finite predictions;
5. publish the model without overwriting an existing file, then publish the
   JSON without overwriting; the JSON is the completion marker;
6. add the descriptor to the SQLite index in a transaction, serialized with
   reindexing and other publications.

Do not publish an index row before a valid artifact exists. On failure,
remove only the operation's specific temporary artifacts; never delete
another model or prior valid artifact. An incomplete pair MUST remain
undiscoverable; an unindexed complete pair MUST be recoverable by reindexing.
Reindexing MUST report incomplete or invalid pairs individually. A same-ID
collision across sports MUST fail without overwriting.

Loading MUST verify UUID, sport, schema hash/version, output classes,
dimensions, Keras/backend compatibility, fitted preprocessor, and finite
predictions before returning a model handle. Reject corrupt, truncated,
unknown-version, mismatched, or unsupported artifacts with typed errors.
Listing MUST use descriptors/index metadata and MUST NOT load model weights.
Reindex MUST scan and validate individual manifests, be idempotent, report
bad artifacts individually, and never destroy the only valid index before a
replacement index is validated.

The SQLite descriptor index MAY share the event database. Use transactional
tables and indexes for model UUID, sport, and sortable metadata. The
filesystem artifact is the source of truth; index reconstruction must be
possible from valid JSON files and their model artifacts. Legacy DL4J archives
are not loadable by this application; do not claim compatibility or mutate them.

### 6.3 Bounded batch jobs

Batch requests MUST be accepted with a positive bounded iteration count,
assigned a correlation/job ID before queue insertion, and return `202`.
Progress and terminal state are discoverable from durable management events.
Use a bounded in-process queue and a fixed number of worker slots configured
at startup; reject overload explicitly instead of creating unbounded tasks or
threads. The v1 deployment MUST run one Uvicorn worker process so queue and
training-concurrency limits are application-wide; multiple worker processes
or replicas require a separately specified shared job coordinator. Default to
one active TensorFlow training job unless deployment
configuration and measured tests justify a higher safe limit.

Each accepted batch captures an immutable versioned configuration snapshot.
Each iteration gets a fresh model ID and deterministic seed, emits correlated
events, and independently reports persisted, below-threshold, cancelled, or
failed outcome. Count these outcomes separately. Continue after a failed
iteration only according to an explicit batch policy. Check cancellation
before every iteration and at every epoch. Shutdown stops acceptance of new
batches, then drains or cancels active work before its configured deadline;
no partial model bundle may be exposed.

Do not rely on FastAPI's unbounded/default request thread behavior as a job
queue. CPU-bound synchronous Keras operations MUST run outside the event loop.
The application factory owns and closes the executor/worker resources through
FastAPI lifespan. No worker may be launched as an import-time side effect.

## 7. Data, event, and configuration repositories

### 7.1 Resource repository

Read all assets through an injected in-process resource repository. Validate
sport/year/file identifiers, resolve paths beneath configured read roots, and
reject absolute paths, `..`, symlink escapes, unsupported extensions, and
invalid identifiers. Missing data MUST produce explicit errors, never a
fabricated `"NO DATA"` sentinel or empty matrix. A missing file error MUST
include the full resolved path that was searched.

`SPORTSNET_RESOURCE_ROOT` is the directory that *contains* the sport
directories, so files resolve to `<root>/<SPORT>/data/<YEAR>/<file>` and the
NCAA alias file to `<root>/NCAABB/data/SeasonData-Alias.csv` (`SPORT` is
`NCAABB` or `MLB`). Pointing it at a sport directory itself is a common
mistake; startup or the first error message SHOULD make the expected layout
obvious. Tests MUST use synthetic CSV fixtures; production CSVs are external
deployment inputs.

Parsed-data caches, if enabled, MUST be bounded/thread-safe and obey explicit
TTL/capacity/invalidation rules. Cache parsed immutable data, never mutable
training matrices or Keras models. Cache metrics/evictions MUST be
observable.

### 7.2 Durable management events

Persist management events in SQLite using transactions. Index `eventId`,
`correlationId`, sport, type, and timestamp. Configure WAL, foreign keys,
busy timeout, and safe transaction boundaries. Open a connection per
operation (or use an equivalent rigorously thread-safe strategy); do not
share a default thread-bound sqlite connection across FastAPI worker threads.
Event insertion and index changes MUST be atomic.

Default retention is no age-based expiration. If retention is configured,
eviction is explicit and observable. Never maintain the authoritative event
history solely in memory. Concurrent create/search and process restart MUST
preserve the documented behavior.

Implement all management routes, filter-intersection semantics, repeated
event-type set semantics, deterministic ordering, positive `mostRecentN`,
summary grouping, eligible metric denominators, extrema and tie rules, status
derivation, and legacy field names from the interfaces/storage spec. Lifecycle
events without model descriptors remain searchable. Do not persist secrets,
credentials, full model bytes, or entire large CSV rows in event context.

### 7.3 Configuration and observability

Validate configuration during application startup before reporting ready.
Expose only redacted diagnostics. Required settings and legacy defaults are
listed in the system/domain and interfaces/storage specs; every chosen
replacement default MUST be recorded in the sample configuration and covered
by default/invalid-config tests.

Runtime state MUST NOT be committed. The default model root
(`resources/Networks/`, derived from the default resource root) and
the default SQLite event database (`var/events.sqlite`, plus its `-wal`,
`-shm`, and journal companions) are generated at runtime relative to
`python-app/`. `python-app/.gitignore` MUST ignore `resources/Networks/` and `var/*`
(together with virtual environments, tool caches, build output, and coverage
artifacts). If the configured defaults change, the ignore rules MUST change
with them. Tests MUST write only to temporary directories, never to these
default locations, and a test SHOULD fail if the ignore rules are missing.

Emit structured logs with timestamp, severity, component, service ID,
correlation ID, event ID, sport, model ID, batch iteration, and elapsed time
when applicable. Do not include stack traces, local absolute paths, secrets,
raw credentials, full descriptors with sensitive context, or model bytes in
client error bodies. Internally log exceptions with correlation IDs. Request
validation failures return `400` with the `code`/`message`/`details` contract
from the interfaces spec (FastAPI/Pydantic's `RequestValidationError` is
converted by an app-level handler into `details` entries without submitted
values) and are logged at warning level with method, path, correlation ID, and
the field/reason list.

Provide liveness and readiness endpoints. Liveness MUST not train or load all
data. Readiness MUST verify validated configuration and writable model/event
storage and any required enabled-plugin assets. Startup fails visibly for
invalid configuration, unusable storage, or missing required enabled-plugin
data.

## 8. HTTP and wire contracts

### 8.1 FastAPI construction

Build the application through `create_app(settings, dependencies)` and a
FastAPI lifespan context. Routers receive application services through
dependency injection. Do not use global mutable service singletons.

Register NCAA, MLB, management, and health routers under the paths below.
Sport-specific route handlers MUST only perform transport validation,
invocation, and response mapping. Configure request-body limits and content
types. Generate OpenAPI from explicit Pydantic request/response DTOs; do not
serialize domain dataclasses or NumPy arrays implicitly.

### 8.2 Required routes

| Method and path | Contract |
|---|---|
| `GET /ncaabb/networks` | `200`, persisted descriptor metadata only; deterministic order. |
| `GET /ncaabb/networks/{networkId}` | UUID validation; persisted lookup only; unknown ID returns the legacy empty optional with `200`. |
| `POST /ncaabb/networks` | Synchronous bounded training; `200` with a persisted trained descriptor, or an unsaved training report (`networkSaved: false`, with every training-data metric and confusion matrix and, when the evaluation phase ran, every evaluation-data metric and confusion matrix) if training completes without meeting persistence criteria. The latter is not an error and does not retain a model. |
| `POST /ncaabb/networks/batch` | Bounded async acceptance; `202` with job/correlation ID assigned before queueing. |
| `POST /ncaabb/simulations` | `{networkIds, tournamentYear}` request; deterministic plugin-defined CSV/text response; reject invalid/empty ID lists unless a documented default-selection policy is configured. |
| `GET /ncaabb/tournamentYears` | Enabled NCAA plugin descriptors only. |
| `GET /ncaabb/trainingYears` | Usable labeled years from the NCAA participant manifest with matching season data. |
| `GET /ncaabb/evaluate/networks/{networkId}/{evaluationYear}` | Persisted model evaluation with saved training preprocessor. |
| `GET /mlb/networks` | `200`, persisted descriptor metadata only. |
| `GET /mlb/networks/{networkId}` | Persisted lookup only; unknown ID returns the legacy empty optional with `200`. |
| `POST /mlb/networks` | Synchronous bounded training; `200` with a persisted descriptor or the unsaved training report if completed below threshold. Persist only qualifying results. |
| `POST /mlb/networks/batch` | Bounded async acceptance; `202` and assigned job/correlation ID. |
| `POST /mlb/simulations` | MLB plugin-defined text response; currently enabled years are 2009 and 2023, output type `TEAM_RANKING`. |
| `GET /mlb/tournamentYears` | Enabled MLB plugin descriptors only. |
| `GET /mlb/trainingYears` | Usable labeled years from the MLB participant manifest with matching season data. |
| `POST /management/events` | Validate event; assign event ID and default correlation ID; `201`. |
| `GET /management/events` | Retained events in deterministic time order. |
| `GET /management/events/{eventId}` | Event or `404`. |
| `GET /management/searchEvents` | AND of all supplied filters; requires correlation ID, sport, or event type; positive `mostRecentN` when supplied. |
| `GET /management/stats` | Same filters; summaries grouped by correlation ID. |

Unknown query parameters SHOULD be rejected.

Preserve legacy descriptor JSON field names, types, threshold precedence,
timestamps at compatibility boundaries, stable confusion-matrix CSV, and
event/summary names specified in the interface/storage spec. Resolve
`networkPerformanceThreshold` before `networkThreshold`, then configuration;
return both legacy aliases with the resolved value. Generate UUIDs, dimensions,
timestamps, metrics, saved state, and resolved architecture server-side.

When training completes without a qualifying persisted network, return HTTP
`200` with JSON `null`. The outcome is recorded in management events and is
not an error response. Backend or repository failures remain errors and MUST
NOT be represented as `null`.

Map validation/schema/enum/path UUID/year errors to `400`; unknown required
models/files to `404` except the explicit legacy empty-optional lookup; storage,
Keras, corruption, and unexpected failures to `500`; accepted batch to `202`;
created durable event to `201`. Use one stable JSON error schema, for example
`{"code": "...", "message": "...", "correlationId": "..."}`. Do not expose
tracebacks, absolute paths, credentials, or backend internals.

The NCAA and MLB targets MAY be selected by configuration or deployed as
separate ASGI entry points, but the normal deployment is one application.
When configured as separate targets, each MUST mount only its selected sport
routes while retaining shared management behavior as documented. Never
register both handlers for the same path or introduce sport-name branches in
generic service code.

### 8.3 Tournament requirements

Tournament orchestration loads only persisted model IDs, validates sport and
year eligibility, applies each model's fitted preprocessor, checks finite
output shape/class order, and emits explicit warnings/events for skipped
corrupt, unknown, or ineligible models. Deduplicate requested IDs before
aggregation; repeated IDs MUST NOT multiply scores. If no valid model remains,
return an explicit failure, not an empty successful ranking.

Keep all sport-specific scoring and rendering in registered plugins. For
NCAA, preserve the specified distance formula, sum across models, rank
descending with canonical team-name ascending tie-break, CSV header and
four-decimal score format. Bracket formatting and tie rules are those in the
system/domain spec. MLB ranking behavior is plugin-owned. Aggregation MUST be
deterministic and race-free regardless of input order or worker completion
order. Only explicitly enabled plugins are exposed; the default NCAA plugin
is the 2024 ranking plugin.

Before any real tournament request proceeds to model inference, validate the
actual configured tournament inputs against the selected module's declared
CSV contract. Check header row count, data row widths, numeric values (filling
blank statistics with zero), duplicate/canonical
team keys, and all required participant/team joins. A layout mismatch or
invalid required row MUST fail the request before any prediction or output is
produced; never return partial rankings or picks. Record a correlated failure
event with a source-located diagnostic.

## 9. Test-first development specification

### 9.1 Required workflow

Every vertical slice MUST follow this loop:

1. Write a failing test that expresses a named requirement and boundary case.
2. Run only the targeted test and confirm it fails for the intended missing
   behavior (not a broken fixture/import).
3. Implement the smallest production change that makes the test pass.
4. Run the targeted test, then the affected layer's suite.
5. Refactor with tests green; run formatting, lint, and static type checks.
6. Add/update API snapshots, manifests, docs/examples, or fixtures affected
   by the behavior.

Do not implement the application broadly and add tests afterward. Do not mock
the unit under test. Prefer pure domain tests; inject fakes at ports. Keep
real TensorFlow, filesystem/SQLite, and HTTP tests in their appropriate
integration layers.

### 9.2 Test layers and mandatory coverage

**Domain/unit tests (no FastAPI, filesystem, SQLite, or TensorFlow):**

- exact NCAA 64-feature order/indexes, duplicate index 21, seven classes;
- exact MLB 78-feature order, six outputs, `E` to errors mapping, alias
  canonicalization, and row merges;
- parser behavior for documented CSV contracts, optional fields, and
  provider-defined aggregate rows using synthetic fixtures;
- deterministic season/participant joins, year manifests, missing/duplicate
  rows, label bounds, and train/evaluation disjointness;
- layout algorithms (`HOUR_GLASS`, `TAPERED_TREE`, `USER_DEFINED`,
  `USER_DEFINED_RAND`): the request-validation matrix with no defaults for
  `networkStructureAlgorithm` or `numberOfHiddenLayers`, `numberOfHiddenLayers
  >= 3`, ignored-parameter combinations, exact shapes for `H` of 3, 4, 5, 7,
  and 20, seeded determinism, malformed user layouts, invalid widths/counts,
  overflow, and parameter limits;
- all normalizers, constant columns, fitted-state roundtrip, non-finite input,
  and proof that evaluation/inference do not refit;
- argmax and tie behavior, confusion matrix, precision/recall/F1/accuracy,
  macro averages, zero division, composite score, and stable CSV;
- early-stop strictness and performance threshold as independent decisions;
- early stopping driven by training-data error only, with the evaluation data
  scored exactly once and only after `maxNetworkError` is reached, and never
  when the epoch limit is exhausted;
- `reportingInterval` controlling progress output without changing stopping;
- NCAA smoothing off by default and exact multiplicities when enabled;
- training outcomes: success/persisted, completed-below-threshold/no model,
  cancellation, and typed failure.

**Repository tests (temporary roots and SQLite):**

- CSV quoting, headers, CRLF/LF, blank rows, whitespace, exact row widths,
  malformed numeric values, duplicate keys, aliases, and source diagnostics;
- explicit supported-year enumeration and missing-year behavior;
- path traversal, absolute paths, symlink escapes, and invalid resource IDs;
- atomic artifact save/reload, identity/schema mismatch, corrupt/truncated/
  unsupported artifacts, index failure, orphan handling, and idempotent
  reindex without loss of valid records;
- durable event ordering/search/indexing across restart, concurrent access,
  retention behavior, and atomic create/search visibility.

**Fake-backend application tests:**

- valid train/persist, below-threshold discard, failed save, repository and
  backend failures, cancellation, event correlation, and no success-shaped
  error response;
- descriptor-only listing does not load model weights;
- lookup/evaluation/simulation only use persisted artifacts;
- batch bounded capacity, ID assignment before execution, per-iteration IDs,
  configuration snapshots, counters, failure policy, cancellation, and
  shutdown;
- no service retains a model handle after a non-persisted outcome;
- extension contract for NCAA, MLB, and the synthetic third sport;
- management filters, summaries, sparse metric denominators, event ordering,
  ties, and lifecycle events without descriptors.

**Keras adapter conformance/integration tests:**

- exact input/output shapes for both production sport schemas;
- reject invalid shapes, unsupported options, and incompatible
  activation/loss combinations;
- fixed-seed repeatability within documented tolerance;
- one-epoch training behavior, cancellation boundary, and no evaluation-row
  updates;
- finite prediction shape `(row_count, class_count)` and valid class order;
- `.keras` save/load roundtrip through a fresh adapter instance with
  prediction parity within an explicit tolerance;
- manifest completeness and runtime-version compatibility checks;
- tiny deterministic synthetic end-to-end train/evaluate/save/reload/predict.

Do not assert that a random model meets a high performance threshold. Test
threshold logic with a controlled fake backend and numeric metrics with known
labels/predictions.

**FastAPI contract tests:**

- use FastAPI's in-process test client; assert every route/method, request
  schema, status, content type, response shape, JSON field name/type, and
  event side effect;
- assert completed-below-threshold training returns `200` with the unsaved
  training report,
  while backend/repository failures return errors and never expose an
  unpersisted descriptor or model;
- assert resource access uses in-process repositories;
- assert unknown persisted lookup's legacy empty result, invalid UUID/year/
  enum errors, stable error body, and no traceback/path/credential leakage;
- assert batch response includes an ID before work can finish and that events
  have the same ID;
- assert simulation output is byte-for-byte stable on deterministic fixtures;
- assert management filter intersection and lifecycle-event search.

**Deployment/end-to-end checks:**

- clean locked install, lint, type-check, unit/integration/API suites;
- start with synthetic fixture data and temporary writable roots;
- verify startup validation, liveness/readiness, train/evaluate/persist/
  restart/reload/simulate, batch/events, graceful shutdown, and no partial
  artifacts;
- verify no production credentials, external service, or untracked dataset
  is needed to run tests.

### 9.3 Test organization and commands

Use pytest markers for `unit`, `repository`, `keras`, `api`, and `e2e`.
Document concise reproducible commands in project configuration/README:

```text
pytest -m unit
pytest -m "repository or api"
pytest -m keras
pytest -m e2e
ruff check .
ruff format --check .
mypy src
```

CI MUST run all non-hardware-specific tests on each change. CSV contract tests
MUST use synthetic fixtures and verify header skipping, data widths, parsing,
and optional-field behavior. Keras tests MUST run on the supported CPU
configuration. GPU tests are optional and cannot replace CPU contract
coverage. A test skipped due to missing optional hardware MUST NOT cause the
corresponding CPU conformance test to be skipped.

## 10. Implementation sequence and acceptance gates

Implement in this order; each milestone requires its tests to be green before
the next is started:

1. **Project skeleton and tooling:** `pyproject.toml`, locked dependencies,
   `src/` package, Ruff/type-check/pytest CI, application factory stub, and
   clean install test.
2. **Pure domain:** immutable records, typed errors, module protocols,
   registry validation, metrics, layouts, preprocessing, threshold/stopping
   rules. Complete domain unit tests before adding I/O.
3. **Sport schemas and parsers:** exact NCAA/MLB feature and label mappings,
   explicit year manifests, aliases, parser diagnostics, and synthetic CSV
   fixtures. Pass both production and synthetic module contract suites.
4. **Repository adapters:** filesystem resources, SQLite event/index
   repository, manifest format, atomic Keras artifact repository interface
   exercised first with a fake backend. Pass failure/rebuild/restart tests.
5. **Application services with fakes:** training, persisted-only model
   lookup, evaluation, batch execution, tournaments, and management. Prove
   events, cancellation, bounds, and persistent model lifecycle
   before exposing HTTP.
6. **Keras adapter:** implement the protocol, option mapping, deterministic
   one-epoch training, predictions, and `.keras` persistence. Pass adapter
   conformance and fresh-instance roundtrip tests.
7. **FastAPI adapters:** add NCAA/MLB/management routes, Pydantic DTOs,
   validation/error mapping, lifespan setup/teardown, OpenAPI and contract
   tests. Verify the documented routes and schemas in OpenAPI.
8. **Operational hardening:** validated sample configuration, redacted
   diagnostics, structured logging, readiness, bounded concurrency, graceful
   shutdown, artifact recovery, and clean deployment tests.
9. **Parity and data migration:** on an authorized copy of real data, compare
   deterministic parsing, feature rows, layouts, metrics, and simulation
   output. Measure expected differences from train-only normalization and
   Keras/runtime changes; do not write into live Java artifact directories.
   Retrain models because DL4J weights are not portable by default.
10. **Release gate:** install from the lockfile in a clean environment and
    pass all required checks below.

The application is complete only when:

- all three existing specs' applicable domain/API/operations requirements are
  implemented and tested;
- NCAA, MLB, and synthetic third-sport module-contract suites pass without
  sport-specific branches in generic services;
- exact feature/class order and parser mappings are fixture asserted;
- no evaluation leakage or inference-time preprocessing refit is possible;
- persisted artifacts are self-describing, Keras-loadable, atomic, and
  rejected when incompatible;
- below-threshold models are not returned, saved, cached, or otherwise
  retained after their request;
- management history/search and model metadata survive restart;
- all specified API routes have contract tests;
- deterministic tournament output matches fixtures;
- startup, readiness, bounded batch processing, cancellation, and shutdown
  are demonstrated in a clean environment;
- the locked install and documented commands work without production data,
  credentials, or external services.
