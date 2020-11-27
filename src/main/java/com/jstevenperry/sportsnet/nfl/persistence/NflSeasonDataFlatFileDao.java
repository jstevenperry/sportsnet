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
import java.util.stream.Collectors;

import static com.jstevenperry.sportsnet.framework.util.NetworkUtils.nice;

@Component("nflSeasonDataDao")
public class NflSeasonDataFlatFileDao implements SeasonDataDao<NflSeasonData> {

  private static final Logger log = LoggerFactory.getLogger(NflSeasonDataFlatFileDao.class);

  private static final String[][] SEASON_DATA_FILE_NAMES = {
    {"2010", "/nfl/2010/2010-Conversions.csv", "/nfl/2010/2010-Drive-Averages.csv", "/nfl/2010/2010-Kick&PuntReturns.csv", "/nfl/2010/2010-Kicking&Punting.csv", "/nfl/2010/2010-Passing-Offense.csv", "/nfl/2010/2010-Rushing-Offense.csv", "/nfl/2010/2010-Scoring-Offense.csv", "/nfl/2010/2010-Team-Offense.csv"},
    {"2011", "/nfl/2011/2011-Conversions.csv", "/nfl/2011/2011-Drive-Averages.csv", "/nfl/2011/2011-Kick&PuntReturns.csv", "/nfl/2011/2011-Kicking&Punting.csv", "/nfl/2011/2011-Passing-Offense.csv", "/nfl/2011/2011-Rushing-Offense.csv", "/nfl/2011/2011-Scoring-Offense.csv", "/nfl/2011/2011-Team-Offense.csv"},
    {"2012", "/nfl/2012/2012-Conversions.csv", "/nfl/2012/2012-Drive-Averages.csv", "/nfl/2012/2012-Kick&PuntReturns.csv", "/nfl/2012/2012-Kicking&Punting.csv", "/nfl/2012/2012-Passing-Offense.csv", "/nfl/2012/2012-Rushing-Offense.csv", "/nfl/2012/2012-Scoring-Offense.csv", "/nfl/2012/2012-Team-Offense.csv"},
    {"2013", "/nfl/2013/2013-Conversions.csv", "/nfl/2013/2013-Drive-Averages.csv", "/nfl/2013/2013-Kick&PuntReturns.csv", "/nfl/2013/2013-Kicking&Punting.csv", "/nfl/2013/2013-Passing-Offense.csv", "/nfl/2013/2013-Rushing-Offense.csv", "/nfl/2013/2013-Scoring-Offense.csv", "/nfl/2013/2013-Team-Offense.csv"},
    {"2014", "/nfl/2014/2014-Conversions.csv", "/nfl/2014/2014-Drive-Averages.csv", "/nfl/2014/2014-Kick&PuntReturns.csv", "/nfl/2014/2014-Kicking&Punting.csv", "/nfl/2014/2014-Passing-Offense.csv", "/nfl/2014/2014-Rushing-Offense.csv", "/nfl/2014/2014-Scoring-Offense.csv", "/nfl/2014/2014-Team-Offense.csv"},
    {"2015", "/nfl/2015/2015-Conversions.csv", "/nfl/2015/2015-Drive-Averages.csv", "/nfl/2015/2015-Kick&PuntReturns.csv", "/nfl/2015/2015-Kicking&Punting.csv", "/nfl/2015/2015-Passing-Offense.csv", "/nfl/2015/2015-Rushing-Offense.csv", "/nfl/2015/2015-Scoring-Offense.csv", "/nfl/2015/2015-Team-Offense.csv"},
    {"2016", "/nfl/2016/2016-Conversions.csv", "/nfl/2016/2016-Drive-Averages.csv", "/nfl/2016/2016-Kick&PuntReturns.csv", "/nfl/2016/2016-Kicking&Punting.csv", "/nfl/2016/2016-Passing-Offense.csv", "/nfl/2016/2016-Rushing-Offense.csv", "/nfl/2016/2016-Scoring-Offense.csv", "/nfl/2016/2016-Team-Offense.csv"},
    {"2017", "/nfl/2017/2017-Conversions.csv", "/nfl/2017/2017-Drive-Averages.csv", "/nfl/2017/2017-Kick&PuntReturns.csv", "/nfl/2017/2017-Kicking&Punting.csv", "/nfl/2017/2017-Passing-Offense.csv", "/nfl/2017/2017-Rushing-Offense.csv", "/nfl/2017/2017-Scoring-Offense.csv", "/nfl/2017/2017-Team-Offense.csv"},
    {"2018", "/nfl/2018/2018-Conversions.csv", "/nfl/2018/2018-Drive-Averages.csv", "/nfl/2018/2018-Kick&PuntReturns.csv", "/nfl/2018/2018-Kicking&Punting.csv", "/nfl/2018/2018-Passing-Offense.csv", "/nfl/2018/2018-Rushing-Offense.csv", "/nfl/2018/2018-Scoring-Offense.csv", "/nfl/2018/2018-Team-Offense.csv"},
    {"2019", "/nfl/2019/2019-Conversions.csv", "/nfl/2019/2019-Drive-Averages.csv", "/nfl/2019/2019-Kick&PuntReturns.csv", "/nfl/2019/2019-Kicking&Punting.csv", "/nfl/2019/2019-Passing-Offense.csv", "/nfl/2019/2019-Rushing-Offense.csv", "/nfl/2019/2019-Scoring-Offense.csv", "/nfl/2019/2019-Team-Offense.csv"}
  };

