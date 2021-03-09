/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.util;

import com.jstevenperry.sportsnet.framework.menus.ActivationFunctionMenu;
import com.jstevenperry.sportsnet.framework.menus.DataNormalizationMenu;
import com.jstevenperry.sportsnet.framework.menus.LossFunctionMenu;
import com.jstevenperry.sportsnet.framework.menus.UpdaterMenu;
import com.jstevenperry.sportsnet.framework.menus.WeightInitMenu;
import com.jstevenperry.sportsnet.framework.model.NetworkParameters;
import com.jstevenperry.sportsnet.framework.persistence.TournamentParticipantDao;
import org.apache.commons.lang3.StringUtils;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.IUpdater;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;

public class NetworkTrainerParameterScanner {

    private static final Logger log = LoggerFactory.getLogger(NetworkTrainerParameterScanner.class);

    /**
     * Get a list of the years for which the network is to be trained, and a list
     * of the years for which the network is to be evaluated, and return them both
     * in a wrapper list.
     * <p>
     * The first element in the wrapper list is a the list of training years.
     */
    public static void scanYearsToTrainAndEvaluateNetwork(final BufferedReader scanner,
                                                          final NetworkParameters networkParameters,
                                                          final TournamentParticipantDao tournamentParticipantDao) {
        System.out.println("Enter year(s) for training (enter multiple years separated by commas)");

        List<Integer> yearsToTrain = networkParameters.getYearsToTrainAndEvaluateNetwork().get(0);
        scanListOfValidYears(scanner, yearsToTrain, tournamentParticipantDao);
        log.debug("Training data year(s): " + StringUtils.join(yearsToTrain, ','));

        System.out.println("Enter year(s) for evaluation/validation (enter multiple years separated by commas)");
        List<Integer> yearsToEvaluate = networkParameters.getYearsToTrainAndEvaluateNetwork().get(1);
        scanListOfValidYears(scanner, yearsToEvaluate, tournamentParticipantDao);
        log.debug("Evaluation data year(s): " + StringUtils.join(yearsToEvaluate, ','));
    }

    public static void scanListOfValidYears(final BufferedReader scanner,
                                            final List<Integer> listOfYears,
                                            final TournamentParticipantDao tournamentParticipantDao) {
        try {
            String userInput = null;
            while (StringUtils.isEmpty(userInput)) {
                if (!listOfYears.isEmpty()) {
                    System.out.println("Press enter to keep your previous selection of " + StringUtils.join(listOfYears, ','));
                }
                // Get user input - a comma-separated list of years
                userInput = scanner.readLine();
                if (StringUtils.isNotEmpty(userInput)) {
                    listOfYears.clear();
                    // Break it up into tokens
                    StringTokenizer strtok = new StringTokenizer(userInput, ",");
                    while (strtok.hasMoreTokens()) {
                        Integer year = Integer.valueOf(strtok.nextToken().trim());
                        // Validate year
                        NetworkUtils.validateYear(year, tournamentParticipantDao);
                        listOfYears.add(year);
                    }
                } else if (!listOfYears.isEmpty()) {
                    break;
                } else {
                    System.out.println("==> "); // Prompt
                }
            }
        } catch (Exception e) {
            String message = String.format("Error getting years for training: %s", e.getLocalizedMessage());
            log.error(message);
            throw new RuntimeException(message, e);
        }
    }

    public static String scanNetworkLayout(final BufferedReader scanner,
                                           final String networkLayout,
                                           final Integer numberOfInputs,
                                           final Integer numberOfOutputs) throws IOException {
        String ret = (StringUtils.isNotEmpty(networkLayout)) ? networkLayout : NetworkUtils.computeNetworkStructure(numberOfInputs, numberOfOutputs);
        String userInput = null;
        while (StringUtils.isEmpty(userInput)) {
            System.out.println("Enter the network structure (for example, " + ret + ")");
            System.out.printf("Note: only specify the hidden layer structure. Input layer (%d neurons) and output layer (%d neurons) are already specified.%n",
                    numberOfInputs, numberOfOutputs);
            System.out.println("==> ");
            int[] existingNetworkLayout = NetworkUtils.parseNetworkStructure(ret);
            // Strip off the first layer and set it to the number of inputs (if the user has added or removed data elements the previous number of inputs will be wrong)
            existingNetworkLayout[0] = numberOfInputs;
            ret = StringUtils.join(existingNetworkLayout, 'x');
            System.out.println("Press enter to use " + ret + ", enter a new one below, or press r to regenerate the suggested layout:");
            userInput = scanner.readLine();
            if (StringUtils.isEmpty(userInput)) {
                System.out.println("Using: " + ret + "....");
                break;
            } else if (StringUtils.isNotEmpty(userInput)) {
                if (userInput.equalsIgnoreCase("r")) {
                    ret = NetworkUtils.computeNetworkStructure(numberOfInputs, numberOfOutputs);
                    userInput = null; // keep looping
                } else {
                    ret = String.format("%dx%sx%d", numberOfInputs, StringUtils.join(NetworkUtils.parseNetworkStructure(userInput), 'x'), numberOfOutputs);
                }
            } else {
                System.out.println("==> ");
            }
        }
        log.info("Network layout: " + ret);
        return ret;
    }

