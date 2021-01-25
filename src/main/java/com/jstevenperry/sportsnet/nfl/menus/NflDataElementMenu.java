package com.jstevenperry.sportsnet.nfl.menus;

import com.jstevenperry.sportsnet.framework.DataElementChoice;
import com.jstevenperry.sportsnet.framework.menus.AbstractDataElementChoice;
import com.jstevenperry.sportsnet.framework.menus.AbstractDataElementMenu;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class NflDataElementMenu extends AbstractDataElementMenu {
    @Override
    public List<AbstractDataElementChoice> values() {
        return Arrays.stream(NflDataElementMenu.DataElementMenuChoiceEnum.values())
                .map(NflDataElementMenu.DataElementMenuChoiceEnum::getDataElementChoice)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<AbstractDataElementChoice> from(final int ordinalValue) {
        Optional<NflDataElementMenu.DataElementMenuChoiceEnum> dataElementMenuChoiceEnumMaybe = NflDataElementMenu.DataElementMenuChoiceEnum.from(ordinalValue);
        return dataElementMenuChoiceEnumMaybe.map(NflDataElementMenu.DataElementMenuChoiceEnum::getDataElementChoice);
    }

    public enum DataElementMenuChoiceEnum {
        //
        teamOffenseRank(new DataElementChoice("teamOffenseRank", 0, 1)),
        gamesPlayed(new DataElementChoice("gamesPlayed", 1, 2)),
        pointsFor(new DataElementChoice("pointsFor", 2, 3)),
        yards(new DataElementChoice("yards", 3, 4)),
        plays(new DataElementChoice("plays", 4, 5)),
        yardsPerPlay(new DataElementChoice("yardsPerPlay", 5, 6)),
        turnovers(new DataElementChoice("turnovers", 6, 7)),
        fumblesLost(new DataElementChoice("fumblesLost", 7, 8)),
        firstDowns(new DataElementChoice("firstDowns", 8, 9)),
        completedPasses(new DataElementChoice("completedPasses", 9, 10)),
        passAttempts(new DataElementChoice("passAttempts", 10, 11)),
        ydPassing(new DataElementChoice("ydPassing", 11, 12)),
        passingTd(new DataElementChoice("passingTd", 12, 13)),
        interceptions(new DataElementChoice("interceptions", 13, 14)),
        netYdPerPassAtt(new DataElementChoice("netYdPerPassAtt", 14, 15)),
        passing1d(new DataElementChoice("passing1d", 15, 16)),
        rushingAtt(new DataElementChoice("rushingAtt", 16, 17)),
        rushingYd(new DataElementChoice("rushingYd", 17, 18)),
        rushingTd(new DataElementChoice("rushingTd", 18, 19)),
        rushingYdPerAtt(new DataElementChoice("rushingYdPerAtt", 19, 20)),
        rushing1d(new DataElementChoice("rushing1d", 20, 21)),
        penalties(new DataElementChoice("penalties", 21, 22)),
        penaltyYd(new DataElementChoice("penaltyYd", 22, 23)),
        firstDownsByPenalty(new DataElementChoice("firstDownsByPenalty", 23, 24)),
        driveScorePct(new DataElementChoice("driveScorePct", 24, 25)),
        driveOffensiveToPct(new DataElementChoice("driveOffensiveToPct", 25, 26)),
        expectedPtsByAllOffense(new DataElementChoice("expectedPtsByAllOffense", 26, 27)),

        // Passing Offense
        passingOffenseRank(new DataElementChoice("passingOffenseRank", 27, 28)),
        passCompletionPct(new DataElementChoice("passCompletionPct", 28, 29)),
        passTdPct(new DataElementChoice("passTdPct", 29, 30)),
        intPct(new DataElementChoice("intPct", 30, 31)),
        longestPass(new DataElementChoice("longestPass", 31, 32)),
        passYdPerAtt(new DataElementChoice("passYdPerAtt", 32, 33)),
        adjYdPerAtt(new DataElementChoice("adjYdPerAtt", 33, 34)),
        ydPerCompletion(new DataElementChoice("ydPerCompletion", 34, 35)),
        yardPerGame(new DataElementChoice("yardPerGame", 35, 36)),
        passRating(new DataElementChoice("passRating", 36, 37)),
        offensiveSacks(new DataElementChoice("offensiveSacks", 37, 38)),
        ydLostSacks(new DataElementChoice("ydLostSacks", 38, 39)),
        adjNetYdPerPassAtt(new DataElementChoice("adjNetYdPerPassAtt", 39, 40)),
        sackedWhilePassingPct(new DataElementChoice("sackedWhilePassingPct", 40, 41)),
        qbLedCombacks(new DataElementChoice("qbLedCombacks", 41, 42)),
        qbGameWinningDrives(new DataElementChoice("qbGameWinningDrives", 42, 43)),
        expectedPtsPassingOffense(new DataElementChoice("expectedPtsPassingOffense", 43, 44)),

        // Rushing Offense
        rushingOffenseRank(new DataElementChoice("rushingOffenseRank", 44, 45)),
        ydRushingPerGame(new DataElementChoice("ydRushingPerGame", 45, 46)),
        rushingFumbles(new DataElementChoice("rushingFumbles", 46, 47)),
        expectedPtsRushingOffense(new DataElementChoice("expectedPtsRushingOffense", 47, 48)),

        // Kick and Punt Returns
        kickPuntReturnRank(new DataElementChoice("kickPuntReturnRank", 48, 49)),
        puntReturns(new DataElementChoice("puntReturns", 49, 50)),
        puntReturnYd(new DataElementChoice("puntReturnYd", 50, 51)),
        puntReturnTd(new DataElementChoice("puntReturnTd", 51, 52)),
        puntReturnLongest(new DataElementChoice("puntReturnLongest", 52, 53)),
        puntReturnYdPerGame(new DataElementChoice("puntReturnYdPerGame", 53, 54)),
        kickoffReturns(new DataElementChoice("kickoffReturns", 54, 55)),
        kickoffReturnYd(new DataElementChoice("kickoffReturnYd", 55, 56)),
        kickoffReturnTd(new DataElementChoice("kickoffReturnTd", 56, 57)),
        kickoffReturnLongest(new DataElementChoice("kickoffReturnLongest", 57, 58)),
        kickoffReturnYdPerGame(new DataElementChoice("kickoffReturnYdPerGame", 58, 59)),
        allPurposeYd(new DataElementChoice("allPurposeYd", 59, 60)),

        // Kicking and Punting
        kickPuntRank(new DataElementChoice("kickPuntRank", 60, 61)),
        fgAtt(new DataElementChoice("fgAtt", 61, 62)),
        fgMade(new DataElementChoice("fgMade", 62, 63)),
        fgLongest(new DataElementChoice("fgLongest", 63, 64)),
        fgPct(new DataElementChoice("fgPct", 64, 65)),
        xpAtt(new DataElementChoice("xpAtt", 65, 66)),
        xpMade(new DataElementChoice("xpMade", 66, 67)),
        xpPct(new DataElementChoice("xpPct", 67, 68)),
        kickoffs(new DataElementChoice("kickoffs", 68, 69)),
        kickoffYd(new DataElementChoice("kickoffYd", 69, 70)),
        touchbacks(new DataElementChoice("touchbacks", 70, 71)),
        touchbackPct(new DataElementChoice("touchbackPct", 71, 72)),
        kickoffAvg(new DataElementChoice("kickoffAvg", 72, 73)),
        punts(new DataElementChoice("punts", 73, 74)),
        puntYards(new DataElementChoice("puntYards", 74, 75)),
        puntLongest(new DataElementChoice("puntLongest", 75, 76)),
        puntsBlocked(new DataElementChoice("puntsBlocked", 76, 77)),
        ydPerPunt(new DataElementChoice("ydPerPunt", 77, 78)),

        // Scoring Offense
        scoringOffenseRank(new DataElementChoice("scoringOffenseRank", 78, 79)),
        twoPtConversionAtt(new DataElementChoice("twoPtConversionAtt", 79, 80)),
        twoPtConversionMade(new DataElementChoice("twoPtConversionMade", 80, 81)),
        safety(new DataElementChoice("safety", 81, 82)),
        ptsPerGame(new DataElementChoice("ptsPerGame", 82, 83)),

        // Conversions
        conversionsRank(new DataElementChoice("conversionsRank", 83, 84)),
        thirdDownConversionAtt(new DataElementChoice("thirdDownConversionAtt", 84, 85)),
        thirdDownConversionMade(new DataElementChoice("thirdDownConversionMade", 85, 86)),
        thirdDownConversionPct(new DataElementChoice("thirdDownConversionPct", 86, 87)),
        fourthDownConversionAtt(new DataElementChoice("fourthDownConversionAtt", 87, 88)),
        fourthDownConversionMade(new DataElementChoice("fourthDownConversionMade", 88, 89)),
        fourthDownConversionPct(new DataElementChoice("fourthDownConversionPct", 89, 90)),
        redZoneAtt(new DataElementChoice("redZoneAtt", 90, 91)),
        redZoneTd(new DataElementChoice("redZoneTd", 91, 92)),
        redZonePct(new DataElementChoice("redZonePct", 92, 93)),

        // Drive Averages
        driveAveragesRank(new DataElementChoice("driveAveragesRank", 93, 94)),
        numberOfDrives(new DataElementChoice("numberOfDrives", 94, 95)),
        numberOfPlays(new DataElementChoice("numberOfPlays", 95, 96)),
        scoringPct(new DataElementChoice("scoringPct", 96, 97)),
        avgNumberOfPlaysPerDrive(new DataElementChoice("avgNumberOfPlaysPerDrive", 97, 98)),
        avgYdPerDrive(new DataElementChoice("avgYdPerDrive", 98, 99)),
        avgTimeOfPosession(new DataElementChoice("avgTimeOfPosession", 99, 100)),
        avgPtsPerPosession(new DataElementChoice("avgPtsPerPosession", 100, 101)),
        ;

        private final AbstractDataElementChoice dataElementChoice;

        DataElementMenuChoiceEnum(final AbstractDataElementChoice dataElementChoice) {
            this.dataElementChoice = dataElementChoice;
        }

        private static Optional<NflDataElementMenu.DataElementMenuChoiceEnum> from(final int choice) {
            Optional<NflDataElementMenu.DataElementMenuChoiceEnum> ret = Optional.empty();
            for (NflDataElementMenu.DataElementMenuChoiceEnum menuChoice : NflDataElementMenu.DataElementMenuChoiceEnum.values()) {
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
