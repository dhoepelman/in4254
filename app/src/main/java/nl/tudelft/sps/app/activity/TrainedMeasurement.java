package nl.tudelft.sps.app.activity;

import java.util.ArrayList;
import java.util.List;

public class TrainedMeasurement extends Measurement {

    private final double[] featureVectors;

    /**
     * Constructs a TrainedMeasurement using a line of CSV data
     *
     * @param line with Comma Separated Values
     */
    public TrainedMeasurement(String line) {
        super();

        final String[] values = line.split(",");
        final double[] doubles = new double[values.length];

        int i = 0;
        for (String value : line.split(",")) {
            doubles[i] = Double.valueOf(value);
            i++;
        }

        featureVectors = doubles;
    }

    @Override
    public double[] getFeatureVector() {
        return featureVectors;
    }

}
