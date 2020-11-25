package com.jstevenperry.sportsnet.nfl.persistence;

import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.nfl.model.NflSeasonData;
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
import java.util.StringTokenizer;

import static com.jstevenperry.sportsnet.framework.util.NetworkUtils.nice;

@Component("nflSeasonDataDao")
public class NflSeasonDataFlatFileDao implements SeasonDataDao<NflSeasonData> {

  private static final Logger log = LoggerFactory.getLogger(NflSeasonDataFlatFileDao.class);

  private static final int SEASON_DATA_NUMBER_OF_FIELDS = 93;
  private static final String[][] SEASON_DATA_FILE_NAMES = {
    {"2012",
      "/nfl/2012/2012-Conversions.csv", "/nfl/2012/2012-Drive-Averages.csv", "/nfl/2012/2012-Kick&PuntReturns",
      "/nfl/2012/2012-Kicking&Punting.csv", "/nfl/2012/2012-Passing-Offense.csv", "/nfl/2012/2012-RushingOffense.csv",
      "/nfl/2012/2012-Scoring-Offense.csv", "/nfl/2012/2012-Team-Offense.csv"
    },
    {"2013",
      "/nfl/2013/2013-Conversions.csv", "/nfl/2013/2013-Drive-Averages.csv", "/nfl/2013/2013-Kick&PuntReturns",
      "/nfl/2013/2013-Kicking&Punting.csv", "/nfl/2013/2013-Passing-Offense.csv", "/nfl/2013/2013-RushingOffense.csv",
      "/nfl/2013/2013-Scoring-Offense.csv", "/nfl/2013/2013-Team-Offense.csv"
    },
    {"2014",
      "/nfl/2014/2014-Conversions.csv", "/nfl/2014/2014-Drive-Averages.csv", "/nfl/2014/2014-Kick&PuntReturns",
      "/nfl/2014/2014-Kicking&Punting.csv", "/nfl/2014/2014-Passing-Offense.csv", "/nfl/2014/2014-RushingOffense.csv",
      "/nfl/2014/2014-Scoring-Offense.csv", "/nfl/2014/2014-Team-Offense.csv"
    },
    {"2015",
      "/nfl/2015/2015-Conversions.csv", "/nfl/2015/2015-Drive-Averages.csv", "/nfl/2015/2015-Kick&PuntReturns",
      "/nfl/2015/2015-Kicking&Punting.csv", "/nfl/2015/2015-Passing-Offense.csv", "/nfl/2015/2015-RushingOffense.csv",
      "/nfl/2015/2015-Scoring-Offense.csv", "/nfl/2015/2015-Team-Offense.csv"
    },
    {"2016",
      "/nfl/2016/2016-Conversions.csv", "/nfl/2016/2016-Drive-Averages.csv", "/nfl/2016/2016-Kick&PuntReturns",
      "/nfl/2016/2016-Kicking&Punting.csv", "/nfl/2016/2016-Passing-Offense.csv", "/nfl/2016/2016-RushingOffense.csv",
      "/nfl/2016/2016-Scoring-Offense.csv", "/nfl/2016/2016-Team-Offense.csv"
    },
    {"2017",
      "/nfl/2017/2017-Conversions.csv", "/nfl/2017/2017-Drive-Averages.csv", "/nfl/2017/2017-Kick&PuntReturns",
      "/nfl/2017/2017-Kicking&Punting.csv", "/nfl/2017/2017-Passing-Offense.csv", "/nfl/2017/2017-RushingOffense.csv",
      "/nfl/2017/2017-Scoring-Offense.csv", "/nfl/2017/2017-Team-Offense.csv"
    },
    {"2018",
      "/nfl/2018/2018-Conversions.csv", "/nfl/2018/2018-Drive-Averages.csv", "/nfl/2018/2018-Kick&PuntReturns",
      "/nfl/2018/2018-Kicking&Punting.csv", "/nfl/2018/2018-Passing-Offense.csv", "/nfl/2018/2018-RushingOffense.csv",
      "/nfl/2018/2018-Scoring-Offense.csv", "/nfl/2018/2018-Team-Offense.csv"
    },
    {"2019",
      "/nfl/2019/2019-Conversions.csv", "/nfl/2019/2019-Drive-Averages.csv", "/nfl/2019/2019-Kick&PuntReturns",
      "/nfl/2019/2019-Kicking&Punting.csv", "/nfl/2019/2019-Passing-Offense.csv", "/nfl/2019/2019-RushingOffense.csv",
      "/nfl/2019/2019-Scoring-Offense.csv", "/nfl/2019/2019-Team-Offense.csv"
    }
  };

  private final Map<Integer, Map<String, NflSeasonData>> database = new HashMap<>();

  public NflSeasonDataFlatFileDao() {
    loadData(database);
  }

