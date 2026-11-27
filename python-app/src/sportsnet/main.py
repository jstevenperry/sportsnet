"""FastAPI application factory and Uvicorn entry point."""

import asyncio
import logging
import tempfile
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager
from pathlib import Path

import uvicorn
from fastapi import FastAPI
from fastapi.responses import JSONResponse
from starlette.types import ASGIApp, Message, Receive, Scope, Send

from sportsnet.adapters.filesystem_resources import FilesystemDatasetProvider
from sportsnet.adapters.keras_backend import KerasBackend
from sportsnet.adapters.model_repository import FilesystemModelRepository
from sportsnet.adapters.sqlite_store import SQLiteEventStore
from sportsnet.api.common import install_error_handlers
from sportsnet.api.management import create_management_router
from sportsnet.api.mlb import create_sport_router as create_mlb_router
from sportsnet.api.ncaabb import create_sport_router as create_ncaabb_router
from sportsnet.application.batch import BatchService
from sportsnet.application.dependencies import ApplicationDependencies
from sportsnet.application.evaluation import EvaluationService
from sportsnet.application.management import ManagementService
from sportsnet.application.tournament import TournamentPlugin, TournamentService
from sportsnet.application.training import TrainingService
from sportsnet.config import AppSettings
from sportsnet.domain.errors import RepositoryError
from sportsnet.sports.mlb.module import MLB
from sportsnet.sports.ncaabb.module import NCAABB
from sportsnet.sports.registry import SportRegistry

logger = logging.getLogger(__name__)


class RequestSizeLimitMiddleware:
    def __init__(self, app: ASGIApp, *, max_request_body_bytes: int) -> None:
        self.app = app
        self.max_request_body_bytes = max_request_body_bytes

    async def __call__(self, scope: Scope, receive: Receive, send: Send) -> None:
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return

        body = bytearray()
        while True:
            message = await receive()
            if message["type"] == "http.disconnect":
                return
            if message["type"] != "http.request":
                continue
            body.extend(message.get("body", b""))
            if len(body) > self.max_request_body_bytes:
                correlation_id = next(
                    (
                        value.decode("latin-1")
                        for key, value in scope["headers"]
                        if key.lower() == b"x-correlation-id"
                    ),
                    None,
                )
                response = JSONResponse(
                    status_code=413,
                    content={
                        "code": "request_too_large",
                        "message": "request body exceeds configured limit",
                        "correlationId": correlation_id,
                    },
                )
                await response(scope, receive, send)
                return
            if not message.get("more_body", False):
                break

        replayed = False

        async def replay_receive() -> Message:
            nonlocal replayed
            if not replayed:
                replayed = True
                return {"type": "http.request", "body": bytes(body), "more_body": False}
            return await receive()

        await self.app(scope, replay_receive, send)


def create_app(settings: AppSettings, dependencies: ApplicationDependencies) -> FastAPI:
    _configure_training_logging(settings.training_log_level)
    _configure_access_logging(settings.access_log_level)
    settings.model_root.mkdir(parents=True, exist_ok=True)

    @asynccontextmanager
    async def lifespan(_: FastAPI) -> AsyncIterator[None]:
        _verify_writable_directory(settings.model_root)
        store = dependencies.management.store
        is_writable = getattr(store, "is_writable", None)
        if is_writable is not None and not is_writable():
            raise RepositoryError("event storage is not writable")
        try:
            yield
        finally:
            if dependencies.batch is not None:
                stopped = await asyncio.to_thread(
                    dependencies.batch.shutdown,
                    settings.shutdown_deadline_seconds,
                )
                if not stopped:
                    logger.error(
                        "batch workers did not stop within %.3f seconds",
                        settings.shutdown_deadline_seconds,
                    )

    app = FastAPI(title="Sportsnet Python", version="0.1.0", lifespan=lifespan)
    app.add_middleware(
        RequestSizeLimitMiddleware,
        max_request_body_bytes=settings.max_request_body_bytes,
    )
    install_error_handlers(app)
    app.include_router(create_ncaabb_router("NCAABB", dependencies, settings))
    app.include_router(create_mlb_router("mlb", dependencies, settings))
    app.include_router(create_management_router(dependencies))

    @app.get("/health/live")
    def liveness() -> dict[str, str]:
        return {"status": "UP"}

    @app.get("/health/ready")
    def readiness() -> JSONResponse:
        try:
            _verify_writable_directory(settings.model_root)
            store = dependencies.management.store
            is_writable = getattr(store, "is_writable", None)
            if is_writable is not None and not is_writable():
                raise OSError("event store is unavailable")
        except OSError as error:
            return JSONResponse(
                status_code=503,
                content={"status": "DOWN", "message": str(error)},
            )
        return JSONResponse(status_code=200, content={"status": "UP"})

    return app


