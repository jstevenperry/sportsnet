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
import java.util.Map;
import java.util.StringTokenizer;

public abstract class AbstractFlatFileProcessor<T extends AbstractSeasonData> {

    private static final Logger log = LoggerFactory.getLogger(AbstractFlatFileProcessor.class);

    /**
     * Common code. Reads the specified file and invokes the FileProcessor that is provided
     * by the caller to support that. Upon successful completion, this method has decorated
     * the Season Data object for the type of data contained in the file with the values
     * from the file for the specified year.
     */
    protected static <T extends AbstractSeasonData> void processSeasonDataFile(final Integer year,
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

    /**
     * Parses the specified string, which must be in HH:MM format (where HH is the number
     * of hours, and MM is the number of minutes), and returns the number of minutes as
     * a double precision floating point number.
     */
    protected double parseHourMinuteAsDouble(final String hourMinuteString) {
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

}
