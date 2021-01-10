package com.jstevenperry.sportsnet.nba.model;

import com.jstevenperry.sportsnet.framework.model.AbstractSeasonData;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;
import java.util.List;

public class NbaSeasonData extends AbstractSeasonData {
    
    public static List<Double> seasonDataAsDoubleList(final NbaSeasonData seasonData, final Double resultClass, final boolean isTraining) {
        List<Double> ret = new ArrayList<>();
        ret.add(asDouble(seasonData.getTeamStatsRank()));
        ret.add(asDouble(seasonData.getGamesPlayed()));
        ret.add(asDouble(seasonData.getMinutesPlayed()));
        ret.add(asDouble(seasonData.getFieldGoals()));
        ret.add(asDouble(seasonData.getFieldGoalAtt()));
        ret.add(asDouble(seasonData.getFieldGoalPct()));
        ret.add(asDouble(seasonData.getThreePointers()));
        ret.add(asDouble(seasonData.getThreePointerAtt()));
        ret.add(asDouble(seasonData.getThreePointerPct()));
        ret.add(asDouble(seasonData.getTwoPointers()));
        ret.add(asDouble(seasonData.getTwoPointerAtt()));
        ret.add(asDouble(seasonData.getTwoPointerPct()));
        ret.add(asDouble(seasonData.getFreeThrows()));
        ret.add(asDouble(seasonData.getFreeThrowAtt()));
        ret.add(asDouble(seasonData.getFreeThrowPct()));
        ret.add(asDouble(seasonData.getOffensiveRebounds()));
        ret.add(asDouble(seasonData.getDefensiveRebounds()));
        ret.add(asDouble(seasonData.getAssists()));
        ret.add(asDouble(seasonData.getSteals()));
        ret.add(asDouble(seasonData.getBlocks()));
        ret.add(asDouble(seasonData.getTurnovers()));
        ret.add(asDouble(seasonData.getFouls()));
        ret.add(asDouble(seasonData.getPoints()));
        ret.add(asDouble(seasonData.getOppStatsRank()));
        ret.add(asDouble(seasonData.getOppGamesPlayed()));
        ret.add(asDouble(seasonData.getOppMinutesPlayed()));
        ret.add(asDouble(seasonData.getOppFieldGoals()));
        ret.add(asDouble(seasonData.getOppFieldGoalAtt()));
        ret.add(asDouble(seasonData.getOppFieldGoalPct()));
        ret.add(asDouble(seasonData.getOppThreePointers()));
        ret.add(asDouble(seasonData.getOppThreePointerAtt()));
        ret.add(asDouble(seasonData.getOppThreePointerPct()));
        ret.add(asDouble(seasonData.getOppTwoPointers()));
        ret.add(asDouble(seasonData.getOppTwoPointerAtt()));
        ret.add(asDouble(seasonData.getOppTwoPointerPct()));
        ret.add(asDouble(seasonData.getOppFreeThrows()));
        ret.add(asDouble(seasonData.getOppFreeThrowAtt()));
        ret.add(asDouble(seasonData.getOppFreeThrowPct()));
        ret.add(asDouble(seasonData.getOppOffensiveRebounds()));
        ret.add(asDouble(seasonData.getOppDefensiveRebounds()));
        ret.add(asDouble(seasonData.getOppAssists()));
        ret.add(asDouble(seasonData.getOppSteals()));
        ret.add(asDouble(seasonData.getOppBlocks()));
        ret.add(asDouble(seasonData.getOppTurnovers()));
        ret.add(asDouble(seasonData.getOppFouls()));
        ret.add(asDouble(seasonData.getOppPoints()));
        ret.add(asDouble(seasonData.getMiscellaneousStatsRank()));
        ret.add(asDouble(seasonData.getAgeOnFeb1()));
        ret.add(asDouble(seasonData.getWins()));
        ret.add(asDouble(seasonData.getLosses()));
        ret.add(asDouble(seasonData.getPythagoreanWins()));
        ret.add(asDouble(seasonData.getPythagoreanLosses()));
        ret.add(asDouble(seasonData.getMarginOfVictory()));
        ret.add(asDouble(seasonData.getStrengthOfSchedule()));
        ret.add(asDouble(seasonData.getSimpleRatingSystem()));
        ret.add(asDouble(seasonData.getOffensiveRating()));
        ret.add(asDouble(seasonData.getDefensiveRating()));
        ret.add(asDouble(seasonData.getNetRating()));
        ret.add(asDouble(seasonData.getPace()));
        ret.add(asDouble(seasonData.getFreeThrowAttRate()));
        ret.add(asDouble(seasonData.getThreePtAttRate()));
        ret.add(asDouble(seasonData.getTrueShootingPct()));
        ret.add(asDouble(seasonData.getEffFieldGoalPct()));
        ret.add(asDouble(seasonData.getTurnoverPct()));
        ret.add(asDouble(seasonData.getOffensiveReboundPct()));
        ret.add(asDouble(seasonData.getFreeThrowMadeRate()));
        ret.add(asDouble(seasonData.getOppEffFieldGoalPct()));
        ret.add(asDouble(seasonData.getOppTurnoverPct()));
        ret.add(asDouble(seasonData.getOppOffensiveReboundPct()));
        ret.add(asDouble(seasonData.getOppFreeThrowMadeRate()));
        ret.add(asDouble(seasonData.getAttendancePerGame()));
        
        return ret;
    }
    
