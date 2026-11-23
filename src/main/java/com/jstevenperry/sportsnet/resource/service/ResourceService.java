package com.jstevenperry.sportsnet.resource.service;

import com.jstevenperry.sportsnet.framework.dto.NetworkCandidateDto;
import com.jstevenperry.sportsnet.framework.dto.ResourceFileWrapperDto;
import com.jstevenperry.sportsnet.framework.exception.BadRequestException;
import com.jstevenperry.sportsnet.framework.exception.InternalServerErrorException;
import com.jstevenperry.sportsnet.framework.model.NetworkCandidate;
import com.jstevenperry.sportsnet.framework.model.NetworkDescriptor;
import com.jstevenperry.sportsnet.framework.model.ResourceType;
import com.jstevenperry.sportsnet.framework.util.AbstractNetworkUtils;
import com.jstevenperry.sportsnet.resource.config.SportsnetProperties;
import com.jstevenperry.sportsnet.resource.util.NetworkFileBasedPersister;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ResourceService {

    private static final Logger log = LoggerFactory.getLogger(ResourceService.class);

    public static final String LOG_PREFIX = "RSRCRS";
    private final NetworkFileBasedPersister networkFileBasedPersister;
    private final SportsnetProperties sportsnetProperties;

    public ResourceService(final NetworkFileBasedPersister networkFileBasedPersister,
                           final SportsnetProperties sportsnetProperties) {
        this.networkFileBasedPersister = networkFileBasedPersister;
        this.sportsnetProperties = sportsnetProperties;
    }

    /**
     * Locates, fetches, and returns the specified resource for the specified
     * sport and year.
     *
     * @param sport        The sport, such as nfl, or nhl
     * @param year         The year, such as 2019
     * @param resourceName The resource name, such as 2006-2007-tournament_result.csv
     * @return The resource's contents as a string
     * @throws BadRequestException - RS0011E if the specified resource type is not supported
     */
    public Optional<String[][]> fetchTextResource(final String sport,
                                                  final Integer year,
                                                  final String resourceName,
                                                  final ResourceType resourceType) {
        final Optional<String[][]> resourceContents;
        //
        // Fetch the resource based on the type(s) we support
        switch (resourceType) {
            case SEASON_DATA_FILE, TOURNAMENT_PARTICIPANT_FILE, CSV_METADATA_FILE ->
                    resourceContents = fetchCsvTextResource(sport, year, resourceName);
            case TOURNAMENT_GAME_LIST_FILE -> resourceContents = fetchPlainTextResource(sport, year, resourceName);
            default -> {
                String errorMessage = String.format("%s0013E: Unsupported resource type %s. Supported types: %s", LOG_PREFIX, resourceType,
                        StringUtils.join(ResourceType.values(), ','));
                log.error("fetchTextResource({},{},{},{}): {}", sport, year, resourceName, resourceType, errorMessage);
                throw new BadRequestException(errorMessage);
            }
        }
        //
        // Return the resource contents to the caller
        if (log.isTraceEnabled()) {
            String sample = resourceContents
                    .map(s -> (s[0][0] != null) ? s[0][0].substring(0, Math.min(256, s[0][0].length())) + "..." : "EMPTY FILE")
                    .orElse("NO DATA");
            log.trace("fetchTextResource({},{},{},{}): Returning resource contents (first {} chars): {}", sport, year, resourceName, resourceType, Math.min(256, sample.length()), sample);
        }
        return resourceContents;
    }

    /**
     * Fetch the specified sports-agnostic resource and return it in a ResourceFileWrapperDto, ready
     * to ship off to the caller.
     *
     * @param resourceFileName The name of the resource, such as network.properties
     * @param resourceType     The resource type, such as PROPERTIES_FILE
     * @return An optional containing the contents of the file in a ResourceFileWrapperDto if the resource
     * could be located, or an empty optional if not.
     */
    public Optional<ResourceFileWrapperDto> fetchTextResource(final String resourceFileName, final ResourceType resourceType) {
        Optional<ResourceFileWrapperDto> resourceContentsOptional;

        if (resourceType == ResourceType.PROPERTIES_FILE) {
            String resourceContents = fetchPropertiesResource(resourceFileName);
            ResourceFileWrapperDto resourceFileWrapperDto = new ResourceFileWrapperDto()
                    .setResourceType(resourceType)
                    .setFileContentsAsString(resourceContents);
            resourceContentsOptional = Optional.of(resourceFileWrapperDto);
        } else {
            String errorMessage = String.format("%s0015E: Unsupported resource type %s. Supported types: %s", LOG_PREFIX, resourceType,
                    StringUtils.join(ResourceType.values(), ','));
            log.error("fetchTextResource({},{}): {}", resourceFileName, resourceType, errorMessage);
            throw new BadRequestException(errorMessage);
        }

        return resourceContentsOptional;
    }

    /**
     * Fetch the specified network resource type by sport and ID.
     *
     * @param sport        The sport, such as nfl
     * @param networkId    The network UUID, such as 67fba115-cee1-48c0-a6e9-bbb2f9225263
     * @param resourceType The resource type such as NETWORK_DESCRIPTOR or DL4L_NETWORK
     * @return An Optional containind a partial decorated (resourceType==NETWORK_DESCRIPTOR)
     * or fully decorated (resourceType==DL4J_NETWORK) NetworkCandidate object if the network
     * could be located and loaded. If not, an exception will be thrown.
     * @throws BadRequestException - RS0013E if the specified resource type is not supported
     */
    public Optional<NetworkCandidateDto> fetchNetworkResource(final String sport,
                                                              final UUID networkId,
                                                              final ResourceType resourceType) {
        final Optional<NetworkCandidateDto> networkResource;
        switch (resourceType) {
            case NETWORK_DESCRIPTOR -> {
                Optional<NetworkCandidateDto> fullNetwork = fetchDl4jNetwork(sport, networkId);
                if (fullNetwork.isPresent()) {
                    //
                    // Create a new NetworkCandidate sans the actual network, since the caller doesn't want it
                    NetworkCandidateDto networkCandidateDto = fullNetwork.get();
                    NetworkCandidate networkCandidate =
                            new NetworkCandidate(NetworkDescriptor.fromDto(networkCandidateDto.getNetworkDescriptorDto()), null);
                    networkResource = Optional.of(networkCandidate.toDto());
                } else {
                    networkResource = Optional.empty();
                }
            }
            case DL4J_NETWORK -> networkResource = fetchDl4jNetwork(sport, networkId);
            default -> {
                String errorMessage = String.format("%s0017E: Unsupported resource type %s. Supported types: %s", LOG_PREFIX, resourceType,
                        StringUtils.join(ResourceType.values(), ','));
                log.error("fetchNetworkResource({},{},{}): {}", sport, networkId, resourceType, errorMessage);
                throw new BadRequestException(errorMessage);
            }
        }
        if (log.isTraceEnabled()) {
            log.trace("fetchNetworkResource({},{},{}): Returning networkResource: {}", sport, networkId, resourceType, networkResource);
        }
        return networkResource;
    }

    /**
     * Fetch all network resources by the sport and resource type.
     * If the resource type is NETWORK_DESCRIPTOR, the NetworkCandidates that are returned will only
     * have their TrainedNetworkParameter objects (that is, their network descriptors) populated.
     * If the resource type if DL4J_NETWORK (the only type of backend supported at the moment), then
     * the full network and its descriptor will be returned. This allows the caller to optimized the
     * call in cases where only the network descriptor is needed.
     *
     * @param sport        The sport, such as ncaabb
     * @param resourceType The resource type such as NETWORK_DESCRIPTOR or DL4J_NETWORK. If the resource
     *                     type is not supported by this method a BadRequestException will be thrown.
     * @return A List of the NetworkCandidate objects loaded from persistent storage for the consumer of
     * this service to do with as they please (we don't tell you what to do with your rats, our job is to
     * get you rats, stat!).
     */
    public List<NetworkCandidateDto> fetchAllNetworkResources(final String sport,
                                                              final ResourceType resourceType) {
        long startTime = System.currentTimeMillis();
        final List<NetworkCandidateDto> ret;
        String networkType = sport.toUpperCase(); // TODO: This is a band-aid until NetworkType and SportsSpecificUri can be combined

        switch (resourceType) {
            case NETWORK_DESCRIPTOR -> {
                //
                // Pull all network indexes, then stuff each inside a NetworkCandiateDto
                List<NetworkDescriptor> networkDescriptors = networkFileBasedPersister.loadIndexes(networkType);
                ret = networkDescriptors
                        .stream()
                        .map(networkDescriptor -> new NetworkCandidate(networkDescriptor, null))
                        .map(NetworkCandidate::toDto)
                        .collect(Collectors.toList());
            }
            case DL4J_NETWORK -> {
                //
                // Get the full networks
                List<NetworkCandidate> networkCandidates = networkFileBasedPersister.loadNetworks(networkType);
                ret = networkCandidates
                        .stream()
                        .map(NetworkCandidate::toDto)
                        .collect(Collectors.toList());
            }
            default -> {
                log.warn("{}2210W: No network resources found for sport '{}' and resourceType '{}'", LOG_PREFIX, sport, resourceType);
                ret = new ArrayList<>();
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("fetchAllNetworkResources({},{}): {}9901D: Fetched {} networks in {}ms.", LOG_PREFIX, sport, resourceType, ret.size(), System.currentTimeMillis() - startTime);
        }
        return ret;
    }

    /**
     * Persist the specified network candidate, and returns its network descriptor after
     * the network has been saved. If the network could not be saved, an exception is thrown.
     *
     * @param networkDescriptor         The network candidate's descriptor: the parameters of a previously
     *                                  trained but, as of yet, unsaved network.
     * @param networkCandidateByteArray The trained network in byte array form, ready to be reconstituted
     *                                  back into the type of network based on the implementation (such
     *                                  as DeepLearning4J).
     * @param resourceType              The resource type, such as DL4J_NETWORK.
     * @return The network descriptor if the network could be saved. If the network could not
     * be saved for some reason, an exception is thrown.
     * @throws InternalServerErrorException - RS0014E if the network could not be persisted
     * @throws BadRequestException          - RS0016E if the specified network resource type is not supported
     */
    public NetworkDescriptor persistNetworkResource(final NetworkDescriptor networkDescriptor,
                                                    final byte[] networkCandidateByteArray,
                                                    final ResourceType resourceType) {
        final NetworkDescriptor persistedNetworkDescriptor;
        //
        // Create the necessary accouterments for the type of network resource(s) we support
        if (resourceType == ResourceType.DL4J_NETWORK) {
            NetworkCandidate networkCandidate = AbstractNetworkUtils.reconstituteNetworkFromComponents(networkDescriptor, networkCandidateByteArray);
            if (networkFileBasedPersister.saveNetwork(networkCandidate)) {
                log.info("persistNetworkResource({}): {}5542I: Network saved: {}", resourceType, LOG_PREFIX, networkCandidate);
                persistedNetworkDescriptor = networkCandidate.getTrainedNetworkParameters();
            } else {
                String errorMessage = String.format("%s0019E: Network could not be saved: %s", LOG_PREFIX, networkCandidate);
                log.error("persistNetworkResource({}): {}", resourceType, errorMessage);
                throw new InternalServerErrorException(errorMessage);
            }
        } else {
            String errorMessage = String.format("%s0021E: Unsupported network type: %s, supported types are: %s", LOG_PREFIX, resourceType, ResourceType.DL4J_NETWORK);
            log.error("persistNetworkResource(): {}", errorMessage);
            throw new BadRequestException(errorMessage);
        }
        return persistedNetworkDescriptor;
    }

    /**
     * Admin function - reindex all network files. Need this right now to create indexes for
     * existing networks. In the future, will need this when network index changes, or for some
     * other reason the indexes need to be rebuilt.
     */
    public void reindexAllNetworks(final String sport) {
        long startTime = System.currentTimeMillis();
        String networkType = sport.toUpperCase(); // TODO: This is a band-aid until NetworkType and SportsSpecificUri can be combined
        //
        // Delete all current indexes, they will be replaced
        networkFileBasedPersister.deleteAllCurrentIndexes(networkType);
        //
        // Rebuild all indexes
        List<NetworkCandidate> networkCandidates = networkFileBasedPersister.loadNetworks(sport);
        networkCandidates.forEach(networkCandidate -> {
            log.info("reindexAllNetworks({}): {}5544I: Creating index file for network ID {}", sport, LOG_PREFIX, networkCandidate.getTrainedNetworkParameters().getNetworkId());
            networkCandidate.getTrainedNetworkParameters().setNetworkSaved(true);
            networkFileBasedPersister.createIndexFile(networkCandidate.getTrainedNetworkParameters());
        });
        log.info("reindexAllNetworks({}): {}5546I: Rebuilt all indexes in {}ms", sport, LOG_PREFIX, System.currentTimeMillis() - startTime);
    }

    /**
     * Fetches and returns the specified DL4J network resource by its ID.
     * <p>
     * Uses local storage to do this, and this is okay (for now) because this is the resource
     * service, and there is only one, and it can do stuff like that. So there.
     */
    private Optional<NetworkCandidateDto> fetchDl4jNetwork(final String sport, final UUID resourceId) {
        Optional<NetworkCandidateDto> networkResource;
        List<NetworkCandidate> networkCandidates = networkFileBasedPersister.loadNetworks(sport.toUpperCase(), List.of(resourceId));
        networkResource = networkCandidates
                .stream()
                .map(NetworkCandidate::toDto)
                .findAny();
        return networkResource;
    }

    private Optional<String[][]> fetchCsvTextResource(final String sport, final Integer year, final String resourceName) {
        return fetchTextResource(sport, year, resourceName, true);
    }

    private Optional<String[][]> fetchPlainTextResource(final String sport, final Integer year, final String resourceName) {
        return fetchTextResource(sport, year, resourceName, false);
    }

    /**
     * Fetches the specified text resource and returns its contents as a String
     */
    private Optional<String[][]> fetchTextResource(final String sport,
                                                   final Integer year,
                                                   final String resourceName,
                                                   final boolean isCsv) {
        final String[][] resourceContents;
        //
        // Use the parameters to compute the resource coordinates
        String resourceCoordinates = computeTextResourceCoordinates(sport, year, resourceName);
        //
        // Open the resource as an InputStream and read its contents
        if (isCsv) {
            resourceContents = readCsvTextResourceContents(resourceCoordinates);
        } else {
            resourceContents = readPlainTextResourceContents(resourceCoordinates);
        }
        if (log.isTraceEnabled()) {
            log.trace("fetchTextResource({},{},{}): Fetched resource as {} x {} array.",
                    sport, year, resourceName, resourceContents.length, resourceContents[0].length);
        }
        if (log.isDebugEnabled()) {
            log.debug("fetchTextResource({},{},{},{}): Fetched resource: {}", sport, year, resourceName, isCsv, resourceCoordinates);
        }
        return Optional.of(resourceContents);
    }

    /**
     * Compute and return the resource coordinates for a resource consisting of
     * the specified parameters.
     *
     * @param sport        The sport, such as nfl
     * @param year         The year, such as 2006
     * @param resourceName The resource name, such as 2006-Kick&PuntReturns.csv
     * @return The resource coordinates, such as /nfl/data/2006/2006-Kick&PuntReturns.csv
     */
    private static String computeTextResourceCoordinates(final String sport, final Integer year, final String resourceName) {
        return (year != null)
                ? String.format("/%s/data/%d/%s", sport.toUpperCase(), year, resourceName) // TODO: Use File.separatorChar
                : String.format("/%s/data/%s", sport.toUpperCase(), resourceName); // Some resources live in the root of their sport's resource directory (e.g., ncaabb/SeasonData-Alias.csv)
    }

    private String[][] readCsvTextResourceContents(final String resourceCoordinates) {
        return readTextResourceContents(resourceCoordinates, true);
    }

    private String[][] readPlainTextResourceContents(final String resourceCoordinates) {
        return readTextResourceContents(resourceCoordinates, false);
    }

    /**
     * Read the contents of the resource at the specified coordinates and return its
     * contents to the caller. If the resource cannot be found, an exception is thrown.
     *
     * @param resourceCoordinates The path (coordinates) to the resource.
     * @return The resource contents as a String
     * @throws InternalServerErrorException - RS0023E if error occurs while reading the resource
     * @throws InternalServerErrorException - RS0025E if the resource could not be located
     */
    private String[][] readTextResourceContents(final String resourceCoordinates, boolean isCsv) {
        final String[][] ret;
        final List<String[]> resourceContents;
        final Optional<InputStream> resourceInputStreamOptional = fetchResourceInputStream(resourceCoordinates);
        if (resourceInputStreamOptional.isPresent()) {
            final InputStream resourceInputStream = resourceInputStreamOptional.get();
            final String fileContentsAsString = readResourceFileContents(resourceCoordinates, resourceInputStream);
            if (isCsv) {
                //
                // Now process the lines into CSV
                resourceContents = processResourceFileContentsIntoCsvText(resourceCoordinates, fileContentsAsString);
            } else {
                resourceContents = processResourceFileContentsIntoPlainText(resourceCoordinates, fileContentsAsString);
            }
        } else {
            final String errorMessage = String.format("%s0023E: Could not find resource: %s", LOG_PREFIX, resourceCoordinates);
            log.error("readResourceContents({}): {}", resourceCoordinates, errorMessage);
            throw new InternalServerErrorException(errorMessage);
        }
        ret = resourceContents.toArray(new String[0][0]);
        if (ret[0] == null) {
            final String errorMessage = String.format("%s0025E: Resource: '%s' is empty!", LOG_PREFIX, resourceCoordinates);
            log.error("readResourceContents({}): {}", resourceCoordinates, errorMessage);
            throw new InternalServerErrorException(errorMessage);
        }
        return ret;
    }

    /**
     * Processes the file as a String, where each line is delimited with a newline, into a 2D string array,
     * where each line is the first dimension, and the contents of the line are in the zeroth element of
     * the second dimension.
     */
    private static List<String[]> processResourceFileContentsIntoPlainText(final String resourceCoordinates, final String fileContentsAsString) {
        List<String[]> resourceContents = new ArrayList<>();
        try (BufferedReader stringReader = new BufferedReader(new StringReader(fileContentsAsString))) {
            String line = stringReader.readLine();
            while (line != null) {
                //
                // Add the entire line as the first element in a 1D string array
                // This is to obey the interface of ultimately returning String[][] as the resource
                // file format (since most resources are CSV, except plain text ones, like the one
                // handled by this method).
                String[] lineAsStringArray = new String[1];
                lineAsStringArray[0] = line;
                resourceContents.add(lineAsStringArray);
                line = stringReader.readLine();
            }
        } catch (IOException e) {
            String errorMessage = String.format("%s0027E: Error reading string contents: %s", LOG_PREFIX, e.getLocalizedMessage());
            log.error("processResourceFileContentsIntoPlainText({}): {}", resourceCoordinates, errorMessage, e);
            throw new InternalServerErrorException(errorMessage, e);
        }
        return resourceContents;
    }

    private static List<String[]> processResourceFileContentsIntoCsvText(final String resourceCoordinates, final String fileContentsAsString) {
        List<String[]> resourceContents = new ArrayList<>();
        CSVReader csvReader = new CSVReader(new StringReader(fileContentsAsString));
        String[] line = null;
        try {
            line = csvReader.readNext();
            while (line != null) {
                resourceContents.add(line);
                line = csvReader.readNext();
            }
            csvReader.close();
        } catch (CsvValidationException | IOException e) {
            String errorMessage = String.format("%s0029E: Error while reading CSV line: '%s', error: %s",
                    LOG_PREFIX, StringUtils.join(line, ','), e.getLocalizedMessage());
            log.error("readResourceContents({}): {}", resourceCoordinates, errorMessage, e);
            throw new InternalServerErrorException(errorMessage);
        }
        return resourceContents;
    }

    private static String readResourceFileContents(final String resourceCoordinates, final InputStream resourceInputStream) {
        StringBuilder fileContents = new StringBuilder();
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resourceInputStream))) {
            int numberOfLinesRead = 0;
            String line = bufferedReader.readLine();
            while (line != null) {
                if (StringUtils.isNotEmpty(line)) {
                    fileContents.append(line);
                    fileContents.append('\n');
                    numberOfLinesRead++;
                }
                //
                // Read next line
                line = bufferedReader.readLine();
            }
            if (log.isTraceEnabled()) {
                log.trace("readResourceContents({}): Read {} lines from resource: {}", resourceCoordinates, numberOfLinesRead, resourceCoordinates);
            }
        } catch (IOException e) {
            String errorMessage = String.format("%s0031E: Error while reading resource: %s, error: %s", LOG_PREFIX, resourceCoordinates, e.getLocalizedMessage());
            log.error("readResourceContents({}): {}", resourceCoordinates, errorMessage, e);
            throw new InternalServerErrorException(errorMessage);
        }
        return fileContents.toString();
    }

    /**
     * Compute the coordinates of the specified properties resource in a standard way.
     *
     * @param resourceName The name of the resource, such as network.properties
     * @return The coordinates of the resource, which can be used to fetch that resource from
     * persistent storage
     */
    private String computePropertiesResourceCoordinates(final String resourceName) {
        return String.format("%s/%s", sportsnetProperties.getBaseDirectory(), resourceName);

    }

    /**
     * Return the contents of the specified resource as a String.
     *
     * @param resource The name of the resource, such as network.properties
     * @return The contents of the resource as a String
     */
    private String fetchPropertiesResource(final String resource) {
        final String propertiesFileContents;

        final String resourceCoordinates = computePropertiesResourceCoordinates(resource);

        InputStream resourceInputStream;
        try {
            resourceInputStream = new BufferedInputStream(new FileInputStream(resourceCoordinates));
            propertiesFileContents = readResourceFileContents(resourceCoordinates, resourceInputStream);
        } catch (FileNotFoundException e) {
            String errorMessage = String.format("%s0033E: Could not find resource: %s", LOG_PREFIX, resourceCoordinates);
            log.error("readResourceContents({}): {}", resourceCoordinates, errorMessage, e);
            throw new InternalServerErrorException(errorMessage);
        }
        return propertiesFileContents;
    }

    private Optional<InputStream> fetchResourceInputStream(final String resourceCoordinates) {
        final Optional<InputStream> inputStreamOptional;
        // Attempt to read the resource from the classpath
        try {
            final InputStream inputStream = ResourceService.class.getResourceAsStream(resourceCoordinates);
            if (inputStream != null) {
                log.debug("fetchResourceInputStream({}): Found resource on the CLASSPATH...", resourceCoordinates);
                inputStreamOptional = Optional.of(inputStream);
            } else {
                log.debug("fetchResourceInputStream({}): Resource not found on the CLASSPATH, reading from the file system...", resourceCoordinates);
                // If that fails, read from the file system
                final String resourcePath = String.format("%s%s", sportsnetProperties.getBaseDirectory(), resourceCoordinates);
                log.debug("fetchResourceInputStream: Reading resource from path: {}", resourcePath);
                final InputStream resourceInputStream = new BufferedInputStream(new FileInputStream(resourcePath));
                inputStreamOptional = Optional.of(resourceInputStream);
            }
        } catch (IOException e) {
            String errorMessage = String.format("%s0035E: Error accessing resource: %s", LOG_PREFIX, resourceCoordinates);
            log.error("readResourceContents({}): {}", resourceCoordinates, errorMessage, e);
            throw new InternalServerErrorException(errorMessage);
        }


        return inputStreamOptional;
    }

}
