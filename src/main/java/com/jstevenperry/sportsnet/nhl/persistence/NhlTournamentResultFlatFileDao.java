package com.jstevenperry.sportsnet.nhl.persistence;

import com.jstevenperry.sportsnet.framework.model.TournamentResult;
import com.jstevenperry.sportsnet.framework.persistence.TournamentResultDao;
import com.opencsv.CSVReader;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component("nhlTournamentResultDao")
public class NhlTournamentResultFlatFileDao implements TournamentResultDao {

  private static final Logger log = LoggerFactory.getLogger(NhlTournamentResultFlatFileDao.class);

  private static final int NUMBER_OF_FIELDS = 4;
  private static final String[][] TOURNAMENT_RESULT_FILE_NAMES = {
    {"2011", "/nhl/2010-11-tournament_result.csv"},
    {"2012", "/nhl/2011-12-tournament_result.csv"},
    {"2013", "/nhl/2012-13-tournament_result.csv"},
    {"2014", "/nhl/2013-14-tournament_result.csv"},
    {"2015", "/nhl/2014-15-tournament_result.csv"},
    {"2016", "/nhl/2015-16-tournament_result.csv"},
    {"2017", "/nhl/2016-17-tournament_result.csv"},
    {"2018", "/nhl/2017-18-tournament_result.csv"},
    {"2019", "/nhl/2018-19-tournament_result.csv"},
    {"2020", "/nhl/2019-20-tournament_result.csv"},
  };

  private final Map<Integer, List<TournamentResult>> database = new HashMap<>();

  public NhlTournamentResultFlatFileDao() {
    loadData(database);
  }

  private static void loadData(final Map<Integer, List<TournamentResult>> database) {
    for (final String[] tournamentEntry : TOURNAMENT_RESULT_FILE_NAMES) {
      Integer year = Integer.parseInt(tournamentEntry[0]);
      String dataFileName = tournamentEntry[1];
      InputStream inputStream = NhlTournamentResultFlatFileDao.class.getResourceAsStream(dataFileName);
      try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
        CSVReader csvReader = new CSVReader(bufferedReader);
        csvReader.readNext(); // Read header
        String[] line = csvReader.readNext();
        while (line != null) {
          if (line.length == NUMBER_OF_FIELDS) {
            TournamentResult data = parseLine(year, line);
            List<TournamentResult> tournamentResults = database.computeIfAbsent(year, k -> new ArrayList<>());
            tournamentResults.add(data);
          } else {
            throw new RuntimeException(String.format("Expected %d fields, got %d instead.", NUMBER_OF_FIELDS, line.length));
          }
          line = csvReader.readNext();
        }
      } catch (Exception e) {
        String message = String.format("Error reading database file name '%s': %s", dataFileName, e.getLocalizedMessage());
        log.error(message, e);
        throw new RuntimeException(message, e);
      }
    }
  }

  private static TournamentResult parseLine(final Integer year, final String[] line) {
    TournamentResult ret = new TournamentResult();
    ret.setYear(year);
    ret.setWinningTeamName(StringUtils.remove(StringUtils.trim(line[0]), "*."));
    ret.setWinningScore(Integer.parseInt(line[1]));
    ret.setLosingTeamName(StringUtils.remove(StringUtils.remove(StringUtils.trim(line[2]), '*'), '.'));
    ret.setLosingScore(Integer.parseInt(line[3]));
    return ret;
  }

  @Override
  public List<TournamentResult> fetchAllByYear(final Integer year) {
    return database.get(year);
  }
}
