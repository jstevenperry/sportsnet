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

public class UpdaterMenu {

  private static final Logger log = LoggerFactory.getLogger(UpdaterMenu.class);

  public static Optional<UpdaterChoice> go(final BufferedReader scanner, final UpdaterChoice updaterChoice) throws IOException {
    String thing = "weight updater function";
    Optional<UpdaterChoice> ret = Optional.ofNullable(updaterChoice);
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
    ADA_DELTA(new AdaDelta(), 1),
    ADA_GRAD(new AdaGrad(NetworkProperties.getLearningRuleLearningRate()), 2),
    ADAM(new Adam(NetworkProperties.getLearningRuleLearningRate()), 3),
    ADA_MAX(new AdaMax(NetworkProperties.getLearningRuleLearningRate()), 4),
    AMS_GRAD(new AMSGrad(NetworkProperties.getLearningRuleLearningRate()), 5),
    NADAM(new Nadam(NetworkProperties.getLearningRuleLearningRate()), 6),
    NESTEROVS(new Nesterovs(NetworkProperties.getLearningRuleLearningRate(), NetworkProperties.getLearningRuleMomentumMax()), 7),
    NOOP(new NoOp(), 8),
    RMS_PROP(new RmsProp(NetworkProperties.getLearningRuleLearningRate()), 9),
    SGD(new Sgd(NetworkProperties.getLearningRuleLearningRate()), 10);

    private final IUpdater updater;
    private final int ordinal;

    UpdaterChoice(final IUpdater updater, final int ordinal) {
      this.updater = updater;
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

    private static Optional<UpdaterChoice> from(final IUpdater updater) {
      Optional<UpdaterChoice> ret = Optional.empty();
      for (UpdaterChoice menuChoice : UpdaterChoice.values()) {
        if (menuChoice.getUpdater().equals(updater)) {
          ret = Optional.of(menuChoice);
          break;
        }
      }
      return ret;
    }

    public IUpdater getUpdater() {
      return updater;
    }

    public int getOrdinal() {
      return ordinal;
    }

    @Override
    public String toString() {
      return String.format("%s (%1.2f)", name(), getUpdater().getLearningRate(0, 0));
    }
  }
}
