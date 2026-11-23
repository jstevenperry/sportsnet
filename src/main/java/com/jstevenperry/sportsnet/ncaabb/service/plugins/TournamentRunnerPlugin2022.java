package com.jstevenperry.sportsnet.ncaabb.service.plugins;

import com.jstevenperry.sportsnet.framework.model.NetworkRunnerPluginOutputType;
import com.jstevenperry.sportsnet.framework.model.TeamCoordinate;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.ncaabb.model.NcaaBbSeasonData;

import java.util.Map;

public class TournamentRunnerPlugin2022 extends NcaaBbTournamentRunnerPlugin {
    public TournamentRunnerPlugin2022(SeasonDataDao<NcaaBbSeasonData> seasonDataDao) {
        super(seasonDataDao);
    }

    @Override
    public Integer getTournamentYear() {
        return 2022;
    }

    @Override
    public String getTournamentYearForDisplay() {
        return "2022-23 (Rankings)";
    }

    @Override
    public Map<TeamCoordinate, NcaaBbSeasonData> buildTeamCoordinateSeasonDataMap() {
        throw new UnsupportedOperationException("buildTeamCoordinateSeasonDataMap(): NOT SUPPORTED FOR THIS OUTPUT TYPE");
    }

    @Override
    public NetworkRunnerPluginOutputType getOutputType() {
        return NetworkRunnerPluginOutputType.TEAM_RANKING;
    }

}
