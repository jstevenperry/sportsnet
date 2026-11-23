/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */
package com.jstevenperry.sportsnet.framework.util;

import com.jstevenperry.sportsnet.framework.config.AbstractSportsnetProperties;
import com.jstevenperry.sportsnet.framework.config.NetworkStructureAlgorithm;
import com.jstevenperry.sportsnet.framework.dto.ResourceFileWrapperDto;
import com.jstevenperry.sportsnet.framework.exception.InternalServerErrorException;
import com.jstevenperry.sportsnet.framework.exception.NotFoundException;
import com.jstevenperry.sportsnet.framework.model.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

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
public abstract class AbstractNetworkProperties {

    public static final String LOG_PREFIX = "COREANP";

    private final Logger log = LoggerFactory.getLogger(AbstractNetworkProperties.class);

    /**
     * The properties file. As of sportsnet core 2.3.0 this lives on the resource server!
     */
    public static final String PROPERTIES_FILE_NAME = "network.properties";

    private final AbstractNetworkUtils networkUtils;
    private final AbstractSportsnetProperties sportsnetProperties;

    /**
     * Returns the name of the implementing sport. Used to access the base properties
     * by sport. For example, if you want to activate the kill switch to stop a batch
     * run for, say, NBA, but you don't want to kill all of the other batch runs, you
     * would set nba.kill.switch.activated=true to signal to the NBA batch training
     * loop to stop.
     *
     * @return The name of the sport such as nfl, nba, ncaabb, etc. Basically, the value
     * of this attribute must match the sport-specific URI for that sport.
     */
    protected abstract String getSport();

    /**
     * Constructor. Subclasses must inject dependencies through this constructor.
     *
     * @param networkUtils        Network utilities bean.
     * @param sportsnetProperties SportsnetProperties bean. Every sport has one, and
     *                            they're all a little different.
     */
    public AbstractNetworkProperties(final AbstractNetworkUtils networkUtils,
                                     final AbstractSportsnetProperties sportsnetProperties) {
        this.networkUtils = networkUtils;
        this.sportsnetProperties = sportsnetProperties;
    }

    /**
     * The underlying PropertiesFile object
     */
    private PropertiesFile propertiesFile;

    /**
     * Cache timeout cannot be set to less than 5000ms
     */
    public static final int MINIMUM_CACHE_TIMEOUT_VALUE = 5000;

    /**
     * The current value of the expiration time of the properties file,
     * when the system time exceeds this value, the properties file is refreshed.
     */
    private long propertiesFileExpirationTime;

    /**
     * The current number of times the system will try and refresh the properties file
     * before giving up and throwing an exception (which may crash the hosted service).
     */
    private int propertiesFileRefreshRetryCount = 0;

