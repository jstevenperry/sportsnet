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
package com.jstevenperry.sportsnet.ncaabb.model;

import com.jstevenperry.sportsnet.framework.model.AbstractSeasonData;
import com.jstevenperry.sportsnet.framework.util.StatsUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represents the v_season_data view, which is used for pretty much
 * everything. Represents the total stats of a team over the course of the
 * regular season.
 * <p>
 * **Does not include any tournament games for any years.**
 * <p>
 * This is an important premise: that we can accurately predict tournament winners
 * based solely on their regular season performance.
 *
 * @author J Steven Perry
 */
public class NcaaBbSeasonData extends AbstractSeasonData {

  // Offense
  private BigDecimal avgPointsPerGame;
  private BigDecimal scoringMarginPerGame;
  private BigDecimal numFgAttemptsPerGame;
  private BigDecimal fgPercentage;
  private BigDecimal num3pPerGame;
  private BigDecimal num3pAttemptsPerGame;
  private BigDecimal t3pPercentage;
  private BigDecimal numFtAttemptsPerGame;
  private BigDecimal ftPercentage;
  private BigDecimal reboundMargin;
  private BigDecimal assistsPerGame;
  private BigDecimal atoRatio;
  // Defense
  private BigDecimal avgOpponentPointsPerGame;
  private BigDecimal numOppFgAttemptsPerGame;
  private BigDecimal oppFgPercentage;
  private BigDecimal numOpp3pAttemptsPerGame;
  private BigDecimal opp3pPercentage;
  private BigDecimal blocksPerGame;
  private BigDecimal stealsPerGame;
  private BigDecimal oppTurnoversPerGame;
  // Errors
  private BigDecimal turnoversPerGame;
  private BigDecimal foulsPerGame;
  private BigDecimal numDq;

  public static List<Double> asDoubleList(final NcaaBbSeasonData seasonData,
                                          final Double resultClass,
                                          final boolean isTraining) {
    List<Double> ret = new ArrayList<>();
    // Offense
    ret.add(seasonData.getAvgPointsPerGame().doubleValue());
    ret.add(seasonData.getScoringMarginPerGame().doubleValue());
    ret.add(seasonData.getNumFgAttemptsPerGame().doubleValue());
    ret.add(seasonData.getFgPercentage().doubleValue());
    ret.add(seasonData.getNum3pPerGame().doubleValue());
    ret.add(seasonData.getNum3pAttemptsPerGame().doubleValue());
    ret.add(seasonData.getT3pPercentage().doubleValue());
    ret.add(seasonData.getNumFtAttemptsPerGame().doubleValue());
    ret.add(seasonData.getFtPercentage().doubleValue());
    ret.add(seasonData.getReboundMargin().doubleValue());
    ret.add(seasonData.getAssistsPerGame().doubleValue());
    ret.add(seasonData.getAtoRatio().doubleValue());
    // Defense
    ret.add(seasonData.getAvgOpponentPointsPerGame().doubleValue());
    ret.add(seasonData.getNumOppFgAttemptsPerGame().doubleValue());
    ret.add(seasonData.getOppFgPercentage().doubleValue());
    ret.add(seasonData.getNumOpp3pAttemptsPerGame().doubleValue());
    ret.add(seasonData.getOpp3pPercentage().doubleValue());
    ret.add(seasonData.getBlocksPerGame().doubleValue());
    ret.add(seasonData.getStealsPerGame().doubleValue());
    ret.add(seasonData.getOppTurnoversPerGame().doubleValue());
    // Errors
    ret.add(seasonData.getTurnoversPerGame().doubleValue());
    ret.add(seasonData.getFoulsPerGame().doubleValue());
    ret.add(seasonData.getNumDq().doubleValue());
    //
    ret.add(seasonData.getWonLostPercentage().doubleValue());
    // Class - the number of tournament victories
    if (isTraining) {
      ret.add(resultClass);
    }
    return ret;
  }

