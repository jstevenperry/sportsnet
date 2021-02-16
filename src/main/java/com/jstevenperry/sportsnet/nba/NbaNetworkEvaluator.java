/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet.nba;

import com.jstevenperry.sportsnet.framework.AbstractNetworkEvaluator;
import com.jstevenperry.sportsnet.nba.model.NbaSeasonData;

import java.util.List;

public class NbaNetworkEvaluator extends AbstractNetworkEvaluator<NbaSeasonData> {
    @Override
    public List<Double> seasonDataAsDoubleList(final NbaSeasonData seasonData, final Double resultClass, final boolean isTraining) {
        return NbaSeasonData.seasonDataAsDoubleList(seasonData, resultClass, isTraining);
    }
}
