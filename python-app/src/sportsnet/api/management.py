"""Compatibility management API for durable event history."""

from collections import defaultdict
from datetime import datetime
from typing import Annotated, Any
from uuid import UUID

from fastapi import APIRouter, Depends, Query
from pydantic import BaseModel, ConfigDict, Field

from sportsnet.api.common import unknown_query_parameters
from sportsnet.domain.errors import NotFoundError
from sportsnet.domain.events import ManagementEvent, ManagementEventType


class EventRequestDto(BaseModel):
    model_config = ConfigDict(populate_by_name=True, extra="ignore")

    management_event_type: ManagementEventType = Field(alias="managementEventType")
    sport: str = ""
    host_name: str = Field(alias="hostName", min_length=1)
    message: str = ""
    correlation_id: UUID | None = Field(default=None, alias="correlationId")
    elapsed_time_millis: int | None = Field(
        default=None, alias="elapsedTimeMillis", ge=0
    )
    network_descriptor: dict[str, Any] | None = Field(
        default=None, alias="networkDescriptor"
    )
    additional_context: dict[str, Any] = Field(
        default_factory=dict, alias="additionalContext"
    )


def create_management_router(dependencies: Any) -> APIRouter:
    router = APIRouter(prefix="/management", tags=["management"])

    @router.post("/events", status_code=201)
    def create_event(body: EventRequestDto) -> dict[str, object]:
        event = dependencies.management.create(
            management_event_type=body.management_event_type,
            sport=body.sport,
            host_name=body.host_name,
            message=body.message,
            correlation_id=str(body.correlation_id) if body.correlation_id else None,
            elapsed_time_millis=body.elapsed_time_millis,
            network_descriptor=body.network_descriptor,
            additional_context=body.additional_context,
        )
        return _event_wire(event)

    @router.get("/events", dependencies=[Depends(unknown_query_parameters(set()))])
    def list_events() -> list[dict[str, object]]:
        return [_event_wire(event) for event in dependencies.management.list_all()]

    @router.get(
        "/events/{event_id}", dependencies=[Depends(unknown_query_parameters(set()))]
    )
    def get_event(event_id: UUID) -> dict[str, object]:
        event = dependencies.management.get(str(event_id))
        if event is None:
            raise NotFoundError(f"unknown management event: {event_id}")
        return _event_wire(event)

    @router.get(
        "/searchEvents",
        dependencies=[
            Depends(
                unknown_query_parameters(
                    {"correlationId", "sport", "managementEventType", "mostRecentN"}
                )
            )
        ],
    )
    def search_events(
        correlation_id: Annotated[UUID | None, Query(alias="correlationId")] = None,
        sport: str | None = None,
        event_types: Annotated[
            list[ManagementEventType] | None, Query(alias="managementEventType")
        ] = None,
        most_recent_n: Annotated[int | None, Query(alias="mostRecentN", gt=0)] = None,
    ) -> list[dict[str, object]]:
        events = dependencies.management.search(
            correlation_id=str(correlation_id) if correlation_id else None,
            sport=sport,
            event_types=event_types or (),
            most_recent_n=most_recent_n,
        )
        return [_event_wire(event) for event in events]

    @router.get(
        "/stats",
        dependencies=[
            Depends(
                unknown_query_parameters(
                    {"correlationId", "sport", "managementEventType", "mostRecentN"}
                )
            )
        ],
    )
    def event_stats(
        correlation_id: Annotated[UUID | None, Query(alias="correlationId")] = None,
        sport: str | None = None,
        event_types: Annotated[
            list[ManagementEventType] | None, Query(alias="managementEventType")
        ] = None,
        most_recent_n: Annotated[int | None, Query(alias="mostRecentN", gt=0)] = None,
    ) -> list[dict[str, object]]:
        events = dependencies.management.search(
            correlation_id=str(correlation_id) if correlation_id else None,
            sport=sport,
            event_types=event_types or (),
            most_recent_n=most_recent_n,
        )
        return _summarize(events)

    return router


def _event_wire(event: ManagementEvent) -> dict[str, object]:
    local_time = datetime.fromtimestamp(event.system_time_millis / 1000).strftime(
        "%m-%d-%Y %H:%M:%S"
    )
    return {
        "eventId": event.event_id,
        "correlationId": event.correlation_id,
        "managementEventType": event.management_event_type.value,
        "systemTimeMillis": event.system_time_millis,
        "systemTimestamp": local_time,
        "elapsedTimeMillis": event.elapsed_time_millis,
        "networkDescriptor": event.network_descriptor,
        "sport": event.sport,
        "serviceId": event.service_id,
        "hostName": event.host_name,
        "message": event.message,
        "additionalContext": event.additional_context,
    }


