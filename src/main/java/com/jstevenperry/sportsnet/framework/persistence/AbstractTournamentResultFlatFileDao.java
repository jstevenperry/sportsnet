package com.jstevenperry.sportsnet.framework.persistence;

import com.jstevenperry.sportsnet.framework.model.AbstractSeasonData;
import com.jstevenperry.sportsnet.framework.model.TournamentResult;
import com.jstevenperry.sportsnet.nhl.persistence.NhlTournamentResultFlatFileDao;
import com.opencsv.CSVReader;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractTournamentResultFlatFileDao<T extends AbstractSeasonData> extends AbstractFlatFileProcessor<T> implements TournamentResultDao {

    private static final Logger log = LoggerFactory.getLogger(AbstractTournamentResultFlatFileDao.class);

    /**
     * The number of fields in the standard Tournament results file, subclasses must implement.
     */
    protected abstract int getNumberOfFields();

    private Map<Integer, List<TournamentResult>> database;

    protected Map<Integer, List<TournamentResult>> getDatabase() {
        if (database == null) {
            database = new HashMap<>();
        }
        return database;
    }

    @Override
    protected void loadData() {
        for (final String[] tournamentEntry : getDataFileNames()) {
            Integer year = Integer.parseInt(tournamentEntry[0]);
            String dataFileName = tournamentEntry[1];
            InputStream inputStream = getClass().getResourceAsStream(dataFileName);
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
                CSVReader csvReader = new CSVReader(bufferedReader);
                csvReader.readNext(); // Read header
                String[] line = csvReader.readNext();
                while (line != null) {
                    if (lineNotEmpty(line)) {
                        if (line.length == getNumberOfFields()) {
                            TournamentResult data = parseLine(year, line);
                            List<TournamentResult> tournamentResults = getDatabase().computeIfAbsent(year, k -> new ArrayList<>());
                            tournamentResults.add(data);
                        } else {
                            throw new RuntimeException(String.format("Expected %d fields, got %d instead. (Line data: [%s])", getNumberOfFields(), line.length, Arrays.toString(line)));
                        }
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

    protected TournamentResult parseLine(final Integer year, final String[] line) {
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
        return getDatabase().get(year);
    }
}
