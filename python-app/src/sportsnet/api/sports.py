"""Transport adapters shared by sport-specific route families."""

from collections.abc import Mapping
from uuid import UUID

from fastapi import APIRouter, Depends
from fastapi.responses import PlainTextResponse
from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator

from sportsnet.api.common import unknown_query_parameters
from sportsnet.application.dependencies import ApplicationDependencies
from sportsnet.application.training import TrainingRequest
from sportsnet.config import AppSettings
from sportsnet.domain.errors import NotFoundError, ValidationError
from sportsnet.domain.schemas import SportSchema


class TrainingRequestDto(BaseModel):
    model_config = ConfigDict(populate_by_name=True, extra="ignore")

    years_to_train: list[int] = Field(alias="yearsToTrainNetwork", min_length=1)
    years_to_evaluate: list[int] = Field(alias="yearsToEvaluateNetwork", min_length=1)
    network_structure_algorithm: str = Field(alias="networkStructureAlgorithm")
    network_layout: str | None = Field(default=None, alias="networkLayout")
    number_of_hidden_layers: int | None = Field(
        default=None, alias="numberOfHiddenLayers"
    )
    max_epochs: int = Field(default=1000, alias="maxNumberOfTrainingEpochs", gt=0)
    evaluation_interval: int = Field(default=10, alias="networkEvaluationIndex", gt=0)
    reporting_interval: int | None = Field(
        default=None, alias="reportingInterval", gt=0
    )
    max_network_error: float = Field(default=0.01, alias="maxNetworkError", ge=0, le=1)
    network_threshold: float | None = Field(default=None, alias="networkThreshold")
    network_performance_threshold: float | None = Field(
        default=None, alias="networkPerformanceThreshold"
    )
    normalizer: str = "NORMALIZER_STANDARDIZE"
    activation: str = Field(default="RELU", alias="activationFunction")
    output_activation: str = Field(default="SOFTMAX", alias="outputLayerActivation")
    loss: str | None = Field(default=None, alias="outputLayerLossFunction")
    general_loss: str | None = Field(default=None, alias="lossFunction")
    optimizer: str = Field(default="ADAM", alias="updater")
    initializer: str = Field(default="XAVIER_UNIFORM", alias="weightInit")
    learning_rate: float = Field(default=0.001, alias="learningRuleLearningRate", gt=0)
    l2: float = Field(default=0.0, alias="l2RegularizationLambda", ge=0)
    seed: int = 0
    shuffle: bool = False
    number_of_inputs: int | None = Field(default=None, alias="numberOfInputs")
    number_of_outputs: int | None = Field(default=None, alias="numberOfOutputs")
    network_type: str | None = Field(default=None, alias="networkType")

    @model_validator(mode="after")
    def validate_layout_parameters(self) -> "TrainingRequestDto":
        algorithm = self.network_structure_algorithm
        if algorithm in {"USER_DEFINED", "USER_DEFINED_RAND"}:
            if not self.network_layout:
                raise ValueError(f"networkLayout is required for {algorithm}")
        elif algorithm in {"HOUR_GLASS", "TAPERED_TREE"} and (
            self.number_of_hidden_layers is None or self.number_of_hidden_layers < 3
        ):
            raise ValueError(
                f"numberOfHiddenLayers of at least 3 is required for {algorithm}"
            )
        return self

    @field_validator(
        "years_to_train",
        "years_to_evaluate",
        mode="before",
        json_schema_input_type=list[int] | str,
    )
    @classmethod
    def parse_year_values(cls, value: object) -> object:
        if not isinstance(value, str):
            return value
        parts = value.split(",")
        if any(not part.strip() for part in parts):
            raise ValueError("CSV year lists cannot contain empty entries")
        try:
            return [int(part.strip()) for part in parts]
        except ValueError as error:
            raise ValueError("CSV year lists must contain integers") from error


class BatchRequestDto(TrainingRequestDto):
    number_of_batch_iterations: int = Field(alias="numberOfBatchIterations", gt=0)


class SimulationRequestDto(BaseModel):
    model_config = ConfigDict(populate_by_name=True, extra="ignore")

    network_ids: list[UUID] = Field(alias="networkIds", min_length=1)
    tournament_year: int = Field(alias="tournamentYear")


