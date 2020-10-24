package com.jstevenperry.sportsnet.nhl.menus;

import com.jstevenperry.sportsnet.framework.DataElementChoice;
import com.jstevenperry.sportsnet.framework.menus.AbstractDataElementChoice;
import com.jstevenperry.sportsnet.framework.menus.AbstractDataElementMenu;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DataElementMenu extends AbstractDataElementMenu {
  @Override
  public List<AbstractDataElementChoice> values() {
    return Arrays.stream(DataElementMenuChoiceEnum.values())
      .map(DataElementMenuChoiceEnum::getDataElementChoice)
      .collect(Collectors.toList());
  }

  @Override
  public Optional<AbstractDataElementChoice> from(final int ordinalValue) {
    Optional<DataElementMenuChoiceEnum> dataElementMenuChoiceEnumMaybe = DataElementMenuChoiceEnum.from(ordinalValue);
    return dataElementMenuChoiceEnumMaybe.map(DataElementMenuChoiceEnum::getDataElementChoice);
  }

  public enum DataElementMenuChoiceEnum {
    //
    RANK(new DataElementChoice("Rank", 0, 1)),
    AVERAGE_AGE(new DataElementChoice("Average age", 1, 2)),
    GAMES_PLAYED(new DataElementChoice("Games played", 2, 3)),
    WINS(new DataElementChoice("Wins", 3, 4)),
    LOSSES(new DataElementChoice("Losses", 4, 5)),
    OVERTIME_LOSSES(new DataElementChoice("OT losses", 5, 6)),
    POINTS(new DataElementChoice("Points", 6, 7)),
    POINTS_PCT(new DataElementChoice("Points %", 7, 8)),
    GOALS_FOR(new DataElementChoice("Goals for", 8, 9)),
    GOALS_AGAINST(new DataElementChoice("Goals against", 9, 10)),
    SHOOTOUT_WINS(new DataElementChoice("Shootout wins", 10, 11)),
    SHOOTOUT_LOSSES(new DataElementChoice("Shootout losses", 11, 12)),
    SIMPLE_RATING_SYSTEM(new DataElementChoice("Simple rating system", 12, 13)),
    STRENGTH_OF_SCHEDULE(new DataElementChoice("Strength of schedule", 13, 14)),
    GOALS_PER_GAME(new DataElementChoice("Goals per game", 14, 15)),
    EVEN_STRENGTH_GOALS(new DataElementChoice("Even-strength goals", 15, 16)),
    EVEN_STRENGTH_GOALS_AGAINST(new DataElementChoice("Even-strength goals against", 16, 17)),
    POWER_PLAY_GOALS(new DataElementChoice("Power play goals", 17, 18)),
    POWER_PLAY_OPPORTUNITIES(new DataElementChoice("Power play opportunities", 18, 19)),
    POWER_PLAY_PCT(new DataElementChoice("Power play percentage", 19, 20)),
    POWER_PLAY_GOALS_AGAINST(new DataElementChoice("Power play goals against", 20, 21)),
    POWER_PLAY_OPPORTUNITIES_AGAINST(new DataElementChoice("Power play opportunities against", 21, 22)),
    PENALTY_KILL_PCT(new DataElementChoice("Penalty kill percentage", 22, 23)),
    SHORT_HANDED_GOALS(new DataElementChoice("Short-handed goals", 23, 24)),
    SHORT_HANDED_GOALS_AGAINST(new DataElementChoice("Short-handed goals against", 24, 25)),
    PENALTY_MINUTES_PER_GAME(new DataElementChoice("Penalty minutes per game", 25, 26)),
    OPPONENT_PENALTY_MINUTES_PER_GAME(new DataElementChoice("Opponent penalty minutes per game", 26, 27)),
    SHOTS_ON_GOAL(new DataElementChoice("Shots on goal", 27, 28)),
    SHOOTING_PCT(new DataElementChoice("Shooting percentage", 28, 29)),
    SHOTS_AGAINST(new DataElementChoice("Shots against", 29, 30)),
    SAVE_PCT(new DataElementChoice("Save percentage", 30, 31)),
    SHUTOUTS(new DataElementChoice("Shutouts", 31, 32)),
    ;

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
