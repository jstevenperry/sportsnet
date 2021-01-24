package com.jstevenperry.sportsnet.nhl;

import com.jstevenperry.sportsnet.framework.AbstractNetworkTrainer;
import com.jstevenperry.sportsnet.framework.menus.AbstractDataElementChoice;
import com.jstevenperry.sportsnet.framework.util.NetworkProperties;
import com.jstevenperry.sportsnet.framework.util.NetworkUtils;
import com.jstevenperry.sportsnet.nhl.menus.NhlDataElementMenu;
import com.jstevenperry.sportsnet.nhl.model.NhlSeasonData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
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
    List<Integer> neuronLayerDescriptor = new ArrayList<>();
    int numberOfHiddenLayers = NetworkProperties.getNumberOfHiddenLayers();
    int primeSeed = NetworkProperties.getPrimeSeed();
    Integer primeDivisor = NetworkProperties.getPrimeDivisor();
    if (numberOfHiddenLayers < 6) {
      String errorMessage = String.format("Algorithm does not work with < 6 hidden layers, you specified %s", numberOfHiddenLayers);
      log.error("generateSampleNetworkLayout({}, {}): {}", numberOfInputs, numberOfOutputs, numberOfHiddenLayers);
      throw new RuntimeException(errorMessage);
    }
    int middleLayerIndex = numberOfHiddenLayers / 2 + 1;
    // First layer: numberOfInputs * primeSeed
    neuronLayerDescriptor.add(numberOfInputs * numberOfInputs * primeSeed);
    for (int index = 1; index < middleLayerIndex; index++) {
      neuronLayerDescriptor.add(index, (neuronLayerDescriptor.get(index - 1) / primeDivisor));
    }
    // Middle layer: numberOfInputs * numberOfOutputs * fudgeFactor
    neuronLayerDescriptor.add(middleLayerIndex, numberOfInputs * numberOfOutputs * primeSeed);
    for (int index = middleLayerIndex + 1; index < numberOfHiddenLayers - 1; index++) {
      neuronLayerDescriptor.add(index, (neuronLayerDescriptor.get(index - 1) / primeDivisor));
    }
    neuronLayerDescriptor.add(numberOfOutputs * numberOfOutputs * numberOfOutputs);
    String hiddenLayerStructure = NetworkUtils.generateLayerStructureString(neuronLayerDescriptor);
    return String.format("%dx%sx%d", numberOfInputs, hiddenLayerStructure, numberOfOutputs);

  }
}
