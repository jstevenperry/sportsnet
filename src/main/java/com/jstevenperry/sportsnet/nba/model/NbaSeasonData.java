/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

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

        if (isTraining) {
            ret.add(resultClass);
        }

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
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        final NbaSeasonData that = (NbaSeasonData) o;

        return new EqualsBuilder().append(teamStatsRank, that.teamStatsRank).append(gamesPlayed, that.gamesPlayed).append(minutesPlayed, that.minutesPlayed).append(fieldGoals, that.fieldGoals).append(fieldGoalAtt, that.fieldGoalAtt).append(fieldGoalPct, that.fieldGoalPct).append(threePointers, that.threePointers).append(threePointerAtt, that.threePointerAtt).append(threePointerPct, that.threePointerPct).append(twoPointers, that.twoPointers).append(twoPointerAtt, that.twoPointerAtt).append(twoPointerPct, that.twoPointerPct).append(freeThrows, that.freeThrows).append(freeThrowAtt, that.freeThrowAtt).append(freeThrowPct, that.freeThrowPct).append(offensiveRebounds, that.offensiveRebounds).append(defensiveRebounds, that.defensiveRebounds).append(assists, that.assists).append(steals, that.steals).append(blocks, that.blocks).append(turnovers, that.turnovers).append(fouls, that.fouls).append(points, that.points).append(oppStatsRank, that.oppStatsRank).append(oppGamesPlayed, that.oppGamesPlayed).append(oppMinutesPlayed, that.oppMinutesPlayed).append(oppFieldGoals, that.oppFieldGoals).append(oppFieldGoalAtt, that.oppFieldGoalAtt).append(oppFieldGoalPct, that.oppFieldGoalPct).append(oppThreePointers, that.oppThreePointers).append(oppThreePointerAtt, that.oppThreePointerAtt).append(oppThreePointerPct, that.oppThreePointerPct).append(oppTwoPointers, that.oppTwoPointers).append(oppTwoPointerAtt, that.oppTwoPointerAtt).append(oppTwoPointerPct, that.oppTwoPointerPct).append(oppFreeThrows, that.oppFreeThrows).append(oppFreeThrowAtt, that.oppFreeThrowAtt).append(oppFreeThrowPct, that.oppFreeThrowPct).append(oppOffensiveRebounds, that.oppOffensiveRebounds).append(oppDefensiveRebounds, that.oppDefensiveRebounds).append(oppAssists, that.oppAssists).append(oppSteals, that.oppSteals).append(oppBlocks, that.oppBlocks).append(oppTurnovers, that.oppTurnovers).append(oppFouls, that.oppFouls).append(oppPoints, that.oppPoints).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(teamStatsRank).append(gamesPlayed).append(minutesPlayed).append(fieldGoals).append(fieldGoalAtt).append(fieldGoalPct).append(threePointers).append(threePointerAtt).append(threePointerPct).append(twoPointers).append(twoPointerAtt).append(twoPointerPct).append(freeThrows).append(freeThrowAtt).append(freeThrowPct).append(offensiveRebounds).append(defensiveRebounds).append(assists).append(steals).append(blocks).append(turnovers).append(fouls).append(points).append(oppStatsRank).append(oppGamesPlayed).append(oppMinutesPlayed).append(oppFieldGoals).append(oppFieldGoalAtt).append(oppFieldGoalPct).append(oppThreePointers).append(oppThreePointerAtt).append(oppThreePointerPct).append(oppTwoPointers).append(oppTwoPointerAtt).append(oppTwoPointerPct).append(oppFreeThrows).append(oppFreeThrowAtt).append(oppFreeThrowPct).append(oppOffensiveRebounds).append(oppDefensiveRebounds).append(oppAssists).append(oppSteals).append(oppBlocks).append(oppTurnovers).append(oppFouls).append(oppPoints).toHashCode();
    }

    public int getTeamStatsRank() {
        return teamStatsRank;
    }

    public NbaSeasonData setTeamStatsRank(final int teamStatsRank) {
        this.teamStatsRank = teamStatsRank;
        return this;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public NbaSeasonData setGamesPlayed(final int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
        return this;
    }

    public int getMinutesPlayed() {
        return minutesPlayed;
    }

    public NbaSeasonData setMinutesPlayed(final int minutesPlayed) {
        this.minutesPlayed = minutesPlayed;
        return this;
    }

    public int getFieldGoals() {
        return fieldGoals;
    }

    public NbaSeasonData setFieldGoals(final int fieldGoals) {
        this.fieldGoals = fieldGoals;
        return this;
    }

    public int getFieldGoalAtt() {
        return fieldGoalAtt;
    }

    public NbaSeasonData setFieldGoalAtt(final int fieldGoalAtt) {
        this.fieldGoalAtt = fieldGoalAtt;
        return this;
    }

    public double getFieldGoalPct() {
        return fieldGoalPct;
    }

    public NbaSeasonData setFieldGoalPct(final double fieldGoalPct) {
        this.fieldGoalPct = fieldGoalPct;
        return this;
    }

    public int getThreePointers() {
        return threePointers;
    }

    public NbaSeasonData setThreePointers(final int threePointers) {
        this.threePointers = threePointers;
        return this;
    }

    public int getThreePointerAtt() {
        return threePointerAtt;
    }

    public NbaSeasonData setThreePointerAtt(final int threePointerAtt) {
        this.threePointerAtt = threePointerAtt;
        return this;
    }

    public double getThreePointerPct() {
        return threePointerPct;
    }

    public NbaSeasonData setThreePointerPct(final double threePointerPct) {
        this.threePointerPct = threePointerPct;
        return this;
    }

    public int getTwoPointers() {
        return twoPointers;
    }

    public NbaSeasonData setTwoPointers(final int twoPointers) {
        this.twoPointers = twoPointers;
        return this;
    }

    public int getTwoPointerAtt() {
        return twoPointerAtt;
    }

    public NbaSeasonData setTwoPointerAtt(final int twoPointerAtt) {
        this.twoPointerAtt = twoPointerAtt;
        return this;
    }

    public double getTwoPointerPct() {
        return twoPointerPct;
    }

    public NbaSeasonData setTwoPointerPct(final double twoPointerPct) {
        this.twoPointerPct = twoPointerPct;
        return this;
    }

    public int getFreeThrows() {
        return freeThrows;
    }

    public NbaSeasonData setFreeThrows(final int freeThrows) {
        this.freeThrows = freeThrows;
        return this;
    }

    public int getFreeThrowAtt() {
        return freeThrowAtt;
    }

    public NbaSeasonData setFreeThrowAtt(final int freeThrowAtt) {
        this.freeThrowAtt = freeThrowAtt;
        return this;
    }

    public double getFreeThrowPct() {
        return freeThrowPct;
    }

    public NbaSeasonData setFreeThrowPct(final double freeThrowPct) {
        this.freeThrowPct = freeThrowPct;
        return this;
    }

    public int getOffensiveRebounds() {
        return offensiveRebounds;
    }

    public NbaSeasonData setOffensiveRebounds(final int offensiveRebounds) {
        this.offensiveRebounds = offensiveRebounds;
        return this;
    }

    public int getDefensiveRebounds() {
        return defensiveRebounds;
    }

    public NbaSeasonData setDefensiveRebounds(final int defensiveRebounds) {
        this.defensiveRebounds = defensiveRebounds;
        return this;
    }

    public int getAssists() {
        return assists;
    }

    public NbaSeasonData setAssists(final int assists) {
        this.assists = assists;
        return this;
    }

    public int getSteals() {
        return steals;
    }

    public NbaSeasonData setSteals(final int steals) {
        this.steals = steals;
        return this;
    }

    public int getBlocks() {
        return blocks;
    }

    public NbaSeasonData setBlocks(final int blocks) {
        this.blocks = blocks;
        return this;
    }

    public int getTurnovers() {
        return turnovers;
    }

    public NbaSeasonData setTurnovers(final int turnovers) {
        this.turnovers = turnovers;
        return this;
    }

    public int getFouls() {
        return fouls;
    }

    public NbaSeasonData setFouls(final int fouls) {
        this.fouls = fouls;
        return this;
    }

    public int getPoints() {
        return points;
    }

    public NbaSeasonData setPoints(final int points) {
        this.points = points;
        return this;
    }

    public int getOppStatsRank() {
        return oppStatsRank;
    }

    public NbaSeasonData setOppStatsRank(final int oppStatsRank) {
        this.oppStatsRank = oppStatsRank;
        return this;
    }

    public int getOppGamesPlayed() {
        return oppGamesPlayed;
    }

    public NbaSeasonData setOppGamesPlayed(final int oppGamesPlayed) {
        this.oppGamesPlayed = oppGamesPlayed;
        return this;
    }

    public int getOppMinutesPlayed() {
        return oppMinutesPlayed;
    }

    public NbaSeasonData setOppMinutesPlayed(final int oppMinutesPlayed) {
        this.oppMinutesPlayed = oppMinutesPlayed;
        return this;
    }

    public int getOppFieldGoals() {
        return oppFieldGoals;
    }

    public NbaSeasonData setOppFieldGoals(final int oppFieldGoals) {
        this.oppFieldGoals = oppFieldGoals;
        return this;
    }

    public int getOppFieldGoalAtt() {
        return oppFieldGoalAtt;
    }

    public NbaSeasonData setOppFieldGoalAtt(final int oppFieldGoalAtt) {
        this.oppFieldGoalAtt = oppFieldGoalAtt;
        return this;
    }

    public double getOppFieldGoalPct() {
        return oppFieldGoalPct;
    }

    public NbaSeasonData setOppFieldGoalPct(final double oppFieldGoalPct) {
        this.oppFieldGoalPct = oppFieldGoalPct;
        return this;
    }

    public int getOppThreePointers() {
        return oppThreePointers;
    }

    public NbaSeasonData setOppThreePointers(final int oppThreePointers) {
        this.oppThreePointers = oppThreePointers;
        return this;
    }

    public int getOppThreePointerAtt() {
        return oppThreePointerAtt;
    }

    public NbaSeasonData setOppThreePointerAtt(final int oppThreePointerAtt) {
        this.oppThreePointerAtt = oppThreePointerAtt;
        return this;
    }

    public double getOppThreePointerPct() {
        return oppThreePointerPct;
    }

    public NbaSeasonData setOppThreePointerPct(final double oppThreePointerPct) {
        this.oppThreePointerPct = oppThreePointerPct;
        return this;
    }

    public int getOppTwoPointers() {
        return oppTwoPointers;
    }

    public NbaSeasonData setOppTwoPointers(final int oppTwoPointers) {
        this.oppTwoPointers = oppTwoPointers;
        return this;
    }

    public int getOppTwoPointerAtt() {
        return oppTwoPointerAtt;
    }

    public NbaSeasonData setOppTwoPointerAtt(final int oppTwoPointerAtt) {
        this.oppTwoPointerAtt = oppTwoPointerAtt;
        return this;
    }

    public double getOppTwoPointerPct() {
        return oppTwoPointerPct;
    }

    public NbaSeasonData setOppTwoPointerPct(final double oppTwoPointerPct) {
        this.oppTwoPointerPct = oppTwoPointerPct;
        return this;
    }

    public int getOppFreeThrows() {
        return oppFreeThrows;
    }

    public NbaSeasonData setOppFreeThrows(final int oppFreeThrows) {
        this.oppFreeThrows = oppFreeThrows;
        return this;
    }

    public int getOppFreeThrowAtt() {
        return oppFreeThrowAtt;
    }

    public NbaSeasonData setOppFreeThrowAtt(final int oppFreeThrowAtt) {
        this.oppFreeThrowAtt = oppFreeThrowAtt;
        return this;
    }

    public double getOppFreeThrowPct() {
        return oppFreeThrowPct;
    }

    public NbaSeasonData setOppFreeThrowPct(final double oppFreeThrowPct) {
        this.oppFreeThrowPct = oppFreeThrowPct;
        return this;
    }

    public int getOppOffensiveRebounds() {
        return oppOffensiveRebounds;
    }

    public NbaSeasonData setOppOffensiveRebounds(final int oppOffensiveRebounds) {
        this.oppOffensiveRebounds = oppOffensiveRebounds;
        return this;
    }

    public int getOppDefensiveRebounds() {
        return oppDefensiveRebounds;
    }

    public NbaSeasonData setOppDefensiveRebounds(final int oppDefensiveRebounds) {
        this.oppDefensiveRebounds = oppDefensiveRebounds;
        return this;
    }

    public int getOppAssists() {
        return oppAssists;
    }

    public NbaSeasonData setOppAssists(final int oppAssists) {
        this.oppAssists = oppAssists;
        return this;
    }

    public int getOppSteals() {
        return oppSteals;
    }

    public NbaSeasonData setOppSteals(final int oppSteals) {
        this.oppSteals = oppSteals;
        return this;
    }

    public int getOppBlocks() {
        return oppBlocks;
    }

    public NbaSeasonData setOppBlocks(final int oppBlocks) {
        this.oppBlocks = oppBlocks;
        return this;
    }

    public int getOppTurnovers() {
        return oppTurnovers;
    }

    public NbaSeasonData setOppTurnovers(final int oppTurnovers) {
        this.oppTurnovers = oppTurnovers;
        return this;
    }

    public int getOppFouls() {
        return oppFouls;
    }

    public NbaSeasonData setOppFouls(final int oppFouls) {
        this.oppFouls = oppFouls;
        return this;
    }

    public int getOppPoints() {
        return oppPoints;
    }

    public NbaSeasonData setOppPoints(final int oppPoints) {
        this.oppPoints = oppPoints;
        return this;
    }
}
