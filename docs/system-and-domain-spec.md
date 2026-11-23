# System and Domain Behavior Specification

This document defines the application and ML behavior independently of
programming language and neural-network framework. It is normative together
with [the master specification](design-spec.md).

## 1. Application boundaries

### 1.1 Required logical components

The implementation MUST provide equivalents of:

1. **Sport module and data repositories** — a registered sport module
   supplies its season records, feature/label schema, supported-year rules,
   and repositories/parsers for its data. NCAA basketball reads season-stat
   CSVs, team aliases, and tournament-participant CSVs. MLB combines batting,
   pitching, and fielding CSVs with playoff-result CSVs.
2. **Resource repository** — reads plain-text and CSV assets (including
   templates and property configuration) from configured roots. It is an
   internal interface, not an HTTP API.
3. **Model repository** — stores and retrieves versioned artifacts and their
   descriptors/indexes; lists metadata without loading model weights.
4. **Training service** — prepares datasets, resolves configuration,
   constructs/trains/evaluates a model, applies thresholds, records events,
   and persists only results that qualify for storage; all other model state
   is discarded.
5. **Tournament service** — checks model/year eligibility, obtains season
   vectors, calls model inference, accumulates tournament scores, and emits
   ranking or bracket output.
6. **Management service** — validates, stores, searches, and summarizes
   correlated events.
7. **ML backend adapter** — translates a resolved network specification and
   numeric arrays to a selected ML library and returns a framework-neutral
   model handle, predictions, and training/evaluation information.

Each component MUST have a narrow interface and injectable dependencies.
NCAA- and MLB-specific data adapters/plugins depend on the shared interfaces;
the shared training and tournament services MUST NOT import either sport's
parser implementation details or assume either sport's feature/output
dimensions.

### 1.2 Sport extension contract

The framework hosts multiple sport modules. NCAA basketball is the reference
implementation, not a special case that generic workflow code may hard-code.
The Java source demonstrates the intended pattern: abstract
`SeasonData`, generic `SeasonDataDao<T extends SeasonData>`, participant DAO
contracts, shared training/tournament services parameterized by sport record
type, and a shared tournament-plugin interface. Both `ncaabb` and `mlb`
provide concrete sport-specific models and services through those contracts.
The replacement MUST preserve and strengthen these extension points.
This generic type structure does not make the legacy framework fully
library-independent: its training service currently imports DL4J/ND4J types.
The replacement MUST separate the sport-module contracts from the ML-backend
adapter instead of treating the existing Java class hierarchy as a finished
plug-in architecture.

Every sport module MUST declare and register:

- a unique stable sport identifier and display name;
- its typed season-record model and versioned ordered feature schema;
- its feature encoder/validator and, for supervised classifiers, target-label
  encoder, output-class order, and output dimension;
- its data repositories/parsers, supported years, aliases, and data-quality
  rules;
- sport-specific configuration defaults/validation and compatibility API
  adapter, if that sport exposes an HTTP API;
- supported application capabilities. Tournament support is optional and,
  when enabled, is supplied through the shared tournament plugin contract
  with year, output mode, and any sport-specific templates/coordinates.

The generic training, evaluation, model lifecycle, persistence, event, and
backend-adapter workflows MUST consume these declared contracts rather than
branch on sport names. Model manifests MUST bind every artifact to its sport
identifier, feature-schema version/hash, target/output schema, and capability
metadata. A model for one sport MUST NOT be loaded or scored with another
sport's adapter/schema.

Register both NCAA basketball and MLB as production modules. Their different
feature widths and output-class counts MUST pass through the same generic
workflows without sport-name conditionals. Run the sport-extension contract
suite against both modules, plus a small synthetic test module to exercise
module discovery and optional capabilities without introducing a third
production sport. New sports that do not use tournament participants or
tournament scoring may omit those optional capabilities; they MUST still
provide the contracts required by whichever workflows they register for.

Current source-specific module baselines:

| Sport | Ordered inputs | Declared outputs | Season-stat years | Participant/training years | Active tournament plugins |
|---|---:|---:|---|---|---|
| NCAA men's basketball (`NCAABB`) | 64 | 7 | 2009–2018, 2020–2024 | 2010–2018, 2020–2023 | 2024 ranking |
| MLB (`mlb`) | 78 | 6 | 2009–2023 | 2010–2022 | 2009 and 2023 ranking |

MLB composes batting, pitching, and fielding CSVs into one team-season record.
Its respective input row widths are 29, 36, and 19 fields, each with one
header row; its participant CSV has two fields. Team normalization includes
historical aliases. The six declared MLB output classes and actual label
range/order MUST be validated against synthetic fixtures and authorized
participant data rather than inferred from the class count alone.

The MLB extension is implemented by `MlbSeasonData`, `MlbSeasonDataCsvDao`,
`MlbTournamentParticipantCsvDao`, `MlbNetworkTrainingService`,
`MlbTournamentRunnerService`, and `AbstractMlbTournamentRunnerPlugin` with
year-specific plugins. It supplies its own URI/network properties, network
utilities, team-name canonicalization, and DAO beans while inheriting generic
framework workflows. The NCAA module follows the corresponding pattern with
`NcaaBbSeasonData`, its DAOs, training service, tournament service, and
plugins. Neither sport module should leak parsing or feature logic into the
shared framework.

### 1.3 Core domain objects

- **Season record:** sport, year, canonical team name, and a finite ordered
  feature vector. Year/team is unique after alias resolution.
- **Tournament participant:** year, team name, participant identifier if
  supplied, and integer number of tournament victories.
- **Network descriptor:** stable model ID, sport/model type, resolved input
  and output dimensions, architecture, training/evaluation years,
  preprocessing and optimization choices, runtime controls, metrics, and
  training/persistence timestamps. The wire fields are listed in the
  interface specification.
