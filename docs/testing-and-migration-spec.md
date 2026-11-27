# Verification and Migration Specification

This document defines the required test suite and conversion gates for the
language- and ML-framework-independent application. It is normative together
with [the master specification](design-spec.md).

## 1. Test principles

1. Tests MUST be deterministic, hermetic, and runnable without production
   credentials, external service processes, or untracked data files.
2. Check in small synthetic fixtures for all required CSV and tournament
   formats. Use fixed seeds and stable serialization.
3. Keep pure domain tests independent of HTTP frameworks, file systems, and ML
   libraries. Use in-memory repositories and a fake ML adapter for application
   orchestration tests.
4. Run filesystem/database and real ML-backend integration tests separately;
   they MUST be runnable in CI and have documented resource requirements.
5. Tests MUST cover defects corrected by this specification and each specified
   difference from legacy behavior.
6. Assert exact values and output shapes where the specification defines them;
   do not substitute “request succeeded” for required semantic checks.
7. Identifiers chosen at code-generation time (types, functions, parameters,
   variables, fields, and persisted keys) MUST NOT collide with, shadow, or be
   ambiguous with similarly named members of the selected backend or its
   libraries, and MUST name what they actually hold. For example, a repository
   method that persists a model MUST NOT be named like a backend method that
   saves model weights, and a value scored on training data MUST NOT carry a name
   that suggests evaluation data.

## 2. Required test layers

### 2.1 Domain unit tests

Test at minimum:

- NCAA: all 64 feature names, order, and indices; seven output classes;
  labels 0 through 6; the intentional duplicate feature at index 21;
- MLB: all 78 feature values in order; six declared outputs; label bounds,
  team-name canonicalization, and batting/pitching/fielding merge behavior;
- numeric feature assembly, opponent values, aliases, and season/team joins;
- NCAA and MLB participant label validation, class bounds, and
  duplicate/unknown team behavior;
- train/evaluation disjointness and tournament-year eligibility, including
  configured evaluation-year prediction;
- every supported network-layout algorithm (`HOUR_GLASS`, `TAPERED_TREE`,
  `USER_DEFINED`, `USER_DEFINED_RAND`): the parameter-validation matrix
  (missing algorithm, missing `networkLayout` or `numberOfHiddenLayers`,
  `numberOfHiddenLayers < 3`, ignored parameter combinations, `I < O`),
  exact worked-example shapes for `H` of 3, 4, 5, 7, and 20, the drawn-value
  bounds for `P`, `N`, and `F`, seed determinism, malformed user layouts,
  overflow, and zero/negative widths;
- feature normalizers, fitted-state round trip, constant columns, NaN/infinity,
  missing data, and reuse of training-fitted state for evaluation and
  inference;
- class prediction (`argmax`) and tie behavior, sport-sized confusion
  matrices, accuracy/precision/recall/F1, macro averaging, zero-division
  handling, composite score, and stable CSV formatting;
- progress reporting: default 10% cadence, `reportingInterval` override and
  validation, milestone/stop/final-epoch printing only, no printing driven by
  `networkEvaluationIndex`, and no effect on stopping;
- unknown JSON request properties are ignored and never cause an error;
- every `400` response explains the cause (missing field, bad value, rule
  violation) in `message` and `details`, never echoes submitted
  values, and produces a server warning log with correlation ID;
- resource-base layout and missing-file errors that include the resolved path;
- early stopping (`1 - recall < maxNetworkError`, with recall measured on the
  **training data**) separately from the accuracy performance threshold
  (measured on the evaluation data) and save decision, including threshold
  equality and cases where accuracy and composite fall on opposite sides; the
  evaluation data is scored once, only after early stopping, and never when the epoch limit is
  exhausted; below-threshold
  completion returns no model and retains no model after the request;
- NCAA smoothing disabled by default; when enabled, exact added multiplicities
  for classes 1–6 (1, 3, 7, 15, 31, 63) plus the original sample, with no
  unintended mutation of source rows;
