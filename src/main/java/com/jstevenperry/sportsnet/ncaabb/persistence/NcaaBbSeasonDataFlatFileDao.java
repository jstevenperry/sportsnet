package com.jstevenperry.sportsnet.ncaabb.persistence;

import com.jstevenperry.sportsnet.framework.persistence.AbstractSeasonDataFlatFileDao;
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
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Component("ncaaBbSeasonDataDao")
public class NcaaBbSeasonDataFlatFileDao extends AbstractSeasonDataFlatFileDao<NcaaBbSeasonData> {

    private static final Logger log = LoggerFactory.getLogger(NcaaBbSeasonDataFlatFileDao.class);

    private static final String ALIAS_FILE_NAME = "/ncaabb/SeasonData-Alias.csv";

    @Override
    protected void loadData() {
        loadData(loadAliases());
    }

    @Override
    protected String[][] getDataFileNames() {
        return new String[][]{
                {"2010", "/ncaabb/2010/SeasonData-2010-2011.csv"},
                {"2011", "/ncaabb/2011/SeasonData-2011-2012.csv"},
                {"2012", "/ncaabb/2012/SeasonData-2012-2013.csv"},
                {"2013", "/ncaabb/2013/SeasonData-2013-2014.csv"},
                {"2014", "/ncaabb/2014/SeasonData-2014-2015.csv"},
                {"2015", "/ncaabb/2015/SeasonData-2015-2016.csv"},
                {"2016", "/ncaabb/2016/SeasonData-2016-2017.csv"},
                {"2017", "/ncaabb/2017/SeasonData-2017-2018.csv"},
                {"2018", "/ncaabb/2018/SeasonData-2018-2019.csv"}
        };
    }

    @Override
    protected int getNumberOfFields() {
        return 38;
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
    private static Map<String, String> loadAliases() {
        Map<String, String> ret = new TreeMap<>();
        // Read the alias file and build the alias map
        InputStream inputStream = NcaaBbSeasonDataFlatFileDao.class.getResourceAsStream(ALIAS_FILE_NAME);
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            CSVReader csvReader = new CSVReader(bufferedReader);
            String[] line = csvReader.readNext();
            while (line != null) {
                String teamName = line[0];
                String alias = teamName;
                if (line.length > 1) {
                    alias = line[1];
                }
                ret.put(teamName, alias);
                line = csvReader.readNext();
            }
        } catch (IOException e) {
            String message = String.format("Error reading database file name '%s': %s", ALIAS_FILE_NAME, e.getLocalizedMessage());
            log.error(message, e);
            throw new RuntimeException(message, e);
        }
        return ret;
    }

    private void loadData(final Map<String, String> aliasMap) {
        for (final String[] seasonDataEntry : getDataFileNames()) {
            Integer year = Integer.parseInt(seasonDataEntry[0]);
            String seasonDataFileName = seasonDataEntry[1];
            InputStream inputStream = NcaaBbSeasonDataFlatFileDao.class.getResourceAsStream(seasonDataFileName);
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
                CSVReader csvReader = new CSVReader(bufferedReader);
                csvReader.readNext(); // Read header
                csvReader.readNext(); // Read header
                String[] line = csvReader.readNext();
                while (line != null) {
                    if (line.length == getNumberOfFields()) {
                        NcaaBbSeasonData seasonData = parseSeasonDataFileLine(year, line, aliasMap);
                        Map<String, NcaaBbSeasonData> teamSeasonDataMap = getDatabase().computeIfAbsent(year, k -> new HashMap<>());
                        teamSeasonDataMap.put(seasonData.getTeamName(), seasonData);
                    } else {
                        throw new RuntimeException(String.format("Expected %d fields, got %d instead.", getNumberOfFields(), line.length));
                    }
                    line = csvReader.readNext();
                }
            } catch (IOException e) {
                String message = String.format("Error reading database file name '%s': %s", seasonDataFileName, e.getLocalizedMessage());
                log.error(message, e);
                throw new RuntimeException(message, e);
            }
        }
    }

    private static NcaaBbSeasonData parseSeasonDataFileLine(final Integer year, final String[] line, final Map<String, String> aliasMap) {
        NcaaBbSeasonData seasonData = new NcaaBbSeasonData();
        seasonData.setYear(year);
        seasonData.setRank(Integer.parseInt(nice(StringUtils.trim(line[0]))));
        String teamName = StringUtils.trim(StringUtils.remove(StringUtils.trim(line[1]), '*'));
        seasonData.setTeamName(fetchTeamNameAlias(teamName, aliasMap));
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

        return seasonData;
    }

    private static String fetchTeamNameAlias(final String teamName, final Map<String, String> aliasMap) {
        String ret = aliasMap.get(teamName);
        if (ret == null) {
            String message = String.format("Team name %s not found in alias map. Did you forget to add an alias?", teamName);
            log.error(message);
            throw new RuntimeException(message);
        }
        return ret;
    }

    private static String nice(final String string) {
        String ret = "0";
        if (StringUtils.isNotEmpty(string)) {
            ret = StringUtils.trim(string);
        }
        return ret;
    }

    @Override
    public List<NcaaBbSeasonData> fetchAllByYear(final Integer year) {
        Map<String, NcaaBbSeasonData> teamSeasonDataMap = getDatabase().get(year);
        List<NcaaBbSeasonData> ret = teamSeasonDataMap.keySet()
                .stream()
                .map(teamSeasonDataMap::get)
                .collect(Collectors.toList());
        log.debug(String.format("fetchAllByYear(%d): Returning %d records.", year, ret.size()));
        return ret;
    }

    @Override
    public NcaaBbSeasonData fetchByYearAndTeamName(final Integer year, final String teamName) {
        Map<String, NcaaBbSeasonData> teamSeasonDataMap = getDatabase().get(year);
        return teamSeasonDataMap.get(teamName);
    }

}
