package com.jstevenperry.sportsnet.ncaabb.menus;

import com.jstevenperry.sportsnet.framework.menus.NetworkMenu;
import com.jstevenperry.sportsnet.ncaabb.NcaaBbNetworkEvaluator;
import com.jstevenperry.sportsnet.ncaabb.NcaaBbNetworkRunner;
import com.jstevenperry.sportsnet.ncaabb.NcaaBbNetworkTrainer;
import com.jstevenperry.sportsnet.ncaabb.model.NcaaBbSeasonData;
import com.jstevenperry.sportsnet.ncaabb.persistence.NcaaBbSeasonDataFlatFileDao;
import com.jstevenperry.sportsnet.ncaabb.persistence.NcaaBbTournamentParticipantFlatFileDao;
import com.jstevenperry.sportsnet.ncaabb.persistence.NcaaBbTournamentResultFlatFileDao;
import org.springframework.context.ApplicationContext;

public class NcaaBbNetworkMenu extends NetworkMenu<NcaaBbSeasonData> {
  public NcaaBbNetworkMenu(final ApplicationContext applicationContext) {
    //
    // Invoke superclass constructor, passing it everything it needs
    super(
      applicationContext.getBean(NcaaBbSeasonDataFlatFileDao.class),
      applicationContext.getBean(NcaaBbTournamentResultFlatFileDao.class),
      applicationContext.getBean(NcaaBbTournamentParticipantFlatFileDao.class),
      new NcaaBbNetworkRunner(),
      new NcaaBbNetworkTrainer(),
      new NcaaBbNetworkEvaluator());
  }
}
