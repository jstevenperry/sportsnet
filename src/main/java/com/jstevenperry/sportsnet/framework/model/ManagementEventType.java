/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.model;

public enum ManagementEventType {
    TRAIN_NETWORK_START,
    TRAIN_NETWORK_END,
    TRAIN_NETWORK_BATCH_START,
    TRAIN_NETWORK_BATCH_ITERATION_END,
    TRAIN_NETWORK_BATCH_END,
    RUN_SIMULATION_START,
    RUN_SIMULATION_END,
    REINDEX_START,
    REINDEX_END,
    GET_ALL_NETWORKS,
    GET_NETWORK_BY_ID,
    SAVE_NETWORK,
    FETCH_RESOURCE_FILE,
    EXCEPTION,
    PING
}
