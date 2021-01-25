package com.jstevenperry.sportsnet.framework.persistence;

import com.jstevenperry.sportsnet.framework.model.TournamentParticipant;

import java.util.List;

public interface TournamentParticipantDao {

    /**
     * Fetch and return the TournamentParticipant object for
     * the specified team and year.
     */
    TournamentParticipant fetchByYear(final String teamName, final Integer year);

    /**
     * Fetch and return a list of all valid tournament years.
     */
    List<Integer> fetchValidTournamentYears();
}
