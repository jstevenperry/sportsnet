/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.persistence;

import com.jstevenperry.sportsnet.framework.exception.InternalServerErrorException;
import com.jstevenperry.sportsnet.framework.model.SeasonData;
import com.jstevenperry.sportsnet.framework.util.AbstractNetworkUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@SuppressWarnings("unused") // Class is heavily used by clients, suppress this spurious warning
public abstract class AbstractSeasonDataCsvDao<T extends SeasonData> extends AbstractCsvDao<T> implements SeasonDataDao<T> {

    private static final Logger log = LoggerFactory.getLogger(AbstractSeasonDataCsvDao.class);

    private final AbstractNetworkUtils networkUtils;
    private Map<Integer, Map<String, T>> database;

    public AbstractSeasonDataCsvDao(final AbstractNetworkUtils networkUtils) {
        super(networkUtils);
        this.networkUtils = networkUtils;
    }

    private Map<Integer, Map<String, T>> getDatabase() {
        if (database == null) {
            synchronized (AbstractSeasonDataCsvDao.class) {
                database = new ConcurrentHashMap<>(new HashMap<>());
                log.info("getDatabase: Created new ConcurrentHashMap...");
            }
        }
        return database;
    }

    protected abstract Map<String, T> loadData(final Map<Integer, Map<String, T>> database,
                                               final Integer year);

    public Map<String, T> getSeasonDataMap(final Integer seasonDataYear) {
        //
        // See if the map already exists
        Map<String, T> seasonDataMap = getDatabase().get(seasonDataYear);
        if (seasonDataMap == null) {
            //
            // Nope, so load it
            seasonDataMap = loadData(getDatabase(), seasonDataYear);
        }
        //
        // Return the map to the caller
        return seasonDataMap;
    }

    /**
     * Common code. Reads the specified file and invokes the FileProcessor that is provided
     * by the caller to support that. Upon successful completion, this method has decorated
     * the Season Data object for the type of data contained in the file with the values
     * from the file for the specified year.
     */
    protected void processSeasonDataFile(final Integer year,
                                         final String seasonDataFileNameUrl,
                                         final Map<String, T> teamSeasonDataMap,
                                         final FileProcessor<T> fileProcessor) {
        if (seasonDataFileNameUrl.endsWith(fileProcessor.getRequiredFilenameEnding())) { // Sanity check
//                Optional<String> seasonDataFileMaybe = networkUtils.fetchDataFileFromResourceServer(seasonDataFileNameUrl);
            Optional<String[][]> seasonDataFileMaybe = fetchCsvData(seasonDataFileNameUrl);
            if (seasonDataFileMaybe.isPresent()) {
                String[][] seasonDataFileContents = seasonDataFileMaybe.get();
                int skipCount = 0;
                int numberOfDataLinesProcessed = 0;
                for (String[] line : seasonDataFileContents) {
                    // Skip header lines
                    if (skipCount++ < fileProcessor.getNumberOfHeaderLines()) {
                        continue; // Skip header
                    }
                    if (line == null) {
                        continue; // skip null lines
                    }
                    if (line.length == fileProcessor.getNumberOfFields()) {
                        T seasonData = fileProcessor.parseAndProcessFileLine(line, teamSeasonDataMap);
                        seasonData.setYear(year);
                    } else {
                        String errorMessage = String.format(
                                "COREASDCD0011E: processFile(%d, %s): Expected %d fields, got %d instead.",
                                year, seasonDataFileNameUrl, fileProcessor.getNumberOfFields(), line.length);
                        log.error("processSeasonDataFile({},{}...): {}", year, seasonDataFileNameUrl, errorMessage);
                        throw new InternalServerErrorException(errorMessage);
                    }
                    numberOfDataLinesProcessed++;
                }
                log.debug("processFile({}, {}, ...) Processed {} lines of data.", year, seasonDataFileNameUrl,
                        numberOfDataLinesProcessed);
            } else {
                String errorMessage = String.format(
                        "COREASDCD0013E: The specified file could not be found on the resource server: %s",
                        seasonDataFileNameUrl);
                log.error("processSeasonDataFile(): {}", errorMessage);
                throw new RuntimeException(errorMessage);
            }
        } else {
            String errorMessage = String.format(
                    "COREASDCD0015E: processFile(%d, %s): This (%s) is not the file you're looking for (correct file ends with '%s')",
                    year, seasonDataFileNameUrl, seasonDataFileNameUrl, fileProcessor.getRequiredFilenameEnding());
            log.error("processSeasonDataFile(): {}", errorMessage);
            throw new RuntimeException(errorMessage);
        }
    }

    protected String nice(final String stringToNice) {
        return networkUtils.nice(stringToNice);
    }

    protected T seasonDataComputeIfAbsent(final String teamName, final Map<String, T> seasonDataMap, final Function<Void, T> seasonDataCreator) {
        return seasonDataMap.computeIfAbsent(teamName, teamNameKey -> {
            // Season data not found for this team, create it and set team name (k, the key to the map)
            T seasonData = seasonDataCreator.apply(null);
            seasonData.setTeamName(teamNameKey);
            return seasonData;
        });
    }

}
