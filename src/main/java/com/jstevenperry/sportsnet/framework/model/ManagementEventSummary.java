/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.model;

import com.jstevenperry.sportsnet.framework.dto.ManagementEventSummaryDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class ManagementEventSummary {
    // The "key" that ties all aggregated stats together
    private UUID correlationId;

    // Other interesting information
    private UUID serviceId;
    private String sport;
    private String hostName;
    private Long numberOfEvents;
    private ManagementEventStatus status;
    private LocalDateTime whenStarted;
    private LocalDateTime whenFinished;

    // Aggregate stats by the above
    // Elapsed time
    private Long totalIterationTime;
    private ManagementEvent slowestBatchIterationTimeEvent;
    private ManagementEvent fastestBatchIterationTimeEvent;
    // Network accuracy and such
    private Double totalNetworkAccuracy;
    private Double totalNetworkPrecision;
    private Double totalNetworkRecall;
    private Double totalF1Score;
    private Double totalRealNetworkAccuracy;
    private ManagementEvent lowestRealNetworkAccuracyEvent;
    private ManagementEvent highestRealNetworkAccuracyEvent;
    private ManagementEvent lowestNetworkAccuracyEvent;
    private ManagementEvent highestNetworkAccuracyEvent;
    private ManagementEvent lowestNetworkPrecisionEvent;
    private ManagementEvent highestNetworkPrecisionEvent;
    private ManagementEvent lowestNetworkRecallEvent;
    private ManagementEvent highestNetworkRecallEvent;
    private ManagementEvent lowestF1ScoreEvent;
    private ManagementEvent highestF1ScoreEvent;
    // Number of networks saved for this batch run
    private Integer numberOfSavedNetworks;

    public ManagementEventSummaryDto toDto() {
        return new ManagementEventSummaryDto()
                .setServiceId(getServiceId())
                .setCorrelationId(getCorrelationId())
                .setSport(getSport())
                .setHostName(getHostName())
                .setNumberOfEvents(getNumberOfEvents())
                .setStatus(getStatus())
                .setWhenStarted((getWhenStarted() != null) ? getWhenStarted().format(DateTimeFormatter.ofPattern(NetworkDescriptor.YYYY_MM_DD_HH_MM_SS)) : null)
                .setWhenFinished((getWhenFinished() != null) ? getWhenFinished().format(DateTimeFormatter.ofPattern(NetworkDescriptor.YYYY_MM_DD_HH_MM_SS)) : null)
                .setTotalBatchIterationTime(getTotalIterationTime())
                .setSlowestBatchIterationTimeEvent(getSlowestBatchIterationTimeEvent() != null ? getSlowestBatchIterationTimeEvent().toDto() : null)
                .setFastestBatchIterationTimeEvent(getFastestBatchIterationTimeEvent() != null ? getFastestBatchIterationTimeEvent().toDto() : null)
                .setTotalNetworkAccuracy(getTotalNetworkAccuracy())
                .setLowestNetworkAccuracyEvent(getLowestNetworkAccuracyEvent() != null ? getLowestNetworkAccuracyEvent().toDto() : null)
                .setHighestNetworkAccuracyEvent(getHighestNetworkAccuracyEvent() != null ? getHighestNetworkAccuracyEvent().toDto() : null)
                .setTotalNetworkPrecision(getTotalNetworkPrecision())
                .setLowestNetworkPrecisionEvent(getLowestNetworkPrecisionEvent() != null ? getLowestNetworkPrecisionEvent().toDto() : null)
                .setHighestNetworkPrecisionEvent(getHighestNetworkPrecisionEvent() != null ? getHighestNetworkPrecisionEvent().toDto() : null)
                .setTotalNetworkRecall(getTotalNetworkRecall())
                .setLowestNetworkRecallEvent(getLowestNetworkRecallEvent() != null ? getLowestNetworkRecallEvent().toDto() : null)
                .setHighestNetworkRecallEvent(getHighestNetworkRecallEvent() != null ? getHighestNetworkRecallEvent().toDto() : null)
                .setTotalF1Score(getTotalF1Score())
                .setTotalRealNetworkAccuracy(getTotalRealNetworkAccuracy())
                .setLowestF1ScoreEvent(getLowestF1ScoreEvent() != null ? getLowestF1ScoreEvent().toDto() : null)
                .setHighestF1ScoreEvent(getHighestF1ScoreEvent() != null ? getHighestF1ScoreEvent().toDto() : null)
                .setLowestRealNetworkAccuracyEvent(getLowestRealNetworkAccuracyEvent() != null ? getLowestRealNetworkAccuracyEvent().toDto() : null)
                .setHighestRealNetworkAccuracyEvent(getHighestRealNetworkAccuracyEvent() != null ? getHighestRealNetworkAccuracyEvent().toDto() : null)
                .setNumberOfSavedNetworks(getNumberOfSavedNetworks())
                ;
    }

    public static ManagementEventSummary fromDto(final ManagementEventSummaryDto dto) {
        return new ManagementEventSummary()
                .setServiceId(dto.getServiceId())
                .setCorrelationId(dto.getCorrelationId())
                .setSport(dto.getSport())
                .setHostName(dto.getHostName())
                .setNumberOfEvents(dto.getNumberOfEvents())
                .setStatus(dto.getStatus())
                .setWhenStarted((dto.getWhenStarted() != null) ? LocalDateTime.parse(dto.getWhenStarted(), DateTimeFormatter.ofPattern(NetworkDescriptor.YYYY_MM_DD_HH_MM_SS)) : null)
                .setWhenFinished((dto.getWhenFinished() != null) ? LocalDateTime.parse(dto.getWhenFinished(), DateTimeFormatter.ofPattern(NetworkDescriptor.YYYY_MM_DD_HH_MM_SS)) : null)
                .setTotalIterationTime(dto.getTotalBatchIterationTime())
                .setSlowestBatchIterationTimeEvent(ManagementEvent.fromDto(dto.getSlowestBatchIterationTimeEvent()))
                .setFastestBatchIterationTimeEvent(ManagementEvent.fromDto(dto.getFastestBatchIterationTimeEvent()))
                .setTotalNetworkAccuracy(dto.getTotalNetworkAccuracy())
                .setLowestNetworkAccuracyEvent(ManagementEvent.fromDto(dto.getLowestNetworkAccuracyEvent()))
                .setHighestNetworkAccuracyEvent(ManagementEvent.fromDto(dto.getHighestNetworkAccuracyEvent()))
                .setHighestNetworkAccuracyEvent(ManagementEvent.fromDto(dto.getHighestNetworkAccuracyEvent()))
                .setTotalNetworkPrecision(dto.getTotalNetworkPrecision())
                .setLowestNetworkPrecisionEvent(ManagementEvent.fromDto(dto.getLowestNetworkPrecisionEvent()))
                .setHighestNetworkPrecisionEvent(ManagementEvent.fromDto(dto.getHighestNetworkPrecisionEvent()))
                .setTotalNetworkRecall(dto.getTotalNetworkRecall())
                .setLowestNetworkRecallEvent(ManagementEvent.fromDto(dto.getLowestNetworkRecallEvent()))
                .setHighestNetworkRecallEvent(ManagementEvent.fromDto(dto.getHighestNetworkRecallEvent()))
                .setTotalF1Score(dto.getTotalF1Score())
                .setTotalRealNetworkAccuracy(dto.getTotalRealNetworkAccuracy())
                .setLowestF1ScoreEvent(ManagementEvent.fromDto(dto.getLowestF1ScoreEvent()))
                .setHighestF1ScoreEvent(ManagementEvent.fromDto(dto.getHighestF1ScoreEvent()))
                .setLowestRealNetworkAccuracyEvent(ManagementEvent.fromDto(dto.getLowestRealNetworkAccuracyEvent()))
                .setHighestRealNetworkAccuracyEvent(ManagementEvent.fromDto(dto.getHighestRealNetworkAccuracyEvent()))
                .setNumberOfSavedNetworks(dto.getNumberOfSavedNetworks())
                ;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public ManagementEventSummary setServiceId(final UUID serviceId) {
        this.serviceId = serviceId;
        return this;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public ManagementEventSummary setCorrelationId(final UUID correlationId) {
        this.correlationId = correlationId;
        return this;
    }

    public String getSport() {
        return sport;
    }

    public ManagementEventSummary setSport(final String sport) {
        this.sport = sport;
        return this;
    }

    public String getHostName() {
        return hostName;
    }

    public ManagementEventSummary setHostName(final String hostName) {
        this.hostName = hostName;
        return this;
    }

    public Long getNumberOfEvents() {
        return numberOfEvents;
    }

    public ManagementEventSummary setNumberOfEvents(final Long numberOfEvents) {
        this.numberOfEvents = numberOfEvents;
        return this;
    }

    public ManagementEventStatus getStatus() {
        return status;
    }

    public ManagementEventSummary setStatus(final ManagementEventStatus status) {
        this.status = status;
        return this;
    }

    public LocalDateTime getWhenStarted() {
        return whenStarted;
    }

    public ManagementEventSummary setWhenStarted(final LocalDateTime whenStarted) {
        this.whenStarted = whenStarted;
        return this;
    }

    public LocalDateTime getWhenFinished() {
        return whenFinished;
    }

    public ManagementEventSummary setWhenFinished(final LocalDateTime whenFinished) {
        this.whenFinished = whenFinished;
        return this;
    }

    public Long getTotalIterationTime() {
        return totalIterationTime;
    }

    public ManagementEventSummary setTotalIterationTime(final Long totalIterationTime) {
        this.totalIterationTime = totalIterationTime;
        return this;
    }

    public ManagementEvent getSlowestBatchIterationTimeEvent() {
        return slowestBatchIterationTimeEvent;
    }

    public ManagementEventSummary setSlowestBatchIterationTimeEvent(final ManagementEvent slowestBatchIterationTimeEvent) {
        this.slowestBatchIterationTimeEvent = slowestBatchIterationTimeEvent;
        return this;
    }

    public ManagementEvent getFastestBatchIterationTimeEvent() {
        return fastestBatchIterationTimeEvent;
    }

    public ManagementEventSummary setFastestBatchIterationTimeEvent(final ManagementEvent fastestBatchIterationTimeEvent) {
        this.fastestBatchIterationTimeEvent = fastestBatchIterationTimeEvent;
        return this;
    }

    public Double getTotalNetworkAccuracy() {
        return totalNetworkAccuracy;
    }

    public ManagementEventSummary setTotalNetworkAccuracy(final Double totalNetworkAccuracy) {
        this.totalNetworkAccuracy = totalNetworkAccuracy;
        return this;
    }

    public Double getTotalNetworkPrecision() {
        return totalNetworkPrecision;
    }

    public ManagementEventSummary setTotalNetworkPrecision(final Double totalNetworkPrecision) {
        this.totalNetworkPrecision = totalNetworkPrecision;
        return this;
    }

    public Double getTotalNetworkRecall() {
        return totalNetworkRecall;
    }

    public ManagementEventSummary setTotalNetworkRecall(final Double totalNetworkRecall) {
        this.totalNetworkRecall = totalNetworkRecall;
        return this;
    }

    public Double getTotalF1Score() {
        return totalF1Score;
    }

    public ManagementEventSummary setTotalF1Score(final Double totalF1Score) {
        this.totalF1Score = totalF1Score;
        return this;
    }

    public Double getTotalRealNetworkAccuracy() {
        return totalRealNetworkAccuracy;
    }

    public ManagementEventSummary setTotalRealNetworkAccuracy(final Double totalRealNetworkAccuracy) {
        this.totalRealNetworkAccuracy = totalRealNetworkAccuracy;
        return this;
    }

    public ManagementEvent getLowestRealNetworkAccuracyEvent() {
        return lowestRealNetworkAccuracyEvent;
    }

    public ManagementEventSummary setLowestRealNetworkAccuracyEvent(final ManagementEvent lowestRealNetworkAccuracyEvent) {
        this.lowestRealNetworkAccuracyEvent = lowestRealNetworkAccuracyEvent;
        return this;
    }

    public ManagementEvent getHighestRealNetworkAccuracyEvent() {
        return highestRealNetworkAccuracyEvent;
    }

    public ManagementEventSummary setHighestRealNetworkAccuracyEvent(final ManagementEvent highestRealNetworkAccuracyEvent) {
        this.highestRealNetworkAccuracyEvent = highestRealNetworkAccuracyEvent;
        return this;
    }

    public ManagementEvent getLowestNetworkAccuracyEvent() {
        return lowestNetworkAccuracyEvent;
    }

    public ManagementEventSummary setLowestNetworkAccuracyEvent(final ManagementEvent lowestNetworkAccuracyEvent) {
        this.lowestNetworkAccuracyEvent = lowestNetworkAccuracyEvent;
        return this;
    }

    public ManagementEvent getHighestNetworkAccuracyEvent() {
        return highestNetworkAccuracyEvent;
    }

    public ManagementEventSummary setHighestNetworkAccuracyEvent(final ManagementEvent highestNetworkAccuracyEvent) {
        this.highestNetworkAccuracyEvent = highestNetworkAccuracyEvent;
        return this;
    }

    public ManagementEvent getLowestNetworkPrecisionEvent() {
        return lowestNetworkPrecisionEvent;
    }

    public ManagementEventSummary setLowestNetworkPrecisionEvent(final ManagementEvent lowestNetworkPrecisionEvent) {
        this.lowestNetworkPrecisionEvent = lowestNetworkPrecisionEvent;
        return this;
    }

    public ManagementEvent getHighestNetworkPrecisionEvent() {
        return highestNetworkPrecisionEvent;
    }

    public ManagementEventSummary setHighestNetworkPrecisionEvent(final ManagementEvent highestNetworkPrecisionEvent) {
        this.highestNetworkPrecisionEvent = highestNetworkPrecisionEvent;
        return this;
    }

    public ManagementEvent getLowestNetworkRecallEvent() {
        return lowestNetworkRecallEvent;
    }

    public ManagementEventSummary setLowestNetworkRecallEvent(final ManagementEvent lowestNetworkRecallEvent) {
        this.lowestNetworkRecallEvent = lowestNetworkRecallEvent;
        return this;
    }

    public ManagementEvent getHighestNetworkRecallEvent() {
        return highestNetworkRecallEvent;
    }

    public ManagementEventSummary setHighestNetworkRecallEvent(final ManagementEvent highestNetworkRecallEvent) {
        this.highestNetworkRecallEvent = highestNetworkRecallEvent;
        return this;
    }

    public ManagementEvent getLowestF1ScoreEvent() {
        return lowestF1ScoreEvent;
    }

    public ManagementEventSummary setLowestF1ScoreEvent(final ManagementEvent lowestF1ScoreEvent) {
        this.lowestF1ScoreEvent = lowestF1ScoreEvent;
        return this;
    }

    public ManagementEvent getHighestF1ScoreEvent() {
        return highestF1ScoreEvent;
    }

    public ManagementEventSummary setHighestF1ScoreEvent(final ManagementEvent highestF1ScoreEvent) {
        this.highestF1ScoreEvent = highestF1ScoreEvent;
        return this;
    }

    public Integer getNumberOfSavedNetworks() {
        return numberOfSavedNetworks;
    }

    public ManagementEventSummary setNumberOfSavedNetworks(final Integer numberOfSavedNetworks) {
        this.numberOfSavedNetworks = numberOfSavedNetworks;
        return this;
    }

    /**
     * Computed attributes
     */
    public Double getAverageBatchIterationTime() {
        return (getTotalIterationTime() != null && getTotalIterationTime() > 0) ? (double) getTotalIterationTime() / getNumberOfEvents() : 0.0;
    }

    public Double getAverageNetworkAccuracy() {
        return (getTotalNetworkAccuracy() != null && getTotalNetworkAccuracy() > 0) ? (double) getTotalNetworkAccuracy() / getNumberOfEvents() : 0.0;
    }

    public Double getAverageNetworkPrecision() {
        return (getTotalNetworkPrecision() != null && getTotalNetworkPrecision() > 0) ? (double) getTotalNetworkPrecision() / getNumberOfEvents() : 0.0;
    }

    public Double getAverageNetworkRecall() {
        return (getTotalNetworkRecall() != null && getTotalNetworkRecall() > 0) ? (double) getTotalNetworkRecall() / getNumberOfEvents() : 0.0;
    }

    public Double getAverageF1Score() {
        return (getTotalF1Score() != null && getTotalF1Score() > 0) ? (double) getTotalF1Score() / getNumberOfEvents() : 0.0;
    }

    public Double getAverageRealNetworkAccuracy() {
        return (getTotalRealNetworkAccuracy() != null && getTotalRealNetworkAccuracy() > 0) ? (double)getTotalRealNetworkAccuracy() / getNumberOfEvents() : 0.0;
    }

}
