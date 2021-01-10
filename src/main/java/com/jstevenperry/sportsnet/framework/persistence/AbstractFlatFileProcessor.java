package com.jstevenperry.sportsnet.framework.persistence;

import com.jstevenperry.sportsnet.framework.model.AbstractSeasonData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.StringTokenizer;

public abstract class AbstractFlatFileProcessor<T extends AbstractSeasonData> {

    private static final Logger log = LoggerFactory.getLogger(AbstractFlatFileProcessor.class);

    public AbstractFlatFileProcessor() {
        loadData();
    }

    /**
     * Load data associated with the Flat File subclass
     */
    protected abstract void loadData();

    /**
     * Return a String array containing the names of the tournament participant
     * file names supported by the subclass implementation.
     */
    protected abstract String[][] getDataFileNames();

    /**
     * Returns the number of fields in each line of the tournament participant file.
     */
    protected int getNumberOfFields() {
        return 2;
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
