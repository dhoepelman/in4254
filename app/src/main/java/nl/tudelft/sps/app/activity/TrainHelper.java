package nl.tudelft.sps.app.activity;

import com.j256.ormlite.dao.RuntimeExceptionDao;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

import nl.tudelft.sps.app.DatabaseHelper;
import nl.tudelft.sps.app.activity.MeasurementWindow.Helper;

/**
 * Helper to divide raw measurements into windows
 */
public class TrainHelper extends Helper {

    public final ACTIVITY activity;

    private final DatabaseHelper databaseHelper;

    protected MeasurementWindow current;
    protected int current_loc = 0;
    private MeasurementWindow next;
    private int numFullWindows = 0;
    private List<Sample> currentSamples;
    private List<Sample> nextSamples;
    private final int windowSize;

    public TrainHelper(ACTIVITY activity, DatabaseHelper dbhelper, int windowSize) {
        this.activity = activity;
        this.databaseHelper = dbhelper;
        this.windowSize = windowSize;
    }

    public boolean isFull() {
        return current_loc == current.getWindowSize();
    }

    public void removeIncompleteMeasurements() {
        // Make current null so that a new MeasurementWindow will be
        // created when addMeasurement() is called
        current = null;
    }

    public int getNumberOfFullWindows() {
        return numFullWindows;
    }

    public int getWindowSize() {
        return windowSize;
    }

    public void addMeasurement(float[] values, long timestamp) {
        if (values.length != 3) {
            throw new IllegalArgumentException("Expected 3 values");
        }

        if (current == null || isFull()) {
            if (current == null) {
                // We don't have any measurement, start one
                current = new MeasurementWindow(timestamp, activity, windowSize);
                currentSamples = new ArrayList<>(current.getWindowSize());
                current_loc = 0;
            } else {
                // Measurement was full, store it
                persistCurrentMeasurement();

                // Replace with next
                current = next;
                currentSamples = nextSamples;
                current_loc = current.getWindowOverlap();
                numFullWindows++;
            }
            // Create an empty "next" measurement
            next = new MeasurementWindow(timestamp, activity, current.getWindowSize());
            nextSamples = new ArrayList<>(next.getWindowSize());
        }

        currentSamples.add(new Sample(current, timestamp, values[0], values[1], values[2]));
        current.addToMeasurement(values);
        if (current_loc >= current.getWindowOverlap()) {
            nextSamples.add(new Sample(current, timestamp, values[0], values[1], values[2]));
            next.addToMeasurement(values);
        }
        current_loc++;
    }

    private void persistCurrentMeasurement() {
        final DatabaseSamplesWorker worker = new DatabaseSamplesWorker(databaseHelper, current, currentSamples);
        new Thread(worker).start();
    }

    private static class DatabaseSamplesWorker implements Runnable {

        private final DatabaseHelper databaseHelper;
        private final MeasurementWindow window;
        private final List<Sample> samples;

        private DatabaseSamplesWorker(DatabaseHelper databaseHelper, MeasurementWindow window, List<Sample> samples) {
            // I know checking for a magic number is kind of ugly, but we are assuming here
            // that the size is 240 so that we can divide it by 4 in order to get 4 windows
            // of 60 samples for the StepsCounter
            assert window.getWindowSize() == 240;

            this.databaseHelper = databaseHelper;
            this.window = window;
            this.samples = samples;
        }

        @Override
        public void run() {
            // TODO split the window and the list of samples into four
            // new smaller windows and add it to the database as well

            window.calculateFeatureVector();

            // Create a new MeasurementWindow in the current thread
            databaseHelper.getMeasurementDao().create(window);

            final RuntimeExceptionDao<Sample, Void> sampleDao = databaseHelper.getSampleDao();

            sampleDao.callBatchTasks(new Callable<Void>() {
                @Override
                public Void call() {
                    System.err.println("Creating Samples...");
                    final long currentTimestamp = System.currentTimeMillis();
                    for (Sample sample : samples) {
                        sampleDao.create(sample);
                    }
                    final long duration = System.currentTimeMillis() - currentTimestamp;
                    System.err.println(String.format("%d samples created in %d ms", samples.size(), duration));
                    return null;
                }
            });
        }
    }
}