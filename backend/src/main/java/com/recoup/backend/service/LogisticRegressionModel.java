package com.recoup.backend.service;

import java.util.List;

/** Plain-Java, L2-regularized logistic regression trained by batch gradient
 *  descent -- no ML library. Every line is inspectable and defensible: this
 *  is sigmoid(w . x), fit by walking downhill on cross-entropy loss.
 *
 *  Chosen over anything fancier on purpose: this model's whole job is to
 *  produce one interpretable number (a probability) that feeds a
 *  deterministic guardrail, not to make the decision itself. A coefficient
 *  vector you can print and read off is worth more here than a marginally
 *  better black box would be. */
public class LogisticRegressionModel {

    private double[] weights;
    private int trainingSamples = 0;

    public boolean isTrained() {
        return weights != null;
    }

    public int getTrainingSamples() {
        return trainingSamples;
    }

    public double[] getWeights() {
        return weights == null ? null : weights.clone();
    }

    /** @param features each row is one training example's feature vector (see {@link RecoveryFeatures})
     *  @param labels   1.0 if that example's outcome was a recovery, 0.0 otherwise
     */
    public void train(List<double[]> features, List<Double> labels, double learningRate, int epochs, double l2Lambda) {
        if (features.isEmpty() || features.size() != labels.size()) {
            throw new IllegalArgumentException("features and labels must be same non-zero size");
        }
        int n = features.size();
        int dim = features.get(0).length;
        double[] w = new double[dim];

        for (int epoch = 0; epoch < epochs; epoch++) {
            double[] gradient = new double[dim];
            for (int i = 0; i < n; i++) {
                double[] x = features.get(i);
                double y = labels.get(i);
                double prediction = sigmoid(dot(w, x));
                double error = prediction - y;
                for (int j = 0; j < dim; j++) {
                    gradient[j] += error * x[j];
                }
            }
            for (int j = 0; j < dim; j++) {
                double regularization = (j == 0) ? 0.0 : l2Lambda * w[j]; // never regularize the bias term
                w[j] -= learningRate * (gradient[j] / n + regularization);
            }
        }

        this.weights = w;
        this.trainingSamples = n;
    }

    public double predict(double[] features) {
        if (!isTrained()) {
            throw new IllegalStateException("model has not been trained yet");
        }
        return sigmoid(dot(weights, features));
    }

    private static double dot(double[] a, double[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    private static double sigmoid(double z) {
        return 1.0 / (1.0 + Math.exp(-z));
    }
}
