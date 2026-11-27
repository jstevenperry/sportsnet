# Interfaces, Persistence, and Operations Specification

This document defines the externally visible interfaces and internal
repository/operational contracts. It is normative together with
[the master specification](design-spec.md).

## 1. HTTP surface

The routes in this section define the current production sport APIs (NCAA
basketball and MLB) plus shared management operations. They do not constrain
the framework to these sports: a later sport module MAY register its own
versioned API adapter under a sport-specific route family, reusing application
services and the module's registered schema. Sport-specific transport code
MUST remain at the API boundary and MUST NOT introduce sport-name branches
into generic training, persistence, or event services.

### 1.1 Route policy

The application exposes the NCAA basketball API, MLB API, and management API.
Season data, properties, templates, network artifacts, metadata indexes, and
reindex operations use in-process repositories and services.

The API MUST preserve the legacy route spelling and JSON field names specified
below. The handler MUST return a stable error body and status code; do not
expose stack traces or backend internals.

### 1.2 NCAA basketball routes

| Method and path | Request | Successful response | Required semantics |
|---|---|---|---|
| `GET /ncaabb/networks` | None | `200`, persisted descriptor array | Return metadata only, not weights; sort deterministically. |
| `GET /ncaabb/networks/{networkId}` | UUID path segment | `200`, descriptor or empty result | Read only from persistent repository. Invalid UUID is `400`; unknown ID returns the legacy empty optional with `200`. |
| `POST /ncaabb/networks` | Network descriptor JSON | `200`, persisted trained descriptor or unsaved training report | Run one training request synchronously. Return the descriptor when the model is persisted after meeting the evaluation accuracy threshold; otherwise return the unsaved training report (§1.6 below), so every run reports its metrics, and retain no model. Invalid parameters are `400`; backend/repository failure is `500`. Do not report a model success when training failed. |
| `POST /ncaabb/networks/batch` | Network descriptor with positive `numberOfBatchIterations` | `202`, accepted descriptor containing `batchTrainingRunId` | Assign the correlation/job ID before queuing work. The response MUST NOT race generation of the ID. Progress and terminal state are discoverable through management events. |
| `POST /ncaabb/simulations` | `{ "networkIds": [UUID, ...], "tournamentYear": integer }` | `200`, ranking or bracket CSV/text body | Reject absent year, unknown plugin year, malformed IDs, and invalid simulation requests. Emit start/end/exception events. |
| `GET /ncaabb/tournamentYears` | None | `200`, array of `{ tournamentYear, pluginDescription, outputType }` | Include only registered/enabled plugins, with output type (`BRACKET` or `TEAM_RANKING`). |
| `GET /ncaabb/trainingYears` | None | `200`, integer array | Return years declared by the NCAA participant manifest that have usable labels and matching season inputs. |
| `GET /ncaabb/evaluate/networks/{networkId}/{evaluationYear}` | UUID and integer year | `200`, evaluated descriptor | Evaluate persisted or otherwise accessible model on the requested year, using the model's training-fitted preprocessor. Unknown model/year is `404`/`400`; invalid evaluation is an explicit error. |

`networkIds` in simulation MAY be empty only if a documented implementation
policy defines a default selection; otherwise reject an empty list. Deduplicate
IDs before aggregation. Duplicate IDs MUST NOT increase a team's score merely
because the caller repeated an ID.

The simulation request is exactly `{ "networkIds": [UUID, ...],
"tournamentYear": integer }`. The simulation response is text; the output
format is determined by the selected plugin and SHOULD be identified by
`Content-Type` or a documented response header in the replacement.

### 1.3 MLB routes

The current MLB controller uses the same generic service operations under
`/mlb`, with the MLB module's 78-input, six-output schema. It does not expose
the NCAA evaluation route.

