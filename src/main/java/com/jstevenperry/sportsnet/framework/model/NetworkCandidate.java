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
  public int compareTo(@NotNull final NetworkCandidate networkCandidate) {
    int result = this.getNetworkParameters().getWhenTrained().compareTo(networkCandidate.getNetworkParameters().getWhenTrained());
    if (result == 0) {
      result = this.getNetworkParameters().getNetworkLayout().compareTo(networkCandidate.getNetworkParameters().getNetworkLayout());
    }
    return result;
  }
}
