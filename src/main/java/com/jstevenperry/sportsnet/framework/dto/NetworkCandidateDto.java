/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.dto;

import java.util.Arrays;

public class NetworkCandidateDto {
    private NetworkDescriptorDto networkDescriptorDto;
    private byte[] network;

    public NetworkDescriptorDto getNetworkDescriptorDto() {
        return networkDescriptorDto;
    }

    public NetworkCandidateDto setNetworkDescriptorDto(final NetworkDescriptorDto networkDescriptorDto) {
        this.networkDescriptorDto = networkDescriptorDto;
        return this;
    }

    public byte[] getNetwork() {
        return network;
    }

    public NetworkCandidateDto setNetwork(final byte[] network) {
        this.network = network;
        return this;
    }

    @Override
    public String toString() {
        return "NetworkCandidateDto{" +
                "networkParametersDto=" + networkDescriptorDto +
                ", network=" + Arrays.toString(network) +
                '}';
    }
}
