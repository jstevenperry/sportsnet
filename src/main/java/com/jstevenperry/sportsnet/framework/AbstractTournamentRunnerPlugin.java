/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet.framework;

import com.jstevenperry.sportsnet.framework.model.AbstractSeasonData;
import com.jstevenperry.sportsnet.framework.model.GameCoordinate;
import com.jstevenperry.sportsnet.framework.model.NetworkCandidate;
import com.jstevenperry.sportsnet.framework.model.NetworkParameters;
import com.jstevenperry.sportsnet.framework.model.TeamCoordinate;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.framework.util.NetworkProperties;
import com.jstevenperry.sportsnet.framework.util.NetworkUtils;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.apache.commons.lang3.StringUtils;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.InputStreamInputSplit;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.jetbrains.annotations.NotNull;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.shade.guava.io.CharStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

/**
 * This is the base class for running networks for a specific tournament.
 * This class is subclassed and key methods are overridden to create a
 * tournament-specific plugin that can be used to make predictions.
 */
public abstract class AbstractTournamentRunnerPlugin<T extends AbstractSeasonData> extends AbstractNetworkBase<T> {

    private static final Logger log = LoggerFactory.getLogger(AbstractTournamentRunnerPlugin.class);

    private static final int BATCH_SIZE = 2000;
    // My "class variables of shame", but sometimes you need a little optimization (and place to put it)
    private final Map<String, T> teamSeasonDataMap = new HashMap<>();
    private final Map<String, String> teamCsvDataMap = new HashMap<>(); // optimization, populated once per team. Savings: NumberOfTournamentTeams * NumberOfNetworks * (NumberOfTournamentGames - 1)

    private final JumpBall posessionArrow = new JumpBall();
    /**
     * Stores raw CSV data for all teams in the sport (if property "compute.csv.for.all.teams" is true),
     * or only teams that remain in the tournament (if property "compute.csv.for.remaining.tournament.teams.only" is true),
     * or all teams in the tournament (the default)
     */
    private String csvSeasonData; // optimization - populates once per run. Savings: varies. Up to NumberOfTournamentTeams * NumberOfNetworks * (NumberOfTournamentGames - 1)
    /**
     * Stores normalized dataset for a single matchup.
     * Key: See computeDataSetMapKeyForMatchup()
     * Value: DataSet that can be used to predict the outcome of a single matchup
     */
    private final Map<String, DataSet> dataSetMap = new HashMap<>(); // Optimization. Savings: NumberOfTournamentGames * (NumberOfNetworks - 1)

    private final SeasonDataDao<T> seasonDataDao;

    public AbstractTournamentRunnerPlugin(final SeasonDataDao<T> seasonDataDao) {
        this.seasonDataDao = seasonDataDao;
        setTeamCoordinateSeasonDataMap(buildTeamCoordinateMap());
    }

    private Map<TeamCoordinate, T> teamCoordinateSeasonDataMap;

    public Map<TeamCoordinate, T> getTeamCoordinateSeasonDataMap() {
        if (teamCoordinateSeasonDataMap == null) {
            String message = "Team Coordinate SeasonData map is null. Did you forget to call setTeamCoordinateSeasonDataMap in the tournament plugin constructor?";
            log.error(message);
            throw new RuntimeException(message);
        }
        return teamCoordinateSeasonDataMap;
    }

    public void setTeamCoordinateSeasonDataMap(final Map<TeamCoordinate, T> teamCoordinateSeasonDataMap) {
        this.teamCoordinateSeasonDataMap = teamCoordinateSeasonDataMap;
    }

    /**
     * Return the tournament year handled by the plugin subclass
     */
    public abstract Integer getTournamentYear();

    /**
     * Return the tournament year as it is to be displayed (e.g., 2018-19)
     */
    public abstract String getTournamentYearForDisplay();

    /**
     * Return the tournament results template file name.
     * Used to visualize the brackets computed by the network
     * when it simulates a tournament.
     */
    protected abstract String getTournamentResultsTemplateFileName();

    protected abstract String getNetworkType();

