package com.jstevenperry.sportsnet.framework.persistence;

import com.jstevenperry.sportsnet.framework.model.AbstractSeasonData;
import com.jstevenperry.sportsnet.nfl.persistence.NflSeasonDataFlatFileDao;
import com.opencsv.CSVReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractSeasonDataFlatFileDao<T extends AbstractSeasonData> extends AbstractFlatFileProcessor<T> implements SeasonDataDao<T> {

    private static final Logger log = LoggerFactory.getLogger(AbstractSeasonDataFlatFileDao.class);

    private Map<Integer, Map<String, T>> database;

    protected Map<Integer, Map<String, T>> getDatabase() {
        if (database == null) {
            database = new HashMap<>();
        }
        return database;
    }

    /**
     * Common code. Reads the specified file and invokes the FileProcessor that is provided
     * by the caller to support that. Upon successful completion, this method has decorated
     * the Season Data object for the type of data contained in the file with the values
     * from the file for the specified year.
     */
    protected <T extends AbstractSeasonData> void processSeasonDataFile(final Integer year,
                                                                        final String seasonDataFileName,
                                                                        final Map<String, T> teamSeasonDataMap,
                                                                        final FileProcessor<T> fileProcessor) {
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
                        T seasonData = fileProcessor.parseAndProcessFileLine(line, teamSeasonDataMap);
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

}
