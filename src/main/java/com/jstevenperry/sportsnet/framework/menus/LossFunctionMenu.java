/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.menus;

import org.apache.commons.lang3.StringUtils;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Optional;

public class LossFunctionMenu {

    private static final Logger log = LoggerFactory.getLogger(LossFunctionMenu.class);

    public static Optional<LossFunctionChoice> go(final BufferedReader scanner, final LossFunctions.LossFunction lossFunction) throws IOException {
        String thing = "loss function";
        Optional<LossFunctionChoice> ret = LossFunctionChoice.from(lossFunction);
        String userInput = null;
        while (StringUtils.isEmpty(userInput)) {
            System.out.printf("Enter the number of the %s you want to use: %n", thing);
            if (lossFunction != null) {
                System.out.printf("Press enter to use the %s you used last time: %s%n", thing, lossFunction);
            }
            displayMenuItems(LossFunctionChoice.values());
            userInput = scanner.readLine();
            if (StringUtils.isEmpty(userInput)) {
                // User wants their previous choice
                break;
            } else if (StringUtils.isNumeric(userInput)) {
                int userChoice = Integer.parseInt(userInput);
                Optional<LossFunctionChoice> menuChoice = LossFunctionChoice.from(userChoice);
                if (menuChoice.isPresent()) {
                    log.debug("You chose loss function: " + menuChoice.get());
                    ret = menuChoice;
                } else {
                    System.out.printf("%d is not a valid choice, please choose from the list.%n", userChoice);
                    userInput = null;
                }
            } else {
                String message = String.format("Not a valid value: %s", userInput);
                log.warn(message);
                userInput = null;
            }
        }
        return ret;
    }

    private static void displayMenuItems(LossFunctionChoice[] choices) {
        int numberOfColumns = 3;
        int numberOfRows = choices.length / numberOfColumns + choices.length % numberOfColumns;
        for (int row = 0; row < numberOfRows; row++) {
            for (int column = 0; column < numberOfColumns; column++) {
                int index = row + (column * numberOfRows);
                if (index < choices.length) {
                    LossFunctionChoice choice = choices[index];
                    System.out.printf("%2d - %30s", choice.getOrdinal(), choice.name());
                }
                System.out.print("\t\t");
            }
            System.out.print('\n');
        }
    }

    public enum LossFunctionChoice {
        MSE(LossFunctions.LossFunction.MSE, 1),
        L1(LossFunctions.LossFunction.L1, 2),
        XENT(LossFunctions.LossFunction.XENT, 3),
        MCXENT(LossFunctions.LossFunction.MCXENT, 4),
        SPARSE_MCXENT(LossFunctions.LossFunction.SPARSE_MCXENT, 5),
        SQUARED_LOSS(LossFunctions.LossFunction.SQUARED_LOSS, 6),
        RECONSTRUCTION_CROSSENTROPY(LossFunctions.LossFunction.RECONSTRUCTION_CROSSENTROPY, 7),
        NEGATIVELOGLIKELIHOOD(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD, 8),
        COSINE_PROXIMITY(LossFunctions.LossFunction.COSINE_PROXIMITY, 9),
        HINGE(LossFunctions.LossFunction.HINGE, 10),
        SQUARED_HINGE(LossFunctions.LossFunction.SQUARED_HINGE, 11),
        KL_DIVERGENCE(LossFunctions.LossFunction.KL_DIVERGENCE, 12),
        MEAN_ABSOLUTE_ERROR(LossFunctions.LossFunction.MEAN_ABSOLUTE_ERROR, 13),
        L2(LossFunctions.LossFunction.L2, 14),
        MEAN_ABSOLUTE_PERCENTAGE_ERROR(LossFunctions.LossFunction.MEAN_ABSOLUTE_PERCENTAGE_ERROR, 15),
        MEAN_SQUARED_LOGARITHMIC_ERROR(LossFunctions.LossFunction.MEAN_SQUARED_LOGARITHMIC_ERROR, 16),
        POISSON(LossFunctions.LossFunction.POISSON, 17),
        WASSERSTEIN(LossFunctions.LossFunction.WASSERSTEIN, 18);

        private final LossFunctions.LossFunction lossFunction;
        private final int ordinal;

        LossFunctionChoice(final LossFunctions.LossFunction lossFunction, final int ordinal) {

            this.lossFunction = lossFunction;
            this.ordinal = ordinal;
        }

        private static Optional<LossFunctionChoice> from(final int choice) {
            Optional<LossFunctionChoice> ret = Optional.empty();
            for (LossFunctionChoice menuChoice : LossFunctionChoice.values()) {
                if (menuChoice.getOrdinal() == choice) {
                    ret = Optional.of(menuChoice);
                    break;
                }
            }
            return ret;
        }

        private static Optional<LossFunctionChoice> from(final LossFunctions.LossFunction lossFunction) {
            Optional<LossFunctionChoice> ret = Optional.empty();
            for (LossFunctionChoice menuChoice : LossFunctionChoice.values()) {
                if (menuChoice.getLossFunction().equals(lossFunction)) {
                    ret = Optional.of(menuChoice);
                    break;
                }
            }
            return ret;
        }

        public LossFunctions.LossFunction getLossFunction() {
            return lossFunction;
        }

        public int getOrdinal() {
            return ordinal;
        }

    }
}
