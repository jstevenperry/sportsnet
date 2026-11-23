/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.service;

import com.jstevenperry.sportsnet.framework.config.AbstractSportsnetProperties;
import com.jstevenperry.sportsnet.framework.dto.NetworkCandidateDto;
import com.jstevenperry.sportsnet.framework.dto.ResourceFileWrapperDto;
import com.jstevenperry.sportsnet.framework.dto.TournamentSimulationDto;
import com.jstevenperry.sportsnet.framework.exception.BadRequestException;
import com.jstevenperry.sportsnet.framework.exception.InternalServerErrorException;
import com.jstevenperry.sportsnet.framework.exception.NotFoundException;
import com.jstevenperry.sportsnet.framework.model.*;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.framework.persistence.TournamentParticipantDao;
import com.jstevenperry.sportsnet.framework.util.AbstractNetworkProperties;
import com.jstevenperry.sportsnet.framework.util.AbstractNetworkUtils;
import com.opencsv.CSVWriter;
import org.apache.commons.lang3.StringUtils;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.InputStreamInputSplit;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.shade.guava.io.CharStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This is the base class for running networks for a specific tournament.
 * This class is subclassed and key methods are overridden to create a
 * tournament-specific plugin that can be used to make predictions.
 */
@SuppressWarnings("unused")
public abstract class AbstractTournamentRunnerService<T extends SeasonData> extends AbstractNetworkService<T> {

    private static final Logger log = LoggerFactory.getLogger(AbstractTournamentRunnerService.class);

    public static final String LOG_PREFIX = "COREATRS";

    private static final int BATCH_SIZE = 2000;

    /**
     * Caches the results for each network. For each network there is a map of the
     * results of that network for each team: key = team name, value = network results.
     * This means for each network, the results for a team are computed only once, no
     * matter how many times that team's results need to be fetched (they do not change
     * for a given network).
     */
    //private SimpleCache<UUID, Map<String, double[]>> networkResultsCache;

//    public SimpleCache<UUID, Map<String, double[]>> getNetworkResultsCache() {
//        return networkResultsCache;
//    }

    private final JumpBall posessionArrow = new JumpBall();

    private final AbstractNetworkUtils networkUtils;
    private final AbstractSportsnetProperties sportsnetProperties;
    private final AbstractNetworkProperties networkProperties;

    private final Map<Integer, AbstractNetworkRunnerPlugin<T>> pluginMap = new ConcurrentHashMap<>();

    /**
     * Season data for all teams. This also sets the order of everything that is done.
     * Maps are used as optimizations throughout, but Maps do not retain order. So
     * when pulling from maps by team name, use this list to do so or the order will
     * be lost. This is not so bad when the team name is handy, but when dealing with
     * normalized data it is impossible to tell which set of data goes with which team.
     * That's when the order established by this list matters most.
     */
    private final Map<Integer, List<T>> tournamentYearSeasonDataMap = new ConcurrentHashMap<>();

    private final Map<Integer, Map<String, String>> tournamentYearTeamCsvDataMap = new ConcurrentHashMap<>(); // optimization, populated once per team. Savings: NumberOfTournamentTeams * NumberOfNetworks * (NumberOfTournamentGames - 1)

    private Map<TeamCoordinate, T> teamCoordinateSeasonDataMap;

    private final Map<String, TeamCoordinate> teamRankingsMap = new HashMap<>();
    public Map<String, TeamCoordinate> getTeamRankingsMap() {
        log.debug("Returning teamRankingsMap... ");
        return teamRankingsMap;
    }

    public AbstractTournamentRunnerService(final SeasonDataDao<T> seasonDataDao,
                                           final TournamentParticipantDao tournamentParticipantDao,
                                           final AbstractNetworkUtils networkUtils,
                                           final AbstractSportsnetProperties sportsnetProperties,
                                           final AbstractNetworkProperties networkProperties) {
        super(seasonDataDao, tournamentParticipantDao, networkUtils, sportsnetProperties, networkProperties);
        this.networkUtils = networkUtils;
        this.sportsnetProperties = sportsnetProperties;
        this.networkProperties = networkProperties;
        //
        // Initialize all caches used
        initializeCaches(networkProperties);
    }

    @Override
    protected void initializeCaches(final AbstractNetworkProperties networkProperties) {
        super.initializeCaches(networkProperties);
        //this.networkResultsCache = new SimpleCache<>(networkProperties);
    }

    @Override
    protected void cleanup() {
        super.cleanup();
        //this.networkResultsCache.cleanup();
        setTeamCoordinateSeasonDataMap(new HashMap<>());
        teamRankingsMap.clear();
        tournamentYearTeamCsvDataMap.clear();
        tournamentYearSeasonDataMap.clear();
        teamCoordinateSeasonDataMap.clear();

        log.info("cleanup: All maps cleaned up...");
    }

