package com.jstevenperry.sportsnet.nba.menus;

import com.jstevenperry.sportsnet.framework.DataElementChoice;
import com.jstevenperry.sportsnet.framework.menus.AbstractDataElementChoice;
import com.jstevenperry.sportsnet.framework.menus.AbstractDataElementMenu;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class NbaDataElementMenu extends AbstractDataElementMenu {
    @Override
    public List<AbstractDataElementChoice> values() {
        return Arrays.stream(NbaDataElementMenu.DataElementMenuChoiceEnum.values())
                .map(NbaDataElementMenu.DataElementMenuChoiceEnum::getDataElementChoice)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<AbstractDataElementChoice> from(final int ordinalValue) {
        Optional<NbaDataElementMenu.DataElementMenuChoiceEnum> dataElementMenuChoiceEnumMaybe = NbaDataElementMenu.DataElementMenuChoiceEnum.from(ordinalValue);
        return dataElementMenuChoiceEnumMaybe.map(NbaDataElementMenu.DataElementMenuChoiceEnum::getDataElementChoice);
    }

    public enum DataElementMenuChoiceEnum {
        //
        teamStatsRank(new DataElementChoice("teamStatsRank", 0, 1)),
        gamesPlayed(new DataElementChoice("gamesPlayed", 1, 2)),
        minutesPlayed(new DataElementChoice("minutesPlayed", 2, 3)),
        fieldGoals(new DataElementChoice("fieldGoals", 3, 4)),
        fieldGoalAtt(new DataElementChoice("fieldGoalAtt", 4, 5)),
        fieldGoalPct(new DataElementChoice("fieldGoalPct", 5, 6)),
        threePointers(new DataElementChoice("threePointers", 6, 7)),
        threePointerAtt(new DataElementChoice("threePointerAtt", 7, 8)),
        threePointerPct(new DataElementChoice("threePointerPct", 8, 9)),
        twoPointers(new DataElementChoice("twoPointers", 9, 10)),
        twoPointerAtt(new DataElementChoice("twoPointerAtt", 10, 11)),
        twoPointerPct(new DataElementChoice("twoPointerPct", 11, 12)),
        freeThrows(new DataElementChoice("freeThrows", 12, 13)),
        freeThrowAtt(new DataElementChoice("freeThrowAtt", 13, 14)),
        freeThrowPct(new DataElementChoice("freeThrowPct", 14, 15)),
        offensiveRebounds(new DataElementChoice("offensiveRebounds", 15, 16)),
        defensiveRebounds(new DataElementChoice("defensiveRebounds", 16, 17)),
        assists(new DataElementChoice("assists", 17, 18)),
        steals(new DataElementChoice("steals", 18, 19)),
        blocks(new DataElementChoice("blocks", 19, 20)),
        turnovers(new DataElementChoice("turnovers", 20, 21)),
        fouls(new DataElementChoice("fouls", 21, 22)),
        points(new DataElementChoice("points", 22, 23)),
        oppStatsRank(new DataElementChoice("oppStatsRank", 23, 24)),
        oppGamesPlayed(new DataElementChoice("oppGamesPlayed", 24, 25)),
        oppMinutesPlayed(new DataElementChoice("oppMinutesPlayed", 25, 26)),
        oppFieldGoals(new DataElementChoice("oppFieldGoals", 26, 27)),
        oppFieldGoalAtt(new DataElementChoice("oppFieldGoalAtt", 27, 28)),
        oppFieldGoalPct(new DataElementChoice("oppFieldGoalPct", 28, 29)),
        oppThreePointers(new DataElementChoice("oppThreePointers", 29, 30)),
        oppThreePointerAtt(new DataElementChoice("oppThreePointerAtt", 30, 31)),
        oppThreePointerPct(new DataElementChoice("oppThreePointerPct", 31, 32)),
        oppTwoPointers(new DataElementChoice("oppTwoPointers", 32, 33)),
        oppTwoPointerAtt(new DataElementChoice("oppTwoPointerAtt", 33, 34)),
        oppTwoPointerPct(new DataElementChoice("oppTwoPointerPct", 34, 35)),
        oppFreeThrows(new DataElementChoice("oppFreeThrows", 35, 36)),
        oppFreeThrowAtt(new DataElementChoice("oppFreeThrowAtt", 36, 37)),
        oppFreeThrowPct(new DataElementChoice("oppFreeThrowPct", 37, 38)),
        oppOffensiveRebounds(new DataElementChoice("oppOffensiveRebounds", 38, 39)),
        oppDefensiveRebounds(new DataElementChoice("oppDefensiveRebounds", 39, 40)),
        oppAssists(new DataElementChoice("oppAssists", 40, 41)),
        oppSteals(new DataElementChoice("oppSteals", 41, 42)),
        oppBlocks(new DataElementChoice("oppBlocks", 42, 43)),
        oppTurnovers(new DataElementChoice("oppTurnovers", 43, 44)),
        oppFouls(new DataElementChoice("oppFouls", 44, 45)),
        oppPoints(new DataElementChoice("oppPoints", 45, 46)),
        miscellaneousStatsRank(new DataElementChoice("miscellaneousStatsRank", 46, 47)),
        ageOnFeb1(new DataElementChoice("ageOnFeb1", 47, 48)),
        wins(new DataElementChoice("wins", 48, 49)),
        losses(new DataElementChoice("losses", 49, 50)),
        pythagoreanWins(new DataElementChoice("pythagoreanWins", 50, 51)),
        pythagoreanLosses(new DataElementChoice("pythagoreanLosses", 51, 52)),
        marginOfVictory(new DataElementChoice("marginOfVictory", 52, 53)),
        strengthOfSchedule(new DataElementChoice("strengthOfSchedule", 53, 54)),
        simpleRatingSystem(new DataElementChoice("simpleRatingSystem", 54, 55)),
        offensiveRating(new DataElementChoice("offensiveRating", 55, 56)),
        defensiveRating(new DataElementChoice("defensiveRating", 56, 57)),
        netRating(new DataElementChoice("netRating", 57, 58)),
        pace(new DataElementChoice("pace", 58, 59)),
        freeThrowAttRate(new DataElementChoice("freeThrowAttRate", 59, 60)),
        threePtAttRate(new DataElementChoice("threePtAttRate", 60, 61)),
        trueShootingPct(new DataElementChoice("trueShootingPct", 61, 62)),
        effFieldGoalPct(new DataElementChoice("effFieldGoalPct", 62, 63)),
        turnoverPct(new DataElementChoice("turnoverPct", 63, 64)),
        offensiveReboundPct(new DataElementChoice("offensiveReboundPct", 64, 65)),
        freeThrowMadeRate(new DataElementChoice("freeThrowMadeRate", 65, 66)),
        oppEffFieldGoalPct(new DataElementChoice("oppEffFieldGoalPct", 66, 67)),
        oppTurnoverPct(new DataElementChoice("oppTurnoverPct", 67, 68)),
        oppOffensiveReboundPct(new DataElementChoice("oppOffensiveReboundPct", 68, 69)),
        oppFreeThrowMadeRate(new DataElementChoice("oppFreeThrowMadeRate", 69, 70)),
        attendancePerGame(new DataElementChoice("attendancePerGame", 70, 71)),

        ;

        private final AbstractDataElementChoice dataElementChoice;

        DataElementMenuChoiceEnum(final AbstractDataElementChoice dataElementChoice) {
            this.dataElementChoice = dataElementChoice;
        }

        private static Optional<NbaDataElementMenu.DataElementMenuChoiceEnum> from(final int choice) {
            Optional<NbaDataElementMenu.DataElementMenuChoiceEnum> ret = Optional.empty();
            for (NbaDataElementMenu.DataElementMenuChoiceEnum menuChoice : NbaDataElementMenu.DataElementMenuChoiceEnum.values()) {
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