  public NcaaBbSeasonData() {
    // Nothing to do
  }

  public BigDecimal getAvgPointsPerGame() {
    return avgPointsPerGame;
  }

  public void setAvgPointsPerGame(BigDecimal avgPointsPerGame) {
    this.avgPointsPerGame = avgPointsPerGame.setScale(StatsUtils.SCALE, RoundingMode.HALF_UP);
  }

  public BigDecimal getScoringMarginPerGame() {
    return scoringMarginPerGame;
  }

  public void setScoringMarginPerGame(BigDecimal scoringMarginPerGame) {
    this.scoringMarginPerGame = scoringMarginPerGame.setScale(StatsUtils.SCALE, RoundingMode.HALF_UP);
  }

  public BigDecimal getNumFgAttemptsPerGame() {
    return numFgAttemptsPerGame;
  }

  public void setNumFgAttemptsPerGame(BigDecimal numFgAttemptsPerGame) {
    this.numFgAttemptsPerGame = numFgAttemptsPerGame.setScale(StatsUtils.SCALE, RoundingMode.HALF_UP);
  }

  public BigDecimal getFgPercentage() {
    return fgPercentage;
  }

  public void setFgPercentage(BigDecimal fgPercentage) {
    this.fgPercentage = fgPercentage.setScale(StatsUtils.SCALE, RoundingMode.HALF_UP);
  }

  public BigDecimal getNum3pPerGame() {
    return num3pPerGame;
  }

  public void setNum3pPerGame(BigDecimal num3pPerGame) {
    this.num3pPerGame = num3pPerGame.setScale(StatsUtils.SCALE, RoundingMode.HALF_UP);
  }

  public BigDecimal getNum3pAttemptsPerGame() {
    return (num3pAttemptsPerGame == null) ? BigDecimal.ZERO : num3pAttemptsPerGame;
  }

  public void setNum3pAttemptsPerGame(BigDecimal num3pAttemptsPerGame) {
    if (num3pAttemptsPerGame != null)
      this.num3pAttemptsPerGame = num3pAttemptsPerGame.setScale(StatsUtils.SCALE, RoundingMode.HALF_UP);
  }

  public BigDecimal getT3pPercentage() {
    return (t3pPercentage == null) ? BigDecimal.ZERO : t3pPercentage;
  }

  public void setT3pPercentage(BigDecimal t3pPercentage) {
    if (t3pPercentage != null)
      this.t3pPercentage = t3pPercentage.setScale(StatsUtils.SCALE, RoundingMode.HALF_UP);
  }

  public BigDecimal getNumFtAttemptsPerGame() {
    return numFtAttemptsPerGame;
  }

  public void setNumFtAttemptsPerGame(BigDecimal numFtAttemptsPerGame) {
    this.numFtAttemptsPerGame = numFtAttemptsPerGame.setScale(StatsUtils.SCALE, RoundingMode.HALF_UP);
  }

  public BigDecimal getFtPercentage() {
    return ftPercentage;
  }

  public void setFtPercentage(BigDecimal ftPercentage) {
    this.ftPercentage = ftPercentage.setScale(StatsUtils.SCALE, RoundingMode.HALF_UP);
  }

  public BigDecimal getReboundMargin() {
    return reboundMargin;
  }

  public void setReboundMargin(BigDecimal reboundMargin) {
    this.reboundMargin = reboundMargin.setScale(StatsUtils.SCALE, RoundingMode.HALF_UP);
  }

  public BigDecimal getAssistsPerGame() {
    return assistsPerGame;
  }

  public void setAssistsPerGame(BigDecimal assistsPerGame) {
    this.assistsPerGame = assistsPerGame.setScale(StatsUtils.SCALE, RoundingMode.HALF_UP);
  }

  public BigDecimal getAtoRatio() {
    return atoRatio;
  }

