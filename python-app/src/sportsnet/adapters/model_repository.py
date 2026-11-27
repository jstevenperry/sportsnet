"""Filesystem model pairs with a rebuildable SQLite descriptor index."""

import json
import os
import re
import shutil
import sqlite3
import tempfile
from collections.abc import Iterator, Mapping
from contextlib import contextmanager
from dataclasses import asdict, replace
from datetime import UTC, datetime
from pathlib import Path
from typing import Any, Protocol

import numpy as np
from numpy.typing import NDArray

from sportsnet.domain.errors import NotFoundError, RepositoryError, ValidationError
from sportsnet.domain.metrics import (
    ClassificationMetrics,
    evaluation_report_fields,
    training_report_fields,
)
from sportsnet.domain.models import ModelHandle, ModelSpec
from sportsnet.domain.preprocessing import Preprocessor
from sportsnet.domain.schemas import SportSchema


class ArtifactBackend(Protocol):
    @property
    def identity(self) -> dict[str, str]: ...

    def save(self, handle: object, path: Path) -> None: ...

    def load(self, path: Path, expected_spec: ModelSpec) -> object: ...

    def predict_scores(
        self, handle: object, features: NDArray[Any]
    ) -> NDArray[Any]: ...


class FilesystemModelRepository:
    def __init__(self, root: Path, backend: ArtifactBackend) -> None:
        self.root = root.resolve()
        self.backend = backend
        self.index_path = self.root / "descriptor-index.sqlite"
        self.root.mkdir(parents=True, exist_ok=True)
        with self._connection() as connection:
            connection.execute(
                """
                CREATE TABLE IF NOT EXISTS flat_models (
                    model_id TEXT PRIMARY KEY,
                    sport TEXT NOT NULL,
                    descriptor_json TEXT NOT NULL,
                    manifest_path TEXT NOT NULL
                )
                """
            )
            connection.execute(
                "CREATE INDEX IF NOT EXISTS idx_flat_models_sport "
                "ON flat_models(sport, model_id)"
            )

    def persist_model(
        self,
        *,
        model_id: str,
        sport: str,
        schema: SportSchema,
        model: object,
        model_spec: ModelSpec,
        preprocessor: Preprocessor,
        metrics: ClassificationMetrics,
        training_years: tuple[int, ...],
        evaluation_years: tuple[int, ...],
        seed: int,
        normalizer: str,
        stop_reason: str,
        threshold: float,
        epochs: int,
        algorithm: str,
        max_network_error: float,
        max_epochs: int,
        evaluation_interval: int,
        shuffle: bool,
        elapsed_time_millis: int,
        correlation_id: str,
        probe_features: NDArray[Any],
        training_error: float,
        training_metrics: ClassificationMetrics,
    ) -> Mapping[str, object]:
        _validate_path_segment(sport, "sport")
        _validate_path_segment(model_id, "model id")
        if sport.casefold() != schema.sport_id.casefold():
            raise ValidationError("model sport does not match schema")
        if not model_spec.layout or (
            model_spec.layout[0] != schema.input_width
            or model_spec.layout[-1] != schema.output_width
        ):
            raise ValidationError("model architecture does not match schema dimensions")
        if not _valid_preprocessor(preprocessor, schema.input_width):
            raise ValidationError("preprocessor state is invalid for model input")
        sport_root = self._sport_root(sport)
        sport_root.mkdir(parents=True, exist_ok=True)
        _fsync_directory(self.root)
        final_artifact = sport_root / f"{model_id}.model.keras"
        final_manifest = sport_root / f"{model_id}.json"
        if final_artifact.exists() or final_manifest.exists():
            raise RepositoryError("model identifier already exists")
        temporary = Path(tempfile.mkdtemp(prefix=".model-write-", dir=sport_root))
        artifact_published = manifest_published = False
        try:
            artifact_path = temporary / "model.keras"
            try:
                self.backend.save(model, artifact_path)
            except Exception as error:
                raise RepositoryError(
                    "backend failed to save model artifact"
                ) from error
            _fsync_file(artifact_path)
            descriptor = _descriptor(
                model_id=model_id,
                sport=sport,
                schema=schema,
                model_spec=model_spec,
                metrics=metrics,
                training_error=training_error,
                training_metrics=training_metrics,
                training_years=training_years,
                evaluation_years=evaluation_years,
                normalizer=normalizer,
                stop_reason=stop_reason,
                threshold=threshold,
                epochs=epochs,
                algorithm=algorithm,
                max_network_error=max_network_error,
                max_epochs=max_epochs,
                evaluation_interval=evaluation_interval,
                shuffle=shuffle,
                elapsed_time_millis=elapsed_time_millis,
                correlation_id=correlation_id,
            )
            manifest = {
                "artifactFormat": "keras-v1",
                "artifactVersion": 1,
                "modelId": model_id,
                "sport": sport,
                "featureSchemaVersion": schema.feature_schema_version,
                "featureNames": schema.feature_names,
                "featureNamesHash": _feature_hash(schema.feature_names),
                "outputClasses": schema.output_classes,
                "architecture": model_spec.layout,
                "modelSpec": asdict(model_spec),
                "backend": self.backend.identity,
                "preprocessor": asdict(preprocessor),
                "trainingYears": training_years,
                "evaluationYears": evaluation_years,
                "seed": seed,
                "normalizer": normalizer,
                "metrics": asdict(metrics),
                "trainingError": training_error,
                "trainingMetrics": asdict(replace(training_metrics, predictions=())),
                "stopReason": stop_reason,
                "threshold": threshold,
                "thresholdDecision": True,
                "epochs": epochs,
                "trainingSettings": {
                    "networkStructureAlgorithm": algorithm,
                    "maxNetworkError": max_network_error,
                    "maxNumberOfTrainingEpochs": max_epochs,
                    "networkEvaluationIndex": evaluation_interval,
                    "shuffle": shuffle,
                },
                "elapsedTimeMillis": elapsed_time_millis,
                "correlationId": correlation_id,
                "createdAt": datetime.now(UTC).isoformat(),
                "applicationVersion": "0.1.0",
                "descriptor": descriptor,
            }
            manifest_path = temporary / "manifest.json"
            manifest_path.write_text(
                json.dumps(
                    manifest, allow_nan=False, sort_keys=True, separators=(",", ":")
                ),
                encoding="utf-8",
            )
            _fsync_file(manifest_path)
            _fsync_directory(temporary)
            try:
                reloaded = self.backend.load(artifact_path, model_spec)
                probe = self.backend.predict_scores(reloaded, probe_features)
            except Exception as error:
                raise RepositoryError(
                    "saved artifact failed reload validation"
                ) from error
            if (
                probe.ndim != 2
                or probe.shape[0] != len(probe_features)
                or probe.shape[1] != schema.output_width
                or not np.isfinite(probe).all()
            ):
                raise RepositoryError(
                    "saved artifact returned invalid probe predictions"
                )
            try:
                with self._connection() as connection:
                    connection.execute("BEGIN IMMEDIATE")
                    if connection.execute(
                        "SELECT 1 FROM flat_models WHERE model_id = ?", (model_id,)
                    ).fetchone():
                        raise RepositoryError("model identifier already exists")
                    try:
                        os.link(artifact_path, final_artifact)
                        artifact_published = True
                        os.link(manifest_path, final_manifest)
                        manifest_published = True
                    except FileExistsError as error:
                        raise RepositoryError(
                            "model identifier already exists"
                        ) from error
                    _fsync_directory(sport_root)
                    connection.execute(
                        """
                        INSERT INTO flat_models
                        (model_id, sport, descriptor_json, manifest_path)
                        VALUES (?, ?, ?, ?)
                        """,
                        (
                            model_id,
                            sport,
                            json.dumps(
                                descriptor,
                                allow_nan=False,
                                sort_keys=True,
                                separators=(",", ":"),
                            ),
                            str(final_manifest),
                        ),
                    )
            except sqlite3.Error as error:
                raise RepositoryError(
                    "model publication or descriptor index update failed"
                ) from error
            except OSError as error:
                raise RepositoryError("model file publication failed") from error
            return descriptor
        finally:
            if artifact_published and not manifest_published:
                final_artifact.unlink()
            if temporary.exists():
                shutil.rmtree(temporary)

    def get_descriptor(self, model_id: str, sport: str) -> Mapping[str, object] | None:
        with self._connection() as connection:
            row = connection.execute(
                "SELECT descriptor_json FROM flat_models "
                "WHERE model_id = ? AND lower(sport) = lower(?)",
                (model_id, sport),
            ).fetchone()
        return json.loads(row[0]) if row else None

    def list_descriptors(self, sport: str) -> tuple[Mapping[str, object], ...]:
        with self._connection() as connection:
            rows = connection.execute(
                "SELECT descriptor_json FROM flat_models WHERE lower(sport) = lower(?) "
                "ORDER BY model_id ASC",
                (sport,),
            ).fetchall()
        return tuple(json.loads(row[0]) for row in rows)

    def load(
        self, model_id: str, sport: str, schema: SportSchema | None = None
    ) -> ModelHandle:
        descriptor = self.get_descriptor(model_id, sport)
        if descriptor is None:
            raise NotFoundError(f"unknown persisted model: {model_id}")
        _validate_path_segment(model_id, "model id")
        sport_root = self._sport_root(sport)
        manifest_path = sport_root / f"{model_id}.json"
        try:
            manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as error:
            raise RepositoryError("model manifest is missing or corrupt") from error
        if (
            manifest.get("modelId") != model_id
            or str(manifest.get("sport", "")).casefold() != sport.casefold()
        ):
            raise RepositoryError("model identity does not match its manifest")
        if (
            manifest.get("artifactFormat") != "keras-v1"
            or manifest.get("artifactVersion") != 1
        ):
            raise RepositoryError("unsupported model artifact format or version")
        if manifest.get("backend") != self.backend.identity:
            raise RepositoryError("model backend/runtime version mismatch")
        if schema is not None:
            if (
                manifest.get("featureSchemaVersion") != schema.feature_schema_version
                or tuple(manifest.get("featureNames", ())) != schema.feature_names
                or manifest.get("featureNamesHash")
                != _feature_hash(schema.feature_names)
                or tuple(manifest.get("outputClasses", ())) != schema.output_classes
            ):
                raise RepositoryError(
                    "model schema does not match requested sport schema"
                )
        try:
            spec = _model_spec_from_manifest(_manifest_mapping(manifest, "modelSpec"))
            preprocessor_data = _manifest_mapping(manifest, "preprocessor")
            preprocessor = Preprocessor(
                method=_manifest_string(preprocessor_data, "method"),
                offsets=_manifest_numbers(preprocessor_data, "offsets"),
                scales=_manifest_numbers(preprocessor_data, "scales"),
                constant_columns=_manifest_bools(preprocessor_data, "constant_columns"),
            )
            if schema is not None and (
                spec.layout[0] != schema.input_width
                or spec.layout[-1] != schema.output_width
                or spec.output_classes != schema.output_classes
            ):
                raise RepositoryError(
                    "model architecture does not match requested sport schema"
                )
            model = self.backend.load(sport_root / f"{model_id}.model.keras", spec)
            probe = np.zeros((1, spec.layout[0]), dtype=np.float64)
            predictions = self.backend.predict_scores(model, probe)
        except Exception as error:
            raise RepositoryError(
                "model artifact failed compatibility validation"
            ) from error
        if (
            predictions.shape != (1, len(spec.output_classes))
            or not np.isfinite(predictions).all()
            or not _valid_preprocessor(preprocessor, spec.layout[0])
        ):
            raise RepositoryError("model artifact produced invalid shape or state")
        return ModelHandle(descriptor, manifest, model, preprocessor, spec)

    def rebuild_index(self, sport: str) -> tuple[str, ...]:
        sport_root = self._sport_root(sport)
        valid: list[tuple[str, str, str]] = []
        invalid: list[str] = []
        with self._connection() as connection:
            connection.execute("BEGIN IMMEDIATE")
            manifests = sorted(sport_root.glob("*.json"))
            for manifest_path in manifests:
                model_id = manifest_path.name.removesuffix(".json")
                artifact_path = sport_root / f"{model_id}.model.keras"
                try:
                    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
                    if not isinstance(manifest, dict):
                        raise ValueError("manifest must be an object")
                    manifest_sport = _manifest_string(manifest, "sport")
                    if manifest_sport.casefold() != sport.casefold():
                        raise ValueError("manifest sport does not match directory")
                    _validate_path_segment(model_id, "model id")
                    if (
                        manifest.get("modelId") != model_id
                        or not artifact_path.is_file()
                    ):
                        raise ValueError("manifest identity or artifact mismatch")
                    _validate_bundle(manifest, artifact_path, self.backend)
                    descriptor = _manifest_mapping(manifest, "descriptor")
                    if (
                        descriptor.get("networkId") != model_id
                        or descriptor.get("networkType") != manifest_sport
                        or descriptor.get("networkSaved") is not True
                    ):
                        raise ValueError("descriptor identity mismatch")
                    valid.append(
                        (
                            model_id,
                            json.dumps(descriptor, allow_nan=False, sort_keys=True),
                            str(manifest_path),
                        )
                    )
                except (OSError, ValueError, KeyError, TypeError, ValidationError):
                    invalid.append(model_id)
            for artifact_path in sorted(sport_root.glob("*.model.keras")):
                model_id = artifact_path.name.removesuffix(".model.keras")
                if not (sport_root / f"{model_id}.json").is_file():
                    invalid.append(model_id)
            connection.execute(
                "DELETE FROM flat_models WHERE lower(sport) = lower(?)", (sport,)
            )
            connection.executemany(
                "INSERT INTO flat_models "
                "(model_id, sport, descriptor_json, manifest_path) "
                "VALUES (?, ?, ?, ?)",
                [
                    (model_id, sport, descriptor, path)
                    for model_id, descriptor, path in valid
                ],
            )
        return tuple(invalid)

    def _sport_root(self, sport: str) -> Path:
        _validate_path_segment(sport, "sport")
        return self.root / sport.upper()

    @contextmanager
    def _connection(self) -> Iterator[sqlite3.Connection]:
        connection = sqlite3.connect(self.index_path, timeout=5.0)
        try:
            connection.execute("PRAGMA journal_mode=WAL")
            connection.execute("PRAGMA foreign_keys=ON")
            yield connection
            connection.commit()
        except Exception:
            connection.rollback()
            raise
        finally:
            connection.close()


