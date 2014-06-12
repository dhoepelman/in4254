package nl.tudelft.sps.app.activity;

import java.util.Iterator;

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

    /**
     * Train the classifier by adding training points from some
     * training data.
     */
    public void train(Iterator<MeasurementWindow> trainingData);

    /**
     * If the classifier has training data available
     */
    public boolean isTrained();

    /**
     * Return the number of training points that were used to train
     * the classifier.
     */
    public int getNumberOfTrainingPoints();

    /**
     * Return the size of the windows that are used by the classifier
     */
    public int getWindowSize();

}