| Method and path | Request | Successful response | Required semantics |
|---|---|---|---|
| `GET /mlb/networks` | None | `200`, persisted descriptor array | List persisted metadata only. |
| `GET /mlb/networks/{networkId}` | UUID path segment | `200`, descriptor or empty result | Read only from persistent repository; unknown ID is empty optional with `200`. |
| `POST /mlb/networks` | Network descriptor JSON | `200`, persisted trained descriptor or unsaved training report | Train one model using MLB features and six declared outputs. Return the descriptor when the model is persisted after meeting the evaluation accuracy threshold; otherwise return the unsaved training report and retain no model. |
| `POST /mlb/simulations` | `{ "networkIds": [UUID, ...], "tournamentYear": integer }` | `200`, plugin-produced text | Supported plugin years are 2009 and 2023; output type is `TEAM_RANKING`. |
| `GET /mlb/tournamentYears` | None | `200`, plugin descriptor array | Return registered MLB plugins only. |
| `GET /mlb/trainingYears` | None | `200`, integer array | Return years declared by the MLB participant manifest that have usable labels and matching season inputs. |
| `POST /mlb/networks/batch` | Descriptor with positive `numberOfBatchIterations` | `202`, accepted descriptor | Queue the batch and assign its correlation/job ID before work starts. |

MLB's current source DTO does not add a different wire schema. The same
descriptor compatibility rules below apply, but the server resolves dimensions
to 78 inputs and six outputs from the MLB module and rejects any mismatch.

### 1.4 Network descriptor wire model

The compatibility JSON model MUST recognize these legacy fields. Fields
marked runtime/result are typically populated by the service; request
validation MUST distinguish caller-controlled values from server-owned values.

| Field | Meaning |
|---|---|
| `networkId` | UUID identity assigned before a model is trained. |
| `numberOfInputs`, `numberOfOutputs` | Resolved dimensions; NCAA is 64 and 7, MLB is 78 and 6. The selected sport module owns these values. |
| `yearsToTrainNetwork`, `yearsToEvaluateNetwork` | Explicit disjoint year lists. |
| `networkLayout` | `x`-separated full layout. Required for `USER_DEFINED[_RAND]`; ignored for `HOUR_GLASS` and `TAPERED_TREE`. In the descriptor, the resolved layout. |
| `activationFunction`, `outputLayerActivation` | Hidden and output activation identifiers. |
| `lossFunction`, `outputLayerLossFunction` | General and output loss identifiers. |
| `numberOfEpochs`, `maxNumberOfTrainingEpochs`, `networkEvaluationIndex`, `maxNetworkError`, `epochStatsIndexCount` | Runtime training controls and observed result count. |
| `reportingInterval` (training request only, optional, replacement addition) | Positive integer epoch cadence for console progress lines; does not affect stopping. Default is every 10% of `maxNumberOfTrainingEpochs`. Invalid (zero/negative) values are rejected as `400`. |
| `weightInit`, `updater`, `learningRuleLearningRate`, `beta1DecayScaleValue`, `l2RegularizationLambda` | Model construction and optimizer parameters. |
| `normalizer` | Feature preprocessing choice; artifact also stores fitted state. |
| `networkType` | Sport/model type; NCAA legacy value is `NCAABB`. |
| `whenTrained` | Legacy local date-time string format `yyyy-MM-dd HH:mm:ss`; replacement SHOULD use an unambiguous UTC timestamp in a versioned API. |
| `networkSaved` | Whether artifact persistence completed. |
| `realNetworkAccuracy` | Mean of accuracy, precision, recall, and F1, on 0–1 scale. |
| `networkAccuracy`, `networkPrecision`, `networkRecall`, `networkF1Score` | Metrics on the evaluation data, each on 0–1 scale. They exist only when training reached `maxNetworkError` and the evaluation phase ran. |
| `trainingError` | Legacy `1 - recall`, scored on the **training data**, at the epoch where training stopped. This is the value compared with `maxNetworkError`. It is stored with the model and never recomputed: re-evaluating a model on another year updates the evaluation metrics but leaves `trainingError` unchanged. |
| `networkThreshold`, `networkPerformanceThreshold` | Legacy aliases/related threshold fields; define one internal threshold and document alias precedence. |
| `networkStructureAlgorithm`, `numberOfHiddenLayers` | `networkStructureAlgorithm` is required (`HOUR_GLASS`, `TAPERED_TREE`, `USER_DEFINED`, `USER_DEFINED_RAND`; no default). `numberOfHiddenLayers` is required and `>= 3` for `HOUR_GLASS` and `TAPERED_TREE`, ignored for the `USER_DEFINED` algorithms. Violations return `400`. |
| `numberOfBatchIterations`, `batchTrainingRunId` | Batch size and correlation ID. |
| `confusionMatrixCsv` | Stable confusion-matrix CSV (header `Actual\Predicted,<classes>`, one row per actual class) scored on the **evaluation data**. |
| `trainingAccuracy`, `trainingPrecision`, `trainingRecall`, `trainingF1Score`, `realTrainingAccuracy`, `trainingConfusionMatrixCsv` | The same metrics and confusion matrix scored on the **training data** at the epoch where training stopped (`trainingError = 1 - trainingRecall`). |