    public static Optional<GameCoordinate> parseTournamentTemplateLine(final String line) {
        Optional<GameCoordinate> ret;
        // A line looks like this:
        // [0,0,0],[0,0,1]
        StringTokenizer strtok = new StringTokenizer(line.trim(), "[]vs");
        List<String> coordinates = new ArrayList<>();
        while (strtok.hasMoreElements()) {
            String element = strtok.nextToken();
            coordinates.add(element);
        }
        // There must be two components (home and away), or something is wrong
        if (coordinates.size() == 2) {
            TeamCoordinate homeTeamCoordinates = TeamCoordinate.parseCoordinates(coordinates.get(0));
            TeamCoordinate awayTeamCoordinates = TeamCoordinate.parseCoordinates(coordinates.get(1));
            ret = Optional.of(new GameCoordinate(homeTeamCoordinates, awayTeamCoordinates));
        } else {
            String message = String.format("%s0033E: Cannot process malformed tournament template file line: '%s'", LOG_PREFIX, line);
            log.error(message);
            throw new InternalServerErrorException(message);
        }
        return ret;
    }

    public Map<TeamCoordinate, T> getTeamCoordinateSeasonDataMap() {
        if (teamCoordinateSeasonDataMap == null) {
            String message = String.format("%s0035E: Team Coordinate SeasonData map is null. Did you forget to call setTeamCoordinateSeasonDataMap " +
                    "in the tournament plugin constructor?", LOG_PREFIX);
            log.error("getTeamCoordindateSeasonDataMap(): {}", message);
            throw new InternalServerErrorException(message);
        }
        return teamCoordinateSeasonDataMap;
    }

    @SuppressWarnings("unused")  // Framework classes do not need this inspection
    public void setTeamCoordinateSeasonDataMap(final Map<TeamCoordinate, T> teamCoordinateSeasonDataMap) {
        this.teamCoordinateSeasonDataMap = teamCoordinateSeasonDataMap;
    }

    @SuppressWarnings("unused") // Used by clients, suppress this spurious warning
    public String getDescription() {
        return StringUtils.EMPTY;
    }

    public Optional<NetworkCandidate> fetchNetworkById(final UUID networkId) {
        Optional<NetworkCandidate> ret;
        // Use the resource server to load the network file
        final String networkFileUrl = getNetworkUtils().computeNetworkFileUrl(sportsnetProperties, getSportsSpecificUri(), networkId, ResourceType.DL4J_NETWORK);
        Optional<NetworkCandidateDto> networkCandidateOptional = getNetworkUtils().fetchNetworkFileFromResourceService(networkFileUrl);
        ret = networkCandidateOptional.map(NetworkCandidate::fromDto);
        return ret;

    }

    public Map<Integer, AbstractNetworkRunnerPlugin<T>> getPluginMap() {
        return pluginMap;
    }

    /**
     * Validate the parameters contained in the specified DTO and throw a BadRequestException
     * if they are not valid. A successful return from this method indicates that the
     * DTO is valid.
     * <p>
     * This method must be implemented by subclasses, which should be a tightly focused
     * service class specific to the network type serviced by that service.
     *
     * @param tournamentSimulationDto The DTO containing the request parameters. While there is
     *                                a single DTO for all network types, the specific conditions
     *                                that make the DTO invalid will vary by network type.
     */
    public void validateParametersOrError(final TournamentSimulationDto tournamentSimulationDto) {
        if (tournamentSimulationDto.getTournamentYear() == null) {
            String errorMessage = String.format("%s0037E: Tournament Year cannot be null", LOG_PREFIX);
            log.error("validateParametersOrError(): {}", errorMessage);
            throw new BadRequestException(errorMessage);
        }
    }

    /**
     * Fetch the plugin for the specified tournament year if it exists. If it does not exist,
     * the Optional will be empty.
     */
    public Optional<NetworkRunnerPlugin<T>> fetchPlugin(final Integer tournamentYear) {
        Optional<NetworkRunnerPlugin<T>> ret = Optional.empty();
        List<Integer> availableTournamentYears = fetchPluginYears();
        if (availableTournamentYears.contains(tournamentYear)) {
            ret = Optional.of(getPluginMap().get(tournamentYear));
        } else {
            log.warn("fetchPlugin(): No plugin for the specified tournament year: {}. Available years: {}",
                    tournamentYear, StringUtils.join(availableTournamentYears, ','));
        }
        return ret;

    }

    /**
     * Fetch the list of years for which this runner has plugins
     */
    public List<Integer> fetchPluginYears() {
        return new ArrayList<>(getPluginMap().keySet());
    }

