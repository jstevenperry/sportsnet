/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */
package com.jstevenperry.sportsnet.framework.persistence;

import com.jstevenperry.sportsnet.framework.model.TournamentResult;

import java.util.List;

/**
 * Data Access Object interface for {@link TournamentResult} model data.
 *
 * @author J Steven Perry
 */
public interface TournamentResultDao {

    /**
     * Fetch all {@link TournamentResult} objects by the specified <code>year</code>
     * and return a <code>List<TournamentResult></code>. The <code>List</code>
     * will be empty if no {@link TournamentResult} objects could be found for
     * the specified year (which probably means you forgot to load the
     * data into the DB, shame, shame, shame).
     *
     * @param year The year for which {@link TournamentResult} objects are to be
     *             fetched.
     * @return <code>List<TournamentResult></code> - the <code>List</code> of
     * {@link TournamentResult} objects if they could be located, or an empty list
     * if not.
     */
    public List<TournamentResult> fetchAllByYear(Integer year);

}
