/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.persistence;

import com.jstevenperry.sportsnet.framework.model.AbstractSeasonData;
import com.jstevenperry.sportsnet.framework.model.TournamentParticipant;
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

public abstract class AbstractTournamentParticipantFlatFileDao<T extends AbstractSeasonData> extends AbstractFlatFileProcessor<T> implements TournamentParticipantDao {

    private static final Logger log = LoggerFactory.getLogger(AbstractTournamentParticipantFlatFileDao.class);

    private Map<Integer, Map<String, TournamentParticipant>> database;

    protected Map<Integer, Map<String, TournamentParticipant>> getDatabase() {
        if (database == null) {
            database = new HashMap<>();
        }
        return database;
    }

    /**
     * The number of fields in the standard tournament participant file.
     * Subclasses will override if their sport differs.
     */
    protected int getNumberOfFields() {
        return 2;
    }

    protected void loadData() {
        for (final String[] tournamentEntry : getDataFileNames()) {
            Integer year = Integer.parseInt(tournamentEntry[0]);
            String dataFileName = tournamentEntry[1];
            InputStream inputStream = this.getClass().getResourceAsStream(dataFileName);
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
                CSVReader csvReader = new CSVReader(bufferedReader);
                csvReader.readNext(); // Read header
                String[] line = csvReader.readNext();
                while (line != null) {
                    if (lineNotEmpty(line)) {
                        if (line.length == getNumberOfFields()) {
                            TournamentParticipant data = parseLine(year, line);
                            Map<String, TournamentParticipant> teamDataMap = getDatabase().computeIfAbsent(year, k -> new HashMap<>());
                            teamDataMap.put(data.getTeamName(), data);
                        } else {
                            throw new RuntimeException(String.format("File %s: Expected %d fields, got %d instead.", dataFileName, getNumberOfFields(), line.length));
                        }
                    }
                    line = csvReader.readNext();
                }
            } catch (IOException e) {
                String message = String.format("Error reading database file name '%s': %s", dataFileName, e.getLocalizedMessage());
                log.error(message, e);
                throw new RuntimeException(message, e);
            }
        }
    }

    protected TournamentParticipant parseLine(final Integer year, final String[] line) {
        TournamentParticipant ret = new TournamentParticipant();
        ret.setYear(year);
        ret.setTeamName(StringUtils.remove(StringUtils.remove(StringUtils.trim(line[0]), '*'), '.'));
        ret.setNumberOfVictories(Integer.parseInt(line[1]));
        return ret;
    }

    @Override
    public TournamentParticipant fetchByYear(final String teamName, final Integer year) {
        TournamentParticipant ret = null;
        Map<String, TournamentParticipant> teamDataMap = getDatabase().get(year);
        if (teamDataMap != null) {
            ret = teamDataMap.get(teamName);
        }
        return ret;
    }

    @Override
    public List<Integer> fetchValidTournamentYears() {
        return getDatabase().keySet().stream().sorted().collect(Collectors.toList());
    }
}
