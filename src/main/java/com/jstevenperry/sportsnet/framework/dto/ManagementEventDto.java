/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.jstevenperry.sportsnet.framework.model.ManagementEventType;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * DTO representing a management event. This is a sort of "catch-all" DTO
 * that represents all of the possible management events.
 */
public class ManagementEventDto {

    @JsonPropertyDescription("The UUID of the service that applies to this event, null if not applicable.")
    private UUID serviceId;
    @JsonPropertyDescription("The UUID of the event, acts as the key, every event is unique.")
    private UUID eventId;
    @JsonPropertyDescription("The UUID used to associate events, events with the same correlationId are related.")
    private UUID correlationId;
    @JsonPropertyDescription("The type of event, see ManagementEventType for a list of values.")
    private ManagementEventType managementEventType;
    @JsonPropertyDescription("The system timestamp when the event was created, if empty will be filled in by the management service.")
    private Long systemTimeMillis;
    @JsonPropertyDescription("The elapsed time for the event from start to end, will be null if not applicable.")
    private Long elapsedTimeMillis;
    @JsonPropertyDescription("Contains information about the neural network involved in the event, will be null if not applicable.")
    private NetworkDescriptorDto networkDescriptor;
    @JsonPropertyDescription("The sport, such as nba, nfl, ncaabb, nhl, etc.")
    private String sport;
    @JsonPropertyDescription("The host where the registrant is running.")
    private String hostName;
    @JsonPropertyDescription("Optional message sent by the registrant, may be empty or null.")
    private String message;
    @JsonPropertyDescription("Optional context to describe the event, sent by the registrant, may be decorated by the management service at its discretion.")
    private String additionalContext;

    public UUID getServiceId() {
        return serviceId;
    }

    public ManagementEventDto setServiceId(final UUID serviceId) {
        this.serviceId = serviceId;
        return this;
    }

    public UUID getEventId() {
        return eventId;
    }

    public ManagementEventDto setEventId(final UUID eventId) {
        this.eventId = eventId;
        return this;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public ManagementEventDto setCorrelationId(final UUID correlationId) {
        this.correlationId = correlationId;
        return this;
    }

    public Long getElapsedTimeMillis() {
        return elapsedTimeMillis;
    }

    public ManagementEventDto setElapsedTimeMillis(final Long elapsedTimeMillis) {
        this.elapsedTimeMillis = elapsedTimeMillis;
        return this;
    }

    public ManagementEventType getManagementEventType() {
        return managementEventType;
    }

    public ManagementEventDto setManagementEventType(final ManagementEventType managementEventType) {
        this.managementEventType = managementEventType;
        return this;
    }

    public Long getSystemTimeMillis() {
        return systemTimeMillis;
    }

    public ManagementEventDto setSystemTimeMillis(final Long systemTimeMillis) {
        this.systemTimeMillis = systemTimeMillis;
        return this;
    }

    public NetworkDescriptorDto getNetworkDescriptor() {
        return networkDescriptor;
    }

    public ManagementEventDto setNetworkDescriptor(final NetworkDescriptorDto networkDescriptor) {
        this.networkDescriptor = networkDescriptor;
        return this;
    }

    public String getSport() {
        return sport;
    }

    public ManagementEventDto setSport(final String sport) {
        this.sport = sport;
        return this;
    }

    public String getHostName() {
        return hostName;
    }

    public ManagementEventDto setHostName(final String hostName) {
        this.hostName = hostName;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public ManagementEventDto setMessage(final String message) {
        this.message = message;
        return this;
    }

    public String getAdditionalContext() {
        return additionalContext;
    }

    public ManagementEventDto setAdditionalContext(final String additionalContext) {
        this.additionalContext = additionalContext;
        return this;
    }

    public String getSystemTimestamp() {
        final String systemTimestamp;
        if (getSystemTimeMillis() != null) {
            systemTimestamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(getSystemTimeMillis()), ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss"));
        } else {
            systemTimestamp = StringUtils.EMPTY;
        }
        return systemTimestamp;
    }

    public void setSystemTimestamp(final String systemTimestamp) {
        // Bogus setter to make marshalling happy
    }

    @Override
    public String toString() {
        return "ManagementEventDto{" +
                "serviceId=" + serviceId +
                ", eventId=" + eventId +
                ", correlationId=" + correlationId +
                ", managementEventType=" + managementEventType +
                ", systemTimeMillis=" + systemTimeMillis +
                ", elapsedTimeMillis=" + elapsedTimeMillis +
                ", networkDescriptor=" + networkDescriptor +
                ", sport='" + sport + '\'' +
                ", hostName='" + hostName + '\'' +
                ", message='" + message + '\'' +
                ", additionalContext='" + additionalContext + '\'' +
                '}';
    }
}
