/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet.ncaabb;

import com.jstevenperry.sportsnet.framework.AbstractNetworkEvaluator;
import com.jstevenperry.sportsnet.ncaabb.model.NcaaBbSeasonData;

import java.util.List;

public class NcaaBbNetworkEvaluator extends AbstractNetworkEvaluator<NcaaBbSeasonData> {
    @Override
    public List<Double> seasonDataAsDoubleList(final NcaaBbSeasonData seasonData, final Double resultClass, final boolean isTraining) {
        return NcaaBbSeasonData.asDoubleList(seasonData, resultClass, isTraining);
    }
}