- **Model artifact:** backend-specific serialized weights plus a backend-neutral
  manifest and descriptor. A model handle couples the loaded artifact with
  its descriptor and fitted preprocessor.
- **Training result:** final metrics, stopping reason, threshold decision, and
  outcome. Training failure, valid-but-below-threshold completion, and
  successful persistence MUST be distinct outcomes. A model descriptor or
  handle is returned only for a qualifying model that has been persisted;
  below-threshold model state is discarded, but its metrics are still reported.
- **Team coordinate:** tournament identity `(region, round, index)`, with
  optional team name, per-match aggregate votes/scores, and cumulative score.
  Equality/identity MUST use only the coordinate triple, not display name or
  score.
- **Management event:** event/service/correlation identifiers, event type,
  timestamp, optional elapsed duration, sport/host, optional descriptor, and
  message/context.

## 2. Sport data contracts

### 2.1 Resource layout

The current resource service resolves sport data below a configured base
directory, using a layout equivalent to:

```text
<base>/<SPORT>/data/<YEAR>/<file>
<base>/<SPORT>/data/<sport-level-file>
<base>/<SPORT>/<network-directory>/<model-artifact>
<base>/<SPORT>/<network-index-directory>/<descriptor-index>
```

`SPORT` is the uppercase sport code (`NCAABB` in this application). The
Python implementation uses `<resource-root>/Networks/<SPORT>/<uuid>.model.keras`
and `<resource-root>/Networks/<SPORT>/<uuid>.json` for model and
descriptor/manifest. A rebuildable SQLite search index is stored in Networks.
All reads and writes MUST go through repository interfaces.
Tournament template resources currently use:

- `ncaabb/tournament-template-ncaabb.txt` for game/coordinate pairings;
- `ncaabb/tournament-results-template.csv` for bracket rendering.

The alias file is `ncaabb/data/SeasonData-Alias.csv`. Do not assume these
external data files exist in this source repository.

The configured base directory is the directory that *contains* the sport
directories (it is not itself a sport directory). Pointing it one level too
deep or too shallow is a common deployment mistake, so any missing-resource
error MUST report the full resolved path that was searched, and the expected
layout MUST be documented for operators. Runtime CSV resources MUST be
external deployment inputs, and tests MUST use synthetic fixtures.

### 2.2 Available years

The Java season parser declares four inputs per season for these years:
2009–2018 and 2020–2024. The participant parser declares
`tournament_participant_all.csv` for 2010–2018 and 2020–2023. The current
`trainingYears` result is derived from participant files, not from all
season-stat files. The 2019 tournament was cancelled and has no participant
entry.

The replacement MUST obtain supported-year lists from the repository's
validated manifest/configuration, not infer continuity from integer ranges.
Training-year listing MUST return only years declared by the participant
manifest and supported by the required season features. Runtime data MUST NOT
extend or alter these year manifests. Tournament-year listing MUST return only
plugins actually enabled by the runtime. In
the current Java runtime only the 2024 NCAA plugin is registered; other
plugin classes are present but inactive. The active plugin set MUST be
explicit configuration and covered by tests.

### 2.3 CSV parsing and merge behavior

For each configured season year, parse and join four files by normalized team
name:

| File | Expected columns | Header rows skipped | Data fields consumed |
|---|---:|---:|---|
| `BasicSeasonData.csv` | 38 | 2 | rank, team, games, record, win percentage, ratings, conference/home/away records, scoring, and basic team statistics |
| `OpponentSeasonData.csv` | 38 | 2 | team name and opponent statistics |
| `AdvancedSeasonData.csv` | 34 | 2 | team name and advanced team statistics |
| `AdvancedOpponentSeasonData.csv` | 34 | 2 | team name and advanced opponent statistics |

The implementation MUST preserve CSV quoting semantics. Strip surrounding
whitespace and the source `*` marker from team names. For numeric season
statistics, map blank or whitespace-only fields to zero and remove `%` before
numeric conversion. Reject malformed or non-finite nonblank values with
file/row/column context. This zero-filling rule does not apply to missing
team rows, team names, or tournament participant labels.
CSV header rows are skipped; their names and contents are not validated.
Data fields are interpreted by position, so input columns must retain the
documented order.

Selected rows with unexpected column counts MUST fail the load and identify
the file, expected and actual counts. A season record assembled from any subset
must be checked for completeness before it is eligible for training or
inference. For NCAA training and evaluation, read the year's tournament
participants first and process season statistics only for those teams, resolving
aliases before selection. Exclude participants that do not join across the four
files for the affected year and log their names, year, missing files, and
guidance to check aliases or data rows. Nonparticipant rows do not affect the
dataset. Tournament inference MUST reject incomplete joins.

The alias CSV may begin with any number of comment lines (first non-blank
character `#`) and then has one
header row. The first column identifies the source team name and the second
column, when present and non-blank, is a name alias. The legacy loader keeps a
**list** of aliases per source name, and the same season record is stored under
every one of them. A team therefore MAY have any number of aliases, across any
number of rows: the data provider renames teams from year to year (for example
`Alabama State`, `Alabama State NCAA`, and `Alabama St`), and the alias file
accumulates every name ever used. Several source names may also share an alias.
A row with a blank second column, or no second column, means the source name is
valid as is.

The replacement MUST NOT model an alias as a one-to-one mapping and MUST NOT
fail merely because one name appears in several rows with different aliases.
Treat all names linked by alias rows as one team, and resolve them to one
deterministic canonical name (for example, the lexicographically first name
that has a blank-alias row, otherwise the lexicographically first linked name).
The result MUST NOT depend on row order. Case/whitespace policy MUST be
explicit. Alias resolution MUST fail validation when two different season
records resolve to the same canonical team for a year. For NCAA training and
evaluation, exclude unmatched participants for the affected year with a
warning that identifies the team and year and suggests checking aliases and
season rows. A dataset with no participant examples MUST fail. A missing alias
file MUST be reported with the resolved path that was searched.

