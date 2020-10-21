package com.jstevenperry.sportsnet.ncaabb.menus;

import com.jstevenperry.sportsnet.framework.menus.AbstractDataElementChoice;
import com.jstevenperry.sportsnet.framework.menus.AbstractDataElementMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
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
    return Optional.empty();
  }

  @Override
  public List<AbstractDataElementChoice> sortChoices() {
    return null;
  }

  public enum DataElementMenuChoiceEnum {
    // Offense
    // The ordinal corresponds to the index in the input data where that element is located.
    // See NetworkParameters.transformRow()
    AVERAGE_POINTS_PER_GAME(new DataElementChoice("AVERAGE_POINTS_PER_GAME", 1, 0)),
    SCORING_MARGIN_PER_GAME(new DataElementChoice("SCORING_MARGIN_PER_GAME", 2, 1)),
    NUMBER_FIELD_GOAL_ATTEMPTS_PER_GAME(new DataElementChoice("NUMBER_FIELD_GOAL_ATTEMPTS_PER_GAME", 3, 2)),
    FIELD_GOAL_PERCENTAGE(new DataElementChoice("FIELD_GOAL_PERCENTAGE", 4, 3)),
    NUMBER_THREE_POINTERS_PER_GAME(new DataElementChoice("NUMBER_THREE_POINTERS_PER_GAME", 5, 4)),
    NUMBER_THREE_POINT_ATTEMPTS_PER_GAME(new DataElementChoice("NUMBER_THREE_POINT_ATTEMPTS_PER_GAME", 6, 5)),
    THREE_POINT_PERCENTAGE(new DataElementChoice("THREE_POINT_PERCENTAGE", 7, 6)),
    NUMBER_FREE_THROW_ATTEMPTS_PER_GAME(new DataElementChoice("NUMBER_FREE_THROW_ATTEMPTS_PER_GAME", 8, 7)),
    FREE_THROW_PERCENTAGE(new DataElementChoice("FREE_THROW_PERCENTAGE", 9, 8)),
    REBOUND_MARGIN(new DataElementChoice("REBOUND_MARGIN", 10, 9)),
    ASSISTS_PER_GAME(new DataElementChoice("ASSISTS_PER_GAME", 11, 10)),
    ASSIST_TO_TURNOVER_RATIO(new DataElementChoice("ASSIST_TO_TURNOVER_RATIO", 12, 11)),
    // Defense
    AVERAGE_OPPONENT_POINTS_PER_GAME(new DataElementChoice("AVERAGE_OPPONENT_POINTS_PER_GAME", 13, 12)),
    NUMBER_OPPONENT_FIELD_GOAL_ATTEMPTS_PER_GAME(new DataElementChoice("NUMBER_OPPONENT_FIELD_GOAL_ATTEMPTS_PER_GAME", 14, 13)),
    OPPONENT_FIELD_GOAL_PERCENTAGE(new DataElementChoice("OPPONENT_FIELD_GOAL_PERCENTAGE", 15, 14)),
    NUMBER_OPPONENT_THREE_POINT_ATTEMPTS_PER_GAME(new DataElementChoice("NUMBER_OPPONENT_THREE_POINT_ATTEMPTS_PER_GAME", 16, 15)),
    OPPONENT_THREE_POINT_PERCENTAGE(new DataElementChoice("OPPONENT_THREE_POINT_PERCENTAGE", 17, 16)),
    BLOCKS_PER_GAME(new DataElementChoice("BLOCKS_PER_GAME", 18, 17)),
    STEALS_PER_GAME(new DataElementChoice("STEALS_PER_GAME", 19, 18)),
    OPPONENT_TURNOVERS_PER_GAME(new DataElementChoice("OPPONENT_TURNOVERS_PER_GAME", 20, 19)),
    // Errors
    TURNOVERS_PER_GAME(new DataElementChoice("TURNOVERS_PER_GAME", 21, 20)),
    FOULS_PER_GAME(new DataElementChoice("FOULS_PER_GAME", 22, 21)),
    NUMBER_DISQUALIFICATIONS(new DataElementChoice("NUMBER_DISQUALIFICATIONS", 23, 22)),
    //
    WON_LOST_PERCENTAGE(new DataElementChoice("WON_LOST_PERCENTAGE", 24, 23));

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

    public int getElementIndex() {
      return dataElementChoice.getElementIndex();
    }

    private int getOrdinal() {
      return dataElementChoice.getOrdinalValue();
    }

  }
}
