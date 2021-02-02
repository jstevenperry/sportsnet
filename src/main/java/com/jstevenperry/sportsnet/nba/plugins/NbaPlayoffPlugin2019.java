package com.jstevenperry.sportsnet.nba.plugins;

import com.jstevenperry.sportsnet.framework.model.TeamCoordinate;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.nba.model.NbaSeasonData;

import java.util.HashMap;
import java.util.Map;

public class NbaPlayoffPlugin2019 extends AbstractNbaTournamentRunnerPlugin {
    public NbaPlayoffPlugin2019(final SeasonDataDao<NbaSeasonData> seasonDataDao) {
        super(seasonDataDao);
    }

    @Override
    public Integer getTournamentYear() {
        return 2019;
    }

    @Override
    protected Map<TeamCoordinate, NbaSeasonData> buildTeamCoordinateMap() {
        Map<TeamCoordinate, NbaSeasonData> ret = new HashMap<>();
        //
        // Add team coordinates
        // Region 0
        createTeamCoordinateMapping(ret, 0, 1, 0, "Milwaukee Bucks");
        createTeamCoordinateMapping(ret, 0, 1, 1, "Orlando Magic");
        createTeamCoordinateMapping(ret, 0, 1, 2, "Miami Heat");
        createTeamCoordinateMapping(ret, 0, 1, 3, "Indiana Pacers");
        // Region 1
        createTeamCoordinateMapping(ret, 1, 1, 0, "Boston Celtics");
        createTeamCoordinateMapping(ret, 1, 1, 1, "Philadelphia 76ers");
        createTeamCoordinateMapping(ret, 1, 1, 2, "Toronto Raptors");
        createTeamCoordinateMapping(ret, 1, 1, 3, "Brooklyn Nets");
        // Region 2
        createTeamCoordinateMapping(ret, 2, 1, 0, "Los Angeles Lakers");
        createTeamCoordinateMapping(ret, 2, 1, 1, "Portland Trail Blazers");
        createTeamCoordinateMapping(ret, 2, 1, 2, "Houston Rockets");
        createTeamCoordinateMapping(ret, 2, 1, 3, "Oklahoma City Thunder");
        // Region 3
        createTeamCoordinateMapping(ret, 3, 1, 0, "Denver Nuggets");
        createTeamCoordinateMapping(ret, 3, 1, 1, "Utah Jazz");
        createTeamCoordinateMapping(ret, 3, 1, 2, "Los Angeles Clippers");
        createTeamCoordinateMapping(ret, 3, 1, 3, "Dallas Mavericks");
        return ret;
    }
}
