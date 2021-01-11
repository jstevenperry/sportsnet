package com.jstevenperry.sportsnet.nba;

import com.jstevenperry.sportsnet.framework.AbstractNetworkRunner;
import com.jstevenperry.sportsnet.framework.AbstractTournamentRunnerPlugin;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.nba.model.NbaSeasonData;
import com.jstevenperry.sportsnet.nba.plugins.NbaPlayoffPlugin2019;

import java.util.ArrayList;
import java.util.List;

public class NbaNetworkRunner extends AbstractNetworkRunner<NbaSeasonData> {
    @Override
    public List<Double> seasonDataAsDoubleList(final NbaSeasonData seasonData, final Double resultClass, final boolean isTraining) {
        return NbaSeasonData.seasonDataAsDoubleList(seasonData, resultClass, isTraining);
    }

    @Override
    protected List<AbstractTournamentRunnerPlugin<NbaSeasonData>> loadPlugins(final SeasonDataDao<NbaSeasonData> seasonDataDao) {
        List<AbstractTournamentRunnerPlugin<NbaSeasonData>> ret = new ArrayList<>();
        //
        // Add plugins here
        ret.add(new NbaPlayoffPlugin2019(seasonDataDao));

        return ret;
    }
}