- NCAA tournament distance scoring, score aggregation, ranking order/ties, output
  CSV header `Rank,Team Name,Tournament Score`, and eligibility filtering;
- management-event filter intersection, repeated-type set semantics,
  ordering, `mostRecentN`, correlation grouping, eligible metric denominators,
  status derivation, extrema/ties, and events without model descriptors.

Property-based or table-driven tests SHOULD cover broad valid/invalid ranges
for numerical inputs and layouts, in addition to the concrete boundary cases.

### 2.2 Data/repository tests

Use temporary roots and synthetic fixtures to verify:

- NCAA basic and advanced season formats have exactly 38 and 34 fields per
  data row, respectively, and support two header rows;
- participant format has exactly two fields and one header row;
- blank and whitespace-only numeric season statistics in every NCAA and MLB
  input file become zero for training, evaluation, and inference; malformed or
  non-finite nonblank numbers, missing team names, and blank participant
  labels are rejected;
- NCAA training and evaluation warn and exclude unmatched teams and
  participants only in affected years, while tournament inference rejects
  incomplete joins and an empty training dataset fails;
- NCAA training and evaluation select only each year's tournament participants
  before parsing season statistics and ignore nonparticipant join and numeric
  errors; aliases are applied before selection;
- blank lines, quoted commas, CRLF/LF, whitespace, malformed numbers, short
  and long rows, duplicate teams, duplicate years, and empty files are either
  handled according to the parser contract or rejected with file/year/line/
  column diagnostics;
- NCAA supported year enumeration matches the explicit manifest (season years
  2009–2018 and 2020–2024; participant/training years 2010–2018 and
  2020–2023) and never invents missing 2019 data;
- CSV fixtures use synthetic team names and values while covering documented
  paths, header skipping without name validation, data widths, parsing rules,
  and field-level structure;
- alias resolution is deterministic and independent of row order, supports
  multiple aliases per team and shared aliases across rows (for example one
  team with three historical names), skips leading `#` comment lines, rejects
  two season records resolving to one team, and reports unknown aliases and
  missing alias files with the resolved path;
- resource paths reject absolute paths, `..`, symlink escapes, unsupported
  extensions, and invalid identifiers;
- parsed-data caches return equivalent values, are thread-safe, and obey
  documented invalidation/TTL/capacity semantics;
- repository and integration tests MUST use small synthetic fixtures and MUST
  not require external production datasets;
- model save/load preserves model identity, descriptor, manifest, preprocessing,
  and predictions; corrupted, truncated, unsupported-version, and mismatched
  identity artifacts fail explicitly;
- write failure leaves no discoverable partial model; reindex is idempotent
  and does not destroy valid existing indexes on failure;
- model and JSON files use exactly `Networks/<SPORT>/<uuid>.model.keras` and
  `Networks/<SPORT>/<uuid>.json`, with uppercase sport codes; test sport
  isolation, collision handling, incomplete pairs, index reconstruction and
  publication/reindex concurrency;
- event insertion/search/indexing are atomic and concurrent, with retention
  and restart semantics matching the selected storage mode.

### 2.3 ML-backend conformance and integration

For every selected backend, run the same adapter contract for both sport
schemas:

- build the requested architecture and report exact input/output shapes;
- reject invalid shapes and unsupported options explicitly;
- fixed seed yields repeatable initialization and training within documented
  numerical tolerances;
- one epoch consumes only the supplied training rows; cancellation is checked
  at epoch boundaries;
- prediction returns finite scores with shape `(row_count, sport_class_count)`
  and that sport's stable class order; probabilities, if exposed, are valid
  and sum to one within tolerance;
- save/load round trip reproduces predictions within backend-specific
  tolerances and verifies backend/version compatibility;
- fitted preprocessing is not refit by evaluation, tournament, or prediction
  calls;
- empty, malformed, NaN, and infinite inputs produce typed failures, not
  success-shaped outputs.

