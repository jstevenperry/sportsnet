package com.jstevenperry.sportsnet.nhl.plugins;

import com.jstevenperry.sportsnet.framework.AbstractTournamentRunnerPlugin;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.nhl.menus.NhlNetworkMenu;
import com.jstevenperry.sportsnet.nhl.model.NhlSeasonData;

public abstract class AbstractNhlTournamentRunnerPlugin extends AbstractTournamentRunnerPlugin<NhlSeasonData> {
  public AbstractNhlTournamentRunnerPlugin(final SeasonDataDao<NhlSeasonData> seasonDataDao) {
    super(seasonDataDao);
  }

  @Override
  public String getTournamentTemplateFileName() {
    return "/nhl/tournament-template-nhl.txt";
  }

  @Override
  protected String getTournamentResultsTemplateFileName() {
    return "/nhl/tournament-results.csv";
  }
  
  @Override
  protected String getNetworkType() {
    return NhlNetworkMenu.NETWORK_TYPE;
  }
}
