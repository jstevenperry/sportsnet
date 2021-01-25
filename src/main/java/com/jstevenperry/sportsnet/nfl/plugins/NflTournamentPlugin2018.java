package com.jstevenperry.sportsnet.nfl.plugins;

import com.jstevenperry.sportsnet.framework.model.TeamCoordinate;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.nfl.model.NflSeasonData;

import java.util.HashMap;
import java.util.Map;

public class NflTournamentPlugin2018 extends AbstractNflTournamentRunnerPlugin {
    public NflTournamentPlugin2018(final SeasonDataDao<NflSeasonData> seasonDataDao) {
        super(seasonDataDao);
    }

    @Override
    public Integer getTournamentYear() {
        return 2018; // 2018-2019 Season
    }

    @Override
    protected Map<TeamCoordinate, NflSeasonData> buildTeamCoordinateMap() {
        Map<TeamCoordinate, NflSeasonData> ret = new HashMap<>();
        //
        // Add team coordinates
        // Round 0 - Wildcard
        // Region 0
        createTeamCoordinateMapping(ret, 0, 0, 0, "Los Angeles Chargers");
        createTeamCoordinateMapping(ret, 0, 0, 1, "Baltimore Ravens");
        createTeamCoordinateMapping(ret, 0, 0, 2, "Indianapolis Colts");
        createTeamCoordinateMapping(ret, 0, 0, 3, "Houston Texans");
        // Region 1
        createTeamCoordinateMapping(ret, 1, 0, 0, "Seattle Seahawks");
        createTeamCoordinateMapping(ret, 1, 0, 1, "Dallas Cowboys");
        createTeamCoordinateMapping(ret, 1, 0, 2, "Philadelphia Eagles");
        createTeamCoordinateMapping(ret, 1, 0, 3, "Chicago Bears");
        //
        // Round 1 - Division Playoffs
        createTeamCoordinateMapping(ret, 0, 1, 0, "New England Patriots");
        // [0, 1, 1] - Wildcard winner - must be computed
        createTeamCoordinateMapping(ret, 0, 1, 2, "Kansas City Chiefs");
        // [0, 1, 3] - Wildcard winner - must be computed
        createTeamCoordinateMapping(ret, 1, 1, 0, "Los Angeles Rams");
        // [1, 1, 1] - Wildcard winner - must be computed
        createTeamCoordinateMapping(ret, 1, 1, 2, "New Orleans Saints");
        // [1, 1, 3] - Wildcard winner - must be computed
        return ret;
    }

}
