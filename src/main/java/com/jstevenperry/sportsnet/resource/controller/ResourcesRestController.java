/*
 * Copyright © 2021 J. Steven Perry
 *  All rights reserved.
 */

package com.jstevenperry.sportsnet.resource.controller;

import com.jstevenperry.sportsnet.framework.dto.NetworkCandidateDto;
import com.jstevenperry.sportsnet.framework.dto.NetworkDescriptorDto;
import com.jstevenperry.sportsnet.framework.dto.ResourceFileWrapperDto;
import com.jstevenperry.sportsnet.framework.model.NetworkDescriptor;
import com.jstevenperry.sportsnet.framework.model.ResourceType;
import com.jstevenperry.sportsnet.resource.service.ResourceService;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/resources")
public class ResourcesRestController {

    public static final String LOG_PREFIX = "RSRRC";

    private static final Logger log = LoggerFactory.getLogger(ResourcesRestController.class);
    private final ResourceService resourceService;

    public ResourcesRestController(final ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    /**
     * Fetch a sports-agnostic resource file, such as a properties file.
     *
     * @param resourceFileName The name of the resource file, such as network.properties
     * @param resourceType     The resource type, such as PROPERTIES_FILE
     * @return HttpStatus of 200, and an Optional containing the contents of the file as a
     * type that makes sense for a resource of that type. For example, for a PROPERTIES_FILE
     * the contents will be a String where each line is delimited by a newline. For a CSV file,
     * on the other hand, a String[][] makes more sense.
     */
    @GetMapping("/{resourceFileName}")
    public ResponseEntity<Optional<ResourceFileWrapperDto>> fetchResourceFile(@PathVariable(name = "resourceFileName") String resourceFileName,
                                                                              @RequestParam(value = "resourceType") final ResourceType resourceType) {
        final ResponseEntity<Optional<ResourceFileWrapperDto>> responseEntity;
        long startTime = System.currentTimeMillis();

        Optional<ResourceFileWrapperDto> resourceFileWrapperDto = resourceService.fetchTextResource(resourceFileName, resourceType);
        responseEntity = ResponseEntity.ok(resourceFileWrapperDto);

        log.info("fetchResourceFile({},{}): {}5526I: Returning resource file in {}ms", resourceFileName, resourceType, LOG_PREFIX, System.currentTimeMillis() - startTime);
        return responseEntity;
    }

    /**
     * Fetch a single resource file by name and type for the specified sport and year.
     *
     * @param sport        The sport, such as nfl
     * @param resourceName The name of the resource, such as 2019-Conversions.csv
     * @param year         The year, such as 2019, might be empty if the resource doesn't need to be stored
     *                     out by year, for example.
     * @param resourceType The resource type such as SEASON_DATA_FILE
     * @return HttpStatus of 200, and An Optional containing the contents of the file,
     * or if the file could not be located the Optional will be empty. If an exception occurred
     * on the server a different HttpStatus (such as 500) will be returned, along with a message
     * indicating what went wrong.
     */
    @GetMapping(value = "/{sport}/{resourceName}")
    public ResponseEntity<ResourceFileWrapperDto> fetchResourceFile(@PathVariable(name = "sport") final String sport,
                                                                    @PathVariable(name = "resourceName") final String resourceName,
                                                                    @RequestParam(value = "resourceType") final ResourceType resourceType,
                                                                    @RequestParam(value = "year", required = false) final Integer year) {
        final ResponseEntity<ResourceFileWrapperDto> responseEntity;
        long startTime = System.currentTimeMillis();
        log.debug("fetchResourceFile({}, {}, {}): {}9902D: Processing request...", sport, year, resourceName, LOG_PREFIX);

        Optional<String[][]> resourceContentsOptional = resourceService.fetchTextResource(sport, year, resourceName, resourceType);

        if (resourceContentsOptional.isPresent()) {
            responseEntity = ResponseEntity.ok(new ResourceFileWrapperDto()
                    .setResourceType(resourceType)
                    .setFileContentsAsDoubleStringArray(resourceContentsOptional.get()));
        } else {
            String[][] noData = {{"NO DATA"}};
            responseEntity = ResponseEntity.ok(new ResourceFileWrapperDto()
                    .setResourceType(resourceType)
                    .setFileContentsAsDoubleStringArray(noData));
        }
        log.info("fetchResourceFile({}, {}, {}): {}5528I: Returning response in {}ms: {}", sport, year, resourceName, LOG_PREFIX, System.currentTimeMillis() - startTime, responseEntity);
        if (log.isTraceEnabled()) {
            log.trace("fetchResourceFile({}, {}, {}): Returning response: {}", sport, year, resourceName, responseEntity);
        }
        return responseEntity;
    }

    /**
     * Fetch and return the network by its ID and sport. While sport is not
     * necessary to locate a resource by its UUID, having the caller provide the sport allows some flexibility
     * for the resource service to optimize storage of network files (which can become quite large) by sport,
     * at its discretion (in case you were wondering).
     *
     * @param sport        The sport, such as nba
     * @param networkId    The network's ID, such as 456e9877-ec05-47b4-be31-c640b6a4ddc9
     * @param resourceType The network resource type, such as NETWORK_DESCRIPTOR or DL4J_NETWORK.
     * @return HttpStatus of 200, and An Optional containing the network DTO,
     * or if the network could not be located the Optional will be empty. If an exception occurred
     * on the server a different HttpStatus (such as 500) will be returned, along with a message
     * indicating what went wrong.
     */
    @GetMapping(value = "/{sport}/networks/{networkId}")
    public ResponseEntity<Optional<NetworkCandidateDto>> fetchNetworkById(@PathVariable(name = "sport") final String sport,
                                                                          @PathVariable(name = "networkId") final UUID networkId,
                                                                          @RequestParam(value = "resourceType") final ResourceType resourceType) {
        final ResponseEntity<Optional<NetworkCandidateDto>> responseEntity;
        long startTime = System.currentTimeMillis();
        log.debug("fetchNetworkById({}, {}, {}): {}9903D: Processing request...", sport, networkId, resourceType, LOG_PREFIX);

        Optional<NetworkCandidateDto> resourceOptional = resourceService.fetchNetworkResource(sport, networkId, resourceType);
        if (resourceOptional.isPresent()) {
            NetworkCandidateDto networkCandidateDto = resourceOptional.get();
            responseEntity = ResponseEntity.ok(Optional.of(networkCandidateDto));
        } else {
            responseEntity = ResponseEntity.ok(Optional.empty());
        }

        log.info("fetchNetworkById({}, {}, {}): {}5530I: Returning response in {}ms", sport, networkId, resourceType, LOG_PREFIX, System.currentTimeMillis() - startTime);
        if (log.isTraceEnabled()) {
            log.trace("fetchNetworkById({}, {}, {}): Returning response: {}", sport, networkId, resourceType, responseEntity);
        }
        return responseEntity;
    }

    /**
     * Fetch and return all networks for the specified sport. Depending on the resource type, the List of
     * NetworkCandidateDto will contain more or less information.
     *
     * @param sport        The sport, such as nhl
     * @param resourceType The resource type, such as DL4J_NETWORK if the actual network is needed, or
     *                     NETWORK_DESCRIPTOR if only the network descriptor is needed (in which case the
     *                     NetworkCandidateDto will only have its NetworkDescriptorDto object populated).
     * @return An HttpStatus of 200, along with a List of NetworkCandidateDto populated with information
     * based on the ResourceType. If no networks could be found for the specified sport, the list will be
     * empty. If an exception occurred on the server a different HttpStatus (such as 500) will be returned,
     * along with a message indicating what went wrong.
     */
    @GetMapping("/{sport}/networks")
    public ResponseEntity<List<NetworkCandidateDto>> fetchAllNetworks(@PathVariable(name = "sport") final String sport,
                                                                      @RequestParam(value = "resourceType") final ResourceType resourceType) {
        final ResponseEntity<List<NetworkCandidateDto>> responseEntity;
        log.debug("fetchAllNetworks({}, {}): {}9904D: Processing request...", LOG_PREFIX, sport, resourceType);
        long startTime = System.currentTimeMillis();

        List<NetworkCandidateDto> networkDtos = resourceService.fetchAllNetworkResources(sport, resourceType);

        responseEntity = ResponseEntity.ok(networkDtos);
        log.info("fetchAllNetworks({},{}): {}5532I: Returning {} network descriptors in {}ms", sport, resourceType, LOG_PREFIX,  networkDtos.size(), System.currentTimeMillis() - startTime);
        return responseEntity;
    }

    /**
     * Persists the specified NetworkCandidate using info in its DTO object.
     *
     * @param networkCandidateDto The network to be persisted
     * @param resourceType        The type of network resource, such as DL4J_NETWORK, currently the only type
     *                            of network that is supported (KERAS someday, maybe, fingers crossed).
     * @return An HttpStatus of 201, along with the resource URI, and the network's descriptor DTO, if successful.
     * If the network could not be saved, an exception will be thrown, and different HttpStatus will be returned
     * (such as 500), along with a message indicating what went wrong.
     */
    @PostMapping
    public ResponseEntity<NetworkDescriptorDto> createNetworkResourceFile(@RequestBody final NetworkCandidateDto networkCandidateDto,
                                                                          @RequestParam(value = "resourceType") final ResourceType resourceType) {
        final ResponseEntity<NetworkDescriptorDto> responseEntity;
        long startTime = System.currentTimeMillis();
        log.debug("createNetworkResourceFile({}): {}9905D: Processing request...", LOG_PREFIX, resourceType);

        NetworkDescriptor networkDescriptor = NetworkDescriptor.fromDto(networkCandidateDto.getNetworkDescriptorDto());
        byte[] networkCandidateByteArray = networkCandidateDto.getNetwork();
        NetworkDescriptor persistedNetworkDescriptor = resourceService.persistNetworkResource(networkDescriptor, networkCandidateByteArray, resourceType);

        NetworkDescriptorDto dto = persistedNetworkDescriptor.toDto();
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .buildAndExpand()
                .toUri();
        responseEntity = ResponseEntity
                .created(location)
                .body(dto);
        log.info("createNetworkResourceFile({}): {}5534I: Returning response in {}ms", resourceType, LOG_PREFIX, System.currentTimeMillis() - startTime);
        if (log.isTraceEnabled()) {
            log.trace("createNetworkResourceFile({}): Returning response in {}ms: {}", resourceType, System.currentTimeMillis() - startTime, responseEntity);
        }
        return responseEntity;
    }

    @PostMapping("/{sport}/networks/reindex")
    public ResponseEntity<Void> reindexNetworkFiles(@PathVariable(name = "sport") final String sport) {
        final ResponseEntity<Void> ret;
        long startTime = System.currentTimeMillis();
        log.info("reindexNetworkFiles({}): {}5536I: Reindexing all network files...", sport, LOG_PREFIX);
        //
        // Kick off the index rebuild, return 202 ACCEPTED to the caller, it may be a while
        // before all the indexes can be rebuilt, depending on how many trained networks there
        // are for this sport.
        CompletableFuture.supplyAsync(() -> {
                    resourceService.reindexAllNetworks(sport);
                    log.info("reindexNetworkFiles({}): {}5538I: Reindexing complete in {}", sport, LOG_PREFIX, DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - startTime));
                    return null;
                })
                .exceptionally(e -> {
                    log.error("reindexNetworkFiles({}): {}0011E: Exception occurred while reindexing network files: {}", sport, LOG_PREFIX, e.getLocalizedMessage(), e);
                    return null;
                });
        //
        // Return to the caller and let the index rebuild marinate
        ret = ResponseEntity.accepted()
                .build();
        log.info("reindexNetworkFiles({}): {}5540I: Initiated index rebuild, returning response to caller: {}", sport, LOG_PREFIX, ret);
        return ret;
    }

}
