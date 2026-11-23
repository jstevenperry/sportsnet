package com.jstevenperry.sportsnet.ncaabb.service.plugins;

import com.jstevenperry.sportsnet.framework.model.NetworkRunnerPluginOutputType;
import com.jstevenperry.sportsnet.framework.model.TeamCoordinate;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.ncaabb.model.NcaaBbSeasonData;

import java.util.Map;

public class TournamentRunnerPlugin2023 extends NcaaBbTournamentRunnerPlugin {
    public TournamentRunnerPlugin2023(SeasonDataDao<NcaaBbSeasonData> seasonDataDao) {
        super(seasonDataDao);
    }

    @Override
    public Integer getTournamentYear() {
        return 2023;
    }

    @Override
    public String getTournamentYearForDisplay() {
        return "2023-24 (Ranking)";
    }

    @Override
    public Map<TeamCoordinate, NcaaBbSeasonData> buildTeamCoordinateSeasonDataMap() {
        throw new UnsupportedOperationException("buildTeamCoordinateSeasonDataMap() not supported for this plugin type!");
    }

    @Override
    public NetworkRunnerPluginOutputType getOutputType() {
        return NetworkRunnerPluginOutputType.TEAM_RANKING;
    }
}
