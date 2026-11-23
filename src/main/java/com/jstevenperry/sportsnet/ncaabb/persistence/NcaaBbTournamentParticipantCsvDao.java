/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet.ncaabb.persistence;

import com.jstevenperry.sportsnet.framework.model.TournamentParticipant;
import com.jstevenperry.sportsnet.framework.persistence.AbstractTournamentParticipantCsvDao;
import com.jstevenperry.sportsnet.framework.util.AbstractNetworkUtils;
import com.jstevenperry.sportsnet.ncaabb.config.SportsnetProperties;
import com.jstevenperry.sportsnet.ncaabb.model.NcaaBbSeasonData;
import org.apache.commons.lang3.StringUtils;

public class NcaaBbTournamentParticipantCsvDao extends AbstractTournamentParticipantCsvDao<NcaaBbSeasonData> {

    private final SportsnetProperties sportsnetProperties;

    public NcaaBbTournamentParticipantCsvDao(final AbstractNetworkUtils networkUtils,
                                             final SportsnetProperties sportsnetProperties) {
        super(networkUtils, sportsnetProperties);
        this.sportsnetProperties = sportsnetProperties;
    }

    @Override
    protected String[][] getDataFileNames() {
        return new String[][]{
                {"2010", "tournament_participant_all.csv"},
                {"2011", "tournament_participant_all.csv"},
                {"2012", "tournament_participant_all.csv"},
                {"2013", "tournament_participant_all.csv"},
                {"2014", "tournament_participant_all.csv"},
                {"2015", "tournament_participant_all.csv"},
                {"2016", "tournament_participant_all.csv"},
                {"2017", "tournament_participant_all.csv"},
                {"2018", "tournament_participant_all.csv"},
                {"2020", "tournament_participant_all.csv"},
                {"2021", "tournament_participant_all.csv"},
                {"2022", "tournament_participant_all.csv"},
                {"2023", "tournament_participant_all.csv"},
        };
    }

    @Override
    protected String getSportsSpecificUrl() {
        return sportsnetProperties.getSportsnetNcaabbUri();
    }

    @Override
    protected TournamentParticipant parseLine(final Integer year, final String[] line) {
        TournamentParticipant ret = new TournamentParticipant();
        ret.setYear(year);
        ret.setTeamName(StringUtils.remove(StringUtils.trim(line[0]), '*'));
        ret.setNumberOfVictories(Integer.parseInt(line[1]));
        return ret;
    }


    /**
     * Returns the number of output classes in the network. This number is super important
     * and returning a simple integer value from here is anti-climatic. But I'm not sure
     * where else to put it.
     */
    @Override
    public Integer getNumberOfOutputs() {
        return 7;
    }
}