import logging
from pathlib import Path
from types import SimpleNamespace

import pytest
from fastapi.testclient import TestClient

from sportsnet.adapters.sqlite_store import SQLiteEventStore
from sportsnet.application.dependencies import ApplicationDependencies
from sportsnet.application.management import ManagementService
from sportsnet.config import AppSettings
from sportsnet.domain.schemas import SportSchema
from sportsnet.main import create_app
from sportsnet.sports.registry import SportRegistry


class _Models:
    def list_descriptors(self, sport: str):
        return ({"networkId": "saved-1", "networkSaved": True},)

    def get_descriptor(self, model_id: str, sport: str):
        return None


class _Training:
    def __init__(self) -> None:
        self.request = None

    def train(self, request):
        self.request = request
        return SimpleNamespace(
            status="completed_below_threshold",
            descriptor=None,
            report={"networkId": "m-1", "networkSaved": False, "trainingError": 0.4},
        )


_GENERATED_LAYOUT = {
    "networkStructureAlgorithm": "HOUR_GLASS",
    "numberOfHiddenLayers": 3,
}


def _client(tmp_path: Path):
    schema = SportSchema(
        sport_id="NCAABB",
        display_name="NCAA",
        feature_schema_version="test-v1",
        feature_names=("x",),
        output_classes=(0, 1),
        season_years=(2020, 2021),
        training_years=(2020, 2021),
    )
    registry = SportRegistry((schema,))
    management = ManagementService(SQLiteEventStore(tmp_path / "events.sqlite"))
    training = _Training()
    dependencies = ApplicationDependencies(
        registry=registry,
        management=management,
        training=training,
        models=_Models(),
    )
    settings = AppSettings(
        resource_root=tmp_path, event_database=tmp_path / "events.sqlite"
    )
    return TestClient(create_app(settings, dependencies)), training


def test_network_routes_return_only_persisted_metadata_and_no_unsaved_filter(
    tmp_path: Path,
) -> None:
    client, _ = _client(tmp_path)
    with client:
        assert client.get("/ncaabb/networks").json() == [
            {"networkId": "saved-1", "networkSaved": True}
        ]
        assert client.get("/ncaabb/networks?unsaved=true").status_code == 400
        assert client.get("/ncaabb/networks/not-a-uuid").status_code == 400
        assert client.get("/health/live").json() == {"status": "UP"}
        assert client.get("/health/ready").status_code == 200


def test_below_threshold_training_returns_unsaved_report_and_threshold_alias_precedence(
    tmp_path: Path,
) -> None:
    client, training = _client(tmp_path)
    with client:
        response = client.post(
            "/ncaabb/networks",
            json={
                "yearsToTrainNetwork": [2020],
                "yearsToEvaluateNetwork": [2021],
                "networkStructureAlgorithm": "USER_DEFINED",
                "networkLayout": "1x2x2",
                "numberOfHiddenLayers": 1,
                "networkThreshold": 40,
                "networkPerformanceThreshold": 65,
            },
        )
    assert response.status_code == 200
    assert response.json() == {
        "networkId": "m-1",
        "networkSaved": False,
        "trainingError": 0.4,
    }
    assert training.request.performance_threshold == 65


def test_training_years_accept_csv_strings_and_preserve_every_year(
    tmp_path: Path,
) -> None:
    client, training = _client(tmp_path)
    with client:
        response = client.post(
            "/ncaabb/networks",
            json={
                "yearsToTrainNetwork": "2019, 2020",
                "yearsToEvaluateNetwork": "2021,2022",
                **_GENERATED_LAYOUT,
            },
        )

    assert response.status_code == 200
    assert training.request.training_years == (2019, 2020)
    assert training.request.evaluation_years == (2021, 2022)


def test_training_year_csv_rejects_empty_or_non_integer_entries(
    tmp_path: Path,
) -> None:
    client, _ = _client(tmp_path)
    with client:
        response = client.post(
            "/ncaabb/networks",
            json={
                "yearsToTrainNetwork": "2019,,2020",
                "yearsToEvaluateNetwork": "2021",
                **_GENERATED_LAYOUT,
            },
        )

    assert response.status_code == 400


def test_reporting_interval_is_optional_and_passed_to_training(
    tmp_path: Path,
) -> None:
    client, training = _client(tmp_path)
    body = {
        "yearsToTrainNetwork": [2020],
        "yearsToEvaluateNetwork": [2021],
        **_GENERATED_LAYOUT,
    }
    with client:
        assert client.post("/ncaabb/networks", json=body).status_code == 200
        assert training.request.reporting_interval is None
        response = client.post(
            "/ncaabb/networks", json={**body, "reportingInterval": 5}
        )
    assert response.status_code == 200
    assert training.request.reporting_interval == 5


