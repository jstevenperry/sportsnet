/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.mlb.service.plugins;

import com.jstevenperry.sportsnet.framework.model.TeamCoordinate;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.framework.service.AbstractNetworkRunnerPlugin;
import com.jstevenperry.sportsnet.mlb.config.MlbNetworkUtils;
import com.jstevenperry.sportsnet.mlb.model.MlbSeasonData;

import java.util.Map;

public abstract class AbstractMlbTournamentRunnerPlugin extends AbstractNetworkRunnerPlugin<MlbSeasonData> {
    public AbstractMlbTournamentRunnerPlugin(final SeasonDataDao<MlbSeasonData> seasonDataDao) {
        super(seasonDataDao);
    }

    @Override
    public String getNetworkType() {
        return MlbNetworkUtils.NETWORK_TYPE;
    }

    //***************************************************************************
    //* Not doing bracket-style tournaments for MLB
    @Override
    public String getTournamentResultsTemplateFileName() {
        return methodNotImplemented("getTournamentResultsTemplateFileName");
    }

    @Override
    public String getTournamentTemplateFileName() {
        return methodNotImplemented("getTournamentResultsTemplateFileName");
    }

    @Override
    public Map<TeamCoordinate, MlbSeasonData> buildTeamCoordinateSeasonDataMap() {
        throw new UnsupportedOperationException("buildTeamCoordinateSeasonDataMap(): Method should be implemented by subclass!");
    }

    private String methodNotImplemented(final String methodName) {
        throw new UnsupportedOperationException(methodName + "Method should be implemented by subclass!");
    }
    //***************************************************************************
}