    // Team stats
    int teamStatsRank;
    int gamesPlayed;
    int minutesPlayed;
    int fieldGoals;
    int fieldGoalAtt;
    double fieldGoalPct;
    int threePointers;
    int threePointerAtt;
    double threePointerPct;
    int twoPointers;
    int twoPointerAtt;
    double twoPointerPct;
    int freeThrows;
    int freeThrowAtt;
    double freeThrowPct;
    int offensiveRebounds;
    int defensiveRebounds;
    int assists;
    int steals;
    int blocks;
    int turnovers;
    int fouls;
    int points;
    // Opponent stats
    int oppStatsRank;
    int oppGamesPlayed;
    int oppMinutesPlayed;
    int oppFieldGoals;
    int oppFieldGoalAtt;
    double oppFieldGoalPct;
    int oppThreePointers;
    int oppThreePointerAtt;
    double oppThreePointerPct;
    int oppTwoPointers;
    int oppTwoPointerAtt;
    double oppTwoPointerPct;
    int oppFreeThrows;
    int oppFreeThrowAtt;
    double oppFreeThrowPct;
    int oppOffensiveRebounds;
    int oppDefensiveRebounds;
    int oppAssists;
    int oppSteals;
    int oppBlocks;
    int oppTurnovers;
    int oppFouls;
    int oppPoints;
    // Misc stats
    int miscellaneousStatsRank;
    double ageOnFeb1;
    int wins;
    int losses;
    int pythagoreanWins;
    int pythagoreanLosses;
    double marginOfVictory;
    double strengthOfSchedule;
    double simpleRatingSystem;
    double offensiveRating;
    double defensiveRating;
    double netRating;
    double pace;
    double freeThrowAttRate;
    double threePtAttRate;
    double trueShootingPct;
    double effFieldGoalPct;
    double turnoverPct;
    double offensiveReboundPct;
    double freeThrowMadeRate;
    double oppEffFieldGoalPct;
    double oppTurnoverPct;
    double oppOffensiveReboundPct;
    double oppFreeThrowMadeRate;
    double attendancePerGame;

