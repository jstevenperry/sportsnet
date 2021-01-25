package com.jstevenperry.sportsnet.nhl.menus;

import com.jstevenperry.sportsnet.framework.menus.AbstractNetworkMenu;
import com.jstevenperry.sportsnet.nhl.NhlNetworkEvaluator;
import com.jstevenperry.sportsnet.nhl.NhlNetworkRunner;
import com.jstevenperry.sportsnet.nhl.NhlNetworkTrainer;
import com.jstevenperry.sportsnet.nhl.model.NhlSeasonData;
import com.jstevenperry.sportsnet.nhl.persistence.NhlSeasonDataFlatFileDao;
import com.jstevenperry.sportsnet.nhl.persistence.NhlTournamentParticipantFlatFileDao;
import com.jstevenperry.sportsnet.nhl.persistence.NhlTournamentResultFlatFileDao;
import org.springframework.context.ApplicationContext;

public class NhlNetworkMenu extends AbstractNetworkMenu<NhlSeasonData> {

    public static final String NETWORK_TYPE = "NHL";

    public NhlNetworkMenu(final ApplicationContext applicationContext) {
        super(
                applicationContext.getBean(NhlSeasonDataFlatFileDao.class),
                applicationContext.getBean(NhlTournamentResultFlatFileDao.class),
                applicationContext.getBean(NhlTournamentParticipantFlatFileDao.class),
                new NhlNetworkRunner(),
                new NhlNetworkTrainer(),
                new NhlNetworkEvaluator());
    }

    @Override
    public String getMenuTitle() {
        return "NHL Hockey Net";
    }

    @Override
    public Integer getNumberOfOutputClasses() {
        //
        // NHL Stanley Cup is a 16 team affair, consisting of 4 rounds,
        // during which any given team could win from zero to four times,
        // for a total of five possible output classes.
        return 5;
    }

    @Override
    public String getNetworkType() {
        return NETWORK_TYPE;
    }

}
