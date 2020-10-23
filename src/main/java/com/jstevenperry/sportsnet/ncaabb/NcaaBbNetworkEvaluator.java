package com.jstevenperry.sportsnet.ncaabb;

import com.jstevenperry.sportsnet.framework.AbstractNetworkEvaluator;
import com.jstevenperry.sportsnet.ncaabb.model.NcaaBbSeasonData;

import java.util.List;

public class NcaaBbNetworkEvaluator extends AbstractNetworkEvaluator<NcaaBbSeasonData> {
  @Override
  public List<Double> writeSeasonData(final NcaaBbSeasonData seasonData, final Double resultClass, final boolean isTraining) {
    return NcaaBbSeasonData.asDoubleList(seasonData, resultClass, isTraining);
  }
}