    @Override
    public String toString() {
        return "NbaSeasonData{" +
                "teamStatsRank=" + teamStatsRank +
                ", gamesPlayed=" + gamesPlayed +
                ", minutesPlayed=" + minutesPlayed +
                ", fieldGoals=" + fieldGoals +
                ", fieldGoalAtt=" + fieldGoalAtt +
                ", fieldGoalPct=" + fieldGoalPct +
                ", threePointers=" + threePointers +
                ", threePointerAtt=" + threePointerAtt +
                ", threePointerPct=" + threePointerPct +
                ", twoPointers=" + twoPointers +
                ", twoPointerAtt=" + twoPointerAtt +
                ", twoPointerPct=" + twoPointerPct +
                ", freeThrows=" + freeThrows +
                ", freeThrowAtt=" + freeThrowAtt +
                ", freeThrowPct=" + freeThrowPct +
                ", offensiveRebounds=" + offensiveRebounds +
                ", defensiveRebounds=" + defensiveRebounds +
                ", assists=" + assists +
                ", steals=" + steals +
                ", blocks=" + blocks +
                ", turnovers=" + turnovers +
                ", fouls=" + fouls +
                ", points=" + points +
                ", oppStatsRank=" + oppStatsRank +
                ", oppGamesPlayed=" + oppGamesPlayed +
                ", oppMinutesPlayed=" + oppMinutesPlayed +
                ", oppFieldGoals=" + oppFieldGoals +
                ", oppFieldGoalAtt=" + oppFieldGoalAtt +
                ", oppFieldGoalPct=" + oppFieldGoalPct +
                ", oppThreePointers=" + oppThreePointers +
                ", oppThreePointerAtt=" + oppThreePointerAtt +
                ", oppThreePointerPct=" + oppThreePointerPct +
                ", oppTwoPointers=" + oppTwoPointers +
                ", oppTwoPointerAtt=" + oppTwoPointerAtt +
                ", oppTwoPointerPct=" + oppTwoPointerPct +
                ", oppFreeThrows=" + oppFreeThrows +
                ", oppFreeThrowAtt=" + oppFreeThrowAtt +
                ", oppFreeThrowPct=" + oppFreeThrowPct +
                ", oppOffensiveRebounds=" + oppOffensiveRebounds +
                ", oppDefensiveRebounds=" + oppDefensiveRebounds +
                ", oppAssists=" + oppAssists +
                ", oppSteals=" + oppSteals +
                ", oppBlocks=" + oppBlocks +
                ", oppTurnovers=" + oppTurnovers +
                ", oppFouls=" + oppFouls +
                ", oppPoints=" + oppPoints +
                ", miscellaneousStatsRank=" + miscellaneousStatsRank +
                ", ageOnFeb1=" + ageOnFeb1 +
                ", wins=" + wins +
                ", losses=" + losses +
                ", pythagoreanWins=" + pythagoreanWins +
                ", pythagoreanLosses=" + pythagoreanLosses +
                ", marginOfVictory=" + marginOfVictory +
                ", strengthOfSchedule=" + strengthOfSchedule +
                ", simpleRatingSystem=" + simpleRatingSystem +
                ", offensiveRating=" + offensiveRating +
                ", defensiveRating=" + defensiveRating +
                ", netRating=" + netRating +
                ", pace=" + pace +
                ", freeThrowAttRate=" + freeThrowAttRate +
                ", threePtAttRate=" + threePtAttRate +
                ", trueShootingPct=" + trueShootingPct +
                ", effFieldGoalPct=" + effFieldGoalPct +
                ", turnoverPct=" + turnoverPct +
                ", offensiveReboundPct=" + offensiveReboundPct +
                ", freeThrowMadeRate=" + freeThrowMadeRate +
                ", oppEffFieldGoalPct=" + oppEffFieldGoalPct +
                ", oppTurnoverPct=" + oppTurnoverPct +
                ", oppOffensiveReboundPct=" + oppOffensiveReboundPct +
                ", oppFreeThrowMadeRate=" + oppFreeThrowMadeRate +
                ", attendancePerGame=" + attendancePerGame +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        final NbaSeasonData that = (NbaSeasonData) o;

        return new EqualsBuilder().append(teamStatsRank, that.teamStatsRank).append(gamesPlayed, that.gamesPlayed).append(minutesPlayed, that.minutesPlayed).append(fieldGoals, that.fieldGoals).append(fieldGoalAtt, that.fieldGoalAtt).append(fieldGoalPct, that.fieldGoalPct).append(threePointers, that.threePointers).append(threePointerAtt, that.threePointerAtt).append(threePointerPct, that.threePointerPct).append(twoPointers, that.twoPointers).append(twoPointerAtt, that.twoPointerAtt).append(twoPointerPct, that.twoPointerPct).append(freeThrows, that.freeThrows).append(freeThrowAtt, that.freeThrowAtt).append(freeThrowPct, that.freeThrowPct).append(offensiveRebounds, that.offensiveRebounds).append(defensiveRebounds, that.defensiveRebounds).append(assists, that.assists).append(steals, that.steals).append(blocks, that.blocks).append(turnovers, that.turnovers).append(fouls, that.fouls).append(points, that.points).append(oppStatsRank, that.oppStatsRank).append(oppGamesPlayed, that.oppGamesPlayed).append(oppMinutesPlayed, that.oppMinutesPlayed).append(oppFieldGoals, that.oppFieldGoals).append(oppFieldGoalAtt, that.oppFieldGoalAtt).append(oppFieldGoalPct, that.oppFieldGoalPct).append(oppThreePointers, that.oppThreePointers).append(oppThreePointerAtt, that.oppThreePointerAtt).append(oppThreePointerPct, that.oppThreePointerPct).append(oppTwoPointers, that.oppTwoPointers).append(oppTwoPointerAtt, that.oppTwoPointerAtt).append(oppTwoPointerPct, that.oppTwoPointerPct).append(oppFreeThrows, that.oppFreeThrows).append(oppFreeThrowAtt, that.oppFreeThrowAtt).append(oppFreeThrowPct, that.oppFreeThrowPct).append(oppOffensiveRebounds, that.oppOffensiveRebounds).append(oppDefensiveRebounds, that.oppDefensiveRebounds).append(oppAssists, that.oppAssists).append(oppSteals, that.oppSteals).append(oppBlocks, that.oppBlocks).append(oppTurnovers, that.oppTurnovers).append(oppFouls, that.oppFouls).append(oppPoints, that.oppPoints).append(miscellaneousStatsRank, that.miscellaneousStatsRank).append(ageOnFeb1, that.ageOnFeb1).append(wins, that.wins).append(losses, that.losses).append(pythagoreanWins, that.pythagoreanWins).append(pythagoreanLosses, that.pythagoreanLosses).append(marginOfVictory, that.marginOfVictory).append(strengthOfSchedule, that.strengthOfSchedule).append(simpleRatingSystem, that.simpleRatingSystem).append(offensiveRating, that.offensiveRating).append(defensiveRating, that.defensiveRating).append(netRating, that.netRating).append(pace, that.pace).append(freeThrowAttRate, that.freeThrowAttRate).append(threePtAttRate, that.threePtAttRate).append(trueShootingPct, that.trueShootingPct).append(effFieldGoalPct, that.effFieldGoalPct).append(turnoverPct, that.turnoverPct).append(offensiveReboundPct, that.offensiveReboundPct).append(freeThrowMadeRate, that.freeThrowMadeRate).append(oppEffFieldGoalPct, that.oppEffFieldGoalPct).append(oppTurnoverPct, that.oppTurnoverPct).append(oppOffensiveReboundPct, that.oppOffensiveReboundPct).append(oppFreeThrowMadeRate, that.oppFreeThrowMadeRate).append(attendancePerGame, that.attendancePerGame).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(teamStatsRank).append(gamesPlayed).append(minutesPlayed).append(fieldGoals).append(fieldGoalAtt).append(fieldGoalPct).append(threePointers).append(threePointerAtt).append(threePointerPct).append(twoPointers).append(twoPointerAtt).append(twoPointerPct).append(freeThrows).append(freeThrowAtt).append(freeThrowPct).append(offensiveRebounds).append(defensiveRebounds).append(assists).append(steals).append(blocks).append(turnovers).append(fouls).append(points).append(oppStatsRank).append(oppGamesPlayed).append(oppMinutesPlayed).append(oppFieldGoals).append(oppFieldGoalAtt).append(oppFieldGoalPct).append(oppThreePointers).append(oppThreePointerAtt).append(oppThreePointerPct).append(oppTwoPointers).append(oppTwoPointerAtt).append(oppTwoPointerPct).append(oppFreeThrows).append(oppFreeThrowAtt).append(oppFreeThrowPct).append(oppOffensiveRebounds).append(oppDefensiveRebounds).append(oppAssists).append(oppSteals).append(oppBlocks).append(oppTurnovers).append(oppFouls).append(oppPoints).append(miscellaneousStatsRank).append(ageOnFeb1).append(wins).append(losses).append(pythagoreanWins).append(pythagoreanLosses).append(marginOfVictory).append(strengthOfSchedule).append(simpleRatingSystem).append(offensiveRating).append(defensiveRating).append(netRating).append(pace).append(freeThrowAttRate).append(threePtAttRate).append(trueShootingPct).append(effFieldGoalPct).append(turnoverPct).append(offensiveReboundPct).append(freeThrowMadeRate).append(oppEffFieldGoalPct).append(oppTurnoverPct).append(oppOffensiveReboundPct).append(oppFreeThrowMadeRate).append(attendancePerGame).toHashCode();
    }