  private static void loadData(final Map<Integer, Map<String, NflSeasonData>> database) {
    for (final String[] seasonDataEntry : SEASON_DATA_FILE_NAMES) {
      Integer year = Integer.parseInt(seasonDataEntry[0]);
      for (int seasonDataFileIndex = 1; seasonDataFileIndex < seasonDataEntry.length; seasonDataFileIndex++) {
        String seasonDataFileName = seasonDataEntry[seasonDataFileIndex];
        Map<String, NflSeasonData> teamSeasonDataMap = database.computeIfAbsent(year, k -> new HashMap<>());
        switch (seasonDataFileIndex) {
          case 1:
            processConversionsFile(year, seasonDataFileName, teamSeasonDataMap);
            break;
          case 2:
            processDriveAveragesFile(year, seasonDataFileName, teamSeasonDataMap);
            break;
          case 3:
            processKickAndPuntReturnsFile(year, seasonDataFileName, teamSeasonDataMap);
            break;
          case 4:
            processKickingAndPuntingFile(year, seasonDataFileName, teamSeasonDataMap);
            break;
          case 5:
            processPassingOffenseFile(year, seasonDataFileName, teamSeasonDataMap);
            break;
          case 6:
            processRushingOffenseFile(year, seasonDataFileName, teamSeasonDataMap);
            break;
          case 7:
            processScoringOffenseFile(year, seasonDataFileName, teamSeasonDataMap);
            break;
          case 8:
            processTeamOffenseFile(year, seasonDataFileName, teamSeasonDataMap);
            break;
        }
      }
    }
  }

  private static void processTeamOffenseFile(final Integer year, final String seasonDataFileName, final Map<String, NflSeasonData> teamSeasonDataMap) {
  }

  private static void processScoringOffenseFile(final Integer year, final String seasonDataFileName, final Map<String, NflSeasonData> teamSeasonDataMap) {
  }

  private static void processRushingOffenseFile(final Integer year, final String seasonDataFileName, final Map<String, NflSeasonData> teamSeasonDataMap) {
  }

  private static void processPassingOffenseFile(final Integer year, final String seasonDataFileName, final Map<String, NflSeasonData> teamSeasonDataMap) {
  }

  private static void processKickingAndPuntingFile(final Integer year, final String seasonDataFileName, final Map<String, NflSeasonData> teamSeasonDataMap) {
  }

  private static void processKickAndPuntReturnsFile(final Integer year, final String seasonDataFileName, final Map<String, NflSeasonData> teamSeasonDataMap) {
  }

