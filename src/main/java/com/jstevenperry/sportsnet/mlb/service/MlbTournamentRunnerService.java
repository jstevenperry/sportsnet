/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.mlb.service;

import com.jstevenperry.sportsnet.framework.dto.TournamentSimulationDto;
import com.jstevenperry.sportsnet.framework.exception.BadRequestException;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.framework.persistence.TournamentParticipantDao;
import com.jstevenperry.sportsnet.framework.service.AbstractTournamentRunnerService;
import com.jstevenperry.sportsnet.mlb.config.MlbNetworkProperties;
import com.jstevenperry.sportsnet.mlb.config.MlbNetworkUtils;
import com.jstevenperry.sportsnet.mlb.config.SportsnetProperties;
import com.jstevenperry.sportsnet.mlb.model.MlbSeasonData;
import com.jstevenperry.sportsnet.mlb.service.plugins.MlbTournamentPlugin2009;
import com.jstevenperry.sportsnet.mlb.service.plugins.MlbTournamentPlugin2023;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class MlbTournamentRunnerService extends AbstractTournamentRunnerService<MlbSeasonData> implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(MlbTournamentRunnerService.class);

    private final SeasonDataDao<MlbSeasonData> seasonDataDao;
    private final SportsnetProperties sportsnetProperties;
    private final MlbNetworkProperties networkProperties;

    private final UUID serviceId;

    public MlbTournamentRunnerService(@Qualifier("mlbSeasonDataDao") final SeasonDataDao<MlbSeasonData> seasonDataDao,
                                      @Qualifier("mlbTournamentParticipantDao") final TournamentParticipantDao tournamentParticipantDao,
                                      @Qualifier("mlbNetworkUtils") final MlbNetworkUtils networkUtils,
                                      final SportsnetProperties sportsnetProperties,
                                      final MlbNetworkProperties networkProperties) {
        super(seasonDataDao, tournamentParticipantDao, networkUtils, sportsnetProperties, networkProperties);
        this.seasonDataDao = seasonDataDao;
        this.sportsnetProperties = sportsnetProperties;
        this.networkProperties = networkProperties;
        this.serviceId = UUID.randomUUID();
    }

    @Override
    public void afterPropertiesSet() {
        loadPlugins();
    }

    private void loadPlugins() {
        //
        // Add plugins here
        //
        super.getPluginMap().put(2009, new MlbTournamentPlugin2009(seasonDataDao));
        super.getPluginMap().put(2023, new MlbTournamentPlugin2023(seasonDataDao));
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
    public void validateParametersOrError(final TournamentSimulationDto tournamentSimulationDto) {
        //
        if (tournamentSimulationDto.getTournamentYear() == null) {
            String errorMessage = "Tournament Year cannot be null";
            log.error("validateParametersOrError(): {}", errorMessage);
            throw new BadRequestException(errorMessage);
        }
    }

}
