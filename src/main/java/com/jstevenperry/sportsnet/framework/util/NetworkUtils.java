package com.jstevenperry.sportsnet.framework.util;

import com.jstevenperry.sportsnet.framework.NetworkPersister;
import com.jstevenperry.sportsnet.framework.model.NetworkCandidate;
import com.jstevenperry.sportsnet.framework.model.NetworkParameters;
import org.apache.commons.lang3.StringUtils;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class NetworkUtils {

  private static final Logger log = LoggerFactory.getLogger(NetworkUtils.class);

  public static final String DL4J_NETWORK_EPILOGUE = "dl4j.zip";

  public static List<NetworkCandidate> loadNetworks() {
    List<NetworkCandidate> ret = new ArrayList<>();
    String networkDirectoryName = NetworkUtils.fetchNetworkDirectoryAndCreateIfNecessary();
    File networkDirectory = new File(networkDirectoryName);
    //
    // Get a listing of *.dl4j.zip
    String[] dl4jNetworkFileNames = networkDirectory.list((dir, name) -> name.endsWith(DL4J_NETWORK_EPILOGUE));
    if (dl4jNetworkFileNames != null && dl4jNetworkFileNames.length > 0) {
      String[] networkFileNames = new String[dl4jNetworkFileNames.length];
      log.info(String.format("Found %d networks at %s", dl4jNetworkFileNames.length, networkDirectoryName));
      int index = 0;
      //
      // Need the full path to the network file
      for (String dl4jNetworkFileName: dl4jNetworkFileNames) {
        networkFileNames[index++] = networkDirectoryName + File.separator + dl4jNetworkFileName;
      }
      ret = loadNetworkFiles(networkFileNames);
    } else {
      log.warn(String.format("No networks found at %s", networkDirectoryName));
    }
    return ret;
  }

  private static List<NetworkCandidate> loadNetworkFiles(final String[] networkFileNames) {
    List<NetworkCandidate> ret = new ArrayList<>();
    for (String networkFileName: networkFileNames) {
      //
      // Load the network
      File networkFile = new File(networkFileName);
      try {
        MultiLayerNetwork network = ModelSerializer.restoreMultiLayerNetwork(networkFile, true);
        if (network == null) {
          log.error(String.format("Network from file %s is null!", networkFileName));
          continue; // Do not add null network
        }
        NetworkParameters networkParameters = ModelSerializer.getObjectFromFile(networkFile, NetworkPersister.NETWORK_PARAMETERS_KEY);
        if (networkParameters == null) {
          log.error(String.format("Network parameters from file %s is null!", networkFileName));
          continue; // Do not add null network parameters
        }
        //
        // Everything looks good, add the loaded network to the return value
        ret.add(new NetworkCandidate(networkParameters, network));
      } catch (IOException e) {
        String message = String.format("Error loading network: %s", networkFileName);
        log.error(message, e);
      }
    }
    return ret;
  }

  public static void displayNetworkList(final List<NetworkCandidate> networkCandidates) {
    System.out.printf("%9s%21s%11s(%%)%35s%10s%45s%30s%15s%n",
      "Network #", "When Trained", "Accuracy", "Layer Structure", "Saved?", "Years (Training)", "Updater", "No. Epochs");
    int index = 0;
    for (NetworkCandidate networkCandidate : networkCandidates) {
      NetworkParameters networkParameters = networkCandidate.getNetworkParameters();
      System.out.printf("%9d%21s%13.2f%%%35s%10s%45s%30s%15d%n",
        index + 1,
        networkParameters.getWhenTrained().format(DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm")),
        networkParameters.getNetworkAccuracy() * 100.0,
        StringUtils.abbreviate(networkParameters.getNetworkLayout(), 32),
        networkParameters.isNetworkSaved() ? "Y": "N",
        networkParameters.getTrainingYearsString(),
        StringUtils.abbreviate(networkParameters.getUpdater().toString(), 24),
        networkParameters.getNumberOfEpochs());
      index++;
    }
  }

  public static void displayNetworks() throws IOException {
    BufferedReader scanner = new BufferedReader(new InputStreamReader(System.in));
    List<NetworkCandidate> networkCandidates = loadNetworks();
    while (true) {
      displayNetworkList(networkCandidates);
      System.out.println("Enter the number of a network to see more about it (just press Enter to quit).");
      String line = scanner.readLine();
      if (StringUtils.isEmpty(line)) {
        break;
      }
      if (StringUtils.isNumeric(line)) {
        int networkNumber = Integer.parseInt(line);
        if (networkNumber > 0 && networkNumber <= networkCandidates.size()) {
          System.out.println(networkCandidates.get(networkNumber - 1).getNetworkParameters());
        } else {
          System.out.printf("%d is not one of the choices. Please choose a network from the list (or just press enter to quit).%n", networkNumber);
        }
      } else {
        System.out.printf("%s is not a valid choice. Please choose a network from the list (or just press enter to quit).%n", line);
      }
    }
    System.out.println("Quitting.");
  }
  /**
   * Fetches the name of the network directory using NetworkProperties settings
   * and returns that name to the caller.
   *
   * @return
   */
  public static String fetchNetworkDirectoryAndCreateIfNecessary() {
    //
    // Compute the directory name from NetworkProperties settings
    String directoryName = NetworkProperties.getBaseDirectory() + File.separator
      + NetworkProperties.getNetworkDirectoryName();
    //
    // If necessary, create the directory
    createDirectoryIfNecessary(directoryName);
    //
    // Return the directory name to the caller
    return directoryName;
  }

  /**
   * Fetches the name of the training data directory using NetworkProperties settings
   * and returns that name to the caller.
   *
   * @return
   */
  public static String fetchTrainingDirectoryAndCreateIfNecessary() {
    //
    // Compute the directory name from NetworkProperties settings
    String directoryName = NetworkProperties.getBaseDirectory() + File.separator
      + NetworkProperties.getTrainingDirectoryName();
    //
    // If necessary, create the directory
    createDirectoryIfNecessary(directoryName);
    //
    // Return the directory name to the caller
    return directoryName;
  }

  /**
   * Fetches the name of the simulation directory using NetworkProperties settings
   * and returns that name to the caller.
   *
   * @return
   */
  public static String fetchSimulationDirectoryAndCreateIfNecessary() {
    //
    // Compute the directory name from NetworkProperties settings
    String directoryName = NetworkProperties.getBaseDirectory() + File.separator
      + NetworkProperties.getSimulationDirectoryName();
    //
    // If necessary, create the directory
    createDirectoryIfNecessary(directoryName);
    //
    // Return the directory name to the caller
    return directoryName;
  }

  /**
   * Helper method to create a directory. Insulates the caller from the details
   * of the Java API used to do this.
   *
   * @param directoryName
   */
  private static void createDirectoryIfNecessary(String directoryName) {
    try {
      File directory = new File(directoryName);
      if (!directory.exists()) {
        log.warn("Directory '" + directoryName + "' does not exist. Creating it now...");
        // Create the directory.
        Path networkDirectoryPath = Files.createDirectory(Paths.get(directoryName));
        log.warn("Network directory '" + networkDirectoryPath.toString() + "' created.");
      }
    } catch (IOException e) {
      String message = "I/O Exception occurred while creating directory '" + directoryName + "'. Cannot continue!";
      log.error(message, e);
      throw new RuntimeException(message, e);
    }
  }

  /**
   * Returns a NxNxNxN style string showing the layer structure
   * of a network that would have the specified layer structure.
   * The 0th element contains the number of neurons in the input
   * layer. The (<code>neuronLayerDescriptor</code>.size()-1)th element
   * contains the number of neurons in the output layer.
   *
   * @param neuronLayerDescriptor
   * @return
   */
  public static String generateLayerStructureString(List<Integer> neuronLayerDescriptor) {
    StringBuilder sb = new StringBuilder();
    int aa = 0;
    for (Integer neuronsCount : neuronLayerDescriptor) {
      if (aa++ > 0) {
        sb.append("x");
      }
      sb.append(Integer.toString(neuronsCount));
    }
    return sb.toString();
  }

  public static String computeDl4jCsvTrainingDataFileName(final Integer[] years) {
    StringBuilder sb = new StringBuilder();
    for (Integer year: years) {
      sb.append("_");
      sb.append(year.toString());
    }
    return fetchTrainingDirectoryAndCreateIfNecessary() + File.separator +
      "SeasonData" + sb.toString() + ".csv";
  }

  public static String computeDl4jCsvSimulationDataFileName(final Integer tournamentYear,
                                                            final String homeTeamName) {
    StringBuilder sb = new StringBuilder();
    sb.append(tournamentYear);
    sb.append('_');
    sb.append(homeTeamName);

    return fetchSimulationDirectoryAndCreateIfNecessary() + File.separator + sb.toString() + ".csv";
  }

  /**
   * Parses the specified network structure into an int array.
   * See getNetworks() for a description of the structure.
   */
  public static int[] parseNetworkStructure(String networkStructure) {
    List<Integer> structure = new ArrayList<>();
    StringTokenizer strtok = new StringTokenizer(networkStructure, "x");
    while (strtok.hasMoreTokens()) {
      try {
        int neuronsInLayer = Integer.parseInt(strtok.nextToken());
        structure.add(neuronsInLayer);
      } catch (NumberFormatException e) {
        String errorMessage = String.format("Cannot make sense out of network structure: '%s' (alpha characters in layers, maybe?)", networkStructure);
        log.error(errorMessage, e);
        throw new RuntimeException(errorMessage, e);
      }
    }
    log.info(String.format("Network layer structure: %s", NetworkUtils.generateLayerStructureString(structure)));

    return structure.stream().mapToInt(Integer::intValue).toArray();
  }
  /**
   * Validates the year. If not between the specified range, an exception is thrown,
   * terminating the run.
   *
   * Note: as you add years to the DB, you will need to modify this method accordingly.
   *
   * TODO: Add a TournamentParticipantDao.fetchAllYears() method to return this list
   * so it is dynamic.
   *
   * @param year
   *          The year to validate.
   *
   * @throws RuntimeException
   *           - if the year to validate is outside the supported range.
   */
  public static void validateYear(Integer year) {
    if (year < 2010 || year > 2019) {
      throw new RuntimeException("Invalid year: " + year + " (must be between 2010 and 2019, inclusive)");
    }
  }

}
