/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.model;

import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.util.Optional;

public enum LossFunctionChoice {
    MSE(LossFunctions.LossFunction.MSE, 1),
    L1(LossFunctions.LossFunction.L1, 2),
    XENT(LossFunctions.LossFunction.XENT, 3),
    MCXENT(LossFunctions.LossFunction.MCXENT, 4),
    SPARSE_MCXENT(LossFunctions.LossFunction.SPARSE_MCXENT, 5),
    SQUARED_LOSS(LossFunctions.LossFunction.SQUARED_LOSS, 6),
    RECONSTRUCTION_CROSSENTROPY(LossFunctions.LossFunction.RECONSTRUCTION_CROSSENTROPY, 7),
    NEGATIVELOGLIKELIHOOD(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD, 8),
    COSINE_PROXIMITY(LossFunctions.LossFunction.COSINE_PROXIMITY, 9),
    HINGE(LossFunctions.LossFunction.HINGE, 10),
    SQUARED_HINGE(LossFunctions.LossFunction.SQUARED_HINGE, 11),
    KL_DIVERGENCE(LossFunctions.LossFunction.KL_DIVERGENCE, 12),
    MEAN_ABSOLUTE_ERROR(LossFunctions.LossFunction.MEAN_ABSOLUTE_ERROR, 13),
    L2(LossFunctions.LossFunction.L2, 14),
    MEAN_ABSOLUTE_PERCENTAGE_ERROR(LossFunctions.LossFunction.MEAN_ABSOLUTE_PERCENTAGE_ERROR, 15),
    MEAN_SQUARED_LOGARITHMIC_ERROR(LossFunctions.LossFunction.MEAN_SQUARED_LOGARITHMIC_ERROR, 16),
    POISSON(LossFunctions.LossFunction.POISSON, 17),
    WASSERSTEIN(LossFunctions.LossFunction.WASSERSTEIN, 18);

    private final LossFunctions.LossFunction lossFunction;
    private final int ordinal;

    LossFunctionChoice(final LossFunctions.LossFunction lossFunction, final int ordinal) {

        this.lossFunction = lossFunction;
        this.ordinal = ordinal;
    }

    public static Optional<LossFunctionChoice> from(final int choice) {
        Optional<LossFunctionChoice> ret = Optional.empty();
        for (LossFunctionChoice menuChoice : LossFunctionChoice.values()) {
            if (menuChoice.getOrdinal() == choice) {
                ret = Optional.of(menuChoice);
                break;
            }
        }
        return ret;
    }

    public static Optional<LossFunctionChoice> from(final LossFunctions.LossFunction lossFunction) {
        Optional<LossFunctionChoice> ret = Optional.empty();
        for (LossFunctionChoice menuChoice : LossFunctionChoice.values()) {
            if (menuChoice.getLossFunction().equals(lossFunction)) {
                ret = Optional.of(menuChoice);
                break;
            }
        }
        return ret;
    }

    public LossFunctions.LossFunction getLossFunction() {
        return lossFunction;
    }

    public int getOrdinal() {
        return ordinal;
    }

}
