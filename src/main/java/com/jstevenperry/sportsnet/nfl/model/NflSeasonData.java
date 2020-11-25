package com.jstevenperry.sportsnet.nfl.model;

import com.jstevenperry.sportsnet.framework.model.AbstractSeasonData;

public class NflSeasonData extends AbstractSeasonData {
  public int getTeamOffenseRank() {
    return teamOffenseRank;
  }

  public NflSeasonData setTeamOffenseRank(final int teamOffenseRank) {
    this.teamOffenseRank = teamOffenseRank;
    return this;
  }

  public int getGamesPlayed() {
    return gamesPlayed;
  }

  public NflSeasonData setGamesPlayed(final int gamesPlayed) {
    this.gamesPlayed = gamesPlayed;
    return this;
  }

  public int getPointsFor() {
    return pointsFor;
  }

  public NflSeasonData setPointsFor(final int pointsFor) {
    this.pointsFor = pointsFor;
    return this;
  }

  public int getYards() {
    return yards;
  }

  public NflSeasonData setYards(final int yards) {
    this.yards = yards;
    return this;
  }

  public int getPlays() {
    return plays;
  }

  public NflSeasonData setPlays(final int plays) {
    this.plays = plays;
    return this;
  }

  public double getYardsPerPlay() {
    return yardsPerPlay;
  }

  public NflSeasonData setYardsPerPlay(final double yardsPerPlay) {
    this.yardsPerPlay = yardsPerPlay;
    return this;
  }

  public int getTurnovers() {
    return turnovers;
  }

  public NflSeasonData setTurnovers(final int turnovers) {
    this.turnovers = turnovers;
    return this;
  }

  public int getFumblesLost() {
    return fumblesLost;
  }

  public NflSeasonData setFumblesLost(final int fumblesLost) {
    this.fumblesLost = fumblesLost;
    return this;
  }

  public int getFirstDowns() {
    return firstDowns;
  }

  public NflSeasonData setFirstDowns(final int firstDowns) {
    this.firstDowns = firstDowns;
    return this;
  }

  public int getCompletedPasses() {
    return completedPasses;
  }

  public NflSeasonData setCompletedPasses(final int completedPasses) {
    this.completedPasses = completedPasses;
    return this;
  }

  public int getPassAttempts() {
    return passAttempts;
  }

  public NflSeasonData setPassAttempts(final int passAttempts) {
    this.passAttempts = passAttempts;
    return this;
  }

  public int getYdPassing() {
    return ydPassing;
  }

  public NflSeasonData setYdPassing(final int ydPassing) {
    this.ydPassing = ydPassing;
    return this;
  }

  public int getPassingTd() {
    return passingTd;
  }

  public NflSeasonData setPassingTd(final int passingTd) {
    this.passingTd = passingTd;
    return this;
  }

  public int getInterceptions() {
    return interceptions;
  }

  public NflSeasonData setInterceptions(final int interceptions) {
    this.interceptions = interceptions;
    return this;
  }

  public double getNetYdPerPassAtt() {
    return netYdPerPassAtt;
  }

  public NflSeasonData setNetYdPerPassAtt(final double netYdPerPassAtt) {
    this.netYdPerPassAtt = netYdPerPassAtt;
    return this;
  }

  public int getPassing1d() {
    return passing1d;
  }

  public NflSeasonData setPassing1d(final int passing1d) {
    this.passing1d = passing1d;
    return this;
  }

  public int getRushingAtt() {
    return rushingAtt;
  }

  public NflSeasonData setRushingAtt(final int rushingAtt) {
    this.rushingAtt = rushingAtt;
    return this;
  }

  public int getRushingYd() {
    return rushingYd;
  }

  public NflSeasonData setRushingYd(final int rushingYd) {
    this.rushingYd = rushingYd;
    return this;
  }

  public int getRushingTd() {
    return rushingTd;
  }

  public NflSeasonData setRushingTd(final int rushingTd) {
    this.rushingTd = rushingTd;
    return this;
  }

  public double getRushingYdPerAtt() {
    return rushingYdPerAtt;
  }

  public NflSeasonData setRushingYdPerAtt(final double rushingYdPerAtt) {
    this.rushingYdPerAtt = rushingYdPerAtt;
    return this;
  }

