package com.jstevenperry.sportsnet.mlb.util;

public class MlbUtils {
    public static String toStandardTeamName(final String rawTeamName) {
        if ("Cleveland Indians".equals(rawTeamName)) {
            return "Cleveland Guardians";
        } else if ("Los Angeles Angels of Anaheim".equals(rawTeamName)) {
            return "Los Angeles Angels";
        }
        return rawTeamName;
    }
}
