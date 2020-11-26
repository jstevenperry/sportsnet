package com.jstevenperry.sportsnet.nfl.menus;

import com.jstevenperry.sportsnet.framework.menus.AbstractNetworkMenu;
import com.jstevenperry.sportsnet.nfl.NflNetworkEvaluator;
import com.jstevenperry.sportsnet.nfl.NflNetworkRunner;
import com.jstevenperry.sportsnet.nfl.NflNetworkTrainer;
import com.jstevenperry.sportsnet.nfl.model.NflSeasonData;
import com.jstevenperry.sportsnet.nfl.persistence.NflSeasonDataFlatFileDao;
import com.jstevenperry.sportsnet.nfl.persistence.NflTournamentParticipantFlatFileDao;
import com.jstevenperry.sportsnet.nfl.persistence.NflTournamentResultFlatFileDao;
import org.springframework.context.ApplicationContext;

public class NflNetworkMenu extends AbstractNetworkMenu<NflSeasonData> {
  public NflNetworkMenu(final ApplicationContext applicationContext) {
    super(
      applicationContext.getBean(NflSeasonDataFlatFileDao.class),
      applicationContext.getBean(NflTournamentResultFlatFileDao.class),
      applicationContext.getBean(NflTournamentParticipantFlatFileDao.class),
      new NflNetworkRunner(),
      new NflNetworkTrainer(),
      new NflNetworkEvaluator()
    );
  }

  @Override
  public String getMenuTitle() {
    return "NFL Football Net";
  }

  @Override
  public Integer getNumberOfOutputClasses() {
    return 4;
  }

  @Override
  public String getNetworkType() {
    return "NFL";
  }
}
