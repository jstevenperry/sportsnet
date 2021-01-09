package com.jstevenperry.sportsnet.framework;

import com.jstevenperry.sportsnet.framework.model.AbstractSeasonData;
import com.jstevenperry.sportsnet.framework.model.GameCoordinate;
import com.jstevenperry.sportsnet.framework.model.NetworkCandidate;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.framework.util.NetworkUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class AbstractNetworkRunner<T extends AbstractSeasonData> extends AbstractNetworkBase<T> {

  private static final Byte KEEP_LOOPING = -1;

  private static final Logger log = LoggerFactory.getLogger(AbstractNetworkRunner.class);

  public void go(final BufferedReader scanner,
                 final List<NetworkCandidate> networkCandidateList,
                 final SeasonDataDao<T> seasonDataDao,
                 final String networkType) throws IOException {
    //
    // Load saved networks
    log.info("Loading networks, please be patient...");
    List<NetworkCandidate> savedNetworks = NetworkUtils.loadNetworks(networkType);
    //
    // Add loaded networks to networkCandidateList
    List<NetworkCandidate> allNetworks = new ArrayList<>();
    allNetworks.addAll(savedNetworks);
    allNetworks.addAll(networkCandidateList);
    if (!allNetworks.isEmpty()) {
      //
      // Load plugins
      List<AbstractTournamentRunnerPlugin<T>> tournamentRunnerPlugins = loadPlugins(seasonDataDao);
      if (!tournamentRunnerPlugins.isEmpty()) {
        //
        // Ask the user what they want to do
        promptUser(scanner, allNetworks, tournamentRunnerPlugins);
      } else {
        log.error("No TournamentRunnerPlugins, cannot run a network to simulate a tournament!");
      }
    } else {
      log.warn("No networks to run. Train some networks and try again.");
    }
  }

  protected abstract List<AbstractTournamentRunnerPlugin<T>> loadPlugins(SeasonDataDao<T> seasonDataDao);

  private void promptUser(final BufferedReader scanner,
                          final List<NetworkCandidate> allNetworks,
                          final List<AbstractTournamentRunnerPlugin<T>> tournamentRunnerPlugins) throws IOException {
    //
    // Get the network the user wants to run
    List<NetworkCandidate> selectedNetworks = NetworkPersister.displayNetworkSelectionMenu(scanner, allNetworks, NetworkPersister.Action.RUN);
    if (!selectedNetworks.isEmpty()) {
      //
      // Prompt the user for the tournament plugin they want to run
      Optional<AbstractTournamentRunnerPlugin<T>> tournamentRunnerPluginMaybe = selectPluginToRunMenu(scanner, tournamentRunnerPlugins);
      if (tournamentRunnerPluginMaybe.isPresent()) {
        AbstractTournamentRunnerPlugin<T> tournamentRunnerPlugin = tournamentRunnerPluginMaybe.get();
        List<GameCoordinate> gameList = tournamentRunnerPlugin.createTournamentGameList();
        tournamentRunnerPlugin.runTournament(gameList, selectedNetworks);
      }
    } else {
      log.info("No network selected, quitting.");
    }
  }

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
