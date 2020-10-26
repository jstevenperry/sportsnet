package com.jstevenperry.sportsnet.framework.menus;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A user may include one or more Data Elements in the
 * data they use to train their networks.
 * <p>
 * A subclass of this class represents a single one of those
 * Data Elements.
 */
public abstract class AbstractDataElementMenu {

  private static final Logger log = LoggerFactory.getLogger(AbstractDataElementMenu.class);

  public abstract List<AbstractDataElementChoice> values();

  public abstract Optional<AbstractDataElementChoice> from(int ordinalValue);

  public List<AbstractDataElementChoice> go(final BufferedReader scanner, final List<AbstractDataElementChoice> selectedElements) throws IOException {
    String thing = "data element";
    List<AbstractDataElementChoice> ret = (selectedElements == null) ? new ArrayList<>() : selectedElements;
    String userInput;
    // Loop until the user finishes
    while (true) {
      if (!ret.isEmpty()) {
        System.out.println("******* CURRENT CHOICES *******");
        ret.sort(Comparator.comparingInt(AbstractDataElementChoice::getOrdinalValue));
        displayMenuItems(ret);
        System.out.println("(To use these choices just press enter)");
        System.out.printf("To remove a choice enter -NUMBER (for example, -%d removes %s from your selections)%n", ret.get(0).getOrdinalValue(), ret.get(0).getName());
      }
      System.out.printf("Enter the number of the %s you want to select (enter 0 to quit): %n", thing);
      displayMenuItems(filter(values(), ret));
      userInput = scanner.readLine();
      if (StringUtils.isEmpty(userInput)) {
        // User wants their previous choices, we're done here
        break;
      } else if (isNumeric(userInput)) {
        int userChoice = Integer.parseInt(userInput);
        if (userChoice == 0) {
          // Zero means quit, this is just for consistency with other menus, hitting enter works too
          break;
        }
        processUserChoice(ret, userChoice);
      } else if (userInput.equalsIgnoreCase("ALL")) {
        //
        // Clear out the current choices, and add all values
        ret.clear();
        ret.addAll(values());
      } else{
        String message = String.format("Not a valid value: %s", userInput);
        log.warn(message);
      }
    }
    return ret;
  }

  private static boolean isNumeric(final String userInput) {
    return StringUtils.isNumeric(userInput) ||
      userInput.startsWith("-") && StringUtils.isNumeric(StringUtils.substring(userInput, 1))
      ;
  }

  private List<AbstractDataElementChoice> filter(final List<AbstractDataElementChoice> values,
                                                 final List<AbstractDataElementChoice> selectedElements) {
    List<AbstractDataElementChoice> ret = values;
    if (selectedElements != null && !selectedElements.isEmpty()) {
      ret = values.stream().filter(dataElementMenuChoice -> !selectedElements.contains(dataElementMenuChoice))
        .collect(Collectors.toList());
    }
    return ret;
  }

  private void processUserChoice(final List<AbstractDataElementChoice> dataElementMenuChoices, int userChoice) {
    boolean removeChoice = false;
    // If the choice is < 0, then the user wants to remove that choice
    if (userChoice < 0) {
      userChoice *= -1;
      removeChoice = true;
    }
    Optional<AbstractDataElementChoice> menuChoice = from(userChoice);
    if (menuChoice.isPresent()) {
      AbstractDataElementChoice dataElementMenuChoice = menuChoice.get();
      if (removeChoice) {
        log.debug("You chose to remove element: " + dataElementMenuChoice);
        dataElementMenuChoices.remove(dataElementMenuChoice);
      } else if (dataElementMenuChoices.contains(dataElementMenuChoice)) {
        System.out.printf("You already chose %s. To remove it use -%d%n.", dataElementMenuChoice.getName(), dataElementMenuChoice.getOrdinalValue());
      } else {
        log.debug("You chose element: " + menuChoice.get());
        dataElementMenuChoices.add(dataElementMenuChoice);
      }
    } else {
      System.out.printf("%d is not a valid choice, please choose from the list.%n", userChoice);
    }
  }

  private void displayMenuItems(List<AbstractDataElementChoice> choices) {
    int numberOfColumns = 3;
    int numberOfRows = choices.size() / numberOfColumns + choices.size() % numberOfColumns;
    for (int row = 0; row < numberOfRows; row++) {
      for (int column = 0; column < numberOfColumns; column++) {
        int index = row + (column * numberOfRows);
        if (index < choices.size()) {
          AbstractDataElementChoice choice = choices.get(index);
          System.out.printf("%2d - %48s", choice.getOrdinalValue(), choice.getName());
        }
        System.out.print("\t\t");
      }
      System.out.print('\n');
    }
  }


}
