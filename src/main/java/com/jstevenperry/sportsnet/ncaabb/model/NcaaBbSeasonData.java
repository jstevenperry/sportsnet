/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */
package com.jstevenperry.sportsnet.ncaabb.model;

import com.jstevenperry.sportsnet.framework.model.SeasonData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class NcaaBbSeasonData extends SeasonData {

    private static final Logger log = LoggerFactory.getLogger(NcaaBbSeasonData.class);

    private int rank; // not used
    private double gamesPlayed; // not used
    private int gamesWon; // not used
    private int gamesLost; // not used
    private double simpleRatingSystem;
    private double strengthOfSchedule;
    private int conferenceWins; // not used
    private int conferenceLosses; // not used
    private int homeWins; // not used
    private int homeLosses; // not used
    private int awayWins; // not used
    private int awayLosses; // not used
    private int pointsScored; // Per Game
    private int opponentPointsScored; // Per Game
    private int minutesPlayed; // not used
    private double fieldGoals; // Per Game
    private double fieldGoalAttempts; // Per Game
    private double fieldGoalPct;
    private double threePointers; // Per Game
    private double threePointAttempts; // Per Game
    private double threePointPct;
    private double freeThrows; // Per Game
    private double freeThrowAttempts; // Per Game
    private double freeThrowPct;
    private double offensiveRebounds; // Per Game
    private double totalRebounds; // Per Game
    private double assists; // Per Game
    private double steals; // Per Game
    private double blocks; // Per Game
    private double turnovers; // Per Game
    private double personalFouls; // Per Game
    //
    // Opponent data
    private double opponentFieldGoals; // Per Game
    private double opponentFieldGoalAttempts; // Per Game
    private double opponentFieldGoalPct;
    private double opponentThreePointers; // Per Game
    private double opponentThreePointAttempts; // Per Game
    private double opponentThreePointPct;
    private double opponentFreeThrows; // Per Game
    private double opponentFreeThrowAttempts; // Per Game
    private double opponentFreeThrowPct;
    private double opponentOffensiveRebounds; // Per Game
    private double opponentTotalRebounds; // Per Game
    private double opponentAssists; // Per Game
    private double opponentSteals; // Per Game
    private double opponentBlocks; // Per Game
    private double opponentTurnovers; // Per Game
    private double opponentPersonalFouls; // Per Game

    //
    // Advanced data
    private double pace;
    private double offensiveRating;
    private double freeThrowAttemptRate;
    private double threePointAttemptRate;
    private double trueShootingPct;
    private double totalReboundPct;
    private double assistPct;
    private double stealPct;
    private double blockPct;
    private double effectiveFgPct;
    private double turnoverPct;
    private double offensiveReboundPct;
    private double freeThrowsPerFgAttempt;

    //
    // Opponent Advanced data
    private double opponentPace;
    private double opponentOffensiveRating;
    private double opponentFreeThrowAttemptRate;
    private double opponentThreePointAttemptRate;
    private double opponentTrueShootingPct;
    private double opponentTotalReboundPct;
    private double opponentAssistPct;
    private double opponentStealPct;
    private double opponentBlockPct;
    private double opponentEffectiveFgPct;
    private double opponentTurnoverPct;
    private double opponentOffensiveReboundPct;
    private double opponentFreeThrowsPerFgAttempt;


    public static List<Double> asDoubleList(final NcaaBbSeasonData seasonData,
                                            final Double resultClass,
                                            final boolean isTraining) {
        log.debug("asDoubleList(): Returning season data as double list...");
        List<Double> ret = new ArrayList<>();
        //
        // Note: most of these are computed as "per game" stats to put all teams on an even
        // footing. The data provider pollutes NCAABB data by including tournament games in the season
        // data, biasing networks towards teams that have been in the tournaments in years past.
        // Using per-game stats is a feeble attempt to correct for this.
        //
        // Basic data
        ret.add((seasonData.getWonLostPercentage() != null) ? seasonData
                .getWonLostPercentage()
                .doubleValue() : 0.0);//0
        ret.add(seasonData.getSimpleRatingSystem());
        ret.add(seasonData.getStrengthOfSchedule());
        ret.add(seasonData.getPointsScored());
        ret.add(seasonData.getOpponentPointsScored());
        ret.add(seasonData.getFieldGoals());
        ret.add(seasonData.getFieldGoalAttempts());
        ret.add(seasonData.getFieldGoalPct());
        ret.add(seasonData.getThreePointers());
        ret.add(seasonData.getThreePointAttempts());
        ret.add(seasonData.getThreePointPct());//10
        ret.add(seasonData.getFreeThrows());
        ret.add(seasonData.getFreeThrowAttempts());
        ret.add(seasonData.getFreeThrowPct());
        ret.add(seasonData.getOffensiveRebounds());
        ret.add(seasonData.getTotalRebounds());
        ret.add(seasonData.getAssists());
        ret.add(seasonData.getSteals());
        ret.add(seasonData.getBlocks());
        ret.add(seasonData.getTurnovers());
        ret.add(seasonData.getPersonalFouls());//20
        //
        // Opponent Data
        ret.add(seasonData.getOpponentPointsScored());//21
        ret.add(seasonData.getOpponentFieldGoals());
        ret.add(seasonData.getOpponentFieldGoalAttempts());
        ret.add(seasonData.getOpponentFieldGoalPct());
        ret.add(seasonData.getOpponentThreePointers());
        ret.add(seasonData.getOpponentThreePointAttempts());
        ret.add(seasonData.getOpponentThreePointPct());
        ret.add(seasonData.getOpponentFreeThrows());
        ret.add(seasonData.getOpponentFreeThrowAttempts());
        ret.add(seasonData.getOpponentFreeThrowPct());//30
        ret.add(seasonData.getOpponentOffensiveRebounds());
        ret.add(seasonData.getOpponentTotalRebounds());
        ret.add(seasonData.getOpponentAssists());
        ret.add(seasonData.getOpponentSteals());
        ret.add(seasonData.getOpponentBlocks());
        ret.add(seasonData.getOpponentTurnovers());
        ret.add(seasonData.getOpponentPersonalFouls());//37
        //
        // Advanced data
        ret.add(seasonData.getPace());//38
        ret.add(seasonData.getOffensiveRating());
        ret.add(seasonData.getFreeThrowAttemptRate());//40
        ret.add(seasonData.getThreePointAttemptRate());
        ret.add(seasonData.getTrueShootingPct());
        ret.add(seasonData.getTotalReboundPct());
        ret.add(seasonData.getAssistPct());
        ret.add(seasonData.getStealPct());
        ret.add(seasonData.getBlockPct());
        ret.add(seasonData.getEffectiveFgPct());
        ret.add(seasonData.getTurnoverPct());
        ret.add(seasonData.getOffensiveReboundPct());
        ret.add(seasonData.getFreeThrowsPerFgAttempt());//50

        //
        // Opponent Advanced data
        ret.add(seasonData.getOpponentPace());//51
        ret.add(seasonData.getOpponentOffensiveRating());
        ret.add(seasonData.getOpponentFreeThrowAttemptRate());
        ret.add(seasonData.getOpponentThreePointAttemptRate());
        ret.add(seasonData.getOpponentTrueShootingPct());
        ret.add(seasonData.getOpponentTotalReboundPct());
        ret.add(seasonData.getOpponentAssistPct());
        ret.add(seasonData.getOpponentStealPct());
        ret.add(seasonData.getOpponentBlockPct());
        ret.add(seasonData.getOpponentEffectiveFgPct());//60
        ret.add(seasonData.getOpponentTurnoverPct());
        ret.add(seasonData.getOpponentOffensiveReboundPct());
        ret.add(seasonData.getOpponentFreeThrowsPerFgAttempt());//63

        // Class - the number of tournament victories
        if (isTraining) {
            ret.add(resultClass);
        }
        return ret;
    }

    public NcaaBbSeasonData() {
        // Nothing to do
    }

    /**
     * Return the number of fields in this SeasonData object. This is needed to compute the
     * network layout, configure the  network, etc. There is probably a more clever way of
     * setting this value, but doing it here makes sense because this is as close to the
     * data as it gets.
     */
    @Override
    public int getNumberOfFields() {
        return NcaaBbSeasonData
                .asDoubleList(new NcaaBbSeasonData(), 0.0, false)
                .size();
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

    public NcaaBbSeasonData setGamesPlayed(final int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
        return this;
    }

    public NcaaBbSeasonData setGamesWon(final int gamesWon) {
        this.gamesWon = gamesWon;
        return this;
    }

    public NcaaBbSeasonData setGamesLost(final int gamesLost) {
        this.gamesLost = gamesLost;
        return this;
    }

    public double getStrengthOfSchedule() {
        return strengthOfSchedule;
    }

    public NcaaBbSeasonData setStrengthOfSchedule(final double strengthOfSchedule) {
        this.strengthOfSchedule = strengthOfSchedule;
        return this;
    }

    public NcaaBbSeasonData setConferenceWins(final int conferenceWins) {
        this.conferenceWins = conferenceWins;
        return this;
    }

    public NcaaBbSeasonData setConferenceLosses(final int conferenceLosses) {
        this.conferenceLosses = conferenceLosses;
        return this;
    }

    public NcaaBbSeasonData setHomeWins(final int homeWins) {
        this.homeWins = homeWins;
        return this;
    }

    public NcaaBbSeasonData setHomeLosses(final int homeLosses) {
        this.homeLosses = homeLosses;
        return this;
    }

    public NcaaBbSeasonData setAwayWins(final int awayWins) {
        this.awayWins = awayWins;
        return this;
    }

    public NcaaBbSeasonData setAwayLosses(final int awayLosses) {
        this.awayLosses = awayLosses;
        return this;
    }

    public double getPointsScored() {
        return 1.0 * pointsScored / Math.max(1.0, gamesPlayed);
    }

    public NcaaBbSeasonData setPointsScored(final int pointsScored) {
        this.pointsScored = pointsScored;
        return this;
    }

    public double getOpponentPointsScored() {
        return 1.0 * opponentPointsScored / Math.max(1.0, gamesPlayed);
    }

    public NcaaBbSeasonData setOpponentPointsScored(final int opponentPointsScored) {
        this.opponentPointsScored = opponentPointsScored;
        return this;
    }

    public NcaaBbSeasonData setMinutesPlayed(final int minutesPlayed) {
        this.minutesPlayed = minutesPlayed;
        return this;
    }

    public double getFieldGoals() {
        return fieldGoals / Math.max(1.0, gamesPlayed);
    }

    public NcaaBbSeasonData setFieldGoals(final int fieldGoals) {
        this.fieldGoals = fieldGoals;
        return this;
    }

    public double getFieldGoalAttempts() {
        return fieldGoalAttempts / Math.max(1.0, gamesPlayed);
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

    public double getThreePointers() {
        return threePointers / Math.max(1.0, gamesPlayed);
    }

    public NcaaBbSeasonData setThreePointers(final int threePointers) {
        this.threePointers = threePointers;
        return this;
    }

    public double getThreePointAttempts() {
        return threePointAttempts / Math.max(1.0, gamesPlayed);
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

    public double getFreeThrows() {
        return freeThrows / Math.max(1.0, gamesPlayed);
    }

    public NcaaBbSeasonData setFreeThrows(final int freeThrows) {
        this.freeThrows = freeThrows;
        return this;
    }

    public double getFreeThrowAttempts() {
        return freeThrowAttempts / Math.max(1.0, gamesPlayed);
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

    public double getOffensiveRebounds() {
        return offensiveRebounds / Math.max(1.0, gamesPlayed);
    }

    public NcaaBbSeasonData setOffensiveRebounds(final int offensiveRebounds) {
        this.offensiveRebounds = offensiveRebounds;
        return this;
    }

    public double getTotalRebounds() {
        return totalRebounds / Math.max(1.0, gamesPlayed);
    }

    public NcaaBbSeasonData setTotalRebounds(final int totalRebounds) {
        this.totalRebounds = totalRebounds;
        return this;
    }

    public double getAssists() {
        return assists / Math.max(1.0, gamesPlayed);
    }

    public NcaaBbSeasonData setAssists(final int assists) {
        this.assists = assists;
        return this;
    }

    public double getSteals() {
        return steals / Math.max(1.0, gamesPlayed);
    }

    public NcaaBbSeasonData setSteals(final int steals) {
        this.steals = steals;
        return this;
    }

    public double getBlocks() {
        return blocks / Math.max(1.0, gamesPlayed);
    }

    public NcaaBbSeasonData setBlocks(final int blocks) {
        this.blocks = blocks;
        return this;
    }

    public double getTurnovers() {
        return turnovers / Math.max(1.0, gamesPlayed);
    }

    public NcaaBbSeasonData setTurnovers(final int turnovers) {
        this.turnovers = turnovers;
        return this;
    }

    public double getPersonalFouls() {
        return personalFouls / Math.max(1.0, gamesPlayed);
    }

    public NcaaBbSeasonData setPersonalFouls(final int personalFouls) {
        this.personalFouls = personalFouls;
        return this;
    }

    public NcaaBbSeasonData setRank(final int rank) {
        this.rank = rank;
        return this;
    }

    //
    // Opponent data

    public double getOpponentFieldGoals() {
        return opponentFieldGoals / Math.max(1.0, gamesPlayed);
    }

    public NcaaBbSeasonData setOpponentFieldGoals(final int opponentFieldGoals) {
        this.opponentFieldGoals = opponentFieldGoals;
        return this;
    }

    public double getOpponentFieldGoalAttempts() {
        return opponentFieldGoalAttempts / Math.max(1.0, gamesPlayed);
    }

    public NcaaBbSeasonData setOpponentFieldGoalAttempts(final int opponentFieldGoalAttempts) {
        this.opponentFieldGoalAttempts = opponentFieldGoalAttempts;
        return this;
    }

    public double getOpponentFieldGoalPct() {
        return opponentFieldGoalPct;
    }

    public NcaaBbSeasonData setOpponentFieldGoalPct(final double opponentFieldGoalPct) {
        this.opponentFieldGoalPct = opponentFieldGoalPct;
        return this;
    }

    public double getOpponentThreePointers() {
        return opponentThreePointers / Math.max(1.0, gamesPlayed);
    }

    public NcaaBbSeasonData setOpponentThreePointers(final int opponentThreePointers) {
        this.opponentThreePointers = opponentThreePointers;
        return this;
    }

    public double getOpponentThreePointAttempts() {
        return opponentThreePointAttempts / Math.max(1.0, gamesPlayed);
    }

    public NcaaBbSeasonData setOpponentThreePointAttempts(final int opponentThreePointAttempts) {
        this.opponentThreePointAttempts = opponentThreePointAttempts;
        return this;
    }

    public double getOpponentThreePointPct() {
        return opponentThreePointPct;
    }

    public NcaaBbSeasonData setOpponentThreePointPct(final double opponentThreePointPct) {
        this.opponentThreePointPct = opponentThreePointPct;
        return this;
    }

    public double getOpponentFreeThrows() {
        return opponentFreeThrows / Math.max(1.0, gamesPlayed);
    }

    public NcaaBbSeasonData setOpponentFreeThrows(final int opponentFreeThrows) {
        this.opponentFreeThrows = opponentFreeThrows;
        return this;
    }

    public double getOpponentFreeThrowAttempts() {
        return opponentFreeThrowAttempts / Math.max(1.0, gamesPlayed);
    }

    public NcaaBbSeasonData setOpponentFreeThrowAttempts(final int opponentFreeThrowAttempts) {
        this.opponentFreeThrowAttempts = opponentFreeThrowAttempts;
        return this;
    }

    public double getOpponentFreeThrowPct() {
        return opponentFreeThrowPct;
    }

    public NcaaBbSeasonData setOpponentFreeThrowPct(final double opponentFreeThrowPct) {
        this.opponentFreeThrowPct = opponentFreeThrowPct;
        return this;
    }

    public double getOpponentOffensiveRebounds() {
        return opponentOffensiveRebounds / Math.max(1.0, gamesPlayed);
    }

    public NcaaBbSeasonData setOpponentOffensiveRebounds(final int opponentOffensiveRebounds) {
        this.opponentOffensiveRebounds = opponentOffensiveRebounds;
        return this;
    }

    public double getOpponentTotalRebounds() {
        return opponentTotalRebounds / Math.max(1.0, gamesPlayed);
    }

    public NcaaBbSeasonData setOpponentTotalRebounds(final int opponentTotalRebounds) {
        this.opponentTotalRebounds = opponentTotalRebounds;
        return this;
    }

    public double getOpponentAssists() {
        return opponentAssists / Math.max(1.0, gamesPlayed);
    }

    public NcaaBbSeasonData setOpponentAssists(final int opponentAssists) {
        this.opponentAssists = opponentAssists;
        return this;
    }

    public double getOpponentSteals() {
        return opponentSteals / Math.max(1.0, gamesPlayed);
    }

    public NcaaBbSeasonData setOpponentSteals(final int opponentSteals) {
        this.opponentSteals = opponentSteals;
        return this;
    }

    public double getOpponentBlocks() {
        return opponentBlocks / Math.max(1.0, gamesPlayed);
    }

    public NcaaBbSeasonData setOpponentBlocks(final int opponentBlocks) {
        this.opponentBlocks = opponentBlocks;
        return this;
    }

    public double getOpponentTurnovers() {
        return opponentTurnovers / Math.max(1.0, gamesPlayed);
    }

    public NcaaBbSeasonData setOpponentTurnovers(final int opponentTurnovers) {
        this.opponentTurnovers = opponentTurnovers;
        return this;
    }

    public double getOpponentPersonalFouls() {
        return opponentPersonalFouls / Math.max(1.0, gamesPlayed);
    }

    public NcaaBbSeasonData setOpponentPersonalFouls(final int opponentPersonalFouls) {
        this.opponentPersonalFouls = opponentPersonalFouls;
        return this;
    }

    public double getPace() {
        return pace;
    }

    public NcaaBbSeasonData setPace(final double pace) {
        this.pace = pace;
        return this;
    }

    public double getOffensiveRating() {
        return offensiveRating;
    }

    public NcaaBbSeasonData setOffensiveRating(final double offensiveRating) {
        this.offensiveRating = offensiveRating;
        return this;
    }

    public double getFreeThrowAttemptRate() {
        return freeThrowAttemptRate;
    }

    public NcaaBbSeasonData setFreeThrowAttemptRate(final double freeThrowAttemptRate) {
        this.freeThrowAttemptRate = freeThrowAttemptRate;
        return this;
    }

    public double getThreePointAttemptRate() {
        return threePointAttemptRate;
    }

    public NcaaBbSeasonData setThreePointAttemptRate(final double threePointAttemptRate) {
        this.threePointAttemptRate = threePointAttemptRate;
        return this;
    }

    public double getTrueShootingPct() {
        return trueShootingPct;
    }

    public NcaaBbSeasonData setTrueShootingPct(final double trueShootingPct) {
        this.trueShootingPct = trueShootingPct;
        return this;
    }

    public double getTotalReboundPct() {
        return totalReboundPct;
    }

    public NcaaBbSeasonData setTotalReboundPct(final double totalReboundPct) {
        this.totalReboundPct = totalReboundPct;
        return this;
    }

    public double getAssistPct() {
        return assistPct;
    }

    public NcaaBbSeasonData setAssistPct(final double assistPct) {
        this.assistPct = assistPct;
        return this;
    }

    public double getStealPct() {
        return stealPct;
    }

    public NcaaBbSeasonData setStealPct(final double stealPct) {
        this.stealPct = stealPct;
        return this;
    }

    public double getBlockPct() {
        return blockPct;
    }

    public NcaaBbSeasonData setBlockPct(final double blockPct) {
        this.blockPct = blockPct;
        return this;
    }

    public double getEffectiveFgPct() {
        return effectiveFgPct;
    }

    public NcaaBbSeasonData setEffectiveFgPct(final double effectiveFgPct) {
        this.effectiveFgPct = effectiveFgPct;
        return this;
    }

    public double getTurnoverPct() {
        return turnoverPct;
    }

    public NcaaBbSeasonData setTurnoverPct(final double turnoverPct) {
        this.turnoverPct = turnoverPct;
        return this;
    }

    public double getOffensiveReboundPct() {
        return offensiveReboundPct;
    }

    public NcaaBbSeasonData setOffensiveReboundPct(final double offensiveReboundPct) {
        this.offensiveReboundPct = offensiveReboundPct;
        return this;
    }

    public double getFreeThrowsPerFgAttempt() {
        return freeThrowsPerFgAttempt;
    }

    public NcaaBbSeasonData setFreeThrowsPerFgAttempt(final double freeThrowsPerFgAttempt) {
        this.freeThrowsPerFgAttempt = freeThrowsPerFgAttempt;
        return this;
    }

    public double getOpponentPace() {
        return opponentPace;
    }

    public NcaaBbSeasonData setOpponentPace(final double opponentPace) {
        this.opponentPace = opponentPace;
        return this;
    }

    public double getOpponentOffensiveRating() {
        return opponentOffensiveRating;
    }

    public NcaaBbSeasonData setOpponentOffensiveRating(final double opponentOffensiveRating) {
        this.opponentOffensiveRating = opponentOffensiveRating;
        return this;
    }

    public double getOpponentFreeThrowAttemptRate() {
        return opponentFreeThrowAttemptRate;
    }

    public NcaaBbSeasonData setOpponentFreeThrowAttemptRate(final double opponentFreeThrowAttemptRate) {
        this.opponentFreeThrowAttemptRate = opponentFreeThrowAttemptRate;
        return this;
    }

    public double getOpponentThreePointAttemptRate() {
        return opponentThreePointAttemptRate;
    }

    public NcaaBbSeasonData setOpponentThreePointAttemptRate(final double opponentThreePointAttemptRate) {
        this.opponentThreePointAttemptRate = opponentThreePointAttemptRate;
        return this;
    }

    public double getOpponentTrueShootingPct() {
        return opponentTrueShootingPct;
    }

    public NcaaBbSeasonData setOpponentTrueShootingPct(final double opponentTrueShootingPct) {
        this.opponentTrueShootingPct = opponentTrueShootingPct;
        return this;
    }

    public double getOpponentTotalReboundPct() {
        return opponentTotalReboundPct;
    }

    public NcaaBbSeasonData setOpponentTotalReboundPct(final double opponentTotalReboundPct) {
        this.opponentTotalReboundPct = opponentTotalReboundPct;
        return this;
    }

    public double getOpponentAssistPct() {
        return opponentAssistPct;
    }

    public NcaaBbSeasonData setOpponentAssistPct(final double opponentAssistPct) {
        this.opponentAssistPct = opponentAssistPct;
        return this;
    }

    public double getOpponentStealPct() {
        return opponentStealPct;
    }

    public NcaaBbSeasonData setOpponentStealPct(final double opponentStealPct) {
        this.opponentStealPct = opponentStealPct;
        return this;
    }

    public double getOpponentBlockPct() {
        return opponentBlockPct;
    }

    public NcaaBbSeasonData setOpponentBlockPct(final double opponentBlockPct) {
        this.opponentBlockPct = opponentBlockPct;
        return this;
    }

    public double getOpponentEffectiveFgPct() {
        return opponentEffectiveFgPct;
    }

    public NcaaBbSeasonData setOpponentEffectiveFgPct(final double opponentEffectiveFgPct) {
        this.opponentEffectiveFgPct = opponentEffectiveFgPct;
        return this;
    }

    public double getOpponentTurnoverPct() {
        return opponentTurnoverPct;
    }

    public NcaaBbSeasonData setOpponentTurnoverPct(final double opponentTurnoverPct) {
        this.opponentTurnoverPct = opponentTurnoverPct;
        return this;
    }

    public double getOpponentOffensiveReboundPct() {
        return opponentOffensiveReboundPct;
    }

    public NcaaBbSeasonData setOpponentOffensiveReboundPct(final double opponentOffensiveReboundPct) {
        this.opponentOffensiveReboundPct = opponentOffensiveReboundPct;
        return this;
    }

    public double getOpponentFreeThrowsPerFgAttempt() {
        return opponentFreeThrowsPerFgAttempt;
    }

    public NcaaBbSeasonData setOpponentFreeThrowsPerFgAttempt(final double opponentFreeThrowsPerFgAttempt) {
        this.opponentFreeThrowsPerFgAttempt = opponentFreeThrowsPerFgAttempt;
        return this;
    }
}
