import json
import os
import sqlite3
from concurrent.futures import ThreadPoolExecutor
from dataclasses import replace
from pathlib import Path
from threading import Event

import numpy as np
import pytest

from sportsnet.adapters.model_repository import FilesystemModelRepository
from sportsnet.domain.errors import RepositoryError, ValidationError
from sportsnet.domain.metrics import ClassificationMetrics
from sportsnet.domain.models import ModelSpec
from sportsnet.domain.preprocessing import Preprocessor
from sportsnet.domain.schemas import SportSchema


class _FakeBackend:
    identity = {"backend": "fake", "version": "1"}

    def __init__(self) -> None:
        self.loads = 0

    def save(self, handle: object, path: Path) -> None:
        path.write_text(json.dumps({"valid": True}), encoding="utf-8")

    def load(self, path: Path, expected_spec: ModelSpec) -> object:
        self.loads += 1
        if json.loads(path.read_text(encoding="utf-8")) != {"valid": True}:
            raise ValueError("invalid fake model")
        return {"spec": expected_spec}

    def predict_scores(self, handle: object, features: np.ndarray) -> np.ndarray:
        return np.tile(np.asarray([[0.7, 0.3]]), (len(features), 1))


def _arguments(model: object) -> dict[str, object]:
    schema = SportSchema(
        sport_id="synthetic",
        display_name="Synthetic",
        feature_schema_version="synthetic-v1",
        feature_names=("x", "y"),
        output_classes=(0, 1),
        season_years=(2020, 2021),
        training_years=(2020, 2021),
    )
    return {
        "model_id": "model-123",
        "sport": schema.sport_id,
        "schema": schema,
        "model": model,
        "model_spec": ModelSpec(
            layout=(2, 3, 2),
            output_classes=(0, 1),
            hidden_activation="RELU",
            output_activation="SOFTMAX",
            loss="SPARSE_MCXENT",
            optimizer="ADAM",
            initializer="XAVIER_UNIFORM",
            learning_rate=0.01,
            l2=0.0,
            seed=1,
        ),
        "preprocessor": Preprocessor.fit(
            [[1.0, 2.0], [2.0, 3.0]], "NORMALIZER_STANDARDIZE"
        ),
        "metrics": ClassificationMetrics(
            1.0, 1.0, 1.0, 1.0, 1.0, ((1, 0), (0, 1)), (0, 1)
        ),
        "training_years": (2020,),
        "evaluation_years": (2021,),
        "seed": 1,
        "normalizer": "NORMALIZER_STANDARDIZE",
        "stop_reason": "max_network_error_reached",
        "threshold": 70.0,
        "epochs": 4,
        "algorithm": "DEFAULT",
        "max_network_error": 0.1,
        "max_epochs": 9,
        "evaluation_interval": 3,
        "shuffle": True,
        "elapsed_time_millis": 12,
        "correlation_id": "run-1",
        "probe_features": np.asarray([[0.0, 0.0]]),
        "training_error": 0.05,
        "training_metrics": ClassificationMetrics(
            0.9, 0.8, 0.95, 0.85, 0.875, ((2, 0), (1, 1)), (0, 0, 0, 1)
        ),
    }


def test_artifacts_are_reload_validated_and_listing_avoids_model_load(
    tmp_path: Path,
) -> None:
    backend = _FakeBackend()
    repository = FilesystemModelRepository(tmp_path / "models", backend)
    descriptor = repository.persist_model(**_arguments(object()))
    assert descriptor["networkSaved"] is True
    assert (repository.root / "SYNTHETIC" / "model-123.model.keras").is_file()
    assert (repository.root / "SYNTHETIC" / "model-123.json").is_file()
    assert backend.loads == 1
    assert repository.list_descriptors("synthetic") == (descriptor,)
    assert backend.loads == 1
    loaded = repository.load("model-123", "synthetic")
    assert loaded.descriptor == descriptor
    assert backend.loads == 2


def test_failed_identity_or_artifact_save_is_not_indexed(tmp_path: Path) -> None:
    backend = _FakeBackend()
    repository = FilesystemModelRepository(tmp_path / "models", backend)
    arguments = _arguments(object())
    arguments["sport"] = "../escape"
    with pytest.raises(ValidationError):
        repository.persist_model(**arguments)
    assert repository.list_descriptors("synthetic") == ()


