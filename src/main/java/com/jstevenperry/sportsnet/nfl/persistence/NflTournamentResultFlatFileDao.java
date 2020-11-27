package com.jstevenperry.sportsnet.nfl.persistence;

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

@Component("nflTournamentResultFlatFileDao")
public class NflTournamentResultFlatFileDao implements TournamentResultDao {
  private static final Logger log = LoggerFactory.getLogger(NflTournamentResultFlatFileDao.class);

  private static final int NUMBER_OF_FIELDS = 9;
  private static final String[][] TOURNAMENT_RESULT_FILE_NAMES = {
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

  private final Map<Integer, List<TournamentResult>> database = new HashMap<>();

  public NflTournamentResultFlatFileDao() {
    loadData(database);
  }

  private static void loadData(final Map<Integer, List<TournamentResult>> database) {
    for (final String[] tournamentEntry : TOURNAMENT_RESULT_FILE_NAMES) {
      Integer year = Integer.parseInt(tournamentEntry[0]);
      String dataFileName = tournamentEntry[1];
      InputStream inputStream = NflTournamentResultFlatFileDao.class.getResourceAsStream(dataFileName);
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
    ret.setWinningTeamName(StringUtils.remove(StringUtils.trim(line[3]), "*."));
    ret.setWinningScore(Integer.parseInt(line[7]));
    ret.setLosingTeamName(StringUtils.remove(StringUtils.remove(StringUtils.trim(line[5]), '*'), '.'));
    ret.setLosingScore(Integer.parseInt(line[8]));
    return ret;
  }

  @Override
  public List<TournamentResult> fetchAllByYear(final Integer year) {
    return database.get(year);
  }
}
