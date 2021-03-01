/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.util;

import com.jstevenperry.sportsnet.framework.model.NetworkCandidate;
import com.jstevenperry.sportsnet.framework.model.NetworkParameters;
import com.jstevenperry.sportsnet.framework.persistence.TournamentParticipantDao;
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
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

public class NetworkUtils {

    private static final Logger log = LoggerFactory.getLogger(NetworkUtils.class);

    public static final String DL4J_NETWORK_EPILOGUE = "dl4j.zip";

    public static List<NetworkCandidate> loadNetworks(final String networkType) {
        List<NetworkCandidate> ret = new ArrayList<>();
        String networkDirectoryName = NetworkUtils.fetchNetworkDirectoryAndCreateIfNecessary(networkType);
        File networkDirectory = new File(networkDirectoryName);
        //
        // Get a listing of *.dl4j.zip
        String[] dl4jNetworkFileNames = networkDirectory.list((dir, name) -> name.endsWith(DL4J_NETWORK_EPILOGUE));
        if (dl4jNetworkFileNames != null && dl4jNetworkFileNames.length > 0) {
            String[] networkFileNames = new String[dl4jNetworkFileNames.length];
            log.debug(String.format("Found %d networks at %s", dl4jNetworkFileNames.length, networkDirectoryName));
            int index = 0;
            //
            // Need the full path to the network file
            for (String dl4jNetworkFileName : dl4jNetworkFileNames) {
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
        System.out.printf("Loading %d networks...%n", networkFileNames.length);
        int numberOfNetworksLoaded = 0;
        for (String networkFileName : networkFileNames) {
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
            if (numberOfNetworksLoaded > 0 && numberOfNetworksLoaded % 100 == 0) {
                System.out.printf("%d\n", numberOfNetworksLoaded);
            }
            System.out.print(".");
            numberOfNetworksLoaded++;
        }
        System.out.print("\n");
        log.info("Loaded {} networks.", networkFileNames.length);
        return ret;
    }

    public static void displayNetworkList(final List<NetworkCandidate> networkCandidates) {
        System.out.printf("%9s%21s%11s(%%)%35s%10s%45s%30s%15s%n",
                "Network #", "When Trained", "Accuracy", "Layer Structure", "Saved?", "Years (Training)", "Updater", "No. Epochs");
        int index = 0;
        log.debug("Sorting networks...");
        //
        // Sort the list, then present it
        Collections.sort(networkCandidates);
        log.debug("Rendering networks suitable for display...");
        for (NetworkCandidate networkCandidate : networkCandidates) {
            NetworkParameters networkParameters = networkCandidate.getNetworkParameters();
            System.out.printf("%9d%21s%13.2f%%%35s%10s%45s%30s%15d%n",
                    index + 1,
                    networkParameters.getWhenTrained().format(DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm")),
                    networkParameters.getNetworkAccuracy() * 100.0,
                    StringUtils.abbreviate(networkParameters.getNetworkLayout(), 32),
                    networkParameters.isNetworkSaved() ? "Y" : "N",
                    networkParameters.getTrainingYearsString(),
                    StringUtils.abbreviate(networkParameters.getUpdater().toString(), 24),
                    networkParameters.getNumberOfEpochs());
            index++;
        }
    }

    public static void displayNetworks(final String networkType) throws IOException {
        BufferedReader scanner = new BufferedReader(new InputStreamReader(System.in));
        log.info("Loading networks for display (this could take a minute, please be patient)...");
        List<NetworkCandidate> networkCandidates = loadNetworks(networkType);
        if (networkCandidates.size() > 0) {
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
        } else {
            log.warn("No saved networks exist. Train some networks, save one or more of them, and try again.");
        }
        System.out.println("Quitting.");
    }

    /**
     * Fetches the name of the network directory using NetworkProperties settings
     * and returns that name to the caller.
     */
    public static String fetchNetworkDirectoryAndCreateIfNecessary(final String networkType) {
        //
        // Compute the directory name from NetworkProperties settings
        String directoryName = NetworkProperties.getBaseDirectory() + File.separator
                + networkType;
        //
        // If necessary, create the directory
        createDirectoryIfNecessary(directoryName);
        directoryName += File.separator + NetworkProperties.getNetworkDirectoryName();
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
     */
    public static String fetchSimulationDirectoryAndCreateIfNecessary(String networkType) {
        //
        // Compute the directory name from NetworkProperties settings
        String directoryName = NetworkProperties.getBaseDirectory() + File.separator + networkType;
        //
        // If necessary, create the directory
        createDirectoryIfNecessary(directoryName);
        directoryName += File.separator + NetworkProperties.getSimulationDirectoryName();
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
     */
    public static void createDirectoryIfNecessary(String directoryName) {
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
     */
    public static String generateLayerStructureString(List<Integer> neuronLayerDescriptor) {
        StringBuilder sb = new StringBuilder();
        int aa = 0;
        for (Integer neuronsCount : neuronLayerDescriptor) {
            if (aa++ > 0) {
                sb.append("x");
            }
            sb.append(neuronsCount);
        }
        return sb.toString();
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
        log.debug(String.format("Network layer structure: %s", NetworkUtils.generateLayerStructureString(structure)));

        return structure.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Validates the year. If not between the specified range, an exception is thrown,
     * terminating the run.
     * <p>
     * Note: as you add years to the DB, you will need to modify this method accordingly.
     *
     * @param year The year to validate.
     * @throws RuntimeException - if the year to validate is outside the supported range.
     */
    public static void validateYear(final Integer year, final TournamentParticipantDao tournamentParticipantDao) {
        List<Integer> validTournamentYears = tournamentParticipantDao.fetchValidTournamentYears();
        if (!validTournamentYears.contains(year)) {
            throw new RuntimeException(String.format("Invalid year: " + year + " (must be one of %s)", StringUtils.join(validTournamentYears.toArray(), ',')));
        }
    }

    public static String nice(final String string) {
        String ret = "0";
        if (StringUtils.isNotEmpty(string)) {
            ret = StringUtils.remove(StringUtils.trim(string), '%');
        }
        return ret;
    }

    public static String generateRandomNetworkLayout(final Integer numberOfInputs, final Integer numberOfOutputs) {
        int numberOfHiddenLayers = NetworkProperties.getNumberOfHiddenLayers();
        List<Integer> neuronLayerDescriptor = new ArrayList<>();
        for (int aa = 0; aa < numberOfHiddenLayers; aa++) {
            neuronLayerDescriptor.add(0);
        }

        if (numberOfHiddenLayers < 5) {
            String errorMessage = String.format("Algorithm does not work well with < 5 hidden layers, you specified %s", numberOfHiddenLayers);
            log.error("generateSampleNetworkLayout({}, {}): {}", numberOfInputs, numberOfOutputs, numberOfHiddenLayers);
            throw new RuntimeException(errorMessage);
        }
        // First hidden layer
        int numberOfNeuronsInFirstLayer = Math.min(numberOfInputs * numberOfHiddenLayers, numberOfInputs * numberOfOutputs * numberOfOutputs);
        neuronLayerDescriptor.set(0, numberOfNeuronsInFirstLayer);
        // Layers between first and middle
        int middleLayerIndex = numberOfHiddenLayers / 2;
        int numberOfNeuronsInMiddleLayer = numberOfInputs * numberOfOutputs;
        for (int index = middleLayerIndex; index > 0; index--) {
            int min = Math.min(numberOfNeuronsInFirstLayer / index, numberOfNeuronsInMiddleLayer);
            int max = Math.max(numberOfNeuronsInFirstLayer / index, numberOfNeuronsInMiddleLayer);
            neuronLayerDescriptor.set(index, getRandomNumberUsingNextInt(min, max));
        }
        // Middle hidden layer
        int numberOfNeuronsInLastLayer = numberOfOutputs * numberOfOutputs * numberOfOutputs;
        neuronLayerDescriptor.set(middleLayerIndex, numberOfNeuronsInMiddleLayer);
        int lastLayerIndex = numberOfHiddenLayers - 1;
        for (int index = lastLayerIndex - 1; index > middleLayerIndex; index--) {
            int min = Math.min(numberOfNeuronsInLastLayer / index, numberOfNeuronsInLastLayer);
            int max = Math.max(numberOfNeuronsInLastLayer / index, numberOfNeuronsInLastLayer);
            neuronLayerDescriptor.set(index, getRandomNumberUsingNextInt(min, max));
        }
        // Last hidden layer (closest to output)
        neuronLayerDescriptor.set(lastLayerIndex, numberOfNeuronsInLastLayer);
        return generateLayerStructureString(neuronLayerDescriptor);
    }

    /**
     * Source: https://www.baeldung.com/java-generating-random-numbers-in-range
     *
     * @param min The lowest possible number that is acceptable
     * @param max The highest possible number that is acceptable
     * @return A number between min and max
     */
    public static int getRandomNumberUsingNextInt(int min, int max) {
        Random random = new Random();
        int bound = Math.max(max - min, 1);
        return random.nextInt(bound) + min;
    }
}
