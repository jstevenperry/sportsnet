package com.jstevenperry.sportsnet.nhl;

import com.jstevenperry.sportsnet.framework.AbstractNetworkEvaluator;
import com.jstevenperry.sportsnet.nhl.model.NhlSeasonData;

import java.util.List;

public class NhlNetworkEvaluator extends AbstractNetworkEvaluator<NhlSeasonData> {
    @Override
    public List<Double> seasonDataAsDoubleList(final NhlSeasonData seasonData, final Double resultClass, final boolean isTraining) {
        return NhlSeasonData.asDoubleList(seasonData, resultClass, isTraining);
    }
}
