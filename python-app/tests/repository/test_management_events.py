from pathlib import Path

import pytest

from sportsnet.adapters.sqlite_store import SQLiteEventStore
from sportsnet.application.management import ManagementService
from sportsnet.domain.errors import ValidationError
from sportsnet.domain.events import ManagementEventType


def test_events_are_durable_and_search_filters_intersect(tmp_path: Path) -> None:
    database = tmp_path / "events.sqlite"
    identifiers = iter(("event-1", "event-2", "event-3"))
    first = ManagementService(
        SQLiteEventStore(database),
        id_factory=lambda: next(identifiers),
        clock=lambda: 100,
    )
    created = first.create(
        management_event_type=ManagementEventType.TRAIN_NETWORK_START,
        sport="NCAABB",
        host_name="test-host",
        message="started",
    )
    assert created.event_id == created.correlation_id == "event-1"
    first.create(
        management_event_type=ManagementEventType.TRAIN_NETWORK_END,
        sport="NCAABB",
        host_name="test-host",
        message="finished",
        correlation_id="run-2",
    )
    first.create(
        management_event_type=ManagementEventType.EXCEPTION,
        sport="mlb",
        host_name="test-host",
        message="failed",
        correlation_id="run-2",
    )

    restarted = ManagementService(SQLiteEventStore(database))
    matches = restarted.search(
        correlation_id="run-2",
        sport="NCAABB",
        event_types=(ManagementEventType.TRAIN_NETWORK_END,),
    )
    assert [event.message for event in matches] == ["finished"]
    assert len(restarted.list_all()) == 3


def test_search_requires_filter_and_positive_limit(tmp_path: Path) -> None:
    service = ManagementService(SQLiteEventStore(tmp_path / "events.sqlite"))
    with pytest.raises(ValidationError, match="requires"):
        service.search()
    with pytest.raises(ValidationError, match="positive"):
        service.search(sport="NCAABB", most_recent_n=0)
