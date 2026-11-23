/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.mlb.config;

import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.framework.persistence.TournamentParticipantDao;
import com.jstevenperry.sportsnet.mlb.model.MlbSeasonData;
import com.jstevenperry.sportsnet.mlb.persistence.MlbSeasonDataCsvDao;
import com.jstevenperry.sportsnet.mlb.persistence.MlbTournamentParticipantCsvDao;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * This is where the beans go that will be used for Data Access Objects.
 */
@Configuration("mlbDaoBeans")
public class DaoBeans {

    private final MlbNetworkUtils mlbNetworkUtils;
    private final SportsnetProperties sportsnetProperties;

    public DaoBeans(final MlbNetworkUtils mlbNetworkUtils,
                    final SportsnetProperties sportsnetProperties) {
        this.mlbNetworkUtils = mlbNetworkUtils;
        this.sportsnetProperties = sportsnetProperties;
    }

    @Bean("mlbSeasonDataDao")
    public SeasonDataDao<MlbSeasonData> seasonDataDao() {
        //
        // Create the Flat File DAO type
        return new MlbSeasonDataCsvDao(mlbNetworkUtils, sportsnetProperties);
    }

    @Bean("mlbTournamentParticipantDao")
    public TournamentParticipantDao tournamentParticipantDao() {
        //
        // Create the Flat File DAO type
        return new MlbTournamentParticipantCsvDao(mlbNetworkUtils, sportsnetProperties);
    }

}
