/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet.framework;

import com.jstevenperry.sportsnet.framework.menus.AbstractDataElementChoice;
import com.jstevenperry.sportsnet.framework.menus.ActivationFunctionMenu;
import com.jstevenperry.sportsnet.framework.menus.DataNormalizationMenu;
import com.jstevenperry.sportsnet.framework.menus.LossFunctionMenu;
import com.jstevenperry.sportsnet.framework.menus.UpdaterMenu;
import com.jstevenperry.sportsnet.framework.menus.WeightInitMenu;
import com.jstevenperry.sportsnet.framework.model.AbstractSeasonData;
import com.jstevenperry.sportsnet.framework.model.NetworkCandidate;
import com.jstevenperry.sportsnet.framework.model.NetworkParameters;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.framework.persistence.TournamentParticipantDao;
import com.jstevenperry.sportsnet.framework.persistence.TournamentResultDao;
import com.jstevenperry.sportsnet.framework.util.NetworkPersister;
import com.jstevenperry.sportsnet.framework.util.NetworkProperties;
import com.jstevenperry.sportsnet.framework.util.NetworkUtils;
import org.apache.commons.lang3.StringUtils;
import org.datavec.api.records.metadata.RecordMetaData;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.jetbrains.annotations.NotNull;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.StringTokenizer;

public abstract class AbstractNetworkTrainer<T extends AbstractSeasonData> extends AbstractNetworkBase<T> {

    private static final Logger log = LoggerFactory.getLogger(AbstractNetworkTrainer.class);

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

    public NetworkCandidate go(final NetworkParameters networkParametersGlobal,
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
        fetchNetworkParameters(bufferedReader, networkParameters, tournamentParticipantDao, numberOfOutputClasses, false);
        log.debug("Network parameters acquired.");
        //
        // Get the number of epochs
        Integer numberOfEpochs = scanNumberOfEpochs(bufferedReader, NetworkProperties.getMaxNetworkIterations());
        //
        // Compute the training and evaluation data needed to train and evaluate networks
        log.debug("Pulling training and evaluation data...");
        String trainingDataCsv = computeTrainingDataCsv(networkParameters.getYearsToTrainNetwork(), seasonDataDao, tournamentResultDao, tournamentParticipantDao, networkParameters);
        String evaluationDataCsv = computeEvaluationDataCsv(networkParameters.getYearsToEvaluateNetwork(), seasonDataDao, tournamentResultDao, tournamentParticipantDao, networkParameters);
        RecordReaderDataSetIterator trainingDataIterator = createIterator(trainingDataCsv, networkParameters);
        RecordReaderDataSetIterator evaluationDataIterator = createIterator(evaluationDataCsv, networkParameters);
        //
        // Create the network definition
        log.debug("Configuring network...");
        MultiLayerConfiguration configuration = configureNetwork(networkParameters);
        //
        // Train the network. If nothing goes wrong, return the trained network.
        log.debug("Training network...");
        MultiLayerNetwork trainedNetwork = goInternal(configuration, networkParameters, trainingDataIterator.next(), evaluationDataIterator.next(), "1.1");
        // Restore the value for next time since it will be set to the actual number used, and the user may not want that next time
        networkParameters.setNumberOfEpochs(numberOfEpochs);
        log.debug("Training network...DONE.");
        //
        // Return the trained network
        return keepNetwork(trainedNetwork, networkParameters);
    }

