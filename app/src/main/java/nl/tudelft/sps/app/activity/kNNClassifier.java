package nl.tudelft.sps.app.activity;

import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.apache.commons.math3.ml.distance.EuclideanDistance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

/**
 * K-nn classifier
 */
public class kNNClassifier implements IClassifier {

    private final List<TrainingPoint> trainingPoints = new ArrayList<>();

    private final DistanceMeasure distance = new EuclideanDistance();

    private final int windowSize;

    public kNNClassifier(int windowSize) {
        this.windowSize = windowSize;
    }

    public int getWindowSize() {
        return windowSize;
    }

    /**
     * Classify a measurement as an activity using K-nn
     */
    public synchronized ACTIVITY classify(IMeasurement measurement) {
        // Check if the classifier is trained yet
        if (!isTrained()) {
            throw new IllegalStateException("kNN-Classifier is still untrained");
        }

        // Contains a map from distance to the measurement to the training point, sorted on distance
        final NavigableMap<Double, TrainingPoint> sortedNeighbors = new TreeMap<>();

        // Compute the distance between the measurements and all training points
        for (TrainingPoint neighbor : trainingPoints) {
            final double neighborDistance = distance.compute(measurement.getFeatureVector(), neighbor.featureVector);
            sortedNeighbors.put(neighborDistance, neighbor);
        }

        // Count how often an ACTIVITY is among the sqrt(n) nearest neighbours
        final Map<ACTIVITY, Integer> NNeighborActivityCount = new HashMap<>();

        // Add the closest sqrt(n) neighbors to a new map
        long sqrtN = Math.round(Math.sqrt(sortedNeighbors.size()));
        // Make it odd if it is even
        if ((sqrtN % 2) == 0) {
            sqrtN++;
        }

        // Go through the sqrt(n) nearest neighbors
        for (int i = 0; i < sqrtN; i++) {
            final ACTIVITY activity = sortedNeighbors.pollFirstEntry().getValue().activity;

            // Add the activity to the Map if it isn't there yet
            if (!NNeighborActivityCount.containsKey(activity)) {
                NNeighborActivityCount.put(activity, 0);
            }
            // Increment count for current activity
            NNeighborActivityCount.put(activity, NNeighborActivityCount.get(activity) + 1);
        }

        // Select the ACTIVITY that is most prevalent among the sqrt(n) Nearest Neighbors
        Map.Entry<ACTIVITY, Integer> winner = null;
        for (Map.Entry<ACTIVITY, Integer> count : NNeighborActivityCount.entrySet()) {
            if (winner == null                                                                      // There currently isn't a winner
                    || winner.getValue() < count.getValue()                                         // Count has a higher value than the current winner
                    || (winner.getValue().equals(count.getValue()) && new Random().nextInt(2) == 0) // When tied pick one at random
                    ) {
                winner = count;
            }
        }

        if (winner != null) {
            return winner.getKey();
        }
        else {
            // There were no neighbors
            return ACTIVITY.Unknown;
        }
    }

    /**
     * Add a training point to the classifier
     */
    public void train(ACTIVITY activity, IMeasurement measurement) {
        trainingPoints.add(new TrainingPoint(activity, measurement.getFeatureVector()));
    }

    @Override
    public synchronized boolean isTrained() {
        return !trainingPoints.isEmpty();
    }

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
    }

}
