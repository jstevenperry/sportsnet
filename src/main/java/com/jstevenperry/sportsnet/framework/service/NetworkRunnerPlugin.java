/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.service;

import com.jstevenperry.sportsnet.framework.model.NetworkRunnerPluginOutputType;
import com.jstevenperry.sportsnet.framework.model.SeasonData;
import com.jstevenperry.sportsnet.framework.model.TeamCoordinate;

import java.util.Map;

public interface NetworkRunnerPlugin<T extends SeasonData> {

    Integer getTournamentYear();

    String getTournamentYearForDisplay();

    String getTournamentResultsTemplateFileName();

    String getNetworkType();

    String getTournamentTemplateFileName();

    Map<TeamCoordinate, T> buildTeamCoordinateSeasonDataMap();

    default NetworkRunnerPluginOutputType getOutputType() {
        return NetworkRunnerPluginOutputType.BRACKET;
    }

}
