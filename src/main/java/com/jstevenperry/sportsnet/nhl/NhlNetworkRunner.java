package com.jstevenperry.sportsnet.nhl;

import com.jstevenperry.sportsnet.framework.AbstractNetworkRunner;
import com.jstevenperry.sportsnet.framework.AbstractTournamentRunnerPlugin;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.nhl.model.NhlSeasonData;
import com.jstevenperry.sportsnet.nhl.plugins.TournamentRunnerPlugin2019;
import com.jstevenperry.sportsnet.nhl.plugins.TournamentRunnerPlugin2020;

import java.util.ArrayList;
import java.util.List;

public class NhlNetworkRunner extends AbstractNetworkRunner<NhlSeasonData> {
    @Override
    protected List<AbstractTournamentRunnerPlugin<NhlSeasonData>> loadPlugins(final SeasonDataDao<NhlSeasonData> seasonDataDao) {
        List<AbstractTournamentRunnerPlugin<NhlSeasonData>> ret = new ArrayList<>();
        //
        // Add plugins here
        ret.add(new TournamentRunnerPlugin2019(seasonDataDao));
        ret.add(new TournamentRunnerPlugin2020(seasonDataDao));

        return ret;
    }

    @Override
    public List<Double> seasonDataAsDoubleList(final NhlSeasonData seasonData, final Double resultClass, final boolean isTraining) {
        return NhlSeasonData.asDoubleList(seasonData, resultClass, isTraining);
    }
}
