package nl.tudelft.jemoetgaanapp.app;

/**
 * The interface for a measurement
 */
public interface IMeasurement {
    /**
     * Get the feature vector of this measurement
     */
    double[] getFeatureVector();
}
