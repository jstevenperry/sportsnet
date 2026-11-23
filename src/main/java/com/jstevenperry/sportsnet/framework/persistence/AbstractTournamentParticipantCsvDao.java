/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.persistence;

import com.jstevenperry.sportsnet.framework.config.AbstractSportsnetProperties;
import com.jstevenperry.sportsnet.framework.dto.ResourceFileWrapperDto;
import com.jstevenperry.sportsnet.framework.exception.InternalServerErrorException;
import com.jstevenperry.sportsnet.framework.exception.NotFoundException;
import com.jstevenperry.sportsnet.framework.model.ResourceType;
import com.jstevenperry.sportsnet.framework.model.SeasonData;
import com.jstevenperry.sportsnet.framework.model.TournamentParticipant;
import com.jstevenperry.sportsnet.framework.util.AbstractNetworkUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings({"unused", "UnusedReturnValue"}) // Class is heavily used by clients, suppress any (probably spurious) warnings
public abstract class AbstractTournamentParticipantCsvDao<T extends SeasonData> extends AbstractCsvDao<T> implements TournamentParticipantDao {

    private static final Logger log = LoggerFactory.getLogger(AbstractTournamentParticipantCsvDao.class);

    private final AbstractSportsnetProperties sportsnetProperties;
    private final AbstractNetworkUtils networkUtils;

    private Map<Integer, Map<String, TournamentParticipant>> database;

    public AbstractTournamentParticipantCsvDao(final AbstractNetworkUtils networkUtils,
                                               final AbstractSportsnetProperties sportsnetProperties) {
        super(networkUtils);
        this.networkUtils = networkUtils;
        this.sportsnetProperties = sportsnetProperties;
    }

    private Map<Integer, Map<String, TournamentParticipant>> getDatabase() {
        if (database == null) {
            database = new HashMap<>();
        }
        return database;
    }

    /**
     * Return the sports-specific URI used by the implementing subclass.
     */
    protected abstract String getSportsSpecificUrl();

    /**
     * Loads the tournament participant data for the specified tournament year.
     *
     * @param database       The database in which the data will be stored, think of it as a cache so the data
     *                       only has to be fetched once from the resource server, as this is a potentially
     *                       expensive operation.
     * @param tournamentYear The tournament year, such as 2019
     * @return A map of tournament participant team name to the tournament participant data for that team
     * @throws InternalServerErrorException - COREATPCD0011E if the format of the file returned from the resource service is not what is expected
     * @throws NotFoundException            - COREATPCD0013E if the specified file could not be found by the resource service
     */
    protected Map<String, TournamentParticipant> loadData(final Map<Integer, Map<String, TournamentParticipant>> database,
                                                          final Integer tournamentYear) {
        Map<String, TournamentParticipant> tournamentParticipantMap = null;
        for (final String[] tournamentEntry : getDataFileNames()) {
            Integer year = Integer.parseInt(tournamentEntry[0]);
            if (tournamentYear.equals(year)) {
                String dataFileName = tournamentEntry[1];
                String dataFileNameUrl = networkUtils.computeDataFileUrl(sportsnetProperties, getSportsSpecificUrl(), tournamentYear, dataFileName, ResourceType.TOURNAMENT_PARTICIPANT_FILE);
                Optional<ResourceFileWrapperDto> dataFileMaybe = networkUtils.fetchDataFileFromResourceServer(dataFileNameUrl);
                if (dataFileMaybe.isPresent()) {
                    String[][] dataFileContents = dataFileMaybe.get().getFileContentsAsDoubleStringArray();
                    int skipCount = 0;
                    for (String[] line : dataFileContents) {
                        if (skipCount++ < 1) {
                            continue; // skip header
                        }
                        if (lineNotEmpty(line)) { // skip line if empty
                            if (line.length == getNumberOfFields()) {
                                TournamentParticipant data = parseLine(year, line);
                                tournamentParticipantMap = database.computeIfAbsent(year, k -> new HashMap<>());
                                tournamentParticipantMap.put(data.getTeamName(), data);
                            } else {
                                String errorMessage = String.format("COREATPCD0011E: File %s: Expected %d fields, got %d instead.", dataFileName, getNumberOfFields(), line.length);
                                log.error("loadData(): {}", errorMessage);
                                throw new InternalServerErrorException(errorMessage);
                            }
                        }
                    }
                    break;
                } else {
                    String errorMessage = String.format("COREATPCD0013E: The specified file could not be found on the resource server: %s", dataFileNameUrl);
                    log.error("loadData(): {}", errorMessage);
                    throw new NotFoundException(errorMessage);
                }
            }
        }
        return tournamentParticipantMap;
    }

    public Map<String, TournamentParticipant> getTournamentParticipantMap(final Integer tournamentYear) {
        //
        // See if the map already exists
        Map<String, TournamentParticipant> tournamentParticipantMap = getDatabase().get(tournamentYear);
        if (tournamentParticipantMap == null) {
            //
            // Nope, so load it
            loadData(getDatabase(), tournamentYear);
            //
            // Try again
            tournamentParticipantMap = getDatabase().get(tournamentYear);
        }
        //
        // Return the map to the caller
        return tournamentParticipantMap;
    }

    /**
     * The number of fields in the standard tournament participant file.
     * Subclasses will override if their sport differs.
     */
    protected int getNumberOfFields() {
        return 2;
    }

    protected TournamentParticipant parseLine(final Integer year, final String[] line) {
        TournamentParticipant ret = new TournamentParticipant();
        ret.setYear(year);
        ret.setTeamName(StringUtils.remove(StringUtils.remove(StringUtils.trim(line[0]), '*'), '.'));
        ret.setNumberOfVictories(Integer.parseInt(line[1]));
        return ret;
    }

    @Override
    public TournamentParticipant fetchByYear(final String teamName, final Integer year) {
        TournamentParticipant ret = null;
        Map<String, TournamentParticipant> teamDataMap = getTournamentParticipantMap(year);
        if (teamDataMap != null) {
            ret = teamDataMap.get(teamName);
        }
        return ret;
    }

    @Override
    public List<TournamentParticipant> fetchAllByYear(final Integer year) {
        final List<TournamentParticipant> tournamentParticipants;
        Map<String, TournamentParticipant> tournamentParticipantMap = getTournamentParticipantMap(year);
        Set<String> teamNames = tournamentParticipantMap.keySet();
        tournamentParticipants = teamNames.stream()
                .map(tournamentParticipantMap::get)
                .collect(Collectors.toList());
        return tournamentParticipants;
    }

    @Override
    public List<Integer> getYearsForTrainingAndEvaluation() {
        //
        // Data Files looks like this:
        // String[][] = .... { "2016", "tournament-participant.csv"}, {"2017", "tournament-participant.csv"), ....
        String[][] dataFileNames = getDataFileNames();
        //
        // The first dimension in each String[] is an array of years
        List<Integer> yearsForTrainingAndEvaluation = new ArrayList<>();
        for (final String[] dataFileName : dataFileNames) {
            String tournamentYear = dataFileName[0];
            yearsForTrainingAndEvaluation.add(Integer.valueOf(tournamentYear));
        }
        return yearsForTrainingAndEvaluation;
    }
}
