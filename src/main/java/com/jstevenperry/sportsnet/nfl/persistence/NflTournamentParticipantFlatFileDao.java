package com.jstevenperry.sportsnet.nfl.persistence;

import com.jstevenperry.sportsnet.framework.model.TournamentParticipant;
import com.jstevenperry.sportsnet.framework.persistence.TournamentParticipantDao;
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

@Component("nflTournamentParticipantFlatFileDao")
public class NflTournamentParticipantFlatFileDao implements TournamentParticipantDao {
  private static final Logger log = LoggerFactory.getLogger(NflTournamentParticipantFlatFileDao.class);

  private static final int NUMBER_OF_FIELDS = 9;
  private static final String[][] TOURNAMENT_PARTICIPANT_FILE_NAMES = {
    {"2006", "/nfl/2006/2006-2007-tournament_result.csv"},
    {"2007", "/nfl/2007/2007-2008-tournament_result.csv"},
    {"2008", "/nfl/2008/2008-2009-tournament_result.csv"},
    {"2009", "/nfl/2009/2009-2010-tournament_result.csv"},
    {"2010", "/nfl/2010/2010-2011-tournament_result.csv"},
    {"2011", "/nfl/2011/2011-2012-tournament_result.csv"},
    {"2012", "/nfl/2012/2012-2013-tournament_result.csv"},
    {"2013", "/nfl/2013/2013-2014-tournament_result.csv"},
    {"2014", "/nfl/2014/2014-2015-tournament_result.csv"},
    {"2015", "/nfl/2015/2015-2016-tournament_result.csv"},
    {"2016", "/nfl/2016/2016-2017-tournament_result.csv"},
    {"2017", "/nfl/2017/2017-2018-tournament_result.csv"},
    {"2018", "/nfl/2018/2018-2019-tournament_result.csv"},
    {"2019", "/nfl/2019/2019-2020-tournament_result.csv"},
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
            Map<String, TournamentParticipant> teamDataMap = database.computeIfAbsent(year, k -> new HashMap<>());
            parseAndProcessLine(year, line, teamDataMap);
          } else {
            throw new RuntimeException(String.format("loadData: File %s: Expected %d fields, got %d instead.", dataFileName, NUMBER_OF_FIELDS, line.length));
          }
          line = csvReader.readNext();
        }
        Map<String, TournamentParticipant> teamDataMap = database.get(year);
        teamDataMap.keySet().forEach(teamName -> log.debug("Year: {}, team: {}, number of victories: {}", year, teamName, teamDataMap.get(teamName).getNumberOfVictories()));
      } catch (IOException e) {
        String message = String.format("loadData: Error reading database file name '%s': %s", dataFileName, e.getLocalizedMessage());
        log.error(message, e);
        throw new RuntimeException(message, e);
      }
    }
  }

  /**
   * Parse and process the tournament results file to figure out who participated
   * and how far they went (that is, the number of victories they had).
   *
   * The logic here looks a little strange because of the way the playoffs are
   * setup (specifically because of the wildcard rounds). Winning a wildcard game
   * gives the winner one victory, but it's possible that a team made the playoffs
   * without having to play in the wildcard round, so they get one victory for that
   * all on its own, which is why the winning team is given a victory right off the
   * bat when their TournamentParticipant object is created, and why nothing special
   * is done to either winner or loser in the "WildCard" case of the switch statement.
   *
   * Maybe the attribute should be "tournamentDistance" rather than "numberOfVictories"
   * but I'm just too lazy to go back and change it.
   */
  private static void parseAndProcessLine(final Integer year, final String[] line, final Map<String, TournamentParticipant> teamDataMap) {
    String matchup = StringUtils.trim(line[0]);

    String winningTeam = StringUtils.remove(StringUtils.remove(StringUtils.trim(line[3]), '*'), '.');
    TournamentParticipant winner = teamDataMap.computeIfAbsent(winningTeam, teamName -> new TournamentParticipant().setTeamName(winningTeam).setYear(year).setNumberOfVictories(1));

    String losingTeam = StringUtils.remove(StringUtils.remove(StringUtils.trim(line[5]), '*'), '.');
    TournamentParticipant loser = teamDataMap.computeIfAbsent(losingTeam, teamName -> new TournamentParticipant().setTeamName(losingTeam).setYear(year).setNumberOfVictories(0));

    // Make sure to give the division matchup loser at least one victory just for making the playoffs
    switch (matchup) {
      case "WildCard":
        // Do nothing special - winner already given a victory when their TR object was created, loser initialized to zero
        break;
      case "Division":
        // Give the loser a single win, just for making the playoffs. If the loser was a wildcard winner, they
        // still retain their one victory (as opposed to wildcard round losers, who are given zero victories).
        loser.setNumberOfVictories(1);
        // Increment winner's tally
        winner.setNumberOfVictories(winner.getNumberOfVictories() + 1);
        break;
      case "ConfChamp":
      case "SuperBowl":
        // Increment winner's tally
        winner.setNumberOfVictories(winner.getNumberOfVictories() + 1);
        break;
    }
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
