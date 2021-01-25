package com.jstevenperry.sportsnet.nhl;

import com.jstevenperry.sportsnet.framework.AbstractNetworkTrainer;
import com.jstevenperry.sportsnet.framework.menus.AbstractDataElementChoice;
import com.jstevenperry.sportsnet.framework.menus.SportsnetChoice;
import com.jstevenperry.sportsnet.framework.util.NetworkProperties;
import com.jstevenperry.sportsnet.framework.util.NetworkUtils;
import com.jstevenperry.sportsnet.nhl.menus.NhlDataElementMenu;
import com.jstevenperry.sportsnet.nhl.menus.NhlNetworkMenu;
import com.jstevenperry.sportsnet.nhl.model.NhlSeasonData;
import org.nd4j.common.io.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

public class NhlNetworkTrainer extends AbstractNetworkTrainer<NhlSeasonData> {

  private static final Logger log = LoggerFactory.getLogger(NhlNetworkTrainer.class);

  @Override
  protected List<AbstractDataElementChoice> scanDataElementChoice(final BufferedReader scanner, final List<AbstractDataElementChoice> choices) throws IOException {
    return new NhlDataElementMenu().go(scanner, choices);
  }

  @Override
  public List<Double> seasonDataAsDoubleList(final NhlSeasonData seasonData, final Double resultClass, final boolean isTraining) {
    return NhlSeasonData.asDoubleList(seasonData, resultClass, isTraining);
  }

  @Override
  protected String computeNetworkStructure(final int numberOfInputs, final int numberOfOutputs) {
    // Read from a property (NHL.network.layout)
    String hiddenLayerStructure = NetworkProperties.getNetworkLayout(NhlNetworkMenu.NETWORK_TYPE);
    String ret = String.format("%dx%sx%d", numberOfInputs, hiddenLayerStructure, numberOfOutputs);
    if (StringUtils.isEmpty(hiddenLayerStructure)) {
      // If property not specified use the default
      ret = NetworkUtils.generateSampleNetworkLayout(numberOfInputs, numberOfOutputs);
      log.debug("computeNetworkStructure({}, {}): Using default network structure: {}", numberOfInputs, numberOfOutputs, ret);
    } else {
      log.debug("computeNetworkStructure({}, {}): Using custom NHL network structure: {}", numberOfInputs, numberOfOutputs, ret);
    }
    return ret;
  }
}
