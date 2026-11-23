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
- every supported network-layout identifier, deterministic seeded random
  variants, malformed/user-defined layouts, overflow, zero/negative widths,
  and impossible layer counts;
- feature normalizers, fitted-state round trip, constant columns, NaN/infinity,
  missing data, and reuse of training-fitted state for evaluation and
  inference;
- class prediction (`argmax`) and tie behavior, sport-sized confusion
  matrices, accuracy/precision/recall/F1, macro averaging, zero-division
  handling, composite score, and stable CSV formatting;
- early stopping (`1 - recall < maxNetworkError`) separately from the
  composite performance threshold and save decision;
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
- blank lines, quoted commas, CRLF/LF, whitespace, malformed numbers, short
  and long rows, duplicate teams, duplicate years, and empty files are either
  handled according to the parser contract or rejected with file/year/line/
  column diagnostics;
- NCAA supported year enumeration matches the explicit manifest (season years
  2009–2018 and 2020–2024; participant/training years 2010–2018 and
  2020–2023) and never invents missing 2019 data;
- MLB season-stat years 2009–2023 and participant/training years 2010–2022
  match its explicit manifests; batting, pitching, and fielding CSV rows have
  29, 36, and 19 fields respectively, and participant rows have two fields;
- MLB team aliases map `Cleveland Indians` to `Cleveland Guardians` and
  `Los Angeles Angels of Anaheim` to `Los Angeles Angels`;
- alias resolution is deterministic, unambiguous, and reports unknown or
  conflicting aliases;
- resource paths reject absolute paths, `..`, symlink escapes, unsupported
  extensions, and invalid identifiers;
- parsed-data caches return equivalent values, are thread-safe, and obey
  documented invalidation/TTL/capacity semantics;
- model save/load preserves model identity, descriptor, manifest, preprocessing,
  and predictions; corrupted, truncated, unsupported-version, and mismatched
  identity artifacts fail explicitly;
- write failure leaves no discoverable partial model; reindex is idempotent
  and does not destroy valid existing indexes on failure;
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
dataset, evaluate on held-out examples, save, reload in a fresh adapter
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
- candidate visibility, expiration/capacity, lookup, save success, retriable
  save failure, and candidate consumption only after successful persistence;
- no evaluation data influencing fitted preprocessing or training updates;
- batch start, each iteration, final summary, per-iteration failure policy,
  cancellation before next iteration, deterministic results despite completion
  order, and parent/per-iteration correlation IDs;
- tournament scoring of multiple valid models, filtering of ineligible or
  corrupt models, duplicate-ID behavior, stable ranking, and no data races;
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

- `/resources` routes are absent; no internal repository operation uses HTTP;
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
7. verify no `/resources` route is exposed and no resource/model dependency
   requires another process;
8. verify shutdown leaves no partial artifact and follows the configured
   batch drain/cancellation policy.

## 3. Golden fixtures and parity protocol

Create version-controlled fixtures containing:

- a tiny season CSV for each season-stat schema, with at least two teams and
  deliberately distinct opponent values;
- MLB batting, pitching, and fielding season CSV fixtures plus participant
  rows that cover all observed victory labels and the two canonical aliases;
- aliases including one valid alternate name and one ambiguous/invalid case;
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

1. **Inventory deployment inputs.** Locate production season data, aliases,
    participants, properties, templates, model archives, and indexes outside
    the code repository. Do not infer that missing checked-in files are empty
    in production. Record checksums and ownership without copying secrets into
    the repository.
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
5. **Expose retained APIs.** Add NCAA, MLB, and management routes and contract
    tests. Verify there are no `/resources` endpoints or internal loopback HTTP
    calls.
6. **Run shadow comparison.** On an authorized copy of real input data, compare
    data rows, feature vectors, layout resolution, metrics, and simulation
    outputs. Keep inference/training runs isolated; never write replacement
    artifacts into the live legacy model directory.
7. **Rebuild models.** Since DL4J weights are not portable by default, retrain
    with recorded seeds/configuration or keep old archives accessible only to
    the legacy runtime. Evaluate on held-out years and document metric shifts.
8. **Cut over with rollback.** Back up source data, event history, indexes,
    and legacy artifacts. Deploy the new app separately, validate readiness
    and smoke tests, switch traffic after they pass, and retain a tested
    rollback path.
9. **Retire old services.** Remove resource and management service
    dependencies only after repository access, event behavior, monitoring,
    and operational ownership are verified in the single application.

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
- no active `/resources` route or required internal HTTP service remains;
- startup, readiness, bounded concurrency, graceful shutdown, and rollback
  are demonstrated in a clean deployment environment;
- any observed parity differences are measured and documented alongside
  expected behavior and test coverage.
