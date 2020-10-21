package com.jstevenperry.sportsnet.framework.model;

import java.math.BigDecimal;

/**
 * Base class for all SeasonData objects, for all sports.
 */
public abstract class AbstractSeasonData {
  private Integer year;
  private String teamName;

  //
  private BigDecimal wonLostPercentage;

  public Integer getYear() {
    return year;
  }

  public AbstractSeasonData setYear(final Integer year) {
    this.year = year;
    return this;
  }

  public String getTeamName() {
    return teamName;
  }

  public AbstractSeasonData setTeamName(final String teamName) {
    this.teamName = teamName;
    return this;
  }

  public BigDecimal getWonLostPercentage() {
    return wonLostPercentage;
  }

  public AbstractSeasonData setWonLostPercentage(final BigDecimal wonLostPercentage) {
    this.wonLostPercentage = wonLostPercentage;
    return this;
  }
}