def test_unknown_properties_are_ignored(tmp_path: Path) -> None:
    client, training = _client(tmp_path)
    with client:
        response = client.post(
            "/ncaabb/networks",
            json={**_BASE, **_GENERATED_LAYOUT, "networkLayout-ignored": "1x2x2"},
        )
    assert response.status_code == 200
    assert training.request.user_layout is None


def test_missing_field_400_names_the_field_and_logs_warning(
    tmp_path: Path, caplog: pytest.LogCaptureFixture
) -> None:
    client, _ = _client(tmp_path)
    caplog.set_level(logging.WARNING)
    with client:
        response = client.post(
            "/ncaabb/networks",
            json={"yearsToEvaluateNetwork": [2021], "secret": "secret-1"},
            headers={"x-correlation-id": "corr-1"},
        )
    body = response.json()
    assert response.status_code == 400
    assert body["code"] == "invalid_request"
    assert body["correlationId"] == "corr-1"
    assert "yearsToTrainNetwork" in body["message"]
    assert {"field": "yearsToTrainNetwork", "message": "Field required"} in body[
        "details"
    ]
    assert "secret-1" not in response.text
    logged = " ".join(record.getMessage() for record in caplog.records)
    assert "yearsToTrainNetwork" in logged and "corr-1" in logged
    assert "secret-1" not in logged


def test_rule_violation_400_explains_the_cause(tmp_path: Path) -> None:
    client, _ = _client(tmp_path)
    with client:
        response = client.post(
            "/ncaabb/networks",
            json={
                **_BASE,
                "networkStructureAlgorithm": "TAPERED_TREE",
                "numberOfHiddenLayers": 2,
            },
        )
    body = response.json()
    assert response.status_code == 400
    assert "numberOfHiddenLayers" in body["message"]
    assert "at least 3" in body["message"]


def test_domain_validation_400_is_logged(
    tmp_path: Path, caplog: pytest.LogCaptureFixture
) -> None:
    client, _ = _client(tmp_path)
    caplog.set_level(logging.WARNING)
    with client:
        response = client.get("/ncaabb/networks?unsaved=true")
    assert response.status_code == 400
    assert "unsaved" in response.json()["message"]
    assert any("unsaved" in record.getMessage() for record in caplog.records)


def test_reporting_interval_must_be_positive(tmp_path: Path) -> None:
    client, _ = _client(tmp_path)
    with client:
        response = client.post(
            "/ncaabb/networks",
            json={
                "yearsToTrainNetwork": [2020],
                "yearsToEvaluateNetwork": [2021],
                "reportingInterval": 0,
                **_GENERATED_LAYOUT,
            },
        )
    assert response.status_code == 400


_BASE = {"yearsToTrainNetwork": [2020], "yearsToEvaluateNetwork": [2021]}


def test_network_structure_algorithm_is_required(tmp_path: Path) -> None:
    client, _ = _client(tmp_path)
    with client:
        response = client.post(
            "/ncaabb/networks", json={**_BASE, "numberOfHiddenLayers": 3}
        )
    assert response.status_code == 400


def test_generated_algorithms_require_at_least_three_hidden_layers(
    tmp_path: Path,
) -> None:
    client, training = _client(tmp_path)
    with client:
        for algorithm in ("HOUR_GLASS", "TAPERED_TREE"):
            body = {**_BASE, "networkStructureAlgorithm": algorithm}
            assert client.post("/ncaabb/networks", json=body).status_code == 400
            for count in (0, 1, 2):
                response = client.post(
                    "/ncaabb/networks", json={**body, "numberOfHiddenLayers": count}
                )
                assert response.status_code == 400
            response = client.post(
                "/ncaabb/networks", json={**body, "numberOfHiddenLayers": 3}
            )
            assert response.status_code == 200
            assert training.request.hidden_layer_count == 3


def test_user_defined_requires_layout_and_ignores_hidden_layer_count(
    tmp_path: Path,
) -> None:
    client, training = _client(tmp_path)
    with client:
        body = {**_BASE, "networkStructureAlgorithm": "USER_DEFINED_RAND"}
        assert client.post("/ncaabb/networks", json=body).status_code == 400
        response = client.post(
            "/ncaabb/networks", json={**body, "networkLayout": "1x2x2"}
        )
    assert response.status_code == 200
    assert training.request.user_layout == "1x2x2"
    assert training.request.hidden_layer_count is None


def test_generated_algorithms_ignore_network_layout(tmp_path: Path) -> None:
    client, training = _client(tmp_path)
    with client:
        response = client.post(
            "/ncaabb/networks",
            json={**_BASE, **_GENERATED_LAYOUT, "networkLayout": "1x2x2"},
        )
    assert response.status_code == 200
    assert training.request.user_layout is None
    assert training.request.hidden_layer_count == 3
