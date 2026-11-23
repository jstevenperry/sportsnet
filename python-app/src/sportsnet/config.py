"""Validated deployment settings."""

from pathlib import Path
from typing import Literal

from pydantic import Field, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class AppSettings(BaseSettings):
    model_config = SettingsConfigDict(
        env_prefix="SPORTSNET_", env_file=".env", extra="ignore"
    )

    service_id: str = "sportsnet-python"
    bind_host: str = "127.0.0.1"
    port: int = Field(default=8287, ge=1, le=65535)
    resource_root: Path = Path("resources")
    event_database: Path = Path("var/events.sqlite")
    performance_threshold: float = Field(default=70.0, ge=0, le=100)
    max_request_body_bytes: int = Field(default=1_048_576, gt=0)
    shutdown_deadline_seconds: float = Field(default=30.0, gt=0)
    batch_workers: int = Field(default=1, ge=1)
    batch_queue_capacity: int = Field(default=10, ge=0)
    max_batch_iterations: int = Field(default=100, ge=1)
    training_log_level: Literal["DEBUG", "INFO"] = "INFO"
    access_log_level: Literal["DEBUG", "INFO"] = "INFO"
    tournament_distance_factor: float = 0.0

    @field_validator("training_log_level", "access_log_level", mode="before")
    @classmethod
    def normalize_log_level(cls, value: object) -> object:
        return value.upper() if isinstance(value, str) else value

    @property
    def model_root(self) -> Path:
        return self.resource_root / "Networks"
