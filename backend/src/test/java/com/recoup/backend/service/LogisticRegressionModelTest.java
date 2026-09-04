package com.recoup.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

/** Proves the hand-rolled gradient descent actually learns something, not
 *  just that it runs. A model that can't separate obviously-separable
 *  synthetic data would be a much bigger problem than any guardrail test
 *  could catch downstream. */
class LogisticRegressionModelTest {

    @Test
    void throwsWhenPredictingBeforeTraining() {
        LogisticRegressionModel model = new LogisticRegressionModel();
        assertThatThrownBy(() -> model.predict(new double[]{1.0, 0.5}))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void learnsToSeparateAClearlyLinearlySeparableDataset() {
        // x = [bias, signal, noise]; label = 1 whenever signal > 0.5, regardless of noise.
        Random rng = new Random(7);
        List<double[]> features = new ArrayList<>();
        List<Double> labels = new ArrayList<>();
        for (int i = 0; i < 400; i++) {
            double signal = rng.nextDouble();
            double noise = rng.nextDouble();
            features.add(new double[]{1.0, signal, noise});
            labels.add(signal > 0.5 ? 1.0 : 0.0);
        }

        LogisticRegressionModel model = new LogisticRegressionModel();
        model.train(features, labels, 0.5, 300, 0.001);

        assertThat(model.isTrained()).isTrue();
        assertThat(model.getTrainingSamples()).isEqualTo(400);

        // Held-out check on fresh points, not the training set.
        int correct = 0;
        int total = 200;
        Random testRng = new Random(99);
        for (int i = 0; i < total; i++) {
            double signal = testRng.nextDouble();
            double noise = testRng.nextDouble();
            double predicted = model.predict(new double[]{1.0, signal, noise});
            boolean predictedPositive = predicted >= 0.5;
            boolean actualPositive = signal > 0.5;
            if (predictedPositive == actualPositive) {
                correct++;
            }
        }
        double accuracy = (double) correct / total;
        assertThat(accuracy).as("accuracy on held-out separable data").isGreaterThan(0.85);
    }

    @Test
    void higherSignalFeatureProducesHigherPredictedProbability() {
        List<double[]> features = List.of(
            new double[]{1.0, 0.0}, new double[]{1.0, 0.1}, new double[]{1.0, 0.9}, new double[]{1.0, 1.0}
        );
        List<Double> labels = List.of(0.0, 0.0, 1.0, 1.0);

        LogisticRegressionModel model = new LogisticRegressionModel();
        model.train(features, labels, 0.5, 500, 0.0);

        double low = model.predict(new double[]{1.0, 0.05});
        double high = model.predict(new double[]{1.0, 0.95});
        assertThat(high).isGreaterThan(low);
    }
}
