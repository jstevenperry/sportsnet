from sportsnet.api.management import _summary
from sportsnet.domain.events import ManagementEvent, ManagementEventType


def _event(event_id: str, elapsed: int) -> ManagementEvent:
    return ManagementEvent(
        event_id=event_id,
        correlation_id="run-1",
        management_event_type=ManagementEventType.TRAIN_NETWORK_BATCH_ITERATION_END,
        system_time_millis=1000 + elapsed,
        service_id="test",
        sport="synthetic",
        host_name="test-host",
        message="completed",
        elapsed_time_millis=elapsed,
        additional_context={"iteration": elapsed},
    )


def test_batch_summary_reports_fastest_and_slowest_iteration_events() -> None:
    result = _summary("run-1", [_event("slow", 20), _event("fast", 5)])

    assert result["totalBatchIterationTime"] == 25
    assert result["averageBatchIterationTime"] == 12.5
    assert result["fastestBatchIterationTimeEvent"]["eventId"] == "fast"
    assert result["slowestBatchIterationTimeEvent"]["eventId"] == "slow"
