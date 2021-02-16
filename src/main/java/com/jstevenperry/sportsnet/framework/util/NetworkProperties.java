/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */
package com.jstevenperry.sportsnet.framework.util;

import com.jstevenperry.sportsnet.framework.menus.ActivationFunctionMenu;
import com.jstevenperry.sportsnet.framework.menus.UpdaterMenu;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * Provides access to the network properties.
 * Every property supported by this component has a default
 * value. <strong>You should only add a property under two circumstances
 * to the properties file:</strong>
 * <ol>
 * <li>The default value will not work (e.g., base.directory has a value that will not work under any circumstances,
 * which forces you to create one).</li>
 * <li>You don't like the default value and want to override it.</li>
 * </ol>
 * Other than that, putting ANYTHING in the properties file is unnecessary.
 *
 * @author J Steven Perry
 */
public class NetworkProperties {

    /**
     * The one property that is absolutely required (if externalized)
     */
    public static final String BASE_DIRECTORY = "base.directory";
    public static final String NETWORK_PROPERTIES_FILE = "network.properties.file";
    private static final String KILL_SWITCH_ACTIVATED = "kill.switch.activated";

    /**
     * Private constructor.
     */
    private NetworkProperties() {
        // Can't touch this
    }

    private static final Logger log = LoggerFactory.getLogger(NetworkProperties.class);

    /**
     * The underlying PropertiesFile object
     */
    private static PropertiesFile propertiesFile;

    /**
     * The properties file
     */
    private static final String PROPERTIES_FILE_NAME = "network.properties";

    private static long cacheExpirationTime;

    private static final long CACHE_TIMEOUT = 10000; // 10 seconds

