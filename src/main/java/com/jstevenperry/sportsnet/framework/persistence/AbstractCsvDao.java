/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.persistence;

import com.jstevenperry.sportsnet.framework.dto.ResourceFileWrapperDto;
import com.jstevenperry.sportsnet.framework.model.SeasonData;
import com.jstevenperry.sportsnet.framework.util.AbstractNetworkUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;

/**
 * Contains common code for Data Access Objects that consume CSV data.
 *
 * @param <T> - the type of Season/Era data used by the implementing subclasses.
 */
@SuppressWarnings("unused")  // Framework classes do not need this inspection
public abstract class AbstractCsvDao<T extends SeasonData> {

    private static final Logger log = LoggerFactory.getLogger(AbstractCsvDao.class);

    private final AbstractNetworkUtils networkUtils;

    public AbstractCsvDao(final AbstractNetworkUtils networkUtils) {
        this.networkUtils = networkUtils;
    }

    /**
     * Return a String array containing the names of the tournament participant
     * file names supported by the subclass implementation.
     */
    protected abstract String[][] getDataFileNames();

    /**
     * Returns the number of fields in each line of the tournament participant file.
     */
    protected abstract int getNumberOfFields();

    /**
     * Fetch and return the contents of the specified file as a 2D String array, or an empty
     * optional if the file could not be located at the specified coordinates.
     *
     * @param dataFileCoordinates The data file's "coordinates" a purposely vague
     *                            term, most likely a URL at the resource service,
     *                            (but it's up to the implementing subclass what the file's
     *                            "coordinates" translate to).
     * @return The contents of the specified CSV file as a 2D String array, or an empty
     * optional if the file could not be found at the specified coordinates.
     */
    protected Optional<String[][]> fetchCsvData(final String dataFileCoordinates) {
        final Optional<String[][]> csvData;
        Optional<ResourceFileWrapperDto> resourceFileWrapperDtoOptional = networkUtils.fetchDataFileFromResourceServer(dataFileCoordinates);
        csvData = resourceFileWrapperDtoOptional.map(ResourceFileWrapperDto::getFileContentsAsDoubleStringArray);
        return csvData;
    }

    /**
     * Return the years that are valid for training and evaluation so the user doesn't
     * try to train a network for year(s) for which data is not available.
     * <p>
     * Uses the year specified in the data files array as an optimization. Fetching
     * this information from the resource service is potentially very expensive, but
     * should probably be moved there anyway at some point once the resource service
     * is running tip-top.
     *
     * @return A List of years as described above.
     */
    public List<Integer> getYearsForTrainingAndEvaluation() {
        List<Integer> ret = new ArrayList<>();
        for (String[] dataFilesByYear : getDataFileNames()) {
            String year = dataFilesByYear[0];
            ret.add(Integer.parseInt(year));
        }
        return ret;
    }

    /**
     * Parses the specified string, which must be in HH:MM format (where HH is the number
     * of hours, and MM is the number of minutes), and returns the number of minutes as
     * a double precision floating point number.
     */
    @SuppressWarnings("unused")  // Framework classes do not need this inspection
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

    protected boolean lineEmpty(final String[] line) {
        return line == null ||
                StringUtils.isEmpty(line[0]);
    }

    protected boolean lineNotEmpty(final String[] line) {
        return !lineEmpty(line);
    }

}
