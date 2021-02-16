/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet.nfl.plugins;

import com.jstevenperry.sportsnet.framework.model.TeamCoordinate;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.nfl.model.NflSeasonData;

import java.util.HashMap;
import java.util.Map;

public class NflTournamentPlugin2020 extends AbstractNflTournamentRunnerPlugin {
    public NflTournamentPlugin2020(SeasonDataDao<NflSeasonData> seasonDataDao) {
        super(seasonDataDao);
    }

    @Override
    public Integer getTournamentYear() {
        return 2020;
    }

    @Override
    public String getTournamentYearForDisplay() {
        return "2020-21";
    }

    @Override
    protected String getTournamentResultsTemplateFileName() {
        return "/nfl/2020/playoff-results-nfl.csv";
    }

    @Override
    public String getTournamentTemplateFileName() {
        return "/nfl/2020/playoff-matchups-template-nfl.txt";
    }

    @Override
    protected Map<TeamCoordinate, NflSeasonData> buildTeamCoordinateMap() {
        Map<TeamCoordinate, NflSeasonData> ret = new HashMap<>();
        //
        // Add team coordinates
        // Round 0 - Wildcard
        // Region 0
        createTeamCoordinateMapping(ret, 0, 0, 0, "Tennessee Titans");
        createTeamCoordinateMapping(ret, 0, 0, 1, "Baltimore Ravens");
        createTeamCoordinateMapping(ret, 0, 0, 2, "Pittsburgh Steelers");
        createTeamCoordinateMapping(ret, 0, 0, 3, "Cleveland Browns");
        createTeamCoordinateMapping(ret, 0, 0, 4, "Buffalo Bills");
        createTeamCoordinateMapping(ret, 0, 0, 5, "Indianapolis Colts");
        // Region 1
        createTeamCoordinateMapping(ret, 1, 0, 0, "Seattle Seahawks");
        createTeamCoordinateMapping(ret, 1, 0, 1, "Los Angeles Rams");
        createTeamCoordinateMapping(ret, 1, 0, 2, "Washington Football Team");
        createTeamCoordinateMapping(ret, 1, 0, 3, "Tampa Bay Buccaneers");
        createTeamCoordinateMapping(ret, 1, 0, 4, "New Orleans Saints");
        createTeamCoordinateMapping(ret, 1, 0, 5, "Chicago Bears");
        //
        // Round 1 - Division Playoffs
        createTeamCoordinateMapping(ret, 0, 1, 0, "Kansas City Chiefs");
        // [0, 1, 1] - Wildcard winner - must be computed3
        // [0, 1, 3] - Wildcard winner - must be computed
        // [0, 1, 5] - Wildcard winner - must be computed
        createTeamCoordinateMapping(ret, 1, 1, 0, "Green Bay Packers");
        // [1, 1, 1] - Wildcard winner - must be computed
        // [1, 1, 3] - Wildcard winner - must be computed
        // [1, 1, 5] - Wildcard winner - must be computed
        return ret;
    }
}
