/*
 * Copyright (c) 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet.mlb.persistence;

import com.jstevenperry.sportsnet.framework.model.TournamentParticipant;
import com.jstevenperry.sportsnet.framework.persistence.AbstractTournamentParticipantCsvDao;
import com.jstevenperry.sportsnet.mlb.config.MlbNetworkUtils;
import com.jstevenperry.sportsnet.mlb.config.SportsnetProperties;
import com.jstevenperry.sportsnet.mlb.model.MlbSeasonData;
import com.jstevenperry.sportsnet.mlb.util.MlbUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MlbTournamentParticipantCsvDao extends AbstractTournamentParticipantCsvDao<MlbSeasonData> {
    private static final Logger log = LoggerFactory.getLogger(MlbTournamentParticipantCsvDao.class);
    private final SportsnetProperties sportsnetProperties;

    public MlbTournamentParticipantCsvDao(final MlbNetworkUtils networkUtils,
                                          final SportsnetProperties sportsnetProperties) {
        super(networkUtils, sportsnetProperties);
        this.sportsnetProperties = sportsnetProperties;
    }

    @Override
    protected String getSportsSpecificUrl() {
        return sportsnetProperties.getSportsnetMlbUri();
    }

    @Override
    protected int getNumberOfFields() {
        // Team Name,Distance
        return 2;
    }

    @Override
    protected String[][] getDataFileNames() {
        return new String[][]{
                {"2010", "playoff-results.csv"},
                {"2011", "playoff-results.csv"},
                {"2012", "playoff-results.csv"},
                {"2013", "playoff-results.csv"},
                {"2014", "playoff-results.csv"},
                {"2015", "playoff-results.csv"},
                {"2016", "playoff-results.csv"},
                {"2017", "playoff-results.csv"},
                {"2018", "playoff-results.csv"},
                {"2019", "playoff-results.csv"},
                {"2020", "playoff-results.csv"},
                {"2021", "playoff-results.csv"},
                {"2022", "playoff-results.csv"},
        };
    }

    /**
     * Returns the number of output classes in the network. This number is super important
     * and returning a simple integer value from here is anti-climatic. But I'm not sure
     * where else to put it.
     */
    @Override
    public Integer getNumberOfOutputs() {
        return 6;
    }

    @Override
    protected TournamentParticipant parseLine(final Integer year, final String[] line) {
        TournamentParticipant ret = new TournamentParticipant();
        ret.setYear(year);
        ret.setTeamName(MlbUtils.toStandardTeamName(StringUtils.remove(StringUtils.remove(StringUtils.trim(line[0]), '*'), '.')));
        ret.setNumberOfVictories(Integer.parseInt(line[1]));
        return ret;
    }

}
