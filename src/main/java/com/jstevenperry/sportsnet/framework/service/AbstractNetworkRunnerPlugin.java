/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.service;

import com.jstevenperry.sportsnet.framework.model.SeasonData;
import com.jstevenperry.sportsnet.framework.model.TeamCoordinate;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;

import java.util.Map;

public abstract class AbstractNetworkRunnerPlugin<T extends SeasonData> implements NetworkRunnerPlugin<T> {

    private final SeasonDataDao<T> seasonDataDao;

    public AbstractNetworkRunnerPlugin(final SeasonDataDao<T> seasonDataDao) {
        this.seasonDataDao = seasonDataDao;
    }

    /**
     * Fetch and return season data for the team specified in the TeamCoordinate object
     * that is passed to this method.
     */
    public T fetchSeasonData(final TeamCoordinate teamCoordinate) {
        return seasonDataDao.fetchByYearAndTeamName(getTournamentYear(), teamCoordinate.getName());
    }

    protected void createTeamCoordinateMapping(final Map<TeamCoordinate, T> coordinateSeasonDataMap,
                                               final int region,
                                               final int round,
                                               final int index,
                                               final String teamName) {
        TeamCoordinate teamCoordinate = new TeamCoordinate()
                .setName(teamName)
                .setIndex(index)
                .setRegion(region)
                .setRound(round);
        T seasonData = fetchSeasonData(teamCoordinate);
        coordinateSeasonDataMap.put(teamCoordinate, seasonData);
    }

    /**
     * Return the name of the tournament template file.
     * <p>
     * The tournament template file is used to setup the games that will
     * be played in the tournament. Each game has a set of coordinates that
     * correspond to the region, round, and teams that participate in the
     * game. The region and round are the same for any given game from year
     * to year, but the teams that participate will be different and are
     * specified by the subclass that is for the tournament year that plugin
     * was written for.
     */
    public abstract String getTournamentTemplateFileName();

}