    /**
     * Run the tournament simulation using the information in the specified TournamentSimulationDto
     * object and return the results of the tournament as a CSV to the caller.
     */
    public Optional<String> runTournament(final TournamentSimulationDto tournamentSimulationDto) {
        Optional<String> ret;
        final ManagementEvent managementEvent = createManagementEvent(ManagementEventType.RUN_SIMULATION_START, "runTournament()");
        Integer tournamentYear = tournamentSimulationDto.getTournamentYear();
        Optional<NetworkRunnerPlugin<T>> networkRunnerPluginOptional = fetchPlugin(tournamentYear);

        if (networkRunnerPluginOptional.isPresent()) {
            NetworkRunnerPlugin<T> networkRunnerPlugin = networkRunnerPluginOptional.get();
            final String tournamentOutput;
            switch (networkRunnerPlugin.getOutputType()) {
                case BRACKET -> tournamentOutput = runTournamentAndProduceBrackets(tournamentSimulationDto.getNetworkIds(), networkRunnerPlugin);
                case TEAM_RANKING -> tournamentOutput = runTournamentAndProduceRankings(tournamentSimulationDto.getNetworkIds(), networkRunnerPlugin);
                default -> {
                    String errorMessage = String.format("%s0139E: Unknown output type: %s, tournament simulation cancelled.",
                            LOG_PREFIX, networkRunnerPlugin.getOutputType());
                    log.error(errorMessage);
                    createManagementEvent(managementEvent, ManagementEventType.EXCEPTION, errorMessage);
                    throw new InternalServerErrorException(errorMessage);
                }
            }
            ret = Optional.of(tournamentOutput);
        } else {
            List<Integer> availablePluginYears = fetchPluginYears().stream().sorted().toList();
            String errorMessage = String.format("%s0041E: No tournament plugin available for year %d, available years are %s",
                    LOG_PREFIX, tournamentYear, StringUtils.join(availablePluginYears, ','));
            log.error("runTournament(): {}", errorMessage);
            createManagementEvent(managementEvent, null, ManagementEventType.EXCEPTION, errorMessage);
            throw new BadRequestException(errorMessage);
        }
        managementEvent.setElapsedTimeMillis(System.currentTimeMillis() - managementEvent.getSystemTimeMillis());
        createManagementEvent(managementEvent, ManagementEventType.RUN_SIMULATION_END, "runTournament()");
        //
        // We're done here. Cleanup on the way out.
        cleanup();
        return ret;
    }

    /**
     * Create the list of games that will be played during the tournament.
     * A game is specified by its GameCoordinate object, since each game
     * can occur in a different round, region, and played by different
     * teams.
     */
    private List<GameCoordinate> createTournamentGameList(final NetworkRunnerPlugin<T> networkRunnerPlugin) {
        List<GameCoordinate> ret = new ArrayList<>();
        String tournamentFileName = networkRunnerPlugin.getTournamentTemplateFileName();
        String tournamentFileNameUrl = getNetworkUtils().computeDataFileUrl(
                sportsnetProperties, getSportsSpecificUri(), networkRunnerPlugin.getTournamentYear(), tournamentFileName, ResourceType.TOURNAMENT_GAME_LIST_FILE);
        Optional<ResourceFileWrapperDto> tournamentFileMaybe = getNetworkUtils().fetchDataFileFromResourceServer(tournamentFileNameUrl);
        if (tournamentFileMaybe.isPresent()) {
            String[][] tournamentFileContents = tournamentFileMaybe.get().getFileContentsAsDoubleStringArray();
            for (String[] line : tournamentFileContents) {
                String gameListFileLine = line[0];
                if (!gameListFileLine.startsWith("#")) {
                    Optional<GameCoordinate> gameCoordinateMaybe = parseTournamentTemplateLine(gameListFileLine);
                    gameCoordinateMaybe.ifPresent(ret::add);
                }
            }
        } else {
            String message = String.format("%s0043E: The specified file could not be found on the resource server: %s", LOG_PREFIX, tournamentFileNameUrl);
            log.error("createTournamentGameList(): {}", message);
            throw new NotFoundException(message);
        }
        return ret;
    }

    /**
     * Using the specified list of GameCoordinates, run the tournament.
     */
    @SuppressWarnings("unused")  // Framework classes do not need this inspection
    protected String runTournamentAndProduceBrackets(final List<UUID> networksToUse,
                                                     final NetworkRunnerPlugin<T> networkRunnerPlugin) {
        final long startTime = System.currentTimeMillis();
        setTeamCoordinateSeasonDataMap(networkRunnerPlugin.buildTeamCoordinateSeasonDataMap());
        String resultsCsv = "NO RESULTS GENERATED";
        //
        // Compute game list
        List<GameCoordinate> gameList = createTournamentGameList(networkRunnerPlugin);
        //
        // Build the Map of network -> Team Results Map (k=team name, v=network results)
        final Map<String, Double> teamTournamentDistanceScoreMap = computeTournamentDistanceMap(networksToUse, networkRunnerPlugin);
        log.debug("runTournament(): Finished running tournament in {}ms", System.currentTimeMillis() - startTime);
        if (teamTournamentDistanceScoreMap.size() > 0) {
            //
            // Generate the brackets
            for (GameCoordinate gameCoordinate : gameList) {
                try {
                    //
                    // Get the home team from the GameCoordinate
                    TeamCoordinate homeTeamCoordinate = gameCoordinate.getHome();
                    T homeTeamSeasonData = getTeamCoordinateSeasonDataMap().get(homeTeamCoordinate);
                    homeTeamCoordinate.setName(homeTeamSeasonData.getTeamName());
                    TeamCoordinate awayTeamCoordinate = gameCoordinate.getAway();
                    T awayTeamSeasonData = getTeamCoordinateSeasonDataMap().get(awayTeamCoordinate);
                    awayTeamCoordinate.setName(awayTeamSeasonData.getTeamName());
                    //
                    // Run all networks for this match up and average the results to compute the winner.
                    computeWinner(teamTournamentDistanceScoreMap, homeTeamCoordinate, homeTeamSeasonData, awayTeamCoordinate, awayTeamSeasonData, networkRunnerPlugin);
                } catch (Exception e) {
                    String errorMessage = String.format("%s0045E: runTournament(): Error while running game: %s", LOG_PREFIX, gameCoordinate);
                    log.error(errorMessage, e);
                    throw new InternalServerErrorException(errorMessage, e);
                }
            }
            //
            // Generate tournament results document
            resultsCsv = visualizeTournamentResults(networkRunnerPlugin);
            //
            // Print team rankings
            printTeamRankingsToLogs(getTeamRankingsMap());
        } else {
            String errorMessage = String.format("%s0047E: Of the %d networks that were supplied, none were found to be valid for tournament year %d",
                    LOG_PREFIX, networksToUse.size(), networkRunnerPlugin.getTournamentYear());
            log.error(errorMessage);
        }

        return resultsCsv;
    }

