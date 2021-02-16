/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.menus;

public enum NetworkChoice {
    UNKNOWN("That is not a choice", -1),
    TRAIN_NETWORK("Train a new neural network", 1),
    TRAIN_NETWORK_BATCH("Train a new neural network (BATCH MODE)", 2),
    PERSIST_UNSAVED_NETWORKS("Persist unsaved neural network(s)", 3),
    LIST_SAVED_NETWORKS("List saved networks", 4),
    EVALUATE_NETWORK("Evaluate a trained neural network", 5),
    CLEAR_UNSAVED_NETWORKS("Clear unsaved networks", 6),
    // 7
    // 8
    RUN_NETWORK("Run a trained neural network", 9),
    QUIT("Go back to the previous menu", 0);

    private final String displayValue;
    private final int ordinalValue;

    NetworkChoice(final String displayValue, final int ordinalValue) {
        this.displayValue = displayValue;
        this.ordinalValue = ordinalValue;
    }

    public String getDisplayValue() {
        return this.displayValue;
    }

    public int getOrdinalValue() {
        return ordinalValue;
    }

    public static NetworkChoice from(final int ordinalValueCandiate) {
        NetworkChoice ret = NetworkChoice.UNKNOWN;
        for (NetworkChoice networkChoice : values()) {
            if (networkChoice.getOrdinalValue() == ordinalValueCandiate) {
                ret = networkChoice;
                break;
            }
        }
        return ret;
    }

    public boolean isSuitableForDisplay() {
        return this != NetworkChoice.UNKNOWN;
    }
}
