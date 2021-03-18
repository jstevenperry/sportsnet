/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.jstevenperry.sportsnet.framework.menus.AbstractDataElementChoice;
import com.jstevenperry.sportsnet.framework.menus.DataNormalizationMenu;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.IUpdater;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NetworkParametersDto implements Serializable {

    @JsonCreator
    public NetworkParametersDto() {

    }

    private Integer numberOfInputs;
    private Integer numberOfOutputs;
    private final List<Integer> yearsToTrainNetwork = new ArrayList<>();
    private final List<Integer> yearsToEvaluateNetwork = new ArrayList<>();
    private String networkLayout;
    private Activation activationFunction;
    private LossFunctions.LossFunction lossFunction;
    private Integer numberOfEpochs;
    private WeightInit weightInit;
    private IUpdater updater;
    private List<AbstractDataElementChoice> selectedElements;
    private LocalDateTime whenTrained;
    private double networkAccuracy;
    private boolean networkSaved;
    private double l2RegularizationLambda = 0.001; // Do not allow this to be zero
    private DataNormalizationMenu.DataNormalizationChoice normalizer;
    private String networkType;
    private double dropoutProbability;

    public Integer getNumberOfInputs() {
        return numberOfInputs;
    }

    public NetworkParametersDto setNumberOfInputs(final Integer numberOfInputs) {
        this.numberOfInputs = numberOfInputs;
        return this;
    }

    public Integer getNumberOfOutputs() {
        return numberOfOutputs;
    }

    public NetworkParametersDto setNumberOfOutputs(final Integer numberOfOutputs) {
        this.numberOfOutputs = numberOfOutputs;
        return this;
    }

    public List<Integer> getYearsToTrainNetwork() {
        return yearsToTrainNetwork;
    }

    public List<Integer> getYearsToEvaluateNetwork() {
        return yearsToEvaluateNetwork;
    }

    public String getNetworkLayout() {
        return networkLayout;
    }

    public NetworkParametersDto setNetworkLayout(final String networkLayout) {
        this.networkLayout = networkLayout;
        return this;
    }

    public Activation getActivationFunction() {
        return activationFunction;
    }

    public NetworkParametersDto setActivationFunction(final Activation activationFunction) {
        this.activationFunction = activationFunction;
        return this;
    }

    public LossFunctions.LossFunction getLossFunction() {
        return lossFunction;
    }

    public NetworkParametersDto setLossFunction(final LossFunctions.LossFunction lossFunction) {
        this.lossFunction = lossFunction;
        return this;
    }

    public Integer getNumberOfEpochs() {
        return numberOfEpochs;
    }

    public NetworkParametersDto setNumberOfEpochs(final Integer numberOfEpochs) {
        this.numberOfEpochs = numberOfEpochs;
        return this;
    }

    public WeightInit getWeightInit() {
        return weightInit;
    }

    public NetworkParametersDto setWeightInit(final WeightInit weightInit) {
        this.weightInit = weightInit;
        return this;
    }

    public IUpdater getUpdater() {
        return updater;
    }

    public NetworkParametersDto setUpdater(final IUpdater updater) {
        this.updater = updater;
        return this;
    }

    public List<AbstractDataElementChoice> getSelectedElements() {
        return selectedElements;
    }

    public NetworkParametersDto setSelectedElements(final List<AbstractDataElementChoice> selectedElements) {
        this.selectedElements = selectedElements;
        return this;
    }

    public LocalDateTime getWhenTrained() {
        return whenTrained;
    }

    public NetworkParametersDto setWhenTrained(final LocalDateTime whenTrained) {
        this.whenTrained = whenTrained;
        return this;
    }

    public double getNetworkAccuracy() {
        return networkAccuracy;
    }

    public NetworkParametersDto setNetworkAccuracy(final double networkAccuracy) {
        this.networkAccuracy = networkAccuracy;
        return this;
    }

    public boolean isNetworkSaved() {
        return networkSaved;
    }

    public NetworkParametersDto setNetworkSaved(final boolean networkSaved) {
        this.networkSaved = networkSaved;
        return this;
    }

    public double getL2RegularizationLambda() {
        return l2RegularizationLambda;
    }

    public NetworkParametersDto setL2RegularizationLambda(final double l2RegularizationLambda) {
        this.l2RegularizationLambda = l2RegularizationLambda;
        return this;
    }

    public DataNormalizationMenu.DataNormalizationChoice getNormalizer() {
        return normalizer;
    }

    public NetworkParametersDto setNormalizer(final DataNormalizationMenu.DataNormalizationChoice normalizer) {
        this.normalizer = normalizer;
        return this;
    }

    public String getNetworkType() {
        return networkType;
    }

    public NetworkParametersDto setNetworkType(final String networkType) {
        this.networkType = networkType;
        return this;
    }

    public double getDropoutProbability() {
        return dropoutProbability;
    }

    public NetworkParametersDto setDropoutProbability(final double dropoutProbability) {
        this.dropoutProbability = dropoutProbability;
        return this;
    }
}
