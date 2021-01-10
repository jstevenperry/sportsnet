package com.jstevenperry.sportsnet.ncaabb.persistence;

import com.jstevenperry.sportsnet.framework.model.TournamentResult;
import com.jstevenperry.sportsnet.framework.persistence.AbstractTournamentResultFlatFileDao;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component("ncaaBbTournamentResultDao")
public class NcaaBbTournamentResultFlatFileDao extends AbstractTournamentResultFlatFileDao<NcaaBbSeasonData> {

  private static final Logger log = LoggerFactory.getLogger(NcaaBbTournamentResultFlatFileDao.class);

  private static final String FILE_NAME = "/ncaabb/tournament_result.csv";

  @Override
  protected String[][] getDataFileNames() {
    throw new UnsupportedOperationException("getDataFileNames() not implemented, reason = Method not appropriate for this class");
  }

  @Override
  protected int getNumberOfFields() {
    return 7;
  }

  @Override
  protected void loadData() {
    String databaseFileName = FILE_NAME;
    InputStream inputStream = NcaaBbTournamentResultFlatFileDao.class.getResourceAsStream(databaseFileName);
    try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
      CSVReader csvReader = new CSVReader(bufferedReader);
      csvReader.readNext(); // Read header
      String[] line = csvReader.readNext();
      while (line != null) {
        if (line.length == getNumberOfFields()) {
          TournamentResult data = parseLine(line);
          List<TournamentResult> tournamentResults = getDatabase().computeIfAbsent(data.getYear(), k -> new ArrayList<>());
          tournamentResults.add(data);
        } else {
          throw new RuntimeException(String.format("Expected %d fields, got %d instead.", getNumberOfFields(), line.length));
        }
        line = csvReader.readNext();
      }
    } catch (IOException e) {
      String message = String.format("Error reading database file name '%s': %s", databaseFileName, e.getLocalizedMessage());
      log.error(message, e);
      throw new RuntimeException(message, e);
    }
  }

  private static TournamentResult parseLine(final String[] line) {
    TournamentResult ret = new TournamentResult();
    ret.setId(Integer.parseInt(line[0]));
    ret.setYear(Integer.parseInt(line[1]));
    ret.setGameDate(Date.from(LocalDate.parse(line[2]).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
    ret.setWinningTeamName(StringUtils.trim(line[3]));
    ret.setWinningScore(Integer.parseInt(line[4]));
    ret.setLosingTeamName(StringUtils.trim(line[5]));
    ret.setLosingScore(Integer.parseInt(line[6]));
    return ret;
  }

}
