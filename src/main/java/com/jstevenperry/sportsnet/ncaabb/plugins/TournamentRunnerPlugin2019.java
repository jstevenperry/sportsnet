package com.jstevenperry.sportsnet.ncaabb.plugins;

import com.jstevenperry.sportsnet.framework.AbstractTournamentRunnerPlugin;
import com.jstevenperry.sportsnet.framework.model.TeamCoordinate;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.ncaabb.model.SeasonData;

import java.util.HashMap;
import java.util.Map;

public class TournamentRunnerPlugin2019 extends AbstractTournamentRunnerPlugin {

  public static final String NCAABB_TOURNAMENT_TEMPLATE_NCAABB_2019_TXT = "/ncaabb/tournament-template-ncaabb-2019.txt";

  public TournamentRunnerPlugin2019(final SeasonDataDao seasonDataDao) {
    super(seasonDataDao);
    setTeamCoordinateSeasonDataMap(buildTeamCoordinateMap());
  }

  @Override
  public Integer getTournamentYear() {
    return 2019;
  }

  @Override
  public String getTournamentTemplateFileName() {
    return NCAABB_TOURNAMENT_TEMPLATE_NCAABB_2019_TXT;
  }

  /**
   * Builds the internal map of TeamCoordinate -> SeasonData
   */
  @Override
  protected Map<TeamCoordinate, SeasonData> buildTeamCoordinateMap() {
    // Map a team to its initial team coordinates
    Map<TeamCoordinate, SeasonData> coordinateSeasonDataMap = new HashMap<>();
    // TODO: Layla writes this code
    // [0,0,0],[0,0,1] maps to Prairie View vs Fairleigh Dickinson
    createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 0, 0, "Prairie View");
    createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 0, 1, "Fairleigh Dickinson");
    // TODO: Add other matchups here
    return coordinateSeasonDataMap;
  }

}