def _summarize(events: tuple[ManagementEvent, ...]) -> list[dict[str, object]]:
    grouped: dict[str, list[ManagementEvent]] = defaultdict(list)
    for event in events:
        grouped[event.correlation_id].append(event)
    return [
        _summary(correlation_id, grouped[correlation_id])
        for correlation_id in sorted(grouped)
    ]


def _summary(correlation_id: str, events: list[ManagementEvent]) -> dict[str, object]:
    events.sort(key=lambda event: (event.system_time_millis, event.event_id))
    descriptors = [
        event.network_descriptor
        for event in events
        if event.network_descriptor is not None
    ]
    metric_fields = {
        "networkAccuracy": "networkAccuracy",
        "networkPrecision": "networkPrecision",
        "networkRecall": "networkRecall",
        "networkF1Score": "networkF1Score",
        "realNetworkAccuracy": "realNetworkAccuracy",
    }
    values = {
        field: _metric_values(events, key) for field, key in metric_fields.items()
    }
    started = next(
        (
            event
            for event in events
            if event.management_event_type
            in {
                ManagementEventType.TRAIN_NETWORK_START,
                ManagementEventType.TRAIN_NETWORK_BATCH_START,
                ManagementEventType.RUN_SIMULATION_START,
            }
        ),
        events[0],
    )
    finished = events[-1]
    saved = sum(bool(item.get("networkSaved")) for item in descriptors)
    iteration_events = [
        event
        for event in events
        if event.management_event_type
        == ManagementEventType.TRAIN_NETWORK_BATCH_ITERATION_END
        and event.elapsed_time_millis is not None
    ]
    iterations = [
        event.elapsed_time_millis
        for event in iteration_events
        if event.elapsed_time_millis is not None
    ]
    status = _status(events)
    result: dict[str, object] = {
        "serviceId": events[0].service_id,
        "correlationId": correlation_id,
        "sport": events[0].sport,
        "hostName": events[0].host_name,
        "numberOfEvents": len(events),
        "numberOfSavedNetworks": saved,
        "status": status,
        "whenStarted": _event_wire(started)["systemTimestamp"],
        "whenFinished": _event_wire(finished)["systemTimestamp"],
        "totalBatchIterationTime": sum(iterations),
        "averageBatchIterationTime": sum(iterations) / len(iterations)
        if iterations
        else 0,
    }
    wire_fields = {
        "networkAccuracy": ("NetworkAccuracy", "NetworkAccuracy"),
        "networkPrecision": ("NetworkPrecision", "NetworkPrecision"),
        "networkRecall": ("NetworkRecall", "NetworkRecall"),
        "networkF1Score": ("F1Score", "F1Score"),
        "realNetworkAccuracy": ("RealNetworkAccuracy", "RealAccuracy"),
    }
    for key, (title, average_title) in wire_fields.items():
        metrics = values[key]
        total = sum(value for value, _ in metrics)
        result[f"total{title}"] = total
        result[f"average{average_title}"] = total / len(metrics) if metrics else 0
        highest = (
            max(metrics, key=lambda item: (item[0], -item[1].system_time_millis))
            if metrics
            else None
        )
        lowest = (
            min(metrics, key=lambda item: (item[0], item[1].system_time_millis))
            if metrics
            else None
        )
        result[f"highest{title}Event"] = _event_wire(highest[1]) if highest else None
        result[f"lowest{title}Event"] = _event_wire(lowest[1]) if lowest else None
    ordered_iterations = sorted(
        iteration_events,
        key=lambda event: (
            event.elapsed_time_millis,
            event.system_time_millis,
            event.event_id,
        ),
    )
    result["fastestBatchIterationTimeEvent"] = (
        _event_wire(ordered_iterations[0]) if ordered_iterations else None
    )
    result["slowestBatchIterationTimeEvent"] = (
        _event_wire(ordered_iterations[-1]) if ordered_iterations else None
    )
    return result


def _status(events: list[ManagementEvent]) -> str:
    types = {event.management_event_type for event in events}
    if ManagementEventType.EXCEPTION in types:
        return "FAILED"
    if (
        ManagementEventType.TRAIN_NETWORK_END in types
        or ManagementEventType.TRAIN_NETWORK_BATCH_END in types
    ):
        return "COMPLETED"
    if types.intersection(
        {
            ManagementEventType.TRAIN_NETWORK_START,
            ManagementEventType.TRAIN_NETWORK_BATCH_START,
        }
    ):
        return "IN_PROGRESS"
    return "COMPLETED"


def _metric_values(
    events: list[ManagementEvent], key: str
) -> list[tuple[float, ManagementEvent]]:
    result: list[tuple[float, ManagementEvent]] = []
    for event in events:
        descriptor = event.network_descriptor
        if descriptor is None:
            continue
        value = descriptor.get(key)
        if isinstance(value, (int, float)) and not isinstance(value, bool):
            result.append((float(value), event))
    return result
