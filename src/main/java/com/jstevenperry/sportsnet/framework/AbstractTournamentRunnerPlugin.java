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
import com.jstevenperry.sportsnet.framework.util.NetworkUtils;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.apache.commons.lang3.StringUtils;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.InputStreamInputSplit;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
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
    private final SeasonDataDao<T> seasonDataDao;
    private Map<TeamCoordinate, T> teamCoordinateSeasonDataMap;

    public AbstractTournamentRunnerPlugin(final SeasonDataDao<T> seasonDataDao) {
        this.seasonDataDao = seasonDataDao;
        setTeamCoordinateSeasonDataMap(buildTeamCoordinateMap());
    }

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
            //
            // Compute RecordReaderDataSetIterator from the two sets of season data
            Optional<RecordReaderDataSetIterator> recordReaderDataSetIteratorMaybe = computeRecordIterator(homeTeamSeasonData, awayTeamSeasonData, networkCandidate.getNetworkParameters());
            if (recordReaderDataSetIteratorMaybe.isPresent()) {
                DataNormalization normalizer = networkCandidate.getNetworkParameters().getNormalizer().getDataNormalization();
                DataSet tournamentData = recordReaderDataSetIteratorMaybe.get().next();
                normalizer.fit(tournamentData);
                normalizer.transform(tournamentData);
                INDArray indArrayResults = network.output(tournamentData.getFeatures());
                //
                // Figure out who we declare the winner and compute their TeamCoordinates according to the algorithm
                TeamCoordinate winningTeamCoordinate = computeWinner(indArrayResults.toDoubleMatrix(), homeTeamCoordinate, awayTeamCoordinate);
                if (winningTeamCoordinate.equals(homeTeamCoordinate)) {
                    homeTeamWins++;
                } else {
                    awayTeamWins++;
                }
            }
        }
        // Compute the winningTeamCoordinate based on the which team has the most votes (tie goes to home team)
        TeamCoordinate teamCoordinate = computeNextRoundCoordinates(homeTeamCoordinate, homeTeamWins, awayTeamCoordinate, awayTeamWins);
        T winnerSeasonData = (homeTeamWins >= awayTeamWins) ? homeTeamSeasonData : awayTeamSeasonData;
        teamCoordinate.setName(winnerSeasonData.getTeamName());
        getTeamCoordinateSeasonDataMap().put(teamCoordinate, winnerSeasonData);
        log.debug("Winner: {} with {} wins in {} simulated matchups",
                teamCoordinate.getName(), teamCoordinate.getHomeWinNetworkVotes(), networkCandidates.size());
    }

    private TeamCoordinate computeWinner(final double[][] results,
                                         final TeamCoordinate homeTeamCoordinate,
                                         final TeamCoordinate awayTeamCoordinate) {
        TeamCoordinate winner;
        double[] homeTeamProbabilities = results[0];
        double[] awayTeamProbabilities = results[1];
        //
        // The winner is the team predicted to win more games
        int homeTeamVictories = 0;
        double homeTeamProbability = 0.0;
        int index = 0;
        for (double probability : homeTeamProbabilities) {
            if (probability > homeTeamProbability) {
                homeTeamProbability = probability; // highest probability so far
                homeTeamVictories = index; // highest probability occurs at index
            }
            index++;
        }
        int awayTeamVictories = 0;
        double awayTeamProbability = 0.0;
        index = 0;
        for (double probability : awayTeamProbabilities) {
            if (probability > awayTeamProbability) {
                awayTeamProbability = probability;
                awayTeamVictories = index;
            }
            index++;
        }
        if (homeTeamVictories > awayTeamVictories) {
            // Home team is the winner
            winner = homeTeamCoordinate;
        } else if (homeTeamVictories == awayTeamVictories) {
            // Whichever has the highest probability for the same number of victories (that is, the network is more sure)
            winner = (homeTeamProbability > awayTeamProbability) ? homeTeamCoordinate : awayTeamCoordinate;
            log.warn("TIE-BREAKER: Giving the win to team: {} (hp/ap: {}/{})", winner.getName(), homeTeamProbability, awayTeamProbability);
        } else {
            // Away team is winner
            winner = awayTeamCoordinate;
        }
        log.debug(String.format("Winner: %s: %s (%s) vs %s (%s): hv/hp %s/%s, av/ap %s/%s",
                winner.getName(),
                homeTeamCoordinate.getName(), Arrays.toString(homeTeamProbabilities), // vs
                awayTeamCoordinate.getName(), Arrays.toString(awayTeamProbabilities), //:
                homeTeamVictories, homeTeamProbability,
                awayTeamVictories, awayTeamProbability
        ));

        return winner;
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

    protected Optional<RecordReaderDataSetIterator> computeRecordIterator(final T homeTeamSeasonData,
                                                                          final T awayTeamSeasonData,
                                                                          final NetworkParameters networkParameters) {
        Optional<RecordReaderDataSetIterator> ret;
        //
        // Compute one row of data with home team first
        String[] home = networkParameters.transformRow(seasonDataAsDoubleList(homeTeamSeasonData));
        //
        // Compute another row of data with away team first
        String[] away = networkParameters.transformRow(seasonDataAsDoubleList(awayTeamSeasonData));
        //
        // Wrap both rows in a CSVRecordReader, with all the bells and whistles and return the appropriate iterator
        StringBuilder stringSource = new StringBuilder();
        String homeString = String.join(",", home);
        stringSource.append(homeString);
        stringSource.append("\n");
        String awayString = String.join(",", away);
        stringSource.append(awayString);
        stringSource.append("\n");
        StringReader stringReader = new StringReader(stringSource.toString());
        try {
            InputStream inputStream = new ByteArrayInputStream(CharStreams.toString(stringReader).getBytes());
            RecordReader tournamentReader = new CSVRecordReader(0, ',');
            InputStreamInputSplit split = new InputStreamInputSplit(inputStream);
            tournamentReader.initialize(split);
            RecordReaderDataSetIterator tournamentIterator = new RecordReaderDataSetIterator(tournamentReader, BATCH_SIZE);
            ret = Optional.of(tournamentIterator);
        } catch (IOException | InterruptedException e) {
            String message = String.format("Error creating InputStream from CSV data:\n%s", stringSource.toString());
            log.error(message, e);
            throw new RuntimeException(e);
        }
        return ret;
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
            csvWriter.writeNext(new String[] { "Number of", "networks used", "for", String.format("simulation: %d", networksUsed.size()) });
            csvWriter.writeNext(new String[] { "Details:" });
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
            log.error("Could not find winning team coordinate: {}, returning the default", teamCoordinate);
        }
        return ret;
    }

}
