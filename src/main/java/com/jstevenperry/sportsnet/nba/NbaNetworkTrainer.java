/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet.nba;

import com.jstevenperry.sportsnet.framework.AbstractNetworkTrainer;
import com.jstevenperry.sportsnet.framework.menus.AbstractDataElementChoice;
import com.jstevenperry.sportsnet.nba.menus.NbaDataElementMenu;
import com.jstevenperry.sportsnet.nba.model.NbaSeasonData;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;

public class NbaNetworkTrainer extends AbstractNetworkTrainer<NbaSeasonData> {
    @Override
    public List<Double> seasonDataAsDoubleList(final NbaSeasonData seasonData, final Double resultClass, final boolean isTraining) {
        return NbaSeasonData.seasonDataAsDoubleList(seasonData, resultClass, isTraining);
    }

    @Override
    protected List<AbstractDataElementChoice> scanDataElementChoice(final BufferedReader scanner, final List<AbstractDataElementChoice> choices) {
        // Don't bother to show the menu, just add ALL NBA data
        return new ArrayList<>(new NbaDataElementMenu().values());
    }
}