    public int getOppStatsRank() {
        return oppStatsRank;
    }

    public NbaSeasonData setOppStatsRank(final int oppStatsRank) {
        this.oppStatsRank = oppStatsRank;
        return this;
    }

    public int getMiscellaneousStatsRank() {
        return miscellaneousStatsRank;
    }

    public NbaSeasonData setMiscellaneousStatsRank(final int miscellaneousStatsRank) {
        this.miscellaneousStatsRank = miscellaneousStatsRank;
        return this;
    }

    public int getTeamStatsRank() {
        return teamStatsRank;
    }

    public NbaSeasonData setTeamStatsRank(int teamStatsRank) {
        this.teamStatsRank = teamStatsRank;
        return this;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public NbaSeasonData setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
        return this;
    }

    public int getMinutesPlayed() {
        return minutesPlayed;
    }

    public NbaSeasonData setMinutesPlayed(int minutesPlayed) {
        this.minutesPlayed = minutesPlayed;
        return this;
    }

    public int getFieldGoals() {
        return fieldGoals;
    }

    public NbaSeasonData setFieldGoals(int fieldGoals) {
        this.fieldGoals = fieldGoals;
        return this;
    }

    public int getFieldGoalAtt() {
        return fieldGoalAtt;
    }

    public NbaSeasonData setFieldGoalAtt(int fieldGoalAtt) {
        this.fieldGoalAtt = fieldGoalAtt;
        return this;
    }

