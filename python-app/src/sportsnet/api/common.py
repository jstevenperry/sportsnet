"""Shared FastAPI validation and error responses."""

import logging
from collections.abc import Callable

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from sportsnet.domain.errors import (
    NotFoundError,
    RepositoryError,
    SportsnetError,
    ValidationError,
)

logger = logging.getLogger(__name__)


def _field_name(location: tuple[object, ...]) -> str:
    parts = [str(part) for part in location if part not in {"body", "query", "path"}]
    return ".".join(parts) or "request"


def _validation_details(error: RequestValidationError) -> list[dict[str, str]]:
    details = []
    for item in error.errors():
        message = str(item["msg"]).removeprefix("Value error, ")
        details.append({"field": _field_name(tuple(item["loc"])), "message": message})
    return details


def _log_bad_request(request: Request, reason: str) -> None:
    logger.warning(
        "Bad request method=%s path=%s correlation_id=%s reason=%s",
        request.method,
        request.url.path,
        request.headers.get("x-correlation-id"),
        reason,
    )


def unknown_query_parameters(allowed: set[str]) -> Callable[[Request], None]:
    def validate(request: Request) -> None:
        unknown = set(request.query_params).difference(allowed)
        if unknown:
            raise ValidationError(
                f"unknown query parameter(s): {', '.join(sorted(unknown))}"
            )

    return validate


def install_error_handlers(app: FastAPI) -> None:
    @app.exception_handler(SportsnetError)
    async def sportsnet_error_handler(
        request: Request, error: SportsnetError
    ) -> JSONResponse:
        if isinstance(error, ValidationError):
            status = 400
            _log_bad_request(request, str(error))
        elif isinstance(error, NotFoundError):
            status = 404
        elif isinstance(error, RepositoryError):
            status = 500
        else:
            status = 500
        return JSONResponse(
            status_code=status,
            content={
                "code": error.code,
                "message": str(error),
                "correlationId": request.headers.get("x-correlation-id"),
            },
        )

    @app.exception_handler(RequestValidationError)
    async def request_validation_handler(
        request: Request, error: RequestValidationError
    ) -> JSONResponse:
        details = _validation_details(error)
        summary = "; ".join(f"{d['field']}: {d['message']}" for d in details)
        _log_bad_request(request, summary)
        return JSONResponse(
            status_code=400,
            content={
                "code": "invalid_request",
                "message": f"request validation failed: {summary}",
                "details": details,
                "correlationId": request.headers.get("x-correlation-id"),
            },
        )

    @app.exception_handler(Exception)
    async def unexpected_error_handler(
        request: Request, error: Exception
    ) -> JSONResponse:
        return JSONResponse(
            status_code=500,
            content={
                "code": "internal_error",
                "message": "internal server error",
                "correlationId": request.headers.get("x-correlation-id"),
            },
        )
