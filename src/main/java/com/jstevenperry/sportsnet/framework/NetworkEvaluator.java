package com.jstevenperry.sportsnet.framework;

import com.jstevenperry.sportsnet.framework.model.NetworkCandidate;
import com.jstevenperry.sportsnet.framework.model.NetworkParameters;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.framework.persistence.TournamentParticipantDao;
import com.jstevenperry.sportsnet.framework.persistence.TournamentResultDao;
import com.jstevenperry.sportsnet.framework.util.NetworkUtils;
import org.apache.commons.lang3.StringUtils;
import org.datavec.api.records.metadata.RecordMetaData;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class NetworkEvaluator {

  private static final Logger log = LoggerFactory.getLogger(NetworkEvaluator.class);

  public static void go(final BufferedReader scanner,
                        final List<NetworkCandidate> networkCandidateList,
                        final SeasonDataDao seasonDataDao,
                        final TournamentResultDao tournamentResultDao,
                        final TournamentParticipantDao tournamentParticipantDao) throws IOException, InterruptedException {
    //
    // Load saved networks
    List<NetworkCandidate> savedNetworks = NetworkUtils.loadNetworks();
    //
    // Add loaded networks to networkCandidateList
    List<NetworkCandidate> allNetworks = new ArrayList<>();
    allNetworks.addAll(savedNetworks);
    allNetworks.addAll(networkCandidateList);
    if (!allNetworks.isEmpty()) {
      //
      // Ask the user what they want to do
      promptUser(scanner, allNetworks, seasonDataDao, tournamentResultDao, tournamentParticipantDao);
    } else {
      log.info("No networks to run. Train some networks and try again.");
    }
  }

  private static void promptUser(final BufferedReader scanner,
                                 final List<NetworkCandidate> allNetworks,
                                 final SeasonDataDao seasonDataDao,
                                 final TournamentResultDao tournamentResultDao,
                                 final TournamentParticipantDao tournamentParticipantDao) throws IOException, InterruptedException {
    //
    // Get the network the user wants to run
    Optional<NetworkCandidate> selectedNetwork = NetworkPersister.displayNetworkSelectionMenu(scanner, allNetworks, NetworkPersister.ACTION_WORK_WITH);
    if (selectedNetwork.isPresent()) {
      NetworkCandidate networkCandidate = selectedNetwork.get();
      NetworkParameters networkParameters = networkCandidate.getNetworkParameters();
      //
      // Prompt the user for the year(s) they want to evaluate
      System.out.println("Enter year(s) for evaluation/validation (enter multiple years separated by commas)");
      List<Integer> yearsToEvaluate = networkParameters.getYearsToTrainAndEvaluateNetwork().get(1);
      AbstractNetworkTrainer.scanListOfValidYears(scanner, yearsToEvaluate);
      log.info("Evaluation data year(s): " + StringUtils.join(yearsToEvaluate, ','));
      //
      // Run the evaluation
      log.info("Pulling evaluation data...");
      RecordReaderDataSetIterator evaluationDataIterator = AbstractNetworkTrainer.createIterator(yearsToEvaluate, seasonDataDao, tournamentResultDao, tournamentParticipantDao, networkParameters);
      DataSet evaluationData = evaluationDataIterator.next();
      networkParameters.getNormalizer().transform(evaluationData);     // Apply normalization to the training data
      double[][] trainingDataFeatures = evaluationData.getFeatures().toDoubleMatrix();
      // Dump out a few records as a sanity check
      int sampleSize = 5;
      int sampleCount = 0;
      for (double[] feature : trainingDataFeatures) {
        if (sampleCount++ == sampleSize) {
          break;
        }
        log.debug(String.format("Training Data Sample:\n%s", Arrays.toString(feature)));
      }
      Evaluation eval = new Evaluation(7);
      INDArray output = networkCandidate.getMultiLayerNetwork().output(evaluationData.getFeatures());
      eval.eval(evaluationData.getLabels(), output, evaluationData.getExampleMetaData(RecordMetaData.class)); // Note we are passing in the test set metadata here
      networkParameters.setNetworkAccuracy(eval.accuracy());
      log.info(String.format("Evaluator stats: %s", eval.stats()));
      log.info(String.format("Network accuracy: %f%%", eval.accuracy() * 100.0));
      log.info(networkParameters.toString());
    } else {
      System.out.println("No network selected, quitting.");
    }
  }
}