    public NetworkParameters goBatch(final NetworkParameters networkParametersGlobal,
                                     final List<NetworkCandidate> networkCandidateList,
                                     final SeasonDataDao<T> seasonDataDao,
                                     final TournamentResultDao tournamentResultDao,
                                     final TournamentParticipantDao tournamentParticipantDao,
                                     final Integer numberOfOutputClasses,
                                     final String networkType) throws IOException {
        NetworkParameters networkParameters = (networkParametersGlobal == null) ? new NetworkParameters() : networkParametersGlobal;
        if (keepGoing()) {
            networkParameters.setNetworkType(networkType);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            //
            // Get the number of batch iterations to use
            int numberOfTrainingIterations = NetworkProperties.getMaxNetworkIterations();
            //
            // Fetch the network configuration parameters (reuse NetworkParameters so user doesn't have to re-enter everything each time)
            fetchNetworkParameters(bufferedReader, networkParameters, tournamentParticipantDao, numberOfOutputClasses);
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
    private static void runBatchTrainingLoop(final List<NetworkCandidate> networkCandidateList,
                                             final String trainingDataCsv,
                                             final String evaluationDataCsv,
                                             final NetworkParameters networkParameters,
                                             final int numberOfTrainingIterations) {
        if (keepGoing()) {
            int iterationNumber = 0;
            while (iterationNumber++ < numberOfTrainingIterations && keepGoing()) {
                // Now generate a new network structure
                String networkLayout = computeNetworkStructure(networkParameters.getNumberOfInputs(), networkParameters.getNumberOfOutputs());
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
    private static void runBatchNetworkLayerTrainingLoop(final List<NetworkCandidate> networkCandidateList,
                                                         final String trainingDataCsv,
                                                         final String evaluationDataCsv,
                                                         final NetworkParameters networkParameters,
                                                         final int numberOfTrainingIterations,
                                                         final int parentIteration) {
        if (keepGoing()) {
            int iterationNumber = 0;
            while (iterationNumber++ < numberOfTrainingIterations && keepGoing()) {
                String iteration = computeIterationNumberString(parentIteration, iterationNumber, numberOfTrainingIterations);
                log.info("Iteration: {}", iteration);
                runBatchActivationLoop(networkCandidateList, trainingDataCsv, evaluationDataCsv, networkParameters, iteration);
            }
            log.debug("**** END OF NETWORK LAYER LOOP ****");
        }
    }

    static String computeIterationNumberString(final int parentIteration, final int iterationNumber, final int numberOfTrainingIterations) {
        int iterationsPerformedSoFar = (parentIteration - 1) * numberOfTrainingIterations + iterationNumber - 1;
        int totalIterationsToPerform = numberOfTrainingIterations * numberOfTrainingIterations + numberOfTrainingIterations;
        double percentageComplete = 100.0 * iterationsPerformedSoFar / totalIterationsToPerform;
        return String.format("%d.%d (%2.1f%%)", parentIteration, iterationNumber, percentageComplete);
    }

    /**
     * Runs all of the activators that are returned from fetchActivationChoices() to try
     * and find the optimal network configuration without having to try them all
     * manually.
     */
    private static void runBatchActivationLoop(final List<NetworkCandidate> networkCandidateList,
                                               final String trainingDataCsv,
                                               final String evaluationDataCsv,
                                               final NetworkParameters networkParameters,
                                               final String iteration) {
        if (keepGoing()) {
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
    private static void runBatchUpdaterLoop(final List<NetworkCandidate> networkCandidateList,
                                            final String trainingDataCsv,
                                            final String evaluationDataCsv,
                                            final NetworkParameters networkParameters,
                                            final String iteration) {
        List<UpdaterMenu.UpdaterChoice> updaterChoicesToUseForTraining = fetchUpdaterChoices();
        if (keepGoing()) {
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
                    MultiLayerConfiguration configuration = configureNetwork(networkParameters);
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

    private static void runBatchNormalizerLoop(final List<NetworkCandidate> networkCandidateList,
                                               final String trainingDataCsv,
                                               final String evaluationDataCsv,
                                               final NetworkParameters networkParameters,
                                               final String iteration,
                                               final MultiLayerConfiguration configuration) throws IOException, InterruptedException {
        if (keepGoing()) {
            DataNormalizationMenu.DataNormalizationChoice[] normalizationChoices = DataNormalizationMenu.DataNormalizationChoice.values();
            log.debug("**** BEGIN NORMALIZER LOOP ****");
            log.debug("\t Using normalizers: {}", StringUtils.join(normalizationChoices, ", "));
            for (DataNormalizationMenu.DataNormalizationChoice dataNormalizationChoice : normalizationChoices) {
                log.debug("\tUsing normalizer: {}", dataNormalizationChoice);
                // Set the normalizer to use
                networkParameters.setNormalizer(dataNormalizationChoice);
                runBatchLoopInternal(networkCandidateList, trainingDataCsv, evaluationDataCsv, networkParameters, iteration, configuration);
            }
        }
        log.debug("**** END OF NORMALIZER LOOP ****");
    }

    private static void runBatchLoopInternal(final List<NetworkCandidate> networkCandidateList,
                                             final String trainingDataCsv,
                                             final String evaluationDataCsv,
                                             final NetworkParameters networkParameters,
                                             final String iteration,
                                             final MultiLayerConfiguration configuration) throws IOException, InterruptedException {
        //
        // Create training and evaluation
        RecordReaderDataSetIterator trainingDataIterator = createIterator(trainingDataCsv, networkParameters); // TODO: CREATE THIS ONCE
        RecordReaderDataSetIterator evaluationDataIterator = createIterator(evaluationDataCsv, networkParameters); // TODO: CREATE THIS ONCE
        //
        // Train the network. If nothing goes wrong, return the trained network.
        log.debug("Training network...");
        DataSet trainingData = trainingDataIterator.next();
        DataSet evaluationData = evaluationDataIterator.next();
        trainingDataIterator.setCollectMetaData(true);
        MultiLayerNetwork trainedNetwork = goInternal(configuration, networkParameters, trainingData, evaluationData, iteration);
        //
        // Save the network if its performance is at or above the threshold.
        // If the network was not saved, retain it anyway in the networkCandidate list
        // that is owned by the caller.
        NetworkCandidate networkCandidate = keepNetwork(trainedNetwork, networkParameters);
        //
        // Only keep networks in memory that are above the keep threshold to avoid running out of memory for large batch runs.
        if (networkCandidate.getNetworkParameters().getNetworkAccuracy() * 100 >= NetworkProperties.getPerformanceThresholdNetworkKeep().doubleValue()) {
            log.info("INNER Iteration: {}, network retained.", iteration);
            Evaluation evaluation = computeNetworkEvaluator(networkParameters.getNumberOfOutputs(), evaluationData, trainedNetwork);
            log.info(evaluation.stats());
            networkCandidateList.add(networkCandidate);
            saveNetworkIfAboveThreshold(networkCandidate, iteration);
        } else {
            log.debug("INNER Iteration: {}, network discarded.", iteration);
        }
    }

    protected static String computeNetworkStructure(final int numberOfInputs, final int numberOfOutputs) {
        String hiddenLayerStructure = NetworkUtils.generateRandomNetworkLayout(numberOfInputs, numberOfOutputs);
        return String.format("%dx%sx%d", numberOfInputs, hiddenLayerStructure, numberOfOutputs);
    }

    /**
     * Fetch network parameters for a batch run, and set the values selected by the user
     * in the NetworkParameters object that is supplied.
     */
    private void fetchNetworkParameters(final BufferedReader scanner,
                                        final NetworkParameters networkParameters,
                                        final TournamentParticipantDao tournamentParticipantDao,
                                        final Integer numberOfOutputClasses) throws IOException {
        fetchNetworkParameters(scanner, networkParameters, tournamentParticipantDao, numberOfOutputClasses, true);
    }

    /**
     * Fetch network parameters and set the values selected by the user in the NetworkParameters
     * object that is supplied. If not for a batch run, the user is prompted for the normalizer
     * to be used.
     */
    private void fetchNetworkParameters(final BufferedReader scanner,
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
            scanYearsToTrainAndEvaluateNetwork(scanner, networkParameters, tournamentParticipantDao);
            if (!isBatch) {
                //
                // Get the network layout in HL1xHL2xHL3x...HLn format, where HL1 is the first hidden layer, HLn is the nth.
                String networkLayout = scanNetworkLayout(scanner, networkParameters.getNetworkLayout(), networkParameters.getNumberOfInputs(), networkParameters.getNumberOfOutputs());
                networkParameters.setNetworkLayout(networkLayout);
                //
                // Get the activation function
                Activation activation = scanActivationFunction(scanner, networkParameters.getActivationFunction());
                networkParameters.setActivationFunction(activation);
                //
                // Get the Normalizer to use
                DataNormalizationMenu.DataNormalizationChoice dataNormalization = scanDataNormalization(scanner, networkParameters.getNormalizer());
                networkParameters.setNormalizer(dataNormalization);
                //
                // Get the weight updater function
                IUpdater updater = scanUpdaterFunction(scanner, networkParameters.getUpdater());
                networkParameters.setUpdater(updater);
            }
            //
            // Get the Loss function
            LossFunctions.LossFunction lossFunction = scanLossFunction(scanner, networkParameters.getLossFunction());
            networkParameters.setLossFunction(lossFunction);
            //
            // Get the WeightInit function
            WeightInit weightInit = scanWeightInitFunction(scanner, networkParameters.getWeightInit());
            networkParameters.setWeightInit(weightInit);
            //
            // Get the L2 Regularization Lambda value
            double l2RegularizationLambda = scanL2RegularizationLambda(scanner, networkParameters.getL2RegularizationLambda());
            networkParameters.setL2RegularizationLambda(l2RegularizationLambda);
        } else {
            log.error("No data elements selected. Cannot train the network.");
        }
    }

    private static MultiLayerNetwork goInternal(final MultiLayerConfiguration configuration,
                                                final NetworkParameters networkParameters,
                                                final DataSet trainingData,
                                                final DataSet evaluationData,
                                                final String iteration) {
        //
        // Prepare the data
        DataNormalization normalizer = networkParameters.getNormalizer().getDataNormalization();
        normalizeTrainingData(trainingData, evaluationData, normalizer);
        //
        // The training loop, returns the trained network model
        MultiLayerNetwork trainedNetwork = trainNetwork(configuration, networkParameters, trainingData, iteration);
        //
        // Evaluate the model on the evaluation set to see how well the trained model performs against unknown data
        Evaluation eval = computeNetworkEvaluator(networkParameters.getNumberOfOutputs(), evaluationData, trainedNetwork);
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
        for (epochCount = 0; epochCount < NetworkProperties.getMaxLearningIterations() && keepGoing(); epochCount++) {
            // Shake it up
            trainingData.shuffle();
            // Copy original network parameters because with each training loop it gets mutated
            //
            // Fit the model against the training data for a single epoch
            network.fit(trainingData);
            if (epochCount % NetworkProperties.getNetworkEvaluationIndex() == 0) {
                Evaluation networkEvaluator = computeNetworkEvaluator(networkParameters.getNumberOfOutputs(), trainingData, network);
                double networkError = computeNetworkError(networkEvaluator);
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
                    reupPropertiesBetweenEpochBreaks(networkParameters);
                }
            }
        }
        log.debug(String.format("\tIteration %s, Number of epochs: %d, Network error: %2.4f%%, Normalizer: %s, Updater: %s, Activation: %s", iteration, epochCount, computeNetworkError(
                computeNetworkEvaluator(networkParameters.getNumberOfOutputs(), trainingData, network)) * 100,
                networkParameters.getNormalizer(), UpdaterMenu.UpdaterChoice.from(networkParameters.getUpdater()),
                networkParameters.getActivationFunction()));
        return network;
    }

    private static boolean keepGoing() {
        boolean ret = NetworkProperties.killSwitchActivated();
        if (ret) {
            log.warn("KILL SWITCH ACTIVE! TRAINING LOOP WILL NOT RUN!");
        }
        return !ret;
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

    private static void reupPropertiesBetweenEpochBreaks(final NetworkParameters networkParameters) {
        // Updater
        IUpdater currentUpdater = networkParameters.getUpdater();
        Optional<UpdaterMenu.UpdaterChoice> refreshedUpdater = UpdaterMenu.UpdaterChoice.from(networkParameters.getUpdater());
        if (refreshedUpdater.isPresent()) {
            networkParameters.setUpdater(refreshedUpdater.get().getUpdater());
        } else {
            networkParameters.setUpdater(currentUpdater);
        }
    }

    private static double computeNetworkError(final Evaluation networkEvaluator) {
        double accuracy = networkEvaluator.accuracy();
        log.debug("Network accuracy: {}", accuracy);
        return 1.0 - accuracy;
    }

    @NotNull
    private static Evaluation computeNetworkEvaluator(final Integer numberOfOutputs, final DataSet evaluationData, final MultiLayerNetwork network) {
        //
        // The network evaluator
        Evaluation eval = new Evaluation(numberOfOutputs);
        //
        // Evaluate the model on the training set to see how well the model fits.
        // If the model fits then stop training early.
        INDArray output = network.output(evaluationData.getFeatures());
        eval.eval(evaluationData.getLabels(), output, evaluationData.getExampleMetaData(RecordMetaData.class));
        return eval;
    }

    private static NetworkCandidate keepNetwork(final MultiLayerNetwork network, final NetworkParameters networkParameters) {
        NetworkCandidate ret = new NetworkCandidate(networkParameters.copy(), network);
        ret.getNetworkParameters().setWhenTrained(LocalDateTime.now());
        return ret;
    }

    private static void saveNetworkIfAboveThreshold(final NetworkCandidate networkCandidate, final String iteration) {
        double performanceThreshold = NetworkProperties.getPerformanceThreshold().doubleValue();
        NetworkParameters networkParameters = networkCandidate.getNetworkParameters();
        if (networkParameters.getNetworkAccuracy() * 100 >= performanceThreshold) {
            log.info(String.format("*** Iteration %s: Performance threshold of %3.4f%% EXCEEDED: %3.5f%%, saving network. ***",
                    iteration, performanceThreshold, networkParameters.getNetworkAccuracy() * 100));
            NetworkPersister.saveNetwork(networkCandidate);
        } else {
            log.info(String.format("\tIteration %s: Network accuracy (%3.5f) below performance threshold (%3.4f).",
                    iteration, networkParameters.getNetworkAccuracy() * 100, performanceThreshold));
        }
    }

    private static void normalizeTrainingData(final DataSet trainingData, final DataSet testData, final DataNormalization normalizer) {
        log.debug("Normalizing data...");
        normalizer.fit(trainingData);           // Collect the statistics (mean/stdev) from the training data. This does not modify the input data
        normalizer.transform(trainingData);     // Apply normalization to the training data
        double[][] trainingDataFeatures = trainingData.getFeatures().toDoubleMatrix();
        // Dump out a few records as a sanity check
        int sampleSize = 5;
        int sampleCount = 0;
        for (double[] feature : trainingDataFeatures) {
            if (sampleCount++ == sampleSize) {
                break;
            }
            log.debug(String.format("normalizeTrainingData(): Training Data:\n%s", Arrays.toString(feature)));
        }
        normalizer.transform(testData);         // Apply normalization to the test data. This is using statistics calculated from the *training* set
        log.debug("Normalizing data...DONE");
    }

    private static MultiLayerConfiguration configureNetwork(final NetworkParameters networkParameters) {
        log.debug("Configuring network...");
        NeuralNetConfiguration.Builder builder = new NeuralNetConfiguration.Builder()
                .seed(new Random(-2).nextInt(100))
                .activation(networkParameters.getActivationFunction())
                .weightInit(networkParameters.getWeightInit())
                .updater(networkParameters.getUpdater())
                .dropOut(NetworkProperties.getDropoutProbability())
                .l2(networkParameters.getL2RegularizationLambda())
                .gradientNormalization(GradientNormalization.RenormalizeL2PerLayer);
        NeuralNetConfiguration.ListBuilder listBuilder = builder.list();
        int[] networkLayout = NetworkUtils.parseNetworkStructure(networkParameters.getNetworkLayout());
        for (int index = 0; index < networkLayout.length - 2; index++) {
            listBuilder.layer(addHiddenLayer(networkLayout[index], networkLayout[index + 1]));
        }
        // Build the final layer and call build to create the MultiLayerConfiguration and return it
        MultiLayerConfiguration ret = listBuilder.layer(new OutputLayer.Builder(networkParameters.getLossFunction())
                .activation(Activation.SOFTMAX).nIn(networkLayout[networkLayout.length - 2]).nOut(networkLayout[networkLayout.length - 1]).build())
                .build();
        log.debug("Configuring network...DONE");
        return ret;
    }

    private static DenseLayer addHiddenLayer(final int numberOfInputs, final int numberOfOutputs) {
        return new DenseLayer.Builder()
                .nIn(numberOfInputs)
                .nOut(numberOfOutputs)
                .build();
    }

    /**
     * Get a list of the years for which the network is to be trained, and a list
     * of the years for which the network is to be evaluated, and return them both
     * in a wrapper list.
     * <p>
     * The first element in the wrapper list is a the list of training years.
     */
    private static void scanYearsToTrainAndEvaluateNetwork(final BufferedReader scanner,
                                                           final NetworkParameters networkParameters,
                                                           final TournamentParticipantDao tournamentParticipantDao) {
        System.out.println("Enter year(s) for training (enter multiple years separated by commas)");

        List<Integer> yearsToTrain = networkParameters.getYearsToTrainAndEvaluateNetwork().get(0);
        scanListOfValidYears(scanner, yearsToTrain, tournamentParticipantDao);
        log.debug("Training data year(s): " + StringUtils.join(yearsToTrain, ','));

        System.out.println("Enter year(s) for evaluation/validation (enter multiple years separated by commas)");
        List<Integer> yearsToEvaluate = networkParameters.getYearsToTrainAndEvaluateNetwork().get(1);
        scanListOfValidYears(scanner, yearsToEvaluate, tournamentParticipantDao);
        log.debug("Evaluation data year(s): " + StringUtils.join(yearsToEvaluate, ','));
    }

    public static void scanListOfValidYears(final BufferedReader scanner,
                                            final List<Integer> listOfYears,
                                            final TournamentParticipantDao tournamentParticipantDao) {
        try {
            String userInput = null;
            while (StringUtils.isEmpty(userInput)) {
                if (!listOfYears.isEmpty()) {
                    System.out.println("Press enter to keep your previous selection of " + StringUtils.join(listOfYears, ','));
                }
                // Get user input - a comma-separated list of years
                userInput = scanner.readLine();
                if (StringUtils.isNotEmpty(userInput)) {
                    listOfYears.clear();
                    // Break it up into tokens
                    StringTokenizer strtok = new StringTokenizer(userInput, ",");
                    while (strtok.hasMoreTokens()) {
                        Integer year = Integer.valueOf(strtok.nextToken().trim());
                        // Validate year
                        NetworkUtils.validateYear(year, tournamentParticipantDao);
                        listOfYears.add(year);
                    }
                } else if (!listOfYears.isEmpty()) {
                    break;
                } else {
                    System.out.println("==> "); // Prompt
                }
            }
        } catch (Exception e) {
            String message = String.format("Error getting years for training: %s", e.getLocalizedMessage());
            log.error(message);
            throw new RuntimeException(message, e);
        }
    }

    private String scanNetworkLayout(final BufferedReader scanner,
                                     final String networkLayout,
                                     final Integer numberOfInputs,
                                     final Integer numberOfOutputs) throws IOException {
        String ret = (StringUtils.isNotEmpty(networkLayout)) ? networkLayout : computeNetworkStructure(numberOfInputs, numberOfOutputs);
        String userInput = null;
        while (StringUtils.isEmpty(userInput)) {
            System.out.println("Enter the network structure (for example, " + ret + ")");
            System.out.printf("Note: only specify the hidden layer structure. Input layer (%d neurons) and output layer (%d neurons) are already specified.%n",
                    numberOfInputs, numberOfOutputs);
            System.out.println("==> ");
            int[] existingNetworkLayout = NetworkUtils.parseNetworkStructure(ret);
            // Strip off the first layer and set it to the number of inputs (if the user has added or removed data elements the previous number of inputs will be wrong)
            existingNetworkLayout[0] = numberOfInputs;
            ret = StringUtils.join(existingNetworkLayout, 'x');
            System.out.println("Press enter to use " + ret + ", enter a new one below, or press r to regenerate the suggested layout:");
            userInput = scanner.readLine();
            if (StringUtils.isEmpty(userInput)) {
                System.out.println("Using: " + ret + "....");
                break;
            } else if (StringUtils.isNotEmpty(userInput)) {
                if (userInput.equalsIgnoreCase("r")) {
                    ret = computeNetworkStructure(numberOfInputs, numberOfOutputs);
                    userInput = null; // keep looping
                } else {
                    ret = String.format("%dx%sx%d", numberOfInputs, StringUtils.join(NetworkUtils.parseNetworkStructure(userInput), 'x'), numberOfOutputs);
                }
            } else {
                System.out.println("==> ");
            }
        }
        log.info("Network layout: " + ret);
        return ret;
    }

    private static Activation scanActivationFunction(final BufferedReader scanner, final Activation activation) throws IOException {
        Activation ret = null;
        while (ret == null) {
            Optional<ActivationFunctionMenu.ActivationFunctionChoice> menuChoice = ActivationFunctionMenu.go(scanner, activation);
            if (menuChoice.isPresent()) {
                ret = menuChoice.get().getActivation();
            }
        }
        return ret;
    }

    private static IUpdater scanUpdaterFunction(final BufferedReader scanner,
                                                final IUpdater updaterChoice) throws IOException {
        IUpdater ret = null;
        while (ret == null) {
            Optional<IUpdater> menuChoice = UpdaterMenu.go(scanner, updaterChoice);
            if (menuChoice.isPresent()) {
                ret = menuChoice.get();
            }
        }
        return ret;
    }

    private static WeightInit scanWeightInitFunction(final BufferedReader scanner, final WeightInit weightInit) throws IOException {
        WeightInit ret = null;
        while (ret == null) {
            Optional<WeightInitMenu.WeightInitChoice> menuChoice = WeightInitMenu.go(scanner, weightInit);
            if (menuChoice.isPresent()) {
                ret = menuChoice.get().getWeightInit();
            }
        }
        return ret;
    }

    private static Integer scanNumberOfEpochs(final BufferedReader scanner, final Integer numberOfEpochs) throws IOException {
        Integer ret = numberOfEpochs;
        String userInput = null;
        while (StringUtils.isEmpty(userInput)) {
            System.out.println("Enter the number of epochs: ");
            if (numberOfEpochs != null) {
                System.out.println("Press enter to use the number of epochs you used last time: " + numberOfEpochs);
            }
            userInput = scanner.readLine();
            if (StringUtils.isEmpty(userInput)) {
                break;
            } else if (StringUtils.isNumeric(userInput)) {
                ret = Integer.parseInt(userInput);
            } else {
                String message = String.format("Not a valid value for number of epochs: %s", userInput);
                log.warn(message);
                ret = null;
            }
        }
        return ret;
    }

    private static LossFunctions.LossFunction scanLossFunction(final BufferedReader scanner, final LossFunctions.LossFunction lossFunction) throws IOException {
        LossFunctions.LossFunction ret = null;
        while (ret == null) {
            Optional<LossFunctionMenu.LossFunctionChoice> menuChoice = LossFunctionMenu.go(scanner, lossFunction);
            if (menuChoice.isPresent()) {
                ret = menuChoice.get().getLossFunction();
            }
        }
        return ret;
    }

    private static Double scanL2RegularizationLambda(final BufferedReader scanner, final Double l2RegularizationLambda) throws IOException {
        Double ret = l2RegularizationLambda;
        String userInput = null;
        while (StringUtils.isEmpty(userInput)) {
            System.out.println("Enter the L2 Regularization Lambda value: ");
            if (l2RegularizationLambda != null) {
                System.out.println("Press enter to use the value you used last time: " + l2RegularizationLambda);
            }
            userInput = scanner.readLine();
            if (StringUtils.isEmpty(userInput)) {
                break;
            } else if (StringUtils.isNumeric(StringUtils.remove(userInput, '.'))) {
                ret = Double.parseDouble(userInput);
            } else {
                String message = String.format("Not a valid value for L2 Regularization Lambda value: %s", userInput);
                log.warn(message);
                ret = null;
            }
        }
        return ret;
    }

    private static DataNormalizationMenu.DataNormalizationChoice scanDataNormalization(final BufferedReader scanner,
                                                                                       final DataNormalizationMenu.DataNormalizationChoice dataNormalization) throws IOException {
        DataNormalizationMenu.DataNormalizationChoice ret = null;
        while (ret == null) {
            Optional<DataNormalizationMenu.DataNormalizationChoice> menuChoice = DataNormalizationMenu.go(scanner, dataNormalization);
            if (menuChoice.isPresent()) {
                ret = menuChoice.get();
            }
        }
        return ret;
    }

}
