package com.jstevenperry.sportsnet.ncaabb.service.plugins;

import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.framework.service.AbstractNetworkRunnerPlugin;
import com.jstevenperry.sportsnet.ncaabb.config.NcaaBbNetworkUtils;
import com.jstevenperry.sportsnet.ncaabb.model.NcaaBbSeasonData;

public abstract class NcaaBbTournamentRunnerPlugin extends AbstractNetworkRunnerPlugin<NcaaBbSeasonData> {
    public NcaaBbTournamentRunnerPlugin(final SeasonDataDao<NcaaBbSeasonData> seasonDataDao) {
        super(seasonDataDao);
    }

    @Override
    public String getTournamentResultsTemplateFileName() {
        return "/ncaabb/tournament-results-template.csv";
    }

    @Override
    public String getNetworkType() {
        return NcaaBbNetworkUtils.NETWORK_TYPE;
    }

    @Override
    public String getTournamentTemplateFileName() {
        return "/ncaabb/tournament-template-ncaabb.txt";
    }

}
