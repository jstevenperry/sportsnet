/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet.framework;

import com.jstevenperry.sportsnet.framework.model.AbstractSeasonData;
import com.jstevenperry.sportsnet.framework.model.NetworkParameters;
import com.jstevenperry.sportsnet.framework.model.TournamentParticipant;
import com.jstevenperry.sportsnet.framework.model.TournamentResult;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.framework.persistence.TournamentParticipantDao;
import com.jstevenperry.sportsnet.framework.persistence.TournamentResultDao;
import com.opencsv.CSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractNetworkBase<T extends AbstractSeasonData> {

    private static final Logger log = LoggerFactory.getLogger(AbstractNetworkBase.class);

    private final Map<Integer, List<TournamentResult>> tournamentYearResultsMap = new HashMap<>();
    private final Map<Integer, Map<String, T>> tournamentYearSeasonDataMap = new HashMap<>();

    public abstract List<Double> seasonDataAsDoubleList(final T seasonData,
                                                        final Double resultClass,
                                                        final boolean isTraining);

    private List<TournamentResult> getTournamentResults(final Integer tournamentYear) {
        return tournamentYearResultsMap.computeIfAbsent(tournamentYear, k -> new ArrayList<>());
    }

    private Map<String, T> getTeamSeasonData(final Integer tournamentYear) {
        return tournamentYearSeasonDataMap.computeIfAbsent(tournamentYear, k -> new HashMap<>());
    }

    public List<Double> seasonDataAsDoubleList(final T seasonData) {
        return seasonDataAsDoubleList(seasonData, null, false);
    }

    protected T pullSeasonData(final Integer year, final String teamName, final SeasonDataDao<T> seasonDataDao) {
        T seasonData = getTeamSeasonData(year).get(teamName);
        if (seasonData == null) {
            log.debug(String.format("Reading %s %d season data from DB...", teamName, year));
            seasonData = seasonDataDao.fetchByYearAndTeamName(year, teamName);
            getTeamSeasonData(year).put(teamName, seasonData);
        }
        return seasonData;
    }

    protected List<TournamentResult> pullTournamentResults(final Integer year, final TournamentResultDao tournamentResultDao) {
        List<TournamentResult> ret = getTournamentResults(year);
        if (ret.isEmpty()) {
            log.debug(String.format("Reading %d tournament results from DB...", year));
            ret.addAll(tournamentResultDao.fetchAllByYear(year));
        }
        return ret;
    }

    public String createCsvData(final List<Integer> yearsToPull,
                                final SeasonDataDao<T> seasonDataDao,
                                final TournamentResultDao tournamentResultDao,
                                final TournamentParticipantDao tournamentParticipantDao,
                                final NetworkParameters networkParameters) {
        StringWriter stringWriter = new StringWriter();
        CSVWriter csvWriter = new CSVWriter(stringWriter);
        for (Integer year : yearsToPull) {
            List<TournamentResult> tournamentResults = pullTournamentResults(year, tournamentResultDao);
            Collections.shuffle(tournamentResults); // Shake things up a little
            for (TournamentResult tournamentResult : tournamentResults) {
                String winningTeamName = tournamentResult.getWinningTeamName();
                T seasonDataWinning = pullSeasonData(year, winningTeamName, seasonDataDao);
                if (seasonDataWinning == null) {
                    String message = String.format("Could not locate season data for team %s", winningTeamName);
                    log.error(message);
                    throw new RuntimeException(message);
                }
                TournamentParticipant tournamentParticipant = tournamentParticipantDao.fetchByYear(winningTeamName, year);
                //
                // Transform the data, then write out the data
                List<Double> rowWinDouble = seasonDataAsDoubleList(seasonDataWinning, tournamentParticipant.getNumberOfVictories().doubleValue(), true);
                String[] rowWinString = networkParameters.transformRow(rowWinDouble, true);
                csvWriter.writeNext(rowWinString);
                log.debug(String.format("Winning Team: %s (%d), data: %s", winningTeamName, tournamentParticipant.getNumberOfVictories(), Arrays.toString(rowWinString)));
                // Losing team
                String losingTeamName = tournamentResult.getLosingTeamName();
                T seasonDataLosing = pullSeasonData(year, losingTeamName, seasonDataDao);
                if (seasonDataLosing == null) {
                    String message = String.format("Could not locate season data for team %s", losingTeamName);
                    log.error(message);
                    throw new RuntimeException(message);
                }
                tournamentParticipant = tournamentParticipantDao.fetchByYear(losingTeamName, year);
                //
                // Transform the data, then write out the data
                List<Double> rowLossDouble = seasonDataAsDoubleList(seasonDataLosing, tournamentParticipant.getNumberOfVictories().doubleValue(), true);
                String[] rowLossString = networkParameters.transformRow(rowLossDouble, true);
                csvWriter.writeNext(rowLossString);
                log.debug(String.format("Losing Team: %s (%d), data: %s", losingTeamName, tournamentParticipant.getNumberOfVictories(), Arrays.toString(rowLossString)));
            }
        }
        //
        // Get the underlying String and via dl4j's arcane class structure transform it into a DataSet and return it
        StringBuffer stringBuffer = stringWriter.getBuffer();
        return stringBuffer.toString();
    }

}
