/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jstevenperry.sportsnet.framework.config.NetworkStructureAlgorithm;
import com.jstevenperry.sportsnet.framework.model.ActivationFunctionChoice;
import com.jstevenperry.sportsnet.framework.model.DataNormalizationChoice;
import com.jstevenperry.sportsnet.framework.model.LossFunctionChoice;
import com.jstevenperry.sportsnet.framework.model.UpdaterChoice;
import com.jstevenperry.sportsnet.framework.model.WeightInitChoice;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("unused")
public class NetworkDescriptorDto implements Serializable {

    private UUID networkId;

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
    private String whenTrained;
    private Double realNetworkAccuracy;
    private Double networkAccuracy;
    private Double networkPrecision;
    private Double networkRecall;
    private Double networkF1Score;
    private Boolean networkSaved;
    private Double l2RegularizationLambda;
    private DataNormalizationChoice normalizer;
    private String networkType;
    private Double dropoutProbability;
    private ActivationFunctionChoice outputLayerActivation;
    private LossFunctionChoice outputLayerLossFunction;
    private Double trainingError;
    private Double networkThreshold;
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
    private String confusionMatrixCsv;

    public Integer getNumberOfInputs() {
        return numberOfInputs;
    }

    public NetworkDescriptorDto setNumberOfInputs(Integer numberOfInputs) {
        this.numberOfInputs = numberOfInputs;
        return this;
    }

    public Integer getNumberOfOutputs() {
        return numberOfOutputs;
    }

    public NetworkDescriptorDto setNumberOfOutputs(Integer numberOfOutputs) {
        this.numberOfOutputs = numberOfOutputs;
        return this;
    }

    public List<Integer> getYearsToTrainNetwork() {
        return yearsToTrainNetwork;
    }

    public NetworkDescriptorDto setYearsToTrainNetwork(List<Integer> yearsToTrainNetwork) {
        this.yearsToTrainNetwork = yearsToTrainNetwork;
        return this;
    }

    public List<Integer> getYearsToEvaluateNetwork() {
        return yearsToEvaluateNetwork;
    }

    public NetworkDescriptorDto setYearsToEvaluateNetwork(List<Integer> yearsToEvaluateNetwork) {
        this.yearsToEvaluateNetwork = yearsToEvaluateNetwork;
        return this;
    }

    public String getNetworkLayout() {
        return networkLayout;
    }

    public NetworkDescriptorDto setNetworkLayout(String networkLayout) {
        this.networkLayout = networkLayout;
        return this;
    }

    public ActivationFunctionChoice getActivationFunction() {
        return activationFunction;
    }

    public NetworkDescriptorDto setActivationFunction(ActivationFunctionChoice activationFunction) {
        this.activationFunction = activationFunction;
        return this;
    }

    public LossFunctionChoice getLossFunction() {
        return lossFunction;
    }

    public NetworkDescriptorDto setLossFunction(LossFunctionChoice lossFunction) {
        this.lossFunction = lossFunction;
        return this;
    }

    public Integer getNumberOfEpochs() {
        return numberOfEpochs;
    }

    public NetworkDescriptorDto setNumberOfEpochs(Integer numberOfEpochs) {
        this.numberOfEpochs = numberOfEpochs;
        return this;
    }

    public WeightInitChoice getWeightInit() {
        return weightInit;
    }

    public NetworkDescriptorDto setWeightInit(WeightInitChoice weightInit) {
        this.weightInit = weightInit;
        return this;
    }

    public UpdaterChoice getUpdater() {
        return updater;
    }

    public NetworkDescriptorDto setUpdater(UpdaterChoice updater) {
        this.updater = updater;
        return this;
    }

    public String getWhenTrained() {
        return whenTrained;
    }

    public NetworkDescriptorDto setWhenTrained(String whenTrained) {
        this.whenTrained = whenTrained;
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
        this.realNetworkAccuracy = (getNetworkAccuracy() + getNetworkPrecision() + getNetworkRecall() + getNetworkF1Score()) / 4.0;
        return this.realNetworkAccuracy;
    }

    public NetworkDescriptorDto setRealNetworkAccuracy(final Double realNetworkAccuracy) {
        this.realNetworkAccuracy = realNetworkAccuracy;
        return this;
    }

    public Double getNetworkAccuracy() {
        return networkAccuracy;
    }

    public NetworkDescriptorDto setNetworkAccuracy(Double networkAccuracy) {
        this.networkAccuracy = networkAccuracy;
        return this;
    }

