package com.jstevenperry.sportsnet.ncaabb.menus;

import com.jstevenperry.sportsnet.framework.menus.AbstractDataElementChoice;
import com.jstevenperry.sportsnet.framework.menus.AbstractDataElementMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Optional;

public class DataElementMenu extends AbstractDataElementMenu {

  private static final Logger log = LoggerFactory.getLogger(DataElementMenuChoiceEnum.class);

  @Override
  public AbstractDataElementChoice[] values() {
    return Arrays.stream(DataElementMenuChoiceEnum.values())
      .map(DataElementMenuChoiceEnum::getDataElementChoice)
      .toArray(AbstractDataElementChoice[]::new);
  }

  @Override
  public Optional<AbstractDataElementChoice> from(final int ordinalValue) {
    Optional<DataElementMenuChoiceEnum> dataElementMenuChoiceEnumMaybe = DataElementMenuChoiceEnum.from(ordinalValue);
    return dataElementMenuChoiceEnumMaybe.map(DataElementMenuChoiceEnum::getDataElementChoice);
  }

  public enum DataElementMenuChoiceEnum {
    // Offense
    // The ordinal corresponds to the index in the input data where that element is located.
    // See NetworkParameters.transformRow()
    AVERAGE_POINTS_PER_GAME(new DataElementChoice("AVERAGE_POINTS_PER_GAME", 0, 1)),
    SCORING_MARGIN_PER_GAME(new DataElementChoice("SCORING_MARGIN_PER_GAME", 1, 2)),
    NUMBER_FIELD_GOAL_ATTEMPTS_PER_GAME(new DataElementChoice("NUMBER_FIELD_GOAL_ATTEMPTS_PER_GAME", 2, 3)),
    FIELD_GOAL_PERCENTAGE(new DataElementChoice("FIELD_GOAL_PERCENTAGE", 3, 4)),
    NUMBER_THREE_POINTERS_PER_GAME(new DataElementChoice("NUMBER_THREE_POINTERS_PER_GAME", 4, 5)),
    NUMBER_THREE_POINT_ATTEMPTS_PER_GAME(new DataElementChoice("NUMBER_THREE_POINT_ATTEMPTS_PER_GAME", 5, 6)),
    THREE_POINT_PERCENTAGE(new DataElementChoice("THREE_POINT_PERCENTAGE", 6, 7)),
    NUMBER_FREE_THROW_ATTEMPTS_PER_GAME(new DataElementChoice("NUMBER_FREE_THROW_ATTEMPTS_PER_GAME", 7, 8)),
    FREE_THROW_PERCENTAGE(new DataElementChoice("FREE_THROW_PERCENTAGE", 8, 9)),
    REBOUND_MARGIN(new DataElementChoice("REBOUND_MARGIN", 9, 10)),
    ASSISTS_PER_GAME(new DataElementChoice("ASSISTS_PER_GAME", 10, 11)),
    ASSIST_TO_TURNOVER_RATIO(new DataElementChoice("ASSIST_TO_TURNOVER_RATIO", 11, 12)),
    // Defense
    AVERAGE_OPPONENT_POINTS_PER_GAME(new DataElementChoice("AVERAGE_OPPONENT_POINTS_PER_GAME", 12, 13)),
    NUMBER_OPPONENT_FIELD_GOAL_ATTEMPTS_PER_GAME(new DataElementChoice("NUMBER_OPPONENT_FIELD_GOAL_ATTEMPTS_PER_GAME", 13, 14)),
    OPPONENT_FIELD_GOAL_PERCENTAGE(new DataElementChoice("OPPONENT_FIELD_GOAL_PERCENTAGE", 14, 15)),
    NUMBER_OPPONENT_THREE_POINT_ATTEMPTS_PER_GAME(new DataElementChoice("NUMBER_OPPONENT_THREE_POINT_ATTEMPTS_PER_GAME", 15, 16)),
    OPPONENT_THREE_POINT_PERCENTAGE(new DataElementChoice("OPPONENT_THREE_POINT_PERCENTAGE", 16, 17)),
    BLOCKS_PER_GAME(new DataElementChoice("BLOCKS_PER_GAME", 17, 18)),
    STEALS_PER_GAME(new DataElementChoice("STEALS_PER_GAME", 18, 19)),
    OPPONENT_TURNOVERS_PER_GAME(new DataElementChoice("OPPONENT_TURNOVERS_PER_GAME", 19, 20)),
    // Errors
    TURNOVERS_PER_GAME(new DataElementChoice("TURNOVERS_PER_GAME", 20, 21)),
    FOULS_PER_GAME(new DataElementChoice("FOULS_PER_GAME", 21, 22)),
    NUMBER_DISQUALIFICATIONS(new DataElementChoice("NUMBER_DISQUALIFICATIONS", 22, 23)),
    //
    WON_LOST_PERCENTAGE(new DataElementChoice("WON_LOST_PERCENTAGE", 23, 24));

    private final AbstractDataElementChoice dataElementChoice;

    DataElementMenuChoiceEnum(final AbstractDataElementChoice dataElementChoice) {
      this.dataElementChoice = dataElementChoice;
    }

    private static Optional<DataElementMenuChoiceEnum> from(final int choice) {
      Optional<DataElementMenuChoiceEnum> ret = Optional.empty();
      for (DataElementMenuChoiceEnum menuChoice : DataElementMenuChoiceEnum.values()) {
        if (menuChoice.getOrdinal() == choice) {
          ret = Optional.of(menuChoice);
          break;
        }
      }
      return ret;
    }

    public AbstractDataElementChoice getDataElementChoice() {
      return dataElementChoice;
    }

    private int getOrdinal() {
      return dataElementChoice.getOrdinalValue();
    }

  }
}
