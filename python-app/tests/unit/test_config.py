import logging

import pytest
from pydantic import ValidationError

from sportsnet.config import AppSettings
from sportsnet.main import _configure_access_logging, _configure_training_logging


def test_training_log_level_defaults_to_info() -> None:
    settings = AppSettings(_env_file=None)

    assert settings.training_log_level == "INFO"


def test_training_log_level_can_be_set_from_environment(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    monkeypatch.setenv("SPORTSNET_TRAINING_LOG_LEVEL", "debug")

    assert AppSettings(_env_file=None).training_log_level == "DEBUG"


def test_training_log_level_rejects_unsupported_values() -> None:
    with pytest.raises(ValidationError):
        AppSettings(_env_file=None, training_log_level="WARNING")


def test_access_log_level_settings(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.delenv("SPORTSNET_ACCESS_LOG_LEVEL", raising=False)
    assert AppSettings(_env_file=None).access_log_level == "INFO"
    monkeypatch.setenv("SPORTSNET_ACCESS_LOG_LEVEL", "debug")
    assert AppSettings(_env_file=None).access_log_level == "DEBUG"
    monkeypatch.setenv("SPORTSNET_ACCESS_LOG_LEVEL", "invalid")
    with pytest.raises(ValidationError):
        AppSettings(_env_file=None)


def test_access_logging_filters_only_routine_requests(
    caplog: pytest.LogCaptureFixture,
) -> None:
    access_logger = logging.getLogger("uvicorn.access")
    original_level = access_logger.level
    original_filters = access_logger.filters[:]
    original_propagate = access_logger.propagate
    try:
        access_logger.propagate = True
        _configure_access_logging("INFO")
        access_logger.info("routine request")
        access_logger.warning("access warning")
        assert [record.getMessage() for record in caplog.records] == ["access warning"]

        caplog.clear()
        _configure_access_logging("DEBUG")
        _configure_access_logging("DEBUG")
        access_logger.info("routine request")
        assert [
            (record.levelname, record.getMessage()) for record in caplog.records
        ] == [("DEBUG", "routine request")]
    finally:
        access_logger.filters[:] = original_filters
        access_logger.setLevel(original_level)
        access_logger.propagate = original_propagate


def test_training_debug_and_batch_info_output_without_existing_logging_handlers(
    capsys: pytest.CaptureFixture[str],
) -> None:
    root_logger = logging.getLogger()
    training_logger = logging.getLogger("sportsnet.application.training")
    batch_logger = logging.getLogger("sportsnet.application.batch")
    previous_handlers = root_logger.handlers[:]
    previous_root_level = root_logger.level
    previous_training_level = training_logger.level
    previous_batch_level = batch_logger.level
    try:
        root_logger.handlers.clear()
        _configure_training_logging("INFO")
        training_logger.debug("Network training completed with metrics")
        batch_logger.info("Batch iteration started iteration=1/2 remaining_after=1")
        output = capsys.readouterr().err
        assert "Network training completed with metrics" not in output
        assert "Batch iteration started iteration=1/2 remaining_after=1" in output

        _configure_training_logging("DEBUG")
        training_logger.debug("Network training completed with metrics")
        output = capsys.readouterr().err
        assert "DEBUG:sportsnet.application.training:" in output
        assert "Network training completed with metrics" in output
    finally:
        root_logger.handlers[:] = previous_handlers
        root_logger.setLevel(previous_root_level)
        training_logger.setLevel(previous_training_level)
        batch_logger.setLevel(previous_batch_level)