Additional legacy DTO fields and constraints:

- `realNetworkAccuracy` is derived from the four metric fields; never accept a
  contradictory client value as authoritative.
- The source descriptor maps both `networkThreshold` and
  `networkPerformanceThreshold` in conversion code. The replacement MUST
  use `networkPerformanceThreshold` when present, otherwise fall back to
  `networkThreshold`, otherwise use the validated configuration default.
  When both are present but differ, the canonical field wins; responses emit
  the same resolved value in both fields.
- `whenTrained` is a local-time string without an offset in the legacy format.
  New persistence MUST use UTC/offset-aware time; compatibility formatting
  must not change the stored instant.
- Server-generated outputs include UUIDs, resolved dimensions/layout, metrics,
  timestamp, training epoch count, training error, and saved state.
- Reject negative/duplicate/out-of-range years, train/evaluation overlap,
  dimensions inconsistent with the schema, invalid enum names, non-finite
  numbers, probability values outside their valid range, and incompatible
  activation/loss/output combinations.

### 1.5 Management API

The management API remains available at `/management`; the `ManagementService`
becomes an in-process service used by training, batch, save, evaluation, and
tournament workflows as well as by these routes.

| Method and path | Request/query | Successful response |
|---|---|---|
| `POST /management/events` | Event body | `201`, decorated event; assign event UUID and, if missing, use event UUID as correlation UUID. |
| `GET /management/events` | None | `200`, all retained events in deterministic time order. |
| `GET /management/events/{eventId}` | Event UUID | `200`, event; unknown/expired ID is `404`. |
| `GET /management/searchEvents` | Optional `correlationId`, `sport`, repeated `managementEventType`, `mostRecentN` | `200`, matching events. At least one of correlation ID, sport, or type is required (`400` otherwise). |
| `GET /management/stats` | Same filters as event search | `200`, summaries grouped by correlation ID. |

An event body includes:

`serviceId`, `eventId`, `correlationId`, `managementEventType`,
`systemTimeMillis`, `elapsedTimeMillis`, `networkDescriptor`, `sport`,
`hostName`, `message`, and `additionalContext`. Require a valid event type and
non-empty host name when accepting externally posted events. Internally, the
application MUST generate consistent host/service identity and event time.
The legacy DTO also exposes computed `systemTimestamp` formatted as
`MM-dd-yyyy HH:mm:ss` in the host's local time zone. The v1 compatibility
response preserves this field; a versioned API SHOULD expose an additional
unambiguous UTC timestamp.

Supported event types from source:

`TRAIN_NETWORK_START`, `TRAIN_NETWORK_END`, `TRAIN_NETWORK_BATCH_START`,
`TRAIN_NETWORK_BATCH_ITERATION_END`, `TRAIN_NETWORK_BATCH_END`,
`RUN_SIMULATION_START`, `RUN_SIMULATION_END`, `REINDEX_START`, `REINDEX_END`,
`GET_ALL_NETWORKS`, `GET_NETWORK_BY_ID`, `SAVE_NETWORK`,
`FETCH_RESOURCE_FILE`, `EXCEPTION`, `PING`.

The replacement may add event types but MUST preserve the listed names in a
compatibility API.

At batch completion, the server logs a summary and emits
`TRAIN_NETWORK_BATCH_END`. Its `additionalContext` contains `persisted`,
`completedBelowThreshold`, `failed`, `cancelled`, `requestedIterations`,
`startedIterations`, `notStarted`, `trained`, `trainingTargetNotMet`,
`evaluationBelowThreshold`, `totalTrainingElapsedMillis`,
`averageIterationElapsedMillis`, `trainingYears`, `evaluationYears`,
`networkStructureAlgorithm`, `hiddenLayerCounts`, `updater`,
`hiddenActivation`, `outputActivation`, `l2`, and `learningRate`. The hidden
layer counts are the distinct counts observed in completed iteration layouts,
or an empty list if no iteration completed. `trained`
counts returned outcomes that reached the training error target, whether or
not they were persisted. `trainingTargetNotMet` counts returned outcomes that
exhausted their epochs; `evaluationBelowThreshold` counts trained outcomes not
saved due to evaluation accuracy. The duration sums attempted iteration times
and excludes queue time. Average iteration time divides that duration by
started iterations, including failed and cancelled attempts; it is null if no
iteration started. `cancelled` includes cancellation detected before an
iteration begins, and an error after a training outcome returns can be counted
as a failure as well as a trained or persisted outcome.

