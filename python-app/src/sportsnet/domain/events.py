"""Validated management-event domain types."""

from dataclasses import dataclass, field
from enum import StrEnum


class ManagementEventType(StrEnum):
    TRAIN_NETWORK_START = "TRAIN_NETWORK_START"
    TRAIN_NETWORK_END = "TRAIN_NETWORK_END"
    TRAIN_NETWORK_BATCH_START = "TRAIN_NETWORK_BATCH_START"
    TRAIN_NETWORK_BATCH_ITERATION_END = "TRAIN_NETWORK_BATCH_ITERATION_END"
    TRAIN_NETWORK_BATCH_END = "TRAIN_NETWORK_BATCH_END"
    RUN_SIMULATION_START = "RUN_SIMULATION_START"
    RUN_SIMULATION_END = "RUN_SIMULATION_END"
    REINDEX_START = "REINDEX_START"
    REINDEX_END = "REINDEX_END"
    GET_ALL_NETWORKS = "GET_ALL_NETWORKS"
    GET_NETWORK_BY_ID = "GET_NETWORK_BY_ID"
    SAVE_NETWORK = "SAVE_NETWORK"
    FETCH_RESOURCE_FILE = "FETCH_RESOURCE_FILE"
    EXCEPTION = "EXCEPTION"
    PING = "PING"


@dataclass(frozen=True, slots=True)
class ManagementEvent:
    event_id: str
    correlation_id: str
    management_event_type: ManagementEventType
    system_time_millis: int
    service_id: str
    sport: str
    host_name: str
    message: str
    elapsed_time_millis: int | None = None
    network_descriptor: dict[str, object] | None = None
    additional_context: dict[str, object] = field(default_factory=dict)
