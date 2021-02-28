/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet.framework;

import com.jstevenperry.sportsnet.framework.model.AbstractSeasonData;
import com.jstevenperry.sportsnet.framework.model.GameCoordinate;
import com.jstevenperry.sportsnet.framework.model.NetworkCandidate;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.framework.util.NetworkPersister;
import com.jstevenperry.sportsnet.framework.util.NetworkUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class AbstractNetworkRunner<T extends AbstractSeasonData> extends AbstractNetworkBase<T> {

    private static final Byte KEEP_LOOPING = -1;

    private static final Logger log = LoggerFactory.getLogger(AbstractNetworkRunner.class);

    public void go(final BufferedReader scanner,
                   final SeasonDataDao<T> seasonDataDao,
                   final String networkType) throws IOException {
        //
        // Load plugins
        List<AbstractTournamentRunnerPlugin<T>> tournamentRunnerPlugins = loadPlugins(seasonDataDao);
        if (!tournamentRunnerPlugins.isEmpty()) {
            //
            // Prompt the user for the tournament plugin they want to run
            Optional<AbstractTournamentRunnerPlugin<T>> tournamentRunnerPluginMaybe = selectPluginToRunMenu(scanner, tournamentRunnerPlugins);
            if (tournamentRunnerPluginMaybe.isPresent()) {
                //
                // Ask the user for a performance threshold
                int performanceThreshold = scanPerformanceThreshold(scanner);
                //
                // Ask the user what they want to do
                AbstractTournamentRunnerPlugin<T> tournamentRunnerPlugin = tournamentRunnerPluginMaybe.get();
                List<NetworkCandidate> allSavedNetworks = NetworkUtils.loadNetworks(networkType);
                List<NetworkCandidate> suitableNetworks = tournamentRunnerPlugin.computeValidNetworksForTournamentYear(allSavedNetworks);
                if (!suitableNetworks.isEmpty()) {
                    //
                    // Filter out networks below the threshold
                    double performanceThresholdDouble = performanceThreshold / 100.0;
                    System.out.printf("Filtering networks above %3.2f%%...%n", performanceThresholdDouble * 100.0);
                    suitableNetworks = suitableNetworks.stream()
                            .filter(networkCandidate -> networkCandidate.getNetworkParameters().getNetworkAccuracy() >= performanceThresholdDouble)
                            .collect(Collectors.toList());
                    System.out.printf("Displaying %d networks.%n", suitableNetworks.size());
                    List<NetworkCandidate> selectedNetworks = NetworkPersister.displayNetworkSelectionMenu(scanner, suitableNetworks, NetworkPersister.Action.RUN);
                    List<GameCoordinate> gameList = tournamentRunnerPlugin.createTournamentGameList();
                    tournamentRunnerPlugin.runTournament(gameList, selectedNetworks);
                } else {
                    log.warn("NO NETWORKS SUITABLE FOR TOURNAMENT YEAR {}", tournamentRunnerPlugin.getTournamentYear());
                }
            }
        } else {
            log.error("No TournamentRunnerPlugins, cannot run a network to simulate a tournament!");
        }
    }

    private Integer scanPerformanceThreshold(final BufferedReader scanner) {
        int ret = 0;
        String userInput = null;
        while (StringUtils.isEmpty(userInput)) {
            System.out.println("Enter network precision threshold (specify a number between 0 - 100) to filter networks to choose from.");
            System.out.println("Only networks that perform above this threshold will be shown (enter 0 to choose all networks)");
            System.out.print("==> ");
            try {
                // Get user input - a floating point number
                userInput = scanner.readLine();
                if (StringUtils.isNotEmpty(userInput)) {
                    ret = Integer.parseInt(userInput);
                    break;
                }
            } catch (Exception e) {
                String message = String.format("Error: %s, please try again.", e.getLocalizedMessage());
                log.error(message);
                userInput = null; // Keep looping
            }
        }
        return ret;
    }

    protected abstract List<AbstractTournamentRunnerPlugin<T>> loadPlugins(SeasonDataDao<T> seasonDataDao);

    public Optional<AbstractTournamentRunnerPlugin<T>> selectPluginToRunMenu(final BufferedReader scanner,
                                                                             final List<? extends AbstractTournamentRunnerPlugin<T>> tournamentRunnerPlugins) throws IOException {
        Optional<AbstractTournamentRunnerPlugin<T>> ret = Optional.empty();
        // Ask the user which plugin they want to run and for what year, and if it's doable, return the plugin
        byte pluginNumber = KEEP_LOOPING;
        while (pluginNumber == KEEP_LOOPING && !tournamentRunnerPlugins.isEmpty()) {
            System.out.println("Select the year you want to simulate (enter 0 to quit):");
            System.out.println("Plugin#\t\tTournament Year");
            int index = 0;
            for (AbstractTournamentRunnerPlugin<T> tournamentRunnerPlugin : tournamentRunnerPlugins) {
                System.out.printf("   %d %24s%n", index + 1, tournamentRunnerPlugin.getTournamentYear().toString());
                index++;
            }
            String line = scanner.readLine();
            if (StringUtils.isNotEmpty(line)) {
                pluginNumber = Byte.parseByte(StringUtils.strip(line));
                if (pluginNumber < 0 || pluginNumber > tournamentRunnerPlugins.size()) {
                    pluginNumber = KEEP_LOOPING;
                    continue;
                }
                if (pluginNumber == 0) {
                    break;
                }
            } else {
                System.out.printf("%s is not a valid choice.%n", line);
                pluginNumber = KEEP_LOOPING;
            }
        }
        if (pluginNumber != 0) {
            ret = Optional.of(tournamentRunnerPlugins.get(pluginNumber - 1));
        }
        return ret;
    }

}