  public int getRushing1d() {
    return rushing1d;
  }

  public NflSeasonData setRushing1d(final int rushing1d) {
    this.rushing1d = rushing1d;
    return this;
  }

  public int getPenalties() {
    return penalties;
  }

  public NflSeasonData setPenalties(final int penalties) {
    this.penalties = penalties;
    return this;
  }

  public int getPenaltyYd() {
    return penaltyYd;
  }

  public NflSeasonData setPenaltyYd(final int penaltyYd) {
    this.penaltyYd = penaltyYd;
    return this;
  }

  public int getFirstDownsByPenalty() {
    return firstDownsByPenalty;
  }

  public NflSeasonData setFirstDownsByPenalty(final int firstDownsByPenalty) {
    this.firstDownsByPenalty = firstDownsByPenalty;
    return this;
  }

  public double getDriveScorePct() {
    return driveScorePct;
  }

  public NflSeasonData setDriveScorePct(final double driveScorePct) {
    this.driveScorePct = driveScorePct;
    return this;
  }

  public double getDriveOffensiveToPct() {
    return driveOffensiveToPct;
  }

  public NflSeasonData setDriveOffensiveToPct(final double driveOffensiveToPct) {
    this.driveOffensiveToPct = driveOffensiveToPct;
    return this;
  }

  public double getExpectedPtsByAllOffense() {
    return expectedPtsByAllOffense;
  }

  public NflSeasonData setExpectedPtsByAllOffense(final double expectedPtsByAllOffense) {
    this.expectedPtsByAllOffense = expectedPtsByAllOffense;
    return this;
  }

  public double getPassCompletionPct() {
    return passCompletionPct;
  }

  public NflSeasonData setPassCompletionPct(final double passCompletionPct) {
    this.passCompletionPct = passCompletionPct;
    return this;
  }

  public double getPassTdPct() {
    return passTdPct;
  }

  public NflSeasonData setPassTdPct(final double passTdPct) {
    this.passTdPct = passTdPct;
    return this;
  }

  public double getIntPct() {
    return intPct;
  }

  public NflSeasonData setIntPct(final double intPct) {
    this.intPct = intPct;
    return this;
  }

  public int getLongestPass() {
    return longestPass;
  }

  public NflSeasonData setLongestPass(final int longestPass) {
    this.longestPass = longestPass;
    return this;
  }

  public double getPassYdPerAtt() {
    return passYdPerAtt;
  }

  public NflSeasonData setPassYdPerAtt(final double passYdPerAtt) {
    this.passYdPerAtt = passYdPerAtt;
    return this;
  }

  public double getAdjYdPerAtt() {
    return adjYdPerAtt;
  }

  public NflSeasonData setAdjYdPerAtt(final double adjYdPerAtt) {
    this.adjYdPerAtt = adjYdPerAtt;
    return this;
  }

  public double getYdPerCompletion() {
    return ydPerCompletion;
  }

  public NflSeasonData setYdPerCompletion(final double ydPerCompletion) {
    this.ydPerCompletion = ydPerCompletion;
    return this;
  }

  public double getYardPerGame() {
    return yardPerGame;
  }

  public NflSeasonData setYardPerGame(final double yardPerGame) {
    this.yardPerGame = yardPerGame;
    return this;
  }

  public double getPassRating() {
    return passRating;
  }

  public NflSeasonData setPassRating(final double passRating) {
    this.passRating = passRating;
    return this;
  }

  public int getOffensiveSacks() {
    return offensiveSacks;
  }

  public NflSeasonData setOffensiveSacks(final int offensiveSacks) {
    this.offensiveSacks = offensiveSacks;
    return this;
  }

  public int getYdLostSacks() {
    return ydLostSacks;
  }

  public NflSeasonData setYdLostSacks(final int ydLostSacks) {
    this.ydLostSacks = ydLostSacks;
    return this;
  }

  public double getAdjNetYdPerPassAtt() {
    return adjNetYdPerPassAtt;
  }

  public NflSeasonData setAdjNetYdPerPassAtt(final double adjNetYdPerPassAtt) {
    this.adjNetYdPerPassAtt = adjNetYdPerPassAtt;
    return this;
  }

  public double getSackedWhilePassingPct() {
    return sackedWhilePassingPct;
  }