    public Double getNetworkPrecision() {
        return networkPrecision;
    }

    public NetworkDescriptorDto setNetworkPrecision(final Double networkPrecision) {
        this.networkPrecision = networkPrecision;
        return this;
    }

    public Double getNetworkRecall() {
        return networkRecall;
    }

    public NetworkDescriptorDto setNetworkRecall(final Double networkRecall) {
        this.networkRecall = networkRecall;
        return this;
    }

    public Double getNetworkF1Score() {
        return networkF1Score;
    }

    public NetworkDescriptorDto setNetworkF1Score(final Double networkF1Score) {
        this.networkF1Score = networkF1Score;
        return this;
    }

    public Boolean getNetworkSaved() {
        return networkSaved;
    }

    public NetworkDescriptorDto setNetworkSaved(Boolean networkSaved) {
        this.networkSaved = networkSaved;
        return this;
    }

    public Double getL2RegularizationLambda() {
        return l2RegularizationLambda;
    }

    public NetworkDescriptorDto setL2RegularizationLambda(Double l2RegularizationLambda) {
        this.l2RegularizationLambda = l2RegularizationLambda;
        return this;
    }

    public DataNormalizationChoice getNormalizer() {
        return normalizer;
    }

    public NetworkDescriptorDto setNormalizer(DataNormalizationChoice normalizer) {
        this.normalizer = normalizer;
        return this;
    }

    public String getNetworkType() {
        return networkType;
    }

    public NetworkDescriptorDto setNetworkType(String networkType) {
        this.networkType = networkType;
        return this;
    }

    public Double getDropoutProbability() {
        return dropoutProbability;
    }

    public NetworkDescriptorDto setDropoutProbability(Double dropoutProbability) {
        this.dropoutProbability = dropoutProbability;
        return this;
    }

    public ActivationFunctionChoice getOutputLayerActivation() {
        return outputLayerActivation;
    }

    public NetworkDescriptorDto setOutputLayerActivation(ActivationFunctionChoice outputLayerActivation) {
        this.outputLayerActivation = outputLayerActivation;
        return this;
    }

    public LossFunctionChoice getOutputLayerLossFunction() {
        return outputLayerLossFunction;
    }

    public NetworkDescriptorDto setOutputLayerLossFunction(LossFunctionChoice outputLayerLossFunction) {
        this.outputLayerLossFunction = outputLayerLossFunction;
        return this;
    }

    public Double getTrainingError() {
        return trainingError;
    }

    public NetworkDescriptorDto setTrainingError(Double trainingError) {
        this.trainingError = trainingError;
        return this;
    }

    public UUID getNetworkId() {
        return networkId;
    }

    public NetworkDescriptorDto setNetworkId(final UUID networkId) {
        this.networkId = networkId;
        return this;
    }

    public Double getNetworkThreshold() {
        return networkThreshold;
    }

    public NetworkDescriptorDto setNetworkThreshold(final Double networkThreshold) {
        this.networkThreshold = networkThreshold;
        return this;
    }

    public NetworkStructureAlgorithm getNetworkStructureAlgorithm() {
        return networkStructureAlgorithm;
    }

    public NetworkDescriptorDto setNetworkStructureAlgorithm(final NetworkStructureAlgorithm networkStructureAlgorithm) {
        this.networkStructureAlgorithm = networkStructureAlgorithm;
        return this;
    }

    public Integer getNumberOfHiddenLayers() {
        return numberOfHiddenLayers;
    }

    public NetworkDescriptorDto setNumberOfHiddenLayers(final Integer numberOfHiddenLayers) {
        this.numberOfHiddenLayers = numberOfHiddenLayers;
        return this;
    }

    public Double getNetworkPerformanceThreshold() {
        return networkPerformanceThreshold;
    }

    public NetworkDescriptorDto setNetworkPerformanceThreshold(final Double networkPerformanceThreshold) {
        this.networkPerformanceThreshold = networkPerformanceThreshold;
        return this;
    }

    public Integer getMaxNumberOfTrainingEpochs() {
        return maxNumberOfTrainingEpochs;
    }

    public NetworkDescriptorDto setMaxNumberOfTrainingEpochs(final Integer maxNumberOfTrainingEpochs) {
        this.maxNumberOfTrainingEpochs = maxNumberOfTrainingEpochs;
        return this;
    }

    public Integer getNetworkEvaluationIndex() {
        return networkEvaluationIndex;
    }