    /**
     * Lazy-loads the underlying properties file.
     *
     * @return PropertiesFile file object.
     */
    private static PropertiesFile getPropertiesFile() {
        if (propertiesFile == null || System.currentTimeMillis() > cacheExpirationTime) {
            cacheExpirationTime = System.currentTimeMillis() + CACHE_TIMEOUT;
            String propertiesFileNameFromEnvironmentVariable = System.getProperty(NETWORK_PROPERTIES_FILE);
            if (StringUtils.isNotEmpty(propertiesFileNameFromEnvironmentVariable)) {
                log.trace(String.format("Override found for property (%s). Attempting to load properties file: %s", NETWORK_PROPERTIES_FILE, propertiesFileNameFromEnvironmentVariable));
                try (BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(propertiesFileNameFromEnvironmentVariable))) {
                    Properties properties = new Properties();
                    properties.load(bufferedInputStream);
                    propertiesFile = new PropertiesFile(properties);
                    log.trace(String.format("Base directory: '%s' (doesn't mean everything is okay, but is a good sign).", propertiesFile.getProperty(BASE_DIRECTORY)));
                } catch (IOException e) {
                    String errorMessage = String.format("Error attempting to read file '%s'", propertiesFileNameFromEnvironmentVariable);
                    log.error(errorMessage, e);
                    throw new RuntimeException(errorMessage, e);
                }
                log.debug(String.format("Loaded properties file: %s", propertiesFileNameFromEnvironmentVariable));
            } else {
                log.warn("System property {} not set, attempting to load network properties file: {} from the classpath...", NETWORK_PROPERTIES_FILE, PROPERTIES_FILE_NAME);
                propertiesFile = new PropertiesFile(PROPERTIES_FILE_NAME);
                log.info("Loaded properties file from CLASSPATH (the default behavior)");
            }
        }
        // Sanity check
        String baseDirectory = propertiesFile.getProperty(BASE_DIRECTORY);
        if (baseDirectory.equalsIgnoreCase("REPLACE_WITH_YOUR_VALUE")) {
            String warningMessage = String.format("You should specify a value for %s. The default value of '.' will be used.", BASE_DIRECTORY);
            log.warn(warningMessage);
        }
        return propertiesFile;
    }

    /**
     * Kill switch - by setting the KILL_SWITCH_ACTIVATED property to true, it kills
     * the training loop at the bottom of the current epoch. Most of the time this
     * appears to be instantaneous.
     */
    public static boolean killSwitchActivated() {
        return getBooleanPropertyValue(KILL_SWITCH_ACTIVATED);
    }

    public static List<ActivationFunctionMenu.ActivationFunctionChoice> getActivationFunctions() {
        List<ActivationFunctionMenu.ActivationFunctionChoice> ret = new ArrayList<>();
        String activationFunctionsCsv = getStringPropertyValue("activation.functions", null);
        if (StringUtils.isNotEmpty(activationFunctionsCsv)) {
            StringTokenizer strtok = new StringTokenizer(activationFunctionsCsv, ",;:");
            while (strtok.hasMoreElements()) {
                String token = strtok.nextToken();
                try {
                    ActivationFunctionMenu.ActivationFunctionChoice activationFunctionChoice = ActivationFunctionMenu.ActivationFunctionChoice.valueOf(token);
                    ret.add(activationFunctionChoice);
                } catch (IllegalArgumentException e) {
                    String errorMessage = String.format("No ActivationFunctionChoice enum member called %s", token);
                    log.error("getActivationFunctions(): {}", errorMessage, e);
                    throw new RuntimeException(errorMessage, e);
                }
            }
        }
        return ret;
    }

    /**
     * The base directory. Used a LOT by the application. You MUST PROVIDE A
     * VALUE IN THE PROPERTIES FILE FOR THIS PROPERTY!
     * <p>
     * This is the ONLY property like this.
     *
     * @return String The Base directory property value.
     */
    public static String getBaseDirectory() {
        return getStringPropertyValue(BASE_DIRECTORY, ".");
    }

    public static Double getDropoutProbability() {
        return getDoublePropertyValue("dropout.probability", 0.25);
    }

    public static Integer getEpochStatsIndexCount() {
        return getIntegerPropertyValue("epoch.stats.index.count", 100);
    }

    public static Integer getFudgeFactor() {
        return getIntegerPropertyValue("fudge.factor", 2);
    }

    public static Double getLearningRuleLearningRate() {
        return getDoublePropertyValue("learning.rule.learning.rate", 1.0);
    }

    public static Double getLearningRuleMomentumMax() {
        return getDoublePropertyValue("learning.rule.momentum.max", 0.25);
    }

    public static Integer getMaxLearningIterations() {
        return getIntegerPropertyValue("max.learning.iterations", 20000);
    }

    public static Double getMaxNetworkError() {
        return getDoublePropertyValue("max.network.error", 0.01); // 1%
    }

    public static Integer getMaxNetworkIterations() {
        return getIntegerPropertyValue("max.network.iterations", 10);
    }

    public static String getNetworkDirectoryName() {
        return getStringPropertyValue("network.directory.name", "Networks");
    }

    public static Integer getNetworkEvaluationIndex() {
        return getIntegerPropertyValue("network.evaluation.index", 10);
    }

    public static String getNetworkLayout(final String sport) {
        return getStringPropertyValue(sport + ".network.layout", StringUtils.EMPTY);
    }

    public static Integer getNumberOfHiddenLayers() {
        return getIntegerPropertyValue("number.of.hidden.layers", 7);
    }

    public static BigDecimal getPerformanceThreshold() {
        Integer performanceThreshold = getIntegerPropertyValue("performance.threshold", 70);
        return BigDecimal.valueOf(performanceThreshold).setScale(0, RoundingMode.HALF_UP);
    }

    public static BigDecimal getPerformanceThresholdNetworkKeep() {
        Integer performanceThreshold = getIntegerPropertyValue("performance.threshold.network.keep", 40);
        return BigDecimal.valueOf(performanceThreshold).setScale(0, RoundingMode.HALF_UP);
    }

    public static Integer getPrimeDivisor() {
        return getIntegerPropertyValue("prime.divisor", 3);
    }

    public static Integer getPrimeSeed() {
        return getIntegerPropertyValue("prime.seed", 31);
    }

    public static String getSimulationDirectoryName() {
        return getStringPropertyValue("simulation.directory.name", "Simulation");
    }

    public static List<UpdaterMenu.UpdaterChoice> getUpdaters() {
        List<UpdaterMenu.UpdaterChoice> ret = new ArrayList<>();
        String updaters = getStringPropertyValue("updaters", null);
        if (StringUtils.isNotEmpty(updaters)) {
            StringTokenizer strtok = new StringTokenizer(updaters, ",;:");
            while (strtok.hasMoreElements()) {
                String token = strtok.nextToken();
                try {
                    UpdaterMenu.UpdaterChoice updaterChoice = UpdaterMenu.UpdaterChoice.valueOf(token);
                    ret.add(updaterChoice);
                } catch (IllegalArgumentException e) {
                    String errorMessage = String.format("No UpdaterChoice enum member called %s", token);
                    log.error("getUpdaters(): {}", errorMessage, e);
                    throw new RuntimeException(errorMessage, e);
                }
            }
        }
        return ret;
    }

    // ****************************
    // * HELPER METHODS - PRIVATE *
    // ****************************

    /**
     * Returns the value found in the underlying properties file, converted
     * to a Boolean, or the specified defaultValue if the property value
     * could not be found.
     *
     * @param propertyName The name of the property.
     * @throws RuntimeException If the <code>defaultValue</code> is null, then the
     *                          property is considered to be <em>required</em> and if it cannot be located in the
     *                          properties file, this exception is thrown.
     */
    private static Boolean getBooleanPropertyValue(String propertyName) {
        boolean ret = false;
        String propertyValue = getPropertiesFile().getProperty(propertyName);
        if (propertyValue != null) {
            ret = Boolean.parseBoolean(propertyValue);
        }
        return ret;
    }

    /**
     * Returns the value found in the underlying properties file, converted
     * to a Double, or the specified defaultValue if the property value
     * could not be found.
     *
     * @param propertyName The name of the property.
     * @param defaultValue If the defaultValue is <code>null</code>, then the property is
     *                     considered to be <em>required</em> and if not found, a <code>RuntimeException</code>
     *                     is thrown.
     * @return Double - the value of the specified property, converted to a
     * Double.
     * @throws NumberFormatException If the property value cannot be parsed, or is not assignable
     *                               to a double precision value
     * @throws RuntimeException      If the <code>defaultValue</code> is null, then the
     *                               property is considered to be <em>required</em> and if it cannot be located in the
     *                               properties file, this exception is thrown.
     */
    private static Double getDoublePropertyValue(String propertyName, Double defaultValue) {
        Double ret = defaultValue;
        String propertyValue = getPropertiesFile().getProperty(propertyName);
        if (propertyValue != null) {
            ret = Double.valueOf(propertyValue);
        }
        if (ret == null) {
            throw new RuntimeException(
                    "You must specify a value for property '" + propertyName + "' in " + PROPERTIES_FILE_NAME + "!");
        }
        return ret;
    }

    /**
     * Returns the value found in the underlying properties file, converted
     * to an Integer, or the specified defaultValue if the property value
     * could not be found.
     *
     * @param propertyName The name of the property.
     * @param defaultValue If the defaultValue is <code>null</code>, then the property is
     *                     considered to be <em>required</em> and if not found, a <code>RuntimeException</code>
     *                     is thrown.
     * @return Integer - the value of the specified property, converted to an
     * integer.
     * @throws NumberFormatException If the property value cannot be parsed, or is not assignable
     *                               to an integer value
     * @throws RuntimeException      If the <code>defaultValue</code> is null, then the
     *                               property is considered to be <em>required</em> and if it cannot be located in the
     *                               properties file, this exception is thrown.
     */
    private static Integer getIntegerPropertyValue(String propertyName, Integer defaultValue) {
        Integer ret = defaultValue;
        String propertyValue = getPropertiesFile().getProperty(propertyName);
        if (propertyValue != null) {
            ret = Integer.valueOf(propertyValue);
        }
        if (ret == null) {
            throw new RuntimeException(
                    "You must specify a value for property '" + propertyName + "' in " + PROPERTIES_FILE_NAME + "!");
        }
        return ret;
    }

    /**
     * Returns the value found in the underlying properties file,
     * or the specified <code>defaultValue</code> if the property value
     * could not be found
     * (and <code>defaultvalue</code> is not null, in which
     * case the property is <em>required</em>).
     *
     * @param propertyName The name of the property.
     * @param defaultValue If the defaultValue is <code>null</code>, then the property is
     *                     considered to be <em>required</em> and if not found, a <code>RuntimeException</code>
     *                     is thrown.
     * @return String - The value of the specified property.
     * @throws RuntimeException If the <code>defaultValue</code> is null, then the
     *                          property is considered to be <em>required</em> and if it cannot be located in the
     *                          properties file, this exception is thrown.
     */
    private static String getStringPropertyValue(String propertyName, String defaultValue) {
        String ret = defaultValue;
        String propertyValue = getPropertiesFile().getProperty(propertyName);
        if (propertyValue != null) {
            ret = propertyValue;
        }
        if (ret == null) {
            throw new RuntimeException(
                    "You must specify a value for property '" + propertyName + "' in " + PROPERTIES_FILE_NAME + "!");
        }
        return ret;
    }

}
