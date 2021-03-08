/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet.framework;

import com.jstevenperry.sportsnet.framework.menus.AbstractDataElementChoice;
import com.jstevenperry.sportsnet.framework.menus.ActivationFunctionMenu;
import com.jstevenperry.sportsnet.framework.menus.DataNormalizationMenu;
import com.jstevenperry.sportsnet.framework.menus.UpdaterMenu;
import com.jstevenperry.sportsnet.framework.model.AbstractSeasonData;
import com.jstevenperry.sportsnet.framework.model.NetworkCandidate;
import com.jstevenperry.sportsnet.framework.model.NetworkParameters;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.framework.persistence.TournamentParticipantDao;
import com.jstevenperry.sportsnet.framework.persistence.TournamentResultDao;
import com.jstevenperry.sportsnet.framework.util.NetworkProperties;
import com.jstevenperry.sportsnet.framework.util.NetworkTrainerParameterScanner;
import com.jstevenperry.sportsnet.framework.util.NetworkUtils;
import org.apache.commons.lang3.StringUtils;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.learning.config.IUpdater;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractNetworkTrainer<T extends AbstractSeasonData> extends AbstractNetworkBase<T> {

    private static final Logger log = LoggerFactory.getLogger(AbstractNetworkTrainer.class);

    // My "class variables of shame", but sometimes you need a little optimization (and place to put it)
    private final Map<DataNormalizationMenu.DataNormalizationChoice, DataSet> trainingDataSetMap = new HashMap<>();
    private final Map<DataNormalizationMenu.DataNormalizationChoice, DataSet> evaluationDataSetMap = new HashMap<>();

    /**
     * Get the data elements the user wants to use to train the network.
     * Only a subclass knows the specific elements for sure, since this
     * varies by sport.
     * <p>
     * This method must be implemented by a sports-specific subclass
     * that knows all of the data elements that are available for that
     * sport.
     *
     * @param scanner The BufferedReader owned by the main menu
     * @param choices The list of previous choices. If the list is empty, this is the
     *                first time through. Otherwise, let the user easily choose the
     *                same elements they chose last time (by pressing enter
     *                for example).
     * @return The list of data elements to use.
     * @throws IOException If something goes horribly wrong.
     */
    protected abstract List<AbstractDataElementChoice> scanDataElementChoice(final BufferedReader scanner,
                                                                             final List<AbstractDataElementChoice> choices) throws IOException;// {

    public Optional<NetworkCandidate> go(final NetworkParameters networkParametersGlobal,
                                         final SeasonDataDao<T> seasonDataDao,
                                         final TournamentResultDao tournamentResultDao,
                                         final TournamentParticipantDao tournamentParticipantDao,
                                         final Integer numberOfOutputClasses,
                                         final String networkType) throws IOException, InterruptedException {
        NetworkParameters networkParameters = (networkParametersGlobal == null) ? new NetworkParameters() : networkParametersGlobal.copy();
        networkParameters.setNetworkType(networkType);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        //
        // Fetch the network configuration parameters (reuse NetworkParameters so user doesn't have to re-enter everything each time)
        promptUserForNetworkParameters(bufferedReader, networkParameters, tournamentParticipantDao, numberOfOutputClasses, false);
        log.debug("Network parameters acquired.");
        //
        // Get the number of epochs
        Integer numberOfEpochs = NetworkTrainerParameterScanner.scanNumberOfEpochs(bufferedReader, NetworkProperties.getMaxNetworkIterations());
        //
        // Compute the training and evaluation data needed to train and evaluate networks
        log.debug("Pulling training and evaluation data...");
        String trainingDataCsv = computeTrainingDataCsv(networkParameters.getYearsToTrainNetwork(), seasonDataDao, tournamentResultDao, tournamentParticipantDao, networkParameters);
        String evaluationDataCsv = computeEvaluationDataCsv(networkParameters.getYearsToEvaluateNetwork(), seasonDataDao, tournamentResultDao, tournamentParticipantDao, networkParameters);
        RecordReaderDataSetIterator trainingDataIterator = NetworkUtils.createIterator(trainingDataCsv, networkParameters);
        RecordReaderDataSetIterator evaluationDataIterator = NetworkUtils.createIterator(evaluationDataCsv, networkParameters);
        //
        // Create the network definition
        log.debug("Configuring network...");
        MultiLayerConfiguration configuration = NetworkUtils.configureNetwork(networkParameters);
        //
        // Train the network. If nothing goes wrong, return the trained network.
        log.debug("Training network...");
        MultiLayerNetwork trainedNetwork = goInternal(configuration, networkParameters, trainingDataIterator.next(), evaluationDataIterator.next(), "1.1");
        // Restore the value for next time since it will be set to the actual number used, and the user may not want that next time
        networkParameters.setNumberOfEpochs(numberOfEpochs);
        log.debug("Training network...DONE.");
        //
        // Return the trained network
        NetworkCandidate networkCandidate = new NetworkCandidate(networkParameters.copy(), trainedNetwork);
        return Optional.of(networkCandidate);
    }

    public NetworkParameters goBatch(final NetworkParameters networkParametersGlobal,
                                     final List<NetworkCandidate> networkCandidateList,
                                     final SeasonDataDao<T> seasonDataDao,
                                     final TournamentResultDao tournamentResultDao,
                                     final TournamentParticipantDao tournamentParticipantDao,
                                     final Integer numberOfOutputClasses,
                                     final String networkType) throws IOException {
        NetworkParameters networkParameters = (networkParametersGlobal == null) ? new NetworkParameters() : networkParametersGlobal;
        if (NetworkUtils.keepGoing()) {
            networkParameters.setNetworkType(networkType);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            //
            // Get the number of batch iterations to use
            int numberOfTrainingIterations = NetworkProperties.getMaxNetworkIterations();
            //
            // Fetch the network configuration parameters (reuse NetworkParameters so user doesn't have to re-enter everything each time)
            promptUserForNetworkParameters(bufferedReader, networkParameters, tournamentParticipantDao, numberOfOutputClasses);
            //
            // Generate network structure
            log.debug("Network parameters acquired.");
            //
            // Compute the training and evaluation data needed to train and evaluate networks
            long startTime = System.currentTimeMillis();
            String trainingDataCsv = computeTrainingDataCsv(networkParameters.getYearsToTrainNetwork(), seasonDataDao, tournamentResultDao, tournamentParticipantDao, networkParameters);
            String evaluationDataCsv = computeEvaluationDataCsv(networkParameters.getYearsToEvaluateNetwork(), seasonDataDao, tournamentResultDao, tournamentParticipantDao, networkParameters);
            log.info("goBatch(): Training and Evaluation data generated in {}ms", System.currentTimeMillis() - startTime);
            //
            // GO!
            runBatchTrainingLoop(networkCandidateList, trainingDataCsv, evaluationDataCsv, networkParameters, numberOfTrainingIterations);
            NetworkUtils.displayNetworkList(networkCandidateList);
            //
            // Clean out maps
            evaluationDataSetMap.clear();
            trainingDataSetMap.clear();
        }
        return networkParameters;
    }

    /**
     * Start here.
     * <p>
     * This is the top-level batch training loop. Each network configuration
     * is tested starting here to try and find the optimal network settings
     * without having to try them all manually.
     */
    private void runBatchTrainingLoop(final List<NetworkCandidate> networkCandidateList,
                                      final String trainingDataCsv,
                                      final String evaluationDataCsv,
                                      final NetworkParameters networkParameters,
                                      final int numberOfTrainingIterations) {
        if (NetworkUtils.keepGoing()) {
            int iterationNumber = 0;
            while (iterationNumber++ < numberOfTrainingIterations && NetworkUtils.keepGoing()) {
                // Now generate a new network structure
                String networkLayout = NetworkUtils.computeNetworkStructure(networkParameters.getNumberOfInputs(), networkParameters.getNumberOfOutputs());
                log.info("runBatchTrainingLoop(OUTER Iteration {}): Using network layout: {}", iterationNumber, networkLayout);

                networkParameters.setNetworkLayout(networkLayout);
                runBatchNetworkLayerTrainingLoop(networkCandidateList, trainingDataCsv, evaluationDataCsv, networkParameters, numberOfTrainingIterations, iterationNumber);
                log.debug("runBatchTriningLoop(): OUTER Iteration {} DONE.", iterationNumber);
            }
            log.debug("**** END OF BATCH LOOP ****");
        }
    }

    /**
     * Start here.
     * <p>
     * This is the top-level batch training loop. Each network configuration
     * is tested starting here to try and find the optimal network settings
     * without having to try them all manually.
     */
    private void runBatchNetworkLayerTrainingLoop(final List<NetworkCandidate> networkCandidateList,
                                                  final String trainingDataCsv,
                                                  final String evaluationDataCsv,
                                                  final NetworkParameters networkParameters,
                                                  final int numberOfTrainingIterations,
                                                  final int parentIteration) {
        if (NetworkUtils.keepGoing()) {
            int iterationNumber = 0;
            while (iterationNumber++ < numberOfTrainingIterations && NetworkUtils.keepGoing()) {
                String iteration = computeIterationNumberString(parentIteration, iterationNumber, numberOfTrainingIterations);
                log.info("Iteration: {}", iteration);
                runBatchActivationLoop(networkCandidateList, trainingDataCsv, evaluationDataCsv, networkParameters, iteration);
            }
            log.debug("**** END OF NETWORK LAYER LOOP ****");
        }
    }

    static String computeIterationNumberString(final int parentIteration, final int iterationNumber, final int numberOfTrainingIterations) {
        int iterationsPerformedSoFar = (parentIteration - 1) * numberOfTrainingIterations + (iterationNumber - 1);
        int totalIterationsToPerform = numberOfTrainingIterations * numberOfTrainingIterations;
        double percentageComplete = 100.0 * iterationsPerformedSoFar / totalIterationsToPerform;
        return String.format("%d.%d (%2.1f%%)", parentIteration, iterationNumber, percentageComplete);
    }

    /**
     * Runs all of the activators that are returned from fetchActivationChoices() to try
     * and find the optimal network configuration without having to try them all
     * manually.
     */
    private void runBatchActivationLoop(final List<NetworkCandidate> networkCandidateList,
                                        final String trainingDataCsv,
                                        final String evaluationDataCsv,
                                        final NetworkParameters networkParameters,
                                        final String iteration) {
        if (NetworkUtils.keepGoing()) {
            //
            // Use select activation functions to configure the network
            List<ActivationFunctionMenu.ActivationFunctionChoice> activationFunctionChoices = fetchActivationChoices();
            log.debug("**** BEGIN ACTIVATION LOOP ****");
            log.debug("\t Using activation functions: {}", StringUtils.join(activationFunctionChoices.toArray(), ", "));
            for (ActivationFunctionMenu.ActivationFunctionChoice activationFunctionChoice : activationFunctionChoices) {
                try {
                    log.debug("\tUsing Activation: {}", activationFunctionChoice);
                    // Set the activation function to use
                    networkParameters.setActivationFunction(activationFunctionChoice.getActivation());
                    //
                    // Use select updaters to configure the network
                    runBatchUpdaterLoop(networkCandidateList, trainingDataCsv, evaluationDataCsv, networkParameters, iteration);
                } catch (Exception e) {
                    log.error("Activation {} does not appear to work for this particular network configuration. Moving on to the next one.", activationFunctionChoice, e);
                }
            }
            log.debug("**** END OF ACTIVATION LOOP ****");
        }
    }

    /**
     * Runs all of the updaters that are returned from fetchUpdaterChoices() to try
     * and find the optimal network configuration without having to try them all
     * manually.
     */
    private void runBatchUpdaterLoop(final List<NetworkCandidate> networkCandidateList,
                                     final String trainingDataCsv,
                                     final String evaluationDataCsv,
                                     final NetworkParameters networkParameters,
                                     final String iteration) {
        List<UpdaterMenu.UpdaterChoice> updaterChoicesToUseForTraining = fetchUpdaterChoices();
        if (NetworkUtils.keepGoing()) {
            log.debug("**** BEGIN UPDATER LOOP ****");
            log.debug("\t Using updaters: {}", StringUtils.join(updaterChoicesToUseForTraining.toArray(), ", "));
            for (UpdaterMenu.UpdaterChoice updaterChoice : updaterChoicesToUseForTraining) {
                // Set the IUpdater to use
                log.debug("\tUsing Updater: {}", updaterChoice);
                networkParameters.setUpdater(updaterChoice.getUpdater());
                try {
                    //
                    // Create the network definition
                    log.debug("Configuring network...");
                    MultiLayerConfiguration configuration = NetworkUtils.configureNetwork(networkParameters);
                    //
                    // Use all normalizers within the current iteration using the same network configuration
                    runBatchNormalizerLoop(networkCandidateList, trainingDataCsv, evaluationDataCsv, networkParameters, iteration, configuration);
                } catch (Exception e) {
                    log.error("Updater {} does not appear to work for this particular network configuration. Moving on to the next one.", updaterChoice, e);
                }
            }
            log.debug("**** END OF UPDATER LOOP ****");
        }
    }

    /**
     * Specifies the Updaters that are to be used for batch training runs.
     */
    private static List<UpdaterMenu.UpdaterChoice> fetchUpdaterChoices() {
        List<UpdaterMenu.UpdaterChoice> ret = NetworkProperties.getUpdaters();
        if (ret.isEmpty()) {
            ret.add(UpdaterMenu.UpdaterChoice.AMS_GRAD);
            ret.add(UpdaterMenu.UpdaterChoice.ADA_MAX);
            ret.add(UpdaterMenu.UpdaterChoice.ADAM);
            ret.add(UpdaterMenu.UpdaterChoice.SGD);
        }
        return ret;
    }

    private static List<ActivationFunctionMenu.ActivationFunctionChoice> fetchActivationChoices() {
        List<ActivationFunctionMenu.ActivationFunctionChoice> ret = NetworkProperties.getActivationFunctions();
        if (ret.isEmpty()) {
            ret.add(ActivationFunctionMenu.ActivationFunctionChoice.RELU);
            ret.add(ActivationFunctionMenu.ActivationFunctionChoice.RATIONALTANH);
            ret.add(ActivationFunctionMenu.ActivationFunctionChoice.RELU6);
            ret.add(ActivationFunctionMenu.ActivationFunctionChoice.SOFTMAX);
            ret.add(ActivationFunctionMenu.ActivationFunctionChoice.TANH);
            ret.add(ActivationFunctionMenu.ActivationFunctionChoice.SIGMOID);
        }
        return ret;
    }

    private void runBatchNormalizerLoop(final List<NetworkCandidate> networkCandidateList,
                                        final String trainingDataCsv,
                                        final String evaluationDataCsv,
                                        final NetworkParameters networkParameters,
                                        final String iteration,
                                        final MultiLayerConfiguration configuration) throws IOException, InterruptedException {
        if (NetworkUtils.keepGoing()) {
            DataNormalizationMenu.DataNormalizationChoice[] normalizationChoices = DataNormalizationMenu.DataNormalizationChoice.values();
            log.debug("**** BEGIN NORMALIZER LOOP ****");
            log.debug("\t Using normalizers: {}", StringUtils.join(normalizationChoices, ", "));
            for (DataNormalizationMenu.DataNormalizationChoice dataNormalizationChoice : normalizationChoices) {
                log.debug("\tUsing normalizer: {}", dataNormalizationChoice);
                // Set the normalizer to use
                networkParameters.setNormalizer(dataNormalizationChoice);
                // Reuse the normalized data from the data set map
                DataSet trainingData = fetchOrCreateTrainingData(trainingDataCsv, networkParameters, dataNormalizationChoice);
                DataSet evaluationData = fetchOrCreateEvaluationData(evaluationDataCsv, networkParameters, dataNormalizationChoice);
                Optional<NetworkCandidate> networkCandidateMaybe = runBatchLoopInternal(trainingData, evaluationData, networkParameters, iteration, configuration);
                networkCandidateMaybe.ifPresent(networkCandidateList::add);
            }
        }
        log.debug("**** END OF NORMALIZER LOOP ****");
    }

    private DataSet fetchOrCreateEvaluationData(final String evaluationDataCsv, final NetworkParameters networkParameters, final DataNormalizationMenu.DataNormalizationChoice dataNormalizationChoice) throws IOException, InterruptedException {
        DataSet evaluationData = evaluationDataSetMap.get(dataNormalizationChoice);
        if (evaluationData == null) {
            log.info("*** CREATING EVALUATION DATA...");
            //
            // Create training and evaluation
            RecordReaderDataSetIterator evaluationDataIterator = NetworkUtils.createIterator(evaluationDataCsv, networkParameters);
            evaluationData = evaluationDataIterator.next();
            //
            // Prepare the data
            DataNormalization normalizer = networkParameters.getNormalizer().getDataNormalization();
            NetworkUtils.normalizeTrainingData(evaluationData, normalizer);
            evaluationDataSetMap.put(dataNormalizationChoice, evaluationData);
        }
        return evaluationData;
    }

    private DataSet fetchOrCreateTrainingData(final String trainingDataCsv, final NetworkParameters networkParameters, final DataNormalizationMenu.DataNormalizationChoice dataNormalizationChoice) throws IOException, InterruptedException {
        DataSet trainingData = trainingDataSetMap.get(dataNormalizationChoice);
        if (trainingData == null) {
            log.info("*** CREATING TRAINING DATA...");
            //
            // Create training and evaluation
            RecordReaderDataSetIterator trainingDataIterator = NetworkUtils.createIterator(trainingDataCsv, networkParameters);
            trainingData = trainingDataIterator.next();
            trainingDataIterator.setCollectMetaData(true);
            //
            // Prepare the data
            DataNormalization normalizer = networkParameters.getNormalizer().getDataNormalization();
            NetworkUtils.normalizeTrainingData(trainingData, normalizer);
            trainingDataSetMap.put(dataNormalizationChoice, trainingData);
        }
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
     * @param networkParameters Network training hyper-parameters
     * @param iteration         The current training iteration (used for logging so the operator
     *                          can tell what is happening)
     * @param configuration     The network configuration to use.
     * @return The trained network if it evaluated above the threshold, an empty Optional
     * if not.
     */
    private Optional<NetworkCandidate> runBatchLoopInternal(final DataSet trainingData,
                                                            final DataSet evaluationData,
                                                            final NetworkParameters networkParameters,
                                                            final String iteration,
                                                            final MultiLayerConfiguration configuration) {
        Optional<NetworkCandidate> ret = Optional.empty();
        //
        // Train the network. If nothing goes wrong, return the trained network.
        log.debug("Training network...");
        MultiLayerNetwork trainedNetwork = goInternal(configuration, networkParameters, trainingData, evaluationData, iteration);
        //
        // Save the network if its performance is at or above the threshold.
        // If the network was not saved, retain it anyway in the networkCandidate list
        // that is owned by the caller.
        NetworkCandidate networkCandidate = new NetworkCandidate(networkParameters.copy(), trainedNetwork);
        networkCandidate.getNetworkParameters().setWhenTrained(LocalDateTime.now());
        if (NetworkUtils.saveNetworkIfAboveThreshold(networkCandidate)) {
            ret = Optional.of(networkCandidate);
            //
            // Only keep networks in memory that are above the keep threshold to avoid running out of memory for large batch runs.
            Evaluation evaluation = NetworkUtils.computeNetworkEvaluator(networkParameters.getNumberOfOutputs(), evaluationData, trainedNetwork);
            double performanceThreshold = NetworkProperties.getPerformanceThreshold().doubleValue();
            log.info(String.format("*** Iteration %s: Performance threshold of %3.4f%% EXCEEDED: %3.5f%%, saving network. ***",
                    iteration, performanceThreshold, networkParameters.getNetworkAccuracy() * 100));
            log.info(evaluation.stats());
        }
        return ret;
    }

    private static MultiLayerNetwork goInternal(final MultiLayerConfiguration configuration,
                                                final NetworkParameters networkParameters,
                                                final DataSet trainingData,
                                                final DataSet evaluationData,
                                                final String iteration) {
        //
        // The training loop, returns the trained network model
        MultiLayerNetwork trainedNetwork = trainNetwork(configuration, networkParameters, trainingData, iteration);
        //
        // Evaluate the model on the evaluation set to see how well the trained model performs against unknown data
        Evaluation eval = NetworkUtils.computeNetworkEvaluator(networkParameters.getNumberOfOutputs(), evaluationData, trainedNetwork);
        networkParameters.setNetworkAccuracy(eval.accuracy());
        log.debug(String.format("Evaluator stats: %s", eval.stats()));
        log.info(String.format("\tIteration %s, Network accuracy: %f%%, No. epochs: %d, Normalizer: %s, Updater: %s, Activation: %s",
                iteration, eval.accuracy() * 100.0,
                networkParameters.getNumberOfEpochs(),
                networkParameters.getNormalizer(), UpdaterMenu.UpdaterChoice.from(networkParameters.getUpdater()),
                networkParameters.getActivationFunction()));
        log.debug(networkParameters.toStringAbbreviated());
        log.debug("**** END OF TRAINING LOOP ({}/{}/{}) ****", networkParameters.getNormalizer(), networkParameters.getActivationFunction(), networkParameters.getUpdater());
        return trainedNetwork;
    }

    /**
     * The network training loop. It uses the user-supplied number of epochs as a starting
     * place and attempts to train the network to below the threshold specified in
     * NetworkProperties. This method will try up to:
     * (user-supplied epochs value) x (max.network.iterations in NetworkProperties)
     * and if it can train the network before it reaches that value it breaks out early.
     * If it cannot it still returns the trained network, albeit one that did not train
     * below the threshold (the user may still want it anyway).
     */
    private static MultiLayerNetwork trainNetwork(final MultiLayerConfiguration configuration,
                                                  final NetworkParameters networkParameters,
                                                  final DataSet trainingData,
                                                  final String iteration) {
        //
        // Compute the number of epochs at which the network will be periodically evaluated
        log.debug("trainNetwork: training up to {} epochs", NetworkProperties.getMaxLearningIterations());
        //
        // Create the model to be trained
        MultiLayerNetwork network = new MultiLayerNetwork(configuration);
        network.init();
        int epochCount;
        for (epochCount = 0; epochCount < NetworkProperties.getMaxLearningIterations() && NetworkUtils.keepGoing(); epochCount++) {
            // Shake it up
            trainingData.shuffle();
            // Copy original network parameters because with each training loop it gets mutated
            //
            // Fit the model against the training data for a single epoch
            network.fit(trainingData);
            if (epochCount % NetworkProperties.getNetworkEvaluationIndex() == 0) {
                Evaluation networkEvaluator = NetworkUtils.computeNetworkEvaluator(networkParameters.getNumberOfOutputs(), trainingData, network);
                double networkError = NetworkUtils.computeNetworkError(networkEvaluator);
                if (networkError < NetworkProperties.getMaxNetworkError()) {
                    // Network is trained, we're done here
                    log.debug("\tNetwork trained after {} epochs.", epochCount);
                    log.debug(networkParameters.toStringAbbreviated());
                    networkParameters.setNumberOfEpochs(epochCount);
                    break;
                } else if (epochCount % NetworkProperties.getEpochStatsIndexCount() == 0) {
                    log.debug("\tEpoch number: {}, Network error: {}", epochCount, networkError);
                    //
                    // Reup properties - okay to mutate original object
                    NetworkUtils.reupPropertiesBetweenEpochBreaks(networkParameters);
                }
            }
        }
        networkParameters.setNumberOfEpochs(epochCount);
        networkParameters.setWhenTrained(LocalDateTime.now());
        log.debug(String.format("\tIteration %s, Number of epochs: %d, Network error: %2.4f%%, Normalizer: %s, Updater: %s, Activation: %s", iteration, epochCount,
                NetworkUtils.computeNetworkError(NetworkUtils.computeNetworkEvaluator(networkParameters.getNumberOfOutputs(), trainingData, network)) * 100,
                networkParameters.getNormalizer(), UpdaterMenu.UpdaterChoice.from(networkParameters.getUpdater()),
                networkParameters.getActivationFunction()));
        return network;
    }

    private String computeTrainingDataCsv(final List<Integer> yearsToCompute,
                                          final SeasonDataDao<T> seasonDataDao,
                                          final TournamentResultDao tournamentResultDao,
                                          final TournamentParticipantDao tournamentParticipantDao,
                                          final NetworkParameters networkParameters) {
        return createCsvData(yearsToCompute, seasonDataDao, tournamentResultDao, tournamentParticipantDao, networkParameters);
    }

    private String computeEvaluationDataCsv(final List<Integer> yearsToCompute,
                                            final SeasonDataDao<T> seasonDataDao,
                                            final TournamentResultDao tournamentResultDao,
                                            final TournamentParticipantDao tournamentParticipantDao,
                                            final NetworkParameters networkParameters) {
        return createCsvData(yearsToCompute, seasonDataDao, tournamentResultDao, tournamentParticipantDao, networkParameters);
    }

    /**
     * Fetch network parameters for a batch run, and set the values selected by the user
     * in the NetworkParameters object that is supplied.
     */
    private void promptUserForNetworkParameters(final BufferedReader scanner,
                                                final NetworkParameters networkParameters,
                                                final TournamentParticipantDao tournamentParticipantDao,
                                                final Integer numberOfOutputClasses) throws IOException {
        promptUserForNetworkParameters(scanner, networkParameters, tournamentParticipantDao, numberOfOutputClasses, true);
    }

    /**
     * Fetch network parameters and set the values selected by the user in the NetworkParameters
     * object that is supplied. If not for a batch run, the user is prompted for the normalizer
     * to be used.
     */
    private void promptUserForNetworkParameters(final BufferedReader scanner,
                                                final NetworkParameters networkParameters,
                                                final TournamentParticipantDao tournamentParticipantDao,
                                                final Integer numberOfOutputClasses,
                                                final boolean isBatch) throws IOException {
        System.out.println("This is the Train Network Menu.");
        //
        // Set the dropout probability
        networkParameters.setDropoutProbability(NetworkProperties.getDropoutProbability());
        //
        // Get number of outputs
        networkParameters.setNumberOfOutputs(numberOfOutputClasses);
        //
        // Get the selected Elements
        List<AbstractDataElementChoice> dataElementMenuChoices = scanDataElementChoice(scanner, networkParameters.getSelectedElements());
        if (dataElementMenuChoices.size() != 0) {
            networkParameters.setSelectedElements(dataElementMenuChoices);
            //
            // Get number of inputs
            networkParameters.setNumberOfInputs(dataElementMenuChoices.size());
            //
            // Fetch years to train and evaluate the network
            NetworkTrainerParameterScanner.scanYearsToTrainAndEvaluateNetwork(scanner, networkParameters, tournamentParticipantDao);
            if (!isBatch) {
                //
                // Get the network layout in HL1xHL2xHL3x...HLn format, where HL1 is the first hidden layer, HLn is the nth.
                String networkLayout = NetworkTrainerParameterScanner.scanNetworkLayout(scanner, networkParameters.getNetworkLayout(), networkParameters.getNumberOfInputs(), networkParameters.getNumberOfOutputs());
                networkParameters.setNetworkLayout(networkLayout);
                //
                // Get the activation function
                Activation activation = NetworkTrainerParameterScanner.scanActivationFunction(scanner, networkParameters.getActivationFunction());
                networkParameters.setActivationFunction(activation);
                //
                // Get the Normalizer to use
                DataNormalizationMenu.DataNormalizationChoice dataNormalization = NetworkTrainerParameterScanner.scanDataNormalization(scanner, networkParameters.getNormalizer());
                networkParameters.setNormalizer(dataNormalization);
                //
                // Get the weight updater function
                IUpdater updater = NetworkTrainerParameterScanner.scanUpdaterFunction(scanner, networkParameters.getUpdater());
                networkParameters.setUpdater(updater);
            }
            //
            // Get the Loss function
            LossFunctions.LossFunction lossFunction = NetworkTrainerParameterScanner.scanLossFunction(scanner, networkParameters.getLossFunction());
            networkParameters.setLossFunction(lossFunction);
            //
            // Get the WeightInit function
            WeightInit weightInit = NetworkTrainerParameterScanner.scanWeightInitFunction(scanner, networkParameters.getWeightInit());
            networkParameters.setWeightInit(weightInit);
            //
            // Get the L2 Regularization Lambda value
            double l2RegularizationLambda = NetworkTrainerParameterScanner.scanL2RegularizationLambda(scanner, networkParameters.getL2RegularizationLambda());
            networkParameters.setL2RegularizationLambda(l2RegularizationLambda);
        } else {
            log.error("No data elements selected. Cannot train the network.");
        }
    }

}
