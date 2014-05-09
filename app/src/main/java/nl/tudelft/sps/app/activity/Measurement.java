package nl.tudelft.sps.app.activity;

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


/**
 * [1] Ravi, Nishkam, et al. "Activity recognition from accelerometer data." AAAI. Vol. 5. 2005.
 */
@DatabaseTable(tableName = "act_measurements")
public class Measurement implements IMeasurement {
    public static final String TABLE_NAME = "act_measurements";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_ACTIVITY = "activity";
    /**
     * Window size. Based on [1]. 50Hz measurements
     */
    public static final int WINDOW_SIZE = 256;
    /**
     * Window overlap. Based on [1]
     */
    public static final int WINDOW_OVERLAP = 128;
    @DatabaseField(id = true)
    long id;
    @DatabaseField
    long timestamp;
    @DatabaseField(unknownEnumName = "Unknown")
    ACTIVITY activity;
    @ForeignCollectionField(eager = false)
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
        window = new DescriptiveStatistics[]{
                new DescriptiveStatistics(WINDOW_SIZE),
                new DescriptiveStatistics(WINDOW_SIZE),
                new DescriptiveStatistics(WINDOW_SIZE)
        };
        this.timestamp = timestamp;
        this.activity = activity;
    }

    public Measurement() {
        // ORMLite
    }

    private DescriptiveStatistics[] getDescriptiveStatistics() {
        if (window == null) {
            window = Sample.toDescriptiveStatistics(samples);
        }
        return window;
    }

    public double getMeanX() {
        if(MeanX == Double.NaN) {
            MeanX = getDescriptiveStatistics()[0].getMean();
        }
        return MeanX;
    }

    public double getMeanY() {
        if(MeanY == Double.NaN) {
            MeanY = getDescriptiveStatistics()[1].getMean();
        }
        return MeanY;
    }

    public double getMeanZ() {
        if(MeanZ == Double.NaN) {
            MeanZ = getDescriptiveStatistics()[2].getMean();
        }
        return MeanZ;
    }

    public double getStdDevX() {
        if(StdDevX == Double.NaN) {
            StdDevX = getDescriptiveStatistics()[0].getStandardDeviation();
        }
        return StdDevX;
    }

    public double getStdDevY() {
        if(StdDevY == Double.NaN) {
            StdDevY = getDescriptiveStatistics()[1].getStandardDeviation();
        }
        return StdDevY;
    }

    public double getStdDevZ() {
        if(StdDevZ == Double.NaN) {
            StdDevZ = getDescriptiveStatistics()[2].getStandardDeviation();
        }
        return StdDevZ;
    }

    public double getCorrXY() {
        if(CorrXY == Double.NaN) {
            CorrXY = new Covariance().covariance(window[0].getValues(), window[1].getValues()) / (getStdDevX() * getStdDevY());
        }
        return CorrXY;
    }

    public double getCorrYZ() {
        if(CorrYZ == Double.NaN) {
            CorrYZ = new Covariance().covariance(window[1].getValues(), window[2].getValues()) / (getStdDevY() * getStdDevZ());
        }
        return CorrYZ;
    }

    public double getCorrZX() {
        if(CorrZX == Double.NaN) {
            CorrZX = new Covariance().covariance(window[2].getValues(), window[0].getValues()) / (getStdDevX() * getStdDevY());
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
        return (int) window[0].getN();
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
        return featureVector;
    }

    @Override
    public String toString() {
        return "Measurement(" + Doubles.join(",", getFeatureVector()) + ")";
    }

    public void addToMeasurement(final float[] values, long timestamp) {
        if (values.length != 3) {
            throw new IllegalArgumentException("Measurement must have only X,Y,Z axis");
        }
        window[0].addValue(values[0]);
        window[1].addValue(values[1]);
        window[2].addValue(values[2]);
        samples.add(new Sample(this, timestamp, values[0], values[1], values[2]));
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

        public synchronized boolean addMeasurement(float[] values, long timestamp) {
            if (values.length != 3) {
                throw new IllegalArgumentException("Expected 3 values");
            }

            if (isFull()) {
                throw new RuntimeException("Window is full");
            }

            current.addToMeasurement(values, timestamp);
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

        public void addMeasurement(float[] values, long timestamp) {
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

            current.addToMeasurement(values, timestamp);
            if (current_loc >= WINDOW_OVERLAP) {
                next.addToMeasurement(values, timestamp);
            }
            current_loc++;
        }
    }
}
