package com.jstevenperry.sportsnet.nba.persistence;

import com.jstevenperry.sportsnet.framework.persistence.AbstractSeasonDataFlatFileDao;
import com.jstevenperry.sportsnet.framework.persistence.FileProcessor;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.nba.model.NbaSeasonData;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.jstevenperry.sportsnet.framework.util.NetworkUtils.nice;

@Component("nbaSeasonDataDao")
public class NbaSeasonDataFlatFileDao extends AbstractSeasonDataFlatFileDao<NbaSeasonData> implements SeasonDataDao<NbaSeasonData> {

    private static final Logger log = LoggerFactory.getLogger(NbaSeasonDataFlatFileDao.class);

    @Override
    protected String[][] getDataFileNames() {
        return new String[][] {
                {"2010", "/nba/2010-11-Team-Stats.csv", "/nba/2010-11-Opponent-Stats.csv", "/nba/2010-11-Miscellaneous-Stats.csv"},
                {"2011", "/nba/2011-12-Team-Stats.csv", "/nba/2011-12-Opponent-Stats.csv", "/nba/2011-12-Miscellaneous-Stats.csv"},
                {"2012", "/nba/2012-13-Team-Stats.csv", "/nba/2012-13-Opponent-Stats.csv", "/nba/2012-13-Miscellaneous-Stats.csv"},
                {"2013", "/nba/2013-14-Team-Stats.csv", "/nba/2013-14-Opponent-Stats.csv", "/nba/2013-14-Miscellaneous-Stats.csv"},
                {"2014", "/nba/2014-15-Team-Stats.csv", "/nba/2014-15-Opponent-Stats.csv", "/nba/2014-15-Miscellaneous-Stats.csv"},
                {"2015", "/nba/2015-16-Team-Stats.csv", "/nba/2015-16-Opponent-Stats.csv", "/nba/2015-16-Miscellaneous-Stats.csv"},
                {"2016", "/nba/2016-17-Team-Stats.csv", "/nba/2016-17-Opponent-Stats.csv", "/nba/2016-17-Miscellaneous-Stats.csv"},
                {"2017", "/nba/2017-18-Team-Stats.csv", "/nba/2017-18-Opponent-Stats.csv", "/nba/2017-18-Miscellaneous-Stats.csv"},
                {"2018", "/nba/2018-19-Team-Stats.csv", "/nba/2018-19-Opponent-Stats.csv", "/nba/2018-19-Miscellaneous-Stats.csv"},
                {"2019", "/nba/2019-20-Team-Stats.csv", "/nba/2019-20-Opponent-Stats.csv", "/nba/2019-20-Miscellaneous-Stats.csv"},
                //{"2020", "/nba/2020-21-Team-Stats.csv", "/nba/2020-21-Opponent-Stats.csv", "/nba/2020-21-Miscellaneous-Stats.csv"}
        };
    }

    @Override
    protected int getNumberOfFields() {
        throw new UnsupportedOperationException("getNumberOfFields() is not implemented. Reason = not appropriate for this class.");
    }

    @Override
    protected void loadData() {
        for (final String[] seasonDataEntry : getDataFileNames()) {
            Integer year = Integer.parseInt(seasonDataEntry[0]);
            for (int seasonDataFileIndex = 1; seasonDataFileIndex < seasonDataEntry.length; seasonDataFileIndex++) {
                String seasonDataFileName = seasonDataEntry[seasonDataFileIndex];
                Map<String, NbaSeasonData> teamSeasonDataMap = getDatabase().computeIfAbsent(year, k -> new HashMap<>());
                // See sternly worded comment in SEASON_DATA_FILE_NAMES declaration
                switch (seasonDataFileIndex) {
                    case 1:
                        processTeamStatsFile(year, seasonDataFileName, teamSeasonDataMap);
                        break;
                    case 2:
                        processOpponentStatsFile(year, seasonDataFileName, teamSeasonDataMap);
                        break;
                    case 3:
                        processMiscellaneousStatsFile(year, seasonDataFileName, teamSeasonDataMap);
                        break;
                }
            }
        }
    }

