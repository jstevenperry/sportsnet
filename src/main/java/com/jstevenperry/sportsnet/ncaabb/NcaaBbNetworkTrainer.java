/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet.ncaabb;

import com.jstevenperry.sportsnet.framework.AbstractNetworkTrainer;
import com.jstevenperry.sportsnet.framework.menus.AbstractDataElementChoice;
import com.jstevenperry.sportsnet.ncaabb.menus.NcaaBbDataElementMenu;
import com.jstevenperry.sportsnet.ncaabb.model.NcaaBbSeasonData;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

public class NcaaBbNetworkTrainer extends AbstractNetworkTrainer<NcaaBbSeasonData> {
    @Override
    protected List<AbstractDataElementChoice> scanDataElementChoice(final BufferedReader scanner, final List<AbstractDataElementChoice> choices) throws IOException {
        NcaaBbDataElementMenu dataElementMenu = new NcaaBbDataElementMenu();
        return dataElementMenu.go(scanner, choices);
    }

    @Override
    public List<Double> seasonDataAsDoubleList(final NcaaBbSeasonData seasonData, final Double resultClass, final boolean isTraining) {
        return NcaaBbSeasonData.asDoubleList(seasonData, resultClass, isTraining);
    }

}
