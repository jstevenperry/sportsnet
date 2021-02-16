/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet.nfl.plugins;

import com.jstevenperry.sportsnet.framework.model.TeamCoordinate;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.nfl.model.NflSeasonData;

import java.util.HashMap;
import java.util.Map;

public class NflTournamentPlugin2019 extends AbstractNflTournamentRunnerPlugin {
    public NflTournamentPlugin2019(final SeasonDataDao<NflSeasonData> seasonDataDao) {
        super(seasonDataDao);
    }

    @Override
    public Integer getTournamentYear() {
        return 2019;
    }

    @Override
    public String getTournamentYearForDisplay() {
        return "2019-20";
    }

    @Override
    protected Map<TeamCoordinate, NflSeasonData> buildTeamCoordinateMap() {
        Map<TeamCoordinate, NflSeasonData> ret = new HashMap<>();
        //
        // Add team coordinates
        // Round 0 - Wildcard
        // Region 0
        createTeamCoordinateMapping(ret, 0, 0, 0, "Seattle Seahawks");
        createTeamCoordinateMapping(ret, 0, 0, 1, "Philadelphia Eagles");
        createTeamCoordinateMapping(ret, 0, 0, 2, "Minnesota Vikings");
        createTeamCoordinateMapping(ret, 0, 0, 3, "New Orleans Saints");
        // Region 1
        createTeamCoordinateMapping(ret, 1, 0, 0, "Tennessee Titans");
        createTeamCoordinateMapping(ret, 1, 0, 1, "New England Patriots");
        createTeamCoordinateMapping(ret, 1, 0, 2, "Houston Texans");
        createTeamCoordinateMapping(ret, 1, 0, 3, "Buffalo Bills");
        //
        // Round 1 - Division Playoffs
        createTeamCoordinateMapping(ret, 0, 1, 0, "Green Bay Packers");
        // [0, 1, 1] - Wildcard winner - must be computed
        createTeamCoordinateMapping(ret, 0, 1, 2, "San Francisco 49ers");
        // [0, 1, 3] - Wildcard winner - must be computed
        createTeamCoordinateMapping(ret, 1, 1, 0, "Baltimore Ravens");
        // [1, 1, 1] - Wildcard winner - must be computed
        createTeamCoordinateMapping(ret, 1, 1, 2, "Kansas City Chiefs");
        // [1, 1, 3] - Wildcard winner - must be computed
        return ret;
    }
}
