package com.jstevenperry.sportsnet.framework.menus;

import org.apache.commons.lang3.StringUtils;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerMinMaxScaler;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Optional;

public class DataNormalizationMenu {

  private static final Logger log = LoggerFactory.getLogger(DataNormalizationMenu.class);

  public static Optional<DataNormalizationChoice> go(final BufferedReader scanner,
                                                     final DataNormalizationChoice dataNormalization) throws IOException {
    String thing = "normalizer";
    Optional<DataNormalizationChoice> ret = Optional.ofNullable(dataNormalization);
    String userInput = null;
    while (StringUtils.isEmpty(userInput)) {
      System.out.printf("Enter the number of the %s you want to use: %n", thing);
      if (dataNormalization != null) {
        System.out.printf("Press enter to use the %s you used last time: %s%n", thing, dataNormalization);
      }
      displayMenuItems(DataNormalizationChoice.values());
      userInput = scanner.readLine();
      if (StringUtils.isEmpty(userInput)) {
        // User wants their previous choice
        break;
      } else if (StringUtils.isNumeric(userInput)) {
        int userChoice = Integer.parseInt(userInput);
        Optional<DataNormalizationChoice> menuChoice = DataNormalizationChoice.from(userChoice);
        if (menuChoice.isPresent()) {
          log.debug("You chose normalizer: " + menuChoice.get());
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

  private static void displayMenuItems(DataNormalizationChoice[] choices) {
    int numberOfColumns = 3;
    int numberOfRows = choices.length / numberOfColumns + choices.length % numberOfColumns;
    for (int row = 0; row < numberOfRows; row++) {
      for (int column = 0; column < numberOfColumns; column++) {
        int index = row + (column * numberOfRows);
        if (index < choices.length) {
          DataNormalizationChoice choice = choices[index];
          System.out.printf("%2d - %30s", choice.getOrdinal(), choice.name());
        }
        System.out.print("\t\t");
      }
      System.out.print('\n');
    }
  }

  public enum DataNormalizationChoice {
    NORMALIZER_STANDARDIZE(new NormalizerStandardize(), 1),
    NORMALIZER_ZERO_TO_ONE(new NormalizerMinMaxScaler(0, 1), 2),
    NORMALIZER_NEGATIVE_ONE_TO_ONE(new NormalizerMinMaxScaler(-1, 1), 3),
    ;

    private final DataNormalization dataNormalization;
    private final int ordinal;

    DataNormalizationChoice(final DataNormalization dataNormalization, final int ordinal) {

      this.dataNormalization = dataNormalization;
      this.ordinal = ordinal;
    }

    private static Optional<DataNormalizationChoice> from(final int choice) {
      Optional<DataNormalizationChoice> ret = Optional.empty();
      for (DataNormalizationChoice menuChoice : DataNormalizationChoice.values()) {
        if (menuChoice.getOrdinal() == choice) {
          ret = Optional.of(menuChoice);
          break;
        }
      }
      return ret;
    }

    private static Optional<DataNormalizationChoice> from(final DataNormalization dataNormalization) {
      Optional<DataNormalizationChoice> ret = Optional.empty();
      for (DataNormalizationChoice menuChoice : DataNormalizationChoice.values()) {
        if (menuChoice.getDataNormalization().equals(dataNormalization)) {
          ret = Optional.of(menuChoice);
          break;
        }
      }
      return ret;
    }

    public DataNormalization getDataNormalization() {
      return dataNormalization;
    }

    public int getOrdinal() {
      return ordinal;
    }

  }
}
