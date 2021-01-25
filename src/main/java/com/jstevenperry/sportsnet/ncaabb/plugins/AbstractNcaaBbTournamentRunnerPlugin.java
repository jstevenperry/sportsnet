package com.jstevenperry.sportsnet.ncaabb.plugins;

import com.jstevenperry.sportsnet.framework.AbstractTournamentRunnerPlugin;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.ncaabb.menus.NcaaBbNetworkMenu;
import com.jstevenperry.sportsnet.ncaabb.model.NcaaBbSeasonData;

public abstract class AbstractNcaaBbTournamentRunnerPlugin extends AbstractTournamentRunnerPlugin<NcaaBbSeasonData> {

    public static final String NCAABB_TOURNAMENT_RESULTS_CSV = "/ncaabb/tournament-results-template.csv";

    public AbstractNcaaBbTournamentRunnerPlugin(final SeasonDataDao<NcaaBbSeasonData> seasonDataDao) {
        super(seasonDataDao);
    }

    @Override
    protected String getTournamentResultsTemplateFileName() {
        return NCAABB_TOURNAMENT_RESULTS_CSV;
    }

    @Override
    public String getNetworkType() {
        return NcaaBbNetworkMenu.NETWORK_TYPE;
    }

}