    public double getFieldGoalPct() {
        return fieldGoalPct;
    }

    public NbaSeasonData setFieldGoalPct(double fieldGoalPct) {
        this.fieldGoalPct = fieldGoalPct;
        return this;
    }

    public int getThreePointers() {
        return threePointers;
    }

    public NbaSeasonData setThreePointers(int threePointers) {
        this.threePointers = threePointers;
        return this;
    }

    public int getThreePointerAtt() {
        return threePointerAtt;
    }

    public NbaSeasonData setThreePointerAtt(int threePointerAtt) {
        this.threePointerAtt = threePointerAtt;
        return this;
    }

    public double getThreePointerPct() {
        return threePointerPct;
    }

    public NbaSeasonData setThreePointerPct(double threePointerPct) {
        this.threePointerPct = threePointerPct;
        return this;
    }

    public int getTwoPointers() {
        return twoPointers;
    }

    public NbaSeasonData setTwoPointers(int twoPointers) {
        this.twoPointers = twoPointers;
        return this;
    }

    public int getTwoPointerAtt() {
        return twoPointerAtt;
    }

    public NbaSeasonData setTwoPointerAtt(int twoPointerAtt) {
        this.twoPointerAtt = twoPointerAtt;
        return this;
    }

    public double getTwoPointerPct() {
        return twoPointerPct;
    }

    public NbaSeasonData setTwoPointerPct(double twoPointerPct) {
        this.twoPointerPct = twoPointerPct;
        return this;
    }

    public int getFreeThrows() {
        return freeThrows;
    }

    public NbaSeasonData setFreeThrows(int freeThrows) {
        this.freeThrows = freeThrows;
        return this;
    }

    public int getFreeThrowAtt() {
        return freeThrowAtt;
    }

    public NbaSeasonData setFreeThrowAtt(int freeThrowAtt) {
        this.freeThrowAtt = freeThrowAtt;
        return this;
    }

