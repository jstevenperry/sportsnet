# Sportsnet Language- and ML-Framework-Independent Design Specification

## Purpose and authority

This is the master specification for replacing the Java application in
`src/main/java/com/jstevenperry/sportsnet` with one application implemented in
any suitable programming language and using any suitable deep-learning
framework. It is intentionally not a Python/Keras implementation plan.
Implementations MUST preserve the domain, data, API, and operational contracts
below, while isolating language and ML-library choices behind adapters.

The specification is split into focused documents:

1. [System and domain behavior](system-and-domain-spec.md) — architecture,
   sport-extension contracts, NCAA and MLB schemas, training and tournament
   algorithms, and the model-backend boundary.
2. [Interfaces, persistence, and operations](interfaces-storage-operations-spec.md)
   — retained HTTP contracts, internal resource repositories, model artifact
   lifecycle, management events, configuration, and error semantics.
3. [Verification and migration](testing-and-migration-spec.md) — required test
   layers, concrete guard-rail cases, parity strategy, acceptance gates, and
   conversion sequence.

## Scope

The replacement is a **single deployable application** combining the shared
framework, the NCAA basketball and MLB sport modules, resource/data access,
model storage, and management-event responsibilities. Sport modules remain
separate internally; the deployment MAY select one sport application at a
time. The application includes resource/data access and management-event
responsibilities in the same deployment. Resource files, properties, model
artifacts, indexes, and reindex operations use in-process repositories.
Preserve the NCAA basketball API, MLB APIs, and management API defined in the
interface specification.

The shared framework is an **extensible sport-neutral core**. NCAA men's
basketball is the reference sport module, and MLB provides a second concrete
implementation of the same extension pattern. Neither defines the limits of
the framework. Each sport provides its own data model/schema, repositories,
feature/label mapping, configuration, and optional tournament plugins while
reusing generic training, model lifecycle, persistence, event, and evaluation
workflows. Adding a sport MUST NOT require copying or forking those generic
workflows.

In scope:

- define reusable sport-extension contracts and implement NCAA men's
  basketball and MLB as separate sport modules;
- load and validate sport-specific season statistics, participants, aliases,
  configuration, and optional tournament templates;
- construct supervised training/evaluation datasets;
- build, train, evaluate, serialize, load, and run feed-forward classifiers;
- batch experimentation and persistence of qualifying trained networks;
- listing and lookup of persisted networks;
- tournament ranking and bracket simulation;
- correlated management-event recording, searching, and summaries;
- configuration, logging, errors, persistence, and test suites.

Out of scope:

- importing DL4J model weights into another ML framework;
- inventing missing season, participant, tournament-template, or properties
  data not included in this repository;
- shipping sports beyond the current NCAA and MLB implementations in the
  first replacement release. The framework's extension path for later sports
  is in scope and MUST be exercised through module-contract tests.

## Normative language

**MUST**, **MUST NOT**, **SHOULD**, and **MAY** are requirements, prohibitions,
recommendations, and options respectively. “Legacy behavior” describes
observable behavior in the source; it is not automatically a requirement to
reproduce a defect. The requirements and compatibility rules in this
specification define the intended replacement behavior.

## Source inventory and observed baseline

The source contains 101 Java files across:

- `framework`: shared DTOs, domain models, exceptions, CSV persistence
  abstractions, training/tournament workflows, network utilities, and
  configuration/cache helpers. Several core services are parameterized by
  `SeasonData` and depend on sport-neutral DAO/plugin contracts;
- `ncaabb`: the NCAA feature model and CSV parsers, training and tournament
  services, controllers, and year-specific tournament plugins;
- `mlb`: the MLB feature model and CSV parsers, training and tournament
  services, `/mlb` controller, sport utilities, and year-specific plugins;
- `resource`: the existing file-backed resource service, model persister,
  management-event service, and their REST controllers.

The source structure confirms the intended extension pattern: generic
framework services and `SeasonDataDao<T extends SeasonData>` operate over the
abstract `SeasonData` base; NCAA and MLB each supply their own season record,
CSV and participant DAO implementations, sport-specific training service,
and tournament plugins through the shared plugin contract. This is evidence
of an intended extensibility pattern and two concrete sport implementations,
not evidence that the current core is already fully decoupled or that more
sports are implemented. The replacement MUST preserve and strengthen that
separation.
The existing framework also contains DL4J-specific code, so its current
classes are architectural prior art rather than a requirement to retain
framework/library coupling.

The Gradle build compiles the shared source tree and produces separate
executable NCAA basketball and MLB archives. `bootRun`/`bootRunNcaaBb` select
the NCAA application; `bootRunMlb` selects MLB. Sport-specific Spring scans
are bounded so one target does not activate the other sport's controllers and
beans. Both default to port 8287, so only one can bind that port at a time;
running separate processes on different ports is possible, subject to safe
storage/configuration isolation.

The repository has no Java test sources. Runtime data, aliases, tournament
templates, models, and network properties are external deployment inputs.
Implementations MUST make these runtime inputs explicit and MUST add
deterministic synthetic test fixtures for data parsing and application behavior.