def _validate_path_segment(value: str, label: str) -> None:
    if not value or not re.fullmatch(r"[A-Za-z0-9_-]+", value):
        raise ValidationError(f"invalid {label}")


def _feature_hash(names: tuple[str, ...]) -> str:
    import hashlib

    return hashlib.sha256("\n".join(names).encode("utf-8")).hexdigest()


def _validate_bundle(
    manifest: Mapping[str, object],
    artifact_path: Path,
    backend: ArtifactBackend,
) -> None:
    if (
        manifest.get("artifactFormat") != "keras-v1"
        or manifest.get("artifactVersion") != 1
    ):
        raise ValueError("unsupported artifact format")
    if manifest.get("backend") != backend.identity:
        raise ValueError("backend/runtime version mismatch")
    feature_names = _manifest_strings(manifest, "featureNames")
    if manifest.get("featureNamesHash") != _feature_hash(feature_names):
        raise ValueError("feature schema hash mismatch")
    output_classes = _manifest_ints(manifest, "outputClasses")
    spec = _model_spec_from_manifest(_manifest_mapping(manifest, "modelSpec"))
    if (
        len(spec.layout) < 3
        or spec.layout[0] != len(feature_names)
        or spec.layout[-1] != len(output_classes)
        or spec.output_classes != output_classes
    ):
        raise ValueError("model architecture does not match manifest schema")
    preprocessor_data = _manifest_mapping(manifest, "preprocessor")
    preprocessor = Preprocessor(
        method=_manifest_string(preprocessor_data, "method"),
        offsets=tuple(_manifest_numbers(preprocessor_data, "offsets")),
        scales=tuple(_manifest_numbers(preprocessor_data, "scales")),
        constant_columns=_manifest_bools(preprocessor_data, "constant_columns"),
    )
    if not _valid_preprocessor(preprocessor, spec.layout[0]):
        raise ValueError("preprocessor state is invalid for model input")
    try:
        model = backend.load(artifact_path, spec)
        predictions = backend.predict_scores(
            model, np.zeros((1, spec.layout[0]), dtype=np.float64)
        )
    except Exception as error:
        raise ValueError("artifact failed reload validation") from error
    if (
        predictions.shape != (1, len(output_classes))
        or not np.isfinite(predictions).all()
    ):
        raise ValueError("artifact returned invalid probe predictions")


