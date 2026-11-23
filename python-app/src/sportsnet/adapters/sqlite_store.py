"""Transactional, connection-per-operation SQLite event store."""

import json
import sqlite3
from collections.abc import Iterator
from contextlib import contextmanager
from pathlib import Path
from typing import Protocol

from sportsnet.domain.events import ManagementEvent, ManagementEventType


class EventStore(Protocol):
    def insert(self, event: ManagementEvent) -> None: ...

    def get(self, event_id: str) -> ManagementEvent | None: ...

    def all(self) -> tuple[ManagementEvent, ...]: ...

    def search(
        self,
        *,
        correlation_id: str | None,
        sport: str | None,
        event_types: frozenset[ManagementEventType],
        most_recent_n: int | None,
    ) -> tuple[ManagementEvent, ...]: ...

    def is_writable(self) -> bool: ...


class SQLiteEventStore:
    def __init__(self, path: Path, *, busy_timeout_ms: int = 5_000) -> None:
        self.path = path
        self.busy_timeout_ms = busy_timeout_ms
        path.parent.mkdir(parents=True, exist_ok=True)
        with self._connection() as connection:
            connection.execute(
                """
                CREATE TABLE IF NOT EXISTS management_events (
                    event_id TEXT PRIMARY KEY,
                    correlation_id TEXT NOT NULL,
                    sport TEXT NOT NULL,
                    event_type TEXT NOT NULL,
                    system_time_millis INTEGER NOT NULL,
                    event_json TEXT NOT NULL
                )
                """
            )
            connection.execute(
                "CREATE INDEX IF NOT EXISTS idx_events_correlation "
                "ON management_events(correlation_id)"
            )
            connection.execute(
                "CREATE INDEX IF NOT EXISTS idx_events_sport_type_time "
                "ON management_events(sport, event_type, system_time_millis)"
            )

    def insert(self, event: ManagementEvent) -> None:
        payload = json.dumps(
            _event_to_dict(event),
            allow_nan=False,
            sort_keys=True,
            separators=(",", ":"),
        )
        with self._connection() as connection:
            connection.execute(
                """
                INSERT INTO management_events
                (event_id, correlation_id, sport, event_type,
                 system_time_millis, event_json)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                (
                    event.event_id,
                    event.correlation_id,
                    event.sport,
                    event.management_event_type.value,
                    event.system_time_millis,
                    payload,
                ),
            )

    def get(self, event_id: str) -> ManagementEvent | None:
        with self._connection() as connection:
            row = connection.execute(
                "SELECT event_json FROM management_events WHERE event_id = ?",
                (event_id,),
            ).fetchone()
        return _event_from_json(row[0]) if row else None

    def all(self) -> tuple[ManagementEvent, ...]:
        with self._connection() as connection:
            rows = connection.execute(
                "SELECT event_json FROM management_events "
                "ORDER BY system_time_millis ASC, event_id ASC"
            ).fetchall()
        return tuple(_event_from_json(row[0]) for row in rows)

    def search(
        self,
        *,
        correlation_id: str | None,
        sport: str | None,
        event_types: frozenset[ManagementEventType],
        most_recent_n: int | None,
    ) -> tuple[ManagementEvent, ...]:
        clauses: list[str] = []
        parameters: list[object] = []
        if correlation_id is not None:
            clauses.append("correlation_id = ?")
            parameters.append(correlation_id)
        if sport is not None:
            clauses.append("lower(sport) = lower(?)")
            parameters.append(sport)
        if event_types:
            placeholders = ",".join("?" for _ in event_types)
            clauses.append(f"event_type IN ({placeholders})")
            parameters.extend(sorted(event_type.value for event_type in event_types))
        where = " WHERE " + " AND ".join(clauses)
        query = "SELECT event_json FROM management_events" + where
        if most_recent_n is None:
            query += " ORDER BY system_time_millis ASC, event_id ASC"
        else:
            query += " ORDER BY system_time_millis DESC, event_id DESC LIMIT ?"
            parameters.append(most_recent_n)
        with self._connect() as connection:
            rows = connection.execute(query, parameters).fetchall()
        events = tuple(_event_from_json(row[0]) for row in rows)
        return tuple(reversed(events)) if most_recent_n is not None else events

    def is_writable(self) -> bool:
        with self._connection() as connection:
            connection.execute("SELECT 1").fetchone()
        return True

    @contextmanager
    def _connection(self) -> Iterator[sqlite3.Connection]:
        connection = self._connect()
        try:
            with connection:
                yield connection
        finally:
            connection.close()

    def _connect(self) -> sqlite3.Connection:
        connection = sqlite3.connect(
            self.path, timeout=self.busy_timeout_ms / 1000, isolation_level="IMMEDIATE"
        )
        connection.row_factory = sqlite3.Row
        connection.execute("PRAGMA journal_mode=WAL")
        connection.execute("PRAGMA foreign_keys=ON")
        connection.execute(f"PRAGMA busy_timeout={self.busy_timeout_ms}")
        return connection


def _event_to_dict(event: ManagementEvent) -> dict[str, object]:
    return {
        "eventId": event.event_id,
        "correlationId": event.correlation_id,
        "managementEventType": event.management_event_type.value,
        "systemTimeMillis": event.system_time_millis,
        "elapsedTimeMillis": event.elapsed_time_millis,
        "networkDescriptor": event.network_descriptor,
        "sport": event.sport,
        "serviceId": event.service_id,
        "hostName": event.host_name,
        "message": event.message,
        "additionalContext": event.additional_context,
    }


def _event_from_json(value: str) -> ManagementEvent:
    payload = json.loads(value)
    return ManagementEvent(
        event_id=payload["eventId"],
        correlation_id=payload["correlationId"],
        management_event_type=ManagementEventType(payload["managementEventType"]),
        system_time_millis=payload["systemTimeMillis"],
        service_id=payload["serviceId"],
        sport=payload["sport"],
        host_name=payload["hostName"],
        message=payload["message"],
        elapsed_time_millis=payload["elapsedTimeMillis"],
        network_descriptor=payload["networkDescriptor"],
        additional_context=payload["additionalContext"],
    )
