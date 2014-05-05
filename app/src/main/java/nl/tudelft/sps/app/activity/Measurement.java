package nl.tudelft.sps.app.activity;

import com.google.common.primitives.Doubles;

import org.apache.commons.math3.stat.correlation.Covariance;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * [1] Ravi, Nishkam, et al. "Activity recognition from accelerometer data." AAAI. Vol. 5. 2005.
 */

public class Measurement implements IMeasurement {

    /**
     * Window size. Based on [1]. 50Hz measurements
     */
    public static final int WINDOW_SIZE = 256;
    /**
     * Window overlap. Based on [1]
     */
    public static final int WINDOW_OVERLAP = 128;

    private final DescriptiveStatistics[] raw_measurements;
    private double[] featureVector = null;

    public Measurement() {
        raw_measurements = new DescriptiveStatistics[]{
                new DescriptiveStatistics(WINDOW_SIZE),
                new DescriptiveStatistics(WINDOW_SIZE),
                new DescriptiveStatistics(WINDOW_SIZE)
        };
    }

    /**
     * True if the window is full
     */
    public boolean isCompleted() {
        return getProgress() >= WINDOW_SIZE;
    }

    @Override
    public boolean isValid() {
        return isCompleted();
    }

    /**
     * Gets the number of samples that are already in the measurement.
     */
    public int getProgress() {
        return (int) raw_measurements[0].getN();
    }

    public double getMean(int axis) {
        return raw_measurements[axis].getMean();
    }

    public double getStdDev(int axis) {
        return raw_measurements[axis].getStandardDeviation();
    }

    /**
     * Get the covariance between two axes
     */
    public double getCorrelation(int axis1, int axis2) {
        return getCorrelation(axis1, axis2, getStdDev(axis1), getStdDev(axis2));
    }

    private double getCorrelation(int axis1, int axis2, double stdDev1, double stdDev2) {
        double cov = new Covariance().covariance(raw_measurements[axis1].getValues(), raw_measurements[axis2].getValues());
        return cov / (stdDev1 * stdDev2);
    }

    /**
     * Get the feature vector of this measurement
     */
    @Override
    public double[] getFeatureVector() {
        if (!isCompleted()) {
            return IMeasurement.INVALID_MEASUREMENT.getFeatureVector();
        }
        if (featureVector == null) {
            final double stdDev0 = getStdDev(0);
            final double stdDev1 = getStdDev(1);
            final double stdDev2 = getStdDev(2);
            featureVector = new double[]{
                    getMean(0),
                    getMean(1),
                    getMean(2),
                    stdDev0,
                    stdDev1,
                    stdDev2,
                    getCorrelation(0, 1, stdDev0, stdDev1),  // Corr(x,y)
                    getCorrelation(1, 2, stdDev1, stdDev2),  // Corr(y,z)
                    getCorrelation(2, 0, stdDev2, stdDev0),  // Corr(z,x)
            };
        }
        return featureVector;
    }

    @Override
    public String toString() {
        return "Measurement(" + Doubles.join(",", getFeatureVector()) + ")";
    }

    public void addToMeasurement(float[] values) {
        if (values.length != 3) {
            throw new IllegalArgumentException("Measurement must have only X,Y,Z axis");
        }
        raw_measurements[0].addValue(values[0]);
        raw_measurements[1].addValue(values[1]);
        raw_measurements[2].addValue(values[2]);
    }

    private static abstract class Helper {
        protected Measurement current;
        protected int current_loc = 0;

        public boolean isFull() {
            return current_loc == WINDOW_SIZE;
        }
    }

    public static class MonitorHelper extends Helper {
        public MonitorHelper() {
            cleanWindow();
        }

        public synchronized void cleanWindow() {
            current = new Measurement();
            current_loc = 0;
        }

        public synchronized Measurement getCurrentWindow() {
            return current;
        }

        public synchronized boolean addMeasurement(float[] values) {
            if (values.length != 3) {
                throw new IllegalArgumentException("Expected 3 values");
            }

            if (isFull()) {
                throw new RuntimeException("Window is full");
            }

            current.addToMeasurement(values);
            current_loc++;

            return isFull();
        }
    }

    /**
     * Helper to divide raw measurements into windows
     */
    public static class TrainHelper extends Helper {
        public final ACTIVITY activity;
        private final List<Measurement> measurements = new ArrayList<>();
        private Measurement next;
        private int numFullWindows = 0;

        public TrainHelper(ACTIVITY activity) {
            this.activity = activity;
        }

        /**
         * Get all measurements
         */
        public Collection<? extends IMeasurement> getMeasurements() {
            return measurements;
        }

        public void removeIncompleteMeasurements() {
            // Remove the last measurement if it's incomplete
            while (measurements.size() > 0 && !measurements.get(measurements.size() - 1).isCompleted()) {
                measurements.remove(measurements.size() - 1);
            }
        }

        public int getNumberOfFullWindows() {
            return numFullWindows;
        }

        public void addMeasurement(float[] values) {
            if (values.length != 3) {
                throw new IllegalArgumentException("Expected 3 values");
            }

            if (current == null || isFull()) {
                if (current == null) {
                    // We don't have any measurement, start one
                    current = new Measurement();
                    current_loc = 0;
                    measurements.add(current);
                } else {
                    // Measurement was full, replace with next
                    current = next;
                    current_loc = WINDOW_OVERLAP;
                    numFullWindows++;
                }
                // Create an empty "next" measurement
                next = new Measurement();
                measurements.add(next);
            }

            current.addToMeasurement(values);
            if (current_loc >= WINDOW_OVERLAP) {
                next.addToMeasurement(values);
            }
            current_loc++;
        }
    }
}