def _valid_preprocessor(preprocessor: Preprocessor, width: int) -> bool:
    return (
        len(preprocessor.offsets) == width
        and len(preprocessor.scales) == width
        and len(preprocessor.constant_columns) == width
        and preprocessor.method
        in {
            "NORMALIZER_STANDARDIZE",
            "NORMALIZER_ZERO_TO_ONE",
            "NORMALIZER_NEGATIVE_ONE_TO_ONE",
        }
        and bool(np.isfinite(preprocessor.offsets).all())
        and bool(np.isfinite(preprocessor.scales).all())
        and all(scale > 0 for scale in preprocessor.scales)
    )


def _descriptor(
    *,
    model_id: str,
    sport: str,
    schema: SportSchema,
    model_spec: ModelSpec,
    metrics: ClassificationMetrics,
    training_error: float,
    training_metrics: ClassificationMetrics,
    training_years: tuple[int, ...],
    evaluation_years: tuple[int, ...],
    normalizer: str,
    stop_reason: str,
    threshold: float,
    epochs: int,
    algorithm: str,
    max_network_error: float,
    max_epochs: int,
    evaluation_interval: int,
    shuffle: bool,
    elapsed_time_millis: int,
    correlation_id: str,
) -> dict[str, object]:
    now = datetime.now(UTC).strftime("%Y-%m-%d %H:%M:%S")
    return {
        "networkId": model_id,
        "networkType": sport,
        "numberOfInputs": schema.input_width,
        "numberOfOutputs": schema.output_width,
        "yearsToTrainNetwork": list(training_years),
        "yearsToEvaluateNetwork": list(evaluation_years),
        "networkLayout": "x".join(map(str, model_spec.layout)),
        "activationFunction": model_spec.hidden_activation,
        "outputLayerActivation": model_spec.output_activation,
        "lossFunction": model_spec.loss,
        "outputLayerLossFunction": model_spec.loss,
        "numberOfEpochs": epochs,
        "maxNumberOfTrainingEpochs": max_epochs,
        "networkEvaluationIndex": evaluation_interval,
        "maxNetworkError": max_network_error,
        "shuffle": shuffle,
        "epochStatsIndexCount": epochs,
        "weightInit": model_spec.initializer,
        "updater": model_spec.optimizer,
        "learningRuleLearningRate": model_spec.learning_rate,
        "beta1DecayScaleValue": 0.9,
        "l2RegularizationLambda": model_spec.l2,
        "normalizer": normalizer,
        "whenTrained": now,
        "networkSaved": True,
        **evaluation_report_fields(metrics, schema.output_classes),
        **training_report_fields(training_metrics, schema.output_classes),
        "trainingError": training_error,
        "networkThreshold": threshold,
        "networkPerformanceThreshold": threshold,
        "networkStructureAlgorithm": algorithm,
        "numberOfHiddenLayers": len(model_spec.layout) - 2,
        "batchTrainingRunId": correlation_id,
        "featureSchemaVersion": schema.feature_schema_version,
        "elapsedTimeMillis": elapsed_time_millis,
        "stopReason": stop_reason,
    }