    private void computeWinner(final Map<String, Double> teamTournamentDistanceScoreMap,
                               final TeamCoordinate homeTeamCoordinate,
                               final T homeTeamSeasonData,
                               final TeamCoordinate awayTeamCoordinate,
                               final T awayTeamSeasonData,
                               final NetworkRunnerPlugin<T> networkRunnerPlugin) {
        final Double homeTeamDistanceScore = teamTournamentDistanceScoreMap.get(homeTeamCoordinate.getName());
        final Double awayTeamDistanceScore = teamTournamentDistanceScoreMap.get(awayTeamCoordinate.getName());
        // Compute the winningTeamCoordinate based on the which team has the most votes (tie goes to home team)
        TeamCoordinate winningTeamCoordinate = computeNextRoundCoordinates(homeTeamCoordinate, homeTeamDistanceScore, awayTeamCoordinate, awayTeamDistanceScore);
        T winnerSeasonData;
        if (homeTeamDistanceScore >= awayTeamDistanceScore) {
            winnerSeasonData = homeTeamSeasonData;
        } else {
            winnerSeasonData = awayTeamSeasonData;
        }
        winningTeamCoordinate.setName(winnerSeasonData.getTeamName());
        getTeamCoordinateSeasonDataMap().put(winningTeamCoordinate, winnerSeasonData);
        if (log.isDebugEnabled()) {
            TeamCoordinate previousCoordinateForWinner =
                    (winningTeamCoordinate.getName().equalsIgnoreCase(homeTeamCoordinate.getName()))
                            ? homeTeamCoordinate : awayTeamCoordinate;
            log.debug("***** WINNER: {} with {} wins, Distance score: {} *****",
                    previousCoordinateForWinner, Math.max(homeTeamDistanceScore, awayTeamDistanceScore),
                    previousCoordinateForWinner.getDistanceScore());
        }
    }

    private Map<String, double[]> loadTeamResults(final NetworkRunnerPlugin<T> networkRunnerPlugin, final NetworkCandidate networkCandidate) {
        try {
            Map<String, double[]> teamResultsMap = new HashMap<>();
            RecordReaderDataSetIterator recordReaderDataSetIterator = computeRecordIterator(networkCandidate.getTrainedNetworkParameters(), networkRunnerPlugin);
            DataNormalization normalizer = networkCandidate.getTrainedNetworkParameters().getNormalizer().getDataNormalization();
            DataSet tournamentData = recordReaderDataSetIterator.next();
            normalizer.fit(tournamentData);
            normalizer.transform(tournamentData);
            MultiLayerNetwork network = networkCandidate.getMultiLayerNetwork();
            // Run the network against the normalized data
            INDArray indArrayResults = network.output(tournamentData.getFeatures());
            // Once again, use the order of allSeasonData to establish the order of the records in the results
            // HUGE ASSUMPTION: the order of records from the iterator matches the order in which they were handed to the iterator!!
            int index = 0;
            double[][] networkResults = indArrayResults.toDoubleMatrix();
            List<T> seasonDataList = tournamentYearSeasonDataMap.get(networkRunnerPlugin.getTournamentYear());
            for (T seasonData : seasonDataList) {
                double[] results = networkResults[index];
                log.debug("loadTeamResults(): Adding results for team {}: {}", seasonData.getTeamName(), StringUtils.join(results, ','));
                teamResultsMap.put(seasonData.getTeamName(), results);
                index++;
            }
            return teamResultsMap;
        } catch (Exception e) {
            log.error("loadTeamResults: Exception occurred: {}", e.getLocalizedMessage(), e);
            return null;
        }
    }

    private void printTeamRankingsToLogs(final Map<String, TeamCoordinate> teamCoordinateMap) {
        Set<TeamCoordinate> teamRankings = new TreeSet<>((lhs, rhs) -> Double.compare(rhs.getDistanceScore(), lhs.getDistanceScore()));
        teamCoordinateMap.keySet().forEach(teamName -> teamRankings.add(teamCoordinateMap.get(teamName)));
        int ranking = 1;
        for (TeamCoordinate teamCoordinate : teamRankings) {
            log.info("{}: {}: {}", ranking, teamCoordinate.getName(), teamCoordinate.getDistanceScore());
            ranking++;
        }
    }

