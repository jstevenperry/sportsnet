/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.dto;

import com.jstevenperry.sportsnet.framework.model.ManagementEventStatus;
import com.jstevenperry.sportsnet.framework.util.AbstractNetworkUtils;

import java.util.UUID;

public class ManagementEventSummaryDto {
    // 4 part Key
    private UUID serviceId;
    private UUID correlationId;
    private String sport;
    private String hostName;
    private Long numberOfEvents;
    private Integer numberOfSavedNetworks;
    private ManagementEventStatus status;
    private String whenStarted;
    private String whenFinished;

    // Aggregate stats by the above
    private ManagementEventDto highestRealNetworkAccuracyEvent;
    private ManagementEventDto lowestRealNetworkAccuracyEvent;
    private ManagementEventDto highestNetworkAccuracyEvent;
    private ManagementEventDto lowestNetworkAccuracyEvent;
    private ManagementEventDto highestNetworkPrecisionEvent;
    private ManagementEventDto lowestNetworkPrecisionEvent;
    private ManagementEventDto highestNetworkRecallEvent;
    private ManagementEventDto lowestNetworkRecallEvent;
    private ManagementEventDto highestF1ScoreEvent;
    private ManagementEventDto lowestF1ScoreEvent;
    private ManagementEventDto slowestBatchIterationTimeEvent;
    private ManagementEventDto fastestBatchIterationTimeEvent;
    // Network accuracy and such
    // Elapsed time
    private Long totalBatchIterationTime;
    private Double totalNetworkAccuracy;
    private Double totalNetworkPrecision;
    private Double totalNetworkRecall;
    private Double totalF1Score;
    private Double totalRealNetworkAccuracy;

    public UUID getServiceId() {
        return serviceId;
    }

