/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */
package com.jstevenperry.sportsnet.ncaabb.model;

import com.jstevenperry.sportsnet.framework.model.AbstractSeasonData;

import java.util.ArrayList;
import java.util.List;

public class NcaaBbSeasonData extends AbstractSeasonData {

    private int rank;
    private int gamesPlayed;
    private int gamesWon;
    private int gamesLost;
    private double simpleRatingSystem;
    private double strengthOfSchedule;
    private int conferenceWins;
    private int conferenceLosses;
    private int homeWins;
    private int homeLosses;
    private int awayWins;
    private int awayLosses;
    private int pointsScored;
    private int opponentPointsScored;
    private int minutesPlayed;
    private int fieldGoals;
    private int fieldGoalAttempts;
    private double fieldGoalPct;
    private int threePointers;
    private int threePointAttempts;
    private double threePointPct;
    private int freeThrows;
    private int freeThrowAttempts;
    private double freeThrowPct;
    private int offensiveRebounds;
    private int totalRebounds;
    private int assists;
    private int steals;
    private int blocks;
    private int turnovers;
    private int personalFouls;

    public static List<Double> asDoubleList(final NcaaBbSeasonData seasonData,
                                            final Double resultClass,
                                            final boolean isTraining) {
        List<Double> ret = new ArrayList<>();
        //
        //ret.add((double) seasonData.getRank()); // Noise
        ret.add(0.0); // Rank is just noise (it's actually just a team's alphabetical position, which is meaningless)
        ret.add((double) seasonData.getGamesPlayed());
        ret.add((double) seasonData.getGamesWon());
        ret.add((double) seasonData.getGamesLost());
        ret.add(seasonData.getWonLostPercentage().doubleValue());
        ret.add(seasonData.getSimpleRatingSystem());
        ret.add(seasonData.getStrengthOfSchedule());
        ret.add((double) seasonData.getConferenceWins());
        ret.add((double) seasonData.getConferenceLosses());
        ret.add((double) seasonData.getHomeWins());
        ret.add((double) seasonData.getHomeLosses());
        ret.add((double) seasonData.getAwayWins());
        ret.add((double) seasonData.getAwayLosses());
        ret.add((double) seasonData.getPointsScored());
        ret.add((double) seasonData.getOpponentPointsScored());
        ret.add((double) seasonData.getMinutesPlayed());
        ret.add((double) seasonData.getFieldGoals());
        ret.add((double) seasonData.getFieldGoalAttempts());
        ret.add(seasonData.getFieldGoalPct());
        ret.add((double) seasonData.getThreePointers());
        ret.add((double) seasonData.getThreePointAttempts());
        ret.add(seasonData.getThreePointPct());
        ret.add((double) seasonData.getFreeThrows());
        ret.add((double) seasonData.getFreeThrowAttempts());
        ret.add(seasonData.getFreeThrowPct());
        ret.add((double) seasonData.getOffensiveRebounds());
        ret.add((double) seasonData.getTotalRebounds());
        ret.add((double) seasonData.getAssists());
        ret.add((double) seasonData.getSteals());
        ret.add((double) seasonData.getBlocks());
        ret.add((double) seasonData.getTurnovers());
        ret.add((double) seasonData.getPersonalFouls());
        // Class - the number of tournament victories
        if (isTraining) {
            ret.add(resultClass);
        }
        return ret;
    }

