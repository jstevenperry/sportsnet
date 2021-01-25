package com.jstevenperry.sportsnet.nhl.plugins;

import com.jstevenperry.sportsnet.framework.model.TeamCoordinate;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.nhl.model.NhlSeasonData;

import java.util.HashMap;
import java.util.Map;

public class TournamentRunnerPlugin2018 extends AbstractNhlTournamentRunnerPlugin {
    public TournamentRunnerPlugin2018(final SeasonDataDao<NhlSeasonData> seasonDataDao) {
        super(seasonDataDao);
    }

    @Override
    public Integer getTournamentYear() {
        return 2018;
    }


    @Override
    protected Map<TeamCoordinate, NhlSeasonData> buildTeamCoordinateMap() {
        Map<TeamCoordinate, NhlSeasonData> ret = new HashMap<>();
        //
        // Add team coordinates
        // Region 0
        createTeamCoordinateMapping(ret, 0, 1, 0, "Tampa Bay Lightning");
        createTeamCoordinateMapping(ret, 0, 1, 1, "Columbus Blue Jackets");
        createTeamCoordinateMapping(ret, 0, 1, 2, "Boston Bruins");
        createTeamCoordinateMapping(ret, 0, 1, 3, "Toronto Maple Leafs");
        // Region 1
        createTeamCoordinateMapping(ret, 1, 1, 0, "Washington Capitals");
        createTeamCoordinateMapping(ret, 1, 1, 1, "Carolina Hurricanes");
        createTeamCoordinateMapping(ret, 1, 1, 2, "New York Islanders");
        createTeamCoordinateMapping(ret, 1, 1, 3, "Pittsburgh Penguins");
        // Region 2
        createTeamCoordinateMapping(ret, 2, 1, 0, "Nashville Predators");
        createTeamCoordinateMapping(ret, 2, 1, 1, "Dallas Stars");
        createTeamCoordinateMapping(ret, 2, 1, 2, "Winnipeg Jets");
        createTeamCoordinateMapping(ret, 2, 1, 3, "St Louis Blues");
        // Region 3
        createTeamCoordinateMapping(ret, 3, 1, 0, "Calgary Flames");
        createTeamCoordinateMapping(ret, 3, 1, 1, "Colorado Avalanche");
        createTeamCoordinateMapping(ret, 3, 1, 2, "San Jose Sharks");
        createTeamCoordinateMapping(ret, 3, 1, 3, "Vegas Golden Knights");
        return ret;
    }


}