    /**
     * Build the TeamCoordinate->SeasonData map, specific to a tournament year.
     */
    protected abstract Map<TeamCoordinate, T> buildTeamCoordinateMap();

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
            String message = String.format("Cannot process malformed tournament template file line: '%s'", line);
            log.error(message);
            throw new RuntimeException(message);
        }
        return ret;
    }

    /**
     * Return the name of the tournament template file.
     * <p>
     * The tournament template file is used to setup the games that will
     * be played in the tournament. Each game has a set of coordinates that
     * correspond to the region, round, and teams that participate in the
     * game. The region and round are the same for any given game from year
     * to year, but the teams that participate will be different and are
     * specified by the subclass that is for the tournament year that plugin
     * was written for.
     */
    public abstract String getTournamentTemplateFileName();

    /**
     * Fetch and return season data for the team specified in the TeamCoordinate object
     * that is passed to this method.
     */
    public T fetchSeasonData(final TeamCoordinate teamCoordinate) {
        return seasonDataDao.fetchByYearAndTeamName(getTournamentYear(), teamCoordinate.getName());
    }

    /**
     * Create the list of games that will be played during the tournament.
     * A game is specified by its GameCoordinate object, since each game
     * can occur in a different round, region, and played by different
     * teams.
     */
    public List<GameCoordinate> createTournamentGameList() {
        List<GameCoordinate> ret = new ArrayList<>();
        String tournamentFileName = getTournamentTemplateFileName();
        InputStream inputStream = AbstractTournamentRunnerPlugin.class.getResourceAsStream(tournamentFileName);
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line = bufferedReader.readLine();
            while (line != null) {
                if (!line.trim().startsWith("#")) {
                    Optional<GameCoordinate> gameCoordinateMaybe = parseTournamentTemplateLine(line);
                    gameCoordinateMaybe.ifPresent(ret::add);
                }
                line = bufferedReader.readLine();
            }
        } catch (IOException e) {
            String message = String.format("Error occurred while processing tournament template file: %s: %s", tournamentFileName, e.getLocalizedMessage());
            log.error(message, e);
        }
        return ret;
    }

    /**
     * Using the specified list of GameCoordinates, run the tournament.
     */
    public void runTournament(final List<GameCoordinate> gameList, final List<NetworkCandidate> networksToUse) {
        //
        // Throw out any networks that are not valid for the tournament year
        final List<NetworkCandidate> networksToRun = computeValidNetworksForTournamentYear(networksToUse);
        if (!networksToRun.isEmpty()) {
            //
            // Build the Map of team name to season data so we don't have to constantly be fetching this information
            buildTeamSeasonDataMap(gameList);
            //
            // Kick off the simulation
            gameList.forEach(gameCoordinate -> {
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
                    runAllNetworksAndComputeWinner(networksToUse, homeTeamCoordinate, homeTeamSeasonData, awayTeamCoordinate, awayTeamSeasonData);
                } catch (Exception e) {
                    String errorMessage = String.format("runTournament(): Error while running game: %s", gameCoordinate);
                    log.error(errorMessage, e);
                    throw new RuntimeException(errorMessage, e);
                }
            });
            //
            // Generate tournament results document
            visualizeTournamentResults(networksToUse);
        } else {
            log.error("***** SIMULATION CANCELLED *****");
            log.error("Of the {} networks you specified to run, there are none suitable for tournament year {}.", networksToUse.size(), getTournamentYear());
            log.error("***** SIMULATION CANCELLED: SEE PREVIOUS LOG ENTRIES FOR DETAILS. *****");
        }
    }

    /**
     * Using the GameCoordinate list, build the map of team name -> season data
     */
    private void buildTeamSeasonDataMap(final List<GameCoordinate> gameList) {
        // Build Team Season Data map for those teams in the tournament
        gameList.forEach(gameCoordinate -> {
            T homeTeamSeasonData = getTeamCoordinateSeasonDataMap().get(gameCoordinate.getHome());
            if (homeTeamSeasonData != null) {
                teamSeasonDataMap.computeIfAbsent(homeTeamSeasonData.getTeamName(), k -> homeTeamSeasonData);
            }
            T awayTeamSeasonData = getTeamCoordinateSeasonDataMap().get(gameCoordinate.getAway());
            if (awayTeamSeasonData != null) {
                teamSeasonDataMap.computeIfAbsent(awayTeamSeasonData.getTeamName(), k -> awayTeamSeasonData);
            }
        });
    }

    public List<NetworkCandidate> computeValidNetworksForTournamentYear(final List<NetworkCandidate> networkCandidates) {
        log.info("computeValidNetworksForTournamentYear(): For tournament year {}, filtering {} networks...", getTournamentYear(), networkCandidates.size());
        //
        // If the network is valid for the tournament year, we're good to go, otherwise throw it out
        List<NetworkCandidate> ret = networkCandidates.stream()
                .filter(networkCandidate -> {
                    if (networkCandidate.getNetworkParameters().isValidYearForTournamentPrediction(getTournamentYear())) {
                        return true;
                    } else {
                        log.warn("computeValidNetworksForTournamentYear(): Network invalid for tournament year {}: {}",
                                getTournamentYear(), networkCandidate.getNetworkParameters().toStringOneLine()
                        );
                        return false;
                    }
                })
                .collect(Collectors.toList());
        log.info("computeValidNetworksForTournamentYear(): There are {} networks valid for tournament year {}.", ret.size(), getTournamentYear());
        return ret;
    }

    private void runAllNetworksAndComputeWinner(final List<NetworkCandidate> networkCandidates,
                                                final TeamCoordinate homeTeamCoordinate,
                                                final T homeTeamSeasonData,
                                                final TeamCoordinate awayTeamCoordinate,
                                                final T awayTeamSeasonData) {
        int homeTeamWins = 0;
        int awayTeamWins = 0;
        for (NetworkCandidate networkCandidate : networkCandidates) {
            MultiLayerNetwork network = networkCandidate.getMultiLayerNetwork();
            // Data is normalized once per matchup. This saves cycles when lots of networks are used.
            String matchupKey = computeDataSetMapKeyForMatchup(homeTeamCoordinate.getName(), awayTeamCoordinate.getName());
            DataSet tournamentData = dataSetMap.get(matchupKey);
            if (tournamentData == null) {
                log.info("***** COMPUTING DataSet for matchup: {} *****", matchupKey);
                //
                // Compute RecordReaderDataSetIterator from the two sets of season data
                RecordReaderDataSetIterator recordReaderDataSetIterator = computeRecordIterator(homeTeamSeasonData, awayTeamSeasonData, networkCandidate.getNetworkParameters());
                DataNormalization normalizer = networkCandidate.getNetworkParameters().getNormalizer().getDataNormalization();
                tournamentData = recordReaderDataSetIterator.next();
                normalizer.fit(tournamentData);
                normalizer.transform(tournamentData);
                dataSetMap.put(matchupKey, tournamentData);
            }
            // Run the network against the normalized data
            INDArray indArrayResults = network.output(tournamentData.getFeatures());
            //
            // Figure out who we declare the winner and compute their TeamCoordinates according to the algorithm
            try {
                TeamCoordinate winningTeamCoordinate = computeWinner(indArrayResults.toDoubleMatrix(), homeTeamCoordinate, awayTeamCoordinate);
                if (winningTeamCoordinate.equals(homeTeamCoordinate)) {
                    homeTeamWins++;
                } else {
                    awayTeamWins++;
                }
            } catch (NumberFormatException e) {
                String errorMessage = String.format("NumberFormatException while running %s vs %s for network %s. Skipping.", networkCandidate, homeTeamCoordinate, awayTeamCoordinate);
                log.error(errorMessage);
            }
        }
        // Compute the winningTeamCoordinate based on the which team has the most votes (tie goes to home team)
        TeamCoordinate teamCoordinate = computeNextRoundCoordinates(homeTeamCoordinate, homeTeamWins, awayTeamCoordinate, awayTeamWins);
        T winnerSeasonData;
        if (homeTeamWins >= awayTeamWins) {
            winnerSeasonData = homeTeamSeasonData;
        } else {
            winnerSeasonData = awayTeamSeasonData;
        }
        teamCoordinate.setName(winnerSeasonData.getTeamName());
        getTeamCoordinateSeasonDataMap().put(teamCoordinate, winnerSeasonData);
        log.debug("***** WINNER: {} with {} wins in {} simulated matchups *****",
                teamCoordinate, Math.max(homeTeamWins, awayTeamWins), networkCandidates.size());
    }

    private String computeDataSetMapKeyForMatchup(final String homeTeamName, final String awayTeamName) {
        return String.format("%s_vs_%s", homeTeamName, awayTeamName);
    }

    protected TeamCoordinate computeWinner(final double[][] results,
                                           final TeamCoordinate homeTeamCoordinate,
                                           final TeamCoordinate awayTeamCoordinate) {
        TeamCoordinate winner;
        BigDecimal[] homeTeamProbabilities = convertToBigDecimal(results[0]);
        BigDecimal[] awayTeamProbabilities = convertToBigDecimal(results[1]);
        // Sanity Check
        if (homeTeamProbabilities.length != awayTeamProbabilities.length) {
            String errorMessage = String.format("ERROR: homeTeamProbabilities array length (%d) is not equal to awayTeamProbabilities array length (%d)",
                    homeTeamProbabilities.length, awayTeamProbabilities.length);
            log.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }
        //
        // The winner is the team predicted to win more games
        double homeTeamDistanceScore = 0.0;
        double awayTeamDistanceScore = 0.0;
        for (int index = 0; index < homeTeamProbabilities.length; index++) {
            BigDecimal homeTeamProbability = homeTeamProbabilities[index];
            // Apply a weight based on index (where index is the round number, giving advantage to tournament distance)
            homeTeamDistanceScore += homeTeamProbability.doubleValue() * (index + 1);
            BigDecimal awayTeamProbability = awayTeamProbabilities[index];
            // Apply a weight based on index (where index is the round number, giving advantage to tournament distance)
            awayTeamDistanceScore += awayTeamProbability.doubleValue() * (index + 1);
        }
        if (homeTeamDistanceScore > awayTeamDistanceScore) {
            // Home team is the winner
            winner = homeTeamCoordinate;
        } else if (homeTeamDistanceScore == awayTeamDistanceScore) {
            // Tie goes to home (it has to go to someone)
            winner = posessionArrow.jumpBall(homeTeamCoordinate, awayTeamCoordinate);
            log.warn("TOURNAMENT DISTANCE TIE-BREAKER({}): Giving the win to team: {}", homeTeamDistanceScore, winner.getName());
        } else {
            // Away team is winner
            winner = awayTeamCoordinate;
        }
        log.debug(String.format("Winner: %s: %s (%s) vs %s (%s): hv/hp %3.4f, av/ap %3.4f",
                winner.getName(),
                homeTeamCoordinate.getName(), Arrays.toString(homeTeamProbabilities), // vs
                awayTeamCoordinate.getName(), Arrays.toString(awayTeamProbabilities), //:
                homeTeamDistanceScore, awayTeamDistanceScore
        ));
        return winner;
    }

    private BigDecimal[] convertToBigDecimal(final double[] predictions) {
        List<BigDecimal> ret = new ArrayList<>();
        for (double prediction : predictions) {
            ret.add(BigDecimal.valueOf(prediction).setScale(4, RoundingMode.HALF_UP));
        }
        return ret.toArray(new BigDecimal[0]);
    }

    private TeamCoordinate computeNextRoundCoordinates(final TeamCoordinate homeTeamCoordinate,
                                                       final int homeWinNetworkVotes,
                                                       final TeamCoordinate awayTeamCoordinate,
                                                       final int awayWinNetworkVotes) {
        TeamCoordinate ret = new TeamCoordinate().setAwayWinNetworkVotes(awayWinNetworkVotes).setHomeWinNetworkVotes(homeWinNetworkVotes);
        switch (homeTeamCoordinate.getRound()) {
            case 0:
                ret.setIndex(awayTeamCoordinate.getIndex());
                ret.setRegion(homeTeamCoordinate.getRegion());
                break;
            case 1:
            case 2:
            case 3:
            case 4:
                ret.setIndex(homeTeamCoordinate.getIndex() / 2);
                ret.setRegion(homeTeamCoordinate.getRegion() * 2 / 2);
                break;
            case 5:
                ret.setIndex(homeTeamCoordinate.getIndex() / 2);
                ret.setRegion(homeTeamCoordinate.getRegion() / 2);
                break;
        }
        ret.setRound(homeTeamCoordinate.getRound() + 1);
        return ret;
    }

    protected RecordReaderDataSetIterator computeRecordIterator(final T homeTeamSeasonData,
                                                                final T awayTeamSeasonData,
                                                                final NetworkParameters networkParameters) {
        RecordReaderDataSetIterator ret;
        //
        // Wrap both rows in a CSVRecordReader, with all the bells and whistles and return the appropriate iterator
        String stringSource = computeCsvData(homeTeamSeasonData, awayTeamSeasonData, networkParameters);
        StringReader stringReader = new StringReader(stringSource);
        try {
            InputStream inputStream = new ByteArrayInputStream(CharStreams.toString(stringReader).getBytes());
            RecordReader tournamentReader = new CSVRecordReader(0, ',');
            InputStreamInputSplit split = new InputStreamInputSplit(inputStream);
            tournamentReader.initialize(split);
            ret = new RecordReaderDataSetIterator(tournamentReader, BATCH_SIZE);
        } catch (IOException | InterruptedException e) {
            String message = String.format("Error creating InputStream from CSV data:\n%s", stringSource);
            log.error(message, e);
            throw new RuntimeException(e);
        }
        return ret;
    }

    @NotNull
    private String computeCsvData(final T homeTeamSeasonData, final T awayTeamSeasonData, final NetworkParameters networkParameters) {
        log.debug("***** COMPUTING CSV DATA FOR MATCHUP {} VS {} *****",
                homeTeamSeasonData.getTeamName(), awayTeamSeasonData.getTeamName());
        StringBuilder stringSource = new StringBuilder();
        //
        // Compute one row of data with home team first
        String homeCsv = teamCsvDataMap.get(homeTeamSeasonData.getTeamName());
        if (homeCsv == null) {
            log.debug("***** COMPUTING SEASON DATA CSV FOR {} *****", homeTeamSeasonData.getTeamName());
            String[] homeData = networkParameters.transformRow(seasonDataAsDoubleList(homeTeamSeasonData));
            homeCsv = String.join(",", homeData);
            teamCsvDataMap.put(homeTeamSeasonData.getTeamName(), homeCsv);
        }
        stringSource.append(homeCsv);
        stringSource.append("\n");
        //
        // Compute another row of data with away team first
        String awayCsv = teamCsvDataMap.get(awayTeamSeasonData.getTeamName());
        if (awayCsv == null) {
            log.debug("***** COMPUTING SEASON DATA CSV FOR {} *****", awayTeamSeasonData.getTeamName());
            String[] awayData = networkParameters.transformRow(seasonDataAsDoubleList(awayTeamSeasonData));
            awayCsv = String.join(",", awayData);
            teamCsvDataMap.put(awayTeamSeasonData.getTeamName(), awayCsv);
        }
        stringSource.append(awayCsv);
        stringSource.append("\n");

        if (NetworkProperties.getNormalizeAcrossAllSeasonData()) {
            // Include all data for a season so normalized values (and network predictions based on them) are consistent
            stringSource.append(computeCsvDataOnlyForTeamsInTournament(networkParameters));
        }

        return stringSource.toString();
    }

    /**
     * Compute CSV season data for all teams that participate in the tournament.
     * This is the most optimal solution, and the data only needs to be created
     * once per simulation.
     */
    private String computeCsvDataOnlyForTeamsInTournament(final NetworkParameters networkParameters) {
        if (csvSeasonData == null) {
            StringBuilder stringSource = new StringBuilder();
            log.info("***** ADDING SEASON DATA FOR TEAMS IN THE TOURNAMENT *****");
            Set<String> teamNames = teamSeasonDataMap.keySet();
            teamNames.forEach(teamName -> {
                log.info("***** ADDING SEASON DATA FOR: {} *****", teamName);
                T seasonData = teamSeasonDataMap.get(teamName);
                String dataCsv = teamCsvDataMap.get(seasonData.getTeamName());
                if (dataCsv == null) {
                    String[] data = networkParameters.transformRow(seasonDataAsDoubleList(seasonData));
                    dataCsv = String.join(",", data);
                    teamCsvDataMap.put(seasonData.getTeamName(), dataCsv);
                }
                stringSource.append(dataCsv);
                stringSource.append("\n");
            });
            csvSeasonData = stringSource.toString();
        }
        return csvSeasonData;
    }

    protected void createTeamCoordinateMapping(final Map<TeamCoordinate, T> coordinateSeasonDataMap,
                                               final int region,
                                               final int round,
                                               final int index,
                                               final String teamName) {
        TeamCoordinate teamCoordinate = new TeamCoordinate()
                .setName(teamName)
                .setIndex(index)
                .setRegion(region)
                .setRound(round);
        T seasonData = fetchSeasonData(teamCoordinate);
        coordinateSeasonDataMap.put(teamCoordinate, seasonData);
    }

    /**
     * Generate the document showing the tournament results
     */
    private void visualizeTournamentResults(final List<NetworkCandidate> networksUsed) {
        //
        // Read the tournament results template, which contains team coordinates in specific spots in the
        // template that can be replaced with the actual team names that held those coordinates during any
        // specific instance of the tournament so the results can be visualized in "bracket" style.
        List<String[]> templateRecords = readTournamentTemplateFile();
        //
        // Replace the TeamCoordinates in the template with the actual team names for the specific tournament
        // that was simulated and write out the file so the user can open it up and visualize the results.
        writeTournamentResultsFile(templateRecords, getTeamCoordinateSeasonDataMap(), networksUsed);
    }

    private List<String[]> readTournamentTemplateFile() {
        //
        // Open and read the tournament results template CSV, which contains the template for visualizing the tournament results
        String tournamentResultsTemplateFileName = getTournamentResultsTemplateFileName();
        //
        // List<String[]> contains the template file, which will be filled in by using the teamCoordinateSeasonDataMap
        // to map from team coordinates in the template to team names
        List<String[]> templateRecords = new ArrayList<>();
        InputStream inputStream = AbstractTournamentRunnerPlugin.class.getResourceAsStream(tournamentResultsTemplateFileName);
        try (BufferedReader tournamentResultsTemplateReader = new BufferedReader(new InputStreamReader(inputStream))) {
            CSVReader csvReader = new CSVReader(tournamentResultsTemplateReader, ',', '"');
            String[] line = csvReader.readNext();
            while (line != null) {
                templateRecords.add(line);
                line = csvReader.readNext();
            }
        } catch (IOException e) {
            String message = String.format("Error reading tournament results template file %s", tournamentResultsTemplateFileName);
            log.error(message, e);
            throw new RuntimeException(message, e);
        }
        return templateRecords;
    }

    private void writeTournamentResultsFile(final List<String[]> templateRecords,
                                            final Map<TeamCoordinate, T> teamCoordinateSeasonDataMap,
                                            final List<NetworkCandidate> networksUsed) {
        //
        // Open and write out the tournament results template for this specific tournament result
        String tournamentResultsFileName = NetworkUtils.fetchSimulationDirectoryAndCreateIfNecessary(getNetworkType()) + File.separatorChar
                + getNetworkType() + "-TournamentYear_" + getTournamentYearForDisplay() + "-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".csv";
        try (BufferedWriter tournamentResultsWriter = new BufferedWriter(new FileWriter(tournamentResultsFileName))) {
            CSVWriter csvWriter = new CSVWriter(tournamentResultsWriter);
            csvWriter.writeNext(new String[]{
                    String.format("Tournament year: %s", getTournamentYearForDisplay())
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
                        TeamCoordinate winningTeamCoordinate = findWinningTeamCoordinate(teamCoordinateSeasonDataMap, teamCoordinate);
                        // Opening rounds have no votes either way, no need to compute network confidence for those.
                        if (winningTeamCoordinate.getAwayWinNetworkVotes() == 0 && winningTeamCoordinate.getHomeWinNetworkVotes() == 0) {
                            templateRecord[cellIndex] = String.format("%s", winningTeamSeasonData.getTeamName());
                        } else {
                            templateRecord[cellIndex] = String.format("%s (%3.2f%%)",
                                    winningTeamSeasonData.getTeamName(),
                                    computeNetworkConfidence(winningTeamCoordinate.getHomeWinNetworkVotes(), winningTeamCoordinate.getAwayWinNetworkVotes())
                            );
                        }
                    }
                }
                //
                // Done with that record, write it out
                csvWriter.writeNext(templateRecord);
            }
            //
            // Write network info
            csvWriter.writeNext(new String[0]);
            csvWriter.writeNext(new String[0]);
            csvWriter.writeNext(new String[0]);
            csvWriter.writeNext(new String[]{"Number of", "networks used", "for", String.format("simulation: %d", networksUsed.size())});
            csvWriter.writeNext(new String[]{"Details:"});
            csvWriter.writeNext(new String[0]);
            networksUsed.forEach(networkCandidate -> {
                NetworkParameters networkParameters = networkCandidate.getNetworkParameters();
                csvWriter.writeNext(networkParameters.toStringCsv());
            });
            log.info("writeTournamentResultsFile(): Wrote file: {}", tournamentResultsFileName);
        } catch (IOException e) {
            String message = String.format("Error writing tournament results output file %s", tournamentResultsFileName);
            log.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    private double computeNetworkConfidence(final int lhs, final int rhs) {
        double numerator;
        double denominator = Double.max(lhs + rhs, 1.0);
        // Larger number goes on top
        if (lhs >= rhs) {
            numerator = 1.0 * lhs;
        } else {
            numerator = 1.0 * rhs;
        }
        return numerator * 100.0 / denominator;
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

}
