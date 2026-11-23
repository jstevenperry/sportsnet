/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.persistence;

import com.jstevenperry.sportsnet.framework.model.TournamentParticipant;

import java.util.List;

public interface TournamentParticipantDao {

    /**
     * Fetch and return the TournamentParticipant object for
     * the specified team and year.
     */
    @SuppressWarnings("unused")  // Framework classes do not need this inspection
    TournamentParticipant fetchByYear(final String teamName, final Integer year);

    /**
     * Fetch and return all TournamentParticipant objects for the specified year.
     */
    List<TournamentParticipant> fetchAllByYear(final Integer year);

    /**
     * Fetch years for training and evaluation using the years for which we have
     * tournament participant data, which is used to evaluate the networks that
     * have been trained. Without tournament participant data, it is impossible
     * to perform supervised learning.
     */
    List<Integer> getYearsForTrainingAndEvaluation();

    Integer getNumberOfOutputs();
}
