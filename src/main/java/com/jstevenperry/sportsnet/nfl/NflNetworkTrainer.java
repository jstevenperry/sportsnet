package com.jstevenperry.sportsnet.nfl;

import com.jstevenperry.sportsnet.framework.AbstractNetworkTrainer;
import com.jstevenperry.sportsnet.framework.menus.AbstractDataElementChoice;
import com.jstevenperry.sportsnet.framework.model.NetworkParameters;
import com.jstevenperry.sportsnet.framework.util.NetworkProperties;
import com.jstevenperry.sportsnet.framework.util.NetworkUtils;
import com.jstevenperry.sportsnet.nfl.menus.NflDataElementMenu;
import com.jstevenperry.sportsnet.nfl.model.NflSeasonData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NflNetworkTrainer extends AbstractNetworkTrainer<NflSeasonData> {

  private static final Logger log = LoggerFactory.getLogger(NflNetworkTrainer.class);

  @Override
  protected List<AbstractDataElementChoice> scanDataElementChoice(final BufferedReader scanner, final List<AbstractDataElementChoice> choices) throws IOException {
    // Don't bother to show the menu, just add ALL NFL data
    return new ArrayList<>(new NflDataElementMenu().values());
  }

  @Override
  public List<Double> seasonDataAsDoubleList(final NflSeasonData seasonData, final Double resultClass, final boolean isTraining) {
    return NflSeasonData.asDoubleList(seasonData, resultClass, isTraining);
  }

  @Override
  protected String computeNetworkStructure(final NetworkParameters networkParameters) {
    List<Integer> neuronLayerDescriptor = new ArrayList<>();
    int numberOfHiddenLayers = NetworkProperties.getNumberOfHiddenLayers();
    int numberOfInputs = networkParameters.getNumberOfInputs();
    int numberOfOutputs = networkParameters.getNumberOfOutputs();
    Integer primeSeed = NetworkProperties.getPrimeSeed();
    Integer primeDivisor = NetworkProperties.getPrimeDivisor();
    int fudgeFactor = NetworkProperties.getFudgeFactor();
    if (numberOfHiddenLayers < 6) {
      String errorMessage = String.format("Algorithm does not work with < 6 hidden layers, you specified %s", numberOfHiddenLayers);
      log.error("generateSampleNetworkLayout({}, {}): {}", numberOfInputs, numberOfOutputs, numberOfHiddenLayers);
      throw new RuntimeException(errorMessage);
    }
    int middleLayerIndex = numberOfHiddenLayers / 2 + 1;
    // First layer: numberOfInputs * primeSeed * 2
    neuronLayerDescriptor.add(numberOfInputs * primeSeed * 2);
    for (int index = 1; index < middleLayerIndex; index++) {
      neuronLayerDescriptor.add(index, (neuronLayerDescriptor.get(index - 1) / primeDivisor) * fudgeFactor);
    }
    // Middle layer: numberOfInputs * numberOfOutputs * fudgeFactor
    neuronLayerDescriptor.add(middleLayerIndex, numberOfInputs * numberOfOutputs * fudgeFactor);
    for (int index = middleLayerIndex + 1; index < numberOfHiddenLayers; index++) {
      neuronLayerDescriptor.add(index, (neuronLayerDescriptor.get(index - 1) / primeDivisor) * fudgeFactor);
    }
    neuronLayerDescriptor.add(numberOfOutputs * numberOfOutputs * numberOfOutputs);
    String hiddenLayerStructure = NetworkUtils.generateLayerStructureString(neuronLayerDescriptor);
    return String.format("%dx%sx%d", networkParameters.getNumberOfInputs(), hiddenLayerStructure, networkParameters.getNumberOfOutputs());
  }
}
