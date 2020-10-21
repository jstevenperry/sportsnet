package com.jstevenperry.sportsnet.ncaabb;

import com.jstevenperry.sportsnet.framework.AbstractNetworkTrainer;
import com.jstevenperry.sportsnet.framework.menus.AbstractDataElementChoice;
import com.jstevenperry.sportsnet.ncaabb.menus.DataElementMenu;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

public class NcaaBbNetworkTrainer extends AbstractNetworkTrainer {
  @Override
  protected List<AbstractDataElementChoice> scanDataElementChoice(final BufferedReader scanner, final List<AbstractDataElementChoice> choices) throws IOException {
    DataElementMenu dataElementMenu = new DataElementMenu();
    return dataElementMenu.go(scanner, choices);
  }
}
