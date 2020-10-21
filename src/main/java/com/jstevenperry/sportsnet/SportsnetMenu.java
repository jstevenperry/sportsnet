package com.jstevenperry.sportsnet;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * This is the main driver for the Sportsnet application.
 *
 * All of the menus in the system are invoked from here.
 *
 * All dependencies are fetched from Spring here and passed
 * down through constructor calls.
 */
public class SportsnetMenu {
  public static void main(final String[] args) {
    ApplicationContext applicationContext = new AnnotationConfigApplicationContext(ApplicationConfig.class);

    // TODO: Show the "Pick your sport" menu
  }
}
