/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet.nba.plugins;

import com.jstevenperry.sportsnet.framework.model.TeamCoordinate;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.nba.model.NbaSeasonData;

import java.util.HashMap;
import java.util.Map;

public class NbaPlayoffPlugin2017 extends AbstractNbaTournamentRunnerPlugin {
    public NbaPlayoffPlugin2017(final SeasonDataDao<NbaSeasonData> seasonDataDao) {
        super(seasonDataDao);
    }

    @Override
    public Integer getTournamentYear() {
        return 2017;
    }

    @Override
    public String getTournamentYearForDisplay() {
        return "2017-18";
    }

    @Override
    protected Map<TeamCoordinate, NbaSeasonData> buildTeamCoordinateMap() {
        Map<TeamCoordinate, NbaSeasonData> ret = new HashMap<>();
        //
        // Add team coordinates
        // Region 0
        createTeamCoordinateMapping(ret, 0, 1, 0, "Toronto Raptors");
        createTeamCoordinateMapping(ret, 0, 1, 1, "Washington Wizards");
        createTeamCoordinateMapping(ret, 0, 1, 2, "Cleveland Cavaliers");
        createTeamCoordinateMapping(ret, 0, 1, 3, "Indiana Pacers");
        // Region 1
        createTeamCoordinateMapping(ret, 1, 1, 0, "Philadelphia 76ers");
        createTeamCoordinateMapping(ret, 1, 1, 1, "Miami Heat");
        createTeamCoordinateMapping(ret, 1, 1, 2, "Boston Celtics");
        createTeamCoordinateMapping(ret, 1, 1, 3, "Milwaukee Bucks");
        // Region 2
        createTeamCoordinateMapping(ret, 2, 1, 0, "Houston Rockets");
        createTeamCoordinateMapping(ret, 2, 1, 1, "Minnesota Timberwolves");
        createTeamCoordinateMapping(ret, 2, 1, 2, "Oklahoma City Thunder");
        createTeamCoordinateMapping(ret, 2, 1, 3, "Utah Jazz");
        // Region 3
        createTeamCoordinateMapping(ret, 3, 1, 0, "Portland Trail Blazers");
        createTeamCoordinateMapping(ret, 3, 1, 1, "New Orleans Pelicans");
        createTeamCoordinateMapping(ret, 3, 1, 2, "Golden State Warriors");
        createTeamCoordinateMapping(ret, 3, 1, 3, "San Antonio Spurs");
        return ret;
    }
}
