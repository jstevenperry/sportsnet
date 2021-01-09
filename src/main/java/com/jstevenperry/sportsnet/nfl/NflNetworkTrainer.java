package com.jstevenperry.sportsnet.nfl;

import com.jstevenperry.sportsnet.framework.AbstractNetworkTrainer;
import com.jstevenperry.sportsnet.framework.menus.AbstractDataElementChoice;
import com.jstevenperry.sportsnet.nfl.menus.NflDataElementMenu;
import com.jstevenperry.sportsnet.nfl.model.NflSeasonData;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NflNetworkTrainer extends AbstractNetworkTrainer<NflSeasonData> {
  @Override
  protected List<AbstractDataElementChoice> scanDataElementChoice(final BufferedReader scanner, final List<AbstractDataElementChoice> choices) throws IOException {
    // Don't bother to show the menu, just add ALL NFL data
    return new ArrayList<>(new NflDataElementMenu().values());
  }

  @Override
  public List<Double> seasonDataAsDoubleList(final NflSeasonData seasonData, final Double resultClass, final boolean isTraining) {
    return NflSeasonData.asDoubleList(seasonData, resultClass, isTraining);
  }
}
