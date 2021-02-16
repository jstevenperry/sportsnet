/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet.ncaabb.menus;

import com.jstevenperry.sportsnet.framework.menus.AbstractNetworkMenu;
import com.jstevenperry.sportsnet.ncaabb.NcaaBbNetworkEvaluator;
import com.jstevenperry.sportsnet.ncaabb.NcaaBbNetworkRunner;
import com.jstevenperry.sportsnet.ncaabb.NcaaBbNetworkTrainer;
import com.jstevenperry.sportsnet.ncaabb.model.NcaaBbSeasonData;
import com.jstevenperry.sportsnet.ncaabb.persistence.NcaaBbSeasonDataFlatFileDao;
import com.jstevenperry.sportsnet.ncaabb.persistence.NcaaBbTournamentParticipantFlatFileDao;
import com.jstevenperry.sportsnet.ncaabb.persistence.NcaaBbTournamentResultFlatFileDao;
import org.springframework.context.ApplicationContext;

public class NcaaBbNetworkMenu extends AbstractNetworkMenu<NcaaBbSeasonData> {

    public static final String NETWORK_TYPE = "NCAABB";

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

    @Override
    public String getMenuTitle() {
        return "NCAA Basketball Net";
    }

    @Override
    public Integer getNumberOfOutputClasses() {
        //
        // NCAA Men's Tournament is a 64 team affair, consisting of 6 rounds,
        // during which any given team could win from zero to six times,
        // for a total of seven possible output classes.
        return 7;
    }

    @Override
    public String getNetworkType() {
        return NETWORK_TYPE;
    }
}