    protected TeamCoordinate computeWinner(final TeamCoordinate homeTeamCoordinate,
                                           final Double homeTeamDistanceScore,
                                           final TeamCoordinate awayTeamCoordinate,
                                           final Double awayTeamDistanceScore) {
        TeamCoordinate winner;
        //
        // Set the distance scores
        homeTeamCoordinate.setDistanceScore(homeTeamCoordinate.getDistanceScore() + homeTeamDistanceScore);
        awayTeamCoordinate.setDistanceScore(awayTeamCoordinate.getDistanceScore() + awayTeamDistanceScore);
        //
        // Compute the winner
        if (homeTeamDistanceScore > awayTeamDistanceScore) {
            // Home team is the winner
            winner = homeTeamCoordinate;
        } else if (homeTeamDistanceScore.equals(awayTeamDistanceScore)) {
            // Tie goes to home (it has to go to someone)
            winner = posessionArrow.jumpBall(homeTeamCoordinate, awayTeamCoordinate);
            log.warn("TOURNAMENT DISTANCE TIE-BREAKER({}): Giving the win to team: {}", homeTeamDistanceScore, winner.getName());
        } else {
            // Away team is winner
            winner = awayTeamCoordinate;
        }
        return winner;
    }

    private TeamCoordinate computeNextRoundCoordinates(final TeamCoordinate homeTeamCoordinate,
                                                       final Double homeWinNetworkVotes,
                                                       final TeamCoordinate awayTeamCoordinate,
                                                       final Double awayWinNetworkVotes) {
        TeamCoordinate ret = new TeamCoordinate().setAwayWinNetworkVotes(awayWinNetworkVotes).setHomeWinNetworkVotes(homeWinNetworkVotes);
        //
        // Set the distance score based on who has the most votes (tie goes to home team)
        double distanceScore = (homeWinNetworkVotes >= awayWinNetworkVotes) ? homeTeamCoordinate.getDistanceScore() : awayTeamCoordinate.getDistanceScore();
        switch (homeTeamCoordinate.getRound()) {
            case 0 -> {
                ret.setIndex(awayTeamCoordinate.getIndex());
                ret.setRegion(homeTeamCoordinate.getRegion());
            }
            case 1, 2, 3, 4 -> {
                ret.setIndex(homeTeamCoordinate.getIndex() / 2);
                ret.setRegion(homeTeamCoordinate.getRegion() * 2 / 2);
            }
            case 5 -> {
                ret.setIndex(homeTeamCoordinate.getIndex() / 2);
                ret.setRegion(homeTeamCoordinate.getRegion() / 2);
            }
        }
        ret.setRound(homeTeamCoordinate.getRound() + 1);
        ret.setDistanceScore(distanceScore);
        return ret;
    }

    protected RecordReaderDataSetIterator computeRecordIterator(final NetworkDescriptor networkDescriptor,
                                                                final NetworkRunnerPlugin<T> networkRunnerPlugin) {
        RecordReaderDataSetIterator ret;
        //
        // Wrap both rows in a CSVRecordReader, with all the bells and whistles and return the appropriate iterator
        StringBuilder stringSource = new StringBuilder();
        Integer tournamentYear = networkRunnerPlugin.getTournamentYear();
        List<T> seasonDataList = tournamentYearSeasonDataMap.get(tournamentYear);
        if (seasonDataList == null) {
            log.info("***** ADDING SEASON DATA FOR YEAR {} *****", tournamentYear);
            seasonDataList = getSeasonDataDao().fetchAllByYear(tournamentYear);
            tournamentYearSeasonDataMap.put(tournamentYear, seasonDataList);
        }
        //
        // IMPORTANT: Note the order of the SeasonData objects in the allSeasonData list
        // sets the order of the records in the iterator!!
        Map<String, String> teamCsvDataMap = tournamentYearTeamCsvDataMap.computeIfAbsent(tournamentYear, year -> new HashMap<>());
        seasonDataList.forEach(seasonData -> {
            String dataCsv = teamCsvDataMap.get(seasonData.getTeamName());
            if (dataCsv == null) {
                log.info("***** ADDING SEASON DATA FOR: {} *****", seasonData.getTeamName());
                String[] data = seasonData.transformRow(seasonDataAsDoubleList(seasonData), false);
                dataCsv = String.join(",", data);
                teamCsvDataMap.put(seasonData.getTeamName(), dataCsv);
            }
            stringSource.append(dataCsv);
            stringSource.append("\n");
        });
        StringReader stringReader = new StringReader(stringSource.toString());
        try {
            InputStream inputStream = new ByteArrayInputStream(CharStreams.toString(stringReader).getBytes());
            RecordReader tournamentReader = new CSVRecordReader(0, ',');
            InputStreamInputSplit split = new InputStreamInputSplit(inputStream);
            tournamentReader.initialize(split);
            ret = new RecordReaderDataSetIterator(tournamentReader, BATCH_SIZE);
        } catch (IOException | InterruptedException e) {
            String message = String.format("%s0053E: Error creating InputStream from CSV data:\n%s", LOG_PREFIX, stringSource);
            log.error(message, e);
            throw new InternalServerErrorException(message, e);
        }
        return ret;
    }

    /**
     * Generate the document showing the tournament results
     */
    protected String visualizeTournamentResults(final NetworkRunnerPlugin<T> networkRunnerPlugin) {
        //
        // Read the tournament results template, which contains team coordinates in specific spots in the
        // template that can be replaced with the actual team names that held those coordinates during any
        // specific instance of the tournament so the results can be visualized in "bracket" style.
        List<String[]> templateRecords = readTournamentTemplateFile(networkRunnerPlugin);
        //
        // Replace the TeamCoordinates in the template with the actual team names for the specific tournament
        // that was simulated and write out the file so the user can open it up and visualize the results.
        return writeTournamentResultsFile(templateRecords, getTeamCoordinateSeasonDataMap(), networkRunnerPlugin);
    }

