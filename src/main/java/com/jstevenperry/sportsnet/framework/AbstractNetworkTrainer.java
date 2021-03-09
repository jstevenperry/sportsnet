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
    private final Map<DataNormalizationMenu.DataNormalizationChoice, DataSet> trainingDataSetMap = new HashMap<>(); // Normalized training data cache
    private final Map<DataNormalizationMenu.DataNormalizationChoice, DataSet> evaluationDataSetMap = new HashMap<>(); // Normalized evaluation data cache

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
        String trainingDataCsv = createCsvData(networkParameters.getYearsToTrainNetwork(), seasonDataDao, tournamentResultDao, tournamentParticipantDao, networkParameters);
        String evaluationDataCsv = createCsvData(networkParameters.getYearsToEvaluateNetwork(), seasonDataDao, tournamentResultDao, tournamentParticipantDao, networkParameters);
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
            String trainingDataCsv = createCsvData(networkParameters.getYearsToTrainNetwork(), seasonDataDao, tournamentResultDao, tournamentParticipantDao, networkParameters);
            String evaluationDataCsv = createCsvData(networkParameters.getYearsToEvaluateNetwork(), seasonDataDao, tournamentResultDao, tournamentParticipantDao, networkParameters);
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

    /**
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
                String iteration = NetworkUtils.computeIterationNumberString(parentIteration, iterationNumber, numberOfTrainingIterations);
                log.info("Iteration: {}", iteration);
                runBatchActivationLoop(networkCandidateList, trainingDataCsv, evaluationDataCsv, networkParameters, iteration);
            }
            log.debug("**** END OF NETWORK LAYER LOOP ****");
        }
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
            List<ActivationFunctionMenu.ActivationFunctionChoice> activationFunctionChoices = NetworkTrainerParameterScanner.fetchActivationChoices();
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
        List<UpdaterMenu.UpdaterChoice> updaterChoicesToUseForTraining = NetworkTrainerParameterScanner.fetchUpdaterChoices();
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
     * Runs all of the normalizers that are defined in the DataNormalizationChoice enum.
     *
     * @param networkCandidateList The list of networks to which the trained network will be
     *                             added if it is deemed worthy (that is, evaluates above the
     *                             threshold)
     * @param trainingDataCsv      Raw training data in CSV format
     * @param evaluationDataCsv    Raw evaluation data in CSV format
     * @param networkParameters    Network hyper parameters
     * @param iteration            The current iteration number (used for logging)
     * @param configuration        The network configuration to be used to instantiate the network
     */
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
                DataSet trainingData = fetchOrCreateTrainingDataSet(trainingDataCsv, networkParameters, dataNormalizationChoice);
                DataSet evaluationData = fetchOrCreateEvaluationDataSet(evaluationDataCsv, networkParameters, dataNormalizationChoice);
                Optional<NetworkCandidate> networkCandidateMaybe = runBatchLoopInternal(trainingData, evaluationData, networkParameters, iteration, configuration);
                networkCandidateMaybe.ifPresent(networkCandidateList::add);
            }
        }
        log.debug("**** END OF NORMALIZER LOOP ****");
    }

    /**
     * Fetch the DataSet used for evaluating trained networks from the cache, or create it if it does
     * not exist yet (then store it in the cache).
     *
     * @param evaluationDataCsv       The raw CSV season data to use to create the DataSet used
     *                                for evaluating trained networks.
     * @param networkParameters       Network hyper parameters
     * @param dataNormalizationChoice The normalizer to use to normalize the data before
     *                                it is turned into a DataSet (if it needs to be created)
     * @return The DataSet, ready to be used to evaluate trained networks
     */
    private DataSet fetchOrCreateEvaluationDataSet(final String evaluationDataCsv, final NetworkParameters networkParameters, final DataNormalizationMenu.DataNormalizationChoice dataNormalizationChoice) throws IOException, InterruptedException {
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

    /**
     * Fetch the DataSet used for training from the cache, or create it if it does
     * not exist yet (then store it in the cache).
     *
     * @param trainingDataCsv         The raw CSV season data to use to create the DataSet used
     *                                for training networks.
     * @param networkParameters       Network hyper parameters
     * @param dataNormalizationChoice The normalizer to use to normalize the data before
     *                                it is turned into a DataSet (if it needs to be created)
     * @return The DataSet, ready to be used to train networks
     */
    private DataSet fetchOrCreateTrainingDataSet(final String trainingDataCsv,
                                                 final NetworkParameters networkParameters,
                                                 final DataNormalizationMenu.DataNormalizationChoice dataNormalizationChoice) throws IOException, InterruptedException {
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

    /**
     * Down to business. Train and return a MultiLayerNetwork.
     */
    private MultiLayerNetwork goInternal(final MultiLayerConfiguration configuration,
                                         final NetworkParameters networkParameters,
                                         final DataSet trainingData,
                                         final DataSet evaluationData,
                                         final String iteration) {
        //
        // The training loop, returns the trained network model
        MultiLayerNetwork trainedNetwork = NetworkUtils.trainNetwork(configuration, networkParameters, trainingData, iteration);
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

}
