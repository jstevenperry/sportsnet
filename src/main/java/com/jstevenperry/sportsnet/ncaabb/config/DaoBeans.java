package com.jstevenperry.sportsnet.ncaabb.config;

import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.framework.persistence.TournamentParticipantDao;
import com.jstevenperry.sportsnet.ncaabb.model.NcaaBbSeasonData;
import com.jstevenperry.sportsnet.ncaabb.persistence.NcaaBbSeasonDataCsvDao;
import com.jstevenperry.sportsnet.ncaabb.persistence.NcaaBbTournamentParticipantCsvDao;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("ncaaBbDaoBeans")
public class DaoBeans {

    private final NcaaBbNetworkUtils ncaaBbNetworkUtils;
    private final SportsnetProperties sportsnetProperties;

    public DaoBeans(final NcaaBbNetworkUtils ncaaBbNetworkUtils,
                    final SportsnetProperties sportsnetProperties) {
        this.ncaaBbNetworkUtils = ncaaBbNetworkUtils;
        this.sportsnetProperties = sportsnetProperties;
    }

    @Bean("ncaaBbSeasonDataDao")
    public SeasonDataDao<NcaaBbSeasonData> ncaaBbSeasonDataDao() {
        return new NcaaBbSeasonDataCsvDao(ncaaBbNetworkUtils, sportsnetProperties);
    }

    @Bean("ncaaBbTournamentParticipantDao")
    public TournamentParticipantDao ncaaBbTournamentParticipantDao() {
        return new NcaaBbTournamentParticipantCsvDao(ncaaBbNetworkUtils, sportsnetProperties);
    }

}