    public NetworkDescriptorDto setNetworkEvaluationIndex(final Integer networkEvaluationIndex) {
        this.networkEvaluationIndex = networkEvaluationIndex;
        return this;
    }

    public Double getMaxNetworkError() {
        return maxNetworkError;
    }

    public NetworkDescriptorDto setMaxNetworkError(final Double maxNetworkError) {
        this.maxNetworkError = maxNetworkError;
        return this;
    }

    public Integer getEpochStatsIndexCount() {
        return epochStatsIndexCount;
    }

    public NetworkDescriptorDto setEpochStatsIndexCount(final Integer epochStatsIndexCount) {
        this.epochStatsIndexCount = epochStatsIndexCount;
        return this;
    }

    public Integer getNumberOfBatchIterations() {
        return numberOfBatchIterations;
    }

    public NetworkDescriptorDto setNumberOfBatchIterations(final Integer numberOfBatchIterations) {
        this.numberOfBatchIterations = numberOfBatchIterations;
        return this;
    }

    public Double getLearningRuleLearningRate() {
        return learningRuleLearningRate;
    }

    public NetworkDescriptorDto setLearningRuleLearningRate(final Double learningRuleLearningRate) {
        this.learningRuleLearningRate = learningRuleLearningRate;
        return this;
    }

    public Double getBeta1DecayScaleValue() {
        return beta1DecayScaleValue;
    }

    public NetworkDescriptorDto setBeta1DecayScaleValue(final Double beta1DecayScaleValue) {
        this.beta1DecayScaleValue = beta1DecayScaleValue;
        return this;
    }

    public UUID getBatchTrainingRunId() {
        return batchTrainingRunId;
    }

    public NetworkDescriptorDto setBatchTrainingRunId(final UUID batchTrainingRunId) {
        this.batchTrainingRunId = batchTrainingRunId;
        return this;
    }

    public String getConfusionMatrixCsv() {
        return confusionMatrixCsv;
    }

    public NetworkDescriptorDto setConfusionMatrixCsv(final String confusionMatrixCsv) {
        this.confusionMatrixCsv = confusionMatrixCsv;
        return this;
    }

    public String toJsonString() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(this);
    }

    @Override
    public String toString() {
        return "NetworkParametersDto{" +
                "numberOfInputs=" + numberOfInputs +
                ", numberOfOutputs=" + numberOfOutputs +
                ", yearsToTrainNetwork=[" + StringUtils.join(yearsToTrainNetwork, ',') + "]" +
                ", yearsToEvaluateNetwork=[" + StringUtils.join(yearsToEvaluateNetwork, ',') + "]" +
                ", networkLayout='" + networkLayout + '\'' +
                ", activationFunction=" + activationFunction +
                ", lossFunction=" + lossFunction +
                ", numberOfEpochs=" + numberOfEpochs +
                ", weightInit=" + weightInit +
                ", updater=" + updater +
                //", selectedElements=" + selectedElements +
                ", whenTrained=" + whenTrained +
                ", networkAccuracy=" + networkAccuracy +
                ", networkSaved=" + networkSaved +
                ", l2RegularizationLambda=" + l2RegularizationLambda +
                ", normalizer=" + normalizer +
                ", networkType='" + networkType + '\'' +
                ", dropoutProbability=" + dropoutProbability +
                ", outputLayerActivation=" + outputLayerActivation +
                ", outputLayerLossFunction=" + outputLayerLossFunction +
                ", trainingError=" + trainingError +
                ", networkId=" + networkId +
                ", networkThreshold=" + networkThreshold +
                ", networkStructureAlgorithm=" + networkStructureAlgorithm +
                ", numberOfHiddenLayers=" + numberOfHiddenLayers +
                ", networkPerformanceThreshold=" + networkPerformanceThreshold +
                ", maxLearningIterations=" + maxNumberOfTrainingEpochs +
                ", networkEvaluationIndex=" + networkEvaluationIndex +
                ", maxNetworkError=" + maxNetworkError +
                ", epochStatsIndexCount=" + epochStatsIndexCount +
                ", numberOfBatchIterations=" + numberOfBatchIterations +
                ", learningRuleLearningRate=" + learningRuleLearningRate +
                ", beta1DecayScaleValue=" + beta1DecayScaleValue +
                ", batchTrainingRunId=" + batchTrainingRunId +
                ", precision=" + networkPrecision +
                ", recall=" + networkRecall +
                ", f1Score=" + networkF1Score +
                ", confusionMatrixCsv=" + confusionMatrixCsv +
                '}';
    }
}
