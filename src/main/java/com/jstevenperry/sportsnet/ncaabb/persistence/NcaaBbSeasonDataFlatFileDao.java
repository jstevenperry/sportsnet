package com.jstevenperry.sportsnet.ncaabb.persistence;

import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
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
import java.util.stream.Collectors;

@Component("ncaaBbSeasonDataDao")
public class NcaaBbSeasonDataFlatFileDao implements SeasonDataDao<NcaaBbSeasonData> {

  private static final Logger log = LoggerFactory.getLogger(NcaaBbSeasonDataFlatFileDao.class);

  private static final int SEASON_DATA_NUMBER_OF_FIELDS = 38;
  private static final String[][] SEASON_DATA_FILE_NAMES =  {
    {"2011", "/ncaabb/SeasonData-2010-2011.csv"},
    {"2012", "/ncaabb/SeasonData-2011-2012.csv"},
    {"2013", "/ncaabb/SeasonData-2012-2013.csv"},
    {"2014", "/ncaabb/SeasonData-2013-2014.csv"},
    {"2015", "/ncaabb/SeasonData-2014-2015.csv"},
    {"2016", "/ncaabb/SeasonData-2015-2016.csv"},
    {"2017", "/ncaabb/SeasonData-2016-2017.csv"},
    {"2018", "/ncaabb/SeasonData-2017-2018.csv"},
    {"2019", "/ncaabb/SeasonData-2018-2019.csv"},
    {"2020", "/ncaabb/SeasonData-2019-2020.csv"}
  };

  private final Map<Integer, Map<String, NcaaBbSeasonData>> database = new HashMap<>();


  public NcaaBbSeasonDataFlatFileDao() {
    loadData(database);
  }

  private static void loadData(final Map<Integer, Map<String, NcaaBbSeasonData>> database) {
    for (final String[] seasonDataEntry : SEASON_DATA_FILE_NAMES) {
      Integer year = Integer.parseInt(seasonDataEntry[0]);
      String seasonDataFileName = seasonDataEntry[1];
      InputStream inputStream = NcaaBbSeasonDataFlatFileDao.class.getResourceAsStream(seasonDataFileName);
      try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
        CSVReader csvReader = new CSVReader(bufferedReader);
        csvReader.readNext(); // Read header
        csvReader.readNext(); // Read header
        String[] line = csvReader.readNext();
        while (line != null) {
          if (line.length == SEASON_DATA_NUMBER_OF_FIELDS) {
            NcaaBbSeasonData seasonData = parseSeasonDataFileLine(year, line);
            Map<String, NcaaBbSeasonData> teamSeasonDataMap = database.computeIfAbsent(year, k -> new HashMap<>());
            teamSeasonDataMap.put(seasonData.getTeamName(), seasonData);
          } else {
            throw new RuntimeException(String.format("Expected %d fields, got %d instead.", SEASON_DATA_NUMBER_OF_FIELDS, line.length));
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

  private static NcaaBbSeasonData parseSeasonDataFileLine(final Integer year, final String[] line) {
    NcaaBbSeasonData seasonData = new NcaaBbSeasonData();
    seasonData.setYear(year);
    seasonData.setRank(Integer.parseInt(nice(StringUtils.trim(line[0]))));
    seasonData.setTeamName(StringUtils.trim(StringUtils.remove(StringUtils.remove(StringUtils.remove(StringUtils.trim(line[1]), '*'), '.'), "NCAA")));
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

  private static String nice(final String string) {
    String ret = "0";
    if (StringUtils.isNotEmpty(string)) {
      ret = StringUtils.trim(string);
    }
    return ret;
  }

  @Override
  public List<NcaaBbSeasonData> fetchAllByYear(final Integer year) {
    Map<String, NcaaBbSeasonData> teamSeasonDataMap = database.get(year);
    List<NcaaBbSeasonData> ret = teamSeasonDataMap.keySet()
      .stream()
      .map(teamSeasonDataMap::get)
      .collect(Collectors.toList());
    log.debug(String.format("fetchAllByYear(%d): Returning %d records.", year, ret.size()));
    return ret;
  }

  @Override
  public NcaaBbSeasonData fetchByYearAndTeamName(final Integer year, final String teamName) {
    Map<String, NcaaBbSeasonData> teamSeasonDataMap = database.get(year);
    return teamSeasonDataMap.get(teamName);
  }

}
