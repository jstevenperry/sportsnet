package com.jstevenperry.sportsnet.ncaabb;

import com.jstevenperry.sportsnet.framework.AbstractNetworkRunner;
import com.jstevenperry.sportsnet.framework.AbstractTournamentRunnerPlugin;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.ncaabb.model.NcaaBbSeasonData;
import com.jstevenperry.sportsnet.ncaabb.plugins.TournamentRunnerPlugin2013;
import com.jstevenperry.sportsnet.ncaabb.plugins.TournamentRunnerPlugin2016;
import com.jstevenperry.sportsnet.ncaabb.plugins.TournamentRunnerPlugin2017;
import com.jstevenperry.sportsnet.ncaabb.plugins.TournamentRunnerPlugin2018;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * NCAA Basketball Network Runner.
 */
@Component
public class NcaaBbNetworkRunner extends AbstractNetworkRunner<NcaaBbSeasonData> {

    @Override
    protected List<AbstractTournamentRunnerPlugin<NcaaBbSeasonData>> loadPlugins(final SeasonDataDao<NcaaBbSeasonData> ncaaBbSeasonDataDao) {
        List<AbstractTournamentRunnerPlugin<NcaaBbSeasonData>> ret = new ArrayList<>();
        ret.add(new TournamentRunnerPlugin2013(ncaaBbSeasonDataDao));
        ret.add(new TournamentRunnerPlugin2016(ncaaBbSeasonDataDao));
        ret.add(new TournamentRunnerPlugin2017(ncaaBbSeasonDataDao));
        ret.add(new TournamentRunnerPlugin2018(ncaaBbSeasonDataDao));

        return ret;
    }

    @Override
    public List<Double> seasonDataAsDoubleList(final NcaaBbSeasonData seasonData, final Double resultClass, final boolean isTraining) {
        return NcaaBbSeasonData.asDoubleList(seasonData, resultClass, isTraining);
    }
}
