package nl.tudelft.sps.app.activity;

import android.util.Log;

import com.google.common.primitives.Doubles;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

import org.apache.commons.math3.stat.correlation.Covariance;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 * [1] Ravi, Nishkam, et al. "Activity recognition from accelerometer data." AAAI. Vol. 5. 2005.
 */

@DatabaseTable(tableName = "measurement")
public class MeasurementWindow implements IMeasurement {

    /**
     * Window size. Based on [1]. 50Hz measurements.
     * Use 256 for normal activity training/testing, use 60 for steps counter training/testing
     */
    public static final int WINDOW_SIZE = 240;

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
    @DatabaseField
    int size;

    /**
     * Window
     */
    private DescriptiveStatistics[] window;
    private double[] featureVector = null;

    public MeasurementWindow(long timestamp, ACTIVITY activity, int size) {
        this.timestamp = timestamp;
        this.activity = activity;
        this.size = size;
        createEmptyWindow();
    }

    public MeasurementWindow(int size) {
        this.size = size;
    }

    public MeasurementWindow() {
        // ORMLite
    }

    public int getWindowSize() {
        return size;
    }

    public int getWindowOverlap() {
        return size / 2;
    }

    private void createEmptyWindow() {
        window = new DescriptiveStatistics[]{
                new DescriptiveStatistics(getWindowSize()),
                new DescriptiveStatistics(getWindowSize()),
                new DescriptiveStatistics(getWindowSize())
        };
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
        return getProgress() >= getWindowSize();
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

    public void calculateFeatureVector() {
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

    public static abstract class Helper {
        protected MeasurementWindow current;
        protected int current_loc = 0;
        protected int loc_this_time = 0;

        public boolean isFull() {
            return current_loc == current.getWindowSize();
        }
    }

    public static class MonitorHelper extends Helper {
        private MeasurementWindow next;

        private long benchmarkTimestamp;

        public MonitorHelper(final int windowSize) {
            current = new MeasurementWindow(windowSize);
            current_loc = 0;

            // Create an empty "next" measurement
            next = new MeasurementWindow(windowSize);

            benchmarkTimestamp = System.currentTimeMillis();
        }

        public synchronized MeasurementWindow getCurrentWindow() {
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
                current_loc = current.getWindowOverlap();

                // Create an empty "next" measurement
                next = new MeasurementWindow(current.getWindowSize());

                final long timestamp = System.currentTimeMillis();
                System.err.println(String.format("Timestamp: %d ms", timestamp - benchmarkTimestamp));
                benchmarkTimestamp = timestamp;
            }
        }

        public synchronized void addToMeasurement(float[] values) {
            if (values.length != 3) {
                throw new IllegalArgumentException("Expected 3 values");
            }

            addNewWindowIfFull();

            current.addToMeasurement(values);
            if (current_loc >= current.getWindowOverlap()) {
                next.addToMeasurement(values);
            }
            current_loc++;
        }
    }


}