Participant CSV files have one header row and exactly two fields per
non-empty row: team name and victory count. Remove surrounding whitespace,
the `*` marker, and the source trailing period convention from participant
names. Parse the victory count as an integer in the supported label range.
Duplicate participant rows for a year/team MUST be rejected or handled by an
explicitly tested conflict policy.

### 2.4 Feature schema `ncaabb-season-v1`

Training and inference MUST use this exact 64-element order until a new
versioned schema is introduced. Feature order is model data: never alphabetize
or derive it from map iteration order.

| Index | Feature |
|---:|---|
| 0 | team win percentage |
| 1 | simple rating system |
| 2 | strength of schedule |
| 3 | team points scored per game |
| 4 | opponent points scored per game (from basic stats) |
| 5 | team field goals per game |
| 6 | team field-goal attempts per game |
| 7 | team field-goal percentage |
| 8 | team three-pointers per game |
| 9 | team three-point attempts per game |
| 10 | team three-point percentage |
| 11 | team free throws per game |
| 12 | team free-throw attempts per game |
| 13 | team free-throw percentage |
| 14 | team offensive rebounds per game |
| 15 | team total rebounds per game |
| 16 | team assists per game |
| 17 | team steals per game |
| 18 | team blocks per game |
| 19 | team turnovers per game |
| 20 | team personal fouls per game |
| 21 | opponent points scored per game (again, as emitted by the legacy feature mapper) |
| 22 | opponent field goals per game |
| 23 | opponent field-goal attempts per game |
| 24 | opponent field-goal percentage |
| 25 | opponent three-pointers per game |
| 26 | opponent three-point attempts per game |
| 27 | opponent three-point percentage |
| 28 | opponent free throws per game |
| 29 | opponent free-throw attempts per game |
| 30 | opponent free-throw percentage |
| 31 | opponent offensive rebounds per game |
| 32 | opponent total rebounds per game |
| 33 | opponent assists per game |
| 34 | opponent steals per game |
| 35 | opponent blocks per game |
| 36 | opponent turnovers per game |
| 37 | opponent personal fouls per game |
| 38 | team pace |
| 39 | team offensive rating |
| 40 | team free-throw attempt rate |
| 41 | team three-point attempt rate |
| 42 | team true-shooting percentage |
| 43 | team total-rebound percentage |
| 44 | team assist percentage |
| 45 | team steal percentage |
| 46 | team block percentage |
| 47 | team effective field-goal percentage |
| 48 | team turnover percentage |
| 49 | team offensive-rebound percentage |
| 50 | team free throws per field-goal attempt |
| 51 | opponent pace |
| 52 | opponent offensive rating |
| 53 | opponent free-throw attempt rate |
| 54 | opponent three-point attempt rate |
| 55 | opponent true-shooting percentage |
| 56 | opponent total-rebound percentage |
| 57 | opponent assist percentage |
| 58 | opponent steal percentage |
| 59 | opponent block percentage |
| 60 | opponent effective field-goal percentage |
| 61 | opponent turnover percentage |
| 62 | opponent offensive-rebound percentage |
| 63 | opponent free throws per field-goal attempt |

Index 21 duplicates the concept at index 4 in the legacy mapper. Preserve it in
`ncaabb-season-v1` for existing model/schema parity; removing or changing it
requires a new feature-schema version and retraining.

Labels are the participant's integer number of tournament victories, encoded
as one of seven classes in ascending order `[0, 1, 2, 3, 4, 5, 6]`. The
training matrix shape is `(examples, 64)` and target representation is
backend-adapter-owned but MUST represent those same seven mutually exclusive
classes. Inference output position `i` MUST correspond to class `i`.

### 2.5 Data assembly for training/evaluation

For each requested year, iterate participant rows, look up the matching
season record, emit its 64 features and victory-count label, then concatenate
years in a deterministic order. The legacy code shuffles the examples before
training. The implementation MUST shuffle only the training examples,
reproducibly from a run seed; evaluation and inference ordering MUST remain
stable.

Legacy smoothing is optional and defaults off. When enabled in the source,
additional duplicate rows per participant are:

| Victory label | Extra copies |
|---:|---:|
| 1 | 1 |
| 2 | 3 |
| 3 | 7 |
| 4 | 15 |
| 5 | 31 |
| 6 | 63 |
| other | 0 |

The original row is also kept, so these labels have total multiplicity
`2^label`; label 0 is not duplicated. The setting is independently applied to
training and evaluation data in the legacy code. This has a large effect on
class distribution and evaluation meaning. It MUST remain disabled by default;
enabling it MUST be explicit, visible in the model manifest, and tested.
Class weighting or resampling is a separate behavior, not an implicit
replacement for these duplicates.

### 2.6 MLB data contract

The MLB module loads team batting, pitching, and fielding files for each
supported season year and joins their rows by canonical team name. Each file
has one header row; the row widths are 29, 36, and 19 columns respectively.
The playoff-result file has two fields per participant row: team name and
number of victories. The Java source declares season-stat years 2009–2023 and
playoff participant/training years 2010–2022; it currently registers ranking
plugins for 2009 and 2023.

Provider-defined aggregate rows MUST be excluded from team records in parsed
season data. Blank or whitespace-only numeric season statistics MUST be
filled with zero. Nonblank malformed or non-finite values, missing team rows,
team names, and participant labels remain validation errors.

The ordered `mlb-season-v1` feature schema has 78 values, grouped and ordered
as follows:

