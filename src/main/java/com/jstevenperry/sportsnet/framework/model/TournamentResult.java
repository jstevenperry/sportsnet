/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */
package com.jstevenperry.sportsnet.framework.model;

import java.util.Date;

/**
 * This class represents the result of a single historical tournament game
 * played between two teams. In basketball there is always a winner and a loser.
 * <p>
 * The interpretations of the fields should be pretty obvious based on their
 * names. Or maybe not.
 *
 * @author J Steven Perry
 * <p>
 * Model object for the PostgreSQL table that has the following structure:
 * <p>
 * <p>
 * Table "public.tournament_results"
 * Column | Type | Modifiers
 * -------------------+-----------------------+-----------------------------------------------------------------
 * id | integer | not null default nextval('tournament_results_id_seq'::regclass)
 * year | integer | not null
 * game_date | date | not null
 * winning_team_name | character varying(64) | not null
 * winning_score | integer | not null
 * losing_team_name | character varying(64) | not null
 * losing_score | integer | not null
 * Indexes:
 * "tournament_results_pkey" PRIMARY KEY, btree (id)
 */
public class TournamentResult implements Comparable<TournamentResult> {

    private Integer id;
    private Integer year;
    private Date gameDate;
    private String winningTeamName;
    private Integer winningScore;
    private String losingTeamName;
    private Integer losingScore;

    public TournamentResult() {
        // Nothing to do
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Date getGameDate() {
        return gameDate;
    }

    public void setGameDate(Date gameDate) {
        this.gameDate = gameDate;
    }

    public String getWinningTeamName() {
        return winningTeamName;
    }

    public void setWinningTeamName(String winningTeamName) {
        this.winningTeamName = winningTeamName;
    }

    public Integer getWinningScore() {
        return winningScore;
    }

    public void setWinningScore(Integer winningScore) {
        this.winningScore = winningScore;
    }

    public String getLosingTeamName() {
        return losingTeamName;
    }

    public void setLosingTeamName(String losingTeamName) {
        this.losingTeamName = losingTeamName;
    }

    public Integer getLosingScore() {
        return losingScore;
    }

    public void setLosingScore(Integer losingScore) {
        this.losingScore = losingScore;
    }

    @Override
    public int compareTo(TournamentResult o) {
        int ret = o.getGameDate().compareTo(getGameDate());
        if (ret == 0) {
            ret = o.getWinningTeamName().compareTo(getWinningTeamName());
        }
        if (ret == 0) {
            ret = o.getLosingTeamName().compareTo(getLosingTeamName());
        }
        return ret;
    }

}
