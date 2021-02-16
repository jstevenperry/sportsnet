/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet.nhl;

import com.jstevenperry.sportsnet.framework.AbstractNetworkTrainer;
import com.jstevenperry.sportsnet.framework.menus.AbstractDataElementChoice;
import com.jstevenperry.sportsnet.nhl.menus.NhlDataElementMenu;
import com.jstevenperry.sportsnet.nhl.model.NhlSeasonData;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

public class NhlNetworkTrainer extends AbstractNetworkTrainer<NhlSeasonData> {

    @Override
    protected List<AbstractDataElementChoice> scanDataElementChoice(final BufferedReader scanner, final List<AbstractDataElementChoice> choices) throws IOException {
        return new NhlDataElementMenu().go(scanner, choices);
    }

    @Override
    public List<Double> seasonDataAsDoubleList(final NhlSeasonData seasonData, final Double resultClass, final boolean isTraining) {
        return NhlSeasonData.asDoubleList(seasonData, resultClass, isTraining);
    }

}