def test_invalid_preprocessor_is_not_published(tmp_path: Path) -> None:
    repository = FilesystemModelRepository(tmp_path, _FakeBackend())
    arguments = _arguments(object())
    arguments["preprocessor"] = replace(arguments["preprocessor"], scales=(0.0, 1.0))
    with pytest.raises(ValidationError, match="preprocessor"):
        repository.persist_model(**arguments)
    assert not (tmp_path / "SYNTHETIC" / "model-123.model.keras").exists()
    assert not (tmp_path / "SYNTHETIC" / "model-123.json").exists()
    assert repository.list_descriptors("synthetic") == ()


def test_reindex_excludes_artifacts_that_cannot_be_loaded(tmp_path: Path) -> None:
    backend = _FakeBackend()
    repository = FilesystemModelRepository(tmp_path / "models", backend)
    repository.persist_model(**_arguments(object()))
    artifact = tmp_path / "models" / "SYNTHETIC" / "model-123.model.keras"
    artifact.write_text('{"valid": false}', encoding="utf-8")
    with sqlite3.connect(repository.index_path) as connection:
        connection.execute("DELETE FROM flat_models")

    invalid = repository.rebuild_index("synthetic")

    assert invalid == ("model-123",)
    assert repository.list_descriptors("synthetic") == ()
    assert backend.loads == 2


def test_manifest_and_descriptor_preserve_training_request_settings(
    tmp_path: Path,
) -> None:
    repository = FilesystemModelRepository(tmp_path / "models", _FakeBackend())
    descriptor = repository.persist_model(**_arguments(object()))
    manifest_path = tmp_path / "models" / "SYNTHETIC" / "model-123.json"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    assert manifest["descriptor"] == descriptor

    assert manifest["trainingSettings"] == {
        "maxNetworkError": 0.1,
        "maxNumberOfTrainingEpochs": 9,
        "networkEvaluationIndex": 3,
        "networkStructureAlgorithm": "DEFAULT",
        "shuffle": True,
    }
    assert descriptor["maxNetworkError"] == 0.1
    assert descriptor["maxNumberOfTrainingEpochs"] == 9
    assert descriptor["networkEvaluationIndex"] == 3
    assert descriptor["networkStructureAlgorithm"] == "DEFAULT"


def test_loading_rejects_runtime_backend_version_mismatch(tmp_path: Path) -> None:
    backend = _FakeBackend()
    repository = FilesystemModelRepository(tmp_path / "models", backend)
    repository.persist_model(**_arguments(object()))
    backend.identity = {"backend": "fake", "version": "2"}

    with pytest.raises(RepositoryError, match="version"):
        repository.load("model-123", "synthetic")


def test_descriptor_training_error_is_the_training_data_error(tmp_path: Path) -> None:
    repository = FilesystemModelRepository(tmp_path / "models", _FakeBackend())
    descriptor = repository.persist_model(**_arguments(object()))
    assert descriptor["trainingError"] == 0.05
    assert descriptor["networkRecall"] == 1.0
    assert descriptor["trainingRecall"] == 0.95
    assert descriptor["trainingAccuracy"] == 0.9
    assert descriptor["trainingConfusionMatrixCsv"] == (
        "Actual\\Predicted,0,1\n0,2,0\n1,1,1\n"
    )
    assert descriptor["confusionMatrixCsv"].startswith("Actual\\Predicted,")
    repository.rebuild_index("synthetic")
    assert repository.list_descriptors("synthetic") == (descriptor,)


def test_reindex_recovers_exact_descriptor_and_is_idempotent(tmp_path: Path) -> None:
    repository = FilesystemModelRepository(tmp_path, _FakeBackend())
    descriptor = repository.persist_model(**_arguments(object()))
    with sqlite3.connect(repository.index_path) as connection:
        connection.execute("DELETE FROM flat_models")
    assert repository.rebuild_index("synthetic") == ()
    assert repository.list_descriptors("synthetic") == (descriptor,)
    assert repository.rebuild_index("synthetic") == ()
    assert repository.list_descriptors("synthetic") == (descriptor,)