    private void processTeamStatsFile(final Integer year,
                                      final String seasonDataFileName,
                                      final Map<String, NbaSeasonData> teamSeasonDataMap) {
        processSeasonDataFile(year, seasonDataFileName, teamSeasonDataMap, new FileProcessor<NbaSeasonData>() {
            @Override
            public String getRequiredFilenameEnding() {
                return "Team-Stats.csv";
            }

            @Override
            public int getNumberOfFields() {
                return 25;
            }

            @Override
            public NbaSeasonData parseAndProcessFileLine(final String[] line, final Map<String, NbaSeasonData> teamSeasonDataMap) {
                return parseAndProcessTeamStatsFileLine(line, teamSeasonDataMap);
            }

            @Override
            public int getNumberOfHeaderLines() {
                return 1;
            }

            private NbaSeasonData parseAndProcessTeamStatsFileLine(final String[] line,
                                                                   final Map<String, NbaSeasonData> teamSeasonDataMap) {
                String teamName = StringUtils.remove(StringUtils.remove(StringUtils.trim(line[1]), '*'), '.');
                NbaSeasonData seasonData = getSeasonDataComputeIfAbsent(teamSeasonDataMap, teamName);
                seasonData.setTeamStatsRank(Integer.parseInt(nice(StringUtils.trim(line[0]))));
                seasonData.setGamesPlayed(Integer.parseInt(nice(StringUtils.trim(line[2]))));
                seasonData.setMinutesPlayed(Integer.parseInt(nice(StringUtils.trim(line[3]))));
                seasonData.setFieldGoals(Integer.parseInt(nice(StringUtils.trim(line[4]))));
                seasonData.setFieldGoalAtt(Integer.parseInt(nice(StringUtils.trim(line[5]))));
                seasonData.setFieldGoalPct(Double.parseDouble(nice(StringUtils.trim(StringUtils.remove(line[6], '%')))));
                seasonData.setThreePointers(Integer.parseInt(nice(StringUtils.trim(line[7]))));
                seasonData.setThreePointerAtt(Integer.parseInt(nice(StringUtils.trim(line[8]))));
                seasonData.setThreePointerPct(Double.parseDouble(nice(StringUtils.trim(StringUtils.remove(line[9], '%')))));
                seasonData.setTwoPointers(Integer.parseInt(nice(StringUtils.trim(line[10]))));
                seasonData.setTwoPointerAtt(Integer.parseInt(nice(StringUtils.trim(line[11]))));
                seasonData.setTwoPointerPct(Double.parseDouble(nice(StringUtils.trim(StringUtils.remove(line[12], '%')))));
                seasonData.setFreeThrows(Integer.parseInt(nice(StringUtils.trim(line[13]))));
                seasonData.setFreeThrowAtt(Integer.parseInt(nice(StringUtils.trim(line[14]))));
                seasonData.setFreeThrowPct(Double.parseDouble(nice(StringUtils.trim(StringUtils.remove(line[15], '%')))));
                seasonData.setOffensiveRebounds(Integer.parseInt(nice(StringUtils.trim(line[16]))));
                seasonData.setDefensiveRebounds(Integer.parseInt(nice(StringUtils.trim(line[17]))));
                // ignore total rebounds
                seasonData.setAssists(Integer.parseInt(nice(StringUtils.trim(line[19]))));
                seasonData.setSteals(Integer.parseInt(nice(StringUtils.trim(line[20]))));
                seasonData.setBlocks(Integer.parseInt(nice(StringUtils.trim(line[21]))));
                seasonData.setTurnovers(Integer.parseInt(nice(StringUtils.trim(line[22]))));
                seasonData.setFouls(Integer.parseInt(nice(StringUtils.trim(line[23]))));
                seasonData.setPoints(Integer.parseInt(nice(StringUtils.trim(line[24]))));

                return seasonData;
            }
        });
    }

