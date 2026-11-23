/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jstevenperry.sportsnet.framework.dto.NetworkCandidateDto;
import com.jstevenperry.sportsnet.framework.dto.NetworkDescriptorDto;
import com.jstevenperry.sportsnet.framework.dto.NetworkRunnerPluginDto;
import com.jstevenperry.sportsnet.framework.dto.TournamentSimulationDto;
import com.jstevenperry.sportsnet.framework.exception.InternalServerErrorException;
import com.jstevenperry.sportsnet.framework.model.NetworkCandidate;
import com.jstevenperry.sportsnet.framework.model.NetworkDescriptor;
import com.jstevenperry.sportsnet.framework.model.ResourceType;
import com.jstevenperry.sportsnet.framework.model.SeasonData;
import com.jstevenperry.sportsnet.framework.service.AbstractNetworkTrainingService;
import com.jstevenperry.sportsnet.framework.service.AbstractTournamentRunnerService;
import com.jstevenperry.sportsnet.framework.service.NetworkRunnerPlugin;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@SuppressWarnings("unused")  // Framework classes do not need this inspection
public abstract class AbstractRestController<T extends SeasonData> {

    public static final String LOG_PREFIX = "COREART";

    private static final Logger log = LoggerFactory.getLogger(AbstractRestController.class);
    private final AbstractNetworkTrainingService<T> networkTrainingService;
    private final AbstractTournamentRunnerService<T> tournamentRunnerService;

    public AbstractRestController(final AbstractNetworkTrainingService<T> abstractNetworkTrainingService,
                                  final AbstractTournamentRunnerService<T> abstractTournamentRunnerService) {
        this.networkTrainingService = abstractNetworkTrainingService;
        this.tournamentRunnerService = abstractTournamentRunnerService;
    }

    /**
     * Using the information in the specified NetworkParametersDto object, train and
     * return information about the trained network to the caller. If the network
     * could not be trained for any reason, the response body will contain an empty
     * Optional object to indicate this.
     *
     * @param networkDescriptorDto Information about the network parameters to be used
     *                             for training the network.
     * @return ResponseEntity whose body contains the NetworkParametersDto object
     * describing the trained network, or an empty Optional if the network could not
     * be trained for any reason.
     */
    public ResponseEntity<Optional<NetworkDescriptorDto>> trainNetwork(@RequestBody final NetworkDescriptorDto networkDescriptorDto) {
        final ResponseEntity<Optional<NetworkDescriptorDto>> ret;
        //
        // Convert from wire format
        log.trace("trainNetwork(): NetworkParametersDto: {}", networkDescriptorDto);
        //
        // Call the service to do the dirty work
        final Optional<NetworkDescriptor> trainedNetworkOptional = networkTrainingService.trainAndEvaluateNetwork(networkDescriptorDto);
        if (trainedNetworkOptional.isPresent()) {
            final NetworkDescriptor networkDescriptor = trainedNetworkOptional.get();
            //
            // Map results to the wire format (DTO)
            final NetworkDescriptorDto dto = networkDescriptor.toDto();
            ret = ResponseEntity.ok(Optional.of(dto));
        } else {
            log.warn("trainNetwork(): {}0010W: Network could not be trained!", LOG_PREFIX);
            ret = ResponseEntity.ok(Optional.empty());
        }
        try {
            log.debug("trainNetwork(): DTO (as JSON):\n{}", networkDescriptorDto.toJsonString());
        } catch (JsonProcessingException e) {
            final String errorMessage = String.format("%s0015E: Error stringifying DTO as JSON object: %s", LOG_PREFIX, e.getLocalizedMessage());
            log.error("trainNetwork(): {}", errorMessage, e);
        }
        log.debug("trainNetwork(): Response: {}", ret);
        return ret;
    }