def create_application() -> FastAPI:
    settings = AppSettings()
    registry = SportRegistry((NCAABB, MLB))
    backend = KerasBackend()
    models = FilesystemModelRepository(settings.model_root, backend)
    event_store = SQLiteEventStore(settings.event_database)
    management = ManagementService(event_store, service_id=settings.service_id)
    provider = FilesystemDatasetProvider(settings.resource_root, registry)
    training = TrainingService(
        registry=registry,
        data=provider,
        backend=backend,
        models=models,
        management=management,
    )
    batch = BatchService(
        training=training,
        management=management,
        max_workers=settings.batch_workers,
        queue_capacity=settings.batch_queue_capacity,
        max_iterations=settings.max_batch_iterations,
    )
    tournaments = TournamentService(
        registry=registry,
        data=provider,
        models=models,
        backend=backend,
        management=management,
        plugins=(
            TournamentPlugin("NCAABB", 2024, "2024-25 (Ranking)", "TEAM_RANKING"),
            TournamentPlugin("mlb", 2009, "2009 (TEAM_RANKING)", "TEAM_RANKING"),
            TournamentPlugin("mlb", 2023, "2023 (TEAM_RANKING)", "TEAM_RANKING"),
        ),
        distance_factor=settings.tournament_distance_factor,
    )
    evaluation = EvaluationService(
        registry=registry,
        data=provider,
        models=models,
        backend=backend,
        management=management,
    )
    dependencies = ApplicationDependencies(
        registry=registry,
        management=management,
        training=training,
        models=models,
        batch=batch,
        tournaments=tournaments,
        evaluation=evaluation,
        available_training_years=provider.available_training_years,
    )
    return create_app(settings, dependencies)


def _configure_training_logging(level: str) -> None:
    logging.basicConfig(level=logging.INFO)
    logging.getLogger("sportsnet.application.training").setLevel(level)
    logging.getLogger("sportsnet.application.batch").setLevel(logging.INFO)


class _AccessLogFilter(logging.Filter):
    def __init__(self, level: str) -> None:
        super().__init__()
        self.level = logging.DEBUG if level == "DEBUG" else logging.INFO

    def filter(self, record: logging.LogRecord) -> bool:
        if record.levelno == logging.INFO:
            record.levelno = logging.DEBUG
            record.levelname = "DEBUG"
        return record.levelno >= self.level


def _configure_access_logging(level: str) -> None:
    access_logger = logging.getLogger("uvicorn.access")
    # Uvicorn checks isEnabledFor(INFO) before creating its access records.
    access_logger.setLevel(logging.INFO)
    for existing in access_logger.filters[:]:
        if isinstance(existing, _AccessLogFilter):
            access_logger.removeFilter(existing)
    access_logger.addFilter(_AccessLogFilter(level))


def run() -> None:
    uvicorn.run(
        "sportsnet.main:create_application",
        host=AppSettings().bind_host,
        port=AppSettings().port,
        factory=True,
        workers=1,
    )


def _verify_writable_directory(path: Path) -> None:
    path.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile(dir=path, prefix=".sportsnet-ready-", delete=True):
        pass
