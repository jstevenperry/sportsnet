/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jstevenperry.sportsnet.framework.config.AbstractSportsnetProperties;
import com.jstevenperry.sportsnet.framework.config.NetworkStructureAlgorithm;
import com.jstevenperry.sportsnet.framework.dto.*;
import com.jstevenperry.sportsnet.framework.exception.InternalServerErrorException;
import com.jstevenperry.sportsnet.framework.model.*;
import com.jstevenperry.sportsnet.framework.service.AbstractTournamentRunnerService;
import org.apache.commons.lang3.StringUtils;
import org.datavec.api.records.metadata.RecordMetaData;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.InputStreamInputSplit;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.common.io.CollectionUtils;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.shade.guava.io.CharStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractNetworkUtils {

    private static final Logger log = LoggerFactory.getLogger(AbstractNetworkUtils.class);

    public static final String LOG_PREFIX = "COREANU";

    /**
     * Returns a NxNxNxN style string showing the layer structure
     * of a network that would have the specified layer structure.
     * The 0th element contains the number of neurons in the input
     * layer. The (<code>neuronLayerDescriptor</code>.size()-1)th element
     * contains the number of neurons in the output layer.
     */
    public static String generateLayerStructureString(List<Integer> neuronLayerDescriptor) {
        StringBuilder sb = new StringBuilder();
        int aa = 0;
        for (Integer neuronsCount : neuronLayerDescriptor) {
            if (aa++ > 0) {
                sb.append("x");
            }
            sb.append(neuronsCount);
        }
        return sb.toString();
    }

    /**
     * Parses the specified network structure into an int array.
     * See getNetworks() for a description of the structure.
     */
    public static int[] parseNetworkStructure(String networkStructure) {
        List<Integer> structure = new ArrayList<>();
        StringTokenizer strtok = new StringTokenizer(networkStructure, "x");
        while (strtok.hasMoreTokens()) {
            try {
                int neuronsInLayer = Integer.parseInt(strtok.nextToken());
                structure.add(neuronsInLayer);
            } catch (NumberFormatException e) {
                String message = String.format("%s0081E: Cannot make sense out of network structure: '%s' (alpha characters " + "in" + " " + "layers, maybe?)", LOG_PREFIX, networkStructure);
                log.error("parseNetworkStructure({}): {}", networkStructure, message, e);
                throw new RuntimeException(message, e);
            }
        }
        log.debug(String.format("parseNetworkStructure(%s): %s9901D: Network layer structure: %s", networkStructure, LOG_PREFIX, generateLayerStructureString(structure)));

        return structure
                .stream()
                .mapToInt(Integer::intValue)
                .toArray();
    }

    /**
     * Validates the year. If not between the specified range, an exception is thrown,
     * terminating the run.
     * <p>
     * Note: as you add years to the DB, you will need to modify this method accordingly.
     *
     * @param year The year to validate.
     * @throws RuntimeException - if the year to validate is outside the supported range.
     */
    public <T extends SeasonData> void validateYear(final Integer year,
                                                    final AbstractTournamentRunnerService<T> tournamentRunnerService) {
        List<Integer> validTournamentYears = tournamentRunnerService.fetchPluginYears();
        if (!validTournamentYears.contains(year)) {
            String message = String.format("%s0083E: Invalid year: %d (must be one of %s)", LOG_PREFIX, year, StringUtils.join(validTournamentYears.toArray(), ','));
            log.error("validateYear({}...): {}", year, message);
            throw new RuntimeException(message);
        }
    }

    /**
     * Removes unwanted characters from the string. Returns "0" if the string it empty.
     */
    public String nice(final String string) {
        String ret = "0";
        if (StringUtils.isNotEmpty(string)) {
            ret = StringUtils.remove(StringUtils.trim(string), '%');
        }
        return ret;
    }

    /**
     * Generate a random network layout.
     *
     * @param networkDescriptor - the parameters that drive everything the network training/eval/run code does
     * @return A String (in Ixh1xh2x...xhNxO format, where I = number of inputs,
     * O = number of outputs, and hN is the number of neurons in the Nth hidden layer)
     */
    public String computeNetworkStructure(final NetworkDescriptor networkDescriptor) {
        final String hiddenLayerStructure;
        final int numberOfInputs = networkDescriptor.getNumberOfInputs();
        final int numberOfOutputs = networkDescriptor.getNumberOfOutputs();
        final int numberOfHiddenLayers = networkDescriptor.getNumberOfHiddenLayers();

        boolean randomizeNumberOfNeurons = false;
        switch (networkDescriptor.getNetworkStructureAlgorithm()) {
            case HOUR_GLASS_RAND:
                randomizeNumberOfNeurons = true;
            case HOUR_GLASS:
                hiddenLayerStructure = NetworkStructureGenerator.generateHourglassNetworkLayout(numberOfInputs, numberOfOutputs, numberOfHiddenLayers, randomizeNumberOfNeurons);
                break;
            case TAPERED_TREE_RAND:
                randomizeNumberOfNeurons = true;
            case TAPERED_TREE:
                hiddenLayerStructure = NetworkStructureGenerator.generateTaperedTreeNetworkLayout(numberOfInputs, numberOfOutputs, numberOfHiddenLayers, randomizeNumberOfNeurons);
                break;
            default:
                String message = String.format("%s2220W: computeNetworkStructure(): No network structure algorithm specified. " + "Using the" + " default (%s)...", LOG_PREFIX, NetworkStructureAlgorithm.DEFAULT_RAND);
                log.warn("computeNetworkStructure(...): {}", message);
            case DEFAULT_RAND_SMALL:
                randomizeNumberOfNeurons = true;
            case DEFAULT_SMALL:
                hiddenLayerStructure = NetworkStructureGenerator.generateSmallDefaultNetworkLayout(numberOfInputs, numberOfOutputs, numberOfHiddenLayers, randomizeNumberOfNeurons);
                break;
            case DEFAULT_RAND:
                randomizeNumberOfNeurons = true;
            case DEFAULT:
                hiddenLayerStructure = NetworkStructureGenerator.generateDefaultNetworkLayout(numberOfInputs, numberOfOutputs, numberOfHiddenLayers, randomizeNumberOfNeurons);
                break;
            case USER_DEFINED_RAND:
                randomizeNumberOfNeurons = true;
            case USER_DEFINED:
                hiddenLayerStructure = NetworkStructureGenerator.generateUserDefinedLayout(networkDescriptor.getNetworkLayout(), randomizeNumberOfNeurons);
                break;
        }
        return String.format("%dx%sx%d", networkDescriptor.getNumberOfInputs(), hiddenLayerStructure, numberOfOutputs);
    }

    /**
     * Configures a Multi-layer neural network using the specified network descriptor
     * and returns the configuration to the caller.
     */
    public MultiLayerConfiguration configureNetwork(final NetworkDescriptor networkDescriptor) {
        NeuralNetConfiguration.Builder builder = new NeuralNetConfiguration.Builder()
                .seed(new Random(-2).nextInt(100))
                .activation(networkDescriptor
                        .getActivationFunction()
                        .getActivation())
                .weightInit(networkDescriptor
                        .getWeightInit()
                        .getWeightInit())
                .updater(UpdaterChoice.fromUpdaterChoice(networkDescriptor))
                .dropOut(networkDescriptor.getDropoutProbability())
                .l2(networkDescriptor.getL2RegularizationLambda())
                .gradientNormalization(GradientNormalization.RenormalizeL2PerLayer);
        NeuralNetConfiguration.ListBuilder listBuilder = builder.list();
        int[] networkLayout = parseNetworkStructure(networkDescriptor.getNetworkLayout());
        for (int index = 0; index < networkLayout.length - 2; index++) {
            listBuilder.layer(createDenseLayer(networkLayout[index], networkLayout[index + 1]));
        }
        // Build the final layer and call build to create the MultiLayerConfiguration and return it
        return listBuilder
                .layer(new OutputLayer.Builder(networkDescriptor
                        .getOutputLayerLossFunction()
                        .getLossFunction())
                        .activation(networkDescriptor
                                .getOutputLayerActivation()
                                .getActivation())
                        .nIn(networkLayout[networkLayout.length - 2])
                        .nOut(networkLayout[networkLayout.length - 1])
                        .build())
                .build();
    }

    /**
     * Creates a single DenseLayer having the specified number of input and output neurons
     * and returns it to the caller.
     *
     * @param numberOfInputs  The number of input neurons to the layer
     * @param numberOfOutputs The number of neurons from the layer
     */
    public DenseLayer createDenseLayer(final int numberOfInputs, final int numberOfOutputs) {
        return new DenseLayer.Builder()
                .nIn(numberOfInputs)
                .nOut(numberOfOutputs)
                .build();
    }

    /**
     * Computes the network error described by the specified Evaluator.
     *
     * @param networkEvaluator The Evaluator containing the stats to be used for the calculation
     * @return The fractional error (0.0 < (fractionalError) < 1.0) of the network described by
     * the specified evaluator.
     */
    public double computeNetworkError(final Evaluation networkEvaluator) {
        double computedNetworkError;
        computedNetworkError = 1.0 - computeNetworkAccuracyScore(networkEvaluator);

        if (log.isTraceEnabled()) {
            log.trace("computeNetworkError(): {}9912D: Network error: {}", LOG_PREFIX, computedNetworkError);
        }
        return computedNetworkError;
    }

    /**
     * Computes and returns the accuracy of the network described by the specified
     * Evaluation object.
     */
    public double computeNetworkAccuracyScore(final Evaluation networkEvaluator) {
        double averageNetworkAccuracy = networkEvaluator.recall();
        if (log.isTraceEnabled()) {
            log.trace("computeNetworkAccuracyScore(): {}9902T: Network accuracy: {}", LOG_PREFIX, averageNetworkAccuracy);
        }
        return averageNetworkAccuracy;
    }

    /**
     * Computes and returns the accuracy score based on the specified accuracy, precision, and recall scores.
     */
    public static double computeNetworkAccuracyScore(final double accuracy, final double precision, final double recall,
                                                     final double f1Score) {
        //
        // Compute the average of the accuracy, precision, and recall (hey, it's gotta be done
        // somehow, and accuracy alone is a lousy way of doing it)
        double averageNetworkAccuracy = (accuracy + precision + recall + f1Score) / 4.0;
        if (log.isTraceEnabled()) {
            log.trace("computeNetworkAccuracyScore(): {}9901T: Network accuracy: avg({} + {} + {} + {}): {}", LOG_PREFIX, accuracy, precision, recall, f1Score, averageNetworkAccuracy);
        }
        return averageNetworkAccuracy;
    }

    /**
     * Computes a Evaluation object that contains a detailed evaluation of the
     * specified network, based on the specified evaluation data, for the specified
     * number of output classes.
     *
     * @param numberOfOutputs The number of output classes in the labeled data
     * @param evaluationData  The labeled data used to evaluate the network
     * @param network         The network to be evaluated
     */
    public Evaluation computeNetworkEvaluator(final Integer numberOfOutputs, final DataSet evaluationData,
                                              final MultiLayerNetwork network) {
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

    /**
     * Returns true if the network is above its configured threshold, false otherwise.
     */
    public boolean networkIsAboveThreshold(final NetworkDescriptor networkDescriptor) {
        boolean savedNetwork = false;
        double performanceThreshold = networkDescriptor.getNetworkPerformanceThreshold();
        double networkAccuracyScore = networkDescriptor.getRealNetworkAccuracy();
        if (log.isDebugEnabled()) {
            log.debug("networkIsAboveThreshold(...): {}9941D: Network accuracy (from {},{},{},{}}): {}",
                    LOG_PREFIX, networkDescriptor.getNetworkAccuracy(), networkDescriptor.getNetworkPrecision(),
                    networkDescriptor.getNetworkRecall(), networkDescriptor.getNetworkF1Score(), networkAccuracyScore);
        }
        if (networkAccuracyScore * 100 >= performanceThreshold) {
            if (log.isDebugEnabled()) {
                log.debug("networkIsAboveThreshold(...): {}9920D: Network accuracy ({}) is above the specified " + "threshold ({}).", LOG_PREFIX, networkAccuracyScore, performanceThreshold);
            }
            savedNetwork = true;
        }
        return savedNetwork;
    }

    public void normalizeTrainingData(final DataSet testData, final DataNormalization normalizer) {
        normalizer.fit(testData);           // Collect the statistics (mean/stdev) from the training data. This does
        // not modify the input data
        normalizer.transform(testData);     // Apply normalization to the training data
        double[][] trainingDataFeatures = testData
                .getFeatures()
                .toDoubleMatrix();
        // Dump out a few records as a sanity check
        int sampleSize = 5;
        int sampleCount = 0;
        for (double[] feature : trainingDataFeatures) {
            if (sampleCount++ == sampleSize) {
                break;
            }
            log.debug(String.format("%s9922D: normalizeTrainingData(): Training Data:\n%s", LOG_PREFIX, Arrays.toString(feature)));
        }
    }

    public RecordReaderDataSetIterator createIterator(final String csvData,
                                                      final NetworkDescriptor networkDescriptor) throws IOException, InterruptedException {
        RecordReader recordReader = new CSVRecordReader(0, ',');
        recordReader.initialize(new InputStreamInputSplit(new ByteArrayInputStream(CharStreams
                .toString(new StringReader(csvData))
                .getBytes())));
        return new RecordReaderDataSetIterator(recordReader, 100000, networkDescriptor.getNumberOfInputs(), networkDescriptor.getNumberOfOutputs());
    }

    public Optional<MultiLayerNetwork> trainNetwork(final MultiLayerConfiguration configuration,
                                                    final NetworkDescriptor networkDescriptor,
                                                    final DataSet trainingData) {
        return trainNetwork(configuration, networkDescriptor, trainingData, 1);
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
    public Optional<MultiLayerNetwork> trainNetwork(final MultiLayerConfiguration configuration,
                                                    final NetworkDescriptor networkDescriptor,
                                                    final DataSet trainingData,
                                                    final Integer iterationNumber) {
        Optional<MultiLayerNetwork> ret = Optional.empty();
        //
        // Compute the number of epochs at which the network will be periodically evaluated
        log.trace("trainNetwork(): training up to {} epochs", networkDescriptor.getMaxNumberOfTrainingEpochs());
        //
        // Create the model to be trained
        MultiLayerNetwork network = new MultiLayerNetwork(configuration);
        network.init();
        int epochCount;
        for (epochCount = 0; epochCount < networkDescriptor.getMaxNumberOfTrainingEpochs(); epochCount++) {
            // Shake it up
            trainingData.shuffle();
            // Copy original network parameters because with each training loop it gets mutated
            //
            // Fit the model against the training data for a single epoch
            network.fit(trainingData);
            networkDescriptor.setNumberOfEpochs(epochCount);
            if (epochCount % networkDescriptor.getNetworkEvaluationIndex() == 0) {
                Evaluation networkEvaluator = computeNetworkEvaluator(networkDescriptor.getNumberOfOutputs(), trainingData, network);
                double networkError = computeNetworkError(networkEvaluator);
                networkDescriptor.setTrainingError(networkError);
                networkDescriptor.setNetworkAccuracy(networkEvaluator.accuracy());
                networkDescriptor.setNetworkPrecision(networkEvaluator.precision());
                networkDescriptor.setNetworkRecall(networkEvaluator.recall());
                networkDescriptor.setNetworkF1Score(networkEvaluator.f1());
                log.trace("trainNetwork(): {}9930T: Network error at epoch {}: {}%", LOG_PREFIX, epochCount, 100.0 * networkError);
                if (networkError < networkDescriptor.getMaxNetworkError()) {
                    // Network is trained, we're done here
                    networkDescriptor.setNumberOfEpochs(epochCount);
                    networkDescriptor.setTrainingError(networkError);
                    networkDescriptor.setWhenTrained(LocalDateTime.now());
                    log.debug("trainNetwork(): {}9931D: Network trained at epoch {}, network error: {}%", LOG_PREFIX, epochCount, 100.0 * networkError);
                    log.debug(networkDescriptor.toStringOneLine());
                    log.debug(networkEvaluator.stats());
                    ret = Optional.of(network);
                    break;
                }
            }
        }
        log.debug(String.format("trainNetwork(...): {}9933D: Iteration %d, Number of epochs: %d, Network error: %2.4f%%, Normalizer: %s, " + "Updater: %s, " + "Activation: %s", iterationNumber, epochCount, computeNetworkError(computeNetworkEvaluator(networkDescriptor.getNumberOfOutputs(), trainingData, network)) * 100, networkDescriptor.getNormalizer(), UpdaterChoice.from(networkDescriptor
                .getUpdater()
                .getOrdinal()), networkDescriptor.getActivationFunction()));
        return ret;
    }

    /**
     * Compute the URL to the specified data file using the specified parameters.
     */
    @SuppressWarnings("unused")
    public String computeDataFileUrl(final AbstractSportsnetProperties sportsnetProperties,
                                     final String sportsSpecificUri, final String dataFileName,
                                     final ResourceType resourceType) {
        return computeDataFileUrl(sportsnetProperties, sportsSpecificUri, null, dataFileName, resourceType);
    }

    /**
     * Compute and return the complete URL to be used to make a REST call to fetch a
     * data file resource from the resource server.
     */
    public String computeDataFileUrl(final AbstractSportsnetProperties sportsnetProperties,
                                     final String sportsSpecificUri, final Integer year, final String dataFileName,
                                     final ResourceType resourceType) {
        return String.format("%s://%s%s%s%s/%s%s",
                //_____________1____2_3_4_5__6_7&8
                sportsnetProperties.getSportsnetResourceServiceProtocol(), // 1
                sportsnetProperties.getSportsnetResourceServiceHost(), // 2
                (sportsnetProperties.getSportsnetResourceServicePort() == null) ? StringUtils.EMPTY : ":" + sportsnetProperties.getSportsnetResourceServicePort(), sportsnetProperties.getSportsnetResourceServiceUri(), // 4
                sportsSpecificUri, // 5
                dataFileName, // 6
                // Query params (note: year may be null)
                String.format("?resourceType=%s%s", resourceType, // 7
                        (year != null) ? "&year=" + year : StringUtils.EMPTY)); // 8
    }

    /**
     * Fetches the specified file from the resource server and returns its contents
     * to the caller as a 2D string array. All resources used by sportsnet are text files.
     * <p>
     * The REST call is via RestTemplate.
     *
     * @param fileNameUrl The full path (URL) to the file (resource)
     * @return The contents of the specified text file if present, an empty optional if the file
     * could not be found on the resource server.
     */
    public Optional<ResourceFileWrapperDto> fetchDataFileFromResourceServer(final String fileNameUrl) {
        Optional<ResourceFileWrapperDto> ret;
        //
        // Format the URL string
        log.debug("fetchDataFileFromResourceServer(): Calling URL: {}", fileNameUrl);
        //
        // Make the REST call and process the response
        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<ResourceFileWrapperDto> responseEntity = restTemplate.exchange(fileNameUrl, HttpMethod.GET, null, ResourceFileWrapperDto.class);
            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                if (log.isTraceEnabled()) {
                    log.trace("fetchDataFileFromResourceServer(): Response: {}", responseEntity);
                }
                ret = Optional.ofNullable(responseEntity.getBody());
            } else {
                String errorMessage = String.format("%s0085E: Exception making REST call to %s: %s", LOG_PREFIX, fileNameUrl, responseEntity.getStatusCode());
                log.error("fetchDataFileFromResourceServer(): {}", errorMessage);
                throw new InternalServerErrorException(errorMessage);
            }
        } catch (ResourceAccessException e) {
            String errorMessage = String.format("%s0087E: Exception occurred while connecting to the resource server: %s. Is the resource " +
                    "server down or not started, maybe?", LOG_PREFIX, e.getLocalizedMessage());
            log.error("fetchDataFileFromResourceServer(): {}", errorMessage, e);
            throw new InternalServerErrorException(errorMessage, e);
        }
        return ret;
    }

    /**
     * Compute and return the complete URL to be used to make a REST call to fetch a
     * network file resource on the resource server.
     */
    public String computeNetworkFileUrl(final AbstractSportsnetProperties sportsnetProperties,
                                        final String sportsSpecificUri, final UUID networkId,
                                        final ResourceType resourceType) {
        // Example http://localhost:8085/resources/nfl/bd9f8e30-08d3-471e-a954-e24fea23bca1?resourceType=DL4J_NETWORK
        return String.format("%s://%s%s%s%s/networks/%s?resourceType=%s",
                //_____________1____2_3_4_5__6_7
                sportsnetProperties.getSportsnetResourceServiceProtocol(), // 1
                sportsnetProperties.getSportsnetResourceServiceHost(), // 2
                (sportsnetProperties.getSportsnetResourceServicePort() == null) ? StringUtils.EMPTY : ":" + sportsnetProperties.getSportsnetResourceServicePort(), sportsnetProperties.getSportsnetResourceServiceUri(), // 4
                sportsSpecificUri, // 5
                networkId.toString(), // 6
                resourceType); // 7
    }

    /**
     * Compute and return the complete URL to be used to make a REST call to fetch a
     * network file resource on the resource server.
     */
    public String computeResourceServiceNetworksUrl(final AbstractSportsnetProperties sportsnetProperties,
                                                    final String sportsSpecificUri, final ResourceType resourceType) {
        // Example http://localhost:8085/resources/nfl?resourceType=NETWORK_DESCRIPTOR
        return String.format("%s://%s%s%s%s/networks?resourceType=%s",
                //_____________1____2_3_4_5_6
                sportsnetProperties.getSportsnetResourceServiceProtocol(), // 1
                sportsnetProperties.getSportsnetResourceServiceHost(), // 2
                (sportsnetProperties.getSportsnetResourceServicePort() == null) ? StringUtils.EMPTY : ":" + sportsnetProperties.getSportsnetResourceServicePort(), sportsnetProperties.getSportsnetResourceServiceUri(), // 4
                sportsSpecificUri, // 5
                resourceType); // 6
    }

    /**
     * Saves the specified network file on the resource server and returns its network
     * descriptor.
     * <p>
     * The REST call is via RestTemplate.
     *
     * @param networkFileUrl The full path (URL) to the network file (resource)
     * @return The network descriptor of the saved network if successful. If the call is not
     * successful an exception is thrown.
     */
    public NetworkDescriptor saveNetworkOnResourceServer(final String networkFileUrl,
                                                         final NetworkCandidate networkCandidate) {
        final NetworkDescriptor ret;
        //
        // Format the URL string
        log.debug("saveNetworkOnResourceServer(): Calling URL: {}", networkFileUrl);
        //
        // Make the REST call and process the response
        try {
            RestTemplate restTemplate = new RestTemplate();
            NetworkCandidateDto networkCandidateDto = networkCandidate.toDto();
            HttpEntity<NetworkCandidateDto> requestEntity = new HttpEntity<>(networkCandidateDto);
            ResponseEntity<NetworkDescriptorDto> responseEntity = restTemplate.postForEntity(networkFileUrl, requestEntity, NetworkDescriptorDto.class);
            if (responseEntity.getStatusCode() == HttpStatus.CREATED) {
                log.debug("saveNetworkOnResourceServer(): Response: {}", responseEntity);
                if (responseEntity.hasBody() && responseEntity.getBody() != null) {
                    NetworkDescriptorDto body = responseEntity.getBody();
                    log.debug("saveNetworkOnResourceServer(): Response body: '{}'", body);
                    //
                    // Transform to correct type
                    ret = NetworkDescriptor.fromDto(body);
                } else {
                    String errorMessage = String.format("%s0089E: Empty response body not expected when calling URL: %s", LOG_PREFIX, networkFileUrl);
                    log.error("saveNetworkOnResourceServer(): {}", errorMessage);
                    throw new InternalServerErrorException(errorMessage);
                }
            } else {
                String errorMessage = String.format("%s0091E: Exception making REST call to %s: %s", LOG_PREFIX, networkFileUrl, responseEntity.getStatusCode());
                log.error("saveNetworkOnResourceServer(): {}", errorMessage);
                throw new InternalServerErrorException(errorMessage);
            }
        } catch (ResourceAccessException e) {
            String errorMessage = String.format("%s0093E: Exception occurred while connecting to the resource server: %s. Is the resource " +
                    "server down or not started, maybe?", LOG_PREFIX, e.getLocalizedMessage());
            log.error("saveNetworkOnResourceServer(): {}", errorMessage, e);
            throw new InternalServerErrorException(errorMessage, e);
        }
        return ret;
    }

    /**
     * Convert the MultiLayer network contained in the specified NetworkCandidate object
     * to a byte array. This is probably for use in sending the object to the server.
     * <p>
     * The TrainedNetworkParameters object contained in the NetworkCandidate is dropped
     * on the floor.
     * <p>
     * This currently only works for DL4J networks.
     */
    public static byte[] convertNetworkCandidateToByteArray(final NetworkCandidate networkCandidate) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        if (networkCandidate != null) {
            try {
                if (networkCandidate.getMultiLayerNetwork() != null) {
                    ModelSerializer.writeModel(networkCandidate.getMultiLayerNetwork(), byteArrayOutputStream, true);
                }
            } catch (IOException e) {
                String errorMessage = String.format("%s0095E: Error serializing NetworkCandidate model: %s", LOG_PREFIX, networkCandidate);
                log.error("convertNetworkCandidateToByteArray(): {}", errorMessage);
                throw new InternalServerErrorException(errorMessage);
            }
        }
        return byteArrayOutputStream.toByteArray();
    }

    public static NetworkCandidate reconstituteNetworkFromComponents(final NetworkDescriptor networkDescriptor,
                                                                     final byte[] networkCandidateByteArray) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(networkCandidateByteArray);
        try {
            MultiLayerNetwork multiLayerNetwork = null;
            //noinspection ConstantConditions
            if (networkCandidateByteArray != null) {
                multiLayerNetwork = ModelSerializer.restoreMultiLayerNetwork(byteArrayInputStream);
            }
            return new NetworkCandidate(networkDescriptor, multiLayerNetwork);
        } catch (IOException e) {
            String errorMessage = String.format("%s0097E: Error reconstituting network from components: networkDescriptor=%s",
                    LOG_PREFIX, networkDescriptor);
            log.error("reconstituteNetworkFromComponents(): {}", errorMessage);
            throw new InternalServerErrorException(errorMessage);
        }
    }

    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        ObjectMapper mapper = new ObjectMapper()
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return new MappingJackson2HttpMessageConverter(mapper);
    }

    public String computeResourceServerUrl(final AbstractSportsnetProperties sportsnetProperties,
                                           final ResourceType resourceType) {
        return String.format("%s://%s%s%s?resourceType=%s",
                //_____________1____2_3_4_5
                sportsnetProperties.getSportsnetResourceServiceProtocol(), // 1
                sportsnetProperties.getSportsnetResourceServiceHost(), // 2
                (sportsnetProperties.getSportsnetResourceServicePort() == null) ? StringUtils.EMPTY : ":" + sportsnetProperties.getSportsnetResourceServicePort(), sportsnetProperties.getSportsnetResourceServiceUri(), // 4
                resourceType); // 5
    }

    public List<NetworkCandidateDto> fetchAllNetworks(final String url) {
        final List<NetworkCandidateDto> networkCandidateDtos;
        RestTemplate restTemplate = new RestTemplate();
        try {
            List<NetworkCandidateDto> dtos;
            ResponseEntity<List<NetworkCandidateDto>> responseEntity = restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {
            });
            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                networkCandidateDtos = responseEntity.getBody();
            } else {
                String errorMessage = String.format("%s0099E: Exception making REST call to %s: %s", LOG_PREFIX, url, responseEntity.getStatusCode());
                log.error("fetchAllNetworks(): {}", errorMessage);
                throw new InternalServerErrorException(errorMessage);
            }
        } catch (Exception e) {
            String errorMessage = String.format("%s0101E: Exception occurred while connecting to the server: %s. Is the server down or not " +
                    "started, maybe?", LOG_PREFIX, e.getLocalizedMessage());
            log.error("fetchAllNetworks(): {}", errorMessage, e);
            throw new InternalServerErrorException(errorMessage, e);
        }
        return networkCandidateDtos;
    }

    public Optional<NetworkCandidateDto> fetchNetworkFileFromResourceService(final String url) {
        final Optional<NetworkCandidateDto> response;
        log.debug("fetchNetworkFileFromResourceService({}): Fetching network file...", url);
        long startTime = System.currentTimeMillis();

        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<NetworkCandidateDto> responseEntity = restTemplate.getForEntity(url, NetworkCandidateDto.class);
            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                response = Optional.ofNullable(responseEntity.getBody());
            } else {
                String errorMessage = String.format("%s0103E: Exception making REST call to %s: %s", LOG_PREFIX, url, responseEntity.getStatusCode());
                log.error("fetchNetworkFileFromResourceService(): {}", errorMessage);
                throw new InternalServerErrorException(errorMessage);
            }
        } catch (Exception e) {
            String errorMessage = String.format("%s0105E: Exception occurred while fetching network from resource service: %s, at url %s. " +
                    "Is the server down or not started, maybe?", LOG_PREFIX, e.getLocalizedMessage(), url);
            log.error("fetchNetworkFileFromResourceService({}): {}", url, errorMessage, e);
            throw new InternalServerErrorException(errorMessage, e);
        }

        log.debug("fetchNetworkFileFromResourceService({}): Done in {}ms", url, System.currentTimeMillis() - startTime);
        return response;
    }

    public String computeDataFileUrl(final AbstractSportsnetProperties sportsnetProperties,
                                     final String dataFileName,
                                     final ResourceType resourceType) {
        // Example http://ix:8299/resources/network.properties?resourceType=PROPERTIES_FILE
        return String.format("%s://%s%s%s/%s?resourceType=%s",
                //_____________1____2_3_4__5_6
                sportsnetProperties.getSportsnetResourceServiceProtocol(), // 1
                sportsnetProperties.getSportsnetResourceServiceHost(), // 2
                (sportsnetProperties.getSportsnetResourceServicePort() == null) ? StringUtils.EMPTY : ":" + sportsnetProperties.getSportsnetResourceServicePort(), sportsnetProperties.getSportsnetResourceServiceUri(), // 4
                dataFileName, resourceType); // 6
    }

    /**
     * Returns true if the specified year was not used to train
     * the network (otherwise that just wouldn't be fair), false otherwise.
     * <p>
     */
    public boolean isValidYearForTournamentPrediction(final Integer tournamentYear,
                                                      final NetworkDescriptor networkDescriptor) {
        return isValidYearForTournamentPrediction(tournamentYear, networkDescriptor, false);
    }

    /**
     * Returns true if the specified year was not used to train
     * the network (otherwise that just wouldn't be fair), false otherwise.
     * If allowEvaluationYears is true, then a network can be used to produce a
     * tournament prediction even if one of its evaluation years was used to
     * determine whether or not it was kept (there are not enough years of data
     * for some sports to allow three separate groups: training, evaluation, and
     * tournament prediction).
     * <p>
     */
    public boolean isValidYearForTournamentPrediction(final Integer tournamentYear,
                                                      final NetworkDescriptor networkDescriptor,
                                                      final Boolean allowEvaluationYears) {
        boolean ret = true;
        //
        // Sanity check
        List<Integer> trainingYears = networkDescriptor.getYearsToTrainNetwork();
        List<Integer> evaluationYears = networkDescriptor.getYearsToEvaluateNetwork();
        if (CollectionUtils.containsAny(trainingYears, evaluationYears)) {
            String errorMessage = String.format("%s0107E: Current network is not valid for tournament prediction, reason: training years and " +
                            "evaluation years overlap: TRN=%s, EVAL=%s", LOG_PREFIX, StringUtils.join(trainingYears, ','),
                    StringUtils.join(evaluationYears, ','));
            log.error("isValidYearForTournamentPrediction({}): {}", tournamentYear, errorMessage);
            ret = false;
        }
        if (trainingYears.contains(tournamentYear)) {
            String message = String.format("%s2222W: Current network is not valid for tournament prediction " +
                    "based on its training years: %s", LOG_PREFIX, StringUtils.join(trainingYears.toArray(), ", "));
            log.warn("isValidYearForTournamentPrediction({}): {}", tournamentYear, message);
            ret = false;
        }
        if (!allowEvaluationYears && evaluationYears.contains(tournamentYear)) {
            log.warn("isValidYearForTournamentPrediction({}): current network is not valid for tournament prediction " + "based on its evaluation years: {}", tournamentYear, StringUtils.join(networkDescriptor
                    .getYearsToEvaluateNetwork()
                    .toArray(), ", "));

            ret = false;
        }
        return ret;
    }

    /**
     * Computes the URL to the management service at the subUri specified.
     *
     * @param sportsnetProperties Sportsnet properties for retrieving the configured host, port, etc
     * @param managementSubUri    The sub-uri to the final service to call
     */
    public String computeManagementServiceUrl(final AbstractSportsnetProperties sportsnetProperties,
                                              final String managementSubUri) {
        // Example http://ix:8299/management/events
        return String.format(
                "%s://%s%s%s%s",
                //_____________1____2_3_4_5
                sportsnetProperties.getSportsnetManagementServiceProtocol(), // 1
                sportsnetProperties.getSportsnetManagementServiceHost(), // 2
                (sportsnetProperties.getSportsnetManagementServicePort() == null) // 3
                        ? StringUtils.EMPTY : ":" + sportsnetProperties.getSportsnetResourceServicePort(),
                sportsnetProperties.getSportsnetManagementServiceUri(), // 4
                managementSubUri); // 5
    }

    /**
     * Calls the management service to create the specified ManagementEvent. This is the
     * interface used by services and other components to create management events that can
     * be queried and displayed by tools.
     *
     * @param url             The URL to the management service endpoint to create the event
     * @param managementEvent The payload for the request
     * @return The original event, decorated by the server with eventId and possibly other fields
     */
    public ManagementEvent createManagementEvent(final String url,
                                                 final ManagementEvent managementEvent) {
        final ManagementEvent response;
        long startTime = System.currentTimeMillis();

        try {
            final RestTemplate restTemplate = new RestTemplate();
            managementEvent.setSystemTimeMillis(System.currentTimeMillis());
            final HttpEntity<ManagementEventDto> payload = new HttpEntity<>(managementEvent.toDto());
            final ResponseEntity<ManagementEventDto> responseEntity = restTemplate.postForEntity(url, payload, ManagementEventDto.class);
            if (responseEntity.getStatusCode() == HttpStatus.CREATED) {
                log.debug("createEvent(): Response: {}", responseEntity.getStatusCode());
                if (responseEntity.getBody() != null) {
                    response = ManagementEvent.fromDto(responseEntity.getBody());
                } else {
                    final String errorMessage = String.format("%s0109E: Response body empty from call to %s: status = %s", LOG_PREFIX, url, responseEntity.getStatusCode());
                    log.error("createEvent(): {}", errorMessage);
                    throw new InternalServerErrorException(errorMessage);
                }
            } else {
                final String errorMessage = String.format("%s0111E: Exception making REST call to %s: %s", LOG_PREFIX, url, responseEntity.getStatusCode());
                log.error("createEvent(): {}", errorMessage);
                throw new InternalServerErrorException(errorMessage);
            }
        } catch (Exception e) {
            final String errorMessage = String.format("%s0113E: Exception occurred while fetching network from resource service: %s, at url %s. " +
                    "Is the server down or not started, maybe?", LOG_PREFIX, e.getLocalizedMessage(), url);
            log.error("createEvent({}): {}", url, errorMessage, e);
            throw new InternalServerErrorException(errorMessage, e);
        }

        log.debug("createEvent({}): Done in {}ms", url, System.currentTimeMillis() - startTime);
        return response;
    }

    public ManagementEvent createManagementEventPayload(final UUID serviceId) {
        final String hostName;
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            String errorMessage = String.format("%s0114E: Exception while getting localhost name: %s", LOG_PREFIX, e.getLocalizedMessage());
            log.error("createManagementEvent(): {}", errorMessage, e);
            throw new InternalServerErrorException(errorMessage, e);
        }
        return new ManagementEvent()
                .setServiceId(serviceId)
                .setHostName(hostName);
    }

    /**
     * Fetch all management events from the management server. The events will be returned
     * in no particular order and it is up to the caller to sort them.
     *
     * @return A list of all ManagementEvent objects currently under management by the
     * management server.
     */
    public List<ManagementEvent> fetchAllManagementEvents(final AbstractSportsnetProperties sportsnetProperties) {
        final List<ManagementEvent> response;
        long startTime = System.currentTimeMillis();

        final String url = computeManagementServiceUrl(sportsnetProperties, sportsnetProperties.getSportsnetServiceUriEvents());

        try {
            final RestTemplate restTemplate = new RestTemplate();
            final ResponseEntity<List<ManagementEventDto>> responseEntity = restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {
            });
            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                log.debug("fetchManagementEvents(): Response: {}", responseEntity.getStatusCode());
                if (responseEntity.getBody() != null) {
                    response = responseEntity.getBody().stream()
                            .map(ManagementEvent::fromDto)
                            .collect(Collectors.toList());
                } else {
                    final String errorMessage = String.format("%s0000E: Response body empty from call to %s: status = %s", LOG_PREFIX, url, responseEntity.getStatusCode());
                    log.error("fetchManagementEvents(): {}", errorMessage);
                    throw new InternalServerErrorException(errorMessage);
                }
            } else {
                final String errorMessage = String.format("%s0000E: Exception making REST call to %s: %s", LOG_PREFIX, url, responseEntity.getStatusCode());
                log.error("fetchManagementEvents(): {}", errorMessage);
                throw new InternalServerErrorException(errorMessage);
            }
        } catch (Exception e) {
            final String errorMessage = String.format("%s0000E: Exception occurred while calling management service: %s, at url %s. " +
                    "Is the server down or not started, maybe?", LOG_PREFIX, e.getLocalizedMessage(), url);
            log.error("fetchManagementEvents({}): {}", url, errorMessage, e);
            throw new InternalServerErrorException(errorMessage, e);
        }

        log.debug("fetchManagementEvents({}): Fetched {} events in {}ms", url, response.size(), System.currentTimeMillis() - startTime);
        return response;
    }

    /**
     * Fetch all management events from the management server. The events will be returned
     * in no particular order and it is up to the caller to sort them.
     *
     * @return A list of all ManagementEvent objects currently under management by the
     * management server.
     */
    public List<ManagementEventSummary> fetchAllManagementEventSummaries(final AbstractSportsnetProperties sportsnetProperties,
                                                                         final String sport) {
        final List<ManagementEventSummary> response;
        long startTime = System.currentTimeMillis();

        final String baseUrl = computeManagementServiceUrl(sportsnetProperties, sportsnetProperties.getSportsnetServiceUriStats());
        final String url = String.format("%s?sport=%s", baseUrl, sport);

        try {
            final RestTemplate restTemplate = new RestTemplate();
            final ResponseEntity<List<ManagementEventSummaryDto>> responseEntity = restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {
            });
            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                log.debug("fetchAllManagementEventSummaries(): Response: {}", responseEntity.getStatusCode());
                if (responseEntity.getBody() != null) {
                    response = responseEntity.getBody().stream()
                            .map(ManagementEventSummary::fromDto)
                            .collect(Collectors.toList());
                } else {
                    final String errorMessage = String.format("%s0000E: Response body empty from call to %s: status = %s", LOG_PREFIX, url, responseEntity.getStatusCode());
                    log.error("fetchAllManagementEventSummaries(): {}", errorMessage);
                    throw new InternalServerErrorException(errorMessage);
                }
            } else {
                final String errorMessage = String.format("%s0000E: Exception making REST call to %s: %s", LOG_PREFIX, url, responseEntity.getStatusCode());
                log.error("fetchAllManagementEventSummaries(): {}", errorMessage);
                throw new InternalServerErrorException(errorMessage);
            }
        } catch (Exception e) {
            final String errorMessage = String.format("%s0000E: Exception occurred while calling management service: %s, at url %s. " +
                    "Is the server down or not started, maybe?", LOG_PREFIX, e.getLocalizedMessage(), url);
            log.error("fetchAllManagementEventSummaries({}): {}", url, errorMessage, e);
            throw new InternalServerErrorException(errorMessage, e);
        }

        log.debug("fetchAllManagementEventSummaries({}): Fetched {} event summaries in {}ms", url, response.size(), System.currentTimeMillis() - startTime);
        return response;
    }

    /**
     * Given the list of ManagementEvent, this method computes and returns a single ManagementEvent that
     * contains the average of all properties for which this makes sense.
     *
     * @param managementEvents The list of ManagementEvent to aggregate
     * @return The ManagementEvent containing the aggregate results
     */
    public List<ManagementEventSummary> computeAverageStats(final List<ManagementEvent> managementEvents) {
        final List<ManagementEventSummary> managementEventSummaries = new ArrayList<>();
        //
        // Sort the list of ManagementEvent by the four-part key
        managementEvents.sort(ManagementEvent::compareTo);
        final Map<UUID, ManagementEventSummary> correlationIdEventSummaryMap = new HashMap<>();
        //
        // Stream through the Map and aggregate by the ManagementEventSummary.correlationId attribute
        managementEvents.forEach(managementEvent -> {
            final UUID correlationId = managementEvent.getCorrelationId();
            final ManagementEventSummary managementEventSummary =
                    correlationIdEventSummaryMap.computeIfAbsent(correlationId, unused -> createNewManagementEventSummary(managementEvent));
            updateManagementEventSummary(managementEventSummary, managementEvent);
        });
        //
        // Now run through the map and generate the return value
        final Set<UUID> correlationIds = correlationIdEventSummaryMap.keySet();
        correlationIds.forEach(correlationId -> managementEventSummaries.add(correlationIdEventSummaryMap.get(correlationId)));

        log.debug("computeAverageStats(): {} ManagementEventSummary computed from {} ManagementEvent", managementEventSummaries.size(), managementEvents.size());
        return managementEventSummaries;
    }

    /**
     * Creates and initializes, and returns a new ManagementEventSummary object using the contents
     * of the specified ManagementEvent that makes up its "key".
     *
     * @param managementEvent The ManagementEvent to use to boot up the ManagementEventSummary
     * @return The newly created ManagementEventSummary
     */
    private ManagementEventSummary createNewManagementEventSummary(final ManagementEvent managementEvent) {
        return new ManagementEventSummary()
                .setCorrelationId(managementEvent.getCorrelationId())
                .setHostName(managementEvent.getHostName())
                .setServiceId(managementEvent.getServiceId())
                .setSport(managementEvent.getSport())
                .setTotalIterationTime(0L)
                .setTotalNetworkAccuracy(0.0)
                .setTotalNetworkPrecision(0.0)
                .setTotalNetworkRecall(0.0)
                .setTotalF1Score(0.0)
                .setTotalRealNetworkAccuracy(0.0)
                .setNumberOfEvents(0L)
                .setNumberOfSavedNetworks(0)
                .setStatus(ManagementEventStatus.UNKNOWN)
                .setWhenStarted(null)
                ;
    }

    /**
     * Updates the specified ManagementEventSummary object with the appropriate data from the list of
     * ManagementEvents that is passed in. The ManagementEventSummary is updated in place.
     *
     * @param aggregate       The ManagmentEventSumary that holds the aggregate values, updated in place
     * @param managementEvent The ManagementEvent whose information is aggregated with the
     *                        existing state of the specified ManagementEventSummary
     */
    private void updateManagementEventSummary(final ManagementEventSummary aggregate,
                                              final ManagementEvent managementEvent) {
        //
        // Aggregate field values, depending on the event type
        final ManagementEventType eventType = managementEvent.getManagementEventType();
        switch (eventType) {
            case TRAIN_NETWORK_BATCH_START, TRAIN_NETWORK_START -> {
                aggregate.setWhenStarted(LocalDateTime.ofInstant(Instant.ofEpochMilli(managementEvent.getSystemTimeMillis()), ZoneId.systemDefault()));
                aggregate.setStatus(ManagementEventStatus.RUNNING);
            }
            case TRAIN_NETWORK_BATCH_ITERATION_END -> {
                computeAggregateStats(aggregate, managementEvent);
                computeEventSuperlatives(aggregate, managementEvent);
            }
            case SAVE_NETWORK -> aggregate.setNumberOfSavedNetworks(aggregate.getNumberOfSavedNetworks() + 1);
            case TRAIN_NETWORK_BATCH_END -> {
                aggregate.setWhenFinished(LocalDateTime.ofInstant(Instant.ofEpochMilli(managementEvent.getSystemTimeMillis()), ZoneId.systemDefault()));
                aggregate.setStatus(ManagementEventStatus.FINISHED);
            }
            case TRAIN_NETWORK_END -> {
                aggregate.setWhenFinished(LocalDateTime.ofInstant(Instant.ofEpochMilli(managementEvent.getSystemTimeMillis()), ZoneId.systemDefault()));
                aggregate.setStatus(ManagementEventStatus.FINISHED);
                computeAggregateStats(aggregate, managementEvent);
            }
        }
    }

    /**
     * Given the ManagementEventSumary that holds the superlative values computed thus far,
     * check the specified ManagementEvent to see if any of its values contain new superlatives
     * and if so use those
     *
     * @param aggregate       The ManagmentEventSumary that holds the aggregate values, updated in place
     * @param managementEvent The ManagementEvent whose information is aggregated with the
     *                        existing state of the specified ManagementEventSummary
     */
    private void computeEventSuperlatives(final ManagementEventSummary aggregate, final ManagementEvent managementEvent) {
        final NetworkDescriptor networkDescriptor = managementEvent.getNetworkDescriptor();
        // Elapsed time
        final ManagementEvent slowestBatchIterationTimeEvent = aggregate.getSlowestBatchIterationTimeEvent();
        if (slowestBatchIterationTimeEvent == null || managementEvent.getElapsedTimeMillis() > slowestBatchIterationTimeEvent.getElapsedTimeMillis()) {
            aggregate.setSlowestBatchIterationTimeEvent(managementEvent);
        }
        final ManagementEvent fastestBatchIterationTimeEvent = aggregate.getFastestBatchIterationTimeEvent();
        if (fastestBatchIterationTimeEvent == null || managementEvent.getElapsedTimeMillis() < fastestBatchIterationTimeEvent.getElapsedTimeMillis()) {
            aggregate.setFastestBatchIterationTimeEvent(managementEvent);
        }
        // Network Accuracy
        final ManagementEvent lowestNetworkAccuracyEvent = aggregate.getLowestNetworkAccuracyEvent();
        if (lowestNetworkAccuracyEvent == null || networkDescriptor.getNetworkAccuracy() < lowestNetworkAccuracyEvent.getNetworkDescriptor().getNetworkAccuracy()) {
            aggregate.setLowestNetworkAccuracyEvent(managementEvent);
        }
        final ManagementEvent highestNetworkAccuracyEvent = aggregate.getHighestNetworkAccuracyEvent();
        if (highestNetworkAccuracyEvent == null || networkDescriptor.getNetworkAccuracy() > highestNetworkAccuracyEvent.getNetworkDescriptor().getNetworkAccuracy()) {
            aggregate.setHighestNetworkAccuracyEvent(managementEvent);
        }
        // Network Precision
        final ManagementEvent lowestNetworkPrecisionEvent = aggregate.getLowestNetworkPrecisionEvent();
        if (lowestNetworkPrecisionEvent == null || networkDescriptor.getNetworkPrecision() < lowestNetworkPrecisionEvent.getNetworkDescriptor().getNetworkPrecision()) {
            aggregate.setLowestNetworkPrecisionEvent(managementEvent);
        }
        final ManagementEvent highestNetworkPrecisionEvent = aggregate.getHighestNetworkPrecisionEvent();
        if (highestNetworkPrecisionEvent == null || networkDescriptor.getNetworkPrecision() > highestNetworkPrecisionEvent.getNetworkDescriptor().getNetworkPrecision()) {
            aggregate.setHighestNetworkPrecisionEvent(managementEvent);
        }
        // Network Recall
        final ManagementEvent lowestNetworkRecallEvent = aggregate.getLowestNetworkRecallEvent();
        if (lowestNetworkRecallEvent == null || networkDescriptor.getNetworkRecall() < lowestNetworkRecallEvent.getNetworkDescriptor().getNetworkRecall()) {
            aggregate.setLowestNetworkRecallEvent(managementEvent);
        }
        final ManagementEvent highestNetworkRecallEvent = aggregate.getHighestNetworkRecallEvent();
        if (highestNetworkRecallEvent == null || networkDescriptor.getNetworkRecall() > highestNetworkRecallEvent.getNetworkDescriptor().getNetworkRecall()) {
            aggregate.setHighestNetworkRecallEvent(managementEvent);
        }
        // F1 Score
        final ManagementEvent lowestF1ScoreEvent = aggregate.getLowestF1ScoreEvent();
        if (lowestF1ScoreEvent == null || networkDescriptor.getNetworkF1Score() < lowestF1ScoreEvent.getNetworkDescriptor().getNetworkF1Score()) {
            aggregate.setLowestF1ScoreEvent(managementEvent);
        }
        final ManagementEvent highestF1ScoreEvent = aggregate.getHighestF1ScoreEvent();
        if (highestF1ScoreEvent == null || networkDescriptor.getNetworkF1Score() > highestF1ScoreEvent.getNetworkDescriptor().getNetworkF1Score()) {
            aggregate.setHighestF1ScoreEvent(managementEvent);
        }
        // Real Network Accuracy
        final ManagementEvent lowestRealNetworkAccuracyEvent = aggregate.getLowestRealNetworkAccuracyEvent();
        if (lowestRealNetworkAccuracyEvent == null || networkDescriptor.getRealNetworkAccuracy() < lowestRealNetworkAccuracyEvent.getNetworkDescriptor().getRealNetworkAccuracy()) {
            aggregate.setLowestRealNetworkAccuracyEvent(managementEvent);
        }
        final ManagementEvent highestRealNetworkAccuracyEvent = aggregate.getHighestRealNetworkAccuracyEvent();
        if (highestRealNetworkAccuracyEvent == null || networkDescriptor.getRealNetworkAccuracy() > highestRealNetworkAccuracyEvent.getNetworkDescriptor().getRealNetworkAccuracy()) {
            aggregate.setHighestRealNetworkAccuracyEvent(managementEvent);
        }
    }

    /**
     * Given the ManagementEventSummary that holds the aggregate values, use the information in the
     * ManagementEvent to compute the aggregate stats in place (that is, update the MES object that
     * is passed in and return void from this method).
     *
     * @param aggregate       The MES object to update
     * @param managementEvent The ManagementEvent whose stats are to be added to the aggregate totals
     */
    private void computeAggregateStats(final ManagementEventSummary aggregate, final ManagementEvent managementEvent) {
        final NetworkDescriptor networkDescriptor = managementEvent.getNetworkDescriptor();
        // Increment number of events
        aggregate.setNumberOfEvents(aggregate.getNumberOfEvents() + 1);
        // Add event elapsed time to aggregate total
        Long eventElapsedTime = managementEvent.getElapsedTimeMillis();
        aggregate.setTotalIterationTime(aggregate.getTotalIterationTime() + eventElapsedTime);
        // Add event accuracy to aggregate total
        Double eventNetworkAccuracy = networkDescriptor.getNetworkAccuracy();
        aggregate.setTotalNetworkAccuracy(aggregate.getTotalNetworkAccuracy() + eventNetworkAccuracy);
        // Add event precision to aggregate total
        Double eventNetworkPrecision = networkDescriptor.getNetworkPrecision();
        aggregate.setTotalNetworkPrecision(aggregate.getTotalNetworkPrecision() + eventNetworkPrecision);
        // Add event recall to aggregate total
        Double eventNetworkRecall = networkDescriptor.getNetworkRecall();
        aggregate.setTotalNetworkRecall(aggregate.getTotalNetworkRecall() + eventNetworkRecall);
        // Add event f1 score to aggregate total
        Double eventNetworkF1Score = networkDescriptor.getNetworkF1Score();
        aggregate.setTotalF1Score(aggregate.getTotalF1Score() + eventNetworkF1Score);
        // Add event real network accuracy to aggregate total
        Double eventRealNetworkAccuracy = networkDescriptor.getRealNetworkAccuracy();
        aggregate.setTotalRealNetworkAccuracy(aggregate.getTotalRealNetworkAccuracy() + eventRealNetworkAccuracy);
    }

    public String computeCompletionTime(final int totalNumberOfThingsToProcess,
                                        final int numberOfThingsProcessedSoFar,
                                        final long startTimeMillis) {
        long elapsedTimeSoFar = System.currentTimeMillis() - startTimeMillis;
        double averageMillisPerThing = (double) elapsedTimeSoFar / numberOfThingsProcessedSoFar;
        int numberOfThingsLeftToProcess = totalNumberOfThingsToProcess - numberOfThingsProcessedSoFar;

        long estimateMillisRemaining = (long)averageMillisPerThing * numberOfThingsLeftToProcess;
        long estimatedCompletionMillis = System.currentTimeMillis() + estimateMillisRemaining;

        LocalDateTime estimatedCompletionTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(estimatedCompletionMillis), ZoneId.systemDefault());

        return estimatedCompletionTime.format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss")) + "(" + estimateMillisRemaining + "ms)";
    }
}
