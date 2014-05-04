package nl.tudelft.sps.app.activity;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Ordering;

import java.util.Comparator;
import java.lang.Math;
import java.util.*;
import java.lang.*;

/**
 * K-nn classifier
 */
public class ActivityClassifier implements IClassifier {

    private final List<ActivityMeasurementData> trainingPoints = new ArrayList<ActivityMeasurementData>();

    private final IMeasurementsDistance distance = new EuclideanMeasurementsDistance();

    /**
     * Classify a measurement as an activity using K-nn
     */
    public ACTIVITY classify(IMeasurement measurement) {
        final TreeMap<Double, ActivityMeasurementData> sortedNeighbors = new TreeMap<>();

        for (ActivityMeasurementData neighbor : trainingPoints) {
            // Compute the distance between the measurement and its neighbor
            final double neighborDistance = distance.getDistance(measurement, neighbor.measurement);

            sortedNeighbors.put(neighborDistance, neighbor);
        }

        final Map<ACTIVITY, Integer> sqrtNNeighbors = new HashMap<ACTIVITY, Integer>();
//        final ActivityCountComparator countComparator = new ActivityCountComparator(sqrtNNeighbors);
//        final SortedMap<ACTIVITY, Integer> sortedSqrtNNeighbors = new TreeMap<ACTIVITY, Integer>(countComparator);

        // Add the closest sqrt(n) neighbors to a new map
        long sqrtN = Math.round(Math.sqrt(trainingPoints.size()));
        // Make it odd if it is even
        if ((sqrtN % 2) == 0) {
            sqrtN++;
        }
        for (int i = 0; i < sqrtN; i++) {
            final ACTIVITY activity = sortedNeighbors.pollFirstEntry().getValue().activity;

            // Increment count for current activity
            if (!sqrtNNeighbors.containsKey(activity)) {
                sqrtNNeighbors.put(activity, 0);
            }
            sqrtNNeighbors.put(activity, sqrtNNeighbors.get(activity) + 1);
        }

        // Now sort them
//        sortedSqrtNNeighbors.addAll(sqrtNNeighbors);

        // Create a comparator that compares the values
        // TODO May result in an infinite loop, see stackoverflow.com/questions/109383/how-to-sort-a-mapkey-value-on-the-values-in-java
        final Ordering valueComparator = Ordering.natural().onResultOf(Functions.forMap(sqrtNNeighbors)).compound(Ordering.natural());
        return (ACTIVITY) ImmutableSortedMap.copyOf(sqrtNNeighbors, valueComparator).lastKey();

//        return sortedSqrtNNeighbors.lastKey();
    }

    /**
     * Add a training point to the classifier
     */
    public void train(ACTIVITY activity, IMeasurement measurement) {
        trainingPoints.add(new ActivityMeasurementData(activity, measurement));
    }

    /**
     * Tuple containing a measurement and its activity type
     */
    private static class ActivityMeasurementData {

        private final ACTIVITY activity;
        private final IMeasurement measurement;

        private ActivityMeasurementData(ACTIVITY activity, IMeasurement measurement) {
            this.activity = activity;
            this.measurement = measurement;
        }
    }

//    private static class ActivityCountComparator implements Comparator<ACTIVITY> {
//
//        private final Map<ACTIVITY, Integer> sqrtNNeighbors;
//
//        public ActivityCountComparator(Map<ACTIVITY, Integer> sqrtNNeighbors) {
//            this.sqrtNNeighbors = sqrtNNeighbors;
//        }
//
//        public int compare(ACTIVITY left, ACTIVITY right) {
//            return sqrtNNeighbors.get(left).compare(sqrtNNeighbors.get(right));
//        }
//
//    };

}
