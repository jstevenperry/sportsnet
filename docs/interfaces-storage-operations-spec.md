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
It MUST NOT implement, proxy, redirect, or internally call `/resources/...` routes.
Season data, properties, templates, network artifacts, metadata indexes, and
reindex operations are in-process repository/service operations.

The legacy `/ncaabb` API includes a `resourceType` query parameter for saving a
trained candidate. Retain the route and parameter only as an NCAA API
compatibility contract; it MUST NOT reintroduce a resource-service REST
surface. Translate the legacy `DL4J_NETWORK` value to the new internal model
artifact type only if compatibility requires it. New clients SHOULD use the
versioned artifact type defined by the replacement.

The API MUST preserve the legacy route spelling and JSON field names specified
below. The handler MUST return a stable error body and status code; do not
expose stack traces or backend internals.

### 1.2 NCAA basketball routes

| Method and path | Request | Successful response | Required semantics |
|---|---|---|---|
| `GET /ncaabb/networks?unsaved={bool}` | Optional boolean; absent/false lists persisted descriptors, true lists in-memory candidates | `200`, descriptor array | Return metadata only, not weights; sort deterministically. |
| `GET /ncaabb/networks/{networkId}` | UUID path segment | `200`, descriptor or empty result | Check unsaved cache, then persistent repository. Invalid UUID is `400`; unknown ID returns the legacy empty optional with `200`. |
| `POST /ncaabb/networks` | Network descriptor JSON | `200`, trained descriptor or explicit no-model outcome | Run one training request synchronously. Invalid parameters are `400`; backend/repository failure is `500`. Do not report a model success when training failed. |
| `POST /ncaabb/networks/{networkId}?resourceType=...` | Candidate UUID and compatible artifact-type parameter | `201`, persisted descriptor | Persist only a known unsaved candidate. Unknown/expired candidate is `404`; an unsuccessful write is `500` and does not consume candidate. |
| `POST /ncaabb/networks/batch` | Network descriptor with positive `numberOfBatchIterations` | `202`, accepted descriptor containing `batchTrainingRunId` | Assign the correlation/job ID before queuing work. The response MUST NOT race generation of the ID. Progress and terminal state are discoverable through management events. |
| `POST /ncaabb/simulations` | `{ "networkIds": [UUID, ...], "tournamentYear": integer }` | `200`, ranking or bracket CSV/text body | Reject absent year, unknown plugin year, malformed IDs, and invalid simulation requests. Emit start/end/exception events. |
| `GET /ncaabb/tournamentYears` | None | `200`, array of `{ tournamentYear, pluginDescription, outputType }` | Include only registered/enabled plugins, with output type (`BRACKET` or `TEAM_RANKING`). |
| `GET /ncaabb/trainingYears` | None | `200`, integer array | Return years that have usable labeled participant data and matching season inputs. |
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
| `GET /mlb/networks?unsaved={bool}` | Optional boolean | `200`, descriptor array | `unsaved=true` lists in-memory candidates; otherwise list persisted metadata. |
| `GET /mlb/networks/{networkId}` | UUID path segment | `200`, descriptor or empty result | Check candidate cache then persistent repository; unknown ID is empty optional with `200`. |
| `POST /mlb/networks` | Network descriptor JSON | `200`, optional trained descriptor | Train one model using MLB features and six declared outputs. |
| `POST /mlb/networks/{networkId}?resourceType=...` | Candidate UUID and resource type | `201`, persisted descriptor | Persist an unsaved MLB candidate only. |
| `POST /mlb/simulations` | `{ "networkIds": [UUID, ...], "tournamentYear": integer }` | `200`, plugin-produced text | Supported plugin years are 2009 and 2023; output type is `TEAM_RANKING`. |
| `GET /mlb/tournamentYears` | None | `200`, plugin descriptor array | Return registered MLB plugins only. |
| `GET /mlb/trainingYears` | None | `200`, integer array | Return participant/training years with usable MLB data. |
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
| `networkLayout` | Legacy `x`-separated resolved layout or normalized user input. |
| `activationFunction`, `outputLayerActivation` | Hidden and output activation identifiers. |
| `lossFunction`, `outputLayerLossFunction` | General and output loss identifiers. |
| `numberOfEpochs`, `maxNumberOfTrainingEpochs`, `networkEvaluationIndex`, `maxNetworkError`, `epochStatsIndexCount` | Runtime training controls and observed result count. |
| `weightInit`, `updater`, `learningRuleLearningRate`, `beta1DecayScaleValue`, `l2RegularizationLambda`, `dropoutProbability` | Model construction and optimizer parameters. |
| `normalizer` | Feature preprocessing choice; artifact also stores fitted state. |
| `networkType` | Sport/model type; NCAA legacy value is `NCAABB`. |
| `whenTrained` | Legacy local date-time string format `yyyy-MM-dd HH:mm:ss`; replacement SHOULD use an unambiguous UTC timestamp in a versioned API. |
| `networkSaved` | Whether artifact persistence completed. |
| `realNetworkAccuracy` | Mean of accuracy, precision, recall, and F1, on 0–1 scale. |
| `networkAccuracy`, `networkPrecision`, `networkRecall`, `networkF1Score` | Held-out metrics, each on 0–1 scale. |
| `trainingError` | Legacy `1 - recall` at stopping/evaluation point. |
| `networkThreshold`, `networkPerformanceThreshold` | Legacy aliases/related threshold fields; define one internal threshold and document alias precedence. |
| `networkStructureAlgorithm`, `numberOfHiddenLayers` | Architecture-generation choice and layer count. |
| `numberOfBatchIterations`, `batchTrainingRunId` | Batch size and correlation ID. |
| `confusionMatrixCsv` | Stable confusion-matrix CSV for compatibility. |

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
compatibility API. Since no resource HTTP endpoint exists in the replacement,
`FETCH_RESOURCE_FILE` may be retained only for old event ingestion; new
internal resource reads need not emit that API-era event.

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
- unknown model, expired unsaved candidate, or missing required file → `404`;
- unsupported artifact/backend format → an explicit client or server error
  according to whether the caller supplied the invalid format;
