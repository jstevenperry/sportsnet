/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.model;

import org.nd4j.linalg.learning.config.AMSGrad;
import org.nd4j.linalg.learning.config.AdaDelta;
import org.nd4j.linalg.learning.config.AdaGrad;
import org.nd4j.linalg.learning.config.AdaMax;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.learning.config.IUpdater;
import org.nd4j.linalg.learning.config.Nadam;
import org.nd4j.linalg.learning.config.NoOp;
import org.nd4j.linalg.learning.config.RmsProp;
import org.nd4j.linalg.learning.config.Sgd;

import java.util.Optional;
import java.util.function.Supplier;

public enum UpdaterChoice {
    ADA_DELTA(1),
    ADA_GRAD(2),
    ADAM(3),
    ADA_MAX(4),
    AMS_GRAD(5),
    NADAM(6),
    NOOP(7),
    RMS_PROP(8),
    SGD(9);


    /**
     * Scale the specified valueToScale by the specified scale value. This is used
     * to scale values like learning rate and epsilon so that single value may
     * be specified in the configuration properties that can then be scaled to work
     * for the wildly inconsistent values that are used across implementations
     * in the DL4J framework.
     * <p>
     * The idea is to specify one value in the properties whose "normal" value is
     * 1 and can then be scaled from there since there is no consistency among
     * the various DL4J implementations (such is the nature of open source projects
     * and software development in general... poor communication)
     */
    private static double scale(final Double valueToScale, final double scaleValue) {
        return valueToScale * scaleValue;
    }

    private final int ordinal;

    UpdaterChoice(final int ordinal) {
        this.ordinal = ordinal;
    }

    public static Optional<UpdaterChoice> from(final int choice) {
        Optional<UpdaterChoice> ret = Optional.empty();
        for (UpdaterChoice menuChoice : UpdaterChoice.values()) {
            if (menuChoice.getOrdinal() == choice) {
                ret = Optional.of(menuChoice);
                break;
            }
        }
        return ret;
    }

    public static IUpdater fromUpdaterChoice(final NetworkDescriptor networkDescriptor) {
        IUpdater ret;
        UpdaterChoice updaterChoice = networkDescriptor.getUpdater();
        switch (updaterChoice) {
            case ADA_DELTA:
                ret = new AdaDelta();
                break;
            case ADA_GRAD:
                ret = new AdaGrad(scale(networkDescriptor.getLearningRuleLearningRate(), 0.1),
                        networkDescriptor.getLearningRuleLearningRate() / 1e8);
                break;
            case ADAM:
                ret = new Adam(scale(networkDescriptor.getLearningRuleLearningRate(), 0.001),
                    scale(Adam.DEFAULT_ADAM_BETA1_MEAN_DECAY, networkDescriptor.getBeta1DecayScaleValue()),
                    scale(Adam.DEFAULT_ADAM_BETA2_VAR_DECAY, networkDescriptor.getBeta1DecayScaleValue()),
                        networkDescriptor.getLearningRuleLearningRate() / 1e8);
                break;
            case ADA_MAX:
                ret = new AdaMax(scale(networkDescriptor.getLearningRuleLearningRate(), 0.001),
                scale(AdaMax.DEFAULT_ADAMAX_BETA1_MEAN_DECAY, networkDescriptor.getBeta1DecayScaleValue()),
                scale(AdaMax.DEFAULT_ADAMAX_BETA2_VAR_DECAY, networkDescriptor.getBeta1DecayScaleValue()),
                        networkDescriptor.getLearningRuleLearningRate() / 1e8);
                break;
            case AMS_GRAD:
                ret = new AMSGrad(scale(networkDescriptor.getLearningRuleLearningRate(), 0.001),
                scale(AMSGrad.DEFAULT_AMSGRAD_BETA1_MEAN_DECAY, networkDescriptor.getBeta1DecayScaleValue()),
                scale(AMSGrad.DEFAULT_AMSGRAD_BETA2_VAR_DECAY, networkDescriptor.getBeta1DecayScaleValue()),
                        networkDescriptor.getLearningRuleLearningRate() / 1e8);
                break;
            case NADAM:
                ret = new Nadam(scale(networkDescriptor.getLearningRuleLearningRate(), 0.001),
                scale(Nadam.DEFAULT_NADAM_BETA1_MEAN_DECAY, networkDescriptor.getBeta1DecayScaleValue()),
                scale(Nadam.DEFAULT_NADAM_BETA2_VAR_DECAY, networkDescriptor.getBeta1DecayScaleValue()),
                        networkDescriptor.getLearningRuleLearningRate() / 1e8);
                break;
            case NOOP:
                ret = new NoOp();
                break;
            case RMS_PROP:
                ret = new RmsProp(scale(networkDescriptor.getLearningRuleLearningRate(), 0.1));
                break;
            case SGD:
                ret = new Sgd(scale(networkDescriptor.getLearningRuleLearningRate(), 0.001));
                break;
            default:
                ret = new NoOp();
                break;
        }
        return ret;
    }

    @SuppressWarnings("unused")  // Framework classes do not need this inspection
    public IUpdater getUpdater(Supplier<IUpdater> updaterSupplier) {
        return updaterSupplier.get();
    }

    public int getOrdinal() {
        return ordinal;
    }

    @Override
    public String toString() {
        return String.format("%s", name());
    }
}
