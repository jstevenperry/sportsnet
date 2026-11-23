/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.mlb.service;

import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.framework.persistence.TournamentParticipantDao;
import com.jstevenperry.sportsnet.framework.service.AbstractNetworkTrainingService;
import com.jstevenperry.sportsnet.mlb.config.MlbNetworkProperties;
import com.jstevenperry.sportsnet.mlb.config.MlbNetworkUtils;
import com.jstevenperry.sportsnet.mlb.config.SportsnetProperties;
import com.jstevenperry.sportsnet.mlb.model.MlbSeasonData;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class MlbNetworkTrainingService extends AbstractNetworkTrainingService<MlbSeasonData> {

    private final SportsnetProperties sportsnetProperties;

    private final UUID serviceId;
    private final MlbNetworkProperties networkProperties;

    public MlbNetworkTrainingService(@Qualifier("mlbSeasonDataDao") final SeasonDataDao<MlbSeasonData> seasonDataDao,
                                     @Qualifier("mlbTournamentParticipantDao") final TournamentParticipantDao tournamentParticipantDao,
                                     @Qualifier("mlbNetworkUtils") final MlbNetworkUtils networkUtils,
                                     final SportsnetProperties sportsnetProperties,
                                     final MlbNetworkProperties networkProperties) {
        super(seasonDataDao, tournamentParticipantDao, networkUtils, sportsnetProperties, networkProperties);
        this.sportsnetProperties = sportsnetProperties;
        this.networkProperties = networkProperties;
        this.serviceId = UUID.randomUUID();
    }

    @Override
    public List<Double> seasonDataAsDoubleList(final MlbSeasonData seasonData, final Double resultClass, final boolean isTraining) {
        return MlbSeasonData.asDoubleList(seasonData, resultClass, isTraining);
    }

    @Override
    public String getNetworkType() {
        return MlbNetworkUtils.NETWORK_TYPE;
    }

    @Override
    protected String getSportsSpecificUri() {
        return sportsnetProperties.getSportsnetMlbUri();
    }

    @Override
    protected UUID getServiceId() {
        return this.serviceId;
    }

    @Override
    protected String getSport() {
        return networkProperties.getSport();
    }

    @Override
    protected Integer getNumberOfInputs() {
        return getSeasonDataDao().getNumberOfInputs();
    }

    @Override
    protected Integer getNumberOfOutputs() {
        return getTournamentParticipantDao().getNumberOfOutputs();
    }

}
