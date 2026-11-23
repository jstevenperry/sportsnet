package com.jstevenperry.sportsnet.ncaabb.service;

import com.jstevenperry.sportsnet.framework.model.TournamentParticipant;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.framework.persistence.TournamentParticipantDao;
import com.jstevenperry.sportsnet.framework.service.AbstractNetworkTrainingService;
import com.jstevenperry.sportsnet.framework.util.AbstractNetworkUtils;
import com.jstevenperry.sportsnet.ncaabb.config.NcaaBbNetworkUtils;
import com.jstevenperry.sportsnet.ncaabb.config.NcaaNetworkProperties;
import com.jstevenperry.sportsnet.ncaabb.config.SportsnetProperties;
import com.jstevenperry.sportsnet.ncaabb.model.NcaaBbSeasonData;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class NcaaBbNetworkTrainingService extends AbstractNetworkTrainingService<NcaaBbSeasonData> {

    private final SportsnetProperties sportsnetProperties;
    private final NcaaNetworkProperties networkProperties;

    private final UUID serviceId;

    public NcaaBbNetworkTrainingService(@Qualifier("ncaaBbSeasonDataDao") final SeasonDataDao<NcaaBbSeasonData> seasonDataDao,
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
    public String getNetworkType() {
        return NcaaBbNetworkUtils.NETWORK_TYPE;
    }

    @Override
    protected String getSportsSpecificUri() {
        return sportsnetProperties.getSportsnetNcaabbUri();
    }

    @Override
    protected UUID getServiceId() {
        return serviceId;
    }

    @Override
    protected String getSport() {
        return networkProperties.getSport();
    }

    @Override
    public List<Double> seasonDataAsDoubleList(final NcaaBbSeasonData ncaaBbSeasonData, final Double resultClass, final boolean isTraining) {
        return NcaaBbSeasonData.asDoubleList(ncaaBbSeasonData, resultClass, isTraining);
    }

    @Override
    protected Integer getNumberOfInputs() {
        return getSeasonDataDao().getNumberOfInputs();
    }

    @Override
    protected Integer getNumberOfOutputs() {
        return getTournamentParticipantDao().getNumberOfOutputs();
    }

    @Override
    protected List<String[]> smoothData(final TournamentParticipant tournamentParticipant, final String[] rowWinString) {
        final List<String[]> additionalDataRows = new ArrayList<>();
        final int numberOfVictories = tournamentParticipant.getNumberOfVictories();
        final int extraRows = switch (numberOfVictories) {
            // Evenly weight everything
            case 1 -> 1;
            case 2 -> 3;
            case 3 -> 7;
            case 4 -> 15;
            case 5 -> 31;
            case 6 -> 63;
            default -> 0; // Do nothing
        };
        for (int aa = 0; aa < extraRows; aa++) {
            additionalDataRows.add(rowWinString);
        }
        return additionalDataRows;
    }
}