    public double getFreeThrowPct() {
        return freeThrowPct;
    }

    public NbaSeasonData setFreeThrowPct(double freeThrowPct) {
        this.freeThrowPct = freeThrowPct;
        return this;
    }

    public int getOffensiveRebounds() {
        return offensiveRebounds;
    }

    public NbaSeasonData setOffensiveRebounds(int offensiveRebounds) {
        this.offensiveRebounds = offensiveRebounds;
        return this;
    }

    public int getDefensiveRebounds() {
        return defensiveRebounds;
    }

    public NbaSeasonData setDefensiveRebounds(int defensiveRebounds) {
        this.defensiveRebounds = defensiveRebounds;
        return this;
    }

    public int getAssists() {
        return assists;
    }

    public NbaSeasonData setAssists(int assists) {
        this.assists = assists;
        return this;
    }

    public int getSteals() {
        return steals;
    }

    public NbaSeasonData setSteals(int steals) {
        this.steals = steals;
        return this;
    }

    public int getBlocks() {
        return blocks;
    }

    public NbaSeasonData setBlocks(int blocks) {
        this.blocks = blocks;
        return this;
    }

    public int getTurnovers() {
        return turnovers;
    }

    public NbaSeasonData setTurnovers(int turnovers) {
        this.turnovers = turnovers;
        return this;
    }

    public int getFouls() {
        return fouls;
    }

    public NbaSeasonData setFouls(int fouls) {
        this.fouls = fouls;
        return this;
    }

    public int getPoints() {
        return points;
    }

    public NbaSeasonData setPoints(int points) {
        this.points = points;
        return this;
    }

    public int getOppGamesPlayed() {
        return oppGamesPlayed;
    }

    public NbaSeasonData setOppGamesPlayed(int oppGamesPlayed) {
        this.oppGamesPlayed = oppGamesPlayed;
        return this;
    }

    public int getOppMinutesPlayed() {
        return oppMinutesPlayed;
    }

    public NbaSeasonData setOppMinutesPlayed(int oppMinutesPlayed) {
        this.oppMinutesPlayed = oppMinutesPlayed;
        return this;
    }

    public int getOppFieldGoals() {
        return oppFieldGoals;
    }

    public NbaSeasonData setOppFieldGoals(int oppFieldGoals) {
        this.oppFieldGoals = oppFieldGoals;
        return this;
    }

    public int getOppFieldGoalAtt() {
        return oppFieldGoalAtt;
    }

    public NbaSeasonData setOppFieldGoalAtt(int oppFieldGoalAtt) {
        this.oppFieldGoalAtt = oppFieldGoalAtt;
        return this;
    }

    public double getOppFieldGoalPct() {
        return oppFieldGoalPct;
    }

    public NbaSeasonData setOppFieldGoalPct(double oppFieldGoalPct) {
        this.oppFieldGoalPct = oppFieldGoalPct;
        return this;
    }

    public int getOppThreePointers() {
        return oppThreePointers;
    }

    public NbaSeasonData setOppThreePointers(int oppThreePointers) {
        this.oppThreePointers = oppThreePointers;
        return this;
    }

    public int getOppThreePointerAtt() {
        return oppThreePointerAtt;
    }

    public NbaSeasonData setOppThreePointerAtt(int oppThreePointerAtt) {
        this.oppThreePointerAtt = oppThreePointerAtt;
        return this;
    }

    public double getOppThreePointerPct() {
        return oppThreePointerPct;
    }

    public NbaSeasonData setOppThreePointerPct(double oppThreePointerPct) {
        this.oppThreePointerPct = oppThreePointerPct;
        return this;
    }

    public int getOppTwoPointers() {
        return oppTwoPointers;
    }

    public NbaSeasonData setOppTwoPointers(int oppTwoPointers) {
        this.oppTwoPointers = oppTwoPointers;
        return this;
    }

    public int getOppTwoPointerAtt() {
        return oppTwoPointerAtt;
    }

    public NbaSeasonData setOppTwoPointerAtt(int oppTwoPointerAtt) {
        this.oppTwoPointerAtt = oppTwoPointerAtt;
        return this;
    }

