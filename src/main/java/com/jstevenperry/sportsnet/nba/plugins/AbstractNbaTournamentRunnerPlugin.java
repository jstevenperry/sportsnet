package com.jstevenperry.sportsnet.nba.plugins;

import com.jstevenperry.sportsnet.framework.AbstractTournamentRunnerPlugin;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.nba.menus.NbaNetworkMenu;
import com.jstevenperry.sportsnet.nba.model.NbaSeasonData;

import java.util.List;

public abstract class AbstractNbaTournamentRunnerPlugin extends AbstractTournamentRunnerPlugin<NbaSeasonData> {
    public AbstractNbaTournamentRunnerPlugin(final SeasonDataDao<NbaSeasonData> seasonDataDao) {
        super(seasonDataDao);
    }

    @Override
    public List<Double> seasonDataAsDoubleList(final NbaSeasonData seasonData, final Double resultClass, final boolean isTraining) {
        return NbaSeasonData.seasonDataAsDoubleList(seasonData, resultClass, isTraining);
    }

    @Override
    public Integer getTournamentYear() {
        return 2019;
    }

    @Override
    protected String getTournamentResultsTemplateFileName() {
        return "/nba/playoff-results-template.csv";
    }

    @Override
    protected String getNetworkType() {
        return NbaNetworkMenu.NETWORK_TYPE;
    }

    @Override
    public String getTournamentTemplateFileName() {
        return "/nba/playoff-matchups-template-nba.txt";
    }
}