def create_sport_router(
    sport_id: str,
    dependencies: ApplicationDependencies,
    settings: AppSettings,
) -> APIRouter:
    router = APIRouter(prefix=f"/{sport_id.casefold()}", tags=[sport_id])

    @router.get("/networks", dependencies=[Depends(unknown_query_parameters(set()))])
    def list_networks() -> tuple[Mapping[str, object], ...]:
        return dependencies.models.list_descriptors(sport_id)

    @router.get(
        "/networks/{network_id}",
        dependencies=[Depends(unknown_query_parameters(set()))],
    )
    def get_network(network_id: UUID) -> Mapping[str, object] | None:
        return dependencies.models.get_descriptor(str(network_id), sport_id)

    @router.post("/networks", dependencies=[Depends(unknown_query_parameters(set()))])
    def train_network(body: TrainingRequestDto) -> Mapping[str, object]:
        schema = dependencies.registry.get(sport_id)
        request = _to_training_request(body, sport_id, schema, settings)
        outcome = dependencies.training.train(request)
        if outcome.status == "persisted" and outcome.descriptor is not None:
            return outcome.descriptor
        return outcome.report

    @router.post(
        "/networks/batch",
        status_code=202,
        dependencies=[Depends(unknown_query_parameters(set()))],
    )
    def submit_batch(body: BatchRequestDto) -> Mapping[str, object]:
        if dependencies.batch is None:
            raise NotFoundError("batch service is not configured")
        schema = dependencies.registry.get(sport_id)
        request = _to_training_request(body, sport_id, schema, settings)
        return dependencies.batch.submit(
            request, number_of_iterations=body.number_of_batch_iterations
        )

    @router.get(
        "/tournamentYears", dependencies=[Depends(unknown_query_parameters(set()))]
    )
    def tournament_years() -> tuple[Mapping[str, object], ...]:
        if dependencies.tournaments is None:
            return ()
        return dependencies.tournaments.years(sport_id)

    @router.get(
        "/trainingYears", dependencies=[Depends(unknown_query_parameters(set()))]
    )
    def training_years() -> tuple[int, ...]:
        if dependencies.available_training_years is not None:
            return dependencies.available_training_years(sport_id)
        return dependencies.registry.get(sport_id).training_years

    @router.post(
        "/simulations",
        response_class=PlainTextResponse,
        dependencies=[Depends(unknown_query_parameters(set()))],
    )
    def simulate(body: SimulationRequestDto) -> str:
        if dependencies.tournaments is None:
            raise NotFoundError("tournament service is not configured")
        return dependencies.tournaments.run(
            sport_id,
            {
                "networkIds": [str(item) for item in body.network_ids],
                "tournamentYear": body.tournament_year,
            },
        )

    if sport_id.casefold() == "ncaabb":

        @router.get(
            "/evaluate/networks/{network_id}/{evaluation_year}",
            dependencies=[Depends(unknown_query_parameters(set()))],
        )
        def evaluate_network(
            network_id: UUID, evaluation_year: int
        ) -> Mapping[str, object]:
            if dependencies.evaluation is None:
                raise NotFoundError("evaluation service is not configured")
            return dependencies.evaluation.evaluate(
                sport_id, str(network_id), evaluation_year
            )

    return router


def _to_training_request(
    body: TrainingRequestDto,
    sport_id: str,
    schema: SportSchema,
    settings: AppSettings,
) -> TrainingRequest:
    if body.network_type and body.network_type.casefold() != sport_id.casefold():
        raise ValidationError("networkType does not match route sport")
    if body.number_of_inputs not in (None, schema.input_width):
        raise ValidationError("numberOfInputs does not match sport schema")
    if body.number_of_outputs not in (None, schema.output_width):
        raise ValidationError("numberOfOutputs does not match sport schema")
    threshold = (
        body.network_performance_threshold
        if body.network_performance_threshold is not None
        else body.network_threshold
        if body.network_threshold is not None
        else settings.performance_threshold
    )
    user_defined = body.network_structure_algorithm in {
        "USER_DEFINED",
        "USER_DEFINED_RAND",
    }
    return TrainingRequest(
        sport_id=sport_id,
        training_years=tuple(body.years_to_train),
        evaluation_years=tuple(body.years_to_evaluate),
        hidden_layer_count=None if user_defined else body.number_of_hidden_layers,
        max_epochs=body.max_epochs,
        evaluation_interval=body.evaluation_interval,
        reporting_interval=body.reporting_interval,
        max_network_error=body.max_network_error,
        performance_threshold=threshold,
        normalizer=body.normalizer,
        seed=body.seed,
        algorithm=body.network_structure_algorithm,
        user_layout=body.network_layout if user_defined else None,
        hidden_activation=body.activation,
        output_activation=body.output_activation,
        loss=body.loss or body.general_loss or "SPARSE_MCXENT",
        optimizer=body.optimizer,
        initializer=body.initializer,
        learning_rate=body.learning_rate,
        l2=body.l2,
        shuffle=body.shuffle,
    )
