/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet.nba.menus;

import com.jstevenperry.sportsnet.framework.menus.AbstractNetworkMenu;
import com.jstevenperry.sportsnet.nba.NbaNetworkEvaluator;
import com.jstevenperry.sportsnet.nba.NbaNetworkRunner;
import com.jstevenperry.sportsnet.nba.NbaNetworkTrainer;
import com.jstevenperry.sportsnet.nba.model.NbaSeasonData;
import com.jstevenperry.sportsnet.nba.persistence.NbaSeasonDataFlatFileDao;
import com.jstevenperry.sportsnet.nba.persistence.NbaTournamentParticipantFlatFileDao;
import com.jstevenperry.sportsnet.nba.persistence.NbaTournamentResultFlatFileDao;
import org.springframework.context.ApplicationContext;

public class NbaNetworkMenu extends AbstractNetworkMenu<NbaSeasonData> {

    public static final String NETWORK_TYPE = "NBA";

    public NbaNetworkMenu(final ApplicationContext applicationContext) {
        super(
                applicationContext.getBean(NbaSeasonDataFlatFileDao.class),
                applicationContext.getBean(NbaTournamentResultFlatFileDao.class),
                applicationContext.getBean(NbaTournamentParticipantFlatFileDao.class),
                new NbaNetworkRunner(),
                new NbaNetworkTrainer(),
                new NbaNetworkEvaluator()
        );
    }

    @Override
    public String getMenuTitle() {
        return "NBA Basketball Net";
    }

    @Override
    public Integer getNumberOfOutputClasses() {
        //
        // NBA Championship is a 16 team affair, consisting of 4 rounds,
        // during which any given team could win from zero to four times,
        // for a total of five possible output classes.
        return 5;
    }

    @Override
    public String getNetworkType() {
        return NETWORK_TYPE;
    }
}
