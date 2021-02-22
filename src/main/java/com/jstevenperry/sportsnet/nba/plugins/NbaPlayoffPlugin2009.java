/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet.nba.plugins;

import com.jstevenperry.sportsnet.framework.model.TeamCoordinate;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.nba.model.NbaSeasonData;

import java.util.HashMap;
import java.util.Map;

public class NbaPlayoffPlugin2009 extends AbstractNbaTournamentRunnerPlugin {
    public NbaPlayoffPlugin2009(final SeasonDataDao<NbaSeasonData> seasonDataDao) {
        super(seasonDataDao);
    }

    @Override
    public Integer getTournamentYear() {
        return 2009;
    }

    @Override
    public String getTournamentYearForDisplay() {
        return "2009-10";
    }

    @Override
    protected Map<TeamCoordinate, NbaSeasonData> buildTeamCoordinateMap() {
        Map<TeamCoordinate, NbaSeasonData> ret = new HashMap<>();
        //
        // Add team coordinates
        // Region 0
        createTeamCoordinateMapping(ret, 0, 1, 0, "Cleveland Cavaliers");
        createTeamCoordinateMapping(ret, 0, 1, 1, "Chicago Bulls");
        createTeamCoordinateMapping(ret, 0, 1, 2, "Boston Celtics");
        createTeamCoordinateMapping(ret, 0, 1, 3, "Miami Heat");
        // Region 1
        createTeamCoordinateMapping(ret, 1, 1, 0, "Atlanta Hawks");
        createTeamCoordinateMapping(ret, 1, 1, 1, "Milwaukee Bucks");
        createTeamCoordinateMapping(ret, 1, 1, 2, "Orlando Magic");
        createTeamCoordinateMapping(ret, 1, 1, 3, "Charlotte Bobcats");
        // Region 2
        createTeamCoordinateMapping(ret, 2, 1, 0, "Los Angeles Lakers");
        createTeamCoordinateMapping(ret, 2, 1, 1, "Oklahoma City Thunder");
        createTeamCoordinateMapping(ret, 2, 1, 2, "Denver Nuggets");
        createTeamCoordinateMapping(ret, 2, 1, 3, "Utah Jazz");
        // Region 3
        createTeamCoordinateMapping(ret, 3, 1, 0, "Phoenix Suns");
        createTeamCoordinateMapping(ret, 3, 1, 1, "Portland Trail Blazers");
        createTeamCoordinateMapping(ret, 3, 1, 2, "Dallas Mavericks");
        createTeamCoordinateMapping(ret, 3, 1, 3, "San Antonio Spurs");
        return ret;
    }
}
