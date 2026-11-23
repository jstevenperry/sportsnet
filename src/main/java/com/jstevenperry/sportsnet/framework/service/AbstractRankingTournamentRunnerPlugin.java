/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.service;

import com.jstevenperry.sportsnet.framework.model.SeasonData;
import com.jstevenperry.sportsnet.framework.model.TeamCoordinate;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;

import java.util.Map;

/**
 * Tournament runner plugin to be used for ranking-based tournament, where a bracket
 * is not computed, rather the output of the simulation is a ranking of teams and
 * their distance scores. The team with the highest score is first, the team with the
 * next highest score is second, and so on. Brackets are a PITA to calculate, and
 * actually close to impossible for the NFL's post-wildcard round reseeding anyway.
 * @param <T> SeasonData
 */
public abstract class AbstractRankingTournamentRunnerPlugin<T extends SeasonData> extends AbstractNetworkRunnerPlugin<T> {
    public AbstractRankingTournamentRunnerPlugin(final SeasonDataDao<T> seasonDataDao) {
        super(seasonDataDao);
    }

    @Override
    public String getTournamentResultsTemplateFileName() {
        throw new UnsupportedOperationException("TODO: com.jstevenperry.sportsnet.framework.service.AbstractTournamentRunnerPlugin.getTournamentResultsTemplateFileName() needs code!");
    }

    @Override
    public String getTournamentTemplateFileName() {
        throw new UnsupportedOperationException("TODO: com.jstevenperry.sportsnet.framework.service.AbstractTournamentRunnerPlugin.getTournamentTemplateFileName() needs code!");
    }

    @Override
    public Map<TeamCoordinate, T> buildTeamCoordinateSeasonDataMap() {
        throw new UnsupportedOperationException("TODO: com.jstevenperry.sportsnet.framework.service.AbstractTournamentRunnerPlugin.buildTeamCoordinateSeasonDataMap() needs code!");
    }
}
