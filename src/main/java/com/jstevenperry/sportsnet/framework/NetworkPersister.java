package com.jstevenperry.sportsnet.framework;

import com.jstevenperry.sportsnet.framework.model.NetworkCandidate;
import com.jstevenperry.sportsnet.framework.model.NetworkParameters;
import com.jstevenperry.sportsnet.framework.util.NetworkUtils;
import org.apache.commons.lang.StringUtils;
import org.deeplearning4j.util.ModelSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class NetworkPersister {

  public static final String NETWORK_PARAMETERS_KEY = "network.parameters";
  private static final Logger log = LoggerFactory.getLogger(NetworkPersister.class);
  private static final byte KEEP_LOOPING = -1;

  public enum Action {
    PERSIST,
    EVALUATE,
    RUN
  }

  public static void persistNetworks(final BufferedReader scanner,
                                     final List<NetworkCandidate> networkCandidateList) throws IOException {
    List<NetworkCandidate> networkCandidates = displayNetworkSelectionMenu(scanner, networkCandidateList, Action.PERSIST);
    if (!networkCandidates.isEmpty()) {
      networkCandidates.forEach(networkCandidate -> {
        saveSelectedNetworkOrNot(scanner, networkCandidate);
        networkCandidateList.remove(networkCandidate);
      });
    }
  }

  /**
   * Displays a menu of networks from the networkCandidates list, asks
   * the user to pick one, and returns their choice.
   */
  public static List<NetworkCandidate> displayNetworkSelectionMenu(final BufferedReader scanner,
                                                                       final List<NetworkCandidate> networkCandidates,
                                                                       final Action action) throws IOException {
    List<NetworkCandidate> ret = new ArrayList<>();
    byte networkNumber = KEEP_LOOPING;
    while (networkNumber == KEEP_LOOPING && !networkCandidates.isEmpty()) {
      System.out.printf("Enter the number of the network you want to %s (enter 0 to quit):%n", action);
      NetworkUtils.displayNetworkList(networkCandidates);
      System.out.println("==> ");
      String line = scanner.readLine();
      if (StringUtils.isNotEmpty(line)) {
        if (StringUtils.isNumeric(line)) {
          networkNumber = Byte.parseByte(StringUtils.strip(line));
          if (networkNumber < 0 || networkNumber > networkCandidates.size()) {
            // Invalid choice, keep looping
            log.warn("Invalid choice: {}, please try again.", networkNumber);
            networkNumber = KEEP_LOOPING;
            continue;
          }
          if (networkNumber == 0) {
            break;
          } else {
            ret.add(networkCandidates.get(networkNumber - 1));
          }
        } else if (line.equalsIgnoreCase("ALL")) {
          if (action == Action.RUN) {
            // Return all networks
            ret.addAll(networkCandidates);
            break;
          } else {
            log.error("Selecting ALL networks for action '{}' is not allowed. Please try again.", action);
            networkNumber = KEEP_LOOPING;
          }
        }
      }
    }
    return ret;
  }

  private static void saveSelectedNetworkOrNot(final BufferedReader scanner,
                                               final NetworkCandidate networkCandidate) {
    String yesOrNo = null;
    while (yesOrNo == null) {
      System.out.printf("Save network %s (y/n)?%n", networkCandidate.getNetworkParameters().getNetworkLayout());
      try {
        yesOrNo = StringUtils.strip(scanner.readLine());
      } catch (IOException e) {
        log.error(String.format("Error reading response: %s, network will not be saved.", e.getLocalizedMessage()));
        yesOrNo = "n";
      }
      if (yesOrNo.equalsIgnoreCase("y")) {
        if (saveNetwork(networkCandidate)) {
          log.info("Network saved.");
        }
      } else if (yesOrNo.equalsIgnoreCase("n")) {
        break;
      } else {
        yesOrNo = null; // Keep looping until we get something we understand
      }
    }
  }

  /**
   * Saves the specified network and returns true if successful, false otherwise.
   */
  public static boolean saveNetwork(final NetworkCandidate networkCandidate) {
    boolean ret = false;
    NetworkParameters networkParameters = networkCandidate.getNetworkParameters();
    String networkFileName = NetworkUtils.fetchNetworkDirectoryAndCreateIfNecessary(networkCandidate.getNetworkParameters().getNetworkType())
      + File.separatorChar + generateNetworkFileNameBase(networkCandidate) + ".zip";
    log.info("Saving network: {} as {}", networkParameters.getNetworkLayout(), networkFileName);
    try {
      File modelFile = new File(networkFileName);
      ModelSerializer.writeModel(networkCandidate.getMultiLayerNetwork(), modelFile, true);
      networkParameters.setNetworkSaved(true);
      ModelSerializer.addObjectToFile(modelFile, NETWORK_PARAMETERS_KEY, networkParameters);
      ret = true;
    } catch (IOException e) {
      String message = String.format("Error saving network file '%s': %s", networkFileName, e.getLocalizedMessage());
      log.error(message, e);
    }
    return ret;
  }

  public static String generateNetworkFileNameBase(final NetworkCandidate networkCandidate) {
    NetworkParameters networkParameters = networkCandidate.getNetworkParameters();
    return String.format("%s-Net-%s-%s-%s.dl4j",
      networkParameters.getNetworkType(),
      networkParameters.getNetworkLayout(),
      networkParameters.getWhenTrained().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")),
      LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
    );
  }
}