Search semantics MUST be the intersection (AND) of all supplied filters.
Management event types are a set of accepted values. Sort by creation time;
`mostRecentN` MUST be positive when supplied and selects the latest N matching
events. Search MUST NOT silently discard lifecycle events merely because they
lack a model descriptor or training timestamp. The legacy implementation does
discard such events; correcting that behavior is required for useful
start/end/failure event search.

Each summary is grouped by correlation ID and includes:

- service ID, sport, host, event count, status, start and finish time;
- total and average elapsed batch-iteration time;
- number of saved models;
- total and average accuracy, precision, recall, F1, and composite score;
- lowest/highest event for each reported metric;
- fastest/slowest iteration event.

The legacy summary wire field names are `serviceId`, `correlationId`, `sport`,
`hostName`, `numberOfEvents`, `numberOfSavedNetworks`, `status`, `whenStarted`,
`whenFinished`; `totalBatchIterationTime`; `totalNetworkAccuracy`,
`totalNetworkPrecision`, `totalNetworkRecall`, `totalF1Score`,
`totalRealNetworkAccuracy`; `averageBatchIterationTime`,
`averageNetworkAccuracy`, `averageNetworkPrecision`, `averageNetworkRecall`,
`averageF1Score`, `averageRealAccuracy`; and the
`highestRealNetworkAccuracyEvent`, `lowestRealNetworkAccuracyEvent`,
`highestNetworkAccuracyEvent`, `lowestNetworkAccuracyEvent`,
`highestNetworkPrecisionEvent`, `lowestNetworkPrecisionEvent`,
`highestNetworkRecallEvent`, `lowestNetworkRecallEvent`,
`highestF1ScoreEvent`, `lowestF1ScoreEvent`,
`fastestBatchIterationTimeEvent`, and `slowestBatchIterationTimeEvent`.

All average denominators MUST be the count of eligible values for that metric,
not the count of unrelated events. Define stable tie behavior, null handling,
zero-event behavior, and start/finish/status derivation. Test them. The legacy
summary DTO exposes computed average fields and extrema; it does not justify
using event count as denominator when some events have no metrics.

### 1.6 Error mapping

At minimum, map:

- malformed JSON, invalid schema/enum/path UUID, invalid request parameters,
  impossible year combinations, or missing required filters → `400`;
- unknown model or missing required file → `404`;
- unsupported artifact/backend format → an explicit client or server error
  according to whether the caller supplied the invalid format;
- storage corruption, read/write failure, model load/predict failure, or
  unexpected implementation failure → `500`;
- accepted asynchronous batch → `202`;
- newly created durable resource/event → `201` where the corresponding route
  creates it.

Response bodies MUST have a stable machine-readable error code/message. Log
internal exceptions with correlation IDs. Do not include secrets, raw
credentials, stack traces, or large model bytes in client error messages.

Every `400` MUST tell the caller why. The error body carries `code`,
`message`, `correlationId`, and, for request-body or parameter validation
failures, a `details` array with one entry per problem: `field` (the JSON
property name as the client sent it, with a dotted path for nested values) and
`message` (a human-readable reason, for example `Field required` or `numberOfHiddenLayers of at least
3 is required for TAPERED_TREE`). `details` MUST NOT echo submitted values. The
top-level `message` summarizes the first problem so that clients showing only
`message` still see the cause. Every `400` MUST also be logged on the server at
warning level with the HTTP method, path, correlation ID, and the same field
and reason list, and MUST NOT log request bodies or submitted values.
Unknown JSON request properties MUST be ignored, not rejected, so callers can
"comment out" a parameter by renaming it (for example
`"networkLayout-ignored"`); an ignored property is never an error and never
changes behavior.

## 2. Internal repositories (no resource REST API)

### 2.1 Resource/data repository

Required in-process capabilities:

- fetch UTF-8 text and parsed CSV by safe relative resource key;
- provide a sport-neutral repository contract, with each sport module
  supplying its data-file layout, parser, aliases, participant data where
  applicable, and supported-year manifest;
