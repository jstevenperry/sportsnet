/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.model;

import com.jstevenperry.sportsnet.framework.dto.ManagementEventDto;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.UUID;

@SuppressWarnings("unused")
public class ManagementEvent implements Comparable<ManagementEvent> {

    private UUID serviceId;
    private UUID eventId;
    private UUID correlationId;
    private ManagementEventType managementEventType;
    private Long systemTimeMillis;
    private Long elapsedTimeMillis;
    private NetworkDescriptor networkDescriptor;
    private String sport;
    private String hostName;
    private String message;
    private String additionalContext;

    public static ManagementEvent fromDto(final ManagementEventDto managementEventDto) {
        if (managementEventDto != null) {
            return new ManagementEvent()
                    .setServiceId(managementEventDto.getServiceId())
                    .setEventId(managementEventDto.getEventId())
                    .setCorrelationId(managementEventDto.getCorrelationId())
                    .setManagementEventType(managementEventDto.getManagementEventType())
                    .setSystemTimeMillis(managementEventDto.getSystemTimeMillis())
                    .setElapsedTimeMillis(managementEventDto.getElapsedTimeMillis())
                    .setNetworkDescriptor((managementEventDto.getNetworkDescriptor() != null) ? NetworkDescriptor.fromDto(managementEventDto.getNetworkDescriptor()) : null)
                    .setSport(managementEventDto.getSport())
                    .setHostName(managementEventDto.getHostName())
                    .setMessage(managementEventDto.getMessage())
                    .setAdditionalContext(managementEventDto.getAdditionalContext());
        } else {
            return new ManagementEvent();
        }
    }

    public ManagementEventDto toDto() {
        return new ManagementEventDto()
                .setServiceId(getServiceId())
                .setEventId(getEventId())
                .setCorrelationId(getCorrelationId())
                .setManagementEventType(getManagementEventType())
                .setSystemTimeMillis(getSystemTimeMillis())
                .setElapsedTimeMillis(getElapsedTimeMillis())
                .setNetworkDescriptor((getNetworkDescriptor() != null) ? getNetworkDescriptor().toDto() : null)
                .setSport(getSport())
                .setHostName(getHostName())
                .setMessage(getMessage())
                .setAdditionalContext(getAdditionalContext());
    }

    public ManagementEvent copy() {
        return new ManagementEvent()
                .setServiceId(getServiceId())
                .setEventId(getEventId())
                .setCorrelationId(getCorrelationId())
                .setManagementEventType(getManagementEventType())
                .setSystemTimeMillis(getSystemTimeMillis())
                .setElapsedTimeMillis(getElapsedTimeMillis())
                .setNetworkDescriptor(getNetworkDescriptor())
                .setSport(getSport())
                .setHostName(getHostName())
                .setMessage(getMessage())
                .setAdditionalContext(getAdditionalContext());
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public ManagementEvent setServiceId(final UUID serviceId) {
        this.serviceId = serviceId;
        return this;
    }

    public UUID getEventId() {
        return eventId;
    }

    public ManagementEvent setEventId(final UUID eventId) {
        this.eventId = eventId;
        return this;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public ManagementEvent setCorrelationId(final UUID correlationId) {
        this.correlationId = correlationId;
        return this;
    }

    public ManagementEventType getManagementEventType() {
        return managementEventType;
    }

    public ManagementEvent setManagementEventType(final ManagementEventType managementEventType) {
        this.managementEventType = managementEventType;
        return this;
    }

    public Long getSystemTimeMillis() {
        return systemTimeMillis;
    }

    public ManagementEvent setSystemTimeMillis(final Long systemTimeMillis) {
        this.systemTimeMillis = systemTimeMillis;
        return this;
    }

    public Long getElapsedTimeMillis() {
        return elapsedTimeMillis;
    }

    public ManagementEvent setElapsedTimeMillis(final Long elapsedTimeMillis) {
        this.elapsedTimeMillis = elapsedTimeMillis;
        return this;
    }

    public NetworkDescriptor getNetworkDescriptor() {
        return networkDescriptor;
    }

    public ManagementEvent setNetworkDescriptor(final NetworkDescriptor networkDescriptor) {
        this.networkDescriptor = networkDescriptor;
        return this;
    }

    public String getSport() {
        return sport;
    }

    public ManagementEvent setSport(final String sport) {
        this.sport = sport;
        return this;
    }

    public String getHostName() {
        return hostName;
    }

    public ManagementEvent setHostName(final String hostName) {
        this.hostName = hostName;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public ManagementEvent setMessage(final String message) {
        this.message = message;
        return this;
    }

    public String getAdditionalContext() {
        return additionalContext;
    }

    public ManagementEvent setAdditionalContext(final String additionalContext) {
        this.additionalContext = additionalContext;
        return this;
    }

    @Override
    public String toString() {
        return "ManagementEvent{" +
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

    @Override
    public int compareTo(final ManagementEvent that) {
        int result = this.getCorrelationId().compareTo(that.getCorrelationId());
        if (result == 0) result = this.getServiceId().compareTo(that.getServiceId());
        if (result == 0) result = this.getSport().compareTo(that.getSport());
        if (result == 0) result = this.getSystemTimeMillis().compareTo(that.getSystemTimeMillis());
        if (result == 0) result = this.getManagementEventType().compareTo(that.getManagementEventType());

        return result;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        final ManagementEvent that = (ManagementEvent) o;

        return new EqualsBuilder().append(serviceId, that.serviceId).append(eventId, that.eventId).append(correlationId, that.correlationId)
                .append(managementEventType, that.managementEventType).append(systemTimeMillis, that.systemTimeMillis).append(elapsedTimeMillis, that.elapsedTimeMillis)
                .append(networkDescriptor, that.networkDescriptor).append(sport, that.sport).append(hostName, that.hostName).append(message, that.message)
                .append(additionalContext, that.additionalContext).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(serviceId).append(eventId).append(correlationId).append(managementEventType).append(systemTimeMillis).append(elapsedTimeMillis)
                .append(networkDescriptor).append(sport).append(hostName).append(message).append(additionalContext).toHashCode();
    }
}