    private List<String[]> readTournamentTemplateFile(final NetworkRunnerPlugin<T> networkRunnerPlugin) {
        //
        // Open and read the tournament results template CSV, which contains the template for visualizing the tournament results
        String tournamentResultsTemplateFileName = networkRunnerPlugin.getTournamentResultsTemplateFileName();
        //
        // List<String[]> contains the template file, which will be filled in by using the teamCoordinateSeasonDataMap
        // to map from team coordinates in the template to team names
        List<String[]> templateRecords = new ArrayList<>();
        String tournamentResultsTemplateFileNameUrl = getNetworkUtils().computeDataFileUrl(
                sportsnetProperties, getSportsSpecificUri(), networkRunnerPlugin.getTournamentYear(), tournamentResultsTemplateFileName, ResourceType.TOURNAMENT_PARTICIPANT_FILE);
        Optional<ResourceFileWrapperDto> tournamentResultsTemplateMaybe = getNetworkUtils().fetchDataFileFromResourceServer(tournamentResultsTemplateFileNameUrl);
        if (tournamentResultsTemplateMaybe.isPresent()) {
            String[][] tournamentResultsTemplateFileContents = tournamentResultsTemplateMaybe.get().getFileContentsAsDoubleStringArray();
            templateRecords.addAll(Arrays.asList(tournamentResultsTemplateFileContents));
        } else {
            String errorMessage = String.format("%s0055E: The specified file could not be found on the resource server: %s",
                    LOG_PREFIX, tournamentResultsTemplateFileNameUrl);
            log.error("readTournamentTemplateFile(): {}", errorMessage);
            throw new NotFoundException(errorMessage);
        }
        return templateRecords;
    }

    protected String writeTournamentResultsFile(final List<String[]> templateRecords,
                                                final Map<TeamCoordinate, T> teamCoordinateSeasonDataMap,
                                                final NetworkRunnerPlugin<T> networkRunnerPlugin) {
        String ret;
        //
        // Open and write out the tournament results template for this specific tournament result
        try (StringWriter tournamentResultsWriter = new StringWriter(4096)) {
            CSVWriter csvWriter = new CSVWriter(tournamentResultsWriter);
            csvWriter.writeNext(new String[]{
                    String.format("Tournament year: %s", networkRunnerPlugin.getTournamentYearForDisplay())
            });
            //
            // Read each line in the templateRecords list and translate from team coordinates like [0,1,0] to
            // a team name using the teamCoordinateSeasonDataMap
            for (String[] templateRecord : templateRecords) {
                for (int cellIndex = 0; cellIndex < templateRecord.length; cellIndex++) {
                    String cell = templateRecord[cellIndex];
                    if (TeamCoordinate.looksLikeTeamCoordinate(cell)) {
                        //
                        // Looks like a TeamCoordinate cell, map it after removing brackets (ironic, huh?)
                        TeamCoordinate teamCoordinate = TeamCoordinate.parseCoordinates(
                                StringUtils.remove(StringUtils.remove(cell, '['), ']')
                        );
                        //
                        // Replace the coordinates with the name of the team that had those coordinates in this incarnation of the tournament
                        T winningTeamSeasonData = teamCoordinateSeasonDataMap.get(teamCoordinate);
                        String winningTeamName = winningTeamSeasonData.getTeamName();
                        TeamCoordinate winningTeamCoordinate = findWinningTeamCoordinate(teamCoordinateSeasonDataMap, teamCoordinate);
                        // Opening rounds have no votes either way, no need to compute network confidence for those.
                        if (winningTeamCoordinate.getAwayWinNetworkVotes() == 0 && winningTeamCoordinate.getHomeWinNetworkVotes() == 0) {
                            templateRecord[cellIndex] = String.format("%s", winningTeamName);
                        } else {
                            templateRecord[cellIndex] = String.format("%s (%4.5f)",
                                    StringUtils.remove(winningTeamName, "NCAA"),
                                    winningTeamCoordinate.getDistanceScore()
                            );
                        }
                    }
                }
                //
                // Done with that record, write it out
                csvWriter.writeNext(templateRecord);
            }
            ret = tournamentResultsWriter.toString();
        } catch (IOException e) {
            String message = String.format("%sError writing tournament results output: %s", LOG_PREFIX, e.getLocalizedMessage());
            log.error(message, e);
            throw new InternalServerErrorException(message, e);
        }
        return ret;
    }

    private TeamCoordinate findWinningTeamCoordinate(final Map<TeamCoordinate, T> teamCoordinateSeasonDataMap, final TeamCoordinate teamCoordinate) {
        TeamCoordinate ret = teamCoordinate;
        Set<TeamCoordinate> keySet = teamCoordinateSeasonDataMap.keySet();
        for (TeamCoordinate winningTeamCoordinate : keySet) {
            if (winningTeamCoordinate.equals(teamCoordinate)) {
                ret = winningTeamCoordinate;
                break;
            }
        }
        if (ret == null) {
            // Did not find the team coordinate we were looking for (for some reason), return the default
            log.error("Could not find winning team coordinate: {}, returning the default", (Object) null);
        }
        return ret;
    }