- Batting (28): `numBat`, `batAge`, `rPerG`, `gamesBat`, `plateAppear`,
  `atBat`, `runs`, `hits`, `doubles`, `triples`, `hr`, `rbis`, `sb`,
  `caughtStealing`, `bb`, `so`, `ba`, `obp`, `slg`, `ops`, `ops_plus`, `tb`,
  `gdp`, `hbp`, `sh`, `sf`, `ibb`, `lob`.
- Pitching (35): `numPitchers`, `pAge`, `rallPerG`, `win`, `loss`, `wlpct`,
  `era`, `gamesPitch`, `gs`, `gf`, `cg`, `tSho`, `cSho`, `sv`, `ip`, `ha`,
  `ra`, `er`, `hra`, `walks`, `intWalks`, `soP`, `hbpP`, `bk`, `wp`, `bf`,
  `era_plus`, `fip`, `whip`, `h9`, `hr9`, `bb9`, `so9`, `soPerW`, `lobP`.
- Fielding (15): `numFld`, `rapg`, `defEff`, `inn`, `ch`, `po`, `assist`,
  `errors`, `dp`, `fldPct`, `rTot`, `rTotYr`, `rdrs`, `rdrsYr`, `rgood`.

The 78 feature values plus one target label form a 79-value supervised row.
The module declares six output classes; fixtures and available participant
data MUST establish the valid label range and output ordering independently
of that declaration. Do not treat the training-row width as the model input
width.

Canonicalize team names consistently across all three season files and the
playoff-result file using the sport's configured alias rules. The current
fielding parser assigns the `#Fld` column to the `errors` feature; the
replacement MUST map `errors` from the CSV `E` column and test that mapping
with a fixture.

## 3. Network definition and ML backend contract

### 3.1 Backend independence

Identifiers chosen at code-generation time (types, functions, parameters,
variables, fields, and persisted keys) MUST NOT collide with, shadow, or be
ambiguous with similarly named members of the selected backend or its
libraries, and MUST name what they actually hold. For example, a repository
method that persists a model MUST NOT be named like a backend method that
saves model weights, and a value scored on training data MUST NOT carry a name
that suggests evaluation data.

The domain layer MUST express models as a resolved network specification:

- positive input width;
- ordered positive hidden-layer widths;
- output width and class order from the registered sport/task schema (seven
  for NCAA basketball);
- hidden and output activation identifiers;
- loss identifier;
- optimizer identifier and numeric parameters;
- weight initialization identifier;
- L2 parameters;
- random seed;
- feature-schema and preprocessing identifiers.

Input and output dimensions MUST be obtained from the registered sport/task
module. Generic backend and training code MUST NOT hard-code NCAA's 64 inputs,
seven outputs, labels, or feature order.

The ML adapter MUST expose equivalent operations to:

1. build a model from the resolved specification;
2. train one epoch (or a precisely defined epoch equivalent) on training
   features/labels;
3. predict class scores/probabilities for a batch;
4. save and load a model artifact;
5. report backend identity/version and shape information.

The domain owns epoch scheduling, stopping rules, metrics, thresholds, model
eligibility, event lifecycle, and artifact metadata. The backend MUST NOT
silently redefine those semantics. A backend conformance suite MUST run
against every selected library.

An artifact is portable only to compatible runtime/backend versions; the
backend-independent part is its manifest and metadata, not necessarily its
weights. A model loader MUST reject unknown or incompatible backend formats
with a clear error.

### 3.2 Architecture layout

Network layouts are sequences of positive layer widths, input first and output
last, written as `x`-separated strings at the API boundary (for example
`64x48x24x7`). Implementations SHOULD store the resolved widths as an integer
array.

Request parameters:

- `networkStructureAlgorithm` is required. It MUST be one of `HOUR_GLASS`,
  `TAPERED_TREE`, `USER_DEFINED`, or `USER_DEFINED_RAND`. A missing or unknown
  value is rejected as a validation error (`400`); there is no default.
- `USER_DEFINED` and `USER_DEFINED_RAND` require `networkLayout`.
  `numberOfHiddenLayers` is ignored because the layout determines the hidden
  layer count.
- `HOUR_GLASS` and `TAPERED_TREE` require `numberOfHiddenLayers`, an integer
  `>= 3` with no default. `networkLayout` is ignored.
- Both generated shapes require the schema's input width `I` to be `>=` its
  output width `O`; otherwise the request is invalid.

Random choices MUST use the explicit run seed, so the same seed and inputs
always produce the same layout. The resolved full layout and the effective
hidden-layer count are persisted in the descriptor.

The layout resolver MUST validate: hidden-layer count > 0 and within
configured limits, every width > 0, first width equals the registered module's
input width, last width equals its output width, and no arithmetic overflow or
unbounded allocation. Reject any result outside configured positive-width and
total-parameter limits.

**Notation.** `I` and `O` are the schema input and output widths, `H` is the
hidden-layer count, and `w[1..H]` are the hidden widths; the resolved layout is
`[I, w[1], ..., w[H], O]`. Layer 0 is the input layer (width `I`). Random
integers are drawn uniformly and inclusively from the stated range using the
seeded generator, in the order stated per algorithm. Linear interpolation
between layer `a` (width `x`) and layer `b` (width `y`) gives layer `j` the
width `x + floor((y - x) * (j - a) / (b - a))`, using mathematical floor.

- `P` (peak width) is a random integer in `[4*I, 10*I]`.
- `N` (neck width) is a random integer in `[2*O, 4*O]`.
- `F` (final hidden width) is a random integer in `[2*O, 4*O]`.