  public NflSeasonData setSackedWhilePassingPct(final double sackedWhilePassingPct) {
    this.sackedWhilePassingPct = sackedWhilePassingPct;
    return this;
  }

  public int getQbLedCombacks() {
    return qbLedCombacks;
  }

  public NflSeasonData setQbLedCombacks(final int qbLedCombacks) {
    this.qbLedCombacks = qbLedCombacks;
    return this;
  }

  public int getQbGameWinningDrives() {
    return qbGameWinningDrives;
  }

  public NflSeasonData setQbGameWinningDrives(final int qbGameWinningDrives) {
    this.qbGameWinningDrives = qbGameWinningDrives;
    return this;
  }

  public double getExpectedPtsPassingOffense() {
    return expectedPtsPassingOffense;
  }

  public NflSeasonData setExpectedPtsPassingOffense(final double expectedPtsPassingOffense) {
    this.expectedPtsPassingOffense = expectedPtsPassingOffense;
    return this;
  }

  public double getYdRushingPerGame() {
    return ydRushingPerGame;
  }

  public NflSeasonData setYdRushingPerGame(final double ydRushingPerGame) {
    this.ydRushingPerGame = ydRushingPerGame;
    return this;
  }

  public int getRushingFumbles() {
    return rushingFumbles;
  }

  public NflSeasonData setRushingFumbles(final int rushingFumbles) {
    this.rushingFumbles = rushingFumbles;
    return this;
  }

  public double getExpectedPtsRushingOffense() {
    return expectedPtsRushingOffense;
  }

  public NflSeasonData setExpectedPtsRushingOffense(final double expectedPtsRushingOffense) {
    this.expectedPtsRushingOffense = expectedPtsRushingOffense;
    return this;
  }

  public int getPuntReturns() {
    return puntReturns;
  }

  public NflSeasonData setPuntReturns(final int puntReturns) {
    this.puntReturns = puntReturns;
    return this;
  }

  public int getPuntReturnYd() {
    return puntReturnYd;
  }

  public NflSeasonData setPuntReturnYd(final int puntReturnYd) {
    this.puntReturnYd = puntReturnYd;
    return this;
  }

  public int getPuntReturnTd() {
    return puntReturnTd;
  }

  public NflSeasonData setPuntReturnTd(final int puntReturnTd) {
    this.puntReturnTd = puntReturnTd;
    return this;
  }

  public int getPuntReturnLongest() {
    return puntReturnLongest;
  }

  public NflSeasonData setPuntReturnLongest(final int puntReturnLongest) {
    this.puntReturnLongest = puntReturnLongest;
    return this;
  }

  public double getPuntReturnYdPerGame() {
    return puntReturnYdPerGame;
  }

  public NflSeasonData setPuntReturnYdPerGame(final double puntReturnYdPerGame) {
    this.puntReturnYdPerGame = puntReturnYdPerGame;
    return this;
  }

  public int getKickoffReturns() {
    return kickoffReturns;
  }

  public NflSeasonData setKickoffReturns(final int kickoffReturns) {
    this.kickoffReturns = kickoffReturns;
    return this;
  }

  public int getKickoffReturnYd() {
    return kickoffReturnYd;
  }

  public NflSeasonData setKickoffReturnYd(final int kickoffReturnYd) {
    this.kickoffReturnYd = kickoffReturnYd;
    return this;
  }

  public int getKickoffReturnTd() {
    return kickoffReturnTd;
  }

  public NflSeasonData setKickoffReturnTd(final int kickoffReturnTd) {
    this.kickoffReturnTd = kickoffReturnTd;
    return this;
  }

  public int getKickoffReturnLongest() {
    return kickoffReturnLongest;
  }

  public NflSeasonData setKickoffReturnLongest(final int kickoffReturnLongest) {
    this.kickoffReturnLongest = kickoffReturnLongest;
    return this;
  }

  public double getKickoffReturnYdPerGame() {
    return kickoffReturnYdPerGame;
  }

  public NflSeasonData setKickoffReturnYdPerGame(final double kickoffReturnYdPerGame) {
    this.kickoffReturnYdPerGame = kickoffReturnYdPerGame;
    return this;
  }

  public int getAllPurposeYd() {
    return allPurposeYd;
  }

