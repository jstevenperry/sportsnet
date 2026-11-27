"""Persisted-model evaluation using the artifact's fitted preprocessor."""

import time
import uuid
from collections.abc import Mapping
from typing import Any, Protocol

import numpy as np
from numpy.typing import NDArray

from sportsnet.application.management import ManagementService
from sportsnet.domain.datasets import TrainingDataset
from sportsnet.domain.errors import ValidationError
from sportsnet.domain.events import ManagementEventType
from sportsnet.domain.metrics import calculate_metrics
from sportsnet.domain.models import ModelHandle
from sportsnet.domain.schemas import SportSchema
from sportsnet.sports.registry import SportRegistry


class EvaluationData(Protocol):
    def build(self, sport: str, years: tuple[int, ...]) -> TrainingDataset: ...


class EvaluationModels(Protocol):
    def load(self, model_id: str, sport: str, schema: SportSchema) -> ModelHandle: ...


class EvaluationBackend(Protocol):
    def predict_scores(self, model: object, features: NDArray[Any]) -> NDArray[Any]: ...


class EvaluationService:
    def __init__(
        self,
        *,
        registry: SportRegistry,
        data: EvaluationData,
        models: EvaluationModels,
        backend: EvaluationBackend,
        management: ManagementService,
    ) -> None:
        self.registry = registry
        self.data = data
        self.models = models
        self.backend = backend
        self.management = management

    def evaluate(
        self, sport: str, model_id: str, evaluation_year: int
    ) -> Mapping[str, object]:
        schema = self.registry.get(sport)
        if evaluation_year not in schema.training_years:
            raise ValidationError(f"unsupported evaluation year: {evaluation_year}")
        correlation_id = str(uuid.uuid4())
        started = time.monotonic()
        self.management.create(
            management_event_type=ManagementEventType.GET_NETWORK_BY_ID,
            sport=schema.sport_id,
            correlation_id=correlation_id,
            message="model evaluation started",
            additional_context={
                "networkId": model_id,
                "evaluationYear": evaluation_year,
            },
        )
        try:
            loaded = self.models.load(model_id, schema.sport_id, schema)
            manifest = loaded.manifest
            if str(manifest.get("sport", "")).casefold() != schema.sport_id.casefold():
                raise ValidationError("model sport does not match evaluation route")
            dataset = self.data.build(schema.sport_id, (evaluation_year,))
            features = np.asarray(dataset.features, dtype=np.float64)
            labels = np.asarray(dataset.labels)
            if (
                features.ndim != 2
                or features.shape[0] == 0
                or features.shape[1] != schema.input_width
                or labels.ndim != 1
                or labels.shape[0] != features.shape[0]
                or not np.isfinite(features).all()
                or not np.issubdtype(labels.dtype, np.integer)
                or any(int(label) not in schema.output_classes for label in labels)
            ):
                raise ValidationError("evaluation dataset violates the sport schema")
            prepared = loaded.preprocessor.transform(features.tolist())
            scores = self.backend.predict_scores(loaded.model, prepared)
            metrics = calculate_metrics(
                tuple(int(label) for label in labels),
                scores.tolist(),
                schema.output_classes,
            )
            result = dict(loaded.descriptor)
            result.update(
                {
                    "yearsToEvaluateNetwork": [evaluation_year],
                    "networkAccuracy": metrics.accuracy,
                    "networkPrecision": metrics.precision,
                    "networkRecall": metrics.recall,
                    "networkF1Score": metrics.f1,
                    "realNetworkAccuracy": metrics.composite,
                    "networkSaved": True,
                }
            )
        except Exception:
            self.management.create(
                management_event_type=ManagementEventType.EXCEPTION,
                sport=schema.sport_id,
                correlation_id=correlation_id,
                message="model evaluation failed",
                additional_context={
                    "networkId": model_id,
                    "evaluationYear": evaluation_year,
                },
            )
            raise
        self.management.create(
            management_event_type=ManagementEventType.GET_NETWORK_BY_ID,
            sport=schema.sport_id,
            correlation_id=correlation_id,
            message="model evaluation completed",
            elapsed_time_millis=max(0, int((time.monotonic() - started) * 1000)),
            network_descriptor=result,
        )
        return result
