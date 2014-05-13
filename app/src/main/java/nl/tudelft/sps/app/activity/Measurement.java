package nl.tudelft.sps.app.activity;

import android.util.Log;

import com.google.common.primitives.Doubles;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

import org.apache.commons.math3.stat.correlation.Covariance;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import nl.tudelft.sps.app.DatabaseHelper;

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
    @DatabaseField(generatedId = true)
    long id;
    @DatabaseField
    long timestamp;
    @DatabaseField(unknownEnumName = "Unknown")
    ACTIVITY activity;
    @ForeignCollectionField(eager = true)
    ForeignCollection<Sample> samples;
    @DatabaseField
    double MeanX = Double.NaN;
    @DatabaseField
    double MeanY = Double.NaN;
    @DatabaseField
    double MeanZ = Double.NaN;
    @DatabaseField
    double StdDevX = Double.NaN;
    @DatabaseField
    double StdDevY = Double.NaN;
    @DatabaseField
    double StdDevZ = Double.NaN;
    @DatabaseField
    double CorrXY = Double.NaN;
    @DatabaseField
    double CorrYZ = Double.NaN;
    @DatabaseField
    double CorrZX = Double.NaN;

    /**
     * Window
     */
    private DescriptiveStatistics[] window;
    private double[] featureVector = null;


    public Measurement(long timestamp, ACTIVITY activity) {
        createEmptyWindow();
        this.timestamp = timestamp;
        this.activity = activity;
    }

    public Measurement() {
        // ORMLite
    }

    private DescriptiveStatistics[] getDescriptiveStatistics() {
        if (window == null) {
            if (samples != null) {
                window = Sample.toDescriptiveStatistics(samples);
            } else {
                createEmptyWindow();
            }
        }
        return window;
    }

    public double getMeanX() {
        if (Double.isNaN(MeanX)) {
            MeanX = getDescriptiveStatistics()[0].getMean();
        }
        return MeanX;
    }

    public double getMeanY() {
        if (Double.isNaN(MeanY)) {
            MeanY = getDescriptiveStatistics()[1].getMean();
        }
        return MeanY;
    }

    public double getMeanZ() {
        if (Double.isNaN(MeanZ)) {
            MeanZ = getDescriptiveStatistics()[2].getMean();
        }
        return MeanZ;
    }

    public double getStdDevX() {
        if (Double.isNaN(StdDevX)) {
            StdDevX = getDescriptiveStatistics()[0].getStandardDeviation();
        }
        return StdDevX;
    }

    public double getStdDevY() {
        if (Double.isNaN(StdDevY)) {
            StdDevY = getDescriptiveStatistics()[1].getStandardDeviation();
        }
        return StdDevY;
    }

    public double getStdDevZ() {
        if (Double.isNaN(StdDevZ)) {
            StdDevZ = getDescriptiveStatistics()[2].getStandardDeviation();
        }
        return StdDevZ;
    }

    public double getCorrXY() {
        if (Double.isNaN(CorrXY)) {
            CorrXY = new Covariance().covariance(getDescriptiveStatistics()[0].getValues(), getDescriptiveStatistics()[1].getValues()) / (getStdDevX() * getStdDevY());
        }
        return CorrXY;
    }

    public double getCorrYZ() {
        if (Double.isNaN(CorrYZ)) {
            CorrYZ = new Covariance().covariance(getDescriptiveStatistics()[1].getValues(), getDescriptiveStatistics()[2].getValues()) / (getStdDevY() * getStdDevZ());
        }
        return CorrYZ;
    }

    public double getCorrZX() {
        if (Double.isNaN(CorrZX)) {
            CorrZX = new Covariance().covariance(getDescriptiveStatistics()[2].getValues(), getDescriptiveStatistics()[0].getValues()) / (getStdDevX() * getStdDevY());
        }
        return CorrZX;
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

    @Override
    public ACTIVITY getActivity() {
        return activity;
    }

    /**
     * Gets the number of samples that are already in the measurement.
     */
    public int getProgress() {
        return (int) getDescriptiveStatistics()[0].getN();
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
            calculateFeatureVector();
        }
        return featureVector;
    }

    private void calculateFeatureVector() {
        featureVector = new double[]{
                getMeanX(),
                getMeanY(),
                getMeanZ(),
                getStdDevX(),
                getStdDevY(),
                getStdDevZ(),
                getCorrXY(),
                getCorrYZ(),
                getCorrZX(),
        };
    }

    @Override
    public String toString() {
        return "Measurement(" + Doubles.join(",", getFeatureVector()) + ")";
    }

    public void addToMeasurement(final float[] values) {
        if (values.length != 3) {
            throw new IllegalArgumentException("Measurement must have only X,Y,Z axis");
        }
        DescriptiveStatistics[] window = getDescriptiveStatistics();
        window[0].addValue(values[0]);
        window[1].addValue(values[1]);
        window[2].addValue(values[2]);
    }

    private static abstract class Helper {
        protected Measurement current;
        protected int current_loc = 0;
        protected int loc_this_time = 0;

        public boolean isFull() {
            return current_loc == WINDOW_SIZE;
        }
    }

    public static class MonitorHelper extends Helper {
        private Measurement next;

        public MonitorHelper() {
            current = new Measurement();
            current_loc = 0;

            // Create an empty "next" measurement
            next = new Measurement();
        }

        public synchronized Measurement getCurrentWindow() {
            return current;
        }

        public synchronized void logCurrentNext() {
            Log.w(getClass().getName(), "TEST CURRENT NEXT " + String.valueOf(current.hashCode()) + " " + String.valueOf(next.hashCode()));
        }

        public synchronized boolean isCompleted() {
            return current.isCompleted();
        }

        public synchronized int getProgress() {
            return current.getProgress();
        }

        public synchronized void addNewWindowIfFull() {
            if (isFull()) {
                // Measurement was full, replace with next
                current = next;
                current_loc = WINDOW_OVERLAP;

                // Create an empty "next" measurement
                next = new Measurement();
            }
        }

        public synchronized void addToMeasurement(float[] values) {
            if (values.length != 3) {
                throw new IllegalArgumentException("Expected 3 values");
            }

            addNewWindowIfFull();

            current.addToMeasurement(values);
            if (current_loc >= WINDOW_OVERLAP) {
                next.addToMeasurement(values);
            }
            current_loc++;
        }
    }

    /**
     * Helper to divide raw measurements into windows
     */
    public static class TrainHelper extends Helper {
        public final ACTIVITY activity;
        private final DatabaseHelper databaseHelper;
        private final List<Measurement> measurements = new ArrayList<>();
        protected Measurement current;
        protected int current_loc = 0;
        private Measurement next;
        private int numFullWindows = 0;
        private Collection<Sample> currentSamples;
        private Collection<Sample> nextSamples;

        public TrainHelper(ACTIVITY activity, DatabaseHelper dbhelper) {
            this.activity = activity;
            this.databaseHelper = dbhelper;
        }

        public boolean isFull() {
            return current_loc == WINDOW_SIZE;
        }

        /**
         * Get all measurements
         */
        public Collection<Measurement> getMeasurements() {
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

        public void addMeasurement(float[] values, long timestamp) {
            if (values.length != 3) {
                throw new IllegalArgumentException("Expected 3 values");
            }

            if (current == null || isFull()) {
                if (current == null) {
                    // We don't have any measurement, start one
                    current = new Measurement(timestamp, activity);
                    currentSamples = new ArrayList<>(WINDOW_SIZE);
                    current_loc = 0;
                    measurements.add(current);
                } else {
                    // Measurement was full, store it
                    persistCurrentMeasurement();
                    // Replace with next
                    current = next;
                    currentSamples = nextSamples;
                    current_loc = WINDOW_OVERLAP;
                    numFullWindows++;
                }
                // Create an empty "next" measurement
                next = new Measurement(timestamp, activity);
                nextSamples = new ArrayList<>(WINDOW_SIZE);
                measurements.add(next);
            }

            currentSamples.add(new Sample(current, timestamp, values[0], values[1], values[2]));
            current.addToMeasurement(values);
            if (current_loc >= WINDOW_OVERLAP) {
                nextSamples.add(new Sample(current, timestamp, values[0], values[1], values[2]));
                next.addToMeasurement(values);
            }
            current_loc++;
        }

        private void persistCurrentMeasurement() {
            current.calculateFeatureVector();
            databaseHelper.getMeasurementDao().create(current);
            for (Sample s : currentSamples) {
                databaseHelper.getSampleDao().create(s);
            }
        }
    }
}
