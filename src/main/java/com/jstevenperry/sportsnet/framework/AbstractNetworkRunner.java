package com.jstevenperry.sportsnet.framework;

import com.jstevenperry.sportsnet.framework.model.GameCoordinate;
import com.jstevenperry.sportsnet.framework.model.NetworkCandidate;
import com.jstevenperry.sportsnet.framework.model.NetworkParameters;
import com.jstevenperry.sportsnet.framework.util.NetworkUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class AbstractNetworkRunner {

  private static final Logger log = LoggerFactory.getLogger(AbstractNetworkRunner.class);

  public void go(final BufferedReader scanner, final List<NetworkCandidate> networkCandidateList) throws IOException {
    //
    // Load saved networks
    List<NetworkCandidate> savedNetworks = NetworkUtils.loadNetworks();
    //
    // Add loaded networks to networkCandidateList
    List<NetworkCandidate> allNetworks = new ArrayList<>();
    allNetworks.addAll(savedNetworks);
    allNetworks.addAll(networkCandidateList);
    if (!allNetworks.isEmpty()) {
      //
      // Load plugins
      List<AbstractTournamentRunnerPlugin> tournamentRunnerPlugins = loadPlugins();
      if (!tournamentRunnerPlugins.isEmpty()) {
        //
        // Ask the user what they want to do
        promptUser(scanner, allNetworks, tournamentRunnerPlugins);
      } else {
        log.error("No TournamentRunnerPlugins, cannot run a network to simulate a tournament!");
      }
    } else {
      log.info("No networks to run. Train some networks and try again.");
    }
  }

  protected abstract List<AbstractTournamentRunnerPlugin> loadPlugins();// {
//    List<TournamentRunnerPlugin> ret = Arrays.asList(
//      //new TournamentRunnerPlugin2019(MainMenu.getApplicationContext())
//      new TournamentRunnerPlugin2018(com.makotojava.ncaabb.dl4j.MainMenu.getApplicationContext())
//    );
//
//    return ret;
//  }

  private void promptUser(final BufferedReader scanner,
                                 final List<NetworkCandidate> allNetworks,
                                 final List<AbstractTournamentRunnerPlugin> tournamentRunnerPlugins) throws IOException {
    //
    // Get the network the user wants to run
    Optional<NetworkCandidate> selectedNetwork = NetworkPersister.displayNetworkSelectionMenu(scanner, allNetworks, NetworkPersister.ACTION_WORK_WITH);
    if (selectedNetwork.isPresent()) {
      NetworkCandidate networkCandidate = selectedNetwork.get();
      NetworkParameters networkParameters = networkCandidate.getNetworkParameters();
      //
      // Prompt the user for the tournament plugin they want to run
      Optional<AbstractTournamentRunnerPlugin> tournamentRunnerPluginMaybe = AbstractTournamentRunnerPlugin.selectPluginToRunMenu(scanner, tournamentRunnerPlugins);
      if (tournamentRunnerPluginMaybe.isPresent()) {
        AbstractTournamentRunnerPlugin tournamentRunnerPlugin = tournamentRunnerPluginMaybe.get();
        //
        // If the network is valid for the specified tournament year, then run the tournament
        if (networkParameters.isValidYearForTournamentPrediction(tournamentRunnerPlugin.getTournamentYear())) {
          List<GameCoordinate> gameList = tournamentRunnerPlugin.createTournamentGameList();
          tournamentRunnerPlugin.runTournament(gameList, networkCandidate);
        }
      }
    } else {
      System.out.println("No network selected, quitting.");
    }
  }

}
