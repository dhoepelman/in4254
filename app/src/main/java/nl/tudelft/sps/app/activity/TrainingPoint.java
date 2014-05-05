package nl.tudelft.sps.app.activity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Tuple containing a measurement and its activity type
 */
public class TrainingPoint {

    public final ACTIVITY activity;
    public final double[] featureVector;

    public TrainingPoint(ACTIVITY activity, double[] featureVector) {
        this.activity = activity;
        this.featureVector = featureVector;
    }

    public static List<TrainingPoint> fromCSV(File f) throws IOException {
        List<TrainingPoint> dataPoints = new ArrayList<>();
        final BufferedReader reader = new BufferedReader(new FileReader(f));

        // Read every line of the file
        String line;
        // Extract the features and put the data from each line into its own IMeasurement instance
        while ((line = reader.readLine()) != null) {
            final String[] split = line.split(",", 2);
            final ACTIVITY activity = ACTIVITY.valueOf(split[0]);
            final String[] features = split[1].split(",");

            final double[] featureVector = new double[features.length];

            int i = 0;
            for (String feature : features) {
                featureVector[i++] = Double.valueOf(feature);
            }

            dataPoints.add(new TrainingPoint(activity, featureVector));
        }

        return dataPoints;

    }

    public static void toCSV(File f, List<TrainingPoint> points) {
        throw new IllegalArgumentException("Not implemented yet");
        // TODO: Implement
    }
}
