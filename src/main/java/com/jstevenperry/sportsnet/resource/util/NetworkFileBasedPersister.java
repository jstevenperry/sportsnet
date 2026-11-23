/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.resource.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jstevenperry.sportsnet.framework.exception.InternalServerErrorException;
import com.jstevenperry.sportsnet.framework.model.NetworkCandidate;
import com.jstevenperry.sportsnet.framework.model.NetworkDescriptor;
import com.jstevenperry.sportsnet.resource.config.SportsnetProperties;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Persister used by the resource service to persist networks and other files
 * as necessary.
 */
@Service
public class NetworkFileBasedPersister {

    private final Logger log = LoggerFactory.getLogger(NetworkFileBasedPersister.class);

    public static final String LOG_PREFIX = "RSNFBP";

    public static final String NETWORK_PARAMETERS_KEY = "network.parameters";

    private static final String INDEX_FILE_EXTENSION = ".json";
    private static final String NETWORK_FILE_EXTENSION = ".zip";
    private static final String DL4J_NETWORK_EPILOGUE = "dl4j" + NETWORK_FILE_EXTENSION;

    private final SportsnetProperties sportsnetProperties;
    private final ObjectMapper objectMapper;

    private final Cache<String, String[]> networkDirectoryListCache;


    /**
     * The network index cache.
     */
    private final Cache<String, NetworkDescriptor> networkDescriptorCache;

    public NetworkFileBasedPersister(final SportsnetProperties sportsnetProperties,
                                     final ObjectMapper objectMapper) {
        this.sportsnetProperties = sportsnetProperties;
        this.objectMapper = objectMapper;
        //
        // Create the caches
        this.networkDescriptorCache = CacheBuilder
                .newBuilder()
                .expireAfterAccess(300, TimeUnit.MINUTES) // Expire 5 hours after last access
                .build();
        this.networkDirectoryListCache = CacheBuilder.newBuilder()
                .expireAfterWrite(120, TimeUnit.MINUTES) // Minimum of 2 hours
                .maximumSize(5000) // Maximum of 2000 items, then start kicking folks out...
                .build();
    }

    /**
     * Saves the specified network and returns true if successful, false otherwise.
     */
    public boolean saveNetwork(final NetworkCandidate networkCandidate) {
        boolean ret = false;
        //
        // Save network file
        Optional<NetworkDescriptor> networkDescriptorOptional = saveDl4jNetwork(networkCandidate);
        if (networkDescriptorOptional.isPresent()) {
            //
            // Save index file
            NetworkDescriptor networkDescriptor = networkDescriptorOptional.get();
            Optional<NetworkDescriptor> networkIndexOptional = createIndexFile(networkDescriptor);
            if (networkIndexOptional.isPresent()) {
                ret = true;
            }
        }
        return ret;
    }