def _manifest_mapping(payload: Mapping[str, object], key: str) -> Mapping[str, object]:
    value = payload.get(key)
    if not isinstance(value, dict):
        raise ValueError(f"manifest field {key} must be an object")
    return value


def _model_spec_from_manifest(values: Mapping[str, object]) -> ModelSpec:
    return ModelSpec(
        layout=_manifest_ints(values, "layout"),
        output_classes=_manifest_ints(values, "output_classes"),
        hidden_activation=_manifest_string(values, "hidden_activation"),
        output_activation=_manifest_string(values, "output_activation"),
        loss=_manifest_string(values, "loss"),
        optimizer=_manifest_string(values, "optimizer"),
        initializer=_manifest_string(values, "initializer"),
        learning_rate=_as_float(values.get("learning_rate")),
        l2=_as_float(values.get("l2")),
        seed=_as_int(values.get("seed")),
    )


def _manifest_string(payload: Mapping[str, object], key: str) -> str:
    value = payload.get(key)
    if not isinstance(value, str):
        raise ValueError(f"manifest field {key} must be a string")
    return value


def _manifest_strings(payload: Mapping[str, object], key: str) -> tuple[str, ...]:
    value = payload.get(key)
    if not isinstance(value, list) or not all(isinstance(item, str) for item in value):
        raise ValueError(f"manifest field {key} must be a string array")
    return tuple(value)


