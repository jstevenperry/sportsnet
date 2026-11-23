/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.dto;

import com.jstevenperry.sportsnet.framework.model.NetworkRunnerPluginOutputType;

public class NetworkRunnerPluginDto {

    private String pluginDescription;
    private Integer tournamentYear;
    private NetworkRunnerPluginOutputType outputType;

    public String getPluginDescription() {
        return pluginDescription;
    }

    public NetworkRunnerPluginDto setPluginDescription(final String pluginDescription) {
        this.pluginDescription = pluginDescription;
        return this;
    }

    public Integer getTournamentYear() {
        return tournamentYear;
    }

    public NetworkRunnerPluginDto setTournamentYear(final Integer tournamentYear) {
        this.tournamentYear = tournamentYear;
        return this;
    }

    public NetworkRunnerPluginOutputType getOutputType() {
        return outputType;
    }

    public NetworkRunnerPluginDto setOutputType(final NetworkRunnerPluginOutputType outputType) {
        this.outputType = outputType;
        return this;
    }

    @Override
    public String toString() {
        return "NetworkRunnerPluginDto{" +
                "pluginDescription='" + pluginDescription + '\'' +
                ", tournamentYear=" + tournamentYear +
                ", outputType=" + outputType +
                '}';
    }
}
