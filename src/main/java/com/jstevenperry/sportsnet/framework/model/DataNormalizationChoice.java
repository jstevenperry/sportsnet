/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.model;

import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerMinMaxScaler;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;

import java.io.Serializable;
import java.util.Optional;
import java.util.function.Supplier;

public enum DataNormalizationChoice implements Serializable {
    NORMALIZER_STANDARDIZE(NormalizerStandardize::new, 1),
    NORMALIZER_ZERO_TO_ONE(() -> new NormalizerMinMaxScaler(0, 1), 2),
    NORMALIZER_NEGATIVE_ONE_TO_ONE(() -> new NormalizerMinMaxScaler(-1, 1), 3),
    ;

    private final Supplier<DataNormalization> dataNormalizationSupplier;
    private final int ordinal;

    DataNormalizationChoice(final Supplier<DataNormalization> dataNormalizationSupplier, final int ordinal) {

        this.dataNormalizationSupplier = dataNormalizationSupplier;
        this.ordinal = ordinal;
    }

    public static Optional<DataNormalizationChoice> from(final int choice) {
        Optional<DataNormalizationChoice> ret = Optional.empty();
        for (DataNormalizationChoice menuChoice : DataNormalizationChoice.values()) {
            if (menuChoice.getOrdinal() == choice) {
                ret = Optional.of(menuChoice);
                break;
            }
        }
        return ret;
    }

    public static Optional<DataNormalizationChoice> from(final DataNormalization dataNormalization) {
        Optional<DataNormalizationChoice> ret = Optional.empty();
        for (DataNormalizationChoice menuChoice : DataNormalizationChoice.values()) {
            if (menuChoice.getDataNormalization().equals(dataNormalization)) {
                ret = Optional.of(menuChoice);
                break;
            }
        }
        return ret;
    }

    public DataNormalization getDataNormalization() {
        return dataNormalizationSupplier.get();
    }

    public int getOrdinal() {
        return ordinal;
    }

}