    private void processOpponentStatsFile(final Integer year,
                                          final String seasonDataFileName,
                                          final Map<String, NbaSeasonData> teamSeasonDataMap) {
        processSeasonDataFile(year, seasonDataFileName, teamSeasonDataMap, new FileProcessor<NbaSeasonData>() {

            @Override
            public String getRequiredFilenameEnding() {
                return "Opponent-Stats.csv";
            }

            @Override
            public int getNumberOfFields() {
                return 25;
            }

            @Override
            public int getNumberOfHeaderLines() {
                return 1;
            }

            @Override
            public NbaSeasonData parseAndProcessFileLine(final String[] line, final Map<String, NbaSeasonData> teamSeasonDataMap) {
                String teamName = StringUtils.remove(StringUtils.remove(StringUtils.trim(line[1]), '*'), '.');
                NbaSeasonData seasonData = getSeasonDataComputeIfAbsent(teamSeasonDataMap, teamName);
                seasonData.setOppStatsRank(Integer.parseInt(nice(StringUtils.trim(line[0]))));
                seasonData.setOppGamesPlayed(Integer.parseInt(nice(StringUtils.trim(line[2]))));
                seasonData.setOppMinutesPlayed(Integer.parseInt(nice(StringUtils.trim(line[3]))));
                seasonData.setOppFieldGoals(Integer.parseInt(nice(StringUtils.trim(line[4]))));
                seasonData.setOppFieldGoalAtt(Integer.parseInt(nice(StringUtils.trim(line[5]))));
                seasonData.setOppFieldGoalPct(Double.parseDouble(nice(StringUtils.trim(StringUtils.remove(line[6], '%')))));
                seasonData.setOppThreePointers(Integer.parseInt(nice(StringUtils.trim(line[7]))));
                seasonData.setOppThreePointerAtt(Integer.parseInt(nice(StringUtils.trim(line[8]))));
                seasonData.setOppThreePointerPct(Double.parseDouble(nice(StringUtils.trim(StringUtils.remove(line[9], '%')))));
                seasonData.setOppTwoPointers(Integer.parseInt(nice(StringUtils.trim(line[10]))));
                seasonData.setOppTwoPointerAtt(Integer.parseInt(nice(StringUtils.trim(line[11]))));
                seasonData.setOppTwoPointerPct(Double.parseDouble(nice(StringUtils.trim(StringUtils.remove(line[12], '%')))));
                seasonData.setOppFreeThrows(Integer.parseInt(nice(StringUtils.trim(line[13]))));
                seasonData.setOppFreeThrowAtt(Integer.parseInt(nice(StringUtils.trim(line[14]))));
                seasonData.setOppFreeThrowPct(Double.parseDouble(nice(StringUtils.trim(StringUtils.remove(line[15], '%')))));
                seasonData.setOppOffensiveRebounds(Integer.parseInt(nice(StringUtils.trim(line[16]))));
                seasonData.setOppDefensiveRebounds(Integer.parseInt(nice(StringUtils.trim(line[17]))));
                // ignore total rebounds
                seasonData.setOppAssists(Integer.parseInt(nice(StringUtils.trim(line[19]))));
                seasonData.setOppSteals(Integer.parseInt(nice(StringUtils.trim(line[20]))));
                seasonData.setOppBlocks(Integer.parseInt(nice(StringUtils.trim(line[21]))));
                seasonData.setOppTurnovers(Integer.parseInt(nice(StringUtils.trim(line[22]))));
                seasonData.setOppFouls(Integer.parseInt(nice(StringUtils.trim(line[23]))));
                seasonData.setOppPoints(Integer.parseInt(nice(StringUtils.trim(line[24]))));

                return seasonData;
            }
        });
    }