| Algorithm | Hidden-width generation |
|---|---|
| `HOUR_GLASS` | Two humps with a narrow neck between them. Draw `P`, then `N`, then `F`. For odd `H`, the neck is layer `n = (H+1)/2`, with peaks `p = n-1` and `q = n+1`. For even `H`, the neck is layers `H/2` and `H/2+1`, with peaks `p = H/2-1` and `q = H/2+2`. Set `w[p] = w[q] = P` and every neck layer to `N`. Layers `1..p-1` interpolate from layer 0 (`I`) to layer `p` (`P`). Layers `q+1..H` interpolate from layer `q` (`P`) to layer `H` (`F`). When `H` is 3 or 4, `q = H` and there are no smoothing layers: the shape is peak, neck, peak (and for `H = 4`, peak, neck, neck, peak). |
| `TAPERED_TREE` | One hump that rises quickly and tapers slowly. Draw `P`, then `F`. The peak is layer `p = ceil(H/4)`, with `w[p] = P`. Layers `1..p-1` interpolate from layer 0 (`I`) to layer `p` (`P`) (the rapid rise). Layers `p+1..H` interpolate from layer `p` (`P`) to layer `H` (`F`) (the gradual taper). |
| `USER_DEFINED` | Parse positive integers separated by `x`. The first and last widths MUST equal `I` and `O`. The hidden widths are the interior values, and `H` is their count (at least 1). The hidden widths are preserved. |
| `USER_DEFINED_RAND` | Resolve the layout as `USER_DEFINED`, then replace each hidden width in order with a seeded random integer in `[max(1, floor(previousWidth/2)), max(lowerBound+1, floor(currentWidth*3/2)))`, exclusive at the upper end. `previousWidth` is `I` for the first hidden layer and the preceding resolved hidden width thereafter. The user's widths act as upper guides; the layer count, `I`, and `O` never change. |

Worked examples with `I = 100`, `O = 3`, and drawn values `P = 700`, `N = 8`,
`F = 12` (hidden widths only):

| Algorithm | `H` | Hidden widths |
|---|---|---|
| `HOUR_GLASS` | 3 | 700, 8, 700 |
| `HOUR_GLASS` | 4 | 700, 8, 8, 700 |
| `HOUR_GLASS` | 7 | 300, 500, 700, 8, 700, 356, 12 |
| `HOUR_GLASS` | 20 | 166, 233, 300, 366, 433, 500, 566, 633, 700, 8, 8, 700, 614, 528, 442, 356, 270, 184, 98, 12 |
| `TAPERED_TREE` | 3 | 700, 356, 12 |
| `TAPERED_TREE` | 7 | 400, 700, 562, 424, 287, 149, 12 |
| `TAPERED_TREE` | 20 | 220, 340, 460, 580, 700, 654, 608, 562, 516, 470, 424, 378, 333, 287, 241, 195, 149, 103, 57, 12 |

### 3.3 Supported legacy option identifiers

The input/config compatibility layer SHOULD recognize the source identifiers
below. Mapping is an adapter responsibility. Unsupported or semantically
incompatible choices MUST be rejected or reported as an explicit migration
limitation; never map to an arbitrary default.

- **Activations:** `TANH`, `CUBE`, `ELU`, `HARDSIGMOID`, `HARDTANH`,
  `IDENTITY`, `LEAKYRELU`, `RATIONALTANH`, `RELU`, `RELU6`, `RRELU`,
  `SIGMOID`, `SOFTMAX`, `SOFTPLUS`, `SOFTSIGN`, `RECTIFIEDTANH`, `SELU`,
  `SWISH`, `THRESHOLDEDRELU`, `GELU`, `MISH`.
- **Losses:** `MSE`, `L1`, `XENT`, `MCXENT`, `SPARSE_MCXENT`,
  `SQUARED_LOSS`, `RECONSTRUCTION_CROSSENTROPY`, `NEGATIVELOGLIKELIHOOD`,
  `COSINE_PROXIMITY`, `HINGE`, `SQUARED_HINGE`, `KL_DIVERGENCE`,
  `MEAN_ABSOLUTE_ERROR`, `L2`, `MEAN_ABSOLUTE_PERCENTAGE_ERROR`,
  `MEAN_SQUARED_LOGARITHMIC_ERROR`, `POISSON`, `WASSERSTEIN`.
- **Optimizers:** `ADA_DELTA`, `ADA_GRAD`, `ADAM`, `ADA_MAX`, `AMS_GRAD`,
  `NADAM`, `NOOP`, `RMS_PROP`, `SGD`.
- **Weight initialization:** `DISTRIBUTION`, `ZERO`, `ONES`,
  `SIGMOID_UNIFORM`, `NORMAL`, `LECUN_NORMAL`, `UNIFORM`, `XAVIER`,
  `XAVIER_UNIFORM`, `XAVIER_FAN_IN`, `XAVIER_LEGACY`, `RELU`,
  `RELU_UNIFORM`, `IDENTITY`, `LECUN_UNIFORM`,
  `VAR_SCALING_NORMAL_FAN_IN`, `VAR_SCALING_NORMAL_FAN_OUT`,
  `VAR_SCALING_NORMAL_FAN_AVG`, `VAR_SCALING_UNIFORM_FAN_IN`,
  `VAR_SCALING_UNIFORM_FAN_OUT`, `VAR_SCALING_UNIFORM_FAN_AVG`.
- **Normalization:** `NORMALIZER_STANDARDIZE`,
  `NORMALIZER_ZERO_TO_ONE`, `NORMALIZER_NEGATIVE_ONE_TO_ONE`.

For an n-class classifier, architecture/loss/activation combinations MUST be
validated as a compatible set. For example, a seven-unit softmax output with
categorical cross-entropy requires targets and score interpretation consistent
with that choice. Several listed legacy losses/activations are not appropriate
for categorical classification; accept them only if a verified supported use
case exists.