- read optional sport-specific tournament game/result templates and
  runtime properties through that module's registered configuration;
- enumerate or validate supported years from the registered module's
  explicit manifest;
- cache parsed data with defined invalidation and thread-safety;
- distinguish missing files from malformed files and report source path,
  year, line, and column.

Paths MUST be constructed from validated sport/year/file identifiers and
resolved beneath configured read roots. Reject absolute paths, `..`, symbolic
link escapes, and unsupported extensions. A missing file MUST NOT be converted
to a fabricated empty matrix or `"NO DATA"` sentinel for internal consumers.

### 2.2 Model repository and artifact index

Required methods are equivalent to:

- `persist_model(qualified model) -> persisted descriptor` (the name is illustrative; it MUST differ from the backend's weight-saving method);
- `load(id) -> descriptor + backend model + preprocessor`;
- `get_descriptor(id)`;
- `list_descriptors(sport, filters)`;
- `rebuild_index(sport)` as an internal command/maintenance operation.

The repository contains persisted models only. Training models that do not
meet the evaluation accuracy threshold are discarded after their outcome is
recorded; they MUST NOT be returned or retained after the training request.

Reindexing MUST be idempotent and MUST NOT delete the only valid index before
replacement indexes have been built and validated. Preserve atomicity and
report corrupt/unsupported artifacts individually.

Each artifact MUST include:

- artifact format and schema version;
- model UUID, sport/type, resolved architecture, and model input/output shape;
- backend name/version and required runtime compatibility;
- feature schema version and ordered feature names/hash;
- output class order;
- preprocessing type and fitted parameters;
- training/evaluation years and random seed;
- resolved optimizer/activation/loss/initializer/regularization values;
- evaluation metrics, the training-data `trainingError`, stopping reason,
  threshold decision, timestamps, and source application version.

Store the model at `SPORTSNET_RESOURCE_ROOT/Networks/<SPORT>/<uuid>.model.keras`
and the per-model JSON index/manifest at
`SPORTSNET_RESOURCE_ROOT/Networks/<SPORT>/<uuid>.json`, where `SPORT` is the
uppercase sport code.
The JSON contains the complete descriptor as well as the manifest fields above.
Write both to temporary paths, fsync where supported, validate the model by
reloading, then publish the model and finally the JSON as the completion
marker, without overwriting existing files. Publish a SQLite search-index row
only after both files are complete. Serialize publishing with reindexing.
Incomplete pairs MUST remain undiscoverable and be reported by reindexing;
complete unindexed pairs MUST be recoverable. Model lookup by sport and UUID
MUST verify that artifact identity matches its manifest.

Existing artifacts are DL4J `.zip` archives with an embedded
`network.parameters` descriptor and JSON indexes in a separate directory. These may be
retained read-only as archives; the new app MUST NOT claim to load them in a
different framework. Network file names may change. If existing file-backed
indices are retained, create an explicit migration/import and rollback
strategy.

### 2.3 Event repository

The source management cache is bounded at 100,000 events and has no active
expiration policy configured; indexes are maintained separately in memory.
The replacement MUST persist event history across restarts, using a
transactional event repository indexed by `eventId`, `correlationId`, `sport`,
type, and time. The storage engine is implementation-specific. Apply a
configurable retention limit/age only if it is explicit in deployment
configuration and reported operationally; do not silently lose events on
restart.

Never use an unbounded in-memory event list in a long-lived application.
Event insertion and search-index updates MUST be atomic from callers'
perspective. Concurrent creates/searches MUST be safe. Preserve insertion
ordering by timestamp plus a stable ID tie-breaker.

## 3. Operational requirements

### 3.1 Configuration

The legacy YAML defines port 8287, NCAA URI `/ncaabb`, multipart limits,
Jackson serialization, and actuator settings. The unified runtime uses
in-process resource repositories and the durable management-event store.

Use environment variables, a validated configuration file, or both. Required
operational settings include:

- bind address/port and allowed request/body sizes;
- resource read root, model artifact root, index location, and event-store
  location;
- active sport and explicit enabled tournament plugin registry;
- cache TTL/capacity and parsed-data cache policy;
- training/batch work limits, kill switch/cancellation, worker/concurrency
  limits, and random-seed policy;
- network structure algorithms, activations, losses, optimizers, normalizer,
  weight initialization, L2, learning rate, epoch limits/evaluation
  interval, error/performance thresholds;
- smoothing flags and evaluation-year prediction policy;
- logging level/format and health/readiness behavior.
- routine HTTP access logs are DEBUG-level and suppressed by default; the
  access log level is configurable independently of training logs and errors.

Configuration MUST be validated at startup and exposed in a redacted
diagnostic form. Secrets MUST come from a secret provider/environment and MUST
never be logged. Config fields that affect a model or batch MUST be copied into
its manifest/event snapshot. No code path may assume that a missing list-valued
setting contains element zero.

### 3.2 Execution and concurrency

Training is CPU/GPU-expensive and MUST NOT run on an unbounded web request
thread pool. Single-request training may be synchronous if bounded and
explicitly configured; batch training MUST use a bounded job executor or
worker. The application MUST enforce:

- maximum active training jobs;
- maximum examples, epochs, batch iterations, and request payload size;
- cancellation and graceful shutdown semantics;
- per-job correlation/log context;
- deterministic aggregation independent of worker completion order.

Model objects and fitted preprocessors MUST be immutable after publication or
protected during inference. Shared model caches MUST be bounded/thread-safe.
Do not share mutable training model state between concurrent jobs.

### 3.3 Health, startup, and shutdown

Startup MUST fail visibly if required configuration is invalid, an enabled
plugin references missing required data, or model/event storage is unusable.
Large datasets may load lazily, but readiness MUST not claim availability
before the required dependencies are usable.

Provide a liveness check that does not trigger model training or full dataset
loads and a readiness check covering configuration and writable model/event
storage. Graceful shutdown MUST stop accepting new batches, request
cancellation or drain according to a configured deadline, and leave artifacts
atomic.

### 3.4 Logging and observability

Log structured event records with timestamp, severity, component, service ID,
correlation ID, event ID, sport, model ID, batch iteration, and elapsed time
where applicable. Do not log full model binaries, sensitive runtime config, or
entire large CSV rows. Metrics SHOULD cover active/queued jobs, duration,
success/failure/stopping reasons, models persisted, repository errors, cache
size/evictions, and tournament models skipped.

Training progress and the single post-training evaluation line are operator
output as defined under "Progress reporting" in the system and domain spec;
they use the locked terminology (training loss/error/accuracy versus evaluation
metrics) and MUST NOT print on every epoch unless the configured interval says
so.

## 4. Compatibility contract

The replacement MUST follow these contracts consistently across both sport
applications:

1. Unknown `GET /ncaabb/networks/{id}` returns `200` with the legacy empty
   optional result. Invalid UUID input remains `400`.
2. `networkPerformanceThreshold` is canonical; if absent, accept
   `networkThreshold`, then the configured default. If both are present,
   canonical wins. Return both legacy fields with the resolved value.
   Model artifacts use the replacement's internal format; DL4J loading is not
   supported.
3. If training completes without meeting both the legacy early-stop condition
   (training-data error below `maxNetworkError`) and the performance threshold
   (evaluation accuracy × 100), the single-training endpoint returns
   `200` with an unsaved training report (`networkSaved: false`) carrying
   `networkId`, `networkType`, `stopReason`, `numberOfEpochs`, the thresholds,
   the year sets, `elapsedTimeMillis`, `trainingError`, and every
   training-data metric (including `trainingConfusionMatrixCsv`), plus, only
   if the evaluation phase ran, every evaluation-data metric (including
   `confusionMatrixCsv`). If `maxNetworkError` was never reached, the
   evaluation data is never scored and the evaluation metrics are absent.
   Record the internal outcome distinctly from failure. A persisted descriptor
   is returned only for a model persisted after meeting both conditions; no
   model is retained otherwise.
4. Management events persist across process restarts. Apply no age-based
   expiration by default; any configured capacity/retention policy must emit
   observable eviction information.
5. Only the 2024 ranking plugin is enabled by default. Other plugin classes
   remain inactive unless explicitly enabled with data and fixtures.
6. Preserve smoothing off by default, legacy smoothing multiplicities when
   enabled, ranking CSV header/precision, and bracket formatting/tie policy
   documented in the domain specification. Ranking ties sort by canonical
   team name ascending. Model prediction ties select the lowest class index.
7. Management events are owned by the unified app and durable event
   repository; no separate management service is required.

The route/schema snapshots, artifact manifests, and test fixtures MUST encode
these contracts.
