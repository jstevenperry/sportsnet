/*
 * Copyright 2017 Makoto Consulting Group, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jstevenperry.sportsnet.nhl.model;

import com.jstevenperry.sportsnet.framework.model.AbstractSeasonData;

public class NhlSeasonData extends AbstractSeasonData {

  private int rank;
  private double averageAge;
  private int gamesPlayed;
  private int wins;
  private int losses;
  private int points;
  private double pointsPct;
  private int goalsFor;
  private int goalsAgainst;
  private int shootOutWins;
  private int shootOutLosses;
  private double simpleRatingSystem;
  private double strengthOfSchedule;
  private double goalsPerGame;
  private int evenStrengthGoals;
  private int evenStrengthGoalsAgainst;
  private int powerPlayGoals;
  private int powerPlayOpportunities;
  private double powerPlayPct;
  private int powerPlayGoalsAgainst;
  private int getPowerPlayOpportunitiesAgainst;
  private double penaltyKillPct;
  private int shortHandedGoals;
  private int getShortHandedGoalsAgainst;
  private double penaltyMinutesPerGame;
  private double opponentPenaltyMinutesPerGame;
  private int shotsOnGoal;
  private double shootingPct;
  private int shotsAgainst;
  private double savePct;
  private int shutouts;

  public int getRank() {
    return rank;
  }

  public NhlSeasonData setRank(final int rank) {
    this.rank = rank;
    return this;
  }

  public double getAverageAge() {
    return averageAge;
  }

  public NhlSeasonData setAverageAge(final double averageAge) {
    this.averageAge = averageAge;
    return this;
  }

  public int getGamesPlayed() {
    return gamesPlayed;
  }

  public NhlSeasonData setGamesPlayed(final int gamesPlayed) {
    this.gamesPlayed = gamesPlayed;
    return this;
  }

  public int getWins() {
    return wins;
  }

  public NhlSeasonData setWins(final int wins) {
    this.wins = wins;
    return this;
  }

  public int getLosses() {
    return losses;
  }

  public NhlSeasonData setLosses(final int losses) {
    this.losses = losses;
    return this;
  }

  public int getPoints() {
    return points;
  }

  public NhlSeasonData setPoints(final int points) {
    this.points = points;
    return this;
  }

  public double getPointsPct() {
    return pointsPct;
  }

  public NhlSeasonData setPointsPct(final double pointsPct) {
    this.pointsPct = pointsPct;
    return this;
  }

  public int getGoalsFor() {
    return goalsFor;
  }

  public NhlSeasonData setGoalsFor(final int goalsFor) {
    this.goalsFor = goalsFor;
    return this;
  }

  public int getGoalsAgainst() {
    return goalsAgainst;
  }

  public NhlSeasonData setGoalsAgainst(final int goalsAgainst) {
    this.goalsAgainst = goalsAgainst;
    return this;
  }

  public int getShootOutWins() {
    return shootOutWins;
  }

  public NhlSeasonData setShootOutWins(final int shootOutWins) {
    this.shootOutWins = shootOutWins;
    return this;
  }

  public int getShootOutLosses() {
    return shootOutLosses;
  }

  public NhlSeasonData setShootOutLosses(final int shootOutLosses) {
    this.shootOutLosses = shootOutLosses;
    return this;
  }

  public double getSimpleRatingSystem() {
    return simpleRatingSystem;
  }

  public NhlSeasonData setSimpleRatingSystem(final double simpleRatingSystem) {
    this.simpleRatingSystem = simpleRatingSystem;
    return this;
  }

  public double getStrengthOfSchedule() {
    return strengthOfSchedule;
  }

  public NhlSeasonData setStrengthOfSchedule(final double strengthOfSchedule) {
    this.strengthOfSchedule = strengthOfSchedule;
    return this;
  }

  public double getGoalsPerGame() {
    return goalsPerGame;
  }

  public NhlSeasonData setGoalsPerGame(final double goalsPerGame) {
    this.goalsPerGame = goalsPerGame;
    return this;
  }

  public int getEvenStrengthGoals() {
    return evenStrengthGoals;
  }

  public NhlSeasonData setEvenStrengthGoals(final int evenStrengthGoals) {
    this.evenStrengthGoals = evenStrengthGoals;
    return this;
  }

  public int getEvenStrengthGoalsAgainst() {
    return evenStrengthGoalsAgainst;
  }

  public NhlSeasonData setEvenStrengthGoalsAgainst(final int evenStrengthGoalsAgainst) {
    this.evenStrengthGoalsAgainst = evenStrengthGoalsAgainst;
    return this;
  }

  public int getPowerPlayGoals() {
    return powerPlayGoals;
  }

  public NhlSeasonData setPowerPlayGoals(final int powerPlayGoals) {
    this.powerPlayGoals = powerPlayGoals;
    return this;
  }

  public int getPowerPlayOpportunities() {
    return powerPlayOpportunities;
  }

  public NhlSeasonData setPowerPlayOpportunities(final int powerPlayOpportunities) {
    this.powerPlayOpportunities = powerPlayOpportunities;
    return this;
  }

  public double getPowerPlayPct() {
    return powerPlayPct;
  }

  public NhlSeasonData setPowerPlayPct(final double powerPlayPct) {
    this.powerPlayPct = powerPlayPct;
    return this;
  }

  public int getPowerPlayGoalsAgainst() {
    return powerPlayGoalsAgainst;
  }

  public NhlSeasonData setPowerPlayGoalsAgainst(final int powerPlayGoalsAgainst) {
    this.powerPlayGoalsAgainst = powerPlayGoalsAgainst;
    return this;
  }

  public int getGetPowerPlayOpportunitiesAgainst() {
    return getPowerPlayOpportunitiesAgainst;
  }

  public NhlSeasonData setGetPowerPlayOpportunitiesAgainst(final int getPowerPlayOpportunitiesAgainst) {
    this.getPowerPlayOpportunitiesAgainst = getPowerPlayOpportunitiesAgainst;
    return this;
  }

  public double getPenaltyKillPct() {
    return penaltyKillPct;
  }

  public NhlSeasonData setPenaltyKillPct(final double penaltyKillPct) {
    this.penaltyKillPct = penaltyKillPct;
    return this;
  }

  public int getShortHandedGoals() {
    return shortHandedGoals;
  }

  public NhlSeasonData setShortHandedGoals(final int shortHandedGoals) {
    this.shortHandedGoals = shortHandedGoals;
    return this;
  }

  public int getGetShortHandedGoalsAgainst() {
    return getShortHandedGoalsAgainst;
  }

  public NhlSeasonData setGetShortHandedGoalsAgainst(final int getShortHandedGoalsAgainst) {
    this.getShortHandedGoalsAgainst = getShortHandedGoalsAgainst;
    return this;
  }

  public double getPenaltyMinutesPerGame() {
    return penaltyMinutesPerGame;
  }

  public NhlSeasonData setPenaltyMinutesPerGame(final double penaltyMinutesPerGame) {
    this.penaltyMinutesPerGame = penaltyMinutesPerGame;
    return this;
  }

  public double getOpponentPenaltyMinutesPerGame() {
    return opponentPenaltyMinutesPerGame;
  }

  public NhlSeasonData setOpponentPenaltyMinutesPerGame(final double opponentPenaltyMinutesPerGame) {
    this.opponentPenaltyMinutesPerGame = opponentPenaltyMinutesPerGame;
    return this;
  }

  public int getShotsOnGoal() {
    return shotsOnGoal;
  }

  public NhlSeasonData setShotsOnGoal(final int shotsOnGoal) {
    this.shotsOnGoal = shotsOnGoal;
    return this;
  }

  public double getShootingPct() {
    return shootingPct;
  }

  public NhlSeasonData setShootingPct(final double shootingPct) {
    this.shootingPct = shootingPct;
    return this;
  }

  public int getShotsAgainst() {
    return shotsAgainst;
  }

  public NhlSeasonData setShotsAgainst(final int shotsAgainst) {
    this.shotsAgainst = shotsAgainst;
    return this;
  }

  public double getSavePct() {
    return savePct;
  }

  public NhlSeasonData setSavePct(final double savePct) {
    this.savePct = savePct;
    return this;
  }

  public int getShutouts() {
    return shutouts;
  }

  public NhlSeasonData setShutouts(final int shutouts) {
    this.shutouts = shutouts;
    return this;
  }
}
