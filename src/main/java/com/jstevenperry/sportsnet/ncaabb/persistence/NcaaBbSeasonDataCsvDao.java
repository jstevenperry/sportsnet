/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet.ncaabb.persistence;

import com.jstevenperry.sportsnet.framework.exception.InternalServerErrorException;
import com.jstevenperry.sportsnet.framework.exception.NotFoundException;
import com.jstevenperry.sportsnet.framework.model.ResourceType;
import com.jstevenperry.sportsnet.framework.persistence.AbstractSeasonDataCsvDao;
import com.jstevenperry.sportsnet.framework.persistence.FileProcessor;
import com.jstevenperry.sportsnet.framework.util.AbstractNetworkUtils;
import com.jstevenperry.sportsnet.ncaabb.config.SportsnetProperties;
import com.jstevenperry.sportsnet.ncaabb.model.NcaaBbSeasonData;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class NcaaBbSeasonDataCsvDao extends AbstractSeasonDataCsvDao<NcaaBbSeasonData> {

    private static final Logger log = LoggerFactory.getLogger(NcaaBbSeasonDataCsvDao.class);

    public static final String ERROR_PREFIX = "NCAABBSDD";

    private static final String ALIAS_FILE_NAME = "SeasonData-Alias.csv";
    private final AbstractNetworkUtils networkUtils;
    private final SportsnetProperties sportsnetProperties;

    public NcaaBbSeasonDataCsvDao(@Qualifier("ncaaBbNetworkUtils") final AbstractNetworkUtils networkUtils,
                                  final SportsnetProperties sportsnetProperties) {
        super(networkUtils);
        this.networkUtils = networkUtils;
        this.sportsnetProperties = sportsnetProperties;
    }

    @Override
    protected Map<String, NcaaBbSeasonData> loadData(final Map<Integer, Map<String, NcaaBbSeasonData>> database,
                                                     final Integer year) {
        return loadData(database, year, loadAliases());
    }

    @Override
    protected String[][] getDataFileNames() {
        return new String[][]{
                {"2009", "BasicSeasonData.csv", "OpponentSeasonData.csv", "AdvancedSeasonData.csv", "AdvancedOpponentSeasonData.csv"},
                {"2010", "BasicSeasonData.csv", "OpponentSeasonData.csv", "AdvancedSeasonData.csv", "AdvancedOpponentSeasonData.csv"},
                {"2011", "BasicSeasonData.csv", "OpponentSeasonData.csv", "AdvancedSeasonData.csv", "AdvancedOpponentSeasonData.csv"},
                {"2012", "BasicSeasonData.csv", "OpponentSeasonData.csv", "AdvancedSeasonData.csv", "AdvancedOpponentSeasonData.csv"},
                {"2013", "BasicSeasonData.csv", "OpponentSeasonData.csv", "AdvancedSeasonData.csv", "AdvancedOpponentSeasonData.csv"},
                {"2014", "BasicSeasonData.csv", "OpponentSeasonData.csv", "AdvancedSeasonData.csv", "AdvancedOpponentSeasonData.csv"},
                {"2015", "BasicSeasonData.csv", "OpponentSeasonData.csv", "AdvancedSeasonData.csv", "AdvancedOpponentSeasonData.csv"},
                {"2016", "BasicSeasonData.csv", "OpponentSeasonData.csv", "AdvancedSeasonData.csv", "AdvancedOpponentSeasonData.csv"},
                {"2017", "BasicSeasonData.csv", "OpponentSeasonData.csv", "AdvancedSeasonData.csv", "AdvancedOpponentSeasonData.csv"},
                {"2018", "BasicSeasonData.csv", "OpponentSeasonData.csv", "AdvancedSeasonData.csv", "AdvancedOpponentSeasonData.csv"},
                // No 2019-20 Tournament (COVID-19)
                {"2020", "BasicSeasonData.csv", "OpponentSeasonData.csv", "AdvancedSeasonData.csv", "AdvancedOpponentSeasonData.csv"},
                {"2021", "BasicSeasonData.csv", "OpponentSeasonData.csv", "AdvancedSeasonData.csv", "AdvancedOpponentSeasonData.csv"},
                {"2022", "BasicSeasonData.csv", "OpponentSeasonData.csv", "AdvancedSeasonData.csv", "AdvancedOpponentSeasonData.csv"},
                {"2023", "BasicSeasonData.csv", "OpponentSeasonData.csv", "AdvancedSeasonData.csv", "AdvancedOpponentSeasonData.csv"},
                {"2024", "BasicSeasonData.csv", "OpponentSeasonData.csv", "AdvancedSeasonData.csv", "AdvancedOpponentSeasonData.csv"},
        };
    }

    @Override
    protected int getNumberOfFields() {
        throw new UnsupportedOperationException("getNumberOfFields() is not implemented. Reason = not appropriate for this class.");
    }

    /**
     * Load the aliases. The team names vary depending on the source,
     * and they don't always match the names in the tournament results
     * file (or even with themselves from the same source, Auburn and Auburn NCAA, e.g.).
     * The aliases file contains an entry for each name in the source data
     * to the name that matches it in the tournament results.
     * <p>
     * If no alias is given, the team name is good as is. If there is an alias
     * for it, it will be after the comma.
     * <p>
     * Examples:
     * Auburn
     * Auburn NCAA,Auburn
     */
    private Map<String, List<String>> loadAliases() {
        Map<String, List<String>> aliasMap = new TreeMap<>();
        // Read the alias file and build the alias map
        final String aliasFileUrl = networkUtils.computeDataFileUrl(sportsnetProperties, sportsnetProperties.getSportsnetNcaabbUri(), ALIAS_FILE_NAME, ResourceType.CSV_METADATA_FILE);
        final Optional<String[][]> aliasFileContentsOptional = fetchCsvData(aliasFileUrl);
        if (aliasFileContentsOptional.isPresent()) {
            String[][] aliasFileContents = aliasFileContentsOptional.get();
            int skipCount = 0;
            for (String[] line : aliasFileContents) {
                if (skipCount++ < 1) {
                    continue;
                }
                String teamName = line[0];
                // Skip comments
                if (teamName.startsWith("#")) {
                    continue;
                }
                List<String> aliases = aliasMap.computeIfAbsent(teamName, k -> new ArrayList<>());
                String alias = teamName;
                if (line.length > 1) {
                    alias = line[1];
                }
                aliases.add(alias);
                log.debug("loadAliases(): Storing k/v: {}/{}", teamName, alias);
            }
        } else {
            String errorMessage = String.format("%s0011E: The specified file could not be found on the resource server: %s", ERROR_PREFIX, aliasFileUrl);
            log.error("loadAliases(): {}", errorMessage);
            throw new NotFoundException(errorMessage);
        }
        return aliasMap;
    }

    private Map<String, NcaaBbSeasonData> loadData(final Map<Integer, Map<String, NcaaBbSeasonData>> database,
                                                   final Integer year,
                                                   final Map<String, List<String>> aliasMap) {
        Map<String, NcaaBbSeasonData> seasonDataMap = null;
        for (final String[] seasonDataEntry : getDataFileNames()) {
            Integer yearFromFile = Integer.parseInt(seasonDataEntry[0]);
            if (year.equals(yearFromFile)) {
                for (int seasonDataFileIndex = 1; seasonDataFileIndex < seasonDataEntry.length; seasonDataFileIndex++) {
                    String seasonDataFileName = seasonDataEntry[seasonDataFileIndex];
                    final String seasonDataFileUrl = networkUtils.computeDataFileUrl(
                            sportsnetProperties, sportsnetProperties.getSportsnetNcaabbUri(), year, seasonDataFileName, ResourceType.SEASON_DATA_FILE);
                    seasonDataMap = database.computeIfAbsent(year, k -> new ConcurrentHashMap<>());
                    switch (seasonDataFileIndex) {
                        case 1:
                            processBasicSeasonDataFile(year, seasonDataFileUrl, seasonDataMap);
                            break;
                        case 2:
                            processOpponentSeasonDataFile(year, seasonDataFileUrl, seasonDataMap);
                            break;
                        case 3:
                            processAdvancedSeasonDataFile(year, seasonDataFileUrl, seasonDataMap);
                            break;
                        case 4:
                            processAdvancedOpponentSeasonDataFile(year, seasonDataFileUrl, seasonDataMap);
                            break;
                        default:
                            String errorMessage = String.format("%s0101E: Unknown file index %d (did you add some files but forget to add code in loadData()?)",
                                    ERROR_PREFIX, seasonDataFileIndex);
                            log.error(errorMessage);
                            throw new InternalServerErrorException(errorMessage);
                    }
                }
                createAliasesForYear(year, database, aliasMap);
                break;
            }
        }
        return seasonDataMap;
    }

    private void processBasicSeasonDataFile(final Integer year, final String seasonDataFileUrl, final Map<String, NcaaBbSeasonData> seasonDataMap) {
        processSeasonDataFile(year, seasonDataFileUrl, seasonDataMap, new FileProcessor<>() {
            @Override
            public String getRequiredFilenameEnding() {
                return String.format("BasicSeasonData.csv?resourceType=SEASON_DATA_FILE&year=%d", year);
            }

            @Override
            public int getNumberOfFields() {
                return 38;
            }

            @Override
            public int getNumberOfHeaderLines() {
                return 2;
            }

            @Override
            public NcaaBbSeasonData parseAndProcessFileLine(final String[] line, final Map<String, NcaaBbSeasonData> teamSeasonDataMap) {
                return parseBasicSeasonDataFileLine(line, teamSeasonDataMap);
            }
        });
    }

    private NcaaBbSeasonData parseBasicSeasonDataFileLine(final String[] line, Map<String, NcaaBbSeasonData> seasonDataMap) {
        String teamName = StringUtils.remove(StringUtils.trim(line[1]), '*');
        NcaaBbSeasonData seasonData = seasonDataComputeIfAbsent(teamName, seasonDataMap, newThing -> new NcaaBbSeasonData());
        seasonData.setRank(Integer.parseInt(nice(StringUtils.trim(line[0]))));
        seasonData.setGamesPlayed(Integer.parseInt(nice(StringUtils.trim(line[2]))));
        seasonData.setGamesWon(Integer.parseInt(nice(StringUtils.trim(line[3]))));
        seasonData.setGamesLost(Integer.parseInt(nice(StringUtils.trim(line[4]))));
        seasonData.setWonLostPercentage(BigDecimal.valueOf(Double.parseDouble(nice(StringUtils.trim(line[5])))));
        seasonData.setSimpleRatingSystem(Double.parseDouble(nice(StringUtils.trim(line[6]))));
        seasonData.setStrengthOfSchedule(Double.parseDouble(nice(StringUtils.trim(line[7]))));
        // Index 8 - empty cell
        seasonData.setConferenceWins(Integer.parseInt(nice(StringUtils.trim(line[9]))));
        seasonData.setConferenceLosses(Integer.parseInt(nice(StringUtils.trim(line[10]))));
        // Index 11 - empty cell
        seasonData.setHomeWins(Integer.parseInt(nice(StringUtils.trim(line[12]))));
        seasonData.setHomeLosses(Integer.parseInt(nice(StringUtils.trim(line[13]))));
        // Index 14 - empty cell
        seasonData.setAwayWins(Integer.parseInt(nice(StringUtils.trim(line[15]))));
        seasonData.setAwayLosses(Integer.parseInt(nice(StringUtils.trim(line[16]))));
        // Index 17 - empty cell
        seasonData.setPointsScored(Integer.parseInt(nice(StringUtils.trim(line[18]))));
        seasonData.setOpponentPointsScored(Integer.parseInt(nice(StringUtils.trim(line[19]))));
        // Index 20 - empty cell
        seasonData.setMinutesPlayed(Integer.parseInt(nice(StringUtils.trim(line[21]))));
        seasonData.setFieldGoals(Integer.parseInt(nice(StringUtils.trim(line[22]))));
        seasonData.setFieldGoalAttempts(Integer.parseInt(nice(StringUtils.trim(line[23]))));
        seasonData.setFieldGoalPct(Double.parseDouble(nice(StringUtils.trim(line[24]))));
        seasonData.setThreePointers(Integer.parseInt(nice(StringUtils.trim(line[25]))));
        seasonData.setThreePointAttempts(Integer.parseInt(nice(StringUtils.trim(line[26]))));
        seasonData.setThreePointPct(Double.parseDouble(nice(StringUtils.trim(line[27]))));
        seasonData.setFreeThrows(Integer.parseInt(nice(StringUtils.trim(line[28]))));
        seasonData.setFreeThrowAttempts(Integer.parseInt(nice(StringUtils.trim(line[29]))));
        seasonData.setFreeThrowPct(Double.parseDouble(nice(StringUtils.trim(line[30]))));
        seasonData.setOffensiveRebounds(Integer.parseInt(nice(StringUtils.trim(line[31]))));
        seasonData.setTotalRebounds(Integer.parseInt(nice(StringUtils.trim(line[32]))));
        seasonData.setAssists(Integer.parseInt(nice(StringUtils.trim(line[33]))));
        seasonData.setSteals(Integer.parseInt(nice(StringUtils.trim(line[34]))));
        seasonData.setBlocks(Integer.parseInt(nice(StringUtils.trim(line[35]))));
        seasonData.setTurnovers(Integer.parseInt(nice(StringUtils.trim(line[36]))));
        seasonData.setPersonalFouls(Integer.parseInt(nice(StringUtils.trim(line[37]))));

        log.debug("parseBasicSeasonDataFileLine(): Processed line for team {}", seasonData.getTeamName());
        return seasonData;
    }

    private void processOpponentSeasonDataFile(final Integer year, final String seasonDataFileUrl, final Map<String, NcaaBbSeasonData> seasonDataMap) {
        processSeasonDataFile(year, seasonDataFileUrl, seasonDataMap, new FileProcessor<>() {
            @Override
            public String getRequiredFilenameEnding() {
                return String.format("OpponentSeasonData.csv?resourceType=SEASON_DATA_FILE&year=%d", year);
            }

            @Override
            public int getNumberOfFields() {
                return 38;
            }

            @Override
            public int getNumberOfHeaderLines() {
                return 2;
            }

            @Override
            public NcaaBbSeasonData parseAndProcessFileLine(final String[] line, final Map<String, NcaaBbSeasonData> teamSeasonDataMap) {
                return parseOpponentDataFileLine(line, teamSeasonDataMap);
            }
        });
    }

    private NcaaBbSeasonData parseOpponentDataFileLine(final String[] line, final Map<String, NcaaBbSeasonData> teamSeasonDataMap) {
        String teamName = StringUtils.remove(StringUtils.trim(line[1]), '*');
        NcaaBbSeasonData seasonData = seasonDataComputeIfAbsent(teamName, teamSeasonDataMap, x -> new NcaaBbSeasonData());

        seasonData.setOpponentFieldGoals(Integer.parseInt(nice(StringUtils.trim(line[22]))));
        seasonData.setOpponentFieldGoalAttempts(Integer.parseInt(nice(StringUtils.trim(line[23]))));
        seasonData.setOpponentFieldGoalPct(Double.parseDouble(nice(StringUtils.trim(line[24]))));
        seasonData.setOpponentThreePointers(Integer.parseInt(nice(StringUtils.trim(line[25]))));
        seasonData.setOpponentThreePointAttempts(Integer.parseInt(nice(StringUtils.trim(line[26]))));
        seasonData.setOpponentThreePointPct(Double.parseDouble(nice(StringUtils.trim(line[27]))));
        seasonData.setOpponentFreeThrows(Integer.parseInt(nice(StringUtils.trim(line[28]))));
        seasonData.setOpponentFreeThrowAttempts(Integer.parseInt(nice(StringUtils.trim(line[29]))));
        seasonData.setOpponentFreeThrowPct(Double.parseDouble(nice(StringUtils.trim(line[30]))));
        seasonData.setOpponentOffensiveRebounds(Integer.parseInt(nice(StringUtils.trim(line[31]))));
        seasonData.setOpponentTotalRebounds(Integer.parseInt(nice(StringUtils.trim(line[32]))));
        seasonData.setOpponentAssists(Integer.parseInt(nice(StringUtils.trim(line[33]))));
        seasonData.setOpponentSteals(Integer.parseInt(nice(StringUtils.trim(line[34]))));
        seasonData.setOpponentBlocks(Integer.parseInt(nice(StringUtils.trim(line[35]))));
        seasonData.setOpponentTurnovers(Integer.parseInt(nice(StringUtils.trim(line[36]))));
        seasonData.setOpponentPersonalFouls(Integer.parseInt(nice(StringUtils.trim(line[37]))));

        return seasonData;
    }

    private void processAdvancedSeasonDataFile(final Integer year, final String seasonDataFileUrl, final Map<String, NcaaBbSeasonData> seasonDataMap) {
        processSeasonDataFile(year, seasonDataFileUrl, seasonDataMap, new FileProcessor<>() {
            @Override
            public String getRequiredFilenameEnding() {
                return String.format("AdvancedSeasonData.csv?resourceType=SEASON_DATA_FILE&year=%d", year);
            }

            @Override
            public int getNumberOfFields() {
                return 34;
            }

            @Override
            public int getNumberOfHeaderLines() {
                return 2;
            }

            @Override
            public NcaaBbSeasonData parseAndProcessFileLine(final String[] line, final Map<String, NcaaBbSeasonData> teamSeasonDataMap) {
                return parseAdvancedSeasonDataFileLine(line, teamSeasonDataMap);
            }
        });
    }

    private NcaaBbSeasonData parseAdvancedSeasonDataFileLine(final String[] line, final Map<String, NcaaBbSeasonData> teamSeasonDataMap) {
        String teamName = StringUtils.remove(StringUtils.trim(line[1]), '*');
        NcaaBbSeasonData seasonData = seasonDataComputeIfAbsent(teamName, teamSeasonDataMap, x -> new NcaaBbSeasonData());

        seasonData.setPace(Double.parseDouble(nice(StringUtils.trim(line[21]))));//38
        seasonData.setOffensiveRating(Double.parseDouble(nice(StringUtils.trim(line[22]))));
        seasonData.setFreeThrowAttemptRate(Double.parseDouble(nice(StringUtils.trim(line[23]))));//40
        seasonData.setThreePointAttemptRate(Double.parseDouble(nice(StringUtils.trim(line[24]))));
        seasonData.setTrueShootingPct(Double.parseDouble(nice(StringUtils.trim(line[25]))));
        seasonData.setTotalReboundPct(Double.parseDouble(nice(StringUtils.trim(line[26]))));
        seasonData.setAssistPct(Double.parseDouble(nice(StringUtils.trim(line[27]))));
        seasonData.setStealPct(Double.parseDouble(nice(StringUtils.trim(line[28]))));
        seasonData.setBlockPct(Double.parseDouble(nice(StringUtils.trim(line[29]))));
        seasonData.setEffectiveFgPct(Double.parseDouble(nice(StringUtils.trim(line[30]))));
        seasonData.setTurnoverPct(Double.parseDouble(nice(StringUtils.trim(line[31]))));
        seasonData.setOffensiveReboundPct(Double.parseDouble(nice(StringUtils.trim(line[32]))));
        seasonData.setFreeThrowsPerFgAttempt(Double.parseDouble(nice(StringUtils.trim(line[33]))));//50

        return seasonData;
    }

    private void processAdvancedOpponentSeasonDataFile(final Integer year, final String seasonDataFileUrl, final Map<String, NcaaBbSeasonData> seasonDataMap) {
        processSeasonDataFile(year, seasonDataFileUrl, seasonDataMap, new FileProcessor<>() {
            @Override
            public String getRequiredFilenameEnding() {
                return String.format("AdvancedOpponentSeasonData.csv?resourceType=SEASON_DATA_FILE&year=%d", year);
            }

            @Override
            public int getNumberOfFields() {
                return 34;
            }

            @Override
            public int getNumberOfHeaderLines() {
                return 2;
            }

            @Override
            public NcaaBbSeasonData parseAndProcessFileLine(final String[] line, final Map<String, NcaaBbSeasonData> teamSeasonDataMap) {
                return parseAdvancedOpponentSeasonDataFileLine(line, teamSeasonDataMap);
            }
        });
    }

    private NcaaBbSeasonData parseAdvancedOpponentSeasonDataFileLine(final String[] line, final Map<String, NcaaBbSeasonData> teamSeasonDataMap) {
        String teamName = StringUtils.remove(StringUtils.trim(line[1]), '*');
        NcaaBbSeasonData seasonData = seasonDataComputeIfAbsent(teamName, teamSeasonDataMap, x -> new NcaaBbSeasonData());

        seasonData.setOpponentPace(Double.parseDouble(nice(StringUtils.trim(line[21]))));//38
        seasonData.setOpponentOffensiveRating(Double.parseDouble(nice(StringUtils.trim(line[22]))));
        seasonData.setOpponentFreeThrowAttemptRate(Double.parseDouble(nice(StringUtils.trim(line[23]))));//40
        seasonData.setOpponentThreePointAttemptRate(Double.parseDouble(nice(StringUtils.trim(line[24]))));
        seasonData.setOpponentTrueShootingPct(Double.parseDouble(nice(StringUtils.trim(line[25]))));
        seasonData.setOpponentTotalReboundPct(Double.parseDouble(nice(StringUtils.trim(line[26]))));
        seasonData.setOpponentAssistPct(Double.parseDouble(nice(StringUtils.trim(line[27]))));
        seasonData.setOpponentStealPct(Double.parseDouble(nice(StringUtils.trim(line[28]))));
        seasonData.setOpponentBlockPct(Double.parseDouble(nice(StringUtils.trim(line[29]))));
        seasonData.setOpponentEffectiveFgPct(Double.parseDouble(nice(StringUtils.trim(line[30]))));
        seasonData.setOpponentTurnoverPct(Double.parseDouble(nice(StringUtils.trim(line[31]))));
        seasonData.setOpponentOffensiveReboundPct(Double.parseDouble(nice(StringUtils.trim(line[32]))));
        seasonData.setOpponentFreeThrowsPerFgAttempt(Double.parseDouble(nice(StringUtils.trim(line[33]))));//50

        return seasonData;
    }

    private void createAliasesForYear(final Integer year,
                                      final Map<Integer, Map<String, NcaaBbSeasonData>> database,
                                      final Map<String, List<String>> aliasMap) {
        Map<String, NcaaBbSeasonData> seasonDataMap = database.get(year);
        // Get team's season data
        List<String> teamNamesSorted = new ArrayList<>(seasonDataMap.keySet());
        Collections.sort(teamNamesSorted);
        for (String teamName : teamNamesSorted) {
            NcaaBbSeasonData seasonData = seasonDataMap.get(teamName);
            List<String> aliases = aliasMap.get(teamName);
            if (aliases != null) {
                for (String alias : aliases) {
                    seasonDataMap.put(alias, seasonData);
                    log.debug("createAliasesForYear({}): Stored season data for {} under alias {}", year, teamName, alias);
                }
            }
        }
        // Add alias as key for that data
    }

    @Override
    public List<NcaaBbSeasonData> fetchAllByYear(final Integer year) {
        Map<String, NcaaBbSeasonData> teamSeasonDataMap = getSeasonDataMap(year);
        List<NcaaBbSeasonData> ret = teamSeasonDataMap.keySet()
                .stream()
                .map(teamSeasonDataMap::get)
                .collect(Collectors.toList());
        log.debug(String.format("fetchAllByYear(%d): Returning %d records.", year, ret.size()));
        return ret;
    }

    @Override
    public NcaaBbSeasonData fetchByYearAndTeamName(final Integer year, final String teamName) {
        Map<String, NcaaBbSeasonData> teamSeasonDataMap = getSeasonDataMap(year);
        return teamSeasonDataMap.get(teamName);
    }

    @Override
    public Integer getNumberOfInputs() {
        return new NcaaBbSeasonData().getNumberOfFields();
    }

}