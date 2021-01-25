package com.jstevenperry.sportsnet.nfl;

import com.jstevenperry.sportsnet.framework.AbstractNetworkEvaluator;
import com.jstevenperry.sportsnet.nfl.model.NflSeasonData;

import java.util.List;

public class NflNetworkEvaluator extends AbstractNetworkEvaluator<NflSeasonData> {
    @Override
    public List<Double> seasonDataAsDoubleList(final NflSeasonData seasonData, final Double resultClass, final boolean isTraining) {
        return NflSeasonData.asDoubleList(seasonData, resultClass, isTraining);
    }
}
