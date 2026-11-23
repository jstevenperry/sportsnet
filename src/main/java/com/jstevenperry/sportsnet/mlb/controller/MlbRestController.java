/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.mlb.controller;

import com.jstevenperry.sportsnet.framework.controller.AbstractRestController;
import com.jstevenperry.sportsnet.framework.dto.NetworkDescriptorDto;
import com.jstevenperry.sportsnet.framework.dto.NetworkRunnerPluginDto;
import com.jstevenperry.sportsnet.framework.dto.TournamentSimulationDto;
import com.jstevenperry.sportsnet.framework.model.ResourceType;
import com.jstevenperry.sportsnet.mlb.model.MlbSeasonData;
import com.jstevenperry.sportsnet.mlb.service.MlbNetworkTrainingService;
import com.jstevenperry.sportsnet.mlb.service.MlbTournamentRunnerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/mlb")
public class MlbRestController extends AbstractRestController<MlbSeasonData> {


    public MlbRestController(final MlbNetworkTrainingService networkTrainingService,
                             final MlbTournamentRunnerService tournamentRunnerService) {
        super(networkTrainingService, tournamentRunnerService);
    }

    @GetMapping("/networks")
    public ResponseEntity<List<NetworkDescriptorDto>> getNetworks(@RequestParam(required = false) final Boolean unsaved) {
        return super.getNetworks(unsaved);
    }

    @GetMapping("/networks/{networkId}")
    public ResponseEntity<Optional<NetworkDescriptorDto>> getNetworkById(@PathVariable final UUID networkId) {
        return super.getNetworkById(networkId);
    }

    @PostMapping("/networks")
    public ResponseEntity<Optional<NetworkDescriptorDto>> trainNetwork(@RequestBody final NetworkDescriptorDto networkParametersDto) {
        return super.trainNetwork(networkParametersDto);
    }

    @PostMapping("/networks/{networkId}")
    public ResponseEntity<NetworkDescriptorDto> saveTrainedNetwork(@PathVariable final UUID networkId,
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
}