  public NflSeasonData setAllPurposeYd(final int allPurposeYd) {
    this.allPurposeYd = allPurposeYd;
    return this;
  }

  public int getFgAtt() {
    return fgAtt;
  }

  public NflSeasonData setFgAtt(final int fgAtt) {
    this.fgAtt = fgAtt;
    return this;
  }

  public int getFgMade() {
    return fgMade;
  }

  public NflSeasonData setFgMade(final int fgMade) {
    this.fgMade = fgMade;
    return this;
  }

  public int getFgLongest() {
    return fgLongest;
  }

  public NflSeasonData setFgLongest(final int fgLongest) {
    this.fgLongest = fgLongest;
    return this;
  }

  public double getFgPct() {
    return fgPct;
  }

  public NflSeasonData setFgPct(final double fgPct) {
    this.fgPct = fgPct;
    return this;
  }

  public int getXpAtt() {
    return xpAtt;
  }

  public NflSeasonData setXpAtt(final int xpAtt) {
    this.xpAtt = xpAtt;
    return this;
  }

  public int getXpMade() {
    return xpMade;
  }

  public NflSeasonData setXpMade(final int xpMade) {
    this.xpMade = xpMade;
    return this;
  }

  public double getXpPct() {
    return xpPct;
  }

  public NflSeasonData setXpPct(final double xpPct) {
    this.xpPct = xpPct;
    return this;
  }

  public int getKickoffs() {
    return kickoffs;
  }

  public NflSeasonData setKickoffs(final int kickoffs) {
    this.kickoffs = kickoffs;
    return this;
  }

  public int getKickoffYd() {
    return kickoffYd;
  }

  public NflSeasonData setKickoffYd(final int kickoffYd) {
    this.kickoffYd = kickoffYd;
    return this;
  }

  public int getTouchbacks() {
    return touchbacks;
  }

  public NflSeasonData setTouchbacks(final int touchbacks) {
    this.touchbacks = touchbacks;
    return this;
  }

  public double getTouchbackPct() {
    return touchbackPct;
  }

  public NflSeasonData setTouchbackPct(final double touchbackPct) {
    this.touchbackPct = touchbackPct;
    return this;
  }

  public double getKickoffAvg() {
    return kickoffAvg;
  }

  public NflSeasonData setKickoffAvg(final double kickoffAvg) {
    this.kickoffAvg = kickoffAvg;
    return this;
  }

  public int getPunts() {
    return punts;
  }

  public NflSeasonData setPunts(final int punts) {
    this.punts = punts;
    return this;
  }

  public int getPuntYards() {
    return puntYards;
  }

  public NflSeasonData setPuntYards(final int puntYards) {
    this.puntYards = puntYards;
    return this;
  }

  public int getPuntLongest() {
    return puntLongest;
  }

  public NflSeasonData setPuntLongest(final int puntLongest) {
    this.puntLongest = puntLongest;
    return this;
  }

  public int getPuntsBlocked() {
    return puntsBlocked;
  }

  public NflSeasonData setPuntsBlocked(final int puntsBlocked) {
    this.puntsBlocked = puntsBlocked;
    return this;
  }

  public double getYdPerPunt() {
    return ydPerPunt;
  }

  public NflSeasonData setYdPerPunt(final double ydPerPunt) {
    this.ydPerPunt = ydPerPunt;
    return this;
  }

  public int getTwoPtConversionAtt() {
    return twoPtConversionAtt;
  }

  public NflSeasonData setTwoPtConversionAtt(final int twoPtConversionAtt) {
    this.twoPtConversionAtt = twoPtConversionAtt;
    return this;
  }

  public int getTwoPtConversionMade() {
    return twoPtConversionMade;
  }

  public NflSeasonData setTwoPtConversionMade(final int twoPtConversionMade) {
    this.twoPtConversionMade = twoPtConversionMade;
    return this;
  }

  public int getSafety() {
    return safety;
  }

  public NflSeasonData setSafety(final int safety) {
    this.safety = safety;
    return this;
  }

  public double getPtsPerGame() {
    return ptsPerGame;
  }

  public NflSeasonData setPtsPerGame(final double ptsPerGame) {
    this.ptsPerGame = ptsPerGame;
    return this;
  }

  public int getThirdDownConversionAtt() {
    return thirdDownConversionAtt;
  }

