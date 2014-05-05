package nl.tudelft.sps.app.activity;

import java.util.List;

/**
 * Interface for Activity classifiers
 */
public interface IClassifier {
    /**
     * Classify a measurement as an activity
     *
     * @throws java.lang.IllegalStateException if the classifier needs further training
     */
    public ACTIVITY classify(IMeasurement m);

    /**
     * Add a training point to the classifier
     *
     * @param a The activity which was performed
     * @param m The measurement
     */
    public void train(ACTIVITY a, IMeasurement m);

    // I wish I could use Java 8 default interface/traits yet
    public void train(List<TrainingPoint> trainingPoints);

    /**
     * If the classifier has training data available
     */
    public boolean isTrained();

}