### 3.4 Preprocessing and evaluation

Fit the selected feature normalizer on training features only. Persist all
parameters needed for transformation. Apply the same transform to training,
evaluation, tournament, and single-record inference. Do not normalize labels.
For a training column with mean `m`, population standard deviation `s`, minimum
`a`, and maximum `b`, use:

- `NORMALIZER_STANDARDIZE`: `(x - m) / s`;
- `NORMALIZER_ZERO_TO_ONE`: `(x - a) / (b - a)`;
- `NORMALIZER_NEGATIVE_ONE_TO_ONE`: `2 * (x - a) / (b - a) - 1`.

If a scale denominator is zero, transform that column to zero for every row
and persist the constant-column marker. Reject missing, NaN, and infinite
feature values; do not silently impute them. Values outside training min/max
are transformed by the same formula without clipping. A future clipping or
imputation policy requires a new preprocessing version.

Metrics are calculated from predicted classes and targets using the registered
module's fixed output-class order. For NCAA basketball that order is
`0, 1, 2, 3, 4, 5, 6`; generic metric code MUST support any valid class count.
Confusion-matrix rows are actual labels and columns are predicted labels. For
each class, calculate
precision=`TP/(TP+FP)`, recall=`TP/(TP+FN)`, and F1 as their harmonic mean;
when a denominator is zero, the corresponding value is zero. Overall
precision, recall, and F1 are unweighted macro averages over the registered
class order. The canonical metric record includes:

- overall accuracy;
- macro precision, recall, and F1;
- per-class confusion counts and a stable CSV representation;
- optional loss, epoch count, training error, and stopping reason.

Legacy `realNetworkAccuracy` is the arithmetic mean of accuracy, precision,
recall, and F1, on a 0–1 scale and remains a reported diagnostic. The
configured performance threshold is a percentage on a 0–100 scale; save when
evaluation `networkAccuracy * 100 >= threshold`. Keep the stopping criterion
and save-threshold score separate. The
canonical metric definitions above are the replacement contract; do not claim
numerical metric parity with DL4J unless golden confusion matrices confirm
equivalent aggregation.

**Training and evaluation are two distinct phases.** The replacement MUST
operate as follows:

1. *Training phase.* Train one epoch at a time. At every
   `networkEvaluationIndex` epochs, score the network on the **training data**
   and compute the network error as `1 - recall` (macro recall on the training
   data). If the error is strictly below `maxNetworkError`, training stops
   early and the network is considered trained. Otherwise continue until
   `maxNumberOfTrainingEpochs`. The evaluation data MUST NOT be read, predicted
   on, or scored during this phase. This error is the descriptor's
   `trainingError`.
2. *Evaluation phase.* If and only if training stopped because
   `maxNetworkError` was reached, score the network once on the evaluation
   data and compute the canonical metrics. Evaluation accuracy is compared to
   the performance threshold to decide whether to persist.
3. If the epoch limit is exhausted without reaching `maxNetworkError`, the
   evaluation phase MUST NOT run. The network is discarded, no evaluation
   metrics exist, and the outcome is completed-below-target.

**Locked terminology.** Implementations MUST NOT conflate these terms in code,
logs, wire data, or docs. The data the network was not trained on is called the
*evaluation data* (never "held-out").

| Term | Definition | Data | Used for |
| --- | --- | --- | --- |
| training loss | The framework's per-epoch loss (for example cross-entropy), the quantity back-propagation minimizes. Not a rate and not comparable to `maxNetworkError`. | training | Diagnostics only |
| training error | `1 - macro recall` of predictions on the training data (legacy `computeNetworkError`). 0 means every outcome category is right. | training | Early stop when strictly `< maxNetworkError` |
| training accuracy | Fraction of training rows predicted correctly. | training | Diagnostics |
| evaluation error/accuracy/precision/recall/F1 | Canonical metrics on the evaluation data; evaluation error is `1 - evaluation recall`. | evaluation | Persist when accuracy × 100 is `>= networkPerformanceThreshold` |
| composite percent | Mean of evaluation accuracy, precision, recall, F1, times 100. | evaluation | Diagnostic |

Rates are fractions in 0–1; loss and percentage fields are not. Very low
training error with high evaluation error means overfitting, which is addressed
by tuning (regularization, layer sizes, epoch counts, `maxNetworkError`), not by
a code change. Because error is sampled every `networkEvaluationIndex` epochs,
it can fall from above the target to far below it (even 0) between checks, so
the stop point may undershoot the target.

**Progress reporting.** Operators need visibility into long training runs.
If the maximum epochs are reached before training error falls strictly below
`maxNetworkError`, the server MUST log the final training error, target, and
readable training confusion matrix at INFO, whether or not the request is a
batch. Evaluation is not run in this case. Each completed batch iteration logs
evaluation metrics and its confusion matrix at INFO when evaluation ran;
otherwise it logs training metrics and the training confusion matrix. It
explains the threshold decisions and whether the network was saved;
non-batch detailed metrics remain DEBUG.
The replacement SHOULD emit progress lines carrying sport, model ID,
correlation ID, epoch and maximum epochs, training loss, training error,
training accuracy, `maxNetworkError`, and the decision (continue, monitoring
only, early stop, maximum epochs reached), and exactly one evaluation line
(evaluation metrics, performance threshold, whether it qualifies for
persistence) when the evaluation phase runs. Progress is independent of
stopping: an optional request property `reportingInterval` (positive integer,
in epochs) sets the print cadence, defaulting to every 10% of
`maxNumberOfTrainingEpochs` (every epoch when fewer than ten). Only milestone
epochs, the stopping epoch, and the final epoch print;
`networkEvaluationIndex` schedules error checks, never printing, and reporting
MUST NOT change when training stops. This property is an addition to the
legacy contract and is not honored by the Java implementation.