Notable observed behavior to account for:

- The NCAA data adapter emits 64 ordered features and one integer class label;
  the participant DAO declares seven output classes. MLB emits 78 ordered
  features and declares six output classes.
- Participant CSVs define the training/evaluation years. Season statistics
  cover a different year range. The 2019 tournament is absent. The
  participant-data year list in source is 2010–2018 and 2020–2023; season
  statistics are declared for 2009–2018 and 2020–2024.
- MLB season statistics are declared for 2009–2023 and participant/training
  years for 2010–2022.
- Multiple NCAA tournament plugin classes exist, but the active service
  registers only the 2024 ranking plugin. MLB registers 2009 and 2023 ranking
  plugins. A plugin class existing in source does not mean its year is
  available through the API.
- Model and data operations use direct in-process repository calls.
- The legacy normalizer is fitted independently to training, evaluation, and
  tournament data. That leaks evaluation/inference distribution into
  preprocessing and prevents reliable train/serve parity. The replacement
  MUST fit preprocessing on training features only, persist it, and reuse it
  for evaluation and inference. This can produce different metrics.
- The legacy in-memory event indexes are not durable and use mutable
  non-concurrent maps. The replacement MUST make concurrency and retention
  behavior explicit and use the durable event storage required by the
  operations specification.
- The legacy network results aggregation uses shared mutable maps while
  processing networks in parallel. The replacement MUST use deterministic,
  race-free aggregation.

## High-level component map

The implementation MUST follow these dependency directions, regardless of
package names:

```text
HTTP adapters / command entry points
              |
       application services
        /      |       \
    domain   repositories   ML backend adapter
                  |
       files / database / caches
```

- **API adapters** parse transport inputs, enforce transport-level schemas,
  call application services, and map domain errors to responses. They MUST NOT
  contain training, scoring, file parsing, or tournament algorithms.
- **Application services** coordinate training, batch runs, network lookup,
  evaluation, tournament execution, and management events.
- **Sport modules** provide their own record/data schema, feature and label
  mapping, supported-year/data validation, sport configuration, and optional
  sport-specific tournament plugins. NCAA basketball and MLB are concrete
  modules; those definitions MUST remain within their respective modules.
- **Domain/framework layer** owns sport-neutral descriptors and model
  lifecycle, generic metrics/contracts, eligibility mechanisms, and shared
  workflow orchestration. Sport-specific schemas and class counts MUST be
  supplied through module contracts, not hard-coded into generic workflows.
  The domain MUST NOT depend on an HTTP framework or specific neural-network
  library.
- **Repositories** own data-file parsing, model metadata/artifacts, event
  persistence, configuration retrieval, and cache policy. They MUST be called
  directly in-process; no resource repository has an HTTP controller.
- **Extension registry** discovers and validates registered sport modules
  and their optional capabilities. Registration of a new sport MUST NOT
  require editing generic training, persistence, or event workflows.
- **ML backend adapter** is the only layer allowed to use a chosen ML library.
  It MUST implement the framework-neutral operations in the domain
  specification and pass the backend conformance tests.

The application MUST be testable with an in-memory repository and a fake ML
backend without starting an HTTP server or loading a real model. The real
backend and real filesystem/database integrations MUST also have their own
integration tests.

## Cross-cutting requirements

1. Feature order, output class order, descriptor meanings, model
   preprocessing, and model artifact version MUST be explicit and versioned.
2. Training and inference MUST use exactly the same ordered feature schema and
   the same fitted preprocessor. Reject incompatible models/data rather than
   silently reorder, truncate, or fill unknown values.
3. Randomness MUST be seedable. Persist the seed and resolved architecture for
   every run, including randomized layout generation.
4. All numerical inputs, settings, predictions, and metrics MUST be checked
   for finite values and valid ranges before persistence or scoring.
5. Filesystem paths MUST be resolved beneath configured roots; user-supplied
   path components MUST NOT escape those roots.
6. Operations that produce persistent model artifacts MUST be atomic from the
   reader's perspective. A partial write MUST NOT be discoverable as a valid
   model.
7. Batch jobs MUST expose a correlation identifier and observable lifecycle
   events. Cancellation MUST be checked at bounded intervals (at least once
   per training epoch and between batch iterations).
8. Errors MUST be explicit and actionable. Do not return an empty success
   response or an empty model in place of a failed repository/backend
   operation.
9. User-visible differences from the legacy API, including model type enum
   values, metric definitions, normalization, smoothing, and plugin years,
   MUST follow the contracts in this specification and be covered by tests.

## Completion gates

An application conversion is not complete until:

- all requirements and tests in the three linked specifications are
  implemented;
- an independent clean environment can install/build and run the application;
- tests use synthetic, version-controlled data and need no production
  credentials, network service, or external dataset;
- feature-parity fixtures and API contract tests pass;
- a real selected ML backend passes backend-conformance and end-to-end
  train/save/load/predict tests;
- data and model access use in-process repositories.
