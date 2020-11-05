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
import org.deeplearning4j.optimize.listeners.PerformanceListener;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
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
    fetchNetworkParameters(bufferedReader, networkParameters, tournamentParticipantDao, numberOfOutputClasses);
    log.info("Network parameters acquired.");
    //
    // Get the number of epochs
    Integer numberOfEpochs = scanNumberOfEpochs(bufferedReader, networkParameters.getNumberOfEpochs());
    networkParameters.setNumberOfEpochs(numberOfEpochs);
    //
    // Get the season data for the teams in the training years
    List<Integer> yearsToTrain = networkParameters.getYearsToTrainAndEvaluateNetwork().get(0);
    log.info("Pulling training data...");
    RecordReaderDataSetIterator trainingDataIterator = createIterator(yearsToTrain, seasonDataDao, tournamentResultDao, tournamentParticipantDao, networkParameters);
    //
    // Get the season data for the teams in the evaluation years
    List<Integer> yearsToEvaluate = networkParameters.getYearsToTrainAndEvaluateNetwork().get(1);
    log.info("Pulling evaluation data...");
    RecordReaderDataSetIterator evaluationDataIterator = createIterator(yearsToEvaluate, seasonDataDao, tournamentResultDao, tournamentParticipantDao, networkParameters);
    //
    // Create the network definition
    log.info("Configuring network...");
    MultiLayerConfiguration configuration = configureNetwork(networkParameters);
    //
    // Train the network. If nothing goes wrong, return the trained network.
    log.info("Training network...");
    int numberOfUserSpecifiedEpochs = networkParameters.getNumberOfEpochs();
    MultiLayerNetwork trainedNetwork = goInternal(configuration, networkParameters, trainingDataIterator, evaluationDataIterator);
    // Restore the value for next time since it will be set to the actual number used, and the user may not want that next time
    networkParameters.setNumberOfEpochs(numberOfUserSpecifiedEpochs);
    log.info("Training network...DONE.");
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
                                   final String networkType) throws IOException, InterruptedException {
    NetworkParameters networkParameters = (networkParametersGlobal == null) ? new NetworkParameters() : networkParametersGlobal;
    networkParameters.setNetworkType(networkType);
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
    //
    // Get the number of batch iterations to use
    int numberOfTrainingIterations = NetworkProperties.getMaxNetworkIterations();
    networkParameters.setNumberOfEpochs(NetworkProperties.getMaxLearningIterations());
    int iterationNumber = 0;
    //
    // Fetch the network configuration parameters (reuse NetworkParameters so user doesn't have to re-enter everything each time)
    fetchNetworkParameters(bufferedReader, networkParameters, tournamentParticipantDao, numberOfOutputClasses);
    log.info("Network parameters acquired.");
    //
    // Get the season data for the teams in the training years
    List<Integer> yearsToTrain = networkParameters.getYearsToTrainAndEvaluateNetwork().get(0);
    //
    // Get the season data for the teams in the evaluation years
    List<Integer> yearsToEvaluate = networkParameters.getYearsToTrainAndEvaluateNetwork().get(1);
    int numberOfEpochs = networkParameters.getNumberOfEpochs();
    while (iterationNumber++ < numberOfTrainingIterations) {
      log.debug("Pulling training data...");
      RecordReaderDataSetIterator trainingDataIterator = createIterator(yearsToTrain, seasonDataDao, tournamentResultDao, tournamentParticipantDao, networkParameters);
      log.debug("Pulling evaluation data...");
      RecordReaderDataSetIterator evaluationDataIterator = createIterator(yearsToEvaluate, seasonDataDao, tournamentResultDao, tournamentParticipantDao, networkParameters);
      //
      // Create the network definition
      log.debug("Configuring network...");
      MultiLayerConfiguration configuration = configureNetwork(networkParameters);
      //
      // Train the network. If nothing goes wrong, return the trained network.
      log.debug("Training network...");
      MultiLayerNetwork trainedNetwork = goInternal(configuration, networkParameters, trainingDataIterator, evaluationDataIterator);
      //
      // Save the network if its performance is at or above the threshold.
      // If the network was not saved, retain it anyway in the networkCandidate list
      // that is owned by the caller.
      if (!saveNetworkIfAboveThreshold(networkParameters, iterationNumber, trainedNetwork)) {
        networkCandidateList.add(keepNetwork(trainedNetwork, networkParameters));
      }
      // Reset to original user-specified number
      networkParameters.setNumberOfEpochs(numberOfEpochs);
    }
    return networkParameters;
  }

  private void fetchNetworkParameters(final BufferedReader scanner,
                                      final NetworkParameters networkParameters,
                                      final TournamentParticipantDao tournamentParticipantDao,
                                      final Integer numberOfOutputClasses) throws IOException {
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
      //
      // Get the network layout in HL1xHL2xHL3x...HLn format, where HL1 is the first hidden layer, HLn is the nth.
      String networkLayout = scanNetworkLayout(scanner, networkParameters.getNetworkLayout(), networkParameters.getNumberOfInputs(), networkParameters.getNumberOfOutputs());
      networkParameters.setNetworkLayout(networkLayout);
      //
      // Get the activation function
      Activation activation = scanActivationFunction(scanner, networkParameters.getActivationFunction());
      networkParameters.setActivationFunction(activation);
      //
      // Get the Loss function
      LossFunctions.LossFunction lossFunction = scanLossFunction(scanner, networkParameters.getLossFunction());
      networkParameters.setLossFunction(lossFunction);
      //
      // Get the WeightInit function
      WeightInit weightInit = scanWeightInitFunction(scanner, networkParameters.getWeightInit());
      networkParameters.setWeightInit(weightInit);
      //
      // Get the weight updater function
      UpdaterMenu.UpdaterChoice updater = scanUpdaterFunction(scanner, networkParameters.getUpdater());
      networkParameters.setUpdater(updater);
      //
      // Get the L2 Regularization Lambda value
      double l2RegularizationLambda = scanL2RegularizationLambda(scanner, networkParameters.getL2RegularizationLambda());
      networkParameters.setL2RegularizationLambda(l2RegularizationLambda);
      //
      // Get the Normalizer to use
      DataNormalizationMenu.DataNormalizationChoice dataNormalization = scanDataNormalization(scanner, networkParameters.getNormalizer());
      networkParameters.setNormalizer(dataNormalization);
    } else {
      log.error("No data elements selected. Cannot train the network.");
    }
  }

  private static MultiLayerNetwork goInternal(final MultiLayerConfiguration configuration,
                                              final NetworkParameters networkParameters,
                                              final RecordReaderDataSetIterator trainingDataIterator,
                                              final RecordReaderDataSetIterator evaluationDataIterator) {
    //
    // Prepare the data
    trainingDataIterator.setCollectMetaData(true);
    DataSet trainingData = trainingDataIterator.next();
    DataSet evaluationData = evaluationDataIterator.next();
    DataNormalization normalizer = networkParameters.getNormalizer().getDataNormalization();
    normalizeTrainingData(trainingData, evaluationData, normalizer);
    //
    // The training loop, returns the trained network model
    MultiLayerNetwork trainedNetwork = trainNetwork(configuration, networkParameters, trainingData, trainingData);
    //
    // Evaluate the model on the evaluation set to see how well the trained model performs against unknown data
    Evaluation eval = new Evaluation(networkParameters.getNumberOfOutputs());
    INDArray output = trainedNetwork.output(evaluationData.getFeatures());
    eval.eval(evaluationData.getLabels(), output, evaluationData.getExampleMetaData(RecordMetaData.class)); // Note we are passing in the test set metadata here
    networkParameters.setNetworkAccuracy(eval.accuracy());
    log.info(String.format("Evaluator stats: %s", eval.stats()));
    log.info(String.format("Network accuracy: %f%%", eval.accuracy() * 100.0));
    log.info(networkParameters.toStringAbbreviated());
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
                                                final DataSet evaluationData) {
    //
    // Compute the number of epochs at which the network will be periodically evaluated
    int networkEvaluationIndex = Integer.min(networkParameters.getNumberOfEpochs() / 10, NetworkProperties.getEpochBreakIterationCount());
    int maxTrainingEpochs = networkParameters.getNumberOfEpochs() * NetworkProperties.getMaxNetworkIterations();
    networkParameters.setNumberOfEpochs(maxTrainingEpochs); // Assume network could not be trained early
    //
    // Create the model to be trained
    MultiLayerNetwork network = new MultiLayerNetwork(configuration);
    network.init();
    network.setListeners(new PerformanceListener(networkParameters.getNumberOfEpochs(), true));
    for (int epochCount = 0; epochCount < maxTrainingEpochs; epochCount++) {
      //
      // Fit the model against the training data for a single epoch
      network.fit(trainingData);
      if (epochCount % networkEvaluationIndex == 0) {
        double networkError = computeNetworkError(networkParameters, evaluationData, network);
        if (networkError < NetworkProperties.getMaxNetworkError()) {
          // Network is trained, we're done here
          log.info("Network trained after {} epochs.", epochCount);
          networkParameters.setNumberOfEpochs(epochCount);
          break;
        } else {
          log.info("Epoch number: {}, Network error: {}", epochCount, networkError);
        }
      }
    }
    log.info(String.format("End of training loop. Network error: %2.4f%%", computeNetworkError(networkParameters, evaluationData, network) * 100));
    return network;
  }

  private static double computeNetworkError(final NetworkParameters networkParameters, final DataSet evaluationData, final MultiLayerNetwork network) {
    //
    // The network evaluator
    Evaluation eval = new Evaluation(networkParameters.getNumberOfOutputs());
    //
    // Evaluate the model on the training set to see how well the model fits.
    // If the model fits then stop training early.
    INDArray output = network.output(evaluationData.getFeatures());
    eval.eval(evaluationData.getLabels(), output, evaluationData.getExampleMetaData(RecordMetaData.class));
    double accuracy = eval.accuracy();
    log.debug("Network accuracy: {}", accuracy);
    return 1.0 - accuracy;
  }

  private static NetworkCandidate keepNetwork(final MultiLayerNetwork network, final NetworkParameters networkParameters) {
    NetworkCandidate ret = new NetworkCandidate(networkParameters.copy(), network);
    networkParameters.setWhenTrained(LocalDateTime.now());
    log.info(String.format("Network %s retained.", network.toString()));
    return ret;
  }

  private static boolean saveNetworkIfAboveThreshold(final NetworkParameters networkParameters, final int iterationNumber, final MultiLayerNetwork network) {
    boolean ret = false; // Network not saved
    double performanceThreshold = NetworkProperties.getPerformanceThreshold().doubleValue();
    NetworkCandidate keptNetwork = keepNetwork(network, networkParameters);
    if (networkParameters.getNetworkAccuracy() * 100 >= performanceThreshold) {
      log.info(String.format("Iteration %d: Performance threshold (%3.4f) EXCEEDED: %3.5f, saving network.",
        iterationNumber, performanceThreshold, networkParameters.getNetworkAccuracy() * 100));
      ret = NetworkPersister.saveNetwork(keptNetwork);
    } else {
      log.info(String.format("Iteration %d: Network accuracy (%3.5f) below performance threshold (%3.4f).",
        iterationNumber, networkParameters.getNetworkAccuracy() * 100, performanceThreshold));
    }
    return ret;
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
      .updater(networkParameters.getUpdater().getUpdater())
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
    log.info("Training data year(s): " + StringUtils.join(yearsToTrain, ','));

    System.out.println("Enter year(s) for evaluation/validation (enter multiple years separated by commas)");
    List<Integer> yearsToEvaluate = networkParameters.getYearsToTrainAndEvaluateNetwork().get(1);
    scanListOfValidYears(scanner, yearsToEvaluate, tournamentParticipantDao);
    log.info("Evaluation data year(s): " + StringUtils.join(yearsToEvaluate, ','));
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

  private static String scanNetworkLayout(final BufferedReader scanner,
                                          final String networkLayout,
                                          final Integer numberOfInputs,
                                          final Integer numberOfOutputs) throws IOException {
    String ret = (StringUtils.isNotEmpty(networkLayout)) ? networkLayout : "23x43x83x87x73x61x43x37x23";
    String userInput = null;
    while (StringUtils.isEmpty(userInput)) {
      System.out.println("Enter the network structure (for example, " + ret + ")");
      System.out.printf("Note: only specify the hidden layer structure. Input layer (%d neurons) and output layer (%d neurons) are already specified.%n",
        numberOfInputs, numberOfOutputs);
      System.out.println("==> ");
      if (StringUtils.isNotEmpty(networkLayout)) {
        int[] existingNetworkLayout = NetworkUtils.parseNetworkStructure(networkLayout);
        // Strip off the first layer and set it to the number of inputs (if the user has added or removed data elements the previous number of inputs will be wrong)
        existingNetworkLayout[0] = numberOfInputs;
        ret = StringUtils.join(existingNetworkLayout, 'x');
        System.out.println("Press enter to keep your previous choice of " + ret + " or enter a new one below:");
      }
      userInput = scanner.readLine();
      if (StringUtils.isEmpty(userInput)) {
        System.out.println("Using your previous choice of " + ret + "....");
        break;
      } else if (StringUtils.isNotEmpty(userInput)) {
        ret = String.format("%dx%sx%d", numberOfInputs, StringUtils.join(NetworkUtils.parseNetworkStructure(userInput), 'x'), numberOfOutputs);
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

  private static UpdaterMenu.UpdaterChoice scanUpdaterFunction(final BufferedReader scanner,
                                                               final UpdaterMenu.UpdaterChoice updaterChoice) throws IOException {
    UpdaterMenu.UpdaterChoice ret = null;
    while (ret == null) {
      Optional<UpdaterMenu.UpdaterChoice> menuChoice = UpdaterMenu.go(scanner, updaterChoice);
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

  private int scanNumberOfTrainingIterations(final BufferedReader scanner) throws IOException {
    int ret = 1; // Default: 1 training iteration
    String userInput = null;
    while (StringUtils.isEmpty(userInput)) {
      System.out.println("Enter the number of training iterations: ");
      System.out.println("Press enter to use: " + ret);
      userInput = scanner.readLine();
      if (StringUtils.isEmpty(userInput)) {
        break;
      } else if (StringUtils.isNumeric(userInput)) {
        ret = Integer.parseInt(userInput);
        break;
      } else {
        String message = String.format("Not a valid value for number of training iterations: %s, please try again.", userInput);
        log.warn(message);
        userInput = StringUtils.EMPTY; // keep looping
      }
    }
    return ret;
  }


}
