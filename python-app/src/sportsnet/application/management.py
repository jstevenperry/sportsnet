"""Management event application service."""

import socket
import time
import uuid
from collections.abc import Callable, Sequence

from sportsnet.adapters.sqlite_store import EventStore
from sportsnet.domain.errors import ValidationError
from sportsnet.domain.events import ManagementEvent, ManagementEventType


class ManagementService:
    def __init__(
        self,
        store: EventStore,
        *,
        id_factory: Callable[[], str] | None = None,
        clock: Callable[[], int] | None = None,
        service_id: str = "sportsnet-python",
        host_name: str | None = None,
    ) -> None:
        self.store = store
        self.id_factory = id_factory or (lambda: str(uuid.uuid4()))
        self.clock = clock or (lambda: int(time.time() * 1000))
        self.service_id = service_id
        self.host_name = host_name or socket.gethostname()

    def create(
        self,
        *,
        management_event_type: ManagementEventType,
        sport: str = "",
        host_name: str | None = None,
        message: str = "",
        correlation_id: str | None = None,
        elapsed_time_millis: int | None = None,
        network_descriptor: dict[str, object] | None = None,
        additional_context: dict[str, object] | None = None,
    ) -> ManagementEvent:
        if not isinstance(management_event_type, ManagementEventType):
            raise ValidationError("invalid management event type")
        if elapsed_time_millis is not None and elapsed_time_millis < 0:
            raise ValidationError("elapsed time must be non-negative")
        event_id = self.id_factory()
        event = ManagementEvent(
            event_id=event_id,
            correlation_id=correlation_id or event_id,
            management_event_type=management_event_type,
            system_time_millis=self.clock(),
            service_id=self.service_id,
            sport=sport,
            host_name=(host_name or self.host_name).strip(),
            message=message,
            elapsed_time_millis=elapsed_time_millis,
            network_descriptor=network_descriptor,
            additional_context=additional_context or {},
        )
        if not event.host_name:
            raise ValidationError("host name is required")
        self.store.insert(event)
        return event

    def get(self, event_id: str) -> ManagementEvent | None:
        return self.store.get(event_id)

    def list_all(self) -> tuple[ManagementEvent, ...]:
        return self.store.all()

    def search(
        self,
        *,
        correlation_id: str | None = None,
        sport: str | None = None,
        event_types: Sequence[ManagementEventType] = (),
        most_recent_n: int | None = None,
    ) -> tuple[ManagementEvent, ...]:
        if correlation_id is None and sport is None and not event_types:
            raise ValidationError("event search requires at least one filter")
        if most_recent_n is not None and most_recent_n <= 0:
            raise ValidationError("mostRecentN must be positive")
        if sport is not None and not sport.strip():
            raise ValidationError("sport filter must not be blank")
        return self.store.search(
            correlation_id=correlation_id,
            sport=sport,
            event_types=frozenset(event_types),
            most_recent_n=most_recent_n,
        )
