package com.jstevenperry.sportsnet.framework.menus;

import org.apache.commons.lang3.StringUtils;
import org.deeplearning4j.nn.weights.WeightInit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Optional;

public class WeightInitMenu {

  private static final Logger log = LoggerFactory.getLogger(WeightInitMenu.class);

  public static Optional<WeightInitChoice> go(final BufferedReader scanner, final WeightInit weightInit) throws IOException {
    String thing = "weight initialization function";
    Optional<WeightInitChoice> ret = WeightInitChoice.from(weightInit);
    String userInput = null;
    while (StringUtils.isEmpty(userInput)) {
      System.out.printf("Enter the number of the %s you want to use: %n", thing);
      if (weightInit != null) {
        System.out.printf("Press enter to use the %s you used last time: %s%n", thing, weightInit);
      }
      displayMenuItems(WeightInitChoice.values());
      userInput = scanner.readLine();
      if (StringUtils.isEmpty(userInput)) {
        // User wants their previous choice
        break;
      } else if (StringUtils.isNumeric(userInput)) {
        int userChoice = Integer.parseInt(userInput);
        Optional<WeightInitChoice> menuChoice = WeightInitChoice.from(userChoice);
        if (menuChoice.isPresent()) {
          log.debug("You chose weight initialization function: " + menuChoice.get());
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

  private static void displayMenuItems(WeightInitChoice[] choices) {
    int numberOfColumns = 4;
    int numberOfRows = choices.length / numberOfColumns + choices.length % numberOfColumns;
    for (int row = 0; row < numberOfRows; row++) {
      for (int column = 0; column < numberOfColumns; column++) {
        int index = row + (column * numberOfRows);
        if (index < choices.length) {
          WeightInitChoice choice = choices[index];
          System.out.printf("%2d - %20s", choice.getOrdinal(), choice.name());
        }
        System.out.print("\t\t");
      }
      System.out.print('\n');
    }
  }


  public enum WeightInitChoice {
    DISTRIBUTION(WeightInit.DISTRIBUTION, 1),
    ZERO(WeightInit.ZERO, 2),
    ONES(WeightInit.ONES, 3),
    SIGMOID_UNIFORM(WeightInit.SIGMOID_UNIFORM, 4),
    NORMAL(WeightInit.NORMAL, 5),
    LECUN_NORMAL(WeightInit.LECUN_NORMAL, 6),
    UNIFORM(WeightInit.UNIFORM, 7),
    XAVIER(WeightInit.XAVIER, 8),
    XAVIER_UNIFORM(WeightInit.XAVIER_UNIFORM, 9),
    XAVIER_FAN_IN(WeightInit.XAVIER_FAN_IN, 10),
    XAVIER_LEGACY(WeightInit.XAVIER_LEGACY, 11),
    RELU(WeightInit.RELU, 12),
    RELU_UNIFORM(WeightInit.RELU_UNIFORM, 13),
    IDENTITY(WeightInit.IDENTITY, 14),
    LECUN_UNIFORM(WeightInit.LECUN_UNIFORM, 15),
    VAR_SCALING_NORMAL_FAN_IN(WeightInit.VAR_SCALING_NORMAL_FAN_IN, 16),
    VAR_SCALING_NORMAL_FAN_OUT(WeightInit.VAR_SCALING_NORMAL_FAN_OUT, 17),
    VAR_SCALING_NORMAL_FAN_AVG(WeightInit.VAR_SCALING_NORMAL_FAN_AVG, 18),
    VAR_SCALING_UNIFORM_FAN_IN(WeightInit.VAR_SCALING_UNIFORM_FAN_IN, 19),
    VAR_SCALING_UNIFORM_FAN_OUT(WeightInit.VAR_SCALING_UNIFORM_FAN_OUT, 20),
    VAR_SCALING_UNIFORM_FAN_AVG(WeightInit.VAR_SCALING_UNIFORM_FAN_AVG, 21);

    private final WeightInit weightInit;
    private final int ordinal;

    WeightInitChoice(final WeightInit weightInit, final int ordinal) {
      this.weightInit = weightInit;
      this.ordinal = ordinal;
    }

    public static Optional<WeightInitChoice> from(final int choice) {
      Optional<WeightInitChoice> ret = Optional.empty();
      for (WeightInitChoice menuChoice : WeightInitChoice.values()) {
        if (menuChoice.getOrdinal() == choice) {
          ret = Optional.of(menuChoice);
          break;
        }
      }
      return ret;
    }

    public static Optional<WeightInitChoice> from(final WeightInit weightInit) {
      Optional<WeightInitChoice> ret = Optional.empty();
      for (WeightInitChoice menuChoice : WeightInitChoice.values()) {
        if (menuChoice.getWeightInit().equals(weightInit)) {
          ret = Optional.of(menuChoice);
          break;
        }
      }
      return ret;
    }

    public WeightInit getWeightInit() {
      return weightInit;
    }

    public int getOrdinal() {
      return ordinal;
    }

  }
}