Legacy detail: the legacy loop counts epochs from zero and so first checks the
error after the first epoch, but never checks after the final epoch unless it
falls on an interval. The replacement MAY additionally check at the final
epoch; if so it MUST record that choice.

The replacement MUST evaluate using an evaluation set that was not used for
training. Evaluation rows MUST NOT influence fitted preprocessing or
gradient updates. Preserve the source's train/evaluation-year disjointness
rule. Tournament-year eligibility MUST reject training years and, by default,
evaluation years; a configuration switch may permit evaluation-year
prediction, matching the legacy `allow.evaluation.years.for.prediction`
setting. An artifact manifest records year sets, the switch value, and the
resolved feature/preprocessing schema.

### 3.5 Training and persisted-model lifecycle

A single training request MUST:

1. validate descriptor fields and supported years;
2. allocate a unique model ID and record a start event/correlation;
3. resolve the active sport/task configuration and architecture, derive input
   width and output classes from the module (64 inputs and seven outputs for
   NCAA basketball), and create train/evaluation matrices;
4. fit preprocessing on training features and persist its state;
5. build and train according to max epochs/evaluation interval/stopping
   criteria, scoring only the training data to decide when to stop and
   checking cancellation between epochs;
6. only if early stopping was reached, calculate evaluation-data metrics and
   record resolved parameters, seed, stopping reason, and runtime;
7. if early stopping is reached and the evaluation accuracy threshold is
   met, persist the model atomically and return its descriptor; otherwise
   record a distinct completed-below-target outcome, return the unsaved
   training report (no model), and discard the model state;
8. emit end/failure events and release transient data.

The replacement MUST distinguish “completed but target not met” from
“training failed.” For v1, the single-training endpoint returns an unsaved training
report for completed-below-target (`networkSaved: false`, with every metric and
confusion matrix available from the run, on training and, if scored, evaluation data), while the internal service and management events
retain the precise outcome. No persisted descriptor or model is returned for
this outcome. Models that do not qualify for persistence MUST
not be retained after the training request.

Batch training MUST:

- accept a requested positive number of iterations and bounded total work;
- use a parent correlation ID and per-iteration IDs/events;
- generate/reuse dataset preparation only when the schema and preprocessing
  semantics match;
- create a fresh model ID/seed per iteration;
- check the kill switch/cancellation at least once each epoch and before a new
  iteration;
- count persisted models separately from completed/below-threshold/failed
  iterations;
- report failures explicitly and continue only if the configured batch policy
  says so;
- emit started, iteration-finished, terminal, and exception events.

The legacy batch loop refreshes several configuration fields between
iterations. The replacement MUST capture an immutable, versioned
configuration snapshot when each batch is accepted. Subsequent configuration
updates apply only to batches accepted afterward.

## 4. Tournament behavior

This section specifies the NCAA basketball tournament behavior implemented by
the first sport module. The framework may share orchestration for loading
eligible models and invoking inference, but sport-specific prediction
aggregation, tie-breaking, bracket/ranking semantics, and rendering MUST be
owned by the registered sport's tournament plugin. Generic framework code
MUST NOT impose the NCAA distance formula or NCAA bracket conventions on other
sports.

### 4.1 NCAA inference and scoring

Before scoring any tournament request, validate every required input file
against the sport module's declared CSV contract: header row count,
data row width, numeric fields (with blank statistics filled with
zero), team-key uniqueness,
feature extraction schema, and participant/team resolution. This validation
MUST run against the configured tournament data used by the request. If a
required file or row no
longer matches the contract, fail the whole simulation before model
prediction or output generation; do not emit partial picks or silently skip
malformed tournament teams. Emit a correlated failure event with a safe,
source-located diagnostic.

For each network ID, load its artifact/descriptor, validate tournament-year
eligibility, build each tournament team's 64-feature row in a stable order,
apply the artifact's persisted preprocessor, and obtain exactly seven finite
class scores/probabilities. Reject malformed output shapes and NaN/infinite
values.

For prediction vector `p[0..6]`, compute the team's distance score:

```text
distance(team, model) = Σ for i=0..6: p[i] * (i + tournamentDistanceFactor)
```

The default factor is zero. aggregate by **summing** the scores from every
valid requested network, preserving the legacy behavior; do not average them. A missing, invalid,
or ineligible model is skipped with an explicit event/result warning. If no
valid models remain, return a documented failure, not a successful empty
ranking.

The legacy code normalizes tournament data by fitting on that tournament
batch. Replace that with the model's training-fitted preprocessor. Model
results are independent across teams; parallelization is optional, but score
aggregation MUST be race-free and deterministic. The request's network list
order MUST NOT change the final aggregate beyond floating-point tolerance.

### 4.2 Ranking mode

Ranking mode returns a CSV/text table with header:

```text
Rank,Team Name,Tournament Score
```

Sort descending by aggregate score and assign ranks starting at one. Use a
stable secondary key (canonical team name) for ties. Use a standards-compliant
CSV writer so commas/quotes in names are escaped. The legacy implementation
formats scores to four decimal places; preserve that format in the compatibility
response.

The currently enabled 2024 plugin is a ranking plugin and displays
`2024-25 (Ranking)`. It obtains NCAA season rows and produces team scores;
its bracket-coordinate-map method is unsupported.

### 4.3 Bracket mode

Bracket plugins define tournament-year metadata, seed/team-to-coordinate
mapping, a game-template, and a result-template. A game-template line contains
two coordinate triples, for example `[region,round,index],[region,round,index]`.
Comments beginning with `#` are ignored. Invalid coordinates, missing
season-data entries, or invalid graph transitions MUST fail with the line or
game identity.

