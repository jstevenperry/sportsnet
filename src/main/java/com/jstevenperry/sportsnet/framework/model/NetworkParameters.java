/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.model;

import com.jstevenperry.sportsnet.framework.menus.AbstractDataElementChoice;
import com.jstevenperry.sportsnet.framework.menus.DataNormalizationMenu;
import com.jstevenperry.sportsnet.framework.menus.UpdaterMenu;
import org.apache.commons.lang3.StringUtils;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.IUpdater;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NetworkParameters implements Serializable {

    public static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(NetworkParameters.class);

    private Integer numberOfInputs;
    private Integer numberOfOutputs;
    private List<List<Integer>> yearsToTrainAndEvaluateNetwork = new ArrayList<>();
    private String networkLayout;
    private Activation activationFunction;
    private LossFunctions.LossFunction lossFunction;
    private Integer numberOfEpochs;
    private WeightInit weightInit;
    private IUpdater updater;
    private List<AbstractDataElementChoice> selectedElements;
    private LocalDateTime whenTrained;
    private double networkAccuracy;
    private boolean networkSaved;
    private double l2RegularizationLambda = 0.0005; // Do not allow this to be zero
    private DataNormalizationMenu.DataNormalizationChoice normalizer;
    private String networkType;
    private double dropoutProbability;

    private static String listToCsvString(final List<Integer> integerList) {
        String ret = StringUtils.EMPTY;
        if (integerList.size() > 0) {
            String[] years = new String[integerList.size()];
            for (int index = 0; index < years.length; index++) {
                years[index] = integerList.get(index).toString();
            }
            ret = String.join(",", years);
        }
        return ret;
    }

    public NetworkParameters copy() {
        NetworkParameters ret = new NetworkParameters()
                .setNetworkSaved(isNetworkSaved())
                .setActivationFunction(getActivationFunction())
                .setLossFunction(getLossFunction())
                .setNetworkAccuracy(getNetworkAccuracy())
                .setNetworkLayout(getNetworkLayout())
                .setNumberOfEpochs(getNumberOfEpochs())
                .setNumberOfInputs(getNumberOfInputs())
                .setNumberOfOutputs(getNumberOfOutputs())
                .setSelectedElements(getSelectedElements())
                .setUpdater(getUpdater())
                .setWeightInit(getWeightInit())
                .setWhenTrained(getWhenTrained())
                .setL2RegularizationLambda(getL2RegularizationLambda())
                .setNormalizer(getNormalizer())
                .setNetworkType(getNetworkType())
                .setDropoutProbability(getDropoutProbability());
        ret.getYearsToTrainAndEvaluateNetwork().get(0).addAll(getYearsToTrainAndEvaluateNetwork().get(0));
        ret.getYearsToTrainAndEvaluateNetwork().get(1).addAll(getYearsToTrainAndEvaluateNetwork().get(1));
        return ret;
    }

    public List<List<Integer>> getYearsToTrainAndEvaluateNetwork() {
        if (yearsToTrainAndEvaluateNetwork == null || yearsToTrainAndEvaluateNetwork.isEmpty()) {
            yearsToTrainAndEvaluateNetwork = new ArrayList<>();
            yearsToTrainAndEvaluateNetwork.add(new ArrayList<>()); // train
            yearsToTrainAndEvaluateNetwork.add(new ArrayList<>()); // evaluate
        }
        return yearsToTrainAndEvaluateNetwork;
    }

    public List<Integer> getYearsToTrainNetwork() {
        return yearsToTrainAndEvaluateNetwork.get(0);
    }

    public List<Integer> getYearsToEvaluateNetwork() {
        return yearsToTrainAndEvaluateNetwork.get(1);
    }

    public String getNetworkLayout() {
        return networkLayout;
    }

    public NetworkParameters setNetworkLayout(final String networkLayout) {
        this.networkLayout = networkLayout;
        return this;
    }

    public Activation getActivationFunction() {
        return activationFunction;
    }

    public NetworkParameters setActivationFunction(final Activation activationFunction) {
        this.activationFunction = activationFunction;
        return this;
    }

    public LossFunctions.LossFunction getLossFunction() {
        return lossFunction;
    }

    public NetworkParameters setLossFunction(final LossFunctions.LossFunction lossFunction) {
        this.lossFunction = lossFunction;
        return this;
    }

    public Integer getNumberOfEpochs() {
        return numberOfEpochs;
    }

    public NetworkParameters setNumberOfEpochs(final Integer numberOfEpochs) {
        this.numberOfEpochs = numberOfEpochs;
        return this;
    }

    public WeightInit getWeightInit() {
        return weightInit;
    }

    public NetworkParameters setWeightInit(final WeightInit weightInit) {
        this.weightInit = weightInit;
        return this;
    }

    public IUpdater getUpdater() {
        return updater;
    }

    public NetworkParameters setUpdater(final IUpdater updater) {
        this.updater = updater;
        return this;
    }

    public Integer getNumberOfInputs() {
        return numberOfInputs;
    }

    public NetworkParameters setNumberOfInputs(final Integer numberOfInputs) {
        this.numberOfInputs = numberOfInputs;
        return this;
    }

    public Integer getNumberOfOutputs() {
        return numberOfOutputs;
    }

    public NetworkParameters setNumberOfOutputs(final Integer numberOfOutputs) {
        this.numberOfOutputs = numberOfOutputs;
        return this;
    }

    public List<AbstractDataElementChoice> getSelectedElements() {
        return selectedElements;
    }

    public NetworkParameters setSelectedElements(final List<AbstractDataElementChoice> selectedElements) {
        this.selectedElements = selectedElements;
        return this;
    }

    public LocalDateTime getWhenTrained() {
        return whenTrained;
    }

    public NetworkParameters setWhenTrained(final LocalDateTime whenTrained) {
        this.whenTrained = whenTrained;
        return this;
    }

    public double getNetworkAccuracy() {
        return networkAccuracy;
    }

    public NetworkParameters setNetworkAccuracy(final double networkAccuracy) {
        this.networkAccuracy = networkAccuracy;
        return this;
    }

    public boolean isNetworkSaved() {
        return networkSaved;
    }

    public NetworkParameters setNetworkSaved(final boolean networkSaved) {
        this.networkSaved = networkSaved;
        return this;
    }

    /**
     * Transform the data from Double to Strings.
     * Pull only the data the user has selected, which is contained in the
     * NetworkParameters.selectedElements property.
     *
     * @param data The input data (Double objects)
     * @return String[] that contains only the data elements the user wants
     * to include to train, evaluate, and run the network.
     */
    public String[] transformRow(final List<Double> data) {
        return transformRow(data, false);
    }

    /**
     * Transform the data from Double to Strings.
     * Pull only the data the user has selected, which is contained in the
     * NetworkParameters.selectedElements property.
     *
     * @param data     The input data (Double objects)
     * @param training true if the row of data is used for training, false if not.
     * @return String[] that contains only the data elements the user wants
     * to include to train, evaluate, and run the network.
     */
    public String[] transformRow(final List<Double> data, final boolean training) {
        // Note: networkParameters is future-proofing, at some point the user can specify the specific fields they want from the data
        //
        // Pull the elements from the networkParameters.selectedElements only
        List<? extends AbstractDataElementChoice> selectedElements = getSelectedElements();
        //
        // New array contains all of the data, including the label (win/loss)
        // The selected elements describes the data once, but the List<Double> contains two sets of data: home and away

        String[] ret = (training)
                // Only include a spot for a label if data used for training
                ? new String[selectedElements.size() + 1]
                // No label required
                : new String[selectedElements.size()];
        int index = 0;
        for (AbstractDataElementChoice dataElementMenuChoice : selectedElements) {
            int elementIndex = dataElementMenuChoice.getElementIndex();
            ret[index++] = String.valueOf(data.get(elementIndex)); // Home
        }
        if (training) {
            // Set the label (that is, the win/loss value AS AN INTEGER- THIS IS VERY IMPORTANT)
            // It is located in the very last spot in the incoming List<Double>
            ret[index] = String.valueOf(data.get(data.size() - 1).intValue());
        }
        return ret;
    }

    /**
     * Returns true if the specified year was not used to train
     * the network (otherwise that just wouldn't be fair), false otherwise.
     */
    public boolean isValidYearForTournamentPrediction(final Integer tournamentYear) {
        boolean ret = true;
        if (getYearsToTrainNetwork().contains(tournamentYear)) {
            log.warn("isValidYearForTournamentPrediction({}): current network is not valid for tournament prediction based on its training years: {}",
                    tournamentYear, StringUtils.join(getYearsToTrainAndEvaluateNetwork().toArray(), ", "));

            ret = false;
        }
        if (getYearsToEvaluateNetwork().contains(tournamentYear)) {
            log.warn("isValidYearForTournamentPrediction({}): current network is not valid for tournament prediction based on its evaluation years: {}",
                    tournamentYear, StringUtils.join(getYearsToTrainAndEvaluateNetwork().toArray(), ", "));

            ret = false;
        }
        return ret;
    }

    public String getTrainingYearsString() {
        return listToCsvString(getYearsToTrainAndEvaluateNetwork().get(0));
    }

    public String toString() {
        return String.format("Network info%n") +
                String.format("\tNetwork structure         : %s%n", getNetworkLayout()) +
                String.format("\tTraining Years            : %s%n", StringUtils.join(getYearsToTrainAndEvaluateNetwork().get(0), ',')) +
                String.format("\tEvaluation Years          : %s%n", StringUtils.join(getYearsToTrainAndEvaluateNetwork().get(1), ',')) +
                String.format("\tActivation function       : %s%n", getActivationFunction().name()) +
                String.format("\tUpdater function          : %s%n", getUpdater()) +
                String.format("\tLoss function             : %s%n", getLossFunction()) +
                String.format("\tWeight init function      : %s%n", getWeightInit()) +
                String.format("\tNumber of inputs          : %d%n", getNumberOfInputs()) +
                String.format("\tNumber of outputs         : %d%n", getNumberOfOutputs()) +
                String.format("\tNumber of training epochs : %d%n", getNumberOfEpochs()) +
                String.format("\tNetwork accuracy          : %5.5f%%%n", getNetworkAccuracy() * 100.0) +
                String.format("\tL2 Regularization Lambda  : %5.5f%n", getL2RegularizationLambda()) +
                String.format("\tNormalizer                : %s%n", getNormalizer()) +
                String.format("\tNetwork type              : %s%n", getNetworkType()) +
                String.format("\tDropout probability       : %3.2f%%%n", getDropoutProbability() * 100) +
                String.format("\tData Elements             : %s%n", StringUtils.join(getSelectedElements(), ','));
    }

    public String toStringAbbreviated() {
        return String.format("Network info%n") +
                String.format("\tNetwork structure         : %s%n", getNetworkLayout()) +
                String.format("\tTraining Years            : %s%n", StringUtils.join(getYearsToTrainAndEvaluateNetwork().get(0), ',')) +
                String.format("\tEvaluation Years          : %s%n", StringUtils.join(getYearsToTrainAndEvaluateNetwork().get(1), ',')) +
                String.format("\tActivation function       : %s%n", getActivationFunction().name()) +
                String.format("\tUpdater function          : %s%n", getUpdater()) +
                String.format("\tLoss function             : %s%n", getLossFunction()) +
                String.format("\tWeight init function      : %s%n", getWeightInit()) +
                String.format("\tNumber of inputs          : %d%n", getNumberOfInputs()) +
                String.format("\tNumber of outputs         : %d%n", getNumberOfOutputs()) +
                String.format("\tNumber of training epochs : %d%n", getNumberOfEpochs()) +
                String.format("\tNetwork accuracy          : %5.5f%%%n", getNetworkAccuracy() * 100.0) +
                String.format("\tNormalizer                : %s%n", getNormalizer()) +
                String.format("\tDropout probability       : %3.2f%%%n", getDropoutProbability() * 100) +
                String.format("\tL2 Regularization Lambda  : %5.5f%n", getL2RegularizationLambda());
    }

    public String toStringOneLine() {
        return "Network info\t" +
                String.format("Network structure: %s, ", getNetworkLayout()) +
                String.format("Training Years: %s, ", StringUtils.join(getYearsToTrainAndEvaluateNetwork().get(0), ',')) +
                String.format("Evaluation Years: %s, ", StringUtils.join(getYearsToTrainAndEvaluateNetwork().get(1), ',')) +
                String.format("Activation function: %s, ", getActivationFunction().name()) +
                String.format("Updater function: %s, ", getUpdater()) +
                String.format("Loss function: %s, ", getLossFunction()) +
                String.format("Weight init function: %s, ", getWeightInit()) +
                String.format("Network accuracy: %3.4f", getNetworkAccuracy());
    }

    public String[] toStringCsv() {
        return new String[]{
                String.format("DRP: %3.4f%%", 100 * (1.0 - getDropoutProbability())),
                String.format("ACC: %3.4f%%", 100 * getNetworkAccuracy()),
                String.format("ACT: %s", getActivationFunction().name()),
                String.format("UPD: %s", UpdaterMenu.UpdaterChoice.from(getUpdater()).orElse(null)),
                String.format("EP: %d", getNumberOfEpochs()),
                StringUtils.EMPTY,
                StringUtils.EMPTY,
                StringUtils.EMPTY,
                String.format("TRN YR: %s", StringUtils.join(getYearsToTrainAndEvaluateNetwork().get(0), '/')),
                String.format("EVAL YR: %s", StringUtils.join(getYearsToTrainAndEvaluateNetwork().get(1), '/')),
                String.format("NORM: %s", getNormalizer()),
                String.format("%s", getNetworkLayout()),

        };
    }

    public double getL2RegularizationLambda() {
        return l2RegularizationLambda;
    }

    public NetworkParameters setL2RegularizationLambda(final double l2RegularizationLambda) {
        this.l2RegularizationLambda = l2RegularizationLambda;
        return this;
    }

    public DataNormalizationMenu.DataNormalizationChoice getNormalizer() {
        return normalizer;
    }

    public NetworkParameters setNormalizer(final DataNormalizationMenu.DataNormalizationChoice normalizer) {
        this.normalizer = normalizer;
        return this;
    }

    public String getNetworkType() {
        return networkType;
    }

    public NetworkParameters setNetworkType(final String networkType) {
        this.networkType = networkType;
        return this;
    }

    public double getDropoutProbability() {
        return dropoutProbability;
    }

    public NetworkParameters setDropoutProbability(final double dropoutProbability) {
        this.dropoutProbability = dropoutProbability;
        return this;
    }
}
