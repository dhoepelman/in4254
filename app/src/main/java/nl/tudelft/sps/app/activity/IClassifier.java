package nl.tudelft.sps.app.activity;

/**
 * Interface for Activity classifiers
 */
public interface IClassifier {
    /**
     * Classify a measurement as an activity
     */
    public ACTIVITY classify(IMeasurement m);

    /**
     * Add a training point to the classifier
     * @param a The activity which was performed
     * @param m The measurement
     */
    public void train(ACTIVITY a, IMeasurement m);
}