    /**
     * Serialize the network descriptor as a JSON text file to the index directory.
     *
     * @param networkDescriptor The network descriptor to serialize as a JSON string to
     *                          a file in the network index directory
     * @return An Optional containing the saved network descriptor if successful or an
     * empty Optional if not.
     */
    public Optional<NetworkDescriptor> createIndexFile(final NetworkDescriptor networkDescriptor) {
        Optional<NetworkDescriptor> ret = Optional.empty();
        long startTime = System.currentTimeMillis();
        //
        //
        final String indexDirectoryName = fetchNetworkIndexDirectoryAndCreateIfNecessary(networkDescriptor.getNetworkType());
        try {
            //
            // Compute the file name
            final String networkDescriptorFileName = computeNetworkFileNameBase(networkDescriptor) + INDEX_FILE_EXTENSION;
            //
            // Convert the network descriptor to a JSON object
            final String networkDescriptorJson = objectMapper.writeValueAsString(networkDescriptor);
            //
            // Now save as a normal file
            final String fqFileName = indexDirectoryName + File.separatorChar + networkDescriptorFileName;
            final File networkDescriptorFile = new File(fqFileName);
            try (BufferedWriter fileWriter = new BufferedWriter(new FileWriter(networkDescriptorFile))) {
                //
                // Write the string to the file
                fileWriter.write(networkDescriptorJson);
                ret = Optional.of(networkDescriptor);
                //
                // Store the index in the cache
                networkDescriptorCache.put(fqFileName, networkDescriptor);
            }
            log.info("{}5502I: Created new index file '{}' in {}ms", LOG_PREFIX, networkDescriptorFileName, System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            final String errorMessage = String.format("%s0065E: Exception while converting network descriptor to JSON string: %s", LOG_PREFIX, e.getLocalizedMessage());
            log.error(errorMessage, e);
        }
        return ret;
    }

    /**
     * Save the DL4J network contained in the specified NetworkCandidate object to the network directory
     * and return the updated network descriptor in an Optional if successful. If something goes wrong,
     * the Optional will be empty.
     *
     * @param networkCandidate The NetworkCandidate to be saved
     * @return An Optional containing the network descriptor of the DL4J network that was saved if
     * successful, or an empty Optional if not.z
     */
    private Optional<NetworkDescriptor> saveDl4jNetwork(final NetworkCandidate networkCandidate) {
        Optional<NetworkDescriptor> ret;
        final NetworkDescriptor networkDescriptor = networkCandidate.getTrainedNetworkParameters();
        final String networkFileName = fetchNetworkDirectoryAndCreateIfNecessary(networkCandidate
                .getTrainedNetworkParameters()
                .getNetworkType()) + File.separatorChar + computeNetworkFileNameBase(networkCandidate.getTrainedNetworkParameters()) + NETWORK_FILE_EXTENSION;
        log.info("{}5504I: Saving network: {} as {}", LOG_PREFIX, networkDescriptor.getNetworkLayout(), networkFileName);
        try {
            final File modelFile = new File(networkFileName);
            ModelSerializer.writeModel(networkCandidate.getMultiLayerNetwork(), modelFile, true);
            networkDescriptor.setNetworkSaved(true);
            ModelSerializer.addObjectToFile(modelFile, NETWORK_PARAMETERS_KEY, networkDescriptor);
            ret = Optional.of(networkDescriptor);
        } catch (IOException e) {
            final String message = String.format("%s0067E: Error saving network file '%s': %s", LOG_PREFIX, networkFileName, e.getLocalizedMessage());
            log.error(message, e);
            ret = Optional.empty();
        }
        return ret;
    }

    public String computeNetworkFileNameBase(final NetworkDescriptor networkDescriptor) {
        return String.format("%s-%s-Net-%s-%2.2fpct.dl4j", networkDescriptor.getNetworkId(), networkDescriptor.getNetworkType(), networkDescriptor
                .getWhenTrained()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")), networkDescriptor.getRealNetworkAccuracy() * 100);
    }

    /**
     * Deletes all of the current indexes for the specified networkType (a.k.a. sport). This
     * method is usually called when rebuilding the indexes.
     *
     * @param networkType The network type (sport) whose indexes are the be deleted.
     */
    public void deleteAllCurrentIndexes(final String networkType) {
        long startTime = System.currentTimeMillis();
        int numberOfIndexesDeleted = 0;
        //
        // Get a list of all indexes and then nuke them
        final String indexDirectoryName = fetchNetworkIndexDirectoryAndCreateIfNecessary(networkType);
        final File indexDirectory = new File(indexDirectoryName);
        //
        // Get a listing
        final String[] indexFileNames = indexDirectory.list((dir, name) -> name.endsWith(INDEX_FILE_EXTENSION));
        if (indexFileNames != null) {
            for (String indexFileName : indexFileNames) {
                final String fqFileName = indexDirectoryName + File.separatorChar + indexFileName;
                File indexFile = new File(fqFileName);
                if (indexFile.delete()) {
                    numberOfIndexesDeleted++;
                } else {
                    log.warn("deleteAllCurrentIndexes({}): {}2202W: Could not delete index file '{}'", networkType, LOG_PREFIX, indexFileName);
                }
            }
        }
        log.info("deleteAllCurrentIndexes(): {}5516I: Deleted {} indexes in {}ms", LOG_PREFIX, numberOfIndexesDeleted, System.currentTimeMillis() - startTime);
    }

    /**
     * Loads all index files for the specified networkType (sport) and returns their NetworkDescriptor
     * objects.
     */
    public List<NetworkDescriptor> loadIndexes(final String networkType) {
        long startTime = System.currentTimeMillis();
        final List<NetworkDescriptor> ret;
        final String indexDirectoryName = fetchNetworkIndexDirectoryAndCreateIfNecessary(networkType);
        final File indexDirectory = new File(indexDirectoryName);
        //
        // Get a listing
        final String[] indexFileNames = indexDirectory.list((dir, name) -> name.endsWith(INDEX_FILE_EXTENSION));
        if (indexFileNames != null && indexFileNames.length > 0) {
            List<String> filteredNetworkFileNames = computeListOfFilesToLoad(new ArrayList<>(), indexDirectoryName, indexFileNames);
            ret = loadIndexFiles(filteredNetworkFileNames);
        } else {
            log.warn(String.format("%s2204W: No indexes found at %s", LOG_PREFIX, indexDirectoryName));
            ret = new ArrayList<>();
        }
        log.info("{}5518I: loadNetworks({}): Loaded {} indexes in {}ms", LOG_PREFIX, networkType, ret.size(), System.currentTimeMillis() - startTime);
        return ret;
    }

    /**
     * Loads all networks for the specified networkType (sport). This method is used to load all of
     * the actual DL4J network files in order to run a simulation. To just get information about all
     * of the networks, call loadIndexes() instead.
     */
    public List<NetworkCandidate> loadNetworks(final String networkType) {
        return loadNetworks(networkType, new ArrayList<>());
    }

    public List<NetworkCandidate> loadNetworks(final String networkType,
                                               final List<UUID> networkIds) {
        long startTime = System.currentTimeMillis();
        final List<NetworkCandidate> ret = new ArrayList<>();
        final String networkDirectoryName = fetchNetworkDirectoryAndCreateIfNecessary(networkType);
        final File networkDirectory = new File(networkDirectoryName);
        try {
            //
            // Get a listing - cache this, key = networkDirectory, value = String[]
            final String[] dl4jNetworkFileNames = networkDirectoryListCache.get(networkDirectoryName, () -> networkDirectory.list((dir, name) -> name.endsWith(DL4J_NETWORK_EPILOGUE)));
            if (dl4jNetworkFileNames.length > 0) {
                List<String> filteredNetworkFileNames = computeListOfFilesToLoad(networkIds, networkDirectoryName, dl4jNetworkFileNames);
                ret.addAll(loadNetworkFiles(filteredNetworkFileNames));
            } else {
                log.warn(String.format("%s2206W: No networks found at %s", LOG_PREFIX, networkDirectoryName));
            }
        } catch (ExecutionException e) {
            String errorMessage = String.format("%s0041E: Error during cache operation: %s", LOG_PREFIX, e.getLocalizedMessage());
            log.error("loadIndexFiles(): {}", errorMessage, e);
        }
        log.info("{}5520I: loadNetworks({}): Loaded {} networks in {}ms", LOG_PREFIX, networkType, ret.size(), System.currentTimeMillis() - startTime);
        return ret;
    }

    /**
     * Compute the list of files to load from the specified directory and filter the list of files
     * to only those in the specified networkIds list.
     *
     * @param networkIds    The list of network ids that controls which files get loaded. If the list is empty
     *                      then all files will be loaded.
     * @param directoryName The directory where the files are located
     * @param fileNames     The list of files in the directory from which zero or more will be loaded
     *                      depending on whether they match the list of networkIds to load, unless the
     *                      list is empty, in which case all files will be loaded.
     * @return List of fully-qualified (full path) file names to be loaded.
     */
    private List<String> computeListOfFilesToLoad(final List<UUID> networkIds,
                                                  final String directoryName,
                                                  final String[] fileNames) {
        final List<String> filteredList;
        if (networkIds.isEmpty()) {
            filteredList = Arrays.stream(fileNames).toList();
        } else {
            filteredList = Arrays.stream(fileNames)
                    .filter(fileName -> fileNameContainsNetworkId(fileName, networkIds))
                    .toList();
        }
        final List<String> fqFileNames = new ArrayList<>();
        log.info("{}5522I: Found {} networks at {}", LOG_PREFIX, filteredList.size(), directoryName);
        //
        // Need the full path to the network file
        for (String fileName : filteredList) {
            fqFileNames.add(directoryName + File.separator + fileName);
        }
        return fqFileNames;
    }

    /**
     * Returns true if the specified file name contains one of the networkIds
     * in the specified list.
     *
     * @param fileName   The file name to test whether or not it contains at least one
     *                   of the network ids
     * @param networkIds The list of network Ids
     * @return True if the file name contains at least one of the specified network IDs,
     * false otherwise.
     */
    private boolean fileNameContainsNetworkId(final String fileName,
                                              final List<UUID> networkIds) {
        return networkIds.stream()
                .anyMatch(networkId -> fileName.contains(networkId.toString()));
    }

    /**
     * Loads the list of network indexes from persistent storage and returns their NetworkDescriptor
     * representations.
     *
     * @param indexFileNames The list of index files to load
     * @return A list of NetworkDescriptor objects, one for each index file loaded
     */
    private List<NetworkDescriptor> loadIndexFiles(final List<String> indexFileNames) {
        List<NetworkDescriptor> ret = new ArrayList<>();
        for (String indexFileName : indexFileNames) {
            try {
                //
                // Get the cached NetworkCandidate value or compute it using the specified lambda
                NetworkDescriptor networkDescriptor = networkDescriptorCache.get(indexFileName, () -> loadIndex(indexFileName));
                //
                // Add the NetworkCandidate to the list of such to be returned
                ret.add(networkDescriptor);
            } catch (ExecutionException e) {
                String errorMessage = String.format("%s0041E: Error during cache operation: %s", LOG_PREFIX, e.getLocalizedMessage());
                log.error("loadIndexFiles(): {}", errorMessage, e);
            }
        }
        log.info("{}2210I: Loaded {} indexes.", LOG_PREFIX, ret.size());
        return ret;
    }

    /**
     * Loads the list of network files from persistent storage and returns their NetworkCandidate
     * representations.
     *
     * @param networkFileNames The list of network files to load
     * @return A list of NetworkCandidate objects, one for each network file loaded
     */
    private List<NetworkCandidate> loadNetworkFiles(final List<String> networkFileNames) {
        List<NetworkCandidate> ret = new ArrayList<>();
        //
        // This looks cool on the console, leave it as System.out.println()...
        System.out.printf("Loading %d networks...%n", networkFileNames.size());
        int numberOfNetworksLoaded = 0;
        for (String networkFileName : networkFileNames) {
            //
            // Get the cached NetworkCandidate value or compute it using the specified lambda
            NetworkCandidate networkCandidate = loadNetwork(networkFileName);
            //
            // Add the NetworkCandidate to the list of such to be returned
            ret.add(networkCandidate);
            if (numberOfNetworksLoaded > 0 && numberOfNetworksLoaded % 100 == 0) {
                System.out.printf("%d\n", numberOfNetworksLoaded);
            } else if (numberOfNetworksLoaded > 0 && numberOfNetworksLoaded % 10 == 0) {
                System.out.printf("%d", numberOfNetworksLoaded);
            }
            System.out.print(".");
            numberOfNetworksLoaded++;
        }
        System.out.print("\n");
        log.info("{}5522I: Loaded {} networks.", LOG_PREFIX, networkFileNames.size());
        return ret;
    }

    /**
     * Load a single index file identified by the specified file name, and
     * return a NetworkDescriptor object representing it.
     *
     * @param indexFileName The FQ name of the index file to load
     * @return A NetworkDescriptor of the index file that was loaded
     */
    private NetworkDescriptor loadIndex(final String indexFileName) {
        final NetworkDescriptor ret;
        long startTime = System.currentTimeMillis();
        //
        // Load the index
        File indexFile = new File(indexFileName);
        try {
            ret = objectMapper.readValue(indexFile, NetworkDescriptor.class);
            //
            // Everything looks good return the newly computed value, which will be cached
            log.info("loadIndex({}): {}5524I: Index loaded in {}ms", indexFileName, LOG_PREFIX, System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            String message = String.format("%s0043E: Error loading index: %s", LOG_PREFIX, indexFileName);
            log.error(message, e);
            throw new InternalServerErrorException(message, e);
        }
        return ret;
    }

    /**
     * Load a single network identified by the specified network file name, and
     * return a NetworkCandidate object representing it.
     */
    private NetworkCandidate loadNetwork(final String networkFileName) {
        final NetworkCandidate ret;
        long startTime = System.currentTimeMillis();
        if (log.isDebugEnabled()) {
            log.debug("loadNetwork({}): {}9906D: Loading network...", networkFileName, LOG_PREFIX);
        }
        //
        // Load the network
        File networkFile = new File(networkFileName);
        try {
            MultiLayerNetwork network = ModelSerializer.restoreMultiLayerNetwork(networkFile, true);
            NetworkDescriptor networkDescriptor = ModelSerializer.getObjectFromFile(networkFile, NETWORK_PARAMETERS_KEY);
            //
            // Everything looks good return the newly computed value, which will be cached
            if (log.isDebugEnabled()) {
                log.debug("loadNetwork({}): {}9907D: Network loaded in {}ms", networkFileName, LOG_PREFIX, System.currentTimeMillis() - startTime);
            }
            ret = new NetworkCandidate(networkDescriptor, network);
        } catch (Exception e) {
            String message = String.format("%s0045E: Error loading network: %s", LOG_PREFIX, networkFileName);
            log.error(message, e);
            throw new InternalServerErrorException(message, e);
        }
        return ret;
    }

    /**
     * Fetches the name of the network directory using SportsnetProperties settings
     * and returns that name to the caller.
     */
    public String fetchNetworkDirectoryAndCreateIfNecessary(final String networkType) {
        return fetchDirectoryAndCreateIfNecessary(networkType, sportsnetProperties.getNetworkDirectoryName());
    }

    /**
     * Fetches the name of the network index directory using SportsnetProperties settings
     * and returns that name to the caller.
     */
    public String fetchNetworkIndexDirectoryAndCreateIfNecessary(final String networkType) {
        return fetchDirectoryAndCreateIfNecessary(networkType, sportsnetProperties.getNetworkIndexDirectoryName());
    }

    /**
     * Fetches a directory relative to the base directory for the specified network type
     * and ending with the specified epilogue and returns that name to the caller. The directory
     * will be created if it does not already exist.
     */
    private String fetchDirectoryAndCreateIfNecessary(final String networkType,
                                                      final String directoryNameEpilogue) {
        //
        // Compute the directory name from NetworkProperties settings
        String directoryName = sportsnetProperties.getBaseDirectory() + File.separator + networkType;
        //
        // If necessary, create the directory
        createDirectoryIfNecessary(directoryName);
        directoryName += File.separator + directoryNameEpilogue;
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
    public void createDirectoryIfNecessary(String directoryName) {
        try {
            File directory = new File(directoryName);
            if (!directory.exists()) {
                log.warn("createDirectoryIfNecessary({}): {}2208W: Directory '{}' does not exist. Creating it now...", directoryName, LOG_PREFIX, directoryName);
                // Create the directory.
                Path networkDirectoryPath = Files.createDirectory(Paths.get(directoryName));
                log.warn("createDirectoryIfNecessary({}): {}2210W: Network directory '{}' created.", directoryName, LOG_PREFIX, networkDirectoryPath);
            }
        } catch (IOException e) {
            String message = String.format("%s0047E: I/O Exception occurred while creating directory '%s'. Cannot continue!", LOG_PREFIX, directoryName);
            log.error("createDirectoryIfNecessary({}): {}", directoryName, message, e);
            throw new RuntimeException(message, e);
        }
    }

}
