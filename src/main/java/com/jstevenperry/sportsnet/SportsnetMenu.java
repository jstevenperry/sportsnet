package com.jstevenperry.sportsnet;

import com.jstevenperry.sportsnet.framework.menus.NetworkMenu;
import com.jstevenperry.sportsnet.framework.menus.SportsnetChoice;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.framework.persistence.TournamentParticipantDao;
import com.jstevenperry.sportsnet.framework.persistence.TournamentResultDao;
import com.jstevenperry.sportsnet.ncaabb.NcaaBbNetworkRunner;
import com.jstevenperry.sportsnet.ncaabb.NcaaBbNetworkTrainer;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

  @Autowired
  private SeasonDataDao seasonDataDao;

  public static void main(final String[] args) {
    ApplicationContext applicationContext = new AnnotationConfigApplicationContext(ApplicationConfig.class);

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
    while (menuChoice == SportsnetChoice.UNKNOWN) {
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
    System.out.println("Welcome to Sports Net.");
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

  private static void processMenuChoice(final SportsnetChoice menuChoice,
                                        final ApplicationContext applicationContext) throws IOException, InterruptedException {
    switch (menuChoice) {
      case NCAA_MENS_BASKETBALL:
        handleNcaaMensBasketball(applicationContext);
        break;
      case NHL:
        handleNhl(applicationContext);
        break;
    }
  }

  private static void handleNcaaMensBasketball(final ApplicationContext applicationContext) {
    //
    // Get beans from Spring config
    SeasonDataDao seasonDataDao = (SeasonDataDao) applicationContext.getBean("ncaaBbSeasonDataDao");
    TournamentParticipantDao tournamentParticipantDao = (TournamentParticipantDao) applicationContext.getBean("ncaaBbTournamentParticipantDao");
    TournamentResultDao tournamentResultDao = (TournamentResultDao) applicationContext.getBean("ncaaBbTournamentResultDao");

    //
    // Create the network runner and network trainer implementations for NCAA BB
    NcaaBbNetworkRunner networkRunner = new NcaaBbNetworkRunner(seasonDataDao);
    NcaaBbNetworkTrainer networkTrainer = new NcaaBbNetworkTrainer();

    //
    // Create the network menu and pass its dependencies in.
    NetworkMenu networkMenu = new NetworkMenu(seasonDataDao, tournamentResultDao, tournamentParticipantDao, networkRunner, networkTrainer);
    //
    // Dependencies injected. Fire up the network Menu.
    networkMenu.go();
  }

  private static void handleNhl(final ApplicationContext applicationContext) {
  }

}