- storage corruption, read/write failure, model load/predict failure, or
  unexpected implementation failure → `500`;
- accepted asynchronous batch → `202`;
- newly persisted candidate/event → `201` where the corresponding route
  creates a durable resource.

Response bodies MUST have a stable machine-readable error code/message. Log
internal exceptions with correlation IDs. Do not include secrets, raw
credentials, stack traces, or large model bytes in client error messages.

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
The old resource controller used a sentinel in one HTTP response path; that is
not the internal repository contract.

### 2.2 Model repository and artifact index

Required methods are equivalent to:

- `save(candidate) -> persisted descriptor`;
- `load(id) -> descriptor + backend model + preprocessor`;
- `get_descriptor(id)`;
- `list_descriptors(sport, filters)`;
- `rebuild_index(sport)` as an internal command/maintenance operation.

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
- metrics, stopping reason, threshold decision, timestamps, and source
  application version.

Persist model data and manifest together, or use a manifest state marker that
is written last. Write to a temporary path, fsync where supported, validate by
reloading, then atomically promote. Do not expose a descriptor index for an
artifact that failed validation. Model lookup by sport and UUID MUST verify
that artifact identity matches its manifest.

Existing artifacts are DL4J `.zip` archives with an embedded
`network.parameters` descriptor and adjacent JSON indexes. These may be
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
Jackson serialization, and actuator settings. The old resource and management
hosts/ports point to localhost:8299. Those internal service URLs MUST be
removed from the unified runtime configuration.

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
  weight initialization, dropout, L2, learning rate, epoch limits/evaluation
  interval, error/performance thresholds;
- smoothing flags and evaluation-year prediction policy;
- logging level/format and health/readiness behavior.

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

## 4. Compatibility contract

The replacement MUST follow these contracts consistently across both sport
applications:

1. Unknown `GET /ncaabb/networks/{id}` returns `200` with the legacy empty
   optional result. Invalid UUID input remains `400`.
2. `networkPerformanceThreshold` is canonical; if absent, accept
   `networkThreshold`, then the configured default. If both are present,
   canonical wins. Return both legacy fields with the resolved value.
   `DL4J_NETWORK` is accepted only as a legacy NCAA save-route value and maps
   to the new internal artifact type; it does not imply DL4J loading support.
3. If training completes but the legacy early-stop condition is never met,
   the legacy single-training endpoint returns `200` with an empty optional.
   Record the internal outcome distinctly from failure. If early stopping is
   reached but the composite save threshold is not, return an unsaved
   candidate.
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