  public NflSeasonData setThirdDownConversionAtt(final int thirdDownConversionAtt) {
    this.thirdDownConversionAtt = thirdDownConversionAtt;
    return this;
  }

  public int getThirdDownConversionMade() {
    return thirdDownConversionMade;
  }

  public NflSeasonData setThirdDownConversionMade(final int thirdDownConversionMade) {
    this.thirdDownConversionMade = thirdDownConversionMade;
    return this;
  }

  public double getThirdDownConversionPct() {
    return thirdDownConversionPct;
  }

  public NflSeasonData setThirdDownConversionPct(final double thirdDownConversionPct) {
    this.thirdDownConversionPct = thirdDownConversionPct;
    return this;
  }

  public int getFourthDownConversionAtt() {
    return fourthDownConversionAtt;
  }

  public NflSeasonData setFourthDownConversionAtt(final int fourthDownConversionAtt) {
    this.fourthDownConversionAtt = fourthDownConversionAtt;
    return this;
  }

  public int getFourthDownConversionMade() {
    return fourthDownConversionMade;
  }

  public NflSeasonData setFourthDownConversionMade(final int fourthDownConversionMade) {
    this.fourthDownConversionMade = fourthDownConversionMade;
    return this;
  }

  public double getFourthDownConversionPct() {
    return fourthDownConversionPct;
  }

  public NflSeasonData setFourthDownConversionPct(final double fourthDownConversionPct) {
    this.fourthDownConversionPct = fourthDownConversionPct;
    return this;
  }

  public int getRedZoneAtt() {
    return redZoneAtt;
  }

  public NflSeasonData setRedZoneAtt(final int redZoneAtt) {
    this.redZoneAtt = redZoneAtt;
    return this;
  }

  public int getRedZoneTd() {
    return redZoneTd;
  }

  public NflSeasonData setRedZoneTd(final int redZoneTd) {
    this.redZoneTd = redZoneTd;
    return this;
  }

  public double getRedZonePct() {
    return redZonePct;
  }

  public NflSeasonData setRedZonePct(final double redZonePct) {
    this.redZonePct = redZonePct;
    return this;
  }

  public int getNumberOfDrives() {
    return numberOfDrives;
  }

  public NflSeasonData setNumberOfDrives(final int numberOfDrives) {
    this.numberOfDrives = numberOfDrives;
    return this;
  }

  public int getNumberOfPlays() {
    return numberOfPlays;
  }

  public NflSeasonData setNumberOfPlays(final int numberOfPlays) {
    this.numberOfPlays = numberOfPlays;
    return this;
  }

  public double getScoringPct() {
    return scoringPct;
  }

  public NflSeasonData setScoringPct(final double scoringPct) {
    this.scoringPct = scoringPct;
    return this;
  }

  public double getAvgNumberOfPlaysPerDrive() {
    return avgNumberOfPlaysPerDrive;
  }

  public NflSeasonData setAvgNumberOfPlaysPerDrive(final double avgNumberOfPlaysPerDrive) {
    this.avgNumberOfPlaysPerDrive = avgNumberOfPlaysPerDrive;
    return this;
  }

  public double getAvgTimeOfPosession() {
    return avgTimeOfPosession;
  }

  public NflSeasonData setAvgTimeOfPosession(final double avgTimeOfPosession) {
    this.avgTimeOfPosession = avgTimeOfPosession;
    return this;
  }

  public double getAvgPtsPerPosession() {
    return avgPtsPerPosession;
  }

  public NflSeasonData setAvgPtsPerPosession(final double avgPtsPerPosession) {
    this.avgPtsPerPosession = avgPtsPerPosession;
    return this;
  }

  public int getPassingOffenseRank() {
    return passingOffenseRank;
  }

  public NflSeasonData setPassingOffenseRank(final int passingOffenseRank) {
    this.passingOffenseRank = passingOffenseRank;
    return this;
  }

  public int getRushingOffenseRank() {
    return rushingOffenseRank;
  }

  public NflSeasonData setRushingOffenseRank(final int rushingOffenseRank) {
    this.rushingOffenseRank = rushingOffenseRank;
    return this;
  }

  public int getKickPuntReturnRank() {
    return kickPuntReturnRank;
  }

