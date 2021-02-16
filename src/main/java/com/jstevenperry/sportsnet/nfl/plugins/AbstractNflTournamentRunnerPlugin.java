/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet.nfl.plugins;

import com.jstevenperry.sportsnet.framework.AbstractTournamentRunnerPlugin;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.nfl.menus.NflNetworkMenu;
import com.jstevenperry.sportsnet.nfl.model.NflSeasonData;

import java.util.List;

public abstract class AbstractNflTournamentRunnerPlugin extends AbstractTournamentRunnerPlugin<NflSeasonData> {
    public AbstractNflTournamentRunnerPlugin(final SeasonDataDao<NflSeasonData> seasonDataDao) {
        super(seasonDataDao);
    }

    @Override
    protected String getTournamentResultsTemplateFileName() {
        return "/nfl/playoff-results-nfl.csv";
    }

    @Override
    protected String getNetworkType() {
        return NflNetworkMenu.NETWORK_TYPE;
    }

    @Override
    public String getTournamentTemplateFileName() {
        return "/nfl/playoff-matchups-template-nfl.txt";
    }

    @Override
    public List<Double> seasonDataAsDoubleList(final NflSeasonData seasonData, final Double resultClass, final boolean isTraining) {
        return NflSeasonData.asDoubleList(seasonData, resultClass, isTraining);
    }
}
