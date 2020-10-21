package com.jstevenperry.sportsnet.ncaabb;

import com.jstevenperry.sportsnet.framework.AbstractNetworkRunner;
import com.jstevenperry.sportsnet.framework.AbstractTournamentRunnerPlugin;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.ncaabb.plugins.TournamentRunnerPlugin2018;
import com.jstevenperry.sportsnet.ncaabb.plugins.TournamentRunnerPlugin2019;

import java.util.Arrays;
import java.util.List;

/**
 * NCAA Basketball Network Runner.
 */
public class NcaaBbNetworkRunner extends AbstractNetworkRunner {

  private final SeasonDataDao ncaaBbSeasonDataDao;

  public NcaaBbNetworkRunner(final SeasonDataDao ncaaBbSeasonDataDao) {
    this.ncaaBbSeasonDataDao = ncaaBbSeasonDataDao;
  }

  @Override
  protected List<AbstractTournamentRunnerPlugin> loadPlugins() {
    return Arrays.asList(
      new TournamentRunnerPlugin2018(ncaaBbSeasonDataDao),
      new TournamentRunnerPlugin2019(ncaaBbSeasonDataDao)
    );
  }
}
