/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet.ncaabb.service;

import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.framework.persistence.TournamentParticipantDao;
import com.jstevenperry.sportsnet.framework.service.AbstractTournamentRunnerService;
import com.jstevenperry.sportsnet.framework.util.AbstractNetworkUtils;
import com.jstevenperry.sportsnet.ncaabb.config.NcaaBbNetworkUtils;
import com.jstevenperry.sportsnet.ncaabb.config.NcaaNetworkProperties;
import com.jstevenperry.sportsnet.ncaabb.config.SportsnetProperties;
import com.jstevenperry.sportsnet.ncaabb.model.NcaaBbSeasonData;
import com.jstevenperry.sportsnet.ncaabb.service.plugins.TournamentRunnerPlugin2024;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class NcaaBbTournamentRunnerService extends AbstractTournamentRunnerService<NcaaBbSeasonData> implements InitializingBean {

    private final SportsnetProperties sportsnetProperties;
    private final NcaaNetworkProperties networkProperties;

    private final UUID serviceId;

    public NcaaBbTournamentRunnerService(@Qualifier("ncaaBbSeasonDataDao") final SeasonDataDao<NcaaBbSeasonData> seasonDataDao,
                                         @Qualifier("ncaaBbTournamentParticipantDao") final TournamentParticipantDao tournamentParticipantDao,
                                         @Qualifier("ncaaBbNetworkUtils") final AbstractNetworkUtils networkUtils,
                                         final SportsnetProperties sportsnetProperties,
                                         final NcaaNetworkProperties networkProperties) {
        super(seasonDataDao, tournamentParticipantDao, networkUtils, sportsnetProperties, networkProperties);
        this.sportsnetProperties = sportsnetProperties;
        this.networkProperties = networkProperties;
        this.serviceId = UUID.randomUUID();
    }

    @Override
    public void afterPropertiesSet() {
        loadPlugins();
    }

    private void loadPlugins() {
        getPluginMap().put(2024, new TournamentRunnerPlugin2024(getSeasonDataDao()));
    }

    @Override
    public List<Double> seasonDataAsDoubleList(final NcaaBbSeasonData seasonData,
                                               final Double resultClass,
                                               final boolean isTraining) {
        return NcaaBbSeasonData.asDoubleList(seasonData, resultClass, isTraining);
    }

    @Override
    public String getNetworkType() {
        return NcaaBbNetworkUtils.NETWORK_TYPE;
    }

    @Override
    protected String getSportsSpecificUri() {
        return sportsnetProperties.getSportsnetNcaabbUri();
    }

    @Override
    protected UUID getServiceId() {
        return this.serviceId;
    }

    @Override
    protected String getSport() {
        return networkProperties.getSport();
    }

}