def test_sport_directory_reindex_preserves_other_sport(tmp_path: Path) -> None:
    repository = FilesystemModelRepository(tmp_path, _FakeBackend())
    first = repository.persist_model(**_arguments(object()))
    arguments = _arguments(object())
    arguments["model_id"] = "other-id"
    arguments["sport"] = "other"
    arguments["schema"] = replace(arguments["schema"], sport_id="other")
    second = repository.persist_model(**arguments)
    assert (tmp_path / "SYNTHETIC" / "model-123.model.keras").is_file()
    assert (tmp_path / "OTHER" / "other-id.model.keras").is_file()
    assert (tmp_path / "OTHER" / "other-id.json").is_file()
    assert repository.load("other-id", "OTHER").descriptor == second
    assert repository.rebuild_index("synthetic") == ()
    assert repository.list_descriptors("synthetic") == (first,)
    assert repository.list_descriptors("other") == (second,)
    assert repository.rebuild_index("other") == ()
    assert repository.list_descriptors("other") == (second,)


@pytest.mark.parametrize(("sport", "directory"), [("NCAABB", "NCAABB"), ("mlb", "MLB")])
def test_sport_paths_are_uppercase_and_load_case_insensitively(
    tmp_path: Path, sport: str, directory: str
) -> None:
    repository = FilesystemModelRepository(tmp_path, _FakeBackend())
    arguments = _arguments(object())
    arguments["sport"] = sport
    arguments["schema"] = replace(arguments["schema"], sport_id=sport)
    descriptor = repository.persist_model(**arguments)
    assert (tmp_path / directory / "model-123.model.keras").is_file()
    assert (tmp_path / directory / "model-123.json").is_file()
    assert repository.load("model-123", sport.swapcase()).descriptor == descriptor
    assert repository.rebuild_index(sport.swapcase()) == ()
    assert repository.list_descriptors(sport) == (descriptor,)


def test_reindex_reports_only_issues_in_requested_sport(tmp_path: Path) -> None:
    repository = FilesystemModelRepository(tmp_path, _FakeBackend())
    repository.persist_model(**_arguments(object()))
    other = tmp_path / "OTHER"
    other.mkdir()
    (other / "orphan.model.keras").write_text("orphan", encoding="utf-8")
    (other / "broken.json").write_text("not json", encoding="utf-8")
    assert repository.rebuild_index("synthetic") == ()
    assert set(repository.rebuild_index("other")) == {"orphan", "broken"}
    assert len(repository.list_descriptors("synthetic")) == 1


def test_reindex_rejects_mismatched_sport_in_directory(tmp_path: Path) -> None:
    repository = FilesystemModelRepository(tmp_path, _FakeBackend())
    repository.persist_model(**_arguments(object()))
    path = tmp_path / "SYNTHETIC" / "model-123.json"
    manifest = json.loads(path.read_text(encoding="utf-8"))
    manifest["sport"] = "other"
    path.write_text(json.dumps(manifest), encoding="utf-8")
    assert repository.rebuild_index("synthetic") == ("model-123",)
    assert repository.list_descriptors("synthetic") == ()


@pytest.mark.parametrize("filename", ["model-123.model.keras", "model-123.json"])
def test_existing_file_collision_never_overwrites(
    tmp_path: Path, filename: str
) -> None:
    repository = FilesystemModelRepository(tmp_path, _FakeBackend())
    directory = tmp_path / "SYNTHETIC"
    directory.mkdir()
    existing = directory / filename
    existing.write_text("existing", encoding="utf-8")
    with pytest.raises(RepositoryError, match="already exists"):
        repository.persist_model(**_arguments(object()))
    assert existing.read_text(encoding="utf-8") == "existing"
    assert repository.list_descriptors("synthetic") == ()


def test_same_id_across_sports_never_overwrites(tmp_path: Path) -> None:
    repository = FilesystemModelRepository(tmp_path, _FakeBackend())
    first = repository.persist_model(**_arguments(object()))
    arguments = _arguments(object())
    arguments["sport"] = "other"
    arguments["schema"] = replace(arguments["schema"], sport_id="other")
    with pytest.raises(RepositoryError, match="already exists"):
        repository.persist_model(**arguments)
    assert repository.list_descriptors("synthetic") == (first,)
    assert repository.list_descriptors("other") == ()


