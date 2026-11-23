/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */
package com.jstevenperry.sportsnet.framework.persistence;

import com.jstevenperry.sportsnet.framework.model.SeasonData;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Data Access Object interface for SeasonData model data.
 *
 * @author J Steven Perry
 */
@Component
public interface SeasonDataDao<T extends SeasonData> {

    /**
     * Fetch all SeasonData objects by the specified <code>year</code>
     * and return a <code>List<SeasonData></code>. The <code>List</code>
     * will be empty if no {@link SeasonData} objects could be found for
     * the specified year (which probably means you forgot to load the
     * data into the DB, shame, shame, shame).
     *
     * @param year The year for which {@link SeasonData} objects are to be
     *             fetched.
     * @return <code>List<SeasonData></code> - the <code>List</code> of
     * {@link SeasonData} objects if they could be located, or an empty list
     * if not.
     */
    List<T> fetchAllByYear(Integer year);

    /**
     * Fetch the {@link SeasonData} object associated with the specified
     * <code>year</code> and <code>teamName</code>. This combination is
     * guaranteed to be unique in the system.
     *
     * @param year     The year for which the {@link SeasonData} object is to be
     *                 fetched.
     * @param teamName The team name for which the {@link SeasonData} object
     *                 is to be fetched.
     * @return {@link SeasonData} - The {@link SeasonData} object associated
     * with the unique combination of <code>year</code> and <code>teamName</code>
     * or <code>null</code> if no such object could be found.
     */
    T fetchByYearAndTeamName(Integer year, String teamName);

    /**
     * Fetch the years available for training and evaluation using the season
     * data that is managed by the implementation.
     */
    List<Integer> getYearsForTrainingAndEvaluation();

    Integer getNumberOfInputs();

}
