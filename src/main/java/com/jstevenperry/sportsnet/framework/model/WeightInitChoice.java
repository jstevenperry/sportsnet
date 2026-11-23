/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.model;

import org.deeplearning4j.nn.weights.WeightInit;

import java.util.Optional;

public enum WeightInitChoice {
    DISTRIBUTION(WeightInit.DISTRIBUTION, 1),
    ZERO(WeightInit.ZERO, 2),
    ONES(WeightInit.ONES, 3),
    SIGMOID_UNIFORM(WeightInit.SIGMOID_UNIFORM, 4),
    NORMAL(WeightInit.NORMAL, 5),
    LECUN_NORMAL(WeightInit.LECUN_NORMAL, 6),
    UNIFORM(WeightInit.UNIFORM, 7),
    XAVIER(WeightInit.XAVIER, 8),
    XAVIER_UNIFORM(WeightInit.XAVIER_UNIFORM, 9),
    XAVIER_FAN_IN(WeightInit.XAVIER_FAN_IN, 10),
    XAVIER_LEGACY(WeightInit.XAVIER_LEGACY, 11),
    RELU(WeightInit.RELU, 12),
    RELU_UNIFORM(WeightInit.RELU_UNIFORM, 13),
    IDENTITY(WeightInit.IDENTITY, 14),
    LECUN_UNIFORM(WeightInit.LECUN_UNIFORM, 15),
    VAR_SCALING_NORMAL_FAN_IN(WeightInit.VAR_SCALING_NORMAL_FAN_IN, 16),
    VAR_SCALING_NORMAL_FAN_OUT(WeightInit.VAR_SCALING_NORMAL_FAN_OUT, 17),
    VAR_SCALING_NORMAL_FAN_AVG(WeightInit.VAR_SCALING_NORMAL_FAN_AVG, 18),
    VAR_SCALING_UNIFORM_FAN_IN(WeightInit.VAR_SCALING_UNIFORM_FAN_IN, 19),
    VAR_SCALING_UNIFORM_FAN_OUT(WeightInit.VAR_SCALING_UNIFORM_FAN_OUT, 20),
    VAR_SCALING_UNIFORM_FAN_AVG(WeightInit.VAR_SCALING_UNIFORM_FAN_AVG, 21);

    private final WeightInit weightInit;
    private final int ordinal;

    WeightInitChoice(final WeightInit weightInit, final int ordinal) {
        this.weightInit = weightInit;
        this.ordinal = ordinal;
    }

    public static Optional<WeightInitChoice> from(final int choice) {
        Optional<WeightInitChoice> ret = Optional.empty();
        for (WeightInitChoice menuChoice : WeightInitChoice.values()) {
            if (menuChoice.getOrdinal() == choice) {
                ret = Optional.of(menuChoice);
                break;
            }
        }
        return ret;
    }

    public static Optional<WeightInitChoice> from(final WeightInit weightInit) {
        Optional<WeightInitChoice> ret = Optional.empty();
        for (WeightInitChoice menuChoice : WeightInitChoice.values()) {
            if (menuChoice.getWeightInit().equals(weightInit)) {
                ret = Optional.of(menuChoice);
                break;
            }
        }
        return ret;
    }

    public WeightInit getWeightInit() {
        return weightInit;
    }

    public int getOrdinal() {
        return ordinal;
    }

}
