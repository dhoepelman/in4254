package nl.tudelft.sps.app.activity;

/**
 * The interface for a measurement
 */
public interface IMeasurement {
    /**
     * Get the feature vector of this measurement
     */
    double[] getFeatureVector();
}
