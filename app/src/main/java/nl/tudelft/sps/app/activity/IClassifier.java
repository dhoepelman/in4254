package nl.tudelft.sps.app.activity;

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
     * If the classifier has training data available
     */
    public boolean isTrained();

    /**
     * Return the size of the windows that are used by the classifier
     */
    public int getWindowSize();

}