  private static void processDriveAveragesFile(final Integer year,
                                               final String seasonDataFileName,
                                               final Map<String, NflSeasonData> teamSeasonDataMap) {
    final String REQUIRED_ENDING = "Drive-Averages.csv";
    final int NUMBER_OF_FIELDS = 12;
    if (seasonDataFileName.endsWith(REQUIRED_ENDING)) { // Sanity check
      InputStream inputStream = NflSeasonDataFlatFileDao.class.getResourceAsStream(seasonDataFileName);
      try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
        CSVReader csvReader = new CSVReader(bufferedReader);
        csvReader.readNext(); // Skip header
        csvReader.readNext(); // Skip header
        String[] line = csvReader.readNext();
        while (line != null) {
          if (line.length == NUMBER_OF_FIELDS) {
            NflSeasonData seasonData = parseAndProcessDriveAveragesFileLine(line, teamSeasonDataMap);
            seasonData.setYear(year);
          } else {
            throw new RuntimeException(String.format("processDriveAveragesFile: Expected %d fields, got %d instead.", NUMBER_OF_FIELDS, line.length));
          }
          line = csvReader.readNext();
        }
      } catch (IOException e) {
        String message = String.format("processDriveAveragesFile: Error reading database file name '%s': %s", seasonDataFileName, e.getLocalizedMessage());
        log.error(message, e);
        throw new RuntimeException(message, e);
      }
    } else {
      String errorMessage = String.format("processDriveAveragesFile: This (%s) is not the file you're looking for (ends with '%s')", seasonDataFileName, REQUIRED_ENDING);
      log.error(errorMessage);
      throw new RuntimeException(errorMessage);
    }
  }

  private static NflSeasonData parseAndProcessDriveAveragesFileLine(final String[] line,
                                                                    final Map<String, NflSeasonData> teamSeasonDataMap) {
    String teamName = StringUtils.remove(StringUtils.remove(StringUtils.trim(line[1]), '*'), '.');
    NflSeasonData seasonData = getSeasonDataComputeIfAbsent(teamSeasonDataMap, teamName);
    seasonData.setDriveAveragesRank(Integer.parseInt(nice(StringUtils.trim(line[0]))));
    seasonData.setNumberOfDrives(Integer.parseInt(nice(StringUtils.trim(line[3]))));
    seasonData.setNumberOfPlays(Integer.parseInt(nice(StringUtils.trim(line[4]))));
    seasonData.setScoringPct(Double.parseDouble(nice(StringUtils.trim(line[5]))));
    seasonData.setAvgNumberOfPlaysPerDrive(Double.parseDouble(nice(StringUtils.trim(line[7]))));
    seasonData.setAvgYdPerDrive(Double.parseDouble(nice(StringUtils.trim(line[8]))));
    seasonData.setAvgTimeOfPosession(parseHourMinuteAsDouble(nice(StringUtils.trim(line[10]))));

    return seasonData;
  }

  /**
   * Parses the specified string, which must be in HH:MM format (where HH is the number
   * of hours, and MM is the number of minutes), and returns the number of minutes as
   * a double precision floating point number.
   */
  private static double parseHourMinuteAsDouble(final String hourMinuteString) {
    double ret;
    StringTokenizer strtok = new StringTokenizer(hourMinuteString, ":");
    if (strtok.countTokens() == 2) {
      int hours = Integer.parseInt(strtok.nextToken());
      int minutes = Integer.parseInt(strtok.nextToken());
      // Compute the number of minutes
      ret = hours * 60.0 + minutes;
    } else {
      String errorMessage = String.format("Cannot parse %s, string should be in HH:MM format!", hourMinuteString);
      log.error(errorMessage);
      throw new RuntimeException(errorMessage);
    }
    return ret;
  }

  private static void processConversionsFile(final Integer year,
                                             final String seasonDataFileName,
                                             final Map<String, NflSeasonData> teamSeasonDataMap) {
    final String REQUIRED_ENDING = "Conversions.csv";
    final int NUMBER_OF_FIELDS = 12;
    if (seasonDataFileName.endsWith(REQUIRED_ENDING)) { // Sanity check
      InputStream inputStream = NflSeasonDataFlatFileDao.class.getResourceAsStream(seasonDataFileName);
      try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
        CSVReader csvReader = new CSVReader(bufferedReader);
        csvReader.readNext(); // Skip header
        csvReader.readNext(); // Skip header
        String[] line = csvReader.readNext();
        while (line != null) {
          if (line.length == NUMBER_OF_FIELDS) {
            NflSeasonData seasonData = parseAndProcessConversionsFileLine(line, teamSeasonDataMap);
            seasonData.setYear(year);
          } else {
            throw new RuntimeException(String.format("Expected %d fields, got %d instead.", NUMBER_OF_FIELDS, line.length));
          }
          line = csvReader.readNext();
        }
      } catch (IOException e) {
        String message = String.format("Error reading database file name '%s': %s", seasonDataFileName, e.getLocalizedMessage());
        log.error(message, e);
        throw new RuntimeException(message, e);
      }
    } else {
      String errorMessage = String.format("processConversionsFile: This (%s) is not the file you're looking for (ends with '%s')", seasonDataFileName, REQUIRED_ENDING);
      log.error(errorMessage);
      throw new RuntimeException(errorMessage);
    }
  }

  private static NflSeasonData parseAndProcessConversionsFileLine(final String[] line,
                                                                  final Map<String, NflSeasonData> teamSeasonDataMap) {
    String teamName = StringUtils.remove(StringUtils.remove(StringUtils.trim(line[1]), '*'), '.');
    NflSeasonData seasonData = getSeasonDataComputeIfAbsent(teamSeasonDataMap, teamName);
    seasonData.setConversionsRank(Integer.parseInt(nice(StringUtils.trim(line[0]))));
    seasonData.setThirdDownConversionAtt(Integer.parseInt(nice(StringUtils.trim(line[3]))));
    seasonData.setThirdDownConversionMade(Integer.parseInt(nice(StringUtils.trim(line[4]))));
    seasonData.setThirdDownConversionPct(Double.parseDouble(nice(StringUtils.trim(line[5]))));
    seasonData.setFourthDownConversionAtt(Integer.parseInt(nice(StringUtils.trim(line[6]))));
    seasonData.setFourthDownConversionMade(Integer.parseInt(nice(StringUtils.trim(line[7]))));
    seasonData.setFourthDownConversionPct(Double.parseDouble(nice(StringUtils.trim(line[8]))));
    seasonData.setRedZoneAtt(Integer.parseInt(nice(StringUtils.trim(line[9]))));
    seasonData.setRedZoneTd(Integer.parseInt(nice(StringUtils.trim(line[10]))));
    seasonData.setRedZonePct(Double.parseDouble(nice(StringUtils.trim(line[11]))));

    return seasonData;
  }

  private static NflSeasonData getSeasonDataComputeIfAbsent(final Map<String, NflSeasonData> teamSeasonDataMap, final String teamName) {
    return teamSeasonDataMap.computeIfAbsent(teamName, k -> {
      // Season data not found for this team, create it and set team name (k, the key to the map)
      NflSeasonData nflSeasonData = new NflSeasonData();
      nflSeasonData.setTeamName(k);
      return nflSeasonData;
    });
  }

  @Override
  public List<NflSeasonData> fetchAllByYear(final Integer year) {
    return null;
  }

  @Override
  public NflSeasonData fetchByYearAndTeamName(final Integer year, final String teamName) {
    return null;
  }
}