    /**
     * Lazy-loads the underlying properties file.
     *
     * @return PropertiesFile file object.
     */
    private PropertiesFile getPropertiesFile() {
        if (propertiesFile == null || System.currentTimeMillis() > propertiesFileExpirationTime) {
            long startTime = System.currentTimeMillis();
            log.debug("getPropertiesFile(): Loading properties file from resource server...");
            final String propertiesFileUrl = networkUtils.computeDataFileUrl(sportsnetProperties, PROPERTIES_FILE_NAME, ResourceType.PROPERTIES_FILE);
            Optional<ResourceFileWrapperDto> propertiesFileContentsOptional = networkUtils.fetchDataFileFromResourceServer(propertiesFileUrl);
            if (propertiesFileContentsOptional.isPresent()) {
                String propertiesFileContents = propertiesFileContentsOptional.get()
                        .getFileContentsAsString();
                try (BufferedInputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(propertiesFileContents.getBytes()))) {
                    Properties properties = new Properties();
                    properties.load(inputStream);
                    propertiesFile = new PropertiesFile(properties);
                    int cacheTimeout = Integer.parseInt(propertiesFile.getProperty("cache.timeout.value"));
                    if (cacheTimeout < MINIMUM_CACHE_TIMEOUT_VALUE) {
                        log.warn("getPropertiesFile(): Current cache timeout value of {}ms needs to be increased above {}ms, defaulting to {}ms ",
                                cacheTimeout, MINIMUM_CACHE_TIMEOUT_VALUE, MINIMUM_CACHE_TIMEOUT_VALUE);
                        cacheTimeout = MINIMUM_CACHE_TIMEOUT_VALUE;
                    }
                    propertiesFileExpirationTime = System.currentTimeMillis() + cacheTimeout;
                    //
                    // Reset or at some point this will go very badly
                    propertiesFileRefreshRetryCount = 0;
                } catch (Exception e) {
                    String errorMessage = String.format("%s0057E: Error attempting to read file '%s'", LOG_PREFIX, PROPERTIES_FILE_NAME);
                    log.error(errorMessage, e);
                    // After 10 tries, something is wrong, throw an exception and bail
                    if (propertiesFileRefreshRetryCount++ > 10) {
                        throw new InternalServerErrorException(errorMessage, e);
                    }
                }
            } else {
                String errorMessage = String.format("%s0059E: Properties file '%s' not found", LOG_PREFIX, PROPERTIES_FILE_NAME);
                log.error(errorMessage);
                throw new NotFoundException(errorMessage);
            }
            log.debug(String.format("getPropertiesFile(): Loaded properties file: %s in %dms", PROPERTIES_FILE_NAME, System.currentTimeMillis() - startTime));
        }
        return propertiesFile;
    }

    /**
     * Kill switch - by setting the KILL_SWITCH_ACTIVATED property to true, it kills
     * the training loop at the bottom of the current epoch. Most of the time this
     * appears to be instantaneous.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean killSwitchActivated() {
        return getBooleanPropertyValue("kill.switch.activated", false);
    }

    public List<ActivationFunctionChoice> getActivationFunctions() {
        List<ActivationFunctionChoice> ret = new ArrayList<>();
        String activationFunctionsCsv = getStringPropertyValue("activation.functions", null);
        if (StringUtils.isNotEmpty(activationFunctionsCsv)) {
            StringTokenizer strtok = new StringTokenizer(activationFunctionsCsv, ",;:");
            while (strtok.hasMoreElements()) {
                String token = strtok.nextToken();
                try {
                    ActivationFunctionChoice activationFunctionChoice = ActivationFunctionChoice.valueOf(token);
                    ret.add(activationFunctionChoice);
                } catch (IllegalArgumentException e) {
                    String errorMessage = String.format("%s0063E: No ActivationFunctionChoice enum member called %s", LOG_PREFIX, token);
                    log.error("getActivationFunctions(): {}", errorMessage, e);
                    throw new InternalServerErrorException(errorMessage, e);
                }
            }
        }
        return ret;
    }

    public Double getBeta1DecayScaleValue() {
        return getDoublePropertyValue("beta.1.decay.scale.value", 0.9);
    }

    public Double getBeta2DecayScaleValue() {
        return getDoublePropertyValue("beta.2.decay.scale.value", 0.999);
    }

    public Double getDropoutProbability() {
        return getDoublePropertyValue("dropout.probability", 0.95);
    }

    public Integer getEpochStatsIndexCount() {
        return getIntegerPropertyValue("epoch.stats.index.count", 100);
    }

    public Double getLearningRuleLearningRate() {
        return getDoublePropertyValue("learning.rule.learning.rate", 1.0);
    }

    public Double getL2RegularizationLambda() {
        return getDoublePropertyValue("l2.regularization.lambda", 0.0005);
    }

    public Double getLearningRuleMomentumMax() {
        return getDoublePropertyValue("learning.rule.momentum.max", 0.25);
    }

    public LossFunctionChoice getLossFunctionChoice() {
        String value = getStringPropertyValue("loss.function.choice", "MCXENT");
        return LossFunctionChoice.valueOf(value);
    }

    public Integer getMaxTrainingEpochs() {
        return getIntegerPropertyValue("max.training.epochs", 1000);
    }

    public Double getMaxNetworkError() {
        return getDoublePropertyValue("max.network.error", 0.01); // 1%
    }

    public Integer getNumberOfBatchIterations() {
        return getIntegerPropertyValue("number.batch.iterations", 10);
    }

    public Integer getNetworkEvaluationIndex() {
        return getIntegerPropertyValue("network.evaluation.index", 10);
    }

    public Boolean getStatsShowEval() {
        return getBooleanPropertyValue("stats.show.eval", false);
    }

    public List<NetworkStructureAlgorithm> getNetworkStructureAlgorithms() {
        List<NetworkStructureAlgorithm> ret = new ArrayList<>();
        String updaters = getStringPropertyValue("networkStructureAlgorithms", null);
        if (StringUtils.isNotEmpty(updaters)) {
            StringTokenizer strtok = new StringTokenizer(updaters, ",;:");
            while (strtok.hasMoreElements()) {
                String token = strtok.nextToken();
                try {
                    NetworkStructureAlgorithm dataNormalizationChoice = NetworkStructureAlgorithm.valueOf(token);
                    ret.add(dataNormalizationChoice);
                } catch (IllegalArgumentException e) {
                    String errorMessage = String.format("%s0065E: No NetworkStructureAlgorithm enum member called %s", LOG_PREFIX, token);
                    log.error("getNetworkStructureAlgorithms(): {}", errorMessage, e);
                    throw new RuntimeException(errorMessage, e);
                }
            }
        }
        return ret;
    }

    public List<DataNormalizationChoice> getNormalizers() {
        List<DataNormalizationChoice> ret = new ArrayList<>();
        String updaters = getStringPropertyValue("normalizers", null);
        if (StringUtils.isNotEmpty(updaters)) {
            StringTokenizer strtok = new StringTokenizer(updaters, ",;:");
            while (strtok.hasMoreElements()) {
                String token = strtok.nextToken();
                try {
                    DataNormalizationChoice dataNormalizationChoice = DataNormalizationChoice.valueOf(token);
                    ret.add(dataNormalizationChoice);
                } catch (IllegalArgumentException e) {
                    String errorMessage = String.format("%s0067E: No DataNormalizationChoice enum member called %s", LOG_PREFIX, token);
                    log.error("getNormalizers(): {}", errorMessage, e);
                    throw new RuntimeException(errorMessage, e);
                }
            }
        }
        return ret;
    }

    public boolean getNormalizeAcrossAllSeasonData() {
        return getBooleanPropertyValue("normalize.across.all.season.data", true);
    }

    public Integer getNumberOfHiddenLayers() {
        return getIntegerPropertyValue("number.of.hidden.layers", 7);
    }

    public ActivationFunctionChoice getOutputLayerActivation() {
        String propertyValue = getStringPropertyValue("output.layer.activation", ActivationFunctionChoice.SOFTMAX.name());
        return ActivationFunctionChoice.valueOf(propertyValue);
    }

    public LossFunctionChoice getOutputLayerLossFunction() {
        String propertyValue = getStringPropertyValue("output.layer.loss.function", LossFunctionChoice.NEGATIVELOGLIKELIHOOD.name());
        return LossFunctionChoice.valueOf(propertyValue);
    }

    public BigDecimal getPerformanceThreshold() {
        Integer performanceThreshold = getIntegerPropertyValue("performance.threshold", 70);
        return BigDecimal.valueOf(performanceThreshold)
                .setScale(0, RoundingMode.HALF_UP);
    }

    public List<UpdaterChoice> getUpdaters() {
        List<UpdaterChoice> ret = new ArrayList<>();
        String updaters = getStringPropertyValue("updaters", null);
        if (StringUtils.isNotEmpty(updaters)) {
            StringTokenizer strtok = new StringTokenizer(updaters, ",;:");
            while (strtok.hasMoreElements()) {
                String token = strtok.nextToken();
                try {
                    UpdaterChoice updaterChoice = UpdaterChoice.valueOf(token);
                    ret.add(updaterChoice);
                } catch (IllegalArgumentException e) {
                    String errorMessage = String.format("%s0069E: No UpdaterChoice enum member called %s", LOG_PREFIX, token);
                    log.error("getUpdaters(): {}", errorMessage, e);
                    throw new RuntimeException(errorMessage, e);
                }
            }
        }
        return ret;
    }

    public Boolean getUseAllSeasonDataForTournament() {
        return getBooleanPropertyValue("use.all.season.data.for.tournament", true);
    }

    public WeightInitChoice getWeightInitChoice() {
        String value = getStringPropertyValue("weight.init.choice", "XAVIER_UNIFORM");
        return WeightInitChoice.valueOf(value);
    }

    public long getSimpleCacheTtl() {
        return getIntegerPropertyValue("simple.cache.ttl.minutes", 1440);
    }

    public Optional<String> getUserDefinedNetworkStructure() {
        final String propertyValue = getStringPropertyValue("user.defined.network.structure", StringUtils.EMPTY);
        if (StringUtils.isEmpty(propertyValue)) {
            return Optional.empty();
        }
        return Optional.of(propertyValue);
    }

    public boolean getSmoothTrainingData() {
        return getBooleanPropertyValue("smooth.training.data", false);
    }

    public boolean getSmoothEvaluationData() {
        return getBooleanPropertyValue("smooth.evaluation.data", false);
    }

    public boolean allowEvaluationYearsForPrediction() {
        return getBooleanPropertyValue("allow.evaluation.years.for.prediction", false);
    }

    public int getTournamentDistanceFactor() {
        // This number is added to the index as each bucket in the output INDArray is read to compute
        // the tournament distance score. Default: 0, so that zero scores are thrown out completely
        return getIntegerPropertyValue("tournament.distance.factor", 0);
    }
    // ****************************
    // * HELPER METHODS - PRIVATE *
    // ****************************

    /**
     * Computes the sport-specific property name for the specified
     * property name. If the base property name is:
     * <p>
     * number.batch.iterations
     * <p>
     * The sport-specific property name for NBA would be:
     * <p>
     * nba.number.batch.iterations
     *
     * @param sport            The name of the sport, matches the sport-specific URI that
     *                         every sport must implement in its SportsnetProperties class
     * @param basePropertyName The base name of the property
     * @return The sport-specific property name
     */
    private String computeSportSpecificPropertyName(final String sport,
                                                    final String basePropertyName) {
        if (StringUtils.isEmpty(sport)) {
            String errorMessage = String.format("%s2220W: Sport is null or empty!", LOG_PREFIX);
            log.warn("computeSportSpecificPropertyName({},{}): {}", sport, basePropertyName, errorMessage);
        }
        if (StringUtils.isEmpty(basePropertyName)) {
            String errorMessage = String.format("%s0073E: Base property name is null or empty!", LOG_PREFIX);
            log.error("computeSportSpecificPropertyName({},{}): {}", sport, basePropertyName, errorMessage);
            throw new InternalServerErrorException(errorMessage);
        }
        //
        // Compute the sport-specific property name, sport is high-order qualifier.
        return sport + "." + basePropertyName;
    }

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
    protected Boolean getBooleanPropertyValue(String propertyName,
                                              boolean defaultValue) {
        boolean ret = defaultValue;
        String sportSpecificPropertyName = computeSportSpecificPropertyName(getSport(), propertyName);
        String propertyValue = getPropertiesFile().getProperty(sportSpecificPropertyName);
        if (propertyValue != null) {
            ret = Boolean.parseBoolean(propertyValue);
            log.trace(String.format("%s=%s", sportSpecificPropertyName, propertyValue));
        } else {
            propertyValue = getPropertiesFile().getProperty(propertyName);
            if (propertyValue != null) {
                ret = Boolean.parseBoolean(propertyValue);
                log.trace(String.format("%s=%s", propertyName, propertyValue));
            }
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
    protected Double getDoublePropertyValue(String propertyName,
                                            Double defaultValue) {
        Double ret = defaultValue;
        String sportSpecificPropertyName = computeSportSpecificPropertyName(getSport(), propertyName);
        String propertyValue = getPropertiesFile().getProperty(sportSpecificPropertyName);
        if (propertyValue != null) {
            ret = Double.valueOf(propertyValue);
            log.trace(String.format("%s=%s", sportSpecificPropertyName, propertyValue));
        } else {
            propertyValue = getPropertiesFile().getProperty(propertyName);
            if (propertyValue != null) {
                ret = Double.valueOf(propertyValue);
                log.trace(String.format("%s=%s", propertyName, propertyValue));
            }
        }
        if (ret == null) {
            String errorMessage = String.format("%s0075E: You must specify a value for property '" + propertyName + "' in " + PROPERTIES_FILE_NAME + "!", LOG_PREFIX);
            log.error("getDoublePropertyValue({},{}): {}", propertyName, defaultValue, errorMessage);
            throw new RuntimeException(errorMessage);
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
    protected Integer getIntegerPropertyValue(String propertyName,
                                              Integer defaultValue) {
        Integer ret = defaultValue;
        String sportSpecificPropertyName = computeSportSpecificPropertyName(getSport(), propertyName);
        String propertyValue = getPropertiesFile().getProperty(sportSpecificPropertyName);
        if (propertyValue != null) {
            ret = Integer.valueOf(propertyValue);
            log.trace(String.format("%s=%s", sportSpecificPropertyName, propertyValue));
        } else {
            propertyValue = getPropertiesFile().getProperty(propertyName);
            if (propertyValue != null) {
                ret = Integer.valueOf(propertyValue);
                log.trace(String.format("%s=%s", propertyName, propertyValue));
            }
        }
        if (ret == null) {
            String errorMessage = String.format("%s0077E: You must specify a value for property '" + propertyName + "' in " + PROPERTIES_FILE_NAME + "!", LOG_PREFIX);
            log.error("getDoublePropertyValue({},{}): {}", propertyName, defaultValue, errorMessage);
            throw new RuntimeException(errorMessage);
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
    protected String getStringPropertyValue(String propertyName,
                                            String defaultValue) {
        String ret = defaultValue;
        String sportSpecificPropertyName = computeSportSpecificPropertyName(getSport(), propertyName);
        String propertyValue = getPropertiesFile().getProperty(sportSpecificPropertyName);
        if (propertyValue != null) {
            ret = propertyValue;
            log.trace(String.format("%s=%s", sportSpecificPropertyName, propertyValue));
        } else {
            propertyValue = getPropertiesFile().getProperty(propertyName);
            if (propertyValue != null) {
                ret = propertyValue;
                log.trace(String.format("%s=%s", propertyName, propertyValue));
            }
        }
        if (ret == null) {
            String errorMessage = String.format("%s0079E: You must specify a value for property '" + propertyName + "' in " + PROPERTIES_FILE_NAME + "!", LOG_PREFIX);
            log.error("getDoublePropertyValue({},{}): {}", propertyName, defaultValue, errorMessage);
            throw new RuntimeException(errorMessage);
        }
        return ret;
    }

}
