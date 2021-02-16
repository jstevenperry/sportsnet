/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.model;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.jetbrains.annotations.NotNull;

/**
 * A network that has been trained and the user has decided to keep.
 * That doesn't mean the network will make the final cut, but it
 * is a candidate for persistence. Hence the name.
 */
public class NetworkCandidate implements Comparable<NetworkCandidate> {
    private final NetworkParameters networkParameters;
    private final MultiLayerNetwork multiLayerNetwork;

    public NetworkCandidate(final NetworkParameters networkParameters,
                            final MultiLayerNetwork multiLayerNetwork) {

        this.networkParameters = networkParameters;
        this.multiLayerNetwork = multiLayerNetwork;
    }

    public NetworkParameters getNetworkParameters() {
        return networkParameters;
    }

    public MultiLayerNetwork getMultiLayerNetwork() {
        return multiLayerNetwork;
    }

    @Override
    public String toString() {
        return "NetworkCandidate{" +
                "networkParameters=" + networkParameters.getNetworkLayout() +
                ", multiLayerNetwork=" + multiLayerNetwork +
                '}';
    }


    @Override
    public int compareTo(@NotNull final NetworkCandidate that) {
        NetworkParameters thisNetworkParameters = this.getNetworkParameters();
        NetworkParameters thatNetworkParameters = that.getNetworkParameters();
        int result = Double.compare(thisNetworkParameters.getNetworkAccuracy(), thatNetworkParameters.getNetworkAccuracy());
        if (result == 0) {
            // Largest first
            result = thisNetworkParameters.getWhenTrained().toLocalDate().compareTo(thatNetworkParameters.getWhenTrained().toLocalDate());
        }
        if (result == 0) {
            result = thisNetworkParameters.getNetworkLayout().compareTo(thatNetworkParameters.getNetworkLayout());
        }
        return result;
    }
}
