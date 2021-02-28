/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet.framework;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbstractNetworkTrainerTest {

    @Test
    void computeIterationNumberString() {
        int totalIterations = 40;
        int currentOuterIteration = 1; // Edge case
        int currentInnerIteration = 34;

        // Percentage: [(currentOuterIteration - 1) * totalIterations +  currentInnerIteration] / totalIterations ^ 2 + totalIterations
        String expected = String.format("%s", "1.34 (2.1%)");
        String actual = AbstractNetworkTrainer.computeIterationNumberString(currentOuterIteration, currentInnerIteration, totalIterations);
        assertEquals(expected, actual);

        totalIterations = 50;
        currentOuterIteration = 24;
        currentInnerIteration = 14;

        // Percentage: [(currentOuterIteration - 1) * totalIterations +  currentInnerIteration] / totalIterations ^ 2 + totalIterations
        expected = String.format("%s", "24.14 (45.6%)");
        actual = AbstractNetworkTrainer.computeIterationNumberString(currentOuterIteration, currentInnerIteration, totalIterations);
        assertEquals(expected, actual);

        totalIterations = 100;
        currentOuterIteration = 1; // Edge case
        currentInnerIteration = 1; // Edge case

        // Percentage: [(currentOuterIteration - 1) * totalIterations +  currentInnerIteration] / totalIterations ^ 2 + totalIterations
        expected = String.format("%s", "1.1 (0.0%)");
        actual = AbstractNetworkTrainer.computeIterationNumberString(currentOuterIteration, currentInnerIteration, totalIterations);
        assertEquals(expected, actual);
    }
}