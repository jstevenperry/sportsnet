/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */
package com.jstevenperry.sportsnet.framework.persistence;

import com.jstevenperry.sportsnet.framework.model.AbstractSeasonData;
import com.jstevenperry.sportsnet.ncaabb.model.NcaaBbSeasonData;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Data Access Object interface for SeasonData model data.
 *
 * @author J Steven Perry
 */
@Component
public interface SeasonDataDao<T extends AbstractSeasonData> {

    /**
     * Fetch all {@link NcaaBbSeasonData} objects by the specified <code>year</code>
     * and return a <code>List<SeasonData></code>. The <code>List</code>
     * will be empty if no {@link NcaaBbSeasonData} objects could be found for
     * the specified year (which probably means you forgot to load the
     * data into the DB, shame, shame, shame).
     *
     * @param year The year for which {@link NcaaBbSeasonData} objects are to be
     *             fetched.
     * @return <code>List<SeasonData></code> - the <code>List</code> of
     * {@link NcaaBbSeasonData} objects if they could be located, or an empty list
     * if not.
     */
    List<T> fetchAllByYear(Integer year);

    /**
     * Fetch the {@link NcaaBbSeasonData} object associated with the specified
     * <code>year</code> and <code>teamName</code>. This combination is
     * guaranteed to be unique in the system.
     *
     * @param year     The year for which the {@link NcaaBbSeasonData} object is to be
     *                 fetched.
     * @param teamName The team name for which the {@link NcaaBbSeasonData} object
     *                 is to be fetched.
     * @return {@link NcaaBbSeasonData} - The {@link NcaaBbSeasonData} object associated
     * with the unique combination of <code>year</code> and <code>teamName</code>
     * or <code>null</code> if no such object could be found.
     */
    T fetchByYearAndTeamName(Integer year, String teamName);

}