  public NflSeasonData setKickPuntReturnRank(final int kickPuntReturnRank) {
    this.kickPuntReturnRank = kickPuntReturnRank;
    return this;
  }

  public int getKickPuntRank() {
    return kickPuntRank;
  }

  public NflSeasonData setKickPuntRank(final int kickPuntRank) {
    this.kickPuntRank = kickPuntRank;
    return this;
  }

  public int getScoringOffenseRank() {
    return scoringOffenseRank;
  }

  public NflSeasonData setScoringOffenseRank(final int scoringOffenseRank) {
    this.scoringOffenseRank = scoringOffenseRank;
    return this;
  }

  public int getConversionsRank() {
    return conversionsRank;
  }

  public NflSeasonData setConversionsRank(final int conversionsRank) {
    this.conversionsRank = conversionsRank;
    return this;
  }

  public int getDriveAveragesRank() {
    return driveAveragesRank;
  }

  public NflSeasonData setDriveAveragesRank(final int driveAveragesRank) {
    this.driveAveragesRank = driveAveragesRank;
    return this;
  }

  public double getAvgYdPerDrive() {
    return avgYdPerDrive;
  }

  public NflSeasonData setAvgYdPerDrive(final double avgYdPerDrive) {
    this.avgYdPerDrive = avgYdPerDrive;
    return this;
  }

  // Team Offense
  int teamOffenseRank;
  int gamesPlayed;
  int pointsFor;
  int yards;
  int plays;
  double yardsPerPlay;
  int turnovers;
  int fumblesLost;
  int firstDowns;
  int completedPasses;
  int passAttempts;
  int ydPassing;
  int passingTd;
  int interceptions;
  double netYdPerPassAtt;
  int passing1d;
  int rushingAtt;
  int rushingYd;
  int rushingTd;
  double rushingYdPerAtt;
  int rushing1d;
  int penalties;
  int penaltyYd;
  int firstDownsByPenalty;
  double driveScorePct;
  double driveOffensiveToPct;
  double expectedPtsByAllOffense;

  // Passing Offense
  int passingOffenseRank;
  double passCompletionPct;
  double passTdPct;
  double intPct;
  int longestPass;
  double passYdPerAtt;
  double adjYdPerAtt;
  double ydPerCompletion;
  double yardPerGame;
  double passRating;
  int offensiveSacks;
  int ydLostSacks;
  double adjNetYdPerPassAtt;
  double sackedWhilePassingPct;
  int qbLedCombacks;
  int qbGameWinningDrives;
  double expectedPtsPassingOffense;

  // Rushing Offense
  int rushingOffenseRank;
  double ydRushingPerGame;
  int rushingFumbles;
  double expectedPtsRushingOffense;

  // Kick and Punt Returns
  int kickPuntReturnRank;
  int puntReturns;
  int puntReturnYd;
  int puntReturnTd;
  int puntReturnLongest;
  double puntReturnYdPerGame;
  int kickoffReturns;
  int kickoffReturnYd;
  int kickoffReturnTd;
  int kickoffReturnLongest;
  double kickoffReturnYdPerGame;
  int allPurposeYd;

  // Kicking and Punting
  int kickPuntRank;
  int fgAtt;
  int fgMade;
  int fgLongest;
  double fgPct;
  int xpAtt;
  int xpMade;
  double xpPct;
  int kickoffs;
  int kickoffYd;
  int touchbacks;
  double touchbackPct;
  double kickoffAvg;
  int punts;
  int puntYards;
  int puntLongest;
  int puntsBlocked;
  double ydPerPunt;

  // Scoring Offense
  int scoringOffenseRank;
  int twoPtConversionAtt;
  int twoPtConversionMade;
  int safety;
  double ptsPerGame;

  // Conversions
  int conversionsRank;
  int thirdDownConversionAtt;
  int thirdDownConversionMade;
  double thirdDownConversionPct;
  int fourthDownConversionAtt;
  int fourthDownConversionMade;
  double fourthDownConversionPct;
  int redZoneAtt;
  int redZoneTd;
  double redZonePct;

  // Drive Averages
  int driveAveragesRank;
  int numberOfDrives;
  int numberOfPlays;
  double scoringPct;
  double avgNumberOfPlaysPerDrive;
  double avgYdPerDrive;
  double avgTimeOfPosession;
  double avgPtsPerPosession;
}
