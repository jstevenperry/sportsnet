/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet.ncaabb.persistence;

import com.jstevenperry.sportsnet.framework.model.TournamentParticipant;
import com.jstevenperry.sportsnet.framework.persistence.AbstractTournamentParticipantFlatFileDao;
import com.jstevenperry.sportsnet.ncaabb.model.NcaaBbSeasonData;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("ncaaBbTournamentParticipantDao")
public class NcaaBbTournamentParticipantFlatFileDao extends AbstractTournamentParticipantFlatFileDao<NcaaBbSeasonData> {

    private static final Logger log = LoggerFactory.getLogger(NcaaBbTournamentParticipantFlatFileDao.class);

    @Override
    protected String[][] getDataFileNames() {
        return new String[][]{
                {"2010", "/ncaabb/2010/tournament_participant.csv"},
                {"2011", "/ncaabb/2011/tournament_participant.csv"},
                {"2012", "/ncaabb/2012/tournament_participant.csv"},
                {"2013", "/ncaabb/2013/tournament_participant.csv"},
                {"2014", "/ncaabb/2014/tournament_participant.csv"},
                {"2015", "/ncaabb/2015/tournament_participant.csv"},
                {"2016", "/ncaabb/2016/tournament_participant.csv"},
                {"2017", "/ncaabb/2017/tournament_participant.csv"},
                {"2018", "/ncaabb/2018/tournament_participant.csv"}
        };
    }

    @Override
    protected int getNumberOfFields() {
        return 3;
    }

    @Override
    protected TournamentParticipant parseLine(final Integer year, final String[] line) {
        TournamentParticipant ret = new TournamentParticipant();
        ret.setYear(year);
        int yearFromDataFile = Integer.parseInt(line[0]);
        // Sanity check
        if (!year.equals(yearFromDataFile)) {
            String errorMessage = String.format("Specified year (%d) does not match year in data file (%d). Line: %s", year, yearFromDataFile,
                    StringUtils.join(line, ","));
            log.error("parseLine(): {}", errorMessage);
            throw new RuntimeException(errorMessage);
        }
        ret.setTeamName(StringUtils.trim(line[1]));
        ret.setNumberOfVictories(Integer.parseInt(line[2]));
        return ret;
    }

}