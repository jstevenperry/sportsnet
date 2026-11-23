/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Base class for all SeasonData objects, for all sports.
 */
@SuppressWarnings("UnusedReturnValue")
public abstract class SeasonData {
    private Integer year;
    private String teamName;
    //
    private BigDecimal wonLostPercentage;
    private double simpleRatingSystem;

    /**
     * This tells the framework how many fields the SeasonData implementation has.
     */
    public abstract int getNumberOfFields();

    @SuppressWarnings("unused")  // Framework classes do not need this inspection
    protected static Double asDouble(final int intValue) {
        return Integer.valueOf(intValue).doubleValue();
    }

    @SuppressWarnings("unused")  // Framework classes do not need this inspection
    protected static Double asDouble(final double doubleValue) {
        return doubleValue;
    }

    public Integer getYear() {
        return year;
    }

    public SeasonData setYear(final Integer year) {
        this.year = year;
        return this;
    }

    public String getTeamName() {
        return teamName;
    }

    public SeasonData setTeamName(final String teamName) {
        this.teamName = teamName;
        return this;
    }

    @SuppressWarnings("unused")  // Framework classes do not need this inspection
    public BigDecimal getWonLostPercentage() {
        if (wonLostPercentage == null) {
            wonLostPercentage = BigDecimal.ZERO;
        }
        return wonLostPercentage;
    }

    @SuppressWarnings("unused")  // Framework classes do not need this inspection
    public SeasonData setWonLostPercentage(final BigDecimal wonLostPercentage) {
        this.wonLostPercentage = wonLostPercentage;
        return this;
    }

    @SuppressWarnings("unused")  // Framework classes do not need this inspection
    public double getSimpleRatingSystem() {
        return simpleRatingSystem;
    }

    @SuppressWarnings("unused")  // Framework classes do not need this inspection
    public SeasonData setSimpleRatingSystem(final double simpleRatingSystem) {
        this.simpleRatingSystem = simpleRatingSystem;
        return this;
    }

    /**
     * Transform the data from Double to Strings.
     * Pull only the data the user has selected, which is contained in the
     * NetworkParameters.selectedElements property.
     *
     * @param data     The input data (Double objects)
     * @param training true if the row of data is used for training, false if not.
     * @return String[] that contains only the data elements the user wants
     * to include to train, evaluate, and run the network.
     * <p>
     */
    public String[] transformRow(final List<Double> data, final boolean training) {
        //
        // New array contains all of the data, including the label (win/loss)
        // The selected elements describes the data once, but the List<Double> contains two sets of data: home and away

        String[] ret = (training)
                // Only include a spot for a label if data used for training
                ? new String[getNumberOfFields() + 1]
                // No label required
                : new String[getNumberOfFields()];
        int index = 0;
        for (int aa = 0; aa < getNumberOfFields(); aa++) {
            ret[index++] = String.valueOf(data.get(aa)); // Home
        }
        if (training) {
            // Set the label (that is, the win/loss value AS AN INTEGER- THIS IS VERY IMPORTANT)
            // It is located in the very last spot in the incoming List<Double>
            ret[index] = String.valueOf(data.get(data.size() - 1).intValue());
        }
        return ret;
    }

}
