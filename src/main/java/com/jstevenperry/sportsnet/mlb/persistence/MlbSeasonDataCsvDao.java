/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet.mlb.persistence;

import com.jstevenperry.sportsnet.framework.model.ResourceType;
import com.jstevenperry.sportsnet.framework.persistence.AbstractSeasonDataCsvDao;
import com.jstevenperry.sportsnet.framework.persistence.FileProcessor;
import com.jstevenperry.sportsnet.mlb.config.MlbNetworkUtils;
import com.jstevenperry.sportsnet.mlb.config.SportsnetProperties;
import com.jstevenperry.sportsnet.mlb.model.MlbSeasonData;
import com.jstevenperry.sportsnet.mlb.util.MlbUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MlbSeasonDataCsvDao extends AbstractSeasonDataCsvDao<MlbSeasonData> {

    private static final Logger log = LoggerFactory.getLogger(MlbSeasonDataCsvDao.class);
    private final MlbNetworkUtils mlbNetworkUtils;
    private final SportsnetProperties sportsnetProperties;

    public MlbSeasonDataCsvDao(final MlbNetworkUtils mlbNetworkUtils,
                               final SportsnetProperties sportsnetProperties) {
        super(mlbNetworkUtils);
        this.mlbNetworkUtils = mlbNetworkUtils;
        this.sportsnetProperties = sportsnetProperties;
    }

    @Override
    protected String[][] getDataFileNames() {
        return new String[][]{
                {"2009", "2009-Team-Batting.csv", "2009-Team-Pitching.csv", "2009-Team-Fielding.csv"},
                {"2010", "2010-Team-Batting.csv", "2010-Team-Pitching.csv", "2010-Team-Fielding.csv"},
                {"2011", "2011-Team-Batting.csv", "2011-Team-Pitching.csv", "2011-Team-Fielding.csv"},
                {"2012", "2012-Team-Batting.csv", "2012-Team-Pitching.csv", "2012-Team-Fielding.csv"},
                {"2013", "2013-Team-Batting.csv", "2013-Team-Pitching.csv", "2013-Team-Fielding.csv"},
                {"2014", "2014-Team-Batting.csv", "2014-Team-Pitching.csv", "2014-Team-Fielding.csv"},
                {"2015", "2015-Team-Batting.csv", "2015-Team-Pitching.csv", "2015-Team-Fielding.csv"},
                {"2016", "2016-Team-Batting.csv", "2016-Team-Pitching.csv", "2016-Team-Fielding.csv"},
                {"2017", "2017-Team-Batting.csv", "2017-Team-Pitching.csv", "2017-Team-Fielding.csv"},
                {"2018", "2018-Team-Batting.csv", "2018-Team-Pitching.csv", "2018-Team-Fielding.csv"},
                {"2019", "2019-Team-Batting.csv", "2019-Team-Pitching.csv", "2019-Team-Fielding.csv"},
                {"2020", "2020-Team-Batting.csv", "2020-Team-Pitching.csv", "2020-Team-Fielding.csv"},
                {"2021", "2021-Team-Batting.csv", "2021-Team-Pitching.csv", "2021-Team-Fielding.csv"},
                {"2022", "2022-Team-Batting.csv", "2022-Team-Pitching.csv", "2022-Team-Fielding.csv"},
                {"2023", "2023-Team-Batting.csv", "2023-Team-Pitching.csv", "2023-Team-Fielding.csv"},
        };
    }

    @Override
    protected Map<String, MlbSeasonData> loadData(final Map<Integer, Map<String, MlbSeasonData>> database, final Integer integer) {
        Map<String, MlbSeasonData> seasonDataMap = null;
        for (final String[] seasonDataEntry : getDataFileNames()) {
            Integer year = Integer.parseInt(seasonDataEntry[0]);
            for (int seasonDataFileIndex = 1; seasonDataFileIndex < seasonDataEntry.length; seasonDataFileIndex++) {
                String seasonDataFileName = seasonDataEntry[seasonDataFileIndex];
                String seasonDataFileNameUrl = mlbNetworkUtils.computeDataFileUrl(sportsnetProperties, sportsnetProperties.getSportsnetMlbUri(), year, seasonDataFileName, ResourceType.SEASON_DATA_FILE);
                seasonDataMap = database.computeIfAbsent(year, k -> new HashMap<>());
                // See sternly worded comment in SEASON_DATA_FILE_NAMES declaration
                switch (seasonDataFileIndex) {
                    case 1 -> processBattingFile(year, seasonDataFileNameUrl, seasonDataMap);
                    case 2 -> processPitchingFile(year, seasonDataFileNameUrl, seasonDataMap);
                    case 3 -> processFieldingFile(year, seasonDataFileNameUrl, seasonDataMap);
                }
            }
        }
        return seasonDataMap;
    }

    @Override
    protected int getNumberOfFields() {
        throw new UnsupportedOperationException("getNumberOfFields() is not implemented. Reason = not appropriate for this class.");
    }

    private void processBattingFile(final Integer year, final String seasonDataFileName, final Map<String, MlbSeasonData> teamSeasonDataMap) {
        processSeasonDataFile(year, seasonDataFileName, teamSeasonDataMap, new FileProcessor<>() {
            @Override
            public String getRequiredFilenameEnding() {
                return String.format("Team-Batting.csv?resourceType=SEASON_DATA_FILE&year=%d", year);
            }

            @Override
            public int getNumberOfFields() {
                return 29;
            }

            @Override
            public MlbSeasonData parseAndProcessFileLine(final String[] line, final Map<String, MlbSeasonData> teamSeasonDataMap) {
                return parseAndProcessTeamBattingFileLine(line, teamSeasonDataMap);
            }

            @Override
            public int getNumberOfHeaderLines() {
                return 1;
            }

        });
    }

    private MlbSeasonData parseAndProcessTeamBattingFileLine(final String[] line,
                                                             final Map<String, MlbSeasonData> teamSeasonDataMap) {
        String teamName = MlbUtils.toStandardTeamName(StringUtils.remove(StringUtils.remove(StringUtils.trim(line[0]), '*'), '.'));
        MlbSeasonData seasonData = getSeasonDataComputeIfAbsent(teamSeasonDataMap, teamName);
        //Tm,#Bat,BatAge,R/G,G,PA,AB,R,H,2B,3B,HR,RBI,SB,CS,BB,SO,BA,OBP,SLG,OPS,OPS+,TB,GDP,HBP,SH,SF,IBB,LOB
        seasonData.setNumBat(Integer.parseInt(nice(StringUtils.trim(line[1]))));
        seasonData.setBatAge(Double.parseDouble(nice(StringUtils.trim(line[2]))));
        seasonData.setrPerG(Double.parseDouble(nice(StringUtils.trim(line[3]))));
        seasonData.setGamesBat(Integer.parseInt(nice(StringUtils.trim(line[4]))));
        seasonData.setPlateAppear(Integer.parseInt(nice(StringUtils.trim(line[5]))));
        seasonData.setAtBat(Integer.parseInt(nice(StringUtils.trim(line[6]))));
        seasonData.setRuns(Integer.parseInt(nice(StringUtils.trim(line[7]))));
        seasonData.setHits(Integer.parseInt(nice(StringUtils.trim(line[8]))));
        seasonData.setDoubles(Integer.parseInt(nice(StringUtils.trim(line[9]))));
        seasonData.setTriples(Integer.parseInt(nice(StringUtils.trim(line[10]))));
        seasonData.setHr(Integer.parseInt(nice(StringUtils.trim(line[11]))));
        seasonData.setRbis(Integer.parseInt(nice(StringUtils.trim(line[12]))));
        seasonData.setSb(Integer.parseInt(nice(StringUtils.trim(line[13]))));
        seasonData.setCaughtStealing(Integer.parseInt(nice(StringUtils.trim(line[14]))));
        seasonData.setBb(Integer.parseInt(nice(StringUtils.trim(line[15]))));
        seasonData.setSo(Integer.parseInt(nice(StringUtils.trim(line[16]))));
        seasonData.setBa(Double.parseDouble(nice(StringUtils.trim(line[17]))));
        seasonData.setObp(Double.parseDouble(nice(StringUtils.trim(line[18]))));
        seasonData.setSlg(Double.parseDouble(nice(StringUtils.trim(line[19]))));
        seasonData.setOps(Double.parseDouble(nice(StringUtils.trim(line[20]))));
        seasonData.setOps_plus(Double.parseDouble(nice(StringUtils.trim(line[21]))));
        seasonData.setTb(Integer.parseInt(nice(StringUtils.trim(line[22]))));
        seasonData.setGdp(Integer.parseInt(nice(StringUtils.trim(line[23]))));
        seasonData.setHbp(Integer.parseInt(nice(StringUtils.trim(line[24]))));
        seasonData.setSh(Integer.parseInt(nice(StringUtils.trim(line[25]))));
        seasonData.setSf(Integer.parseInt(nice(StringUtils.trim(line[26]))));
        seasonData.setIbb(Integer.parseInt(nice(StringUtils.trim(line[27]))));
        seasonData.setLob(Integer.parseInt(nice(StringUtils.trim(line[28]))));
        return seasonData;
    }

    private void processPitchingFile(final Integer year, final String seasonDataFileName, final Map<String, MlbSeasonData> teamSeasonDataMap) {
        processSeasonDataFile(year, seasonDataFileName, teamSeasonDataMap, new FileProcessor<>() {
            @Override
            public String getRequiredFilenameEnding() {
                return String.format("Team-Pitching.csv?resourceType=SEASON_DATA_FILE&year=%d", year);
            }

            @Override
            public int getNumberOfFields() {
                return 36;
            }

            @Override
            public MlbSeasonData parseAndProcessFileLine(final String[] line, final Map<String, MlbSeasonData> teamSeasonDataMap) {
                return parseAndProcessTeamPitchingFileLine(line, teamSeasonDataMap);
            }

            @Override
            public int getNumberOfHeaderLines() {
                return 1;
            }
        });
    }

    private MlbSeasonData parseAndProcessTeamPitchingFileLine(final String[] line,
                                                              final Map<String, MlbSeasonData> teamSeasonDataMap) {
        String teamName = MlbUtils.toStandardTeamName(StringUtils.remove(StringUtils.remove(StringUtils.trim(line[0]), '*'), '.'));
        MlbSeasonData seasonData = getSeasonDataComputeIfAbsent(teamSeasonDataMap, teamName);
        //Tm,#P,PAge,RA/G,W,L,W-L%,ERA,G,GS,GF,CG,tSho,cSho,SV,IP,H,R,ER,HR,BB,IBB,SO,HBP,BK,WP,BF,ERA+,FIP,WHIP,H9,HR9,BB9,SO9,SO/W,LOB
        seasonData.setNumPitchers(Integer.parseInt(nice(StringUtils.trim(line[1]))));
        seasonData.setpAge(Double.parseDouble(nice(StringUtils.trim(line[2]))));
        seasonData.setRallPerG(Double.parseDouble(nice(StringUtils.trim(line[3]))));
        seasonData.setWin(Integer.parseInt(nice(StringUtils.trim(line[4]))));
        seasonData.setLoss(Integer.parseInt(nice(StringUtils.trim(line[5]))));
        seasonData.setWlpct(Double.parseDouble(nice(StringUtils.trim(line[6]))));
        seasonData.setEra(Double.parseDouble(nice(StringUtils.trim(line[7]))));
        seasonData.setGamesPitch(Integer.parseInt(nice(StringUtils.trim(line[8]))));
        seasonData.setGs(Integer.parseInt(nice(StringUtils.trim(line[9]))));
        seasonData.setGf(Integer.parseInt(nice(StringUtils.trim(line[10]))));
        seasonData.setCg(Integer.parseInt(nice(StringUtils.trim(line[11]))));
        seasonData.settSho(Integer.parseInt(nice(StringUtils.trim(line[12]))));
        seasonData.setcSho(Integer.parseInt(nice(StringUtils.trim(line[13]))));
        seasonData.setSv(Integer.parseInt(nice(StringUtils.trim(line[14]))));
        seasonData.setIp(Double.parseDouble(nice(StringUtils.trim(line[15]))));
        seasonData.setHa(Integer.parseInt(nice(StringUtils.trim(line[16]))));
        seasonData.setRa(Integer.parseInt(nice(StringUtils.trim(line[17]))));
        seasonData.setEr(Integer.parseInt(nice(StringUtils.trim(line[18]))));
        seasonData.setHra(Integer.parseInt(nice(StringUtils.trim(line[19]))));
        seasonData.setWalks(Integer.parseInt(nice(StringUtils.trim(line[20]))));
        seasonData.setIntWalks(Integer.parseInt(nice(StringUtils.trim(line[21]))));
        seasonData.setSoP(Integer.parseInt(nice(StringUtils.trim(line[22]))));
        seasonData.setHbpP(Integer.parseInt(nice(StringUtils.trim(line[23]))));
        seasonData.setBk(Integer.parseInt(nice(StringUtils.trim(line[24]))));
        seasonData.setWp(Integer.parseInt(nice(StringUtils.trim(line[25]))));
        seasonData.setBf(Integer.parseInt(nice(StringUtils.trim(line[26]))));
        seasonData.setEra_plus(Integer.parseInt(nice(StringUtils.trim(line[27]))));
        seasonData.setFip(Double.parseDouble(nice(StringUtils.trim(line[28]))));
        seasonData.setWhip(Double.parseDouble(nice(StringUtils.trim(line[29]))));
        seasonData.setH9(Double.parseDouble(nice(StringUtils.trim(line[30]))));
        seasonData.setHr9(Double.parseDouble(nice(StringUtils.trim(line[31]))));
        seasonData.setBb9(Double.parseDouble(nice(StringUtils.trim(line[32]))));
        seasonData.setSo9(Double.parseDouble(nice(StringUtils.trim(line[33]))));
        seasonData.setSoPerW(Double.parseDouble(nice(StringUtils.trim(line[34]))));
        seasonData.setLobP(Integer.parseInt(nice(StringUtils.trim(line[35]))));

        return seasonData;
    }

    private void processFieldingFile(final Integer year, final String seasonDataFileName, final Map<String, MlbSeasonData> teamSeasonDataMap) {
        processSeasonDataFile(year, seasonDataFileName, teamSeasonDataMap, new FileProcessor<>() {
            @Override
            public String getRequiredFilenameEnding() {
                return String.format("Team-Fielding.csv?resourceType=SEASON_DATA_FILE&year=%d", year);
            }

            @Override
            public int getNumberOfFields() {
                return 19;
            }

            @Override
            public MlbSeasonData parseAndProcessFileLine(final String[] line, final Map<String, MlbSeasonData> teamSeasonDataMap) {
                return parseAndProcessTeamFieldingFileLine(line, teamSeasonDataMap);
            }
            @Override
            public int getNumberOfHeaderLines() {
                return 1;
            }

        });
    }

    private MlbSeasonData parseAndProcessTeamFieldingFileLine(final String[] line,
                                                              final Map<String, MlbSeasonData> teamSeasonDataMap) {
        String teamName = MlbUtils.toStandardTeamName(StringUtils.remove(StringUtils.remove(StringUtils.trim(line[0]), '*'), '.'));
        MlbSeasonData seasonData = getSeasonDataComputeIfAbsent(teamSeasonDataMap, teamName);
        //Tm,#Fld,RA/G,DefEff,G,GS,CG,Inn,Ch,PO,A,E,DP,Fld%,Rtot,Rtot/yr,Rdrs,Rdrs/yr,Rgood
        seasonData.setNumFld(Integer.parseInt(nice(StringUtils.trim(line[1]))));
        seasonData.setRapg(Double.parseDouble(nice(StringUtils.trim(line[2]))));
        seasonData.setDefEff(Double.parseDouble(nice(StringUtils.trim(line[3]))));
        // Skip 4-6
        seasonData.setInn(Double.parseDouble(nice(StringUtils.trim(line[7]))));
        seasonData.setCh(Integer.parseInt(nice(StringUtils.trim(line[8]))));
        seasonData.setPo(Integer.parseInt(nice(StringUtils.trim(line[9]))));
        seasonData.setAssist(Integer.parseInt(nice(StringUtils.trim(line[10]))));
        seasonData.setErrors(Integer.parseInt(nice(StringUtils.trim(line[1]))));
        seasonData.setDp(Integer.parseInt(nice(StringUtils.trim(line[12]))));
        seasonData.setFldPct(Double.parseDouble(nice(StringUtils.trim(line[13]))));
        seasonData.setrTot(Double.parseDouble(nice(StringUtils.trim(line[14]))));
        seasonData.setrTotYr(Double.parseDouble(nice(StringUtils.trim(line[15]))));
        seasonData.setRdrs(Double.parseDouble(nice(StringUtils.trim(line[16]))));
        seasonData.setRdrsYr(Double.parseDouble(nice(StringUtils.trim(line[17]))));
        seasonData.setRgood(Double.parseDouble(nice(StringUtils.trim(line[18]))));
        return seasonData;
    }

    private static MlbSeasonData getSeasonDataComputeIfAbsent(final Map<String, MlbSeasonData> teamSeasonDataMap, final String teamName) {
        return teamSeasonDataMap.computeIfAbsent(teamName, k -> {
            // Season data not found for this team, create it and set team name (k, the key to the map)
            MlbSeasonData mlbSeasonData = new MlbSeasonData();
            mlbSeasonData.setTeamName(k);
            return mlbSeasonData;
        });
    }

    @Override
    public List<MlbSeasonData> fetchAllByYear(final Integer year) {
        Map<String, MlbSeasonData> teamSeasonDataMap = getSeasonDataMap(year);
        List<MlbSeasonData> ret = teamSeasonDataMap.keySet()
                .stream()
                .map(teamSeasonDataMap::get)
                .collect(Collectors.toList());
        log.debug(String.format("fetchAllByYear(%d): Returning %d records.", year, ret.size()));
        return ret;
    }

    @Override
    public MlbSeasonData fetchByYearAndTeamName(final Integer year, final String teamName) {
        MlbSeasonData ret;
        Map<String, MlbSeasonData> teamSeasonDataMap = getSeasonDataMap(year);
        ret = teamSeasonDataMap.get(teamName);

        log.trace("fetchByYearAndTeamName({}, {}): Returning MlbSeasonData: {}", year, teamName, ret);
        return ret;
    }

    @Override
    public Integer getNumberOfInputs() {
        return new MlbSeasonData().getNumberOfFields();
    }

}
