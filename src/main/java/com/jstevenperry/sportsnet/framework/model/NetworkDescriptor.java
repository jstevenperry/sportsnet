/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.model;

import com.jstevenperry.sportsnet.framework.config.NetworkStructureAlgorithm;
import com.jstevenperry.sportsnet.framework.dto.NetworkDescriptorDto;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NetworkDescriptor implements Serializable {

    public static final long serialVersionUID = 1L;

    public static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";

    private Integer numberOfInputs;
    private Integer numberOfOutputs;
    private List<Integer> yearsToTrainNetwork;
    private List<Integer> yearsToEvaluateNetwork;
    private String networkLayout;
    private ActivationFunctionChoice activationFunction;
    private LossFunctionChoice lossFunction;
    private Integer numberOfEpochs;
    private WeightInitChoice weightInit;
    private UpdaterChoice updater;
    private LocalDateTime whenTrained;
    private Boolean networkSaved;
    private Double l2RegularizationLambda;
    private DataNormalizationChoice normalizer;
    private String networkType;
    private Double dropoutProbability;
    private ActivationFunctionChoice outputLayerActivation;
    private LossFunctionChoice outputLayerLossFunction;
    private Double trainingError;
    private UUID networkId;
    private NetworkStructureAlgorithm networkStructureAlgorithm;
    private Integer numberOfHiddenLayers;
    // Runtime training stuff
    private Double networkPerformanceThreshold;
    private Integer maxNumberOfTrainingEpochs;
    private Integer networkEvaluationIndex;
    private Double maxNetworkError;
    private Integer epochStatsIndexCount;
    private Integer numberOfBatchIterations;
    private Double learningRuleLearningRate;
    private Double beta1DecayScaleValue;
    private UUID batchTrainingRunId;
    private Double networkAccuracy;
    private Double networkPrecision;
    private Double networkRecall;
    private Double networkF1Score;
    private String confusionMatrixCsv;

    public NetworkDescriptor copy() {
        return new NetworkDescriptor()
                .setNetworkSaved(getNetworkSaved())
                .setActivationFunction(getActivationFunction())
                .setLossFunction(getLossFunction())
                .setNetworkAccuracy(getNetworkAccuracy())
                .setNetworkLayout(getNetworkLayout())
                .setNumberOfEpochs(getNumberOfEpochs())
                .setNumberOfInputs(getNumberOfInputs())
                .setNumberOfOutputs(getNumberOfOutputs())
                .setUpdater(getUpdater())
                .setWeightInit(getWeightInit())
                .setWhenTrained(getWhenTrained())
                .setL2RegularizationLambda(getL2RegularizationLambda())
                .setNormalizer(getNormalizer())
                .setNetworkType(getNetworkType())
                .setDropoutProbability(getDropoutProbability())
                .setOutputLayerActivation(getOutputLayerActivation())
                .setOutputLayerLossFunction(getOutputLayerLossFunction())
                .setTrainingError(getTrainingError())
                .setYearsToTrainNetwork(getYearsToTrainNetwork())
                .setYearsToEvaluateNetwork(getYearsToEvaluateNetwork())
                .setNetworkId(getNetworkId())
                .setNetworkPerformanceThreshold(getNetworkPerformanceThreshold())
                .setNetworkStructureAlgorithm(getNetworkStructureAlgorithm())
                .setNumberOfHiddenLayers(getNumberOfHiddenLayers())
                .setMaxNumberOfTrainingEpochs(getMaxNumberOfTrainingEpochs())
                .setMaxNetworkError(getMaxNetworkError())
                .setNetworkPerformanceThreshold(getNetworkPerformanceThreshold())
                .setNetworkEvaluationIndex(getNetworkEvaluationIndex())
                .setEpochStatsIndexCount(getEpochStatsIndexCount())
                .setNumberOfBatchIterations(getNumberOfBatchIterations())
                .setLearningRuleLearningRate(getLearningRuleLearningRate())
                .setBeta1DecayScaleValue(getBeta1DecayScaleValue())
                .setNumberOfBatchIterations(getNumberOfBatchIterations())
                .setBatchTrainingRunId(getBatchTrainingRunId())
                .setNetworkPrecision(getNetworkPrecision())
                .setNetworkRecall(getNetworkRecall())
                .setNetworkF1Score(getNetworkF1Score())
                .setConfusionMatrixCsv(getConfusionMatrixCsv());
    }

    @SuppressWarnings("unused")
    public static NetworkDescriptor fromDto(final NetworkDescriptorDto dto) {
        return new NetworkDescriptor()
                .setNetworkSaved(dto.getNetworkSaved())
                .setActivationFunction(dto.getActivationFunction())
                .setLossFunction(dto.getLossFunction())
                .setNetworkAccuracy(dto.getNetworkAccuracy())
                .setNetworkLayout(dto.getNetworkLayout())
                .setNumberOfEpochs(dto.getNumberOfEpochs())
                .setNumberOfInputs(dto.getNumberOfInputs())
                .setNumberOfOutputs(dto.getNumberOfOutputs())
                .setUpdater(dto.getUpdater())
                .setWeightInit(dto.getWeightInit())
                .setWhenTrained((dto.getWhenTrained() != null) ? LocalDateTime.parse(dto.getWhenTrained(), DateTimeFormatter.ofPattern(YYYY_MM_DD_HH_MM_SS)) : null)
                .setL2RegularizationLambda(dto.getL2RegularizationLambda())
                .setNormalizer(dto.getNormalizer())
                .setNetworkType(dto.getNetworkType())
                .setDropoutProbability(dto.getDropoutProbability())
                .setOutputLayerActivation(dto.getOutputLayerActivation())
                .setOutputLayerLossFunction(dto.getOutputLayerLossFunction())
                .setTrainingError(dto.getTrainingError())
                .setYearsToTrainNetwork(dto.getYearsToTrainNetwork())
                .setYearsToEvaluateNetwork(dto.getYearsToEvaluateNetwork())
                .setNetworkId(dto.getNetworkId())
                .setNetworkPerformanceThreshold(dto.getNetworkThreshold())
                .setNetworkStructureAlgorithm(dto.getNetworkStructureAlgorithm())
                .setNumberOfHiddenLayers(dto.getNumberOfHiddenLayers())
                .setMaxNumberOfTrainingEpochs(dto.getMaxNumberOfTrainingEpochs())
                .setMaxNetworkError(dto.getMaxNetworkError())
                .setNetworkPerformanceThreshold(dto.getNetworkPerformanceThreshold())
                .setNetworkEvaluationIndex(dto.getNetworkEvaluationIndex())
                .setEpochStatsIndexCount(dto.getEpochStatsIndexCount())
                .setNumberOfBatchIterations(dto.getNumberOfBatchIterations())
                .setLearningRuleLearningRate(dto.getLearningRuleLearningRate())
                .setBeta1DecayScaleValue(dto.getBeta1DecayScaleValue())
                .setNumberOfBatchIterations(dto.getNumberOfBatchIterations())
                .setBatchTrainingRunId(dto.getBatchTrainingRunId())
                .setNetworkPrecision(dto.getNetworkPrecision())
                .setNetworkRecall(dto.getNetworkRecall())
                .setNetworkF1Score(dto.getNetworkF1Score())
                .setConfusionMatrixCsv(dto.getConfusionMatrixCsv());
    }

    @SuppressWarnings("unused")
    public NetworkDescriptorDto toDto() {
        return new NetworkDescriptorDto()
                .setNetworkSaved(getNetworkSaved())
                .setActivationFunction(getActivationFunction())
                .setLossFunction(getLossFunction())
                .setNetworkAccuracy(getNetworkAccuracy())
                .setNetworkLayout(getNetworkLayout())
                .setNumberOfEpochs(getNumberOfEpochs())
                .setNumberOfInputs(getNumberOfInputs())
                .setNumberOfOutputs(getNumberOfOutputs())
                .setUpdater(getUpdater())
                .setWeightInit(getWeightInit())
                .setWhenTrained((getWhenTrained() != null) ? getWhenTrained().format(DateTimeFormatter.ofPattern(YYYY_MM_DD_HH_MM_SS)) : null)
                .setL2RegularizationLambda(getL2RegularizationLambda())
                .setNormalizer(getNormalizer())
                .setNetworkType(getNetworkType())
                .setDropoutProbability(getDropoutProbability())
                .setOutputLayerActivation(getOutputLayerActivation())
                .setOutputLayerLossFunction(getOutputLayerLossFunction())
                .setTrainingError(getTrainingError())
                .setYearsToTrainNetwork(getYearsToTrainNetwork())
                .setYearsToEvaluateNetwork(getYearsToEvaluateNetwork())
                .setNetworkId(getNetworkId())
                .setNetworkThreshold(getNetworkPerformanceThreshold())
                .setNetworkStructureAlgorithm(getNetworkStructureAlgorithm())
                .setNumberOfHiddenLayers(getNumberOfHiddenLayers())
                .setMaxNumberOfTrainingEpochs(getMaxNumberOfTrainingEpochs())
                .setMaxNetworkError(getMaxNetworkError())
                .setNetworkPerformanceThreshold(getNetworkPerformanceThreshold())
                .setNetworkEvaluationIndex(getNetworkEvaluationIndex())
                .setEpochStatsIndexCount(getEpochStatsIndexCount())
                .setNumberOfBatchIterations(getNumberOfBatchIterations())
                .setLearningRuleLearningRate(getLearningRuleLearningRate())
                .setBeta1DecayScaleValue(getBeta1DecayScaleValue())
                .setNumberOfBatchIterations(getNumberOfBatchIterations())
                .setBatchTrainingRunId(getBatchTrainingRunId())
                .setNetworkPrecision(getNetworkPrecision())
                .setNetworkRecall(getNetworkRecall())
                .setNetworkF1Score(getNetworkF1Score())
                .setConfusionMatrixCsv(getConfusionMatrixCsv());
    }

    public Integer getNumberOfInputs() {
        return numberOfInputs;
    }

    public NetworkDescriptor setNumberOfInputs(Integer numberOfInputs) {
        this.numberOfInputs = numberOfInputs;
        return this;
    }

    public Integer getNumberOfOutputs() {
        return numberOfOutputs;
    }

    public NetworkDescriptor setNumberOfOutputs(Integer numberOfOutputs) {
        this.numberOfOutputs = numberOfOutputs;
        return this;
    }

    public List<Integer> getYearsToTrainNetwork() {
        if (yearsToTrainNetwork == null) {
            yearsToTrainNetwork = new ArrayList<>();
        }
        return yearsToTrainNetwork;
    }

    public NetworkDescriptor setYearsToTrainNetwork(List<Integer> yearsToTrainNetwork) {
        this.yearsToTrainNetwork = yearsToTrainNetwork;
        return this;
    }

    public List<Integer> getYearsToEvaluateNetwork() {
        if (yearsToEvaluateNetwork == null) {
            yearsToEvaluateNetwork = new ArrayList<>();
        }
        return yearsToEvaluateNetwork;
    }

    public NetworkDescriptor setYearsToEvaluateNetwork(List<Integer> yearsToEvaluateNetwork) {
        this.yearsToEvaluateNetwork = yearsToEvaluateNetwork;
        return this;
    }

    public String getNetworkLayout() {
        return networkLayout;
    }

    public NetworkDescriptor setNetworkLayout(String networkLayout) {
        this.networkLayout = networkLayout;
        return this;
    }

    public ActivationFunctionChoice getActivationFunction() {
        return activationFunction;
    }

    public NetworkDescriptor setActivationFunction(ActivationFunctionChoice activationFunction) {
        this.activationFunction = activationFunction;
        return this;
    }

    public LossFunctionChoice getLossFunction() {
        return lossFunction;
    }

    public NetworkDescriptor setLossFunction(LossFunctionChoice lossFunction) {
        this.lossFunction = lossFunction;
        return this;
    }

    public Integer getNumberOfEpochs() {
        return numberOfEpochs;
    }

    public NetworkDescriptor setNumberOfEpochs(Integer numberOfEpochs) {
        this.numberOfEpochs = numberOfEpochs;
        return this;
    }

    public WeightInitChoice getWeightInit() {
        return weightInit;
    }

    public NetworkDescriptor setWeightInit(WeightInitChoice weightInit) {
        this.weightInit = weightInit;
        return this;
    }

    public UpdaterChoice getUpdater() {
        return updater;
    }

    public NetworkDescriptor setUpdater(UpdaterChoice updater) {
        this.updater = updater;
        return this;
    }

    public LocalDateTime getWhenTrained() {
        return whenTrained;
    }

    public NetworkDescriptor setWhenTrained(LocalDateTime whenTrained) {
        this.whenTrained = whenTrained;
        return this;
    }

    public Double getNetworkAccuracy() {
        return (networkAccuracy == null) ? 0.0 : networkAccuracy;
    }

    public NetworkDescriptor setNetworkAccuracy(Double networkAccuracy) {
        this.networkAccuracy = networkAccuracy;
        return this;
    }

    public Boolean getNetworkSaved() {
        return networkSaved;
    }

    public NetworkDescriptor setNetworkSaved(Boolean networkSaved) {
        this.networkSaved = networkSaved;
        return this;
    }

    public Double getL2RegularizationLambda() {
        return l2RegularizationLambda;
    }

    public NetworkDescriptor setL2RegularizationLambda(Double l2RegularizationLambda) {
        this.l2RegularizationLambda = l2RegularizationLambda;
        return this;
    }

    public DataNormalizationChoice getNormalizer() {
        return normalizer;
    }

    public NetworkDescriptor setNormalizer(DataNormalizationChoice normalizer) {
        this.normalizer = normalizer;
        return this;
    }

    public String getNetworkType() {
        return networkType;
    }

    public NetworkDescriptor setNetworkType(String networkType) {
        this.networkType = networkType;
        return this;
    }

    public Double getDropoutProbability() {
        return dropoutProbability;
    }

    public NetworkDescriptor setDropoutProbability(Double dropoutProbability) {
        this.dropoutProbability = dropoutProbability;
        return this;
    }

    public ActivationFunctionChoice getOutputLayerActivation() {
        return outputLayerActivation;
    }

    public NetworkDescriptor setOutputLayerActivation(ActivationFunctionChoice outputLayerActivation) {
        this.outputLayerActivation = outputLayerActivation;
        return this;
    }

    public LossFunctionChoice getOutputLayerLossFunction() {
        return outputLayerLossFunction;
    }

    public NetworkDescriptor setOutputLayerLossFunction(LossFunctionChoice outputLayerLossFunction) {
        this.outputLayerLossFunction = outputLayerLossFunction;
        return this;
    }

    public Double getTrainingError() {
        return (trainingError == null) ? 0.0 : trainingError;
    }

    public NetworkDescriptor setTrainingError(Double trainingError) {
        this.trainingError = trainingError;
        return this;
    }

    public UUID getNetworkId() {
        return networkId;
    }

    public NetworkDescriptor setNetworkId(final UUID networkId) {
        this.networkId = networkId;
        return this;
    }

    public Double getNetworkPerformanceThreshold() {
        return networkPerformanceThreshold;
    }

    public NetworkDescriptor setNetworkPerformanceThreshold(final Double networkPerformanceThreshold) {
        this.networkPerformanceThreshold = networkPerformanceThreshold;
        return this;
    }

    public NetworkStructureAlgorithm getNetworkStructureAlgorithm() {
        return networkStructureAlgorithm;
    }

    public NetworkDescriptor setNetworkStructureAlgorithm(final NetworkStructureAlgorithm networkStructureAlgorithm) {
        this.networkStructureAlgorithm = networkStructureAlgorithm;
        return this;
    }

    public Integer getNumberOfHiddenLayers() {
        return numberOfHiddenLayers;
    }

    public NetworkDescriptor setNumberOfHiddenLayers(final Integer numberOfHiddenLayers) {
        this.numberOfHiddenLayers = numberOfHiddenLayers;
        return this;
    }

    public Integer getMaxNumberOfTrainingEpochs() {
        return maxNumberOfTrainingEpochs;
    }

    public NetworkDescriptor setMaxNumberOfTrainingEpochs(final Integer maxNumberOfTrainingEpochs) {
        this.maxNumberOfTrainingEpochs = maxNumberOfTrainingEpochs;
        return this;
    }

    public Integer getNetworkEvaluationIndex() {
        return networkEvaluationIndex;
    }

    public NetworkDescriptor setNetworkEvaluationIndex(final Integer networkEvaluationIndex) {
        this.networkEvaluationIndex = networkEvaluationIndex;
        return this;
    }

    public Double getMaxNetworkError() {
        return maxNetworkError;
    }

    public NetworkDescriptor setMaxNetworkError(final Double maxNetworkError) {
        this.maxNetworkError = maxNetworkError;
        return this;
    }

    public Integer getEpochStatsIndexCount() {
        return epochStatsIndexCount;
    }

    public NetworkDescriptor setEpochStatsIndexCount(final Integer epochStatsIndexCount) {
        this.epochStatsIndexCount = epochStatsIndexCount;
        return this;
    }

    public Integer getNumberOfBatchIterations() {
        return numberOfBatchIterations;
    }

    public NetworkDescriptor setNumberOfBatchIterations(final Integer numberOfBatchIterations) {
        this.numberOfBatchIterations = numberOfBatchIterations;
        return this;
    }

    public Double getLearningRuleLearningRate() {
        return learningRuleLearningRate;
    }

    public NetworkDescriptor setLearningRuleLearningRate(final Double learningRuleLearningRate) {
        this.learningRuleLearningRate = learningRuleLearningRate;
        return this;
    }

    public Double getBeta1DecayScaleValue() {
        return beta1DecayScaleValue;
    }

    public NetworkDescriptor setBeta1DecayScaleValue(final Double beta1DecayScaleValue) {
        this.beta1DecayScaleValue = beta1DecayScaleValue;
        return this;
    }

    public UUID getBatchTrainingRunId() {
        return batchTrainingRunId;
    }

    public NetworkDescriptor setBatchTrainingRunId(final UUID batchTrainingRunId) {
        this.batchTrainingRunId = batchTrainingRunId;
        return this;
    }

    public Double getNetworkPrecision() {
        return (networkPrecision == null) ? 0.0 : networkPrecision;
    }

    public NetworkDescriptor setNetworkPrecision(final Double networkPrecision) {
        this.networkPrecision = networkPrecision;
        return this;
    }

    public Double getNetworkRecall() {
        return (networkRecall == null) ? 0.0 : networkRecall;
    }

    public NetworkDescriptor setNetworkRecall(final Double networkRecall) {
        this.networkRecall = networkRecall;
        return this;
    }

    public Double getNetworkF1Score() {
        return (networkF1Score == null) ? 0.0 : networkF1Score;
    }

    public NetworkDescriptor setNetworkF1Score(final Double networkF1Score) {
        this.networkF1Score = networkF1Score;
        return this;
    }

    @SuppressWarnings("unused")  // Framework classes do not need this inspection
    public String getTrainingYearsString() {
        return StringUtils.join(getYearsToTrainNetwork(), ',');
    }

    public String getConfusionMatrixCsv() {
        return confusionMatrixCsv;
    }

    public NetworkDescriptor setConfusionMatrixCsv(final String confusionMatrixCsv) {
        this.confusionMatrixCsv = confusionMatrixCsv;
        return this;
    }

    /**
     * Computes the real accuracy of the network by combining the four horsemen:
     * - accuracy
     * - precision
     * - recall
     * -f1
     *
     * This method returns the average of those numbers as a fraction (between 0.0 and 1.0).
     */
    public Double getRealNetworkAccuracy() {
        return (getNetworkAccuracy() + getNetworkPrecision() + getNetworkRecall() + getNetworkF1Score()) / 4.0;
    }

    public String toString() {
        return String.format("Network info%n") +
                String.format("\tNetwork structure           : %s%n", getNetworkLayout()) +
                String.format("\tTraining Years              : %s%n", (!getYearsToTrainNetwork().isEmpty()) ? StringUtils.join(getYearsToTrainNetwork(), ',') : getYearsToTrainNetwork()) +
                String.format("\tEvaluation Years            : %s%n", (!getYearsToEvaluateNetwork().isEmpty()) ? StringUtils.join(getYearsToEvaluateNetwork(), ',') : getYearsToEvaluateNetwork()) +
                String.format("\tUpdater function            : %s%n", getUpdater()) +
                String.format("\tActivation function         : %s%n", getActivationFunction()) +
                String.format("\tActivation function (OL)    : %s%n", getOutputLayerActivation()) +
                String.format("\tLoss function               : %s%n", getLossFunction()) +
                String.format("\tLoss function (OL)          : %s%n", getOutputLayerLossFunction()) +
                String.format("\tWeight init function        : %s%n", getWeightInit()) +
                String.format("\tNumber of inputs            : %d%n", getNumberOfInputs()) +
                String.format("\tNumber of outputs           : %d%n", getNumberOfOutputs()) +
                String.format("\tNumber of training epochs   : %d%n", getNumberOfEpochs()) +
                String.format("\tNetwork accuracy            : %5.5f%%%n", getNetworkAccuracy() * 100.0) +
                String.format("\tNetwork accuracy (real)     : %5.5f%%%n", getRealNetworkAccuracy() * 100.0) +
                String.format("\tNetwork precision           : %5.5f%%%n", getNetworkPrecision() * 100.0) +
                String.format("\tNetwork recall              : %5.5f%%%n", getNetworkRecall() * 100.0) +
                String.format("\tF1 Score                    : %5.5f%%%n", getNetworkF1Score() * 100.0) +
                String.format("\tL2 Regularization Lambda    : %5.5f%n", getL2RegularizationLambda()) +
                String.format("\tNormalizer                  : %s%n", getNormalizer()) +
                String.format("\tNetwork type                : %s%n", getNetworkType()) +
                String.format("\tDropout probability         : %3.2f%%%n", getDropoutProbability() * 100) +
                String.format("\tTraining error              : %3.2f%%%n", getTrainingError() * 100) +
                //String.format("\tData Elements               : %s%n", StringUtils.join(getSelectedElements(), ',')) +
                String.format("\tNetwork ID                  : %s%n", (getNetworkId() != null) ? getNetworkId().toString() : null) +
                String.format("\tNetwork Threshold           : %2.2f%n", getNetworkPerformanceThreshold()) +
                String.format("\tNetwork Structure Algorithm : %s%n", getNetworkStructureAlgorithm()) +
                String.format("\tNumber of hidden layers     : %d%n", getNumberOfHiddenLayers()) +
                String.format("\tBatch training run ID       : %s%n", getBatchTrainingRunId()) +
                String.format("\tNetwork Saved Flag          : %s%n", getNetworkSaved()) +
                String.format("\tRule Learning Rate          : %5.5f%%%n", getLearningRuleLearningRate())
                ;
    }

    public String toStringAbbreviated() {
        return toString();
    }

    public String toStringOneLine() {
        return String.format("ACC=%3.2f%%/", 100.0 * getRealNetworkAccuracy()) +
                String.format("ERR=%3.2f%%/", 100.0 * getTrainingError()) +
                String.format("ACT=%8s/", getActivationFunction().name()) +
                String.format("UPD=%20s/", getUpdater().name()) +
                String.format("LOSS=%10s/", getLossFunction().name()) +
                String.format("LOSSOL=%10s/", getOutputLayerLossFunction().name()) +
                String.format("WT=%10s/", getWeightInit()) +
                String.format("ACTO=%10s/", getOutputLayerActivation().name()) +
                String.format("L2=%1.6f/", getL2RegularizationLambda()) +
                String.format("DRP=%3.2f%%/", 100.0 * getDropoutProbability()) +
                String.format("EP=%5d/", getNumberOfEpochs()) +
                String.format("N=%s/", getNormalizer().name()) +
                String.format("NSA=%s/", getNetworkStructureAlgorithm().name()) +
                String.format("%s", getNetworkLayout())
                ;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        final NetworkDescriptor that = (NetworkDescriptor) o;

        return new EqualsBuilder()
                .append(numberOfInputs, that.numberOfInputs)
                .append(numberOfOutputs, that.numberOfOutputs)
                .append(yearsToTrainNetwork, that.yearsToTrainNetwork)
                .append(yearsToEvaluateNetwork, that.yearsToEvaluateNetwork)
                .append(networkLayout, that.networkLayout)
                .append(activationFunction, that.activationFunction)
                .append(lossFunction, that.lossFunction)
                .append(numberOfEpochs, that.numberOfEpochs)
                .append(weightInit, that.weightInit)
                .append(updater, that.updater)
                .append(whenTrained, that.whenTrained)
                .append(networkSaved, that.networkSaved)
                .append(l2RegularizationLambda, that.l2RegularizationLambda)
                .append(normalizer, that.normalizer)
                .append(networkType, that.networkType)
                .append(dropoutProbability, that.dropoutProbability)
                .append(outputLayerActivation, that.outputLayerActivation)
                .append(outputLayerLossFunction, that.outputLayerLossFunction)
                .append(trainingError, that.trainingError)
                .append(networkId, that.networkId)
                .append(networkStructureAlgorithm, that.networkStructureAlgorithm)
                .append(numberOfHiddenLayers, that.numberOfHiddenLayers)
                .append(networkPerformanceThreshold, that.networkPerformanceThreshold)
                .append(maxNumberOfTrainingEpochs, that.maxNumberOfTrainingEpochs)
                .append(networkEvaluationIndex, that.networkEvaluationIndex)
                .append(maxNetworkError, that.maxNetworkError)
                .append(epochStatsIndexCount, that.epochStatsIndexCount)
                .append(numberOfBatchIterations, that.numberOfBatchIterations)
                .append(learningRuleLearningRate, that.learningRuleLearningRate)
                .append(beta1DecayScaleValue, that.beta1DecayScaleValue)
                .append(batchTrainingRunId, that.batchTrainingRunId)
                .append(networkAccuracy, that.networkAccuracy)
                .append(networkPrecision, that.networkPrecision)
                .append(networkRecall, that.networkRecall)
                .append(networkF1Score, that.networkF1Score)
                .append(confusionMatrixCsv, that.confusionMatrixCsv)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(numberOfInputs)
                .append(numberOfOutputs)
                .append(yearsToTrainNetwork)
                .append(yearsToEvaluateNetwork)
                .append(networkLayout)
                .append(activationFunction)
                .append(lossFunction)
                .append(numberOfEpochs)
                .append(weightInit)
                .append(updater)
                .append(whenTrained)
                .append(networkSaved)
                .append(l2RegularizationLambda)
                .append(normalizer)
                .append(networkType)
                .append(dropoutProbability)
                .append(outputLayerActivation)
                .append(outputLayerLossFunction)
                .append(trainingError)
                .append(networkId)
                .append(networkStructureAlgorithm)
                .append(numberOfHiddenLayers)
                .append(networkPerformanceThreshold)
                .append(maxNumberOfTrainingEpochs)
                .append(networkEvaluationIndex)
                .append(maxNetworkError)
                .append(epochStatsIndexCount)
                .append(numberOfBatchIterations)
                .append(learningRuleLearningRate)
                .append(beta1DecayScaleValue)
                .append(batchTrainingRunId)
                .append(networkAccuracy)
                .append(networkPrecision)
                .append(networkRecall)
                .append(networkF1Score)
                .append(confusionMatrixCsv)
                .toHashCode();
    }
}