    private static class JumpBall {
        private boolean jumpBall = false;

        public TeamCoordinate jumpBall(final TeamCoordinate homeTeamCoordinate, final TeamCoordinate awayTeamCoordinate) {
            jumpBall = !jumpBall; // toggle the possession arrow
            return (jumpBall) ? homeTeamCoordinate : awayTeamCoordinate;
        }
    }

    /**
     * Okay, so you wanna know what the point of all of this is? It's summarized in this little
     * method. Read on.
     * <p>
     * Compute the tournament "distance" score. The classification done by sportsnet networks
     * is the probability of advancement to a given round (each sport has a different number
     * of rounds, so the number of dimensions varies by sport), but here's an example from NFL:
     * 0: probability of not making the playoffs
     * 1: probability of competing in wildcard round
     * 2: probability of winning a wildcard spot
     * 3: probability of winning the divisional playoffs
     * 4: probability of winning the conference
     * 5: probability of winning the superbowl
     * <p>
     * The numbers in buckets 1-5 add up to one (since they are probabilities).
     * <p>
     * Each bucket has a number between 0-1 representing the probability that the team will advance
     * as far as the class represented by that bucket (as described above).
     * <p>
     * This method computes the distance score, which is the probability in each bucket times
     * its index + 1 (a.k.a., the round).
     * <p>
     * So the number in bucket 0 is multiplied by 1, the number in bucket 1 is multiplied by 2,
     * and so on. The buckets are weighted in this fashion to bias teams predicted to have more
     * tournament wins.
     *
     * @param networkTournamentDistancePredictions The double array representing a network's prediction
     *                                             for how far a team will go in the tournament. Each
     *                                             element in the array represents the probability of
     *                                             the team having that classification based off their
     *                                             season data.
     */
    protected double computeTournamentDistanceScore(final double[] networkTournamentDistancePredictions) {
        double tournamentDistanceScore = 0.0;
        for (int index = 0; index < networkTournamentDistancePredictions.length; index++) {
            double teamProbability = networkTournamentDistancePredictions[index];
            // Apply a weight based on index (where index is the round number, giving advantage to tournament distance)
            tournamentDistanceScore += teamProbability * (index + networkProperties.getTournamentDistanceFactor());
        }
        return tournamentDistanceScore;
    }

    /**
     * Run the tournament simualtion, and produce a simple list of rankings for each team in the league.
     * <p>
     * The predictions are weighted by classification: the higher
     * the predicted classification, the more weight it has. This allows all predictions to
     * factor into the overall score, but predictions that the team will go higher in the
     * tournament should carry more weight (that's the idea, anyway).
     *
     * @param networksToUse       The networks to use for running the tournament simulation
     * @param networkRunnerPlugin The network runner plugin to use
     * @return A newline-delimited string containing the rankings of each team in the league, with
     * their score, which is a determination of how well the network predicts they will do in the
     * tournament. Highest score first.
     */
    protected String runTournamentAndProduceRankings(final List<UUID> networksToUse,
                                                     final NetworkRunnerPlugin<T> networkRunnerPlugin) {
        final long startTime = System.currentTimeMillis();
        //
        // Build the Map of network -> Team Results Map (k=team name, v=network results)
        final Map<String, Double> teamTournamentDistanceScoreMap = computeTournamentDistanceMap(networksToUse, networkRunnerPlugin);
        log.debug("runTournament(): Finished running tournament in {}ms", System.currentTimeMillis() - startTime);
        //
        // Zip through the map, sort it by score, and build a newline-delimited string of teams in order of score.
        final Set<Map.Entry<String, Double>> entries = teamTournamentDistanceScoreMap.entrySet();
        final StringBuilder sb = new StringBuilder("Rank,Team Name,Tournament Score\n");
        final List<String> rankings = entries.stream()
                .sorted((lhs, rhs) -> rhs.getValue().compareTo(lhs.getValue())) // Sort descending (rhs.compareTo(lhs))
                .map(entry -> String.format("%s,%10.4f", entry.getKey(), entry.getValue()))
                .toList();
        int rank = 1;
        for (String rankingsString : rankings) {
            sb.append(String.format("%d,%s%n", rank++, rankingsString));
        }
        log.debug("runTournament(): Rankings computed in {}ms", System.currentTimeMillis() - startTime);
        return sb.toString();
    }