    private void processMiscellaneousStatsFile(final Integer year,
                                               final String seasonDataFileName,
                                               final Map<String, NbaSeasonData> teamSeasonDataMap) {
        processSeasonDataFile(year, seasonDataFileName, teamSeasonDataMap, new FileProcessor<NbaSeasonData>() {

            @Override
            public String getRequiredFilenameEnding() {
                return "Miscellaneous-Stats.csv";
            }

            @Override
            public int getNumberOfFields() {
                return 28;
            }

            @Override
            public NbaSeasonData parseAndProcessFileLine(final String[] line, final Map<String, NbaSeasonData> teamSeasonDataMap) {
                String teamName = StringUtils.remove(StringUtils.remove(StringUtils.trim(line[1]), '*'), '.');
                NbaSeasonData seasonData = getSeasonDataComputeIfAbsent(teamSeasonDataMap, teamName);
                seasonData.setMiscellaneousStatsRank(Integer.parseInt(nice(StringUtils.trim(line[0]))));
                seasonData.setAgeOnFeb1(Double.parseDouble(nice(StringUtils.trim(StringUtils.remove(line[2], '%')))));
                seasonData.setWins(Integer.parseInt(nice(StringUtils.trim(line[3]))));
                seasonData.setLosses(Integer.parseInt(nice(StringUtils.trim(line[4]))));
                seasonData.setPythagoreanWins(Integer.parseInt(nice(StringUtils.trim(line[5]))));
                seasonData.setPythagoreanLosses(Integer.parseInt(nice(StringUtils.trim(line[6]))));
                seasonData.setMarginOfVictory(Double.parseDouble(nice(StringUtils.trim(StringUtils.remove(line[7], '%')))));
                seasonData.setStrengthOfSchedule(Double.parseDouble(nice(StringUtils.trim(StringUtils.remove(line[8], '%')))));
                seasonData.setSimpleRatingSystem(Double.parseDouble(nice(StringUtils.trim(StringUtils.remove(line[9], '%')))));
                seasonData.setOffensiveRating(Double.parseDouble(nice(StringUtils.trim(StringUtils.remove(line[10], '%')))));
                seasonData.setDefensiveRating(Double.parseDouble(nice(StringUtils.trim(StringUtils.remove(line[11], '%')))));
                seasonData.setNetRating(Double.parseDouble(nice(StringUtils.trim(StringUtils.remove(line[12], '%')))));
                seasonData.setPace(Double.parseDouble(nice(StringUtils.trim(StringUtils.remove(line[13], '%')))));
                seasonData.setFreeThrowAttRate(Double.parseDouble(nice(StringUtils.trim(StringUtils.remove(line[14], '%')))));
                seasonData.setThreePtAttRate(Double.parseDouble(nice(StringUtils.trim(StringUtils.remove(line[15], '%')))));
                seasonData.setTrueShootingPct(Double.parseDouble(nice(StringUtils.trim(StringUtils.remove(line[16], '%')))));
                seasonData.setEffFieldGoalPct(Double.parseDouble(nice(StringUtils.trim(StringUtils.remove(line[17], '%')))));
                seasonData.setTurnoverPct(Double.parseDouble(nice(StringUtils.trim(StringUtils.remove(line[18], '%')))));
                seasonData.setOffensiveReboundPct(Double.parseDouble(nice(StringUtils.trim(StringUtils.remove(line[19], '%')))));
                seasonData.setFreeThrowMadeRate(Double.parseDouble(nice(StringUtils.trim(StringUtils.remove(line[20], '%')))));
                seasonData.setOppEffFieldGoalPct(Double.parseDouble(nice(StringUtils.trim(StringUtils.remove(line[21], '%')))));
                seasonData.setOppTurnoverPct(Double.parseDouble(nice(StringUtils.trim(StringUtils.remove(line[22], '%')))));
                seasonData.setOppOffensiveReboundPct(Double.parseDouble(nice(StringUtils.trim(StringUtils.remove(line[23], '%')))));
                seasonData.setOppFreeThrowMadeRate(Double.parseDouble(nice(StringUtils.trim(StringUtils.remove(line[24], '%')))));
                // ignore arena
                // ignore attendance
                seasonData.setAttendancePerGame(Double.parseDouble(nice(StringUtils.trim(StringUtils.remove(line[27], '%')))));

                return seasonData;
            }
        });
    }

    private static NbaSeasonData getSeasonDataComputeIfAbsent(final Map<String, NbaSeasonData> teamSeasonDataMap,
                                                              final String teamName) {
        return teamSeasonDataMap.computeIfAbsent(teamName, k -> {
            // Season data not found for this team, create it and set team name (k, the key to the map)
            NbaSeasonData nbaSeasonData = new NbaSeasonData();
            nbaSeasonData.setTeamName(k);
            return nbaSeasonData;
        });
    }

    @Override
    public List<NbaSeasonData> fetchAllByYear(final Integer year) {
        Map<String, NbaSeasonData> teamSeasonDataMap = getDatabase().get(year);
        List<NbaSeasonData> ret = teamSeasonDataMap.keySet()
                .stream()
                .map(teamSeasonDataMap::get)
                .collect(Collectors.toList());
        log.debug(String.format("fetchAllByYear(%d): Returning %d records.", year, ret.size()));
        return ret;
    }

    @Override
    public NbaSeasonData fetchByYearAndTeamName(final Integer year, final String teamName) {
        NbaSeasonData ret;
        Map<String, NbaSeasonData> teamSeasonDataMap = getDatabase().get(year);
        ret = teamSeasonDataMap.get(teamName);

        log.trace("fetchByYearAndTeamName({}, {}): Returning NbaSeasonData: {}", year, teamName, ret);
        return ret;
    }
}
