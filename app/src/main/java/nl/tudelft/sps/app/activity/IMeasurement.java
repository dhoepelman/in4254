package nl.tudelft.sps.app.activity;

import java.io.Serializable;

/**
 * The interface for a measurement
 */
public interface IMeasurement extends Serializable {
    /**
     * Get the feature vector of this measurement
     */
    double[] getFeatureVector();
}