def test_existing_index_entry_prevents_reuse_after_files_removed(
    tmp_path: Path,
) -> None:
    repository = FilesystemModelRepository(tmp_path, _FakeBackend())
    original = repository.persist_model(**_arguments(object()))
    (tmp_path / "SYNTHETIC" / "model-123.model.keras").unlink()
    (tmp_path / "SYNTHETIC" / "model-123.json").unlink()
    with pytest.raises(RepositoryError, match="already exists"):
        repository.persist_model(**_arguments(object()))
    assert repository.get_descriptor("model-123", "synthetic") == original
    assert not (tmp_path / "SYNTHETIC" / "model-123.model.keras").exists()


def test_reindex_reports_incomplete_pairs(tmp_path: Path) -> None:
    repository = FilesystemModelRepository(tmp_path, _FakeBackend())
    repository.persist_model(**_arguments(object()))
    (tmp_path / "SYNTHETIC" / "model-123.model.keras").unlink()
    (tmp_path / "SYNTHETIC" / "orphan.model.keras").write_text(
        "orphan", encoding="utf-8"
    )
    assert set(repository.rebuild_index("synthetic")) == {"model-123", "orphan"}
    assert repository.list_descriptors("synthetic") == ()


@pytest.mark.parametrize(
    ("field", "value"),
    [("scales", [0.0, 1.0]), ("constant_columns", [False]), ("method", "INVALID")],
)
def test_reindex_rejects_invalid_preprocessor(
    tmp_path: Path, field: str, value: object
) -> None:
    repository = FilesystemModelRepository(tmp_path, _FakeBackend())
    repository.persist_model(**_arguments(object()))
    path = tmp_path / "SYNTHETIC" / "model-123.json"
    manifest = json.loads(path.read_text(encoding="utf-8"))
    manifest["preprocessor"][field] = value
    path.write_text(json.dumps(manifest), encoding="utf-8")
    assert repository.rebuild_index("synthetic") == ("model-123",)
    assert repository.list_descriptors("synthetic") == ()


def test_failed_json_publication_removes_only_new_model(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    repository = FilesystemModelRepository(tmp_path, _FakeBackend())
    real_link = os.link

    def failing_link(source, destination):
        if str(destination).endswith(".json"):
            raise FileExistsError("simulated collision")
        real_link(source, destination)

    monkeypatch.setattr(os, "link", failing_link)
    with pytest.raises(RepositoryError, match="already exists"):
        repository.persist_model(**_arguments(object()))
    assert not (tmp_path / "SYNTHETIC" / "model-123.model.keras").exists()
    assert repository.list_descriptors("synthetic") == ()


def test_index_failure_leaves_complete_pair_recoverable(tmp_path: Path) -> None:
    repository = FilesystemModelRepository(tmp_path, _FakeBackend())
    with sqlite3.connect(repository.index_path) as connection:
        connection.execute(
            "CREATE TRIGGER reject_insert BEFORE INSERT ON flat_models "
            "BEGIN SELECT RAISE(FAIL, 'test failure'); END"
        )
    with pytest.raises(RepositoryError, match="index update failed"):
        repository.persist_model(**_arguments(object()))
    assert repository.list_descriptors("synthetic") == ()
    assert (tmp_path / "SYNTHETIC" / "model-123.model.keras").is_file()
    assert (tmp_path / "SYNTHETIC" / "model-123.json").is_file()
    with sqlite3.connect(repository.index_path) as connection:
        connection.execute("DROP TRIGGER reject_insert")
    assert repository.rebuild_index("synthetic") == ()
    assert repository.get_descriptor("model-123", "synthetic") is not None


def test_reindex_serializes_with_publication(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    repository = FilesystemModelRepository(tmp_path, _FakeBackend())
    real_link = os.link
    model_published, release = Event(), Event()

    def blocking_link(source, destination):
        real_link(source, destination)
        if str(destination).endswith(".model.keras"):
            model_published.set()
            if not release.wait(5):
                raise TimeoutError("publication was not released")

    monkeypatch.setattr(os, "link", blocking_link)
    with ThreadPoolExecutor(max_workers=2) as executor:
        publication = executor.submit(repository.persist_model, **_arguments(object()))
        try:
            assert model_published.wait(5)
            reindex = executor.submit(repository.rebuild_index, "synthetic")
            assert not reindex.done()
        finally:
            release.set()
        descriptor = publication.result(timeout=5)
        assert reindex.result(timeout=5) == ()
    assert repository.list_descriptors("synthetic") == (descriptor,)
