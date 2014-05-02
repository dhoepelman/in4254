package nl.tudelft.sps.app;

/**
 * Created by David on 2-5-2014.
 */
public interface IClassifier {
    /**
     * Classify a measurement as an activity
     */
    public ACTIVITY classify(Measurement m);

    /**
     * Add a training point to the classifier
     * @param a The activity which was performed
     * @param m The measurement
     */
    public void train(ACTIVITY a, Measurement m);
}