For each template game, compare the two teams' aggregate distance scores. The
higher score advances. A tie is resolved by a deterministic policy. The legacy
possession-arrow policy begins with the home team and alternates the selected
side on each tie; preserving that policy requires a test and an explicit
tie-break setting. Every advancement updates the winner's next coordinate and
preserves the team-season-record mapping.

Render the result template by replacing coordinate cells with winning team
names. Include the tournament display name as the first output row. For
non-opening-round cells, the legacy output includes the winner's distance score
formatted to five decimal places and removes the text `NCAA` from the display
name; opening-round cells have no votes and display only the name. Make these
formatting rules explicit in output tests.

The existing Java source includes several bracket plugin classes (2009, 2010,
2013, 2016–2018, 2020–2023, including 2022B) as well as a 2024 ranking plugin,
but registers only 2024. The replacement MUST not activate old plugins by
accident. The active registry, human-readable name, output type, data
requirements, and fixture for every enabled year MUST be explicit.

## 5. Configuration and defaults

Configuration is sport-scoped with a generic fallback in the legacy design.
The replacement SHOULD load one validated immutable configuration snapshot
from deployment configuration, with optional sport-specific overrides. Do not
depend on the old remote properties REST endpoint.

Legacy property names and defaults observed in
`framework/util/AbstractNetworkProperties.java`:

| Setting | Legacy default / note |
|---|---|
| `beta.1.decay.scale.value` | `0.9` |
| `beta.2.decay.scale.value` | `0.999`; legacy Adam-family mapping appears to use beta-1 scale for beta-2 as well; verify before parity |
| `epoch.stats.index.count` | `100` |
| `learning.rule.learning.rate` | `1.0`, then optimizer-specific scaling in legacy code |
| `l2.regularization.lambda` | `0.0005` |
| `learning.rule.momentum.max` | `0.25`; no use found in the inspected training path |
| `loss.function.choice` | `MCXENT` |
| `max.training.epochs` | `1000` |
| `max.network.error` | `0.01` |
| `number.batch.iterations` | `10` |
| `network.evaluation.index` | `10`; MUST be positive |
| `stats.show.eval` | `false` |
| `networkStructureAlgorithms` | no default; the request MUST name one of `HOUR_GLASS`, `TAPERED_TREE`, `USER_DEFINED`, `USER_DEFINED_RAND` |
| `normalizers` | no usable default; legacy lookup may return an empty list |
| `normalize.across.all.season.data` | `true`; no use found in inspected pipeline |
| `number.of.hidden.layers` | no default; required (integer `>= 3`) for `HOUR_GLASS` and `TAPERED_TREE`, ignored for `USER_DEFINED[_RAND]` |
| `output.layer.activation` | `SOFTMAX` |
| `output.layer.loss.function` | `NEGATIVELOGLIKELIHOOD` |
| `performance.threshold` | `70` percent |
| `updaters` | no usable default; legacy lookup may return an empty list |
| `use.all.season.data.for.tournament` | `true`; no use found in inspected pipeline |
| `weight.init.choice` | `XAVIER_UNIFORM` |
| `simple.cache.ttl.minutes` | `1440` |
| `user.defined.network.structure` | absent/empty |
| `smooth.training.data` | `false` |
| `smooth.evaluation.data` | `false` |
| `allow.evaluation.years.for.prediction` | `false` |
| `tournament.distance.factor` | `0` |
| remote properties cache timeout | minimum `5000` ms; actual source-file default is external |

The list-valued algorithm/normalizer/updater properties are operationally
required by the current refresh path, even though no defaults are declared in
the Java class. The new implementation MUST either provide explicit safe
defaults or fail startup with a clear missing/invalid-setting diagnostic.
Document every chosen default, validate ranges and cross-field combinations,
and test both defaulting and invalid configuration.

## 6. Source map

Primary source evidence:

- Feature definition/parser: `ncaabb/model/NcaaBbSeasonData.java`,
  `ncaabb/persistence/NcaaBbSeasonDataCsvDao.java`.
- Participant years/classes: `ncaabb/persistence/NcaaBbTournamentParticipantCsvDao.java`,
  `framework/persistence/AbstractTournamentParticipantCsvDao.java`.
- Sport-extension contracts and implementation: `framework/model/SeasonData.java`,
  `framework/persistence/SeasonDataDao.java`,
  `framework/persistence/AbstractSeasonDataCsvDao.java`,
  `framework/service/AbstractNetworkService.java`,
  `framework/service/AbstractNetworkTrainingService.java`,
  `framework/service/NetworkRunnerPlugin.java`,
  `ncaabb/service/NcaaBbNetworkTrainingService.java`,
  `ncaabb/persistence/NcaaBbSeasonDataCsvDao.java`,
  `ncaabb/service/NcaaBbTournamentRunnerService.java`.
- Training and batch workflow: `framework/service/AbstractNetworkTrainingService.java`,
  `framework/service/AbstractNetworkService.java`.
- Network/backend and file/HTTP utilities: `framework/util/AbstractNetworkUtils.java`,
  `framework/util/NetworkStructureGenerator.java`.
- Tournament execution: `framework/service/AbstractTournamentRunnerService.java`,
  `framework/service/AbstractNetworkRunnerPlugin.java`,
  `ncaabb/service/NcaaBbTournamentRunnerService.java`,
  `ncaabb/service/plugins`.
- Model metadata and option enums: `framework/model/NetworkDescriptor.java`,
  `framework/dto/NetworkDescriptorDto.java`, `framework/model/*Choice.java`.
- Resource and artifact behavior: `resource/service/ResourceService.java`,
  `resource/util/NetworkFileBasedPersister.java`.
- Management: `resource/service/ManagementService.java`,
  `framework/model/ManagementEvent.java`,
  `framework/model/ManagementEventSummary.java`.
