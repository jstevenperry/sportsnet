/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet;

import com.jstevenperry.sportsnet.framework.menus.AbstractNetworkMenu;
import com.jstevenperry.sportsnet.framework.menus.SportsnetChoice;
import com.jstevenperry.sportsnet.nba.menus.NbaNetworkMenu;
import com.jstevenperry.sportsnet.nba.model.NbaSeasonData;
import com.jstevenperry.sportsnet.ncaabb.menus.NcaaBbNetworkMenu;
import com.jstevenperry.sportsnet.ncaabb.model.NcaaBbSeasonData;
import com.jstevenperry.sportsnet.nfl.menus.NflNetworkMenu;
import com.jstevenperry.sportsnet.nfl.model.NflSeasonData;
import com.jstevenperry.sportsnet.nhl.menus.NhlNetworkMenu;
import com.jstevenperry.sportsnet.nhl.model.NhlSeasonData;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * This is the main driver for the Sportsnet application.
 * <p>
 * All of the menus in the system are invoked from here.
 * <p>
 * All dependencies are fetched from Spring here and passed
 * down through constructor calls.
 */
@SpringBootApplication
public class SportsnetMenu {

    private static final Logger log = LoggerFactory.getLogger(SportsnetMenu.class);
    private static final String SPORTSNET_VERSION = "2.6.1";

    public static void main(final String[] args) {
        SpringApplication.run(SportsnetMenu.class, args);
        //
        // Read the spring configuration
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(ApplicationConfig.class);
        //
        // Show the "Pick your sport" menu
        SportsnetMenu.go(applicationContext);
    }

    /**
     * Entry point for the main network menu.
     */
    public static void go(final ApplicationContext applicationContext) {
        //
        // No arguments - prompt the user for EVERYTHING
        BufferedReader scanner = new BufferedReader(new InputStreamReader(System.in));
        //
        // Display main menu. When it terminates, so does program.
        SportsnetChoice menuChoice = SportsnetChoice.UNKNOWN;
        while (menuChoice != SportsnetChoice.QUIT) {
            try {
                //
                // Display the main menu, let the user make their choice
                menuChoice = displayMainMenu(scanner);
                //
                // Process the user's choice
                processMenuChoice(menuChoice, applicationContext);
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
    private static SportsnetChoice displayMainMenu(final BufferedReader scanner) throws IOException {
        SportsnetChoice ret = SportsnetChoice.UNKNOWN;
        //
        // Show the options and camp out until they decide to quit
        // Use System.out.println() because it just looks better
        System.out.printf("Welcome to Sports Net v%s.%n", SPORTSNET_VERSION);
        System.out.println("This is the main menu. Choose your sport.");
        for (SportsnetChoice menuItem : SportsnetChoice.values()) {
            if (menuItem.isSuitableForDisplay()) {
                System.out.printf("%d - %s%n", menuItem.getOrdinalValue(), menuItem.getDisplayName());
            }
        }
        System.out.println("==> ");
        String line = scanner.readLine();
        if (StringUtils.isNotEmpty(line)) {
            //
            // Get the menu choice from the number entered by the user or UNKNOWN
            ret = SportsnetChoice.from(Integer.parseInt(StringUtils.strip(line)));
        } else {
            log.error("{}: {}", ret.getDisplayName(), line);
        }
        return ret;
    }

    private static void processMenuChoice(final SportsnetChoice menuChoice, final ApplicationContext applicationContext) {
        switch (menuChoice) {
            case NCAA_MENS_BASKETBALL:
                //
                // Create the network menu and pass its dependencies in.
                AbstractNetworkMenu<NcaaBbSeasonData> ncaaBbNetworkMenu = new NcaaBbNetworkMenu(applicationContext);
                //
                // Fire up the network Menu.
                ncaaBbNetworkMenu.go();
                break;
            case NHL:
                AbstractNetworkMenu<NhlSeasonData> nhlNetworkMenu = new NhlNetworkMenu(applicationContext);
                //
                // Fire up the network Menu.
                nhlNetworkMenu.go();
                break;
            case NFL:
                AbstractNetworkMenu<NflSeasonData> nflNetworkMenu = new NflNetworkMenu(applicationContext);
                //
                // Fire up the network menu
                nflNetworkMenu.go();
                break;
            case NBA:
                AbstractNetworkMenu<NbaSeasonData> nbaNetworkMenu = new NbaNetworkMenu(applicationContext);
                //
                // Fire up the network menu
                nbaNetworkMenu.go();
                break;
            case QUIT:
                log.info("Quitting...");
                break;
        }
    }

}
