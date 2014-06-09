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
    private final List<MeasurementWindow> measurementWindows = new ArrayList<>();

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

    /**
     * Get all measurements
     */
    public List<MeasurementWindow> getMeasurementWindows() {
        return measurementWindows;
    }

    public void removeIncompleteMeasurements() {
        // Remove the last measurement if it's incomplete
        while (!measurementWindows.isEmpty() && !measurementWindows.get(measurementWindows.size() - 1).isCompleted()) {
            measurementWindows.remove(measurementWindows.size() - 1);
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
                current = new MeasurementWindow(timestamp, activity, windowSize);
                currentSamples = new ArrayList<>(current.getWindowSize());
                current_loc = 0;
                measurementWindows.add(current);
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
            measurementWindows.add(next);
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
        current.calculateFeatureVector();

        final DatabaseSamplesWorker worker = new DatabaseSamplesWorker(databaseHelper, current, currentSamples);
        new Thread(worker).start();
    }

    private static class DatabaseSamplesWorker implements Runnable {

        private final Iterator<Sample> snapshot;
        private final DatabaseHelper databaseHelper;

        private DatabaseSamplesWorker(DatabaseHelper databaseHelper, MeasurementWindow window, List<Sample> samples) {
            // Create a snapshot, so we can safely iterate over it
            snapshot = new CopyOnWriteArrayList(samples).iterator();

            // Create a new MeasurementWindow in the current thread
            databaseHelper.getMeasurementDao().create(window);

            this.databaseHelper = databaseHelper;
        }

        @Override
        public void run() {
            final RuntimeExceptionDao<Sample, Void> sampleDao = databaseHelper.getSampleDao();

            sampleDao.callBatchTasks(new Callable<Void>() {
                @Override
                public Void call() {
                    while (snapshot.hasNext()) {
                        sampleDao.create(snapshot.next());
                    }
                    return null;
                }
            });
        }
    }
}