  public void setAtoRatio(BigDecimal atoRatio) {
    this.atoRatio = atoRatio.setScale(StatsUtils.SCALE, RoundingMode.HALF_UP);
  }

  public BigDecimal getAvgOpponentPointsPerGame() {
    return avgOpponentPointsPerGame;
  }

  public void setAvgOpponentPointsPerGame(BigDecimal avgOpponentPointsPerGame) {
    this.avgOpponentPointsPerGame = avgOpponentPointsPerGame.setScale(StatsUtils.SCALE, RoundingMode.HALF_UP);
  }

  public BigDecimal getNumOppFgAttemptsPerGame() {
    return numOppFgAttemptsPerGame;
  }

  public void setNumOppFgAttemptsPerGame(BigDecimal numOppFgAttemptsPerGame) {
    this.numOppFgAttemptsPerGame = numOppFgAttemptsPerGame.setScale(StatsUtils.SCALE, RoundingMode.HALF_UP);
  }

  public BigDecimal getOppFgPercentage() {
    return oppFgPercentage;
  }

  public void setOppFgPercentage(BigDecimal oppFgPercentage) {
    this.oppFgPercentage = oppFgPercentage.setScale(StatsUtils.SCALE, RoundingMode.HALF_UP);
  }

  public BigDecimal getNumOpp3pAttemptsPerGame() {
    return numOpp3pAttemptsPerGame;
  }

  public void setNumOpp3pAttemptsPerGame(BigDecimal numOpp3pAttemptsPerGame) {
    this.numOpp3pAttemptsPerGame = numOpp3pAttemptsPerGame.setScale(StatsUtils.SCALE, RoundingMode.HALF_UP);
  }

  public BigDecimal getOpp3pPercentage() {
    return opp3pPercentage;
  }

  public void setOpp3pPercentage(BigDecimal opp3pPercentage) {
    this.opp3pPercentage = opp3pPercentage.setScale(StatsUtils.SCALE, RoundingMode.HALF_UP);
  }

  public BigDecimal getBlocksPerGame() {
    return blocksPerGame;
  }

  public void setBlocksPerGame(BigDecimal blocksPerGame) {
    this.blocksPerGame = blocksPerGame.setScale(StatsUtils.SCALE, RoundingMode.HALF_UP);
  }

  public BigDecimal getStealsPerGame() {
    return stealsPerGame;
  }

  public void setStealsPerGame(BigDecimal stealsPerGame) {
    this.stealsPerGame = stealsPerGame.setScale(StatsUtils.SCALE, RoundingMode.HALF_UP);
  }

  public BigDecimal getOppTurnoversPerGame() {
    return (oppTurnoversPerGame == null) ? BigDecimal.ZERO : oppTurnoversPerGame;
  }

  public void setOppTurnoversPerGame(BigDecimal oppTurnoversPerGame) {
    if (oppTurnoversPerGame != null)
      this.oppTurnoversPerGame = oppTurnoversPerGame.setScale(StatsUtils.SCALE, RoundingMode.HALF_UP);
  }

  public BigDecimal getTurnoversPerGame() {
    return turnoversPerGame;
  }

  public void setTurnoversPerGame(BigDecimal turnoversPerGame) {
    this.turnoversPerGame = turnoversPerGame.setScale(StatsUtils.SCALE, RoundingMode.HALF_UP);
  }

  public BigDecimal getFoulsPerGame() {
    return foulsPerGame;
  }

  public void setFoulsPerGame(BigDecimal foulsPerGame) {
    this.foulsPerGame = foulsPerGame.setScale(StatsUtils.SCALE, RoundingMode.HALF_UP);
  }

  public BigDecimal getNumDq() {
    return numDq;
  }

  public void setNumDq(BigDecimal numDq) {
    this.numDq = numDq.setScale(StatsUtils.SCALE, RoundingMode.HALF_UP);
  }

}