Real-backend end-to-end tests MUST train on a tiny deterministic synthetic
dataset, evaluate on the separate evaluation examples, save, reload in a fresh adapter
instance, and predict. They MUST assert dimensions, finite metrics, manifest
completeness, and prediction parity within an explicitly configured tolerance.
Do not assert that a random model meets a high performance threshold; test
threshold logic with a controlled fake backend and test numeric metrics with
known labels/predictions.

### 2.4 Application service tests

Use fake repositories and a fake backend to cover:

- valid training success, early-stop success, max-epoch completion below
  threshold, invalid request, repository failure, backend failure, and
  cancellation;
- persisted-model listing and lookup, atomic persistence of qualifying
  training results, and assertions that below-threshold results return an unsaved
  training report with all the run's metrics and confusion matrices (and no descriptor/model) and retain
  no model after the request;
- no evaluation data influencing fitted preprocessing or training updates, and
  no evaluation-data prediction before early stopping is reached;
- batch start, each iteration, final summary counts distinguishing training
  target failures from evaluation rejections, per-iteration failure policy,
  cancellation during or before the next iteration, deterministic results despite completion
  order, and parent/per-iteration correlation IDs;
- tournament scoring of multiple valid models, filtering of ineligible or
  corrupt models, duplicate-ID behavior, stable ranking, and no data races;
- invalid tournament-data row widths, required values, or team joins
  fail the request before any model prediction; no partial ranking or bracket
  output is returned, and a correlated failure event is recorded;
- lifecycle events always emitted for start/end/failure paths and accurately
  correlated, including exceptions before model creation.

Run sport-extension contract tests against NCAA and MLB, and against a
synthetic sport module whose feature width, class count, data parser, and
supported years differ from both. Verify each can register, train/evaluate
through shared workflows, persist and reload a sport-bound model, and reject
loading that model through another sport module. These tests MUST require no
sport-name conditionals in generic training, evaluation, model storage, or
event services. Verify optional capabilities (such as tournament
participation) can be absent without breaking unrelated workflows.

### 2.5 HTTP contract tests

Run the API adapter against an in-process server or framework test client.
Assert route methods/paths, query/body names, JSON serialization, status
codes, content types, stable error schemas, and event side effects for every
route in the interface specification.

Additional required checks:

- resource and model data access uses in-process repositories;
- NCAA, MLB, and management routes remain reachable under configured base
  paths in their respective selected application targets;
- the MLB target does not activate NCAA routes/controllers and vice versa;
- descriptor input/output snapshots contain legacy JSON field names and types;
- UUID, enum, year, and numeric validation map to `400`; unknown resources map
  to the specified `404`/empty compatibility choice; persistence/backend failures
  never return success-shaped bodies;
- batch returns `202` with an ID that exists before work can finish, and emits
  lifecycle events with that ID;
- simulation output is byte-for-byte stable for deterministic fixture data
  and matches the formatting specified for its sport/plugin;
- malformed tournament input returns an error before inference and never
  returns partial picks;
- management filtering applies every supplied filter, and lifecycle events
  without model metadata remain searchable;
- error messages contain no stack trace, local absolute path, credential, or
  raw model bytes.

### 2.6 End-to-end and deployment checks

In a clean environment:

1. install/build from documented instructions;
2. run unit and integration test suites;
3. start with fixture data and temporary writable storage;
4. verify liveness/readiness and all health requirements;
5. train, evaluate, persist, restart, reload, and run a tournament simulation;
6. run a batch and query its management events;
7. verify resource/model dependencies and storage are available in the
   unified deployment;
8. verify shutdown leaves no partial artifact and follows the configured
   batch drain/cancellation policy.

## 3. Golden fixtures and parity protocol

Create version-controlled fixtures containing:

- a tiny season CSV for each season-stat schema, with at least two teams and
  deliberately distinct opponent values;
- MLB batting, pitching, and fielding season CSV fixtures plus participant
  rows that cover all observed victory labels and the two canonical aliases;
- aliases including a team with several alternate names, a shared alias, and one ambiguous/invalid case;
- NCAA participant rows for all seven labels, plus MLB rows covering every
  valid observed label; include malformed and duplicate cases for both;
