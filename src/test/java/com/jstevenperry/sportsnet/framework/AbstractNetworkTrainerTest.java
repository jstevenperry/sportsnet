/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet.framework;

import com.jstevenperry.sportsnet.framework.util.NetworkUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbstractNetworkTrainerTest {

    @Test
    void computeIterationNumberString() {
        int totalIterations = 40;
        int currentOuterIteration = 1; // Edge case
        int currentInnerIteration = 34;

        // Percentage: [(currentOuterIteration - 1) * totalIterations +  currentInnerIteration - 1] / totalIterations ^ 2
        String expected = String.format("%s", "1.34 (2.1%)");
        String actual = NetworkUtils.computeIterationNumberString(currentOuterIteration, currentInnerIteration, totalIterations);
        assertEquals(expected, actual);

        totalIterations = 50;
        currentOuterIteration = 24;
        currentInnerIteration = 14;

        // Percentage: [(currentOuterIteration - 1) * totalIterations +  currentInnerIteration - 1] / totalIterations ^ 2
        expected = String.format("%s", "24.14 (46.5%)");
        actual = NetworkUtils.computeIterationNumberString(currentOuterIteration, currentInnerIteration, totalIterations);
        assertEquals(expected, actual);

        totalIterations = 100;
        currentOuterIteration = 1; // Edge case
        currentInnerIteration = 1; // Edge case

        // Percentage: [(currentOuterIteration - 1) * totalIterations +  currentInnerIteration - 1] / totalIterations ^ 2
        expected = String.format("%s", "1.1 (0.0%)");
        actual = NetworkUtils.computeIterationNumberString(currentOuterIteration, currentInnerIteration, totalIterations);
        assertEquals(expected, actual);

        totalIterations = 1; // Edge case
        currentOuterIteration = 1; // Edge case
        currentInnerIteration = 1; // Edge case

        // Percentage: [(currentOuterIteration - 1) * totalIterations +  currentInnerIteration - 1] / totalIterations ^ 2
        expected = String.format("%s", "1.1 (0.0%)");
        actual = NetworkUtils.computeIterationNumberString(currentOuterIteration, currentInnerIteration, totalIterations);
        assertEquals(expected, actual);

        totalIterations = 2;
        currentOuterIteration = 1; // Edge case
        currentInnerIteration = 2;

        // Percentage: [(currentOuterIteration - 1) * totalIterations +  currentInnerIteration - 1] / totalIterations ^ 2
        expected = String.format("%s", "1.2 (25.0%)");
        actual = NetworkUtils.computeIterationNumberString(currentOuterIteration, currentInnerIteration, totalIterations);
        assertEquals(expected, actual);

        totalIterations = 4;
        currentOuterIteration = 2;
        currentInnerIteration = 3;

        // Percentage: [(currentOuterIteration - 1) * totalIterations +  currentInnerIteration - 1] / totalIterations ^ 2
        expected = String.format("%s", "2.3 (37.5%)");
        actual = NetworkUtils.computeIterationNumberString(currentOuterIteration, currentInnerIteration, totalIterations);
        assertEquals(expected, actual);

        totalIterations = 5;
        currentOuterIteration = 5;
        currentInnerIteration = 5;

        // Percentage: [(currentOuterIteration - 1) * totalIterations +  currentInnerIteration - 1] / totalIterations ^ 2
        expected = String.format("%s", "5.5 (96.0%)");
        actual = NetworkUtils.computeIterationNumberString(currentOuterIteration, currentInnerIteration, totalIterations);
        assertEquals(expected, actual);
    }
}