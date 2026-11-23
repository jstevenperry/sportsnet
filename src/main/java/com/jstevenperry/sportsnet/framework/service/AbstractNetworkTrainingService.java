/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.service;

import com.jstevenperry.sportsnet.framework.config.AbstractSportsnetProperties;
import com.jstevenperry.sportsnet.framework.config.NetworkStructureAlgorithm;
import com.jstevenperry.sportsnet.framework.dto.NetworkDescriptorDto;
import com.jstevenperry.sportsnet.framework.exception.BadConfigurationException;
import com.jstevenperry.sportsnet.framework.exception.BadRequestException;
import com.jstevenperry.sportsnet.framework.exception.InternalServerErrorException;
import com.jstevenperry.sportsnet.framework.model.ManagementEvent;
import com.jstevenperry.sportsnet.framework.model.ManagementEventType;
import com.jstevenperry.sportsnet.framework.model.NetworkCandidate;
import com.jstevenperry.sportsnet.framework.model.NetworkDescriptor;
import com.jstevenperry.sportsnet.framework.model.ResourceType;
import com.jstevenperry.sportsnet.framework.model.SeasonData;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.framework.persistence.TournamentParticipantDao;
import com.jstevenperry.sportsnet.framework.util.AbstractNetworkProperties;
import com.jstevenperry.sportsnet.framework.util.AbstractNetworkUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public abstract class AbstractNetworkTrainingService<T extends SeasonData> extends AbstractNetworkService<T> {

    private static final Logger log = LoggerFactory.getLogger(AbstractNetworkTrainingService.class);

    public static final String LOG_PREFIX = "COREATRS";

    private final AbstractNetworkProperties networkProperties;

    public AbstractNetworkTrainingService(final SeasonDataDao<T> seasonDataDao,
                                          final TournamentParticipantDao tournamentParticipantDao,
                                          final AbstractNetworkUtils networkUtils,
                                          final AbstractSportsnetProperties sportsnetProperties,
                                          final AbstractNetworkProperties networkProperties) {
        super(seasonDataDao, tournamentParticipantDao, networkUtils, sportsnetProperties, networkProperties);
        this.networkProperties = networkProperties;
    }

    protected abstract Integer getNumberOfInputs();

    protected abstract Integer getNumberOfOutputs();


    /**
     * Return the list of years valid for training. Deduced from the season data files
     * that are available for the perticular sport suclass.
     */
    public List<Integer> getYearsForTraining() {
        return getTournamentParticipantDao().getYearsForTrainingAndEvaluation();
    }

    /**
     * Validate the parameters contained in the specified DTO and throw a RuntimeException
     * if they are not valid. A successful return from this method indicates that the
     * DTO is valid.
     * <p>
     * This method must be implemented by subclasses, which should be a tightly focused
     * service class specific to the network type serviced by that service.
     *
     * @param requestParameters The network descriptor containing the request parameters. This gives
     *                          us a chance to validate the request before we try and call toDto() on it.
     * @param isBatch           If true, special batch checks can be done.
     */
    public void validateParametersOrError(final NetworkDescriptorDto requestParameters, final boolean isBatch) {
        if (requestParameters == null) {
            String errorMessage = String.format("%s0023E: Network descriptor cannot be null", LOG_PREFIX);
            log.error("validateParametersOrError(): {}", errorMessage);
            throw new BadRequestException(errorMessage);
        }
        if (StringUtils.isEmpty(requestParameters.getNetworkType())) {
            String errorMessage = String.format("%s0025E: Network Type cannot be empty or null", LOG_PREFIX);
            log.error("validateParametersOrError(): {}", errorMessage);
            throw new BadRequestException(errorMessage);
        }
        if (isBatch) {
            if (requestParameters.getNumberOfBatchIterations() == null) {
                String errorMessage = String.format("%s0027E: Number of batch iterations cannot be null", LOG_PREFIX);
                log.error("validateParametersOrError(): {}", errorMessage);
                throw new BadRequestException(errorMessage);
            }
        }
    }

    public void validateParametersOrError(final NetworkDescriptorDto requestParameters) {
        validateParametersOrError(requestParameters, false);
    }

    /**
     * Using the parameters in the specified DTO, train a new neural network. If the network was trained
     * successfully (sometimes a network just won't train within the max number of epochs - one of the training
     * parameters - and the training run fails), then evaluate it. If the network performs above the performance
     * threshold (you guessed it, another training parameter), then save it. What that means (saved, that is)
     * is dependent on the underlying implementation, which could be to a flat file, to a relational database,
     * or something else. This framework doesn't care.
     *
     * @param networkDescriptorDto The parameters to be used to train the network. Some data will be set
     *                             (decorated) to return to the caller that tells what happened during the
     *                             training run.
     * @return TrainedNetworkParameters - a decorated copy of the original TrainedNetworkParameters object
     * that was passed in.
     */
    public Optional<NetworkDescriptor> trainAndEvaluateNetwork(final NetworkDescriptorDto networkDescriptorDto) {
        //
        // Validate the request parameters
        validateParametersOrError(networkDescriptorDto);
        //
        // Send management event
        final long startTime = System.currentTimeMillis();
        final NetworkDescriptor networkDescriptor = NetworkDescriptor.fromDto(networkDescriptorDto);
        final ManagementEvent managementEvent = createManagementEvent(networkDescriptor, ManagementEventType.TRAIN_NETWORK_START, "trainAndEvaluateNetwork()");
        Optional<NetworkDescriptor> ret;
        try {
            log.trace("trainAndEvaluateNetwork(): network parameters: {}", networkDescriptor);
            //
            // Finish filling out the descriptor
            networkDescriptor.setNumberOfOutputs(getNumberOfOutputs());
            networkDescriptor.setNumberOfInputs(getNumberOfInputs());
            networkDescriptor.setNetworkLayout(computeNetworkStructureToUse(networkDescriptor));
            //
            // Create the training data and network config
            final String trainingDataCsv = createTrainingCsvData(networkDescriptor);
            final DataSet trainingData = fetchOrCreateTrainingDataSet(trainingDataCsv, networkDescriptor);
            final MultiLayerConfiguration multiLayerConfiguration = getNetworkUtils().configureNetwork(networkDescriptor);
            //
            // Train the network
            final Optional<MultiLayerNetwork> trainedNetworkOptional = getNetworkUtils().trainNetwork(multiLayerConfiguration, networkDescriptor, trainingData);
            if (trainedNetworkOptional.isPresent()) {
                ret = evaluateNetwork(networkDescriptor, trainedNetworkOptional.get());
            } else {
                final String errorMessage = String.format("%s2212W: Network could not be trained: %s", LOG_PREFIX, networkDescriptor);
                log.warn("trainAndEvaluateNetwork(): {}", errorMessage);
                ret = Optional.empty();
            }
            managementEvent.setElapsedTimeMillis(System.currentTimeMillis() - startTime);
            createManagementEvent(managementEvent, ret.orElse(null), ManagementEventType.TRAIN_NETWORK_END, "trainAndEvaluateNetwork()");
        } catch (Exception e) {
            final String errorMessage = String.format("%s0029E: Exception occurred: %s", LOG_PREFIX, e.getLocalizedMessage());
            log.error("trainAndEvaluateNetwork(): {}", errorMessage, e);
            //
            // Create an exception event
            createManagementEvent(managementEvent, networkDescriptor, ManagementEventType.EXCEPTION, errorMessage);
            //
            // Throw exception as usual
            throw new InternalServerErrorException(errorMessage, e);
        } finally {
            //
            // We're done here. Cleanup on the way out
            cleanup();
        }
        return ret;
    }

    private Optional<NetworkDescriptor> evaluateNetwork(final NetworkDescriptor networkDescriptor,
                                                        final MultiLayerNetwork trainedNetwork) throws IOException, InterruptedException {
        Optional<NetworkDescriptor> ret;
        //
        // Evaluate the network
        String evaluationDataCsv = createEvaluationCsvData(networkDescriptor);
        DataSet evaluationData = fetchOrCreateEvaluationDataSet(evaluationDataCsv, networkDescriptor);
        Evaluation networkEvaluator = getNetworkUtils().computeNetworkEvaluator(networkDescriptor.getNumberOfOutputs(), evaluationData, trainedNetwork);
        networkDescriptor.setNetworkAccuracy(networkEvaluator.accuracy());
        networkDescriptor.setNetworkPrecision(networkEvaluator.precision());
        networkDescriptor.setNetworkRecall(networkEvaluator.recall());
        networkDescriptor.setNetworkF1Score(networkEvaluator.f1());
        networkDescriptor.setNetworkId(UUID.randomUUID());
        networkDescriptor.setWhenTrained(LocalDateTime.now());
        networkDescriptor.setConfusionMatrixCsv(networkEvaluator.getConfusionMatrix().toCSV());
        //
        // Save network if above threshold
        NetworkCandidate networkCandidate = new NetworkCandidate(networkDescriptor, trainedNetwork);
        if (getNetworkUtils().networkIsAboveThreshold(networkDescriptor)) {
            saveNetwork(networkCandidate, ResourceType.DL4J_NETWORK);
            log.info(String.format("*** Performance threshold of %3.4f%% EXCEEDED: %3.5f%%, saving network. ***",
                    networkDescriptor.getNetworkPerformanceThreshold(), networkDescriptor.getRealNetworkAccuracy() * 100));
        } else {
            //
            // Stash this network
            stashNetworkCandidate(networkCandidate);
        }
        //
        // Dump some stats
        if (networkProperties.getStatsShowEval()) {
            log.info(String.format("Evaluator stats: %s", networkEvaluator.stats()));
        }
        log.debug(networkDescriptor.toStringAbbreviated());
        log.debug("**** END OF TRAINING LOOP ({}/{}/{}) ****", networkDescriptor.getNormalizer(), networkDescriptor.getActivationFunction(), networkDescriptor.getUpdater());
        ret = Optional.of(networkDescriptor);
        return ret;
    }

    /**
     * The entry point for batch processing.
     *
     * @param networkDescriptorDto The network descriptor, contains information to kick off the batch
     *                             run.
     * @return The number of networks that were trained and saved.
     */
    @SuppressWarnings("unused")
    public Integer goBatch(final NetworkDescriptorDto networkDescriptorDto) throws IOException, InterruptedException {
        final int numberOfTrainedAndSavedNetworks;
        //
        // First thing, validate the request
        validateParametersOrError(networkDescriptorDto, true);
        final NetworkDescriptor batchRequestParameters = NetworkDescriptor.fromDto(networkDescriptorDto);
        final ManagementEvent managementEvent = getNetworkUtils().createManagementEventPayload(getServiceId());
        managementEvent.setCorrelationId(batchRequestParameters.getBatchTrainingRunId());
        createManagementEvent(managementEvent, batchRequestParameters, ManagementEventType.TRAIN_NETWORK_BATCH_START, "goBatch()");
        managementEvent.setCorrelationId(batchRequestParameters.getBatchTrainingRunId());
        try {
            //
            // Finish filling out the descriptor
            batchRequestParameters.setNumberOfOutputs(getNumberOfOutputs());
            batchRequestParameters.setNumberOfInputs(getNumberOfInputs());
            //
            // Get the number of batch iterations to use
            final int numberOfBatchIterations = batchRequestParameters.getNumberOfBatchIterations();
            //
            // Compute the training and evaluation data needed to train and evaluate networks
            final long startTime = System.currentTimeMillis();
            final String trainingDataCsv = createTrainingCsvData(batchRequestParameters);
            final String evaluationDataCsv = createEvaluationCsvData(batchRequestParameters);
            //
            // Create training and evaluation data
            final DataSet trainingData = fetchOrCreateTrainingDataSet(trainingDataCsv, batchRequestParameters);
            final DataSet evaluationData = fetchOrCreateEvaluationDataSet(evaluationDataCsv, batchRequestParameters);
            log.debug("goBatch(): Training and Evaluation data generated in {}ms", System.currentTimeMillis() - startTime);
            //
            // GO!
            numberOfTrainedAndSavedNetworks = runBatchTrainingLoop(trainingData, evaluationData, batchRequestParameters, numberOfBatchIterations, managementEvent);
        } finally {
            //
            // We're done here. Cleanup on the way out
            cleanup();
        }
        createManagementEvent(managementEvent, batchRequestParameters, ManagementEventType.TRAIN_NETWORK_BATCH_END, "goBatch()");
        //
        // Return the number of trained networks
        return numberOfTrainedAndSavedNetworks;
    }

    /**
     * This is the top-level batch training loop. Each network configuration
     * is tested starting here to try and find the optimal network settings
     * without having to try them all manually.
     */
    private int runBatchTrainingLoop(final DataSet trainingData,
                                     final DataSet evaluationData,
                                     final NetworkDescriptor batchTrainingParameters,
                                     final int numberOfTrainingIterations,
                                     final ManagementEvent managementEvent) {
        long overallStartTime = System.currentTimeMillis();
        int iterationNumber = 0;
        int numberOfNetworksSaved = 0;
        NetworkDescriptor networkDescriptor = refreshProperties(batchTrainingParameters);
        while (iterationNumber < numberOfTrainingIterations && !networkProperties.killSwitchActivated()) {
            try {
                // Set NetworkID at start of training iteration, very important!
                networkDescriptor.setNetworkId(UUID.randomUUID());
                String networkLayout = computeNetworkStructureToUse(networkDescriptor);
                networkDescriptor.setNetworkLayout(networkLayout);
                long startTime = System.currentTimeMillis();
                log.info("Iteration: {}/{}", ++iterationNumber, numberOfTrainingIterations);
                log.debug("\tUsing generator: {}, network layout: {}", networkDescriptor.getNetworkStructureAlgorithm(), networkLayout);
                log.debug("\tUsing Activation: {}", networkDescriptor.getActivationFunction());
                log.debug("\tUsing Updater: {}", networkDescriptor.getUpdater());
                log.debug("Configuring network...");
                try {
                    MultiLayerConfiguration configuration = getNetworkUtils().configureNetwork(networkDescriptor);
                    log.debug("\tUsing normalizer: {}", networkDescriptor.getNormalizer());
                    boolean networkSaved = runBatchLoopInternal(trainingData, evaluationData, networkDescriptor, iterationNumber, configuration, managementEvent);
                    if (networkSaved) {
                        numberOfNetworksSaved++;
                    }
                } catch (Exception e) {
                    String errorMessage = String.format("%s0031E: Exception occurred while training network: %s", LOG_PREFIX, e.getLocalizedMessage());
                    log.error("runBatchTrainingLoop(): {}", errorMessage, e);
                    createManagementEvent(managementEvent, networkDescriptor, ManagementEventType.EXCEPTION, errorMessage);
                    throw new InternalServerErrorException(errorMessage, e);
                }
                managementEvent.setElapsedTimeMillis(System.currentTimeMillis() - startTime);
                createManagementEvent(managementEvent, networkDescriptor, ManagementEventType.TRAIN_NETWORK_BATCH_ITERATION_END, "runBatchTrainingLoop()");
                log.debug("**** BATCH LOOP COMPLETED IN (h:m:s.SSS) {} ****", DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - startTime));
                //
                // Read latest property values (allows user to tweak the runs on-the-fly), returns
                // a copy of the descriptor that is passed in
                networkDescriptor = refreshProperties(networkDescriptor);
            } catch (BadConfigurationException e) {
                String warningMessage = String.format("%s2214W: Pausing batch loop: Bad Configuration: %s", LOG_PREFIX, e.getLocalizedMessage());
                log.warn("runBatchTrainingLoop(): {}", warningMessage);
                try {
                    // Something might be mis-configured, give the user a chance to fix it before crashing the run completely
                    //noinspection BusyWait
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    String message = String.format("%s2216W: Interrupted exception: %s", LOG_PREFIX, e.getLocalizedMessage());
                    log.warn("runBatchTrainingLoop(): {}", message);
                }
            }
        }
        log.info("**** BATCH RUN COMPLETED IN (h:m:s.SSS) {} ****", DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - overallStartTime));
        return numberOfNetworksSaved;
    }

    /**
     * Compute the network structure to be used for the current iteration using information in the
     * specified network descriptor and user-supplied network properties. The user-specified value
     * takes precedence (if supplied).
     *
     * @param networkDescriptor The network descriptor to use
     */
    private String computeNetworkStructureToUse(final NetworkDescriptor networkDescriptor) {
        if (networkDescriptor.getNetworkStructureAlgorithm() == NetworkStructureAlgorithm.USER_DEFINED ||
                networkDescriptor.getNetworkStructureAlgorithm() == NetworkStructureAlgorithm.USER_DEFINED_RAND) {
            Optional<String> userDefinedNetworkStructureOptional = networkProperties.getUserDefinedNetworkStructure();
            if (userDefinedNetworkStructureOptional.isPresent()) {
                String userDefinedNetworkStructure = userDefinedNetworkStructureOptional.get();
                networkDescriptor.setNetworkLayout(userDefinedNetworkStructure);
            } else {
                String errorMessage = String.format("%s0032E: NetworkStructureAlgorithm is USER_DEFINED, expected to find a " +
                        "corresponding user-defined network structure property value, but found none. Check the configuration!", LOG_PREFIX);
                log.error(errorMessage);
                throw new BadConfigurationException(errorMessage);
            }
        }
        return getNetworkUtils().computeNetworkStructure(networkDescriptor);
    }

    /**
     * Fetch the DataSet used for evaluating trained networks from the cache, or create it if it does
     * not exist yet (then store it in the cache).
     *
     * @param evaluationDataCsv The raw CSV season data to use to create the DataSet used
     *                          for evaluating trained networks.
     * @param networkDescriptor Network hyper parameters
     * @return The DataSet, ready to be used to evaluate trained networks
     */
    public DataSet fetchOrCreateEvaluationDataSet(final String evaluationDataCsv,
                                                  final NetworkDescriptor networkDescriptor) throws IOException, InterruptedException {
        DataSet evaluationData;
        log.info("*** CREATING {} EVALUATION DATA ***", networkProperties.getSmoothEvaluationData() ? "SMOOTHED" : "REGULAR");
        //
        // Create training and evaluation
        RecordReaderDataSetIterator evaluationDataIterator = getNetworkUtils().createIterator(evaluationDataCsv, networkDescriptor);
        evaluationData = evaluationDataIterator.next();
        //
        // Prepare the data
        DataNormalization normalizer = networkDescriptor.getNormalizer().getDataNormalization();
        getNetworkUtils().normalizeTrainingData(evaluationData, normalizer);
        return evaluationData;
    }

    /**
     * Fetch the DataSet used for training from the cache, or create it if it does
     * not exist yet (then store it in the cache).
     *
     * @param trainingDataCsv   The raw CSV season data to use to create the DataSet used
     *                          for training networks.
     * @param networkDescriptor Network hyper parameters
     * @return The DataSet, ready to be used to train networks
     */
    public DataSet fetchOrCreateTrainingDataSet(final String trainingDataCsv,
                                                final NetworkDescriptor networkDescriptor) throws IOException, InterruptedException {
        DataSet trainingData;
        log.info("*** CREATING {} TRAINING DATA ***", (networkProperties.getSmoothTrainingData()) ? "SMOOTHED" : "REGULAR");
        //
        // Create training and evaluation
        RecordReaderDataSetIterator trainingDataIterator = getNetworkUtils().createIterator(trainingDataCsv, networkDescriptor);
        trainingData = trainingDataIterator.next();
        trainingDataIterator.setCollectMetaData(true);
        //
        // Prepare the data
        DataNormalization normalizer = networkDescriptor.getNormalizer().getDataNormalization();
        getNetworkUtils().normalizeTrainingData(trainingData, normalizer);
        return trainingData;
    }

    /**
     * Use the specified Training and evaluation data to train a network. If it performs
     * above the threshold specified in networkParameters, then save and return it in the
     * Optional. Otherwise return an empty Optional.
     *
     * @param trainingData      The data set used to train the network
     * @param evaluationData    The data set used to evaluate the network to see if it's
     *                          above the threshold.
     * @param networkDescriptor Network training hyper-parameters
     * @param iterationNumber   The current training iteration (used for logging so the operator
     *                          can tell what is happening)
     * @param configuration     The network configuration to use.
     * @return Returns true if the network trained during this iteration was above the threshold
     * and saved, false otherwise.
     */
    private boolean runBatchLoopInternal(final DataSet trainingData,
                                         final DataSet evaluationData,
                                         final NetworkDescriptor networkDescriptor,
                                         final Integer iterationNumber,
                                         final MultiLayerConfiguration configuration,
                                         final ManagementEvent managementEvent) {
        boolean networkSaved = false;
        //
        // If the kill switch has been flipped since we started this loop, GTFO
        if (!networkProperties.killSwitchActivated()) {
            //
            // Train the network. If nothing goes wrong, return the trained network.
            log.debug("Training network...");
            Optional<MultiLayerNetwork> trainedNetworkOptional = goInternal(configuration, networkDescriptor, trainingData, evaluationData, iterationNumber);
            if (trainedNetworkOptional.isPresent()) {
                MultiLayerNetwork trainedNetwork = trainedNetworkOptional.get();
                //
                // Save the network if its performance is at or above the threshold.
                // If the network was not saved, retain it anyway in the networkCandidate list
                // that is owned by the caller.
                NetworkCandidate networkCandidate = new NetworkCandidate(networkDescriptor, trainedNetwork);
                networkCandidate.getTrainedNetworkParameters().setWhenTrained(LocalDateTime.now());
                if (getNetworkUtils().networkIsAboveThreshold(networkDescriptor)) {
                    networkDescriptor.setNetworkSaved(true);
                    saveNetwork(networkCandidate, ResourceType.DL4J_NETWORK);
                    //
                    // Only keep networks in memory that are above the keep threshold to avoid running out of memory for large batch runs.
                    Evaluation evaluation = getNetworkUtils().computeNetworkEvaluator(networkDescriptor.getNumberOfOutputs(), evaluationData, trainedNetwork);
                    double performanceThreshold = networkDescriptor.getNetworkPerformanceThreshold();
                    log.info(String.format("*** Iteration %d/%d: Performance threshold of %3.4f%% EXCEEDED: %3.5f%%, saving network. ***",
                            iterationNumber, networkDescriptor.getNumberOfBatchIterations(), performanceThreshold, networkDescriptor.getRealNetworkAccuracy() * 100));
                    log.info(evaluation.stats());
                    networkSaved = true;
                    //
                    // Create ManagementEvent for the save
                    createManagementEvent(managementEvent, networkDescriptor, ManagementEventType.SAVE_NETWORK, "runBatchLoopInternal()");
                }
            } else {
                log.warn("***** NETWORK COULD NOT BE TRAINED *****\n{}", networkDescriptor.toStringOneLine());
            }
        }
        return networkSaved;
    }

    /**
     * Down to business. Train and return a MultiLayerNetwork.
     */
    protected Optional<MultiLayerNetwork> goInternal(final MultiLayerConfiguration configuration,
                                                     final NetworkDescriptor networkDescriptor,
                                                     final DataSet trainingData,
                                                     final DataSet evaluationData,
                                                     final Integer iterationNumber) {
        //
        // The training loop, returns the trained network model
        Optional<MultiLayerNetwork> trainedNetwork = getNetworkUtils().trainNetwork(configuration, networkDescriptor, trainingData, iterationNumber);
        if (trainedNetwork.isPresent()) {
            //
            // Evaluate the model on the evaluation set to see how well the trained model performs against unknown data
            Evaluation networkEvaluator = getNetworkUtils().computeNetworkEvaluator(networkDescriptor.getNumberOfOutputs(), evaluationData, trainedNetwork.get());
            networkDescriptor.setNetworkAccuracy(networkEvaluator.accuracy());
            networkDescriptor.setNetworkPrecision(networkEvaluator.precision());
            networkDescriptor.setNetworkRecall(networkEvaluator.recall());
            networkDescriptor.setNetworkF1Score(networkEvaluator.f1());
            networkDescriptor.setConfusionMatrixCsv(networkEvaluator.getConfusionMatrix().toCSV());
            if (networkProperties.getStatsShowEval()) {
                log.info(String.format("Evaluator stats: %s", networkEvaluator.stats()));
                log.info(String.format("\tIteration %d/%d, %s", iterationNumber, networkDescriptor.getNumberOfBatchIterations(), networkDescriptor.toStringOneLine()));
            }
            log.debug(String.format("\tIteration %d/%d, %s", iterationNumber, networkDescriptor.getNumberOfBatchIterations(), networkDescriptor.toStringOneLine()));
            log.debug(networkDescriptor.toStringAbbreviated());
            log.debug("**** END OF TRAINING LOOP ({}/{}/{}) ****", networkDescriptor.getNormalizer(), networkDescriptor.getActivationFunction(), networkDescriptor.getUpdater());
        }
        return trainedNetwork;
    }

    /**
     * Refreshes the network descriptor from properties from the resource service.
     * Allows the user to tweak training runs on-the-fly.
     */
    private NetworkDescriptor refreshProperties(final NetworkDescriptor networkDescriptor) {
        log.info("refreshProperties(): Refreshing properties...");
        return networkDescriptor.copy()
                .setNetworkStructureAlgorithm(networkProperties.getNetworkStructureAlgorithms().get(0))
                .setActivationFunction(networkProperties.getActivationFunctions().get(0))
                .setNumberOfHiddenLayers(networkProperties.getNumberOfHiddenLayers())
                .setUpdater(networkProperties.getUpdaters().get(0))
                .setLossFunction(networkProperties.getLossFunctionChoice())
                .setWeightInit(networkProperties.getWeightInitChoice())
                .setNormalizer(networkProperties.getNormalizers().get(0))
                .setL2RegularizationLambda(networkProperties.getL2RegularizationLambda())
                .setMaxNumberOfTrainingEpochs(networkProperties.getMaxTrainingEpochs())
                .setLearningRuleLearningRate(networkProperties.getLearningRuleLearningRate())
                .setBeta1DecayScaleValue(networkProperties.getBeta1DecayScaleValue())
                .setDropoutProbability(networkProperties.getDropoutProbability())
                .setOutputLayerLossFunction(networkProperties.getOutputLayerLossFunction())
                .setOutputLayerActivation(networkProperties.getOutputLayerActivation())
                .setNetworkEvaluationIndex(networkProperties.getNetworkEvaluationIndex())
                .setMaxNetworkError(networkProperties.getMaxNetworkError())
                .setNetworkPerformanceThreshold(networkProperties.getPerformanceThreshold().doubleValue())
                .setEpochStatsIndexCount(networkDescriptor.getEpochStatsIndexCount())
                // Other properties not from properties file
                .setNetworkSaved(false)
                .setNumberOfOutputs(getNumberOfOutputs())
                .setNumberOfInputs(getNumberOfInputs())
                .setWhenTrained(null)
                .setNetworkAccuracy(null)
                .setNetworkPrecision(null)
                .setNetworkRecall(null)
                .setNetworkF1Score(null)
                .setNumberOfEpochs(null)
                .setConfusionMatrixCsv(null)
        ;

    }

}