- a training/evaluation split with disjoint years and a tournament fixture;
- a small tournament bracket/template and a ranking output fixture;
- known targets and predictions for a hand-computed confusion matrix;
- network descriptors for every layout and important option boundary;
- sample management lifecycle events with missing optional metrics and tied
  timestamps.

For source-parity-sensitive behavior, record the Java version/build and
configuration that generated each golden output. Compare the replacement
with:

- parsed rows and exact feature vector values/order;
- architecture widths and normalized configuration;
- confusion matrix and metric definitions;
- tournament eligibility, per-model score, aggregate ranking, ties, and CSV;
- descriptor wire JSON and event-summary schema.

Never compare stochastic training weights or claim model-score parity merely
because both implementations use the same seed. Different runtimes, kernels,
initializers, and optimizer implementations can yield different weights.
Compare deterministic data transformations and controlled predictions first;
then measure model-level parity over repeated seeds and report tolerance and
distribution. The train-only normalization correction is expected to change
legacy evaluation/tournament results and MUST be measured separately, not
hidden by recalibrating the fixture.

If source code fails or cannot be executed for a legacy golden case, record
the failing input, observed exception/ambiguity, and replacement behavior
specified here. Do not fabricate a legacy expected output.

## 4. Migration sequence

1. **Inventory deployment inputs.** Locate authorized production season data,
   aliases, participants, properties, templates, model archives, and indexes.
   Do not infer that missing files are empty in production. Record checksums
   and ownership without copying secrets into the repository.
2. **Build the generic framework and fixture suite.** Implement sport-module
    registration and contracts, schema/versioning, repositories, validation,
    deterministic transforms, metrics, and shared workflows. Add NCAA as the
    reference module, MLB as the second concrete module, and the synthetic
    extension-contract module. Require generic, NCAA, and MLB domain/data tests
    to pass before adding an ML backend.
3. **Implement repository and adapter contracts.** Add filesystem/database
    repositories, artifact manifests, backend adapter, fake adapter, and
    conformance suite. Keep legacy DL4J archives read-only unless a separately
    tested import tool is included.
4. **Implement application workflows.** Add model lifecycle, evaluation,
    batch, tournament execution, and management events/services; prove them
    with fakes before adding HTTP.
5. **Expose APIs.** Add NCAA, MLB, and management routes with contract tests.
6. **Run shadow comparison.** On an authorized copy of real input data, compare
    data rows, feature vectors, layout resolution, metrics, and simulation
    outputs. Keep inference/training runs isolated; never write replacement
    artifacts into the live legacy model directory.
7. **Rebuild models.** Since DL4J weights are not portable by default, retrain
    with recorded seeds/configuration or keep old archives accessible only to
    the legacy runtime. Evaluate on separate evaluation years and document metric shifts.
8. **Cut over with rollback.** Back up source data, event history, indexes,
    and legacy artifacts. Deploy the new app separately, validate readiness
    and smoke tests, switch traffic after they pass, and retain a tested
    rollback path.
9. **Verify operational ownership.** Confirm the unified application owns
    repository access, event behavior, monitoring, and operational procedures.

## 5. Acceptance gates

The replacement is ready for production only when all gates pass:

- all domain, parser, repository, API, backend-conformance, and end-to-end
  required tests pass in CI;
- the synthetic alternate sport completes extension-contract tests with
  different input/output dimensions and without changes to generic workflows;
- all 64 feature positions and seven classes are asserted by golden fixtures;
- no train/evaluation split leakage or inference-time preprocessor refit is
  detected by explicit tests;
- all model artifacts are reloadable and self-describing, with atomic failure
  behavior tested;
- management search and statistics match their documented semantics under
  sparse/missing fields and concurrent access;
- NCAA, MLB, and management API snapshots/errors match the contracts in the
  interface specification;
- deterministic simulation output matches the specified fixtures;
- startup, readiness, bounded concurrency, graceful shutdown, and rollback
  are demonstrated in a clean deployment environment;
- any observed parity differences are measured and documented alongside
  expected behavior and test coverage.
