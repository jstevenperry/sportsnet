/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.menus;

import com.jstevenperry.sportsnet.framework.util.NetworkProperties;
import org.apache.commons.lang3.StringUtils;
import org.nd4j.linalg.learning.config.AMSGrad;
import org.nd4j.linalg.learning.config.AdaDelta;
import org.nd4j.linalg.learning.config.AdaGrad;
import org.nd4j.linalg.learning.config.AdaMax;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.learning.config.IUpdater;
import org.nd4j.linalg.learning.config.Nadam;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.learning.config.NoOp;
import org.nd4j.linalg.learning.config.RmsProp;
import org.nd4j.linalg.learning.config.Sgd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;

public class UpdaterMenu {

    private static final Logger log = LoggerFactory.getLogger(UpdaterMenu.class);

    public static Optional<IUpdater> go(final BufferedReader scanner, final IUpdater updaterChoice) throws IOException {
        String thing = "weight updater function";
        Optional<IUpdater> ret = Optional.ofNullable(updaterChoice);
        String userInput = null;
        while (StringUtils.isEmpty(userInput)) {
            System.out.printf("Enter the number of the %s you want to use: %n", thing);
            if (updaterChoice != null) {
                System.out.printf("Press enter to use the %s you used last time: %s%n", thing, updaterChoice);
            }
            displayMenuItems(UpdaterChoice.values());
            userInput = scanner.readLine();
            if (StringUtils.isEmpty(userInput)) {
                // User wants their previous choice
                break;
            } else if (StringUtils.isNumeric(userInput)) {
                int userChoice = Integer.parseInt(userInput);
                Optional<UpdaterChoice> menuChoice = UpdaterChoice.from(userChoice);
                if (menuChoice.isPresent()) {
                    log.debug("You chose updater function: " + menuChoice.get());
                    ret = Optional.ofNullable(menuChoice.get().getUpdater());
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

    private static void displayMenuItems(UpdaterChoice[] choices) {
        int numberOfColumns = 3;
        int numberOfRows = choices.length / numberOfColumns + choices.length % numberOfColumns;
        for (int row = 0; row < numberOfRows; row++) {
            for (int column = 0; column < numberOfColumns; column++) {
                int index = row + (column * numberOfRows);
                if (index < choices.length) {
                    UpdaterChoice choice = choices[index];
                    System.out.printf("%2d - %10s", choice.getOrdinal(), choice.name());
                }
                System.out.print("\t\t");
            }
            System.out.print('\n');
        }
    }

    public enum UpdaterChoice {
        ADA_DELTA(AdaDelta::new, 1),
        ADA_GRAD(() -> new AdaGrad(scale(NetworkProperties.getLearningRuleLearningRate(), 0.1), NetworkProperties.getLearningRuleLearningRate() / 1e5), 2),
        ADAM(() -> new Adam(scale(NetworkProperties.getLearningRuleLearningRate(), 0.001), Adam.DEFAULT_ADAM_BETA1_MEAN_DECAY, Adam.DEFAULT_ADAM_BETA2_VAR_DECAY, NetworkProperties.getLearningRuleLearningRate() / 1e5), 3),
        ADA_MAX(() -> new AdaMax(scale(NetworkProperties.getLearningRuleLearningRate(), 0.001), AdaMax.DEFAULT_ADAMAX_BETA1_MEAN_DECAY, AdaMax.DEFAULT_ADAMAX_BETA2_VAR_DECAY, NetworkProperties.getLearningRuleLearningRate() / 1e5), 4),
        AMS_GRAD(() -> new AMSGrad(scale(NetworkProperties.getLearningRuleLearningRate(), 0.001), AMSGrad.DEFAULT_AMSGRAD_BETA1_MEAN_DECAY, AMSGrad.DEFAULT_AMSGRAD_BETA2_VAR_DECAY, NetworkProperties.getLearningRuleLearningRate() / 1e5), 5),
        NADAM(() -> new Nadam(scale(NetworkProperties.getLearningRuleLearningRate(), 0.001), Nadam.DEFAULT_NADAM_BETA1_MEAN_DECAY, Nadam.DEFAULT_NADAM_BETA2_VAR_DECAY, NetworkProperties.getLearningRuleLearningRate() / 1e5), 6),
        NESTEROVS(() -> new Nesterovs(scale(NetworkProperties.getLearningRuleLearningRate(), 0.1), NetworkProperties.getLearningRuleMomentumMax()), 7),
        NOOP(NoOp::new, 8),
        RMS_PROP(() -> new RmsProp(scale(NetworkProperties.getLearningRuleLearningRate(), 0.1)), 9),
        SGD(() -> new Sgd(scale(NetworkProperties.getLearningRuleLearningRate(), 0.001)), 10);

        /**
         * Scale the specified valueToScale by the specified scale value. This is used
         * to scale values like learning rate and epsilon so that single value may
         * be specified in the configuration properties that can then be scaled to work
         * for the wildly inconsistent values that are used across implementations
         * in the DL4J framework.
         * <p>
         * The idea is to specify one value in the properties whose "normal" value is
         * 1 and can then be scaled from there since there is no consistency among
         * the various DL4J implementations (such is the nature of open source projects
         * and software development in general... poor communication)
         */
        private static double scale(final Double valueToScale, final double scaleValue) {
            return valueToScale * scaleValue;
        }

        private final Supplier<IUpdater> updaterSupplier;
        private final int ordinal;

        UpdaterChoice(final Supplier<IUpdater> updaterCreator, final int ordinal) {
            this.updaterSupplier = updaterCreator;
            this.ordinal = ordinal;
        }

        private static Optional<UpdaterChoice> from(final int choice) {
            Optional<UpdaterChoice> ret = Optional.empty();
            for (UpdaterChoice menuChoice : UpdaterChoice.values()) {
                if (menuChoice.getOrdinal() == choice) {
                    ret = Optional.of(menuChoice);
                    break;
                }
            }
            return ret;
        }

        public static Optional<UpdaterChoice> from(final IUpdater updater) {
            Optional<UpdaterChoice> ret = Optional.empty();
            for (UpdaterChoice menuChoice : UpdaterChoice.values()) {
                if (menuChoice.getUpdater().getClass().equals(updater.getClass())) {
                    log.debug("from({}): Found matching IUpdater, returning UpdaterChoice: {}", updater.getClass(), menuChoice);
                    ret = Optional.of(menuChoice);
                    break;
                }
            }
            return ret;
        }

        public IUpdater getUpdater() {
            return updaterSupplier.get();
        }

        public int getOrdinal() {
            return ordinal;
        }

        @Override
        public String toString() {
            return String.format("%s (%1.5f)", name(), getUpdater().getLearningRate(0, 0));
        }
    }
}
