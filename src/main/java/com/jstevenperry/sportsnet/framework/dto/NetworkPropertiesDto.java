/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.IUpdater;

import java.io.Serializable;
import java.util.Set;

public class NetworkPropertiesDto implements Serializable {

    private Integer numberOfHiddenLayers;
    private Double learningRuleLearningRate;
    private Double maxNetworkError;
    private Double dropOutProbability;
    private Set<Activation> activations;
    private Set<IUpdater> updaters;
    private Integer maxNetworkIterations;
    private Integer epochStatsIndexCount;
    private Double performanceThreshold;

    @JsonCreator
    public NetworkPropertiesDto() {

    }

    public Integer getNumberOfHiddenLayers() {
        return numberOfHiddenLayers;
    }

    public NetworkPropertiesDto setNumberOfHiddenLayers(final Integer numberOfHiddenLayers) {
        this.numberOfHiddenLayers = numberOfHiddenLayers;
        return this;
    }

    public Double getLearningRuleLearningRate() {
        return learningRuleLearningRate;
    }

    public NetworkPropertiesDto setLearningRuleLearningRate(final Double learningRuleLearningRate) {
        this.learningRuleLearningRate = learningRuleLearningRate;
        return this;
    }

    public Double getMaxNetworkError() {
        return maxNetworkError;
    }

    public NetworkPropertiesDto setMaxNetworkError(final Double maxNetworkError) {
        this.maxNetworkError = maxNetworkError;
        return this;
    }

    public Double getDropOutProbability() {
        return dropOutProbability;
    }

    public NetworkPropertiesDto setDropOutProbability(final Double dropOutProbability) {
        this.dropOutProbability = dropOutProbability;
        return this;
    }

    public Set<Activation> getActivations() {
        return activations;
    }

    public NetworkPropertiesDto setActivations(final Set<Activation> activations) {
        this.activations = activations;
        return this;
    }

    public Set<IUpdater> getUpdaters() {
        return updaters;
    }

    public NetworkPropertiesDto setUpdaters(final Set<IUpdater> updaters) {
        this.updaters = updaters;
        return this;
    }

    public Integer getMaxNetworkIterations() {
        return maxNetworkIterations;
    }

    public NetworkPropertiesDto setMaxNetworkIterations(final Integer maxNetworkIterations) {
        this.maxNetworkIterations = maxNetworkIterations;
        return this;
    }

    public Integer getEpochStatsIndexCount() {
        return epochStatsIndexCount;
    }

    public NetworkPropertiesDto setEpochStatsIndexCount(final Integer epochStatsIndexCount) {
        this.epochStatsIndexCount = epochStatsIndexCount;
        return this;
    }

    public Double getPerformanceThreshold() {
        return performanceThreshold;
    }

    public NetworkPropertiesDto setPerformanceThreshold(final Double performanceThreshold) {
        this.performanceThreshold = performanceThreshold;
        return this;
    }
}