    public ManagementEventSummaryDto setServiceId(final UUID serviceId) {
        this.serviceId = serviceId;
        return this;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public ManagementEventSummaryDto setCorrelationId(final UUID correlationId) {
        this.correlationId = correlationId;
        return this;
    }

    public String getSport() {
        return sport;
    }

    public ManagementEventSummaryDto setSport(final String sport) {
        this.sport = sport;
        return this;
    }

    public String getHostName() {
        return hostName;
    }

    public ManagementEventSummaryDto setHostName(final String hostName) {
        this.hostName = hostName;
        return this;
    }

    public Long getNumberOfEvents() {
        return numberOfEvents;
    }

    public ManagementEventSummaryDto setNumberOfEvents(final Long numberOfEvents) {
        this.numberOfEvents = numberOfEvents;
        return this;
    }

    public Integer getNumberOfSavedNetworks() {
        return numberOfSavedNetworks;
    }

    public ManagementEventSummaryDto setNumberOfSavedNetworks(final Integer numberOfSavedNetworks) {
        this.numberOfSavedNetworks = numberOfSavedNetworks;
        return this;
    }

    public ManagementEventStatus getStatus() {
        return status;
    }

    public ManagementEventSummaryDto setStatus(final ManagementEventStatus status) {
        this.status = status;
        return this;
    }

    public String getWhenStarted() {
        return whenStarted;
    }

    public ManagementEventSummaryDto setWhenStarted(final String whenStarted) {
        this.whenStarted = whenStarted;
        return this;
    }

    public String getWhenFinished() {
        return whenFinished;
    }

    public ManagementEventSummaryDto setWhenFinished(final String whenFinished) {
        this.whenFinished = whenFinished;
        return this;
    }

    /*
     * BEGIN Computed attributes
     */
    public Double getAverageBatchIterationTime() {
        return (getTotalBatchIterationTime() != null && getTotalBatchIterationTime() > 0) ? (double) getTotalBatchIterationTime() / getNumberOfEvents() : 0.0;
    }

    // BS setter just so these computed properties unmarshal correctly
    public void setAverageBatchIterationTime(final Double value) {}

    public Double getAverageNetworkAccuracy() {
        return (getTotalNetworkAccuracy() != null && getTotalNetworkAccuracy() > 0) ? (double) getTotalNetworkAccuracy() / getNumberOfEvents() : 0.0;
    }

    // BS setter just so these computed properties unmarshal correctly
    public void setAverageNetworkAccuracy(final Double value) {}

    public Double getAverageNetworkPrecision() {
        return (getTotalNetworkPrecision() != null && getTotalNetworkPrecision() > 0) ? (double) getTotalNetworkPrecision() / getNumberOfEvents() : 0.0;
    }

    // BS setter just so these computed properties unmarshal correctly
    public void setAverageNetworkPrecision(final Double value) {}

    public Double getAverageNetworkRecall() {
        return (getTotalNetworkRecall() != null && getTotalNetworkRecall() > 0) ? (double) getTotalNetworkRecall() / getNumberOfEvents() : 0.0;
    }

    // BS setter just so these computed properties unmarshal correctly
    public void setAverageNetworkRecall(final Double value) {}

    public Double getAverageF1Score() {
        return (getTotalF1Score() != null && getTotalF1Score() > 0) ? (double) getTotalF1Score() / getNumberOfEvents() : 0.0;
    }

    // BS setter just so these computed properties unmarshal correctly
    public void setAverageF1Score(final Double value) {}

    public Double getAverageRealAccuracy() {
        return AbstractNetworkUtils.computeNetworkAccuracyScore(
                getAverageNetworkAccuracy(),
                getAverageNetworkPrecision(),
                getAverageNetworkRecall(),
                getAverageF1Score()
        );
    }

    // BS setter just so these computed properties unmarshal correctly
    public void setAverageRealAccuracy(final Double value) {}
    /*
     * END Computed attributes
     */

    public ManagementEventDto getHighestRealNetworkAccuracyEvent() {
        return highestRealNetworkAccuracyEvent;
    }

    public ManagementEventSummaryDto setHighestRealNetworkAccuracyEvent(final ManagementEventDto highestRealNetworkAccuracyEvent) {
        this.highestRealNetworkAccuracyEvent = highestRealNetworkAccuracyEvent;
        return this;
    }

    public ManagementEventDto getLowestRealNetworkAccuracyEvent() {
        return lowestRealNetworkAccuracyEvent;
    }

    public ManagementEventSummaryDto setLowestRealNetworkAccuracyEvent(final ManagementEventDto lowestRealNetworkAccuracyEvent) {
        this.lowestRealNetworkAccuracyEvent = lowestRealNetworkAccuracyEvent;
        return this;
    }

    public ManagementEventDto getHighestNetworkAccuracyEvent() {
        return highestNetworkAccuracyEvent;
    }

    public ManagementEventSummaryDto setHighestNetworkAccuracyEvent(final ManagementEventDto highestNetworkAccuracyEvent) {
        this.highestNetworkAccuracyEvent = highestNetworkAccuracyEvent;
        return this;
    }

    public ManagementEventDto getLowestNetworkAccuracyEvent() {
        return lowestNetworkAccuracyEvent;
    }

    public ManagementEventSummaryDto setLowestNetworkAccuracyEvent(final ManagementEventDto lowestNetworkAccuracyEvent) {
        this.lowestNetworkAccuracyEvent = lowestNetworkAccuracyEvent;
        return this;
    }

    public ManagementEventDto getHighestNetworkPrecisionEvent() {
        return highestNetworkPrecisionEvent;
    }

    public ManagementEventSummaryDto setHighestNetworkPrecisionEvent(final ManagementEventDto highestNetworkPrecisionEvent) {
        this.highestNetworkPrecisionEvent = highestNetworkPrecisionEvent;
        return this;
    }

    public ManagementEventDto getLowestNetworkPrecisionEvent() {
        return lowestNetworkPrecisionEvent;
    }

    public ManagementEventSummaryDto setLowestNetworkPrecisionEvent(final ManagementEventDto lowestNetworkPrecisionEvent) {
        this.lowestNetworkPrecisionEvent = lowestNetworkPrecisionEvent;
        return this;
    }

    public ManagementEventDto getHighestNetworkRecallEvent() {
        return highestNetworkRecallEvent;
    }

    public ManagementEventSummaryDto setHighestNetworkRecallEvent(final ManagementEventDto highestNetworkRecallEvent) {
        this.highestNetworkRecallEvent = highestNetworkRecallEvent;
        return this;
    }

    public ManagementEventDto getLowestNetworkRecallEvent() {
        return lowestNetworkRecallEvent;
    }

    public ManagementEventSummaryDto setLowestNetworkRecallEvent(final ManagementEventDto lowestNetworkRecallEvent) {
        this.lowestNetworkRecallEvent = lowestNetworkRecallEvent;
        return this;
    }

    public ManagementEventDto getHighestF1ScoreEvent() {
        return highestF1ScoreEvent;
    }

    public ManagementEventSummaryDto setHighestF1ScoreEvent(final ManagementEventDto highestF1ScoreEvent) {
        this.highestF1ScoreEvent = highestF1ScoreEvent;
        return this;
    }

    public ManagementEventDto getLowestF1ScoreEvent() {
        return lowestF1ScoreEvent;
    }

    public ManagementEventSummaryDto setLowestF1ScoreEvent(final ManagementEventDto lowestF1ScoreEvent) {
        this.lowestF1ScoreEvent = lowestF1ScoreEvent;
        return this;
    }

    public ManagementEventDto getSlowestBatchIterationTimeEvent() {
        return slowestBatchIterationTimeEvent;
    }

    public ManagementEventSummaryDto setSlowestBatchIterationTimeEvent(final ManagementEventDto slowestBatchIterationTimeEvent) {
        this.slowestBatchIterationTimeEvent = slowestBatchIterationTimeEvent;
        return this;
    }

    public Long getTotalBatchIterationTime() {
        return totalBatchIterationTime;
    }

    public ManagementEventSummaryDto setTotalBatchIterationTime(final Long totalBatchIterationTime) {
        this.totalBatchIterationTime = totalBatchIterationTime;
        return this;
    }

    public ManagementEventDto getFastestBatchIterationTimeEvent() {
        return fastestBatchIterationTimeEvent;
    }

    public ManagementEventSummaryDto setFastestBatchIterationTimeEvent(final ManagementEventDto fastestBatchIterationTimeEvent) {
        this.fastestBatchIterationTimeEvent = fastestBatchIterationTimeEvent;
        return this;
    }

    public Double getTotalNetworkAccuracy() {
        return totalNetworkAccuracy;
    }

    public ManagementEventSummaryDto setTotalNetworkAccuracy(final Double totalNetworkAccuracy) {
        this.totalNetworkAccuracy = totalNetworkAccuracy;
        return this;
    }

    public Double getTotalNetworkPrecision() {
        return totalNetworkPrecision;
    }

    public ManagementEventSummaryDto setTotalNetworkPrecision(final Double totalNetworkPrecision) {
        this.totalNetworkPrecision = totalNetworkPrecision;
        return this;
    }

    public Double getTotalNetworkRecall() {
        return totalNetworkRecall;
    }

    public ManagementEventSummaryDto setTotalNetworkRecall(final Double totalNetworkRecall) {
        this.totalNetworkRecall = totalNetworkRecall;
        return this;
    }

    public Double getTotalF1Score() {
        return totalF1Score;
    }

    public ManagementEventSummaryDto setTotalF1Score(final Double totalF1Score) {
        this.totalF1Score = totalF1Score;
        return this;
    }

    public Double getTotalRealNetworkAccuracy() {
        return totalRealNetworkAccuracy;
    }

    public ManagementEventSummaryDto setTotalRealNetworkAccuracy(final Double totalRealNetworkAccuracy) {
        this.totalRealNetworkAccuracy = totalRealNetworkAccuracy;
        return this;
    }

}