    public static Activation scanActivationFunction(final BufferedReader scanner, final Activation activation) throws IOException {
        Activation ret = null;
        while (ret == null) {
            Optional<ActivationFunctionMenu.ActivationFunctionChoice> menuChoice = ActivationFunctionMenu.go(scanner, activation);
            if (menuChoice.isPresent()) {
                ret = menuChoice.get().getActivation();
            }
        }
        return ret;
    }

    public static IUpdater scanUpdaterFunction(final BufferedReader scanner,
                                               final IUpdater updaterChoice) throws IOException {
        IUpdater ret = null;
        while (ret == null) {
            Optional<IUpdater> menuChoice = UpdaterMenu.go(scanner, updaterChoice);
            if (menuChoice.isPresent()) {
                ret = menuChoice.get();
            }
        }
        return ret;
    }

    public static WeightInit scanWeightInitFunction(final BufferedReader scanner, final WeightInit weightInit) throws IOException {
        WeightInit ret = null;
        while (ret == null) {
            Optional<WeightInitMenu.WeightInitChoice> menuChoice = WeightInitMenu.go(scanner, weightInit);
            if (menuChoice.isPresent()) {
                ret = menuChoice.get().getWeightInit();
            }
        }
        return ret;
    }

    public static Integer scanNumberOfEpochs(final BufferedReader scanner, final Integer numberOfEpochs) throws IOException {
        Integer ret = numberOfEpochs;
        String userInput = null;
        while (StringUtils.isEmpty(userInput)) {
            System.out.println("Enter the number of epochs: ");
            if (numberOfEpochs != null) {
                System.out.println("Press enter to use the number of epochs you used last time: " + numberOfEpochs);
            }
            userInput = scanner.readLine();
            if (StringUtils.isEmpty(userInput)) {
                break;
            } else if (StringUtils.isNumeric(userInput)) {
                ret = Integer.parseInt(userInput);
            } else {
                String message = String.format("Not a valid value for number of epochs: %s", userInput);
                log.warn(message);
                ret = null;
            }
        }
        return ret;
    }

    public static LossFunctions.LossFunction scanLossFunction(final BufferedReader scanner, final LossFunctions.LossFunction lossFunction) throws IOException {
        LossFunctions.LossFunction ret = null;
        while (ret == null) {
            Optional<LossFunctionMenu.LossFunctionChoice> menuChoice = LossFunctionMenu.go(scanner, lossFunction);
            if (menuChoice.isPresent()) {
                ret = menuChoice.get().getLossFunction();
            }
        }
        return ret;
    }

    public static Double scanL2RegularizationLambda(final BufferedReader scanner, final Double l2RegularizationLambda) throws IOException {
        Double ret = l2RegularizationLambda;
        String userInput = null;
        while (StringUtils.isEmpty(userInput)) {
            System.out.println("Enter the L2 Regularization Lambda value: ");
            if (l2RegularizationLambda != null) {
                System.out.println("Press enter to use the value you used last time: " + l2RegularizationLambda);
            }
            userInput = scanner.readLine();
            if (StringUtils.isEmpty(userInput)) {
                break;
            } else if (StringUtils.isNumeric(StringUtils.remove(userInput, '.'))) {
                ret = Double.parseDouble(userInput);
            } else {
                String message = String.format("Not a valid value for L2 Regularization Lambda value: %s", userInput);
                log.warn(message);
                ret = null;
            }
        }
        return ret;
    }

    public static DataNormalizationMenu.DataNormalizationChoice scanDataNormalization(final BufferedReader scanner,
                                                                                      final DataNormalizationMenu.DataNormalizationChoice dataNormalization) throws IOException {
        DataNormalizationMenu.DataNormalizationChoice ret = null;
        while (ret == null) {
            Optional<DataNormalizationMenu.DataNormalizationChoice> menuChoice = DataNormalizationMenu.go(scanner, dataNormalization);
            if (menuChoice.isPresent()) {
                ret = menuChoice.get();
            }
        }
        return ret;
    }

    /**
     * Specifies the Updaters that are to be used for batch training runs.
     */
    public static List<UpdaterMenu.UpdaterChoice> fetchUpdaterChoices() {
        List<UpdaterMenu.UpdaterChoice> ret = NetworkProperties.getUpdaters();
        if (ret.isEmpty()) {
            ret.add(UpdaterMenu.UpdaterChoice.AMS_GRAD);
            ret.add(UpdaterMenu.UpdaterChoice.ADA_MAX);
            ret.add(UpdaterMenu.UpdaterChoice.ADAM);
            ret.add(UpdaterMenu.UpdaterChoice.SGD);
        }
        return ret;
    }

    public static List<ActivationFunctionMenu.ActivationFunctionChoice> fetchActivationChoices() {
        List<ActivationFunctionMenu.ActivationFunctionChoice> ret = NetworkProperties.getActivationFunctions();
        if (ret.isEmpty()) {
            ret.add(ActivationFunctionMenu.ActivationFunctionChoice.RELU);
            ret.add(ActivationFunctionMenu.ActivationFunctionChoice.RATIONALTANH);
            ret.add(ActivationFunctionMenu.ActivationFunctionChoice.RELU6);
            ret.add(ActivationFunctionMenu.ActivationFunctionChoice.SOFTMAX);
            ret.add(ActivationFunctionMenu.ActivationFunctionChoice.TANH);
            ret.add(ActivationFunctionMenu.ActivationFunctionChoice.SIGMOID);
        }
        return ret;
    }


}
