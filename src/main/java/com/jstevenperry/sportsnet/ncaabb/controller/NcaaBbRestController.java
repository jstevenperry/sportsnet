/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.ncaabb.controller;

import com.jstevenperry.sportsnet.framework.controller.AbstractRestController;
import com.jstevenperry.sportsnet.framework.dto.NetworkDescriptorDto;
import com.jstevenperry.sportsnet.framework.dto.NetworkRunnerPluginDto;
import com.jstevenperry.sportsnet.framework.dto.TournamentSimulationDto;
import com.jstevenperry.sportsnet.framework.model.ResourceType;
import com.jstevenperry.sportsnet.ncaabb.model.NcaaBbSeasonData;
import com.jstevenperry.sportsnet.ncaabb.service.NcaaBbNetworkTrainingService;
import com.jstevenperry.sportsnet.ncaabb.service.NcaaBbTournamentRunnerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/ncaabb")
public class NcaaBbRestController extends AbstractRestController<NcaaBbSeasonData> {


    public NcaaBbRestController(final NcaaBbNetworkTrainingService networkTrainingService,
                                final NcaaBbTournamentRunnerService tournamentRunnerService) {
        super(networkTrainingService, tournamentRunnerService);
    }

    @GetMapping("/networks")
    public ResponseEntity<List<NetworkDescriptorDto>> getNetworks(@RequestParam(required = false) final Boolean unsaved) {
        return super.getNetworks(unsaved);
    }

    @GetMapping("/networks/{networkId}")
    public ResponseEntity<Optional<NetworkDescriptorDto>> getNetworkById(@PathVariable(name = "networkId") final UUID networkId) {
        return super.getNetworkById(networkId);
    }

    @PostMapping("/networks")
    public ResponseEntity<Optional<NetworkDescriptorDto>> trainNetwork(@RequestBody final NetworkDescriptorDto networkParametersDto) {
        return super.trainNetwork(networkParametersDto);
    }

    @PostMapping("/networks/{networkId}")
    public ResponseEntity<NetworkDescriptorDto> saveTrainedNetwork(@PathVariable(name = "networkId") final UUID networkId,
                                                                   @RequestParam final ResourceType resourceType) {
        return super.saveNetwork(networkId, resourceType);
    }

    @PostMapping("/simulations")
    public ResponseEntity<String> runSimulation(@RequestBody final TournamentSimulationDto tournamentSimulationDto) {
        return super.runSimulation(tournamentSimulationDto);
    }

    @GetMapping("/tournamentYears")
    public ResponseEntity<List<NetworkRunnerPluginDto>> getTournamentYears() {
        return super.getTournamentYears();
    }

    @GetMapping("/trainingYears")
    public ResponseEntity<List<Integer>> getTrainingYears() {
        return super.getTrainingYears();
    }

    @PostMapping("/networks/batch")
    public ResponseEntity<NetworkDescriptorDto> trainNetworkBatch(@RequestBody final NetworkDescriptorDto networkDescriptorDto) {
        return super.trainNetworkBatch(networkDescriptorDto);
    }

    @GetMapping("/evaluate/networks/{networkId}/{evaluationYear}")
    public ResponseEntity<NetworkDescriptorDto> evaluateNetwork(@PathVariable(name = "networkId") final UUID networkId,
                                                                @PathVariable(name = "evaluationYear") final Integer evaluationYear) {
        return super.evaluateNetwork(networkId, evaluationYear);
    }
}
