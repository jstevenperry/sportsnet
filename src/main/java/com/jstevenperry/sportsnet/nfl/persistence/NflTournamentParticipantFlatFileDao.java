package com.jstevenperry.sportsnet.nfl.persistence;

import com.jstevenperry.sportsnet.framework.model.TournamentParticipant;
import com.jstevenperry.sportsnet.framework.persistence.TournamentParticipantDao;
import com.opencsv.CSVReader;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NflTournamentParticipantFlatFileDao implements TournamentParticipantDao {
  private static final Logger log = LoggerFactory.getLogger(NflTournamentParticipantFlatFileDao.class);

  private static final int NUMBER_OF_FIELDS = 2;
  private static final String[][] TOURNAMENT_PARTICIPANT_FILE_NAMES = {
    {"2012", "/Nfl/2012-13-tournament_participant.csv"},
    {"2013", "/Nfl/2013-14-tournament_participant.csv"},
    {"2014", "/Nfl/2014-15-tournament_participant.csv"},
    {"2015", "/Nfl/2015-16-tournament_participant.csv"},
    {"2016", "/Nfl/2016-17-tournament_participant.csv"},
    {"2017", "/Nfl/2017-18-tournament_participant.csv"},
    {"2018", "/Nfl/2018-19-tournament_participant.csv"},
    {"2019", "/Nfl/2019-20-tournament_participant.csv"},
  };

  private final Map<Integer, Map<String, TournamentParticipant>> database = new HashMap<>();

  public NflTournamentParticipantFlatFileDao() {
    loadData(database);
  }

  private static void loadData(final Map<Integer, Map<String, TournamentParticipant>> database) {
    for (final String[] tournamentEntry : TOURNAMENT_PARTICIPANT_FILE_NAMES) {
      Integer year = Integer.parseInt(tournamentEntry[0]);
      String dataFileName = tournamentEntry[1];
      InputStream inputStream = NflTournamentParticipantFlatFileDao.class.getResourceAsStream(dataFileName);
      try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
        CSVReader csvReader = new CSVReader(bufferedReader);
        csvReader.readNext(); // Read header
        String[] line = csvReader.readNext();
        while (line != null) {
          if (line.length == NUMBER_OF_FIELDS) {
            TournamentParticipant data = parseLine(year, line);
            Map<String, TournamentParticipant> teamDataMap = database.computeIfAbsent(year, k -> new HashMap<>());
            teamDataMap.put(data.getTeamName(), data);
          } else {
            throw new RuntimeException(String.format("loadData: File %s: Expected %d fields, got %d instead.", dataFileName, NUMBER_OF_FIELDS, line.length));
          }
          line = csvReader.readNext();
        }
      } catch (IOException e) {
        String message = String.format("loadData: Error reading database file name '%s': %s", dataFileName, e.getLocalizedMessage());
        log.error(message, e);
        throw new RuntimeException(message, e);
      }
    }
  }

  private static TournamentParticipant parseLine(final Integer year, final String[] line) {
    TournamentParticipant ret = new TournamentParticipant();
    ret.setYear(year);
    ret.setTeamName(StringUtils.remove(StringUtils.remove(StringUtils.trim(line[0]), '*'), '.'));
    ret.setNumberOfVictories(Integer.parseInt(line[1]));
    return ret;
  }

  @Override
  public TournamentParticipant fetchByYear(final String teamName, final Integer year) {
    TournamentParticipant ret = null;
    Map<String, TournamentParticipant> teamDataMap = database.get(year);
    if (teamDataMap != null) {
      ret = teamDataMap.get(teamName);
    }
    return ret;
  }

  @Override
  public List<Integer> fetchValidTournamentYears() {
    return database.keySet().stream().sorted().collect(Collectors.toList());
  }
}
