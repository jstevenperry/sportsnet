"""Typed errors shared across domain, repository, and API boundaries."""


class SportsnetError(Exception):
    """Base class for expected application errors."""

    code = "sportsnet_error"


class ValidationError(SportsnetError):
    """Invalid caller input or incompatible domain data."""

    code = "validation_error"


class NotFoundError(SportsnetError):
    """A requested persistent resource does not exist."""

    code = "not_found"


class RepositoryError(SportsnetError):
    """A repository operation failed."""

    code = "repository_error"


class BackendError(SportsnetError):
    """A model backend operation failed."""

    code = "backend_error"


class TrainingCancelled(SportsnetError):
    """A training request was cancelled at a safe epoch boundary."""

    code = "training_cancelled"