  private final Map<Integer, Map<String, NflSeasonData>> database = new HashMap<>();

  public NflSeasonDataFlatFileDao() {
    loadData(database);
  }

  private interface FileProcessor {

    String getRequiredFilenameEnding();

    int getNumberOfFields();

    default int getNumberOfHeaderLines() {
      return 2;
    }

    NflSeasonData parseAndProcessFileLine(final String[] line, final Map<String, NflSeasonData> teamSeasonDataMap);
  }

  private static void loadData(final Map<Integer, Map<String, NflSeasonData>> database) {
    for (final String[] seasonDataEntry : SEASON_DATA_FILE_NAMES) {
      Integer year = Integer.parseInt(seasonDataEntry[0]);
      for (int seasonDataFileIndex = 1; seasonDataFileIndex < seasonDataEntry.length; seasonDataFileIndex++) {
        String seasonDataFileName = seasonDataEntry[seasonDataFileIndex];
        Map<String, NflSeasonData> teamSeasonDataMap = database.computeIfAbsent(year, k -> new HashMap<>());
        // See sternly worded comment in SEASON_DATA_FILE_NAMES declaration
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

  private static void processConversionsFile(final Integer year, final String seasonDataFileName, final Map<String, NflSeasonData> teamSeasonDataMap) {
    processFile(year, seasonDataFileName, teamSeasonDataMap, new FileProcessor() {
      @Override
      public String getRequiredFilenameEnding() {
        return "Conversions.csv";
      }

      @Override
      public int getNumberOfFields() {
        return 12;
      }

      @Override
      public NflSeasonData parseAndProcessFileLine(final String[] line, final Map<String, NflSeasonData> teamSeasonDataMap) {
        return parseAndProcessConversionsFileLine(line, teamSeasonDataMap);
      }
    });
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

  private static void processDriveAveragesFile(final Integer year, final String seasonDataFileName, final Map<String, NflSeasonData> teamSeasonDataMap) {
    processFile(year, seasonDataFileName, teamSeasonDataMap, new FileProcessor() {
      @Override
      public String getRequiredFilenameEnding() {
        return "Drive-Averages.csv";
      }

      @Override
      public int getNumberOfFields() {
        return 12;
      }

      @Override
      public NflSeasonData parseAndProcessFileLine(final String[] line, final Map<String, NflSeasonData> teamSeasonDataMap) {
        return parseAndProcessDriveAveragesFileLine(line, teamSeasonDataMap);
      }
    });
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

  private static void processKickAndPuntReturnsFile(final Integer year, final String seasonDataFileName, final Map<String, NflSeasonData> teamSeasonDataMap) {
    processFile(year, seasonDataFileName, teamSeasonDataMap, new FileProcessor() {
      @Override
      public String getRequiredFilenameEnding() {
        return "Kick&PuntReturns.csv";
      }

      @Override
      public int getNumberOfFields() {
        return 14;
      }

      @Override
      public NflSeasonData parseAndProcessFileLine(final String[] line, final Map<String, NflSeasonData> teamSeasonDataMap) {
        return parseAndProcessKickAndPuntReturnsFile(line, teamSeasonDataMap);
      }
    });

  }

  private static NflSeasonData parseAndProcessKickAndPuntReturnsFile(final String[] line, final Map<String, NflSeasonData> teamSeasonDataMap) {
    String teamName = StringUtils.remove(StringUtils.remove(StringUtils.trim(line[1]), '*'), '.');
    NflSeasonData seasonData = getSeasonDataComputeIfAbsent(teamSeasonDataMap, teamName);
    seasonData.setKickPuntReturnRank(Integer.parseInt(nice(StringUtils.trim(line[0]))));
    seasonData.setPuntReturns(Integer.parseInt(nice(StringUtils.trim(line[3]))));
    seasonData.setPuntReturnYd(Integer.parseInt(nice(StringUtils.trim(line[4]))));
    seasonData.setPuntReturnTd(Integer.parseInt(nice(StringUtils.trim(line[5]))));
    seasonData.setPuntReturnLongest(Integer.parseInt(nice(StringUtils.trim(line[6]))));
    seasonData.setPuntReturnYdPerGame(Double.parseDouble(nice(StringUtils.trim(line[7]))));
    seasonData.setKickoffReturns(Integer.parseInt(nice(StringUtils.trim(line[8]))));
    seasonData.setKickoffReturnYd(Integer.parseInt(nice(StringUtils.trim(line[9]))));
    seasonData.setKickoffReturnTd(Integer.parseInt(nice(StringUtils.trim(line[10]))));
    seasonData.setKickoffReturnLongest(Integer.parseInt(nice(StringUtils.trim(line[11]))));
    seasonData.setKickoffReturnYdPerGame(Double.parseDouble(nice(StringUtils.trim(line[12]))));
    seasonData.setAllPurposeYd(Integer.parseInt(nice(StringUtils.trim(line[13]))));

    return seasonData;
  }

  private static void processKickingAndPuntingFile(final Integer year, final String seasonDataFileName, final Map<String, NflSeasonData> teamSeasonDataMap) {
    processFile(year, seasonDataFileName, teamSeasonDataMap, new FileProcessor() {
      @Override
      public String getRequiredFilenameEnding() {
        return "Kicking&Punting.csv";
      }

      @Override
      public int getNumberOfFields() {
        return 30;
      }

      @Override
      public NflSeasonData parseAndProcessFileLine(final String[] line, final Map<String, NflSeasonData> teamSeasonDataMap) {
        return parseAndProcessKickingAndPuntingFile(line, teamSeasonDataMap);
      }
    });
  }

  private static NflSeasonData parseAndProcessKickingAndPuntingFile(final String[] line, final Map<String, NflSeasonData> teamSeasonDataMap) {
    String teamName = StringUtils.remove(StringUtils.remove(StringUtils.trim(line[1]), '*'), '.');
    NflSeasonData seasonData = getSeasonDataComputeIfAbsent(teamSeasonDataMap, teamName);
    seasonData.setKickPuntRank(Integer.parseInt(nice(StringUtils.trim(line[0]))));
    seasonData.setFgAtt(Integer.parseInt(nice(StringUtils.trim(line[13]))));
    seasonData.setFgMade(Integer.parseInt(nice(StringUtils.trim(line[14]))));
    seasonData.setFgLongest(Integer.parseInt(nice(StringUtils.trim(line[15]))));
    seasonData.setFgPct(Double.parseDouble(nice(StringUtils.trim(line[16]))));
    seasonData.setXpAtt(Integer.parseInt(nice(StringUtils.trim(line[17]))));
    seasonData.setXpMade(Integer.parseInt(nice(StringUtils.trim(line[18]))));
    seasonData.setXpPct(Double.parseDouble(nice(StringUtils.trim(line[19]))));
    seasonData.setKickoffs(Integer.parseInt(nice(StringUtils.trim(line[20]))));
    seasonData.setKickoffYd(Integer.parseInt(nice(StringUtils.trim(line[21]))));
    seasonData.setTouchbacks(Integer.parseInt(nice(StringUtils.trim(line[22]))));
    seasonData.setTouchbackPct(Double.parseDouble(nice(StringUtils.trim(line[23]))));
    seasonData.setKickoffAvg(Double.parseDouble(nice(StringUtils.trim(line[24]))));
    seasonData.setPunts(Integer.parseInt(nice(StringUtils.trim(line[25]))));
    seasonData.setPuntYards(Integer.parseInt(nice(StringUtils.trim(line[26]))));
    seasonData.setPuntLongest(Integer.parseInt(nice(StringUtils.trim(line[27]))));
    seasonData.setPuntsBlocked(Integer.parseInt(nice(StringUtils.trim(line[28]))));
    seasonData.setYdPerPunt(Double.parseDouble(nice(StringUtils.trim(line[29]))));

    return seasonData;
  }

  private static void processPassingOffenseFile(final Integer year, final String seasonDataFileName, final Map<String, NflSeasonData> teamSeasonDataMap) {
    processFile(year, seasonDataFileName, teamSeasonDataMap, new FileProcessor() {
      @Override
      public String getRequiredFilenameEnding() {
        return "Passing-Offense.csv";
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
      public NflSeasonData parseAndProcessFileLine(final String[] line, final Map<String, NflSeasonData> teamSeasonDataMap) {
        return parseAndProcessPassingOffenseFile(line, teamSeasonDataMap);
      }
    });
  }

  private static NflSeasonData parseAndProcessPassingOffenseFile(final String[] line, final Map<String, NflSeasonData> teamSeasonDataMap) {
    String teamName = StringUtils.remove(StringUtils.remove(StringUtils.trim(line[1]), '*'), '.');
    NflSeasonData seasonData = getSeasonDataComputeIfAbsent(teamSeasonDataMap, teamName);
    seasonData.setPassingOffenseRank(Integer.parseInt(nice(StringUtils.trim(line[0]))));
    seasonData.setPassCompletionPct(Double.parseDouble(nice(StringUtils.trim(line[5]))));
    seasonData.setPassTdPct(Double.parseDouble(nice(StringUtils.trim(line[8]))));
    seasonData.setIntPct(Double.parseDouble(nice(StringUtils.trim(line[10]))));
    seasonData.setLongestPass(Integer.parseInt(nice(StringUtils.trim(line[11]))));
    seasonData.setPassYdPerAtt(Double.parseDouble(nice(StringUtils.trim(line[12]))));
    seasonData.setAdjYdPerAtt(Double.parseDouble(nice(StringUtils.trim(line[13]))));
    seasonData.setYdPerCompletion(Double.parseDouble(nice(StringUtils.trim(line[14]))));
    seasonData.setYardPerGame(Double.parseDouble(nice(StringUtils.trim(line[15]))));
    seasonData.setPassRating(Double.parseDouble(nice(StringUtils.trim(line[16]))));
    seasonData.setOffensiveSacks(Integer.parseInt(nice(StringUtils.trim(line[17]))));
    seasonData.setYdLostSacks(Integer.parseInt(nice(StringUtils.trim(line[18]))));
    seasonData.setAdjNetYdPerPassAtt(Double.parseDouble(nice(StringUtils.trim(line[20]))));
    seasonData.setSackedWhilePassingPct(Double.parseDouble(nice(StringUtils.trim(line[21]))));
    seasonData.setQbLedCombacks(Integer.parseInt(nice(StringUtils.trim(line[22]))));
    seasonData.setQbGameWinningDrives(Integer.parseInt(nice(StringUtils.trim(line[23]))));
    seasonData.setExpectedPtsPassingOffense(Double.parseDouble(nice(StringUtils.trim(line[24]))));

    return seasonData;
  }

  private static void processRushingOffenseFile(final Integer year, final String seasonDataFileName, final Map<String, NflSeasonData> teamSeasonDataMap) {
    processFile(year, seasonDataFileName, teamSeasonDataMap, new FileProcessor() {
      @Override
      public String getRequiredFilenameEnding() {
        return "Rushing-Offense.csv";
      }

      @Override
      public int getNumberOfFields() {
        return 11;
      }

      @Override
      public int getNumberOfHeaderLines() {
        return 1;
      }

      @Override
      public NflSeasonData parseAndProcessFileLine(final String[] line, final Map<String, NflSeasonData> teamSeasonDataMap) {
        return parseAndProcessRushingOffenseFile(line, teamSeasonDataMap);
      }
    });
  }

  private static NflSeasonData parseAndProcessRushingOffenseFile(final String[] line, final Map<String, NflSeasonData> teamSeasonDataMap) {
    String teamName = StringUtils.remove(StringUtils.remove(StringUtils.trim(line[1]), '*'), '.');
    NflSeasonData seasonData = getSeasonDataComputeIfAbsent(teamSeasonDataMap, teamName);
    seasonData.setRushingOffenseRank(Integer.parseInt(nice(StringUtils.trim(line[0]))));
    seasonData.setYdRushingPerGame(Double.parseDouble(nice(StringUtils.trim(line[8]))));
    seasonData.setRushingFumbles(Integer.parseInt(nice(StringUtils.trim(line[9]))));
    seasonData.setExpectedPtsRushingOffense(Double.parseDouble(nice(StringUtils.trim(line[10]))));

    return seasonData;
  }

  private static void processScoringOffenseFile(final Integer year, final String seasonDataFileName, final Map<String, NflSeasonData> teamSeasonDataMap) {
    processFile(year, seasonDataFileName, teamSeasonDataMap, new FileProcessor() {
      @Override
      public String getRequiredFilenameEnding() {
        return "Scoring-Offense.csv";
      }

      @Override
      public int getNumberOfFields() {
        return 20;
      }

      @Override
      public int getNumberOfHeaderLines() {
        return 1;
      }

      @Override
      public NflSeasonData parseAndProcessFileLine(final String[] line, final Map<String, NflSeasonData> teamSeasonDataMap) {
        return parseAndProcessScoringOffenseLine(line, teamSeasonDataMap);
      }
    });
  }

  private static NflSeasonData parseAndProcessScoringOffenseLine(final String[] line, final Map<String, NflSeasonData> teamSeasonDataMap) {
    String teamName = StringUtils.remove(StringUtils.remove(StringUtils.trim(line[1]), '*'), '.');
    NflSeasonData seasonData = getSeasonDataComputeIfAbsent(teamSeasonDataMap, teamName);
    seasonData.setScoringOffenseRank(Integer.parseInt(nice(StringUtils.trim(line[0]))));
    seasonData.setTwoPtConversionAtt(Integer.parseInt(nice(StringUtils.trim(line[12]))));
    seasonData.setTwoPtConversionMade(Integer.parseInt(nice(StringUtils.trim(line[11]))));
    seasonData.setSafety(Integer.parseInt(nice(StringUtils.trim(line[17]))));
    seasonData.setPtsPerGame(Double.parseDouble(nice(StringUtils.trim(line[19]))));

    return seasonData;
  }

  private static void processTeamOffenseFile(final Integer year, final String seasonDataFileName, final Map<String, NflSeasonData> teamSeasonDataMap) {
    processFile(year, seasonDataFileName, teamSeasonDataMap, new FileProcessor() {
      @Override
      public String getRequiredFilenameEnding() {
        return "Team-Offense.csv";
      }

      @Override
      public int getNumberOfFields() {
        return 28;
      }

      @Override
      public NflSeasonData parseAndProcessFileLine(final String[] line, final Map<String, NflSeasonData> teamSeasonDataMap) {
        return parseAndProcessTeamOffenseFileLine(line, teamSeasonDataMap);
      }
    });
  }

  private static NflSeasonData parseAndProcessTeamOffenseFileLine(final String[] line, final Map<String, NflSeasonData> teamSeasonDataMap) {
    String teamName = StringUtils.remove(StringUtils.remove(StringUtils.trim(line[1]), '*'), '.');
    NflSeasonData seasonData = getSeasonDataComputeIfAbsent(teamSeasonDataMap, teamName);
    seasonData.setTeamOffenseRank(Integer.parseInt(nice(StringUtils.trim(line[0]))));
    seasonData.setGamesPlayed(Integer.parseInt(nice(StringUtils.trim(line[2]))));
    seasonData.setPointsFor(Integer.parseInt(nice(StringUtils.trim(line[3]))));
    seasonData.setYards(Integer.parseInt(nice(StringUtils.trim(line[4]))));
    seasonData.setPlays(Integer.parseInt(nice(StringUtils.trim(line[5]))));
    seasonData.setYardsPerPlay(Double.parseDouble(nice(StringUtils.trim(line[6]))));
    seasonData.setTurnovers(Integer.parseInt(nice(StringUtils.trim(line[7]))));
    seasonData.setFumblesLost(Integer.parseInt(nice(StringUtils.trim(line[8]))));
    seasonData.setFirstDowns(Integer.parseInt(nice(StringUtils.trim(line[9]))));
    seasonData.setCompletedPasses(Integer.parseInt(nice(StringUtils.trim(line[10]))));
    seasonData.setPassAttempts(Integer.parseInt(nice(StringUtils.trim(line[11]))));
    seasonData.setYdPassing(Integer.parseInt(nice(StringUtils.trim(line[12]))));
    seasonData.setPassingTd(Integer.parseInt(nice(StringUtils.trim(line[13]))));
    seasonData.setInterceptions(Integer.parseInt(nice(StringUtils.trim(line[14]))));
    seasonData.setNetYdPerPassAtt(Double.parseDouble(nice(StringUtils.trim(line[15]))));
    seasonData.setPassing1d(Integer.parseInt(nice(StringUtils.trim(line[16]))));
    seasonData.setRushingAtt(Integer.parseInt(nice(StringUtils.trim(line[17]))));
    seasonData.setRushingYd(Integer.parseInt(nice(StringUtils.trim(line[18]))));
    seasonData.setRushingTd(Integer.parseInt(nice(StringUtils.trim(line[19]))));
    seasonData.setRushingYdPerAtt(Double.parseDouble(nice(StringUtils.trim(line[20]))));
    seasonData.setRushing1d(Integer.parseInt(nice(StringUtils.trim(line[21]))));
    seasonData.setPenalties(Integer.parseInt(nice(StringUtils.trim(line[22]))));
    seasonData.setPenaltyYd(Integer.parseInt(nice(StringUtils.trim(line[23]))));
    seasonData.setFirstDownsByPenalty(Integer.parseInt(nice(StringUtils.trim(line[24]))));
    seasonData.setDriveScorePct(Double.parseDouble(nice(StringUtils.trim(StringUtils.remove(line[25], '%')))));
    seasonData.setDriveOffensiveToPct(Double.parseDouble(nice(StringUtils.trim(StringUtils.remove(line[26], '%')))));
    seasonData.setExpectedPtsByAllOffense(Double.parseDouble(nice(StringUtils.trim(line[27]))));

    return seasonData;
  }

  /**
   * Common code. Reads the specified file and invokes the FileProcessor that is provided
   * by the caller to support that. Upon successful completion, this method has decorated
   * the Season Data object for the type of data contained in the file with the values
   * from the file for the specified year.
   */
  private static void processFile(final Integer year,
                                  final String seasonDataFileName,
                                  final Map<String, NflSeasonData> teamSeasonDataMap,
                                  final FileProcessor fileProcessor) {
    final String REQUIRED_ENDING = fileProcessor.getRequiredFilenameEnding();
    final int NUMBER_OF_FIELDS = fileProcessor.getNumberOfFields();
    if (seasonDataFileName.endsWith(REQUIRED_ENDING)) { // Sanity check
      InputStream inputStream = NflSeasonDataFlatFileDao.class.getResourceAsStream(seasonDataFileName);
      try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
        CSVReader csvReader = new CSVReader(bufferedReader);
        // Skip header lines
        for (int skipCount = 0; skipCount < fileProcessor.getNumberOfHeaderLines(); skipCount++) {
          csvReader.readNext(); // Skip header
        }
        int numberOfDataLinesProcessed = 0;
        String[] line = csvReader.readNext();
        while (line != null) {
          if (line.length == NUMBER_OF_FIELDS) {
            NflSeasonData seasonData = fileProcessor.parseAndProcessFileLine(line, teamSeasonDataMap);
            seasonData.setYear(year);
          } else {
            throw new RuntimeException(String.format("processFile(%d, %s): Expected %d fields, got %d instead.", year, seasonDataFileName, NUMBER_OF_FIELDS, line.length));
          }
          line = csvReader.readNext();
          numberOfDataLinesProcessed++;
        }
        log.debug("processFile({}, {}, ...) Processed {} lines of data.", year, seasonDataFileName, numberOfDataLinesProcessed);
      } catch (IOException e) {
        String message = String.format("processFile(%d, %s): Error reading database file name '%s': %s", year, seasonDataFileName, seasonDataFileName, e.getLocalizedMessage());
        log.error(message, e);
        throw new RuntimeException(message, e);
      }
    } else {
      String errorMessage = String.format("processFile(%d, %s): This (%s) is not the file you're looking for (ends with '%s')", year, seasonDataFileName, seasonDataFileName, REQUIRED_ENDING);
      log.error(errorMessage);
      throw new RuntimeException(errorMessage);
    }
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
    Map<String, NflSeasonData> teamSeasonDataMap = database.get(year);
    List<NflSeasonData> ret = teamSeasonDataMap.keySet()
      .stream()
      .map(teamSeasonDataMap::get)
      .collect(Collectors.toList());
    log.debug(String.format("fetchAllByYear(%d): Returning %d records.", year, ret.size()));
    return ret;
  }

  @Override
  public NflSeasonData fetchByYearAndTeamName(final Integer year, final String teamName) {
    NflSeasonData ret;
    Map<String, NflSeasonData> teamSeasonDataMap = database.get(year);
    ret = teamSeasonDataMap.get(teamName);

    log.trace("fetchByYearAndTeamName({}, {}): Returning NflSeasonData: {}", year, teamName, ret);
    return ret;
  }
}
