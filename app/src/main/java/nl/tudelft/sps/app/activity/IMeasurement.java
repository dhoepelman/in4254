package nl.tudelft.sps.app.activity;

/**
 * The interface for a measurement
 */
public interface IMeasurement {
    /**
     * Get the feature vector of this measurement
     * Returns a double array of size 9
     */
    double[] getFeatureVector();

    /**
     * False if this measurement is not valid
     */
    boolean isValid();

    /**
     * Get the activity associated to this measurement.
     */
    public ACTIVITY getActivity();

    public long getTimestamp();

    /**
     * This measurement can be returned when there is an invalid measurement
     */
    public static IMeasurement INVALID_MEASUREMENT = new IMeasurement() {
        private final double[] feature_vector = new double[]{
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                Double.NaN,
        };

        @Override
        public double[] getFeatureVector() {
            return feature_vector;
        }

        @Override
        public boolean isValid() {
            return false;
        }

        @Override
        public ACTIVITY getActivity() {
            return ACTIVITY.Unknown;
        }

        @Override
        public long getTimestamp() {
            return 0;
        }
    };
}
