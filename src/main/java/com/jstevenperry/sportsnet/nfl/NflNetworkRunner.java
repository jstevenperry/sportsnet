package com.jstevenperry.sportsnet.nfl;

import com.jstevenperry.sportsnet.framework.AbstractNetworkRunner;
import com.jstevenperry.sportsnet.framework.AbstractTournamentRunnerPlugin;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.nfl.model.NflSeasonData;
import com.jstevenperry.sportsnet.nfl.plugins.NflTournamentPlugin2018;
import com.jstevenperry.sportsnet.nfl.plugins.NflTournamentPlugin2019;
import com.jstevenperry.sportsnet.nfl.plugins.NflTournamentPlugin2020;

import java.util.ArrayList;
import java.util.List;

public class NflNetworkRunner extends AbstractNetworkRunner<NflSeasonData> {
    @Override
    protected List<AbstractTournamentRunnerPlugin<NflSeasonData>> loadPlugins(final SeasonDataDao<NflSeasonData> seasonDataDao) {
        List<AbstractTournamentRunnerPlugin<NflSeasonData>> ret = new ArrayList<>();
        //
        // Add plugins here
        ret.add(new NflTournamentPlugin2018(seasonDataDao));
        ret.add(new NflTournamentPlugin2019(seasonDataDao));
        ret.add(new NflTournamentPlugin2020(seasonDataDao));
        return ret;
    }

    @Override
    public List<Double> seasonDataAsDoubleList(final NflSeasonData seasonData, final Double resultClass, final boolean isTraining) {
        return NflSeasonData.asDoubleList(seasonData, resultClass, isTraining);
    }
}
