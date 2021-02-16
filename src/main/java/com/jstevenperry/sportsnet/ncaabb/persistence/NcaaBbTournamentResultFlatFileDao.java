/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet.ncaabb.persistence;

import com.jstevenperry.sportsnet.framework.model.TournamentResult;
import com.jstevenperry.sportsnet.framework.persistence.AbstractTournamentResultFlatFileDao;
import com.jstevenperry.sportsnet.ncaabb.model.NcaaBbSeasonData;
import com.opencsv.CSVReader;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component("ncaaBbTournamentResultDao")
public class NcaaBbTournamentResultFlatFileDao extends AbstractTournamentResultFlatFileDao<NcaaBbSeasonData> {

    private static final Logger log = LoggerFactory.getLogger(NcaaBbTournamentResultFlatFileDao.class);

    @Override
    protected String[][] getDataFileNames() {
        return new String[][]{
                {"2010", "/ncaabb/2010/tournament_result.csv"},
                {"2011", "/ncaabb/2011/tournament_result.csv"},
                {"2012", "/ncaabb/2012/tournament_result.csv"},
                {"2013", "/ncaabb/2013/tournament_result.csv"},
                {"2014", "/ncaabb/2014/tournament_result.csv"},
                {"2015", "/ncaabb/2015/tournament_result.csv"},
                {"2016", "/ncaabb/2016/tournament_result.csv"},
                {"2017", "/ncaabb/2017/tournament_result.csv"},
                {"2018", "/ncaabb/2018/tournament_result.csv"}
        };
    }

    @Override
    protected int getNumberOfFields() {
        return 6;
    }

    @Override
    protected TournamentResult parseLine(final Integer year, final String[] line) {
        TournamentResult ret = new TournamentResult();
        ret.setYear(Integer.parseInt(line[0]));
        ret.setGameDate(Date.from(LocalDate.parse(line[1]).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        ret.setWinningTeamName(StringUtils.trim(line[2]));
        ret.setWinningScore(Integer.parseInt(line[3]));
        ret.setLosingTeamName(StringUtils.trim(line[4]));
        ret.setLosingScore(Integer.parseInt(line[5]));
        return ret;
    }

}
