package com.jstevenperry.sportsnet.ncaabb.persistence;

import com.jstevenperry.sportsnet.framework.model.TournamentParticipant;
import com.jstevenperry.sportsnet.framework.persistence.AbstractTournamentParticipantFlatFileDao;
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
import java.util.HashMap;
import java.util.Map;

@Component("ncaaBbTournamentParticipantDao")
public class NcaaBbTournamentParticipantFlatFileDao extends AbstractTournamentParticipantFlatFileDao<NcaaBbSeasonData> {

  private static final Logger log = LoggerFactory.getLogger(NcaaBbTournamentParticipantFlatFileDao.class);

  private static final String FILE_NAME = "/ncaabb/tournament_participant.csv";

  @Override
  protected String[][] getDataFileNames() {
    throw new UnsupportedOperationException("getDataFileNames() not implemented, reason = Method not appropriate for this class");
  }

  @Override
  protected int getNumberOfFields() {
    return 4;
  }

  @Override
  protected void loadData() {
    String databaseFileName = FILE_NAME;
    InputStream inputStream = NcaaBbTournamentParticipantFlatFileDao.class.getResourceAsStream(databaseFileName);
    try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
      CSVReader csvReader = new CSVReader(bufferedReader);
      csvReader.readNext(); // Read header
      String[] line = csvReader.readNext();
      while (line != null) {
        if (line.length == getNumberOfFields()) {
          TournamentParticipant data = parseLine(line);
          Map<String, TournamentParticipant> teamDataMap = getDatabase().computeIfAbsent(data.getYear(), k -> new HashMap<>());
          teamDataMap.put(data.getTeamName(), data);
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

  private static TournamentParticipant parseLine(final String[] line) {
    TournamentParticipant ret = new TournamentParticipant();
    ret.setId(Integer.parseInt(line[0]));
    ret.setYear(Integer.parseInt(line[1]));
    ret.setTeamName(StringUtils.trim(line[2]));
    ret.setNumberOfVictories(Integer.parseInt(line[3]));
    return ret;
  }

}