/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.menus;

import com.jstevenperry.sportsnet.framework.AbstractNetworkEvaluator;
import com.jstevenperry.sportsnet.framework.AbstractNetworkRunner;
import com.jstevenperry.sportsnet.framework.AbstractNetworkTrainer;
import com.jstevenperry.sportsnet.framework.model.AbstractSeasonData;
import com.jstevenperry.sportsnet.framework.model.NetworkCandidate;
import com.jstevenperry.sportsnet.framework.model.NetworkParameters;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.framework.persistence.TournamentParticipantDao;
import com.jstevenperry.sportsnet.framework.persistence.TournamentResultDao;
import com.jstevenperry.sportsnet.framework.util.NetworkPersister;
import com.jstevenperry.sportsnet.framework.util.NetworkUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractNetworkMenu<T extends AbstractSeasonData> {
    private static final Logger log = LoggerFactory.getLogger(AbstractNetworkMenu.class);

    private final List<NetworkCandidate> unsavedNetworks = new ArrayList<>();

    // Dependencies go here
    private final SeasonDataDao<T> seasonDataDao;
    private final TournamentResultDao tournamentResultDao;
    private final TournamentParticipantDao tournamentParticipantDao;
    private final AbstractNetworkTrainer<T> networkTrainer;
    private final AbstractNetworkEvaluator<T> networkEvaluator;
    private final AbstractNetworkRunner<T> networkRunner;

    /**
     * Save NetworkParameters so last settings are retained between
     * training runs (it's a pain to have to re-enter everything all
     * the time).
     */
    private NetworkParameters networkParametersGlobal;

    public AbstractNetworkMenu(final SeasonDataDao<T> seasonDataDao,
                               final TournamentResultDao tournamentResultDao,
                               final TournamentParticipantDao tournamentParticipantDao,
                               final AbstractNetworkRunner<T> networkRunner,
                               final AbstractNetworkTrainer<T> networkTrainer,
                               final AbstractNetworkEvaluator<T> networkEvaluator) {
        this.seasonDataDao = seasonDataDao;
        this.tournamentResultDao = tournamentResultDao;
        this.tournamentParticipantDao = tournamentParticipantDao;
        this.networkRunner = networkRunner;
        this.networkTrainer = networkTrainer;
        this.networkEvaluator = networkEvaluator;
    }

    /**
     * Returns the title of the network menu, must be implemented by
     * subclasses.
     */
    public abstract String getMenuTitle();

    /**
     * The number of output classes this network has. Must
     * be implemented by subclasses.
     */
    public abstract Integer getNumberOfOutputClasses();

    /**
     * The type of network, used to discriminate between artifacts
     * (like saved networks) among different types of networks that
     * are imcompatible with one another.
     */
    public abstract String getNetworkType();

    /**
     * Entry point for the main network menu.
     */
    public void go() {
        //
        // No arguments - prompt the user for EVERYTHING
        BufferedReader scanner = new BufferedReader(new InputStreamReader(System.in));
        //
        // Display main menu. When it terminates, so does program.
        NetworkChoice menuChoice = NetworkChoice.UNKNOWN;
        while (menuChoice != NetworkChoice.QUIT) {
            try {
                //
                // Display the main menu, let the user make their choice
                menuChoice = displayMainMenu(scanner);
                //
                // Process the user's choice
                menuChoice = processMenuChoice(scanner, menuChoice);
            } catch (Throwable t) {
                //
                // Something bad happened, but we soldier on...
                log.error("Bad things happened, man: {}", t.getLocalizedMessage(), t);
            }
        }
        //
        // Bye now.
        log.info("Exiting network menu.");
    }

    /**
     * Display the main menu. Javadoc. Check.
     */
    private NetworkChoice displayMainMenu(final BufferedReader scanner) throws IOException {
        NetworkChoice ret = NetworkChoice.UNKNOWN;
        //
        // Show the options and camp out until they decide to quit
        // Use System.out.println() because it just looks better
        System.out.println("Welcome to " + getMenuTitle());
        System.out.println("This is the main menu. What do you want to do?");
        for (NetworkChoice menuItem : NetworkChoice.values()) {
            if (menuItem.isSuitableForDisplay()) {
                System.out.printf("%d - %s%n", menuItem.getOrdinalValue(), menuItem.getDisplayValue());
            }
        }
        System.out.println("==> ");
        String line = scanner.readLine();
        if (StringUtils.isNotEmpty(line)) {
            //
            // Get the menu choice from the number entered by the user or UNKNOWN
            ret = NetworkChoice.from(Integer.parseInt(StringUtils.strip(line)));
        } else {
            log.error("{}: {}", ret.getDisplayValue(), line);
        }
        return ret;
    }

    private NetworkChoice processMenuChoice(final BufferedReader scanner,
                                            final NetworkChoice menuChoice) throws IOException, InterruptedException {
        NetworkChoice ret = menuChoice;
        switch (menuChoice) {
            case QUIT:
                if (unsavedNetworks.size() > 0) {
                    while (true) {
                        System.out.println("You have unsaved networks. Are you sure you want to quit (y/n)?");
                        String input = StringUtils.strip(scanner.readLine());
                        if (input.equalsIgnoreCase("y")) {
                            break;
                        } else if (input.equalsIgnoreCase("n")) {
                            // Set the return value to UNKNOWN so the main menu displays again
                            ret = NetworkChoice.UNKNOWN;
                            break;
                        }
                    }
                }
                break;
            case TRAIN_NETWORK:
                NetworkCandidate trainedNetwork = networkTrainer.go(
                        networkParametersGlobal, seasonDataDao, tournamentResultDao, tournamentParticipantDao, getNumberOfOutputClasses(), getNetworkType()
                );
                unsavedNetworks.add(trainedNetwork);
                networkParametersGlobal = trainedNetwork.getNetworkParameters();
                networkParametersGlobal.setNetworkSaved(false); // Reset, this is a shared object
                break;
            case TRAIN_NETWORK_BATCH:
                networkParametersGlobal = networkTrainer.goBatch(
                        networkParametersGlobal, unsavedNetworks, seasonDataDao, tournamentResultDao, tournamentParticipantDao, getNumberOfOutputClasses(), getNetworkType()
                );
                networkParametersGlobal.setNetworkSaved(false); // Reset, this is a shared object
                break;
            case EVALUATE_NETWORK:
                networkEvaluator.go(
                        scanner, unsavedNetworks, seasonDataDao, tournamentResultDao, tournamentParticipantDao, getNetworkType()
                );
                break;
            case RUN_NETWORK:
                networkRunner.go(
                        scanner, seasonDataDao, getNetworkType()
                );
                break;
            case PERSIST_UNSAVED_NETWORKS:
                if (unsavedNetworks.isEmpty()) {
                    System.out.println("No networks to save.");
                } else {
                    NetworkPersister.persistNetworks(scanner, unsavedNetworks);
                }
                break;
            case LIST_SAVED_NETWORKS:
                NetworkUtils.displayNetworks(getNetworkType());
                break;
            case CLEAR_UNSAVED_NETWORKS:
                log.info("Discarding {} unsaved networks...", unsavedNetworks.size());
                unsavedNetworks.clear();
                log.info("Done.");
                break;
        }
        return ret;
    }

}
