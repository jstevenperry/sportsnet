/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.config;

import org.springframework.beans.factory.annotation.Value;

/**
 * Example configuration for a sportsnet client.
 */
public abstract class AbstractSportsnetProperties {

    @Value("${sportsnet.service.uri.networks}")
    private String sportsnetServiceUriNetworks;

    @Value("${sportsnet.service.uri.tournamentYears}")
    private String sportsnetServiceUriTournamentYears;

    @Value("${sportsnet.service.uri.dataElements}")
    private String sportsnetServiceUriDataElements;

    @Value("${sportsnet.service.uri.simulations}")
    private String sportsnetServiceUriSimulations;

    @Value("${sportsnet.service.uri.trainingYears}")
    private String sportsnetServiceUriTrainingYears;

    @Value("${sportsnet.service.uri.events}")
    private String sportsnetServiceUriEvents;

    @Value("${sportsnet.service.uri.stats}")
    private String sportsnetServiceUriStats;

    @Value("${sportsnet.service.uri.searchEvents}")
    private String sportsnetServiceUriSearchEvents;

    @Value("${sportsnet.resource.service.protocol}")
    private String sportsnetResourceServiceProtocol;

    @Value("${sportsnet.resource.service.host}")
    private String sportsnetResourceServiceHost;

    @Value("${sportsnet.resource.service.port}")
    private Integer sportsnetResourceServicePort;

    @Value("${sportsnet.resource.service.uri}")
    private String sportsnetResourceServiceUri;

    @Value("${sportsnet.management.service.protocol}")
    private String sportsnetManagementServiceProtocol;

    @Value("${sportsnet.management.service.host}")
    private String sportsnetManagementServiceHost;

    @Value("${sportsnet.management.service.port}")
    private Integer sportsnetManagementServicePort;

    @Value("${sportsnet.management.service.uri}")
    private String sportsnetManagementServiceUri;

    public String getSportsnetServiceUriTournamentYears() {
        return sportsnetServiceUriTournamentYears;
    }

    public String getSportsnetServiceUriNetworks() {
        return sportsnetServiceUriNetworks;
    }

    public String getSportsnetServiceUriDataElements() {
        return sportsnetServiceUriDataElements;
    }

    public String getSportsnetServiceUriSimulations() {
        return sportsnetServiceUriSimulations;
    }

    public String getSportsnetServiceUriTrainingYears() {
        return sportsnetServiceUriTrainingYears;
    }

    public String getSportsnetServiceUriEvents() {
        return sportsnetServiceUriEvents;
    }

    public String getSportsnetServiceUriStats() {
        return sportsnetServiceUriStats;
    }

    public String getSportsnetServiceUriSearchEvents() {
        return sportsnetServiceUriSearchEvents;
    }

    public String getSportsnetResourceServiceProtocol() {
        return sportsnetResourceServiceProtocol;
    }

    public String getSportsnetResourceServiceHost() {
        return sportsnetResourceServiceHost;
    }

    public Integer getSportsnetResourceServicePort() {
        return sportsnetResourceServicePort;
    }

    public String getSportsnetResourceServiceUri() {
        return sportsnetResourceServiceUri;
    }

    public String getSportsnetManagementServiceProtocol() {
        return sportsnetManagementServiceProtocol;
    }

    public String getSportsnetManagementServiceHost() {
        return sportsnetManagementServiceHost;
    }

    public Integer getSportsnetManagementServicePort() {
        return sportsnetManagementServicePort;
    }

    public String getSportsnetManagementServiceUri() {
        return sportsnetManagementServiceUri;
    }

    //******************************
    //* SPORTS-SPECIFIC PROPERTIES *
    //******************************
}
