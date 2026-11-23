/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.mlb.service.plugins;

import com.jstevenperry.sportsnet.framework.model.NetworkRunnerPluginOutputType;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.mlb.model.MlbSeasonData;

public class MlbTournamentPlugin2023 extends AbstractMlbTournamentRunnerPlugin {
    public MlbTournamentPlugin2023(final SeasonDataDao<MlbSeasonData> seasonDataDao) {
        super(seasonDataDao);
    }

    @Override
    public Integer getTournamentYear() {
        return 2023;
    }

    @Override
    public String getTournamentYearForDisplay() {
        return String.format("%s (%s)", "2023", getOutputType());
    }

    @Override
    public NetworkRunnerPluginOutputType getOutputType() {
        return NetworkRunnerPluginOutputType.TEAM_RANKING;
    }
}