    private Map<String, Double> computeTournamentDistanceMap(final List<UUID> networksToUse, final NetworkRunnerPlugin<T> networkRunnerPlugin) {
        final Map<String, Double> teamTournamentDistanceScoreMap = new ConcurrentHashMap<>();
        final long startTime = System.currentTimeMillis();
        //
        // Compute a team distance score for each network, and accumulate the scores for all
        // networks (just add them up).
        final AtomicInteger count = new AtomicInteger(0);
        networksToUse.parallelStream().forEach(networkId -> {
            long networkLoopStartTime = System.currentTimeMillis();
            Map<String, double[]> teamResults = computeNetworkResults(networkId, networkRunnerPlugin);
            if (teamResults != null) {
                log.debug("runTournament(): Computed results for network {} in {} ms", networkId, System.currentTimeMillis() - networkLoopStartTime);
                //
                // Now zip through the network results team map and tally up the score for each team
                Set<String> teamNames = teamResults.keySet();
                teamNames.forEach(teamName -> {
                    double[] networkPrediction = teamResults.get(teamName);
                    double tournamentDistanceScore = computeTournamentDistanceScore(networkPrediction);
                    double accumulatedScore = teamTournamentDistanceScoreMap.computeIfAbsent(teamName, s -> 0.0);
                    teamTournamentDistanceScoreMap.put(teamName, accumulatedScore + tournamentDistanceScore);
                });
                log.debug("runTournament(): Computed scores for {} teams in {}ms", teamNames.size(), System.currentTimeMillis() - networkLoopStartTime);
            } else {
                String errorMessage = String.format("%s2218W: No team results. Network %s skipped.", LOG_PREFIX, networkId);
                log.warn("runTournamentAndProduceRankings(): {}", errorMessage);
            }
            final int currentCount = count.getAndIncrement();
            String message = String.format("%d of %d networks processed, %3.2f%% complete, estimated completion: %s...",
                    currentCount, networksToUse.size(), 100.0 * (double)currentCount / (double) networksToUse.size(),
                    networkUtils.computeCompletionTime(networksToUse.size(), currentCount, startTime));
            log.info("*** {} ***", message);
        });
        return teamTournamentDistanceScoreMap;
    }

    private void runNetwork(final List<UUID> networksToUse, final NetworkRunnerPlugin<T> networkRunnerPlugin, final Map<String, Double> teamTournamentDistanceScoreMap, final long startTime, final AtomicInteger count, final UUID networkId) {
    }

    protected Map<String, double[]> computeNetworkResults(final UUID networkId,
                                                          final NetworkRunnerPlugin<T> networkRunnerPlugin) {
        //
        // Compute RecordReaderDataSetIterator from the two sets of season data
        final Map<String, double[]> teamResultsMap;
        Optional<NetworkCandidate> networkCandidateOptional = fetchNetworkById(networkId);
        if (networkCandidateOptional.isPresent()) {
            NetworkCandidate networkCandidate = networkCandidateOptional.get();
            final boolean allowEvaluationYearsForPrediction = getNetworkProperties().allowEvaluationYearsForPrediction();
            if (networkUtils.isValidYearForTournamentPrediction(networkRunnerPlugin.getTournamentYear(), networkCandidate.getTrainedNetworkParameters(), allowEvaluationYearsForPrediction)) {
                teamResultsMap = loadTeamResults(networkRunnerPlugin, networkCandidate);
            } else {
                String errorMessage = String.format("%s2218W: Network %s is not valid for tournament year %d", LOG_PREFIX, networkId, networkRunnerPlugin.getTournamentYear());
                log.warn("computeNetworkResults(): {}", errorMessage);
                teamResultsMap = null;
            }
        } else {
            teamResultsMap = null;
        }
        return teamResultsMap;
    }
    public Optional<NetworkDescriptor> evaluateNetwork(final UUID networkId, final Integer evaluationYear) {
        Optional<NetworkDescriptor> evaluatedNetworkDescriptor = Optional.empty();

        Optional<NetworkCandidate> networkCandidateOptional = fetchNetworkById(networkId);
        if (networkCandidateOptional.isPresent()) {
            NetworkCandidate networkCandidate = networkCandidateOptional.get();
            MultiLayerNetwork trainedNetwork = networkCandidate.getMultiLayerNetwork();
            NetworkDescriptor networkDescriptor = networkCandidate.getTrainedNetworkParameters();
            networkDescriptor.setYearsToEvaluateNetwork(List.of(evaluationYear));
            String evaluationDataCsv = createEvaluationCsvData(networkDescriptor);
            try {
                RecordReaderDataSetIterator evaluationDataIterator = getNetworkUtils().createIterator(evaluationDataCsv, networkDescriptor);
                DataSet evaluationData = evaluationDataIterator.next();
                //
                // Prepare the data
                DataNormalization normalizer = networkDescriptor.getNormalizer().getDataNormalization();
                getNetworkUtils().normalizeTrainingData(evaluationData, normalizer);
                Evaluation networkEvaluator = getNetworkUtils().computeNetworkEvaluator(networkDescriptor.getNumberOfOutputs(), evaluationData, trainedNetwork);
                networkDescriptor.setNetworkAccuracy(networkEvaluator.accuracy());
                networkDescriptor.setNetworkPrecision(networkEvaluator.precision());
                networkDescriptor.setNetworkRecall(networkEvaluator.recall());
                networkDescriptor.setNetworkF1Score(networkEvaluator.f1());
                networkDescriptor.setWhenTrained(LocalDateTime.now());
                networkDescriptor.setConfusionMatrixCsv(networkEvaluator.getConfusionMatrix().toCSV());
                log.info("Evaluator stats: {}", networkEvaluator.stats());
                evaluatedNetworkDescriptor = Optional.of(networkDescriptor);
            } catch (Exception e) {
                String errorMessage = String.format("%s0000E: Exception while evaluating network: %s", LOG_PREFIX, e.getLocalizedMessage());
                log.error(errorMessage);
            }
        }
        return evaluatedNetworkDescriptor;
    }

}
