package com.jstevenperry.sportsnet.ncaabb.menus;

import com.jstevenperry.sportsnet.framework.DataElementChoice;
import com.jstevenperry.sportsnet.framework.menus.AbstractDataElementChoice;
import com.jstevenperry.sportsnet.framework.menus.AbstractDataElementMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class NcaaBbDataElementMenu extends AbstractDataElementMenu {

    private static final Logger log = LoggerFactory.getLogger(DataElementMenuChoiceEnum.class);

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
        // The ordinal corresponds to the index in the input data where that element is located.
        // See NcaaBbSeasonData.asDoubleList() for the indexes
        RANK(new DataElementChoice("Rank", 0, 1)),
        GAMES_PLAYED(new DataElementChoice("Games Played", 1, 2)),
        GAMES_WON(new DataElementChoice("Games Won", 2, 3)),
        GAMES_LOST(new DataElementChoice("Games Lost", 3, 4)),
        WON_LOST_PCT(new DataElementChoice("Won/Lost %", 4, 5)),
        SIMPLE_RATING_SYSTEM(new DataElementChoice("Simple Rating System", 5, 6)),
        STRENGTH_OF_SCHEDULE(new DataElementChoice("Strength of Schedule", 6, 7)),
        CONFERENCE_WINS(new DataElementChoice("Conference Wins", 7, 8)),
        CONFERENCE_LOSSES(new DataElementChoice("Conference Losses", 8, 9)),
        HOME_WINS(new DataElementChoice("Home Wins", 9, 10)),
        HOME_LOSSES(new DataElementChoice("Home Losses", 10, 11)),
        AWAY_WINS(new DataElementChoice("Away Wins", 11, 12)),
        AWAY_LOSSES(new DataElementChoice("Away Losses", 12, 13)),
        POINTS_SCORED(new DataElementChoice("Points Scored", 13, 14)),
        OPPONENT_POINTS_SCORED(new DataElementChoice("Opponent Points Scored", 14, 15)),
        MINUTES_PLAYED(new DataElementChoice("Minutes Played", 15, 16)),
        FIELD_GOALS(new DataElementChoice("Field Goals", 16, 17)),
        FIELD_GOAL_ATTEMPTS(new DataElementChoice("FG Attempts", 17, 18)),
        FIELD_GOAL_PCT(new DataElementChoice("FG %", 18, 19)),
        THREE_POINTERS(new DataElementChoice("Three Pointers", 19, 20)),
        THREE_POINT_ATTEMPTS(new DataElementChoice("Three Pointer Attempts", 20, 21)),
        THREE_POINT_PCT(new DataElementChoice("Three Pointer %", 21, 22)),
        FREE_THROWS(new DataElementChoice("Free Throws", 22, 23)),
        FREE_THROW_ATTEMPTS(new DataElementChoice("FT Attempts", 23, 24)),
        FREE_THROW_PCT(new DataElementChoice("Free Throw %", 24, 25)),
        OFFENSIVE_REBOUNDS(new DataElementChoice("Offensive Rebounds", 25, 26)),
        TOTAL_REBOUNDS(new DataElementChoice("Total Rebounds", 26, 27)),
        ASSISTS(new DataElementChoice("Assists", 27, 28)),
        STEALS(new DataElementChoice("Steals", 28, 29)),
        BLOCKS(new DataElementChoice("Blocks", 29, 30)),
        TURNOVERS(new DataElementChoice("Turnovers", 30, 31)),
        PERSONAL_FOULS(new DataElementChoice("Personal Fouls", 31, 32));

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