    public double getOppTwoPointerPct() {
        return oppTwoPointerPct;
    }

    public NbaSeasonData setOppTwoPointerPct(double oppTwoPointerPct) {
        this.oppTwoPointerPct = oppTwoPointerPct;
        return this;
    }

    public int getOppFreeThrows() {
        return oppFreeThrows;
    }

    public NbaSeasonData setOppFreeThrows(int oppFreeThrows) {
        this.oppFreeThrows = oppFreeThrows;
        return this;
    }

    public int getOppFreeThrowAtt() {
        return oppFreeThrowAtt;
    }

    public NbaSeasonData setOppFreeThrowAtt(int oppFreeThrowAtt) {
        this.oppFreeThrowAtt = oppFreeThrowAtt;
        return this;
    }

    public double getOppFreeThrowPct() {
        return oppFreeThrowPct;
    }

    public NbaSeasonData setOppFreeThrowPct(double oppFreeThrowPct) {
        this.oppFreeThrowPct = oppFreeThrowPct;
        return this;
    }

    public int getOppOffensiveRebounds() {
        return oppOffensiveRebounds;
    }

    public NbaSeasonData setOppOffensiveRebounds(int oppOffensiveRebounds) {
        this.oppOffensiveRebounds = oppOffensiveRebounds;
        return this;
    }

    public int getOppDefensiveRebounds() {
        return oppDefensiveRebounds;
    }

    public NbaSeasonData setOppDefensiveRebounds(int oppDefensiveRebounds) {
        this.oppDefensiveRebounds = oppDefensiveRebounds;
        return this;
    }

    public int getOppAssists() {
        return oppAssists;
    }

    public NbaSeasonData setOppAssists(int oppAssists) {
        this.oppAssists = oppAssists;
        return this;
    }

    public int getOppSteals() {
        return oppSteals;
    }

    public NbaSeasonData setOppSteals(int oppSteals) {
        this.oppSteals = oppSteals;
        return this;
    }

    public int getOppBlocks() {
        return oppBlocks;
    }

    public NbaSeasonData setOppBlocks(int oppBlocks) {
        this.oppBlocks = oppBlocks;
        return this;
    }

    public int getOppTurnovers() {
        return oppTurnovers;
    }

    public NbaSeasonData setOppTurnovers(int oppTurnovers) {
        this.oppTurnovers = oppTurnovers;
        return this;
    }

    public int getOppFouls() {
        return oppFouls;
    }

    public NbaSeasonData setOppFouls(int oppFouls) {
        this.oppFouls = oppFouls;
        return this;
    }

    public int getOppPoints() {
        return oppPoints;
    }

    public NbaSeasonData setOppPoints(int oppPoints) {
        this.oppPoints = oppPoints;
        return this;
    }

    public double getAgeOnFeb1() {
        return ageOnFeb1;
    }

    public NbaSeasonData setAgeOnFeb1(double ageOnFeb1) {
        this.ageOnFeb1 = ageOnFeb1;
        return this;
    }

    public int getWins() {
        return wins;
    }

    public NbaSeasonData setWins(int wins) {
        this.wins = wins;
        return this;
    }

    public int getLosses() {
        return losses;
    }

    public NbaSeasonData setLosses(int losses) {
        this.losses = losses;
        return this;
    }

    public int getPythagoreanWins() {
        return pythagoreanWins;
    }

    public NbaSeasonData setPythagoreanWins(int pythagoreanWins) {
        this.pythagoreanWins = pythagoreanWins;
        return this;
    }

    public int getPythagoreanLosses() {
        return pythagoreanLosses;
    }

    public NbaSeasonData setPythagoreanLosses(int pythagoreanLosses) {
        this.pythagoreanLosses = pythagoreanLosses;
        return this;
    }

    public double getMarginOfVictory() {
        return marginOfVictory;
    }

    public NbaSeasonData setMarginOfVictory(double marginOfVictory) {
        this.marginOfVictory = marginOfVictory;
        return this;
    }

    public double getStrengthOfSchedule() {
        return strengthOfSchedule;
    }

