/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.model;

import org.nd4j.linalg.activations.Activation;

import java.util.Optional;

public enum ActivationFunctionChoice {

    TANH(Activation.TANH, 1),
    CUBE(Activation.CUBE, 2),
    ELU(Activation.ELU, 3),
    HARDSIGMOID(Activation.HARDSIGMOID, 4),
    HARDTANH(Activation.HARDTANH, 5),
    IDENTITY(Activation.IDENTITY, 6),
    LEAKYRELU(Activation.LEAKYRELU, 7),
    RATIONALTANH(Activation.RATIONALTANH, 8),
    RELU(Activation.RELU, 9),
    RELU6(Activation.RELU6, 10),
    RRELU(Activation.RRELU, 11),
    SIGMOID(Activation.SIGMOID, 12),
    SOFTMAX(Activation.SOFTMAX, 13),
    SOFTPLUS(Activation.SOFTPLUS, 14),
    SOFTSIGN(Activation.SOFTSIGN, 15),
    RECTIFIEDTANH(Activation.RECTIFIEDTANH, 16),
    SELU(Activation.SELU, 17),
    SWISH(Activation.SWISH, 18),
    THRESHOLDEDRELU(Activation.THRESHOLDEDRELU, 19),
    GELU(Activation.GELU, 20),
    MISH(Activation.MISH, 21);

    private final Activation activation;
    private final int ordinal;

    ActivationFunctionChoice(final Activation activation, final int ordinal) {
        this.activation = activation;
        this.ordinal = ordinal;
    }

    public static Optional<ActivationFunctionChoice> from(final int choice) {
        Optional<ActivationFunctionChoice> ret = Optional.empty();
        for (ActivationFunctionChoice activationFunctionChoice : ActivationFunctionChoice.values()) {
            if (activationFunctionChoice.getOrdinal() == choice) {
                ret = Optional.of(activationFunctionChoice);
                break;
            }
        }
        return ret;
    }

    public static Optional<ActivationFunctionChoice> from(final Activation activation) {
        Optional<ActivationFunctionChoice> ret = Optional.empty();
        for (ActivationFunctionChoice activationFunctionChoice : ActivationFunctionChoice.values()) {
            if (activationFunctionChoice.getActivation().equals(activation)) {
                ret = Optional.of(activationFunctionChoice);
                break;
            }
        }
        return ret;
    }

    public Activation getActivation() {
        return activation;
    }

    public int getOrdinal() {
        return ordinal;
    }
}
