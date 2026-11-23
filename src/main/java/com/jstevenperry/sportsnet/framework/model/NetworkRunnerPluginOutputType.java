/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.model;

public enum NetworkRunnerPluginOutputType {
    /**
     * Produce a formatted bracket and return the contents as s String
     */
    BRACKET,
    /**
     * Produce a ranked list of tournament scores by team in descending
     * order (highest score first), and return the contents as a String
     */
    TEAM_RANKING
}