    public NcaaBbSeasonData() {
        // Nothing to do
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public NcaaBbSeasonData setGamesPlayed(final int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
        return this;
    }

    public int getGamesWon() {
        return gamesWon;
    }

    public NcaaBbSeasonData setGamesWon(final int gamesWon) {
        this.gamesWon = gamesWon;
        return this;
    }

    public int getGamesLost() {
        return gamesLost;
    }

    public NcaaBbSeasonData setGamesLost(final int gamesLost) {
        this.gamesLost = gamesLost;
        return this;
    }

    @Override
    public double getSimpleRatingSystem() {
        return simpleRatingSystem;
    }

    @Override
    public NcaaBbSeasonData setSimpleRatingSystem(final double simpleRatingSystem) {
        this.simpleRatingSystem = simpleRatingSystem;
        return this;
    }

    public double getStrengthOfSchedule() {
        return strengthOfSchedule;
    }

    public NcaaBbSeasonData setStrengthOfSchedule(final double strengthOfSchedule) {
        this.strengthOfSchedule = strengthOfSchedule;
        return this;
    }

    public int getConferenceWins() {
        return conferenceWins;
    }

    public NcaaBbSeasonData setConferenceWins(final int conferenceWins) {
        this.conferenceWins = conferenceWins;
        return this;
    }

    public int getConferenceLosses() {
        return conferenceLosses;
    }

    public NcaaBbSeasonData setConferenceLosses(final int conferenceLosses) {
        this.conferenceLosses = conferenceLosses;
        return this;
    }

    public int getHomeWins() {
        return homeWins;
    }

    public NcaaBbSeasonData setHomeWins(final int homeWins) {
        this.homeWins = homeWins;
        return this;
    }

    public int getHomeLosses() {
        return homeLosses;
    }

    public NcaaBbSeasonData setHomeLosses(final int homeLosses) {
        this.homeLosses = homeLosses;
        return this;
    }

    public int getAwayWins() {
        return awayWins;
    }

    public NcaaBbSeasonData setAwayWins(final int awayWins) {
        this.awayWins = awayWins;
        return this;
    }

    public int getAwayLosses() {
        return awayLosses;
    }

    public NcaaBbSeasonData setAwayLosses(final int awayLosses) {
        this.awayLosses = awayLosses;
        return this;
    }

    public int getPointsScored() {
        return pointsScored;
    }

    public NcaaBbSeasonData setPointsScored(final int pointsScored) {
        this.pointsScored = pointsScored;
        return this;
    }

    public int getOpponentPointsScored() {
        return opponentPointsScored;
    }

    public NcaaBbSeasonData setOpponentPointsScored(final int opponentPointsScored) {
        this.opponentPointsScored = opponentPointsScored;
        return this;
    }

    public int getMinutesPlayed() {
        return minutesPlayed;
    }

    public NcaaBbSeasonData setMinutesPlayed(final int minutesPlayed) {
        this.minutesPlayed = minutesPlayed;
        return this;
    }

    public int getFieldGoals() {
        return fieldGoals;
    }

    public NcaaBbSeasonData setFieldGoals(final int fieldGoals) {
        this.fieldGoals = fieldGoals;
        return this;
    }

    public int getFieldGoalAttempts() {
        return fieldGoalAttempts;
    }

    public NcaaBbSeasonData setFieldGoalAttempts(final int fieldGoalAttempts) {
        this.fieldGoalAttempts = fieldGoalAttempts;
        return this;
    }

    public double getFieldGoalPct() {
        return fieldGoalPct;
    }

    public NcaaBbSeasonData setFieldGoalPct(final double fieldGoalPct) {
        this.fieldGoalPct = fieldGoalPct;
        return this;
    }

    public int getThreePointers() {
        return threePointers;
    }

    public NcaaBbSeasonData setThreePointers(final int threePointers) {
        this.threePointers = threePointers;
        return this;
    }

    public int getThreePointAttempts() {
        return threePointAttempts;
    }

    public NcaaBbSeasonData setThreePointAttempts(final int threePointAttempts) {
        this.threePointAttempts = threePointAttempts;
        return this;
    }

    public double getThreePointPct() {
        return threePointPct;
    }

    public NcaaBbSeasonData setThreePointPct(final double threePointPct) {
        this.threePointPct = threePointPct;
        return this;
    }

    public int getFreeThrows() {
        return freeThrows;
    }

    public NcaaBbSeasonData setFreeThrows(final int freeThrows) {
        this.freeThrows = freeThrows;
        return this;
    }

    public int getFreeThrowAttempts() {
        return freeThrowAttempts;
    }

    public NcaaBbSeasonData setFreeThrowAttempts(final int freeThrowAttempts) {
        this.freeThrowAttempts = freeThrowAttempts;
        return this;
    }

    public double getFreeThrowPct() {
        return freeThrowPct;
    }

    public NcaaBbSeasonData setFreeThrowPct(final double freeThrowPct) {
        this.freeThrowPct = freeThrowPct;
        return this;
    }

    public int getOffensiveRebounds() {
        return offensiveRebounds;
    }

    public NcaaBbSeasonData setOffensiveRebounds(final int offensiveRebounds) {
        this.offensiveRebounds = offensiveRebounds;
        return this;
    }

    public int getTotalRebounds() {
        return totalRebounds;
    }

    public NcaaBbSeasonData setTotalRebounds(final int totalRebounds) {
        this.totalRebounds = totalRebounds;
        return this;
    }

    public int getAssists() {
        return assists;
    }

    public NcaaBbSeasonData setAssists(final int assists) {
        this.assists = assists;
        return this;
    }

    public int getSteals() {
        return steals;
    }

    public NcaaBbSeasonData setSteals(final int steals) {
        this.steals = steals;
        return this;
    }

    public int getBlocks() {
        return blocks;
    }

    public NcaaBbSeasonData setBlocks(final int blocks) {
        this.blocks = blocks;
        return this;
    }

    public int getTurnovers() {
        return turnovers;
    }

    public NcaaBbSeasonData setTurnovers(final int turnovers) {
        this.turnovers = turnovers;
        return this;
    }

    public int getPersonalFouls() {
        return personalFouls;
    }

    public NcaaBbSeasonData setPersonalFouls(final int personalFouls) {
        this.personalFouls = personalFouls;
        return this;
    }

    public int getRank() {
        return rank;
    }

    public NcaaBbSeasonData setRank(final int rank) {
        this.rank = rank;
        return this;
    }
}
