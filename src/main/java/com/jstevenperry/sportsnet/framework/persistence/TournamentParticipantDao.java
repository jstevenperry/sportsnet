package com.jstevenperry.sportsnet.framework.persistence;

import com.jstevenperry.sportsnet.framework.model.TournamentParticipant;

public interface TournamentParticipantDao {

  /**
   * Fetch and return the TournamentParticipant object for
   * the specified team and year.
   */
  TournamentParticipant fetchByYear(final String teamName, final Integer year);
}
