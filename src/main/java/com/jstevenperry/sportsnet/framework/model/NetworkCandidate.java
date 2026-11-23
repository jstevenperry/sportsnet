/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.model;

import com.jstevenperry.sportsnet.framework.dto.NetworkCandidateDto;
import com.jstevenperry.sportsnet.framework.util.AbstractNetworkUtils;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;

import java.io.Serializable;

/**
 * A network that has been trained and the user has decided to keep.
 * That doesn't mean the network will make the final cut, but it
 * is a candidate for persistence. Hence the name.
 */
public class NetworkCandidate implements Comparable<NetworkCandidate>, Serializable {

    private final NetworkDescriptor networkDescriptor;
    private final MultiLayerNetwork multiLayerNetwork;

    public NetworkCandidate(final NetworkDescriptor networkDescriptor,
                            final MultiLayerNetwork multiLayerNetwork) {

        this.networkDescriptor = networkDescriptor;
        this.multiLayerNetwork = multiLayerNetwork;
    }

    public static NetworkCandidate fromDto(final NetworkCandidateDto networkCandidateDto) {
        NetworkDescriptor networkDescriptor = NetworkDescriptor.fromDto(networkCandidateDto.getNetworkDescriptorDto());
        return AbstractNetworkUtils.reconstituteNetworkFromComponents(
                networkDescriptor,
                networkCandidateDto.getNetwork()
        );
    }

    public NetworkCandidateDto toDto() {
        return new NetworkCandidateDto()
                .setNetworkDescriptorDto(getTrainedNetworkParameters().toDto())
                .setNetwork(AbstractNetworkUtils.convertNetworkCandidateToByteArray(this));
    }

    public NetworkDescriptor getTrainedNetworkParameters() {
        return networkDescriptor;
    }

    public MultiLayerNetwork getMultiLayerNetwork() {
        return multiLayerNetwork;
    }

    @Override
    public String toString() {
        return "NetworkCandidate{" +
                "networkParameters=" + networkDescriptor.getNetworkId() +
                ", multiLayerNetwork=" + multiLayerNetwork +
                '}';
    }


    @Override
    public int compareTo(final NetworkCandidate that) {
        NetworkDescriptor thisNetworkDescriptor = this.getTrainedNetworkParameters();
        NetworkDescriptor thatNetworkDescriptor = that.getTrainedNetworkParameters();
        int result = Double.compare(thisNetworkDescriptor.getNetworkAccuracy(), thatNetworkDescriptor.getNetworkAccuracy());
        if (result == 0) {
            // Largest first
            result = thisNetworkDescriptor.getWhenTrained().toLocalDate().compareTo(thatNetworkDescriptor.getWhenTrained().toLocalDate());
        }
        if (result == 0) {
            result = thisNetworkDescriptor.getNetworkLayout().compareTo(thatNetworkDescriptor.getNetworkLayout());
        }
        return result;
    }
}