def _manifest_ints(payload: Mapping[str, object], key: str) -> tuple[int, ...]:
    value = payload.get(key)
    if not isinstance(value, (list, tuple)) or not all(
        isinstance(item, int) and not isinstance(item, bool) for item in value
    ):
        raise ValueError(f"manifest field {key} must be an integer array")
    return tuple(value)


def _manifest_numbers(payload: Mapping[str, object], key: str) -> tuple[float, ...]:
    value = payload.get(key)
    if not isinstance(value, list) or not all(
        isinstance(item, (int, float))
        and not isinstance(item, bool)
        and np.isfinite(item)
        for item in value
    ):
        raise ValueError(f"manifest field {key} must be a finite numeric array")
    return tuple(float(item) for item in value)


def _manifest_bools(payload: Mapping[str, object], key: str) -> tuple[bool, ...]:
    value = payload.get(key)
    if not isinstance(value, list) or not all(isinstance(item, bool) for item in value):
        raise ValueError(f"manifest field {key} must be a boolean array")
    return tuple(value)


def _as_int(value: object) -> int:
    if not isinstance(value, int) or isinstance(value, bool):
        raise ValueError("manifest integer value is invalid")
    return value


def _as_float(value: object) -> float:
    if not isinstance(value, (int, float)) or isinstance(value, bool):
        raise ValueError("manifest numeric value is invalid")
    result = float(value)
    if not np.isfinite(result):
        raise ValueError("manifest numeric value must be finite")
    return result


def _fsync_file(path: Path) -> None:
    with path.open("rb") as stream:
        os.fsync(stream.fileno())


def _fsync_directory(path: Path) -> None:
    descriptor = os.open(path, os.O_RDONLY)
    try:
        os.fsync(descriptor)
    finally:
        os.close(descriptor)
