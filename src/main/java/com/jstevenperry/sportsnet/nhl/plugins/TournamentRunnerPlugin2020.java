package com.jstevenperry.sportsnet.nhl.plugins;

import com.jstevenperry.sportsnet.framework.model.TeamCoordinate;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.nhl.model.NhlSeasonData;

import java.util.HashMap;
import java.util.Map;

public class TournamentRunnerPlugin2020 extends AbstractNhlTournamentRunnerPlugin {
  public TournamentRunnerPlugin2020(final SeasonDataDao<NhlSeasonData> seasonDataDao) {
    super(seasonDataDao);
  }

  @Override
  public Integer getTournamentYear() {
    return 2019;
  }

  @Override
  protected Map<TeamCoordinate, NhlSeasonData> buildTeamCoordinateMap() {
    Map<TeamCoordinate, NhlSeasonData> ret = new HashMap<>();
    //
    // Add team coordinates
    // Region 0
    createTeamCoordinateMapping(ret, 0, 1, 0, "Philadelphia Flyers");
    createTeamCoordinateMapping(ret, 0, 1, 1, "Montreal Canadiens");
    createTeamCoordinateMapping(ret, 0, 1, 2, "Washington Capitals");
    createTeamCoordinateMapping(ret, 0, 1, 3, "New York Islanders");
    // Region 1
    createTeamCoordinateMapping(ret, 1, 1, 0, "Tampa Bay Lightning");
    createTeamCoordinateMapping(ret, 1, 1, 1, "Columbus Blue Jackets");
    createTeamCoordinateMapping(ret, 1, 1, 2, "Boston Bruins");
    createTeamCoordinateMapping(ret, 1, 1, 3, "Carolina Hurricanes");
    // Region 2
    createTeamCoordinateMapping(ret, 2, 1, 0, "Vegas Golden Knights");
    createTeamCoordinateMapping(ret, 2, 1, 1, "Chicago Blackhawks");
    createTeamCoordinateMapping(ret, 2, 1, 2, "St Louis Blues");
    createTeamCoordinateMapping(ret, 2, 1, 3, "Vancouver Canucks");
    // Region 3
    createTeamCoordinateMapping(ret, 3, 1, 0, "Colorado Avalanche");
    createTeamCoordinateMapping(ret, 3, 1, 1, "Arizona Coyotes");
    createTeamCoordinateMapping(ret, 3, 1, 2, "Dallas Stars");
    createTeamCoordinateMapping(ret, 3, 1, 3, "Calgary Flames");
    return ret;
  }

}
