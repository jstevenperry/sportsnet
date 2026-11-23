/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.dto;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public class TournamentSimulationDto implements Serializable {

    private List<UUID> networkIds; // The networks to use for the simulation
    private Integer tournamentYear; // The year of the tournament to simulate

    public List<UUID> getNetworkIds() {
        return networkIds;
    }

    public TournamentSimulationDto setNetworkIds(final List<UUID> networkIds) {
        this.networkIds = networkIds;
        return this;
    }

    public Integer getTournamentYear() {
        return tournamentYear;
    }

    public TournamentSimulationDto setTournamentYear(final Integer tournamentYear) {
        this.tournamentYear = tournamentYear;
        return this;
    }
}