    /**
     * Train one or more networks using the parameters supplied in the specified NetworkParametersDto
     * object. The networks will be trained on the server, saved if above the threshold, and can be
     * queried or run later. This method simply returns to the caller that the request to train the
     * networks in a batch run has been accepted. The caller will have to use other methods on the
     * REST interface to determine what to do next.
     *
     * @return ResponseEntity containing the NetworkParametersDto and an HTTP ACCEPTED response.
     */
    public ResponseEntity<NetworkDescriptorDto> trainNetworkBatch(@RequestBody final NetworkDescriptorDto batchRequestParameters) {
        final ResponseEntity<NetworkDescriptorDto> ret;
        final long startTime = System.currentTimeMillis();
        //
        // Validate the parameters
        networkTrainingService.validateParametersOrError(batchRequestParameters);
        //
        // Convert from wire format
        log.trace("trainNetwork(): NetworkParametersDto: {}", batchRequestParameters);
        //
        // Validate the network before we enter the async loop
        networkTrainingService.validateParametersOrError(batchRequestParameters, true);
        //
        // Wrap the logic in a Future, but throw away the reference, we don't care about it
        CompletableFuture.supplyAsync(() -> {
            //
            // Process this async, let it finish whenever
            Integer numberOfTrainedNetworks;
            try {
                batchRequestParameters.setBatchTrainingRunId(UUID.randomUUID());
                numberOfTrainedNetworks = networkTrainingService.goBatch(batchRequestParameters);
            } catch (IOException | InterruptedException e) {
                final String errorMessage = String.format("%s0017E: Exception while training network: %s", LOG_PREFIX, e.getLocalizedMessage());
                log.error("trainNetworkBatch(): {}", errorMessage, e);
                throw new InternalServerErrorException(errorMessage, e);
            }
            //
            // Logic is done.
            log.debug("trainNetworkBatch(): Future completed: {} networks were trained: in {}ms", numberOfTrainedNetworks,
                    DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - startTime));
            return numberOfTrainedNetworks;
        }).thenAccept(numberOfTrainedNetworks -> {
            log.info("trainNetworkBatch(): Batch run finished. {} networks successfully trained and kept.", numberOfTrainedNetworks);
            log.info("trainNetworkBatch(): Batch run completed in {}ms", DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - startTime));
        }).exceptionally(e -> {
            //
            // Something went wrong, tell us about it
            log.error("trainNetworkBatch(): Error occurred during batch run: {}", e.getLocalizedMessage(), e);
            return null;
        });
        log.info("trainNetworkBatch(): Batch run initiated in {}ms", System.currentTimeMillis() - startTime);
        ret = ResponseEntity.accepted().body(batchRequestParameters);
        return ret;
    }

    /**
     * Return a list of the years for which the implementing subclass has tournament plugins.
     * The list will be empty if there are no plugins written for the network type of the implementing
     * subclass. This is rare and probably due to some lazy programmer not having gotten around to it
     * yet (it happens).
     */
    public ResponseEntity<List<NetworkRunnerPluginDto>> getTournamentYears() {
        final List<Integer> tournamentYears = tournamentRunnerService.fetchPluginYears();

        final List<NetworkRunnerPluginDto> networkRunnerPluginDtos = new ArrayList<>();
        for (Integer tournamentYear : tournamentYears) {
            final Optional<NetworkRunnerPlugin<T>> networkRunnerPluginOptional = tournamentRunnerService.fetchPlugin(tournamentYear);
            if (networkRunnerPluginOptional.isPresent()) {
                final NetworkRunnerPlugin<T> networkRunnerPlugin = networkRunnerPluginOptional.get();
                networkRunnerPluginDtos.add(
                        new NetworkRunnerPluginDto()
                                .setTournamentYear(networkRunnerPlugin.getTournamentYear())
                                .setPluginDescription(networkRunnerPlugin.getTournamentYearForDisplay())
                                .setOutputType(networkRunnerPlugin.getOutputType())
                );
            }
        }

        log.debug("getTournamentYears(): Returning NetworkRunnerPluginDtos: {}",
                StringUtils.join(networkRunnerPluginDtos.toString(), ','));
        return ResponseEntity.ok(networkRunnerPluginDtos);
    }

    /**
     * Run a simulation using the tournament simulation parameters in the specified DTO and return the
     * visualized tournament results as a String suitable for saving (most likely a CSV) to disk.
     *
     * @param tournamentSimulationDto The tournament simulation parameters DTO
     * @return As described above
     */
    public ResponseEntity<String> runSimulation(@RequestBody final TournamentSimulationDto tournamentSimulationDto) {
        final ResponseEntity<String> ret;
        final Optional<String> simulationResultsOptional = tournamentRunnerService.runTournament(tournamentSimulationDto);
        ret = simulationResultsOptional.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.ok("SIMULATION FAILED"));
        return ret;
    }

    /**
     * Returns a list of all years suitable for training (and evaluation) supported by the
     * training data defined for the network type of the implementing subclass.
     *
     * @return A List of years, such as 2010,2011,2012 and so forth that can be used to
     * train and evaluate networks for supervised learning pusposes.
     */
    public ResponseEntity<List<Integer>> getTrainingYears() {
        final List<Integer> trainingYears = networkTrainingService.getYearsForTraining();

        log.debug("getTrainingYears(): Returning years: {}", StringUtils.join(trainingYears, ','));
        return ResponseEntity.ok(trainingYears);
    }

    /**
     * Persist the network matching the specified UUID. If the network was previously trained
     * and not yet saved its network descriptor DTO will be returned after persisting it. If the network
     * already exists or the networkId doesn't exist in the unsaved networks map, a NotFoundException
     * will be thrown.
     *
     * @param networkId    The UUID of the network to be persisted
     * @param resourceType The resource type, such as DL4J_NETWORK
     * @return The network descriptor of the persisted network if found in the unsaved networks map.
     * If not found, a NotFoundException will be thrown.
     */
    public ResponseEntity<NetworkDescriptorDto> saveNetwork(final UUID networkId, final ResourceType resourceType) {
        final NetworkDescriptor networkDescriptor = networkTrainingService.saveNetwork(networkId, resourceType);

        final NetworkDescriptorDto dto = networkDescriptor.toDto();
        final URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .buildAndExpand()
                .toUri();
        log.debug("saveNetwork({}): Saved network: {} at URI: {}", networkId, dto, location);
        return ResponseEntity.created(location).body(dto);
    }

    /**
     * Fetch the all network descriptors for the type of network of the implementing subclass.
     * Callers should not expect the actual trained network. All that is returned from this
     * method is a list of the network descriptors if any are found, or an empty list if none
     * could be found.
     *
     * @param unsaved If true, return only the unsaved network descriptors, otherwise return
     *                the list of persisted network descriptors
     * @return As described above.
     */
    public ResponseEntity<List<NetworkDescriptorDto>> getNetworks(final Boolean unsaved) {
        final ResponseEntity<List<NetworkDescriptorDto>> ret;
        final List<NetworkCandidateDto> networkCandidateDtos;
        if (unsaved != null && unsaved) {
            networkCandidateDtos = networkTrainingService.fetchUnsavedNetworks();
        } else {
            networkCandidateDtos = networkTrainingService.getNetworks();
        }
        //
        // Convert the List of NetworkCandidates into a List of NetworkParameterDto
        // Callers expose this method on their own RestController just to get the network
        // descriptors, they don't need the actual trained network.
        final List<NetworkDescriptorDto> networkDescriptorDtos = networkCandidateDtos.stream()
                .map(NetworkCandidateDto::getNetworkDescriptorDto)
                .collect(Collectors.toList());
        ret = ResponseEntity.ok(networkDescriptorDtos);
        return ret;
    }

    /**
     * Fetch the specified network by its ID. If the network has not been saved, it will be
     * present in the map, and can be peeked out of there and returned in an Optional.
     * If it is not in the unsaved networks map, then try to find it from persistent storage
     * and return it after fetching it from there in the Optional.
     * If the network cannot be located in either place, the Optional that is returned will
     * be empty.
     *
     * @param networkId The ID of the network to be fetched and returned
     * @return An Optional that contains the NetworkCandidatDto if the network could be found,
     * or an empty Optional if it could not be.
     */
    public ResponseEntity<Optional<NetworkDescriptorDto>> getNetworkById(final UUID networkId) {
        final ResponseEntity<Optional<NetworkDescriptorDto>> ret;
        final NetworkCandidate networkCandidate = networkTrainingService.peekNetworkCandidate(networkId);
        if (networkCandidate != null) {
            ret = ResponseEntity.ok(Optional.of(networkCandidate.getTrainedNetworkParameters().toDto()));
        } else {
            final Optional<NetworkCandidateDto> networkCandidateDto = networkTrainingService.findNetworkById(networkId);
            ret = networkCandidateDto
                    .map(candidateDto -> ResponseEntity.ok(Optional.of(candidateDto.getNetworkDescriptorDto())))
                    .orElseGet(() -> ResponseEntity.ok(Optional.empty()));
        }
        return ret;
    }

    /**
     * Evaluate the specified network using tournament data for the specified year. This method is used
     * as a sanity check to ensure that trained and saved networks have been properly trained for the
     * years they were trained for.
     *
     * @param networkId      The network to evaluate
     * @param evaluationYear The year for which the network is to be evaluated
     * @return A network descriptor DTO describing the network after evaluation
     */
    public ResponseEntity<NetworkDescriptorDto> evaluateNetwork(final UUID networkId, final Integer evaluationYear) {
        final ResponseEntity<NetworkDescriptorDto> ret;
        final Optional<NetworkDescriptor> networkDescriptorOptional = tournamentRunnerService.evaluateNetwork(networkId, evaluationYear);
        if (networkDescriptorOptional.isPresent()) {
            NetworkDescriptor networkDescriptor = networkDescriptorOptional.get();
            NetworkDescriptorDto networkDescriptorDto = networkDescriptor.toDto();
            ret = ResponseEntity.ok(networkDescriptorDto);
        } else {
            String error = String.format("%s0000E: Error evaluating network ID %s for year %d", LOG_PREFIX, networkId, evaluationYear);
            log.error(error);
            throw new InternalServerErrorException(error);
        }
        return ret;

    }

}