    public NbaSeasonData setStrengthOfSchedule(double strengthOfSchedule) {
        this.strengthOfSchedule = strengthOfSchedule;
        return this;
    }

    @Override
    public double getSimpleRatingSystem() {
        return simpleRatingSystem;
    }

    @Override
    public NbaSeasonData setSimpleRatingSystem(double simpleRatingSystem) {
        this.simpleRatingSystem = simpleRatingSystem;
        return this;
    }

    public double getOffensiveRating() {
        return offensiveRating;
    }

    public NbaSeasonData setOffensiveRating(double offensiveRating) {
        this.offensiveRating = offensiveRating;
        return this;
    }

    public double getDefensiveRating() {
        return defensiveRating;
    }

    public NbaSeasonData setDefensiveRating(double defensiveRating) {
        this.defensiveRating = defensiveRating;
        return this;
    }

    public double getNetRating() {
        return netRating;
    }

    public NbaSeasonData setNetRating(double netRating) {
        this.netRating = netRating;
        return this;
    }

    public double getPace() {
        return pace;
    }

    public NbaSeasonData setPace(double pace) {
        this.pace = pace;
        return this;
    }

    public double getFreeThrowAttRate() {
        return freeThrowAttRate;
    }

    public NbaSeasonData setFreeThrowAttRate(double freeThrowAttRate) {
        this.freeThrowAttRate = freeThrowAttRate;
        return this;
    }

    public double getThreePtAttRate() {
        return threePtAttRate;
    }

    public NbaSeasonData setThreePtAttRate(double threePtAttRate) {
        this.threePtAttRate = threePtAttRate;
        return this;
    }

    public double getTrueShootingPct() {
        return trueShootingPct;
    }

    public NbaSeasonData setTrueShootingPct(double trueShootingPct) {
        this.trueShootingPct = trueShootingPct;
        return this;
    }

    public double getEffFieldGoalPct() {
        return effFieldGoalPct;
    }

    public NbaSeasonData setEffFieldGoalPct(double effFieldGoalPct) {
        this.effFieldGoalPct = effFieldGoalPct;
        return this;
    }

    public double getTurnoverPct() {
        return turnoverPct;
    }

    public NbaSeasonData setTurnoverPct(double turnoverPct) {
        this.turnoverPct = turnoverPct;
        return this;
    }

    public double getOffensiveReboundPct() {
        return offensiveReboundPct;
    }

    public NbaSeasonData setOffensiveReboundPct(double offensiveReboundPct) {
        this.offensiveReboundPct = offensiveReboundPct;
        return this;
    }

    public double getFreeThrowMadeRate() {
        return freeThrowMadeRate;
    }

    public NbaSeasonData setFreeThrowMadeRate(double freeThrowMadeRate) {
        this.freeThrowMadeRate = freeThrowMadeRate;
        return this;
    }

    public double getOppEffFieldGoalPct() {
        return oppEffFieldGoalPct;
    }

    public NbaSeasonData setOppEffFieldGoalPct(double oppEffFieldGoalPct) {
        this.oppEffFieldGoalPct = oppEffFieldGoalPct;
        return this;
    }

    public double getOppTurnoverPct() {
        return oppTurnoverPct;
    }

    public NbaSeasonData setOppTurnoverPct(double oppTurnoverPct) {
        this.oppTurnoverPct = oppTurnoverPct;
        return this;
    }

    public double getOppOffensiveReboundPct() {
        return oppOffensiveReboundPct;
    }

    public NbaSeasonData setOppOffensiveReboundPct(double oppOffensiveReboundPct) {
        this.oppOffensiveReboundPct = oppOffensiveReboundPct;
        return this;
    }

    public double getOppFreeThrowMadeRate() {
        return oppFreeThrowMadeRate;
    }

    public NbaSeasonData setOppFreeThrowMadeRate(double oppFreeThrowMadeRate) {
        this.oppFreeThrowMadeRate = oppFreeThrowMadeRate;
        return this;
    }

    public double getAttendancePerGame() {
        return attendancePerGame;
    }

    public NbaSeasonData setAttendancePerGame(double attendancePerGame) {
        this.attendancePerGame = attendancePerGame;
        return this;
    }
}
