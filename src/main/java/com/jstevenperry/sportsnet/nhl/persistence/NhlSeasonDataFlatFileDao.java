package com.jstevenperry.sportsnet.nhl.persistence;

import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.nhl.model.NhlSeasonData;
import com.opencsv.CSVReader;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component("nhlSeasonDataDao")
public class NhlSeasonDataFlatFileDao implements SeasonDataDao<NhlSeasonData> {

  private static final Logger log = LoggerFactory.getLogger(NhlSeasonDataFlatFileDao.class);

  private static final int SEASON_DATA_NUMBER_OF_FIELDS = 33;
  private static final String[][] SEASON_DATA_FILE_NAMES = {
    {"2011", "/nhl/2010-11-Summary.csv"},
    {"2012", "/nhl/2011-12-Summary.csv"},
    {"2013", "/nhl/2012-13-Summary.csv"},
    {"2014", "/nhl/2013-14-Summary.csv"},
    {"2015", "/nhl/2014-15-Summary.csv"},
    {"2016", "/nhl/2015-16-Summary.csv"},
    {"2017", "/nhl/2016-17-Summary.csv"},
    {"2018", "/nhl/2017-18-Summary.csv"},
    {"2019", "/nhl/2018-19-Summary.csv"},
    {"2020", "/nhl/2019-20-Summary.csv"}
  };

  private final Map<Integer, Map<String, NhlSeasonData>> database = new HashMap<>();


  public NhlSeasonDataFlatFileDao() {
    loadData(database);
  }

  private static void loadData(final Map<Integer, Map<String, NhlSeasonData>> database) {
    for (final String[] seasonDataEntry : SEASON_DATA_FILE_NAMES) {
      Integer year = Integer.parseInt(seasonDataEntry[0]);
      String seasonDataFileName = seasonDataEntry[1];
      InputStream inputStream = NhlSeasonDataFlatFileDao.class.getResourceAsStream(seasonDataFileName);
      try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
        CSVReader csvReader = new CSVReader(bufferedReader);
        csvReader.readNext(); // Skip header
        csvReader.readNext(); // Skip header
        String[] line = csvReader.readNext();
        while (line != null) {
          if (line.length == SEASON_DATA_NUMBER_OF_FIELDS) {
            NhlSeasonData seasonData = parseSeasonDataFileLine(year, line);
            Map<String, NhlSeasonData> teamSeasonDataMap = database.computeIfAbsent(year, k -> new HashMap<>());
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

  private static NhlSeasonData parseSeasonDataFileLine(final Integer year, final String[] line) {
    NhlSeasonData seasonData = new NhlSeasonData();
    seasonData.setYear(year);
    seasonData.setRank(Integer.parseInt(nice(StringUtils.trim(line[0]))));
    seasonData.setTeamName(StringUtils.remove(StringUtils.remove(StringUtils.trim(line[1]), '*'), '.'));
    seasonData.setAverageAge(Double.parseDouble(nice(StringUtils.trim(line[2]))));
    seasonData.setGamesPlayed(Integer.parseInt(nice(StringUtils.trim(line[3]))));
    seasonData.setWins(Integer.parseInt(nice(StringUtils.trim(line[4]))));
    seasonData.setLosses(Integer.parseInt(nice(StringUtils.trim(line[5]))));
    seasonData.setOvertimeLosses(Integer.parseInt(nice(StringUtils.trim(line[6]))));
    seasonData.setPoints(Integer.parseInt(nice(StringUtils.trim(line[7]))));
    seasonData.setPointsPct(Double.parseDouble(nice(StringUtils.trim(line[8]))));
    seasonData.setGoalsFor(Integer.parseInt(nice(StringUtils.trim(line[9]))));
    seasonData.setGoalsAgainst(Integer.parseInt(nice(StringUtils.trim(line[10]))));
    seasonData.setShootOutWins(Integer.parseInt(nice(StringUtils.trim(line[11]))));
    seasonData.setShootOutLosses(Integer.parseInt(nice(StringUtils.trim(line[12]))));
    seasonData.setSimpleRatingSystem(Double.parseDouble(nice(StringUtils.trim(line[13]))));
    seasonData.setStrengthOfSchedule(Double.parseDouble(nice(StringUtils.trim(line[14]))));
    seasonData.setGoalsPerGame(Double.parseDouble(nice(StringUtils.trim(line[15]))));
    seasonData.setEvenStrengthGoals(Integer.parseInt(nice(StringUtils.trim(line[16]))));
    seasonData.setEvenStrengthGoalsAgainst(Integer.parseInt(nice(StringUtils.trim(line[17]))));
    seasonData.setPowerPlayGoals(Integer.parseInt(nice(StringUtils.trim(line[18]))));
    seasonData.setPowerPlayOpportunities(Integer.parseInt(nice(StringUtils.trim(line[19]))));
    seasonData.setPowerPlayPct(Double.parseDouble(nice(StringUtils.trim(line[20]))));
    seasonData.setPowerPlayGoalsAgainst(Integer.parseInt(nice(StringUtils.trim(line[21]))));
    seasonData.setPowerPlayOpportunitiesAgainst(Integer.parseInt(nice(StringUtils.trim(line[22]))));
    seasonData.setPenaltyKillPct(Double.parseDouble(nice(StringUtils.trim(line[23]))));
    seasonData.setShortHandedGoals(Integer.parseInt(nice(StringUtils.trim(line[24]))));
    seasonData.setShortHandedGoalsAgainst(Integer.parseInt(nice(StringUtils.trim(line[25]))));
    seasonData.setPenaltyMinutesPerGame(Double.parseDouble(nice(StringUtils.trim(line[26]))));
    seasonData.setOpponentPenaltyMinutesPerGame(Double.parseDouble(nice(StringUtils.trim(line[27]))));
    seasonData.setShotsOnGoal(Integer.parseInt(nice(StringUtils.trim(line[28]))));
    seasonData.setShootingPct(Double.parseDouble(nice(StringUtils.trim(line[29]))));
    seasonData.setShotsAgainst(Integer.parseInt(nice(StringUtils.trim(line[30]))));
    seasonData.setSavePct(Double.parseDouble(nice(StringUtils.trim(line[31]))));
    seasonData.setShutouts(Integer.parseInt(nice(StringUtils.trim(line[32]))));

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
  public List<NhlSeasonData> fetchAllByYear(final Integer year) {
    Map<String, NhlSeasonData> teamSeasonDataMap = database.get(year);
    List<NhlSeasonData> ret = teamSeasonDataMap.keySet()
      .stream()
      .map(teamSeasonDataMap::get)
      .collect(Collectors.toList());
    log.debug(String.format("fetchAllByYear(%d): Returning %d records.", year, ret.size()));
    return ret;
  }

  @Override
  public NhlSeasonData fetchByYearAndTeamName(final Integer year, final String teamName) {
    Map<String, NhlSeasonData> teamSeasonDataMap = database.get(year);
    return teamSeasonDataMap.get(teamName);
  }

}
