/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.util;

import com.jstevenperry.sportsnet.framework.exception.BadConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class NetworkStructureGenerator {

    public static final String LOG_PREFIX = "CORENSG";

    private static final Logger log = LoggerFactory.getLogger(NetworkStructureGenerator.class);

    public static String generateDefaultNetworkLayout(final Integer numberOfInputs,
                                                      final Integer numberOfOutputs,
                                                      final Integer numberOfHiddenLayers,
                                                      final Boolean randomizeNumberOfNeurons) {
        List<Integer> neuronLayerDescriptor = new ArrayList<>();
        for (int aa = 0; aa < numberOfHiddenLayers; aa++) {
            neuronLayerDescriptor.add(0);
        }

        // First hidden layer
        int numberOfNeuronsInFirstLayer = Math.min(numberOfInputs * numberOfHiddenLayers, numberOfInputs * numberOfOutputs * numberOfOutputs);
        neuronLayerDescriptor.set(0, numberOfNeuronsInFirstLayer);
        // Layers between first and middle
        int middleLayerIndex = numberOfHiddenLayers / 2;
        int numberOfNeuronsInMiddleLayer = numberOfInputs * numberOfOutputs;
        for (int index = middleLayerIndex; index > 0; index--) {
            int min = Math.min(numberOfNeuronsInFirstLayer / index, numberOfNeuronsInMiddleLayer);
            int max = Math.max(numberOfNeuronsInFirstLayer / index, numberOfNeuronsInMiddleLayer);
            neuronLayerDescriptor.set(index, (randomizeNumberOfNeurons) ? getRandomNumberUsingNextInt(min, max) : (max + min) / 2);
        }
        // Middle hidden layer
        int numberOfNeuronsInLastLayer = numberOfOutputs * numberOfOutputs * numberOfOutputs;
        neuronLayerDescriptor.set(middleLayerIndex, numberOfNeuronsInMiddleLayer);
        int lastLayerIndex = numberOfHiddenLayers - 1;
        for (int index = lastLayerIndex - 1; index > middleLayerIndex; index--) {
            int min = Math.min(numberOfNeuronsInLastLayer / index, numberOfNeuronsInLastLayer);
            int max = Math.max(numberOfNeuronsInLastLayer / index, numberOfNeuronsInLastLayer);
            neuronLayerDescriptor.set(index, (randomizeNumberOfNeurons) ? getRandomNumberUsingNextInt(min, max) : (max + min) / 2);
        }
        // Last hidden layer (closest to output)
        neuronLayerDescriptor.set(lastLayerIndex, numberOfNeuronsInLastLayer);
        return AbstractNetworkUtils.generateLayerStructureString(neuronLayerDescriptor);
    }

    public static String generateSmallDefaultNetworkLayout(final Integer numberOfInputs,
                                                           final Integer numberOfOutputs,
                                                           final Integer numberOfHiddenLayers,
                                                           final Boolean randomizeNumberOfNeurons) {
        List<Integer> neuronLayerDescriptor = new ArrayList<>();
        for (int aa = 0; aa < numberOfHiddenLayers; aa++) {
            neuronLayerDescriptor.add(0);
        }

        // First hidden layer
        int numberOfNeuronsInFirstLayer = numberOfInputs + 2 * numberOfOutputs;
        neuronLayerDescriptor.set(0, numberOfNeuronsInFirstLayer);
        // Layers between first and middle
        int middleLayerIndex = numberOfHiddenLayers / 2;
        int numberOfNeuronsInMiddleLayer = numberOfInputs * 2 * numberOfOutputs;
        for (int index = 1; index < middleLayerIndex; index++) {
            int numberOfNeuronsInPreviousLayer = neuronLayerDescriptor.get(index - 1);
            int min = numberOfNeuronsInPreviousLayer + 2 * numberOfOutputs;
            int max = numberOfNeuronsInPreviousLayer + 2 * index * numberOfOutputs;
            neuronLayerDescriptor.set(index, (randomizeNumberOfNeurons) ? getRandomNumberUsingNextInt(min, max) : max);
        }
        // Middle hidden layer
        neuronLayerDescriptor.set(middleLayerIndex, numberOfNeuronsInMiddleLayer);
        for (int index = middleLayerIndex + 1; index < numberOfHiddenLayers - 1; index++) {
            int numberOfNeuronsInPreviousLayer = neuronLayerDescriptor.get(index - 1);
            int min = (numberOfNeuronsInPreviousLayer / index) * 2;
            int max = numberOfNeuronsInPreviousLayer - numberOfInputs;
            neuronLayerDescriptor.set(index, (randomizeNumberOfNeurons) ? getRandomNumberUsingNextInt(min, max) : min);
        }
        // Last hidden layer (closest to output)
        int numberOfNeuronsInLastLayer = numberOfOutputs + numberOfInputs;
        neuronLayerDescriptor.set(numberOfHiddenLayers - 1, numberOfNeuronsInLastLayer);
        return AbstractNetworkUtils.generateLayerStructureString(neuronLayerDescriptor);
    }

    public static String generateHourglassNetworkLayout(final Integer numberOfInputs,
                                                        final Integer numberOfOutputs,
                                                        final Integer numberOfHiddenLayers,
                                                        final Boolean randomizeNumberOfNeurons) {
        List<Integer> neuronLayerDescriptor = new ArrayList<>();
        for (int layer = 1; layer < numberOfHiddenLayers; layer++) {
            int min = Math.min(numberOfInputs * numberOfOutputs * numberOfHiddenLayers / layer, numberOfInputs * numberOfOutputs * numberOfOutputs / layer);
            int max = Math.max(numberOfInputs * numberOfOutputs * numberOfHiddenLayers / layer, numberOfInputs * numberOfOutputs * numberOfOutputs / layer);
            neuronLayerDescriptor.add((randomizeNumberOfNeurons) ? getRandomNumberUsingNextInt(min, max) : (min + max) / 2);
        }
        int min = Math.min(numberOfOutputs * numberOfOutputs * numberOfOutputs, numberOfHiddenLayers * numberOfHiddenLayers * numberOfHiddenLayers);
        int max = Math.max(numberOfOutputs * numberOfOutputs * numberOfOutputs, numberOfHiddenLayers * numberOfHiddenLayers * numberOfHiddenLayers);
        neuronLayerDescriptor.add((randomizeNumberOfNeurons) ? getRandomNumberUsingNextInt(min, max) : (min + max) / 2);
        return AbstractNetworkUtils.generateLayerStructureString(neuronLayerDescriptor);
    }

    public static String generateTaperedTreeNetworkLayout(final Integer numberOfInputs,
                                                          final Integer numberOfOutputs,
                                                          final Integer numberOfHiddenLayers,
                                                          final Boolean randomizeNumberOfNeurons) {
        List<Integer> neuronLayerDescriptor = new ArrayList<>();
        int middleLayerIndex = numberOfHiddenLayers / 2;
        for (int layer = 1; layer < middleLayerIndex; layer++) {
            int min = Math.min(numberOfInputs * numberOfOutputs * numberOfHiddenLayers / (layer + 2), numberOfInputs * numberOfOutputs * numberOfOutputs / (layer + 2));
            int max = Math.max(numberOfInputs * numberOfOutputs * numberOfHiddenLayers / (layer + 2), numberOfInputs * numberOfOutputs * numberOfOutputs / (layer + 2));
            neuronLayerDescriptor.add((randomizeNumberOfNeurons) ? getRandomNumberUsingNextInt(min, max) : (min + max) / 2);
        }
        int min = Math.min(numberOfInputs * numberOfOutputs * middleLayerIndex, numberOfInputs * numberOfOutputs);
        int max = Math.max(numberOfInputs * numberOfOutputs * middleLayerIndex, numberOfInputs * numberOfOutputs);
        neuronLayerDescriptor.add((randomizeNumberOfNeurons) ? getRandomNumberUsingNextInt(min, max) : (min + max) / 2);

        for (int layer = middleLayerIndex; layer < numberOfHiddenLayers; layer++) {
            min = Math.min(numberOfInputs * numberOfOutputs * numberOfHiddenLayers / (layer + 2), numberOfInputs * numberOfOutputs * numberOfOutputs / (layer + 2));
            max = Math.max(numberOfInputs * numberOfOutputs * numberOfHiddenLayers / (layer + 2), numberOfInputs * numberOfOutputs * numberOfOutputs / (layer + 2));
            neuronLayerDescriptor.add((randomizeNumberOfNeurons) ? getRandomNumberUsingNextInt(min, max) : (min + max) / 2);
        }

        return AbstractNetworkUtils.generateLayerStructureString(neuronLayerDescriptor);
    }


    public static String generateUserDefinedLayout(final String userDefinedNetworkStructure,
                                                   final boolean randomizeNumberOfNeurons) {
        List<Integer> computedHiddenLayerStructure = new ArrayList<>();
        int[] userDefinedHiddenLayerStructure = AbstractNetworkUtils.parseNetworkStructure(userDefinedNetworkStructure);
        if (userDefinedHiddenLayerStructure.length < 3) {
            String errorMessage = String.format("%s0081E: Bad user-defined network structure: '%s', must have at least 3  hidden layers!",
                    LOG_PREFIX, userDefinedNetworkStructure);
            log.error(errorMessage);
            throw new BadConfigurationException(errorMessage);
        }
        int numberOfNeuronsInPreviousLayer = userDefinedHiddenLayerStructure[0];
        computedHiddenLayerStructure.add(numberOfNeuronsInPreviousLayer);
        for (int layerIndex = 1; layerIndex < userDefinedHiddenLayerStructure.length; layerIndex++) {
            final int numberOfNeuronsInCurrentLayer = userDefinedHiddenLayerStructure[layerIndex];
            if (randomizeNumberOfNeurons) {
                int min = (int)(numberOfNeuronsInPreviousLayer * 0.5);
                int max = (int)(numberOfNeuronsInCurrentLayer * 1.5);
                computedHiddenLayerStructure.add(getRandomNumberUsingNextInt(min, max));
            } else {
                computedHiddenLayerStructure.add(numberOfNeuronsInCurrentLayer);
            }
        }
        return AbstractNetworkUtils.generateLayerStructureString(computedHiddenLayerStructure);
    }

    /**
     * Source: https://www.baeldung.com/java-generating-random-numbers-in-range
     *
     * @param min The lowest possible number that is acceptable
     * @param max The highest possible number that is acceptable
     * @return A number between min and max
     */
    public static int getRandomNumberUsingNextInt(int min, int max) {
        Random random = new Random();
        int bound = Math.max(max - min, 1);
        return random.nextInt(bound) + min;
    }
}
