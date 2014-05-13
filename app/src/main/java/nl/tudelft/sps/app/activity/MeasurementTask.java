package nl.tudelft.sps.app.activity;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.util.Log;

/**
 * An AsyncTask that takes a single Measurement in the background
 */
public class MeasurementTask extends AsyncTask<Activity, Integer, IMeasurement> implements SensorEventListener {

    private final Measurement.MonitorHelper measurement;

    /**
     * Object on which doInBackground can wait for the measurement to complete
     */
    private final Object gate = new Object();

    public static interface ProgressUpdater {
        public void update(Integer progress);
    }
    public static interface ResultProcessor {
        public void result(IMeasurement result);
    }

    private final ProgressUpdater progressUpdater;
    private final ResultProcessor resultProcessor;

    public MeasurementTask(ResultProcessor processor, ProgressUpdater updater, Measurement.MonitorHelper helper) {
        if (helper == null) {
            throw new NullPointerException("Helper is null");
        }
        progressUpdater = updater;
        resultProcessor = processor;
        measurement = helper;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (!measurement.isCompleted() && !isCancelled()) {
            measurement.addToMeasurement(sensorEvent.values);
            publishProgress(measurement.getProgress());
        }
        else {
            // We're done
            synchronized (gate) {
                gate.notify();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // Ignore
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        if (progressUpdater != null) {
            progressUpdater.update(values[0]);
        }
    }

    @Override
    protected void onPostExecute(IMeasurement result) {
        resultProcessor.result(result);
    }

    @Override
    protected IMeasurement doInBackground(Activity... activities) {
        final SensorManager sensorManager = (SensorManager) activities[0].getSystemService(Context.SENSOR_SERVICE);
        final Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        measurement.addNewWindowIfFull();

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);

        try {
            // The measurement should be done in well under 10 seconds.
            synchronized (gate) {
                gate.wait(10 * 1000);
            }
        }
        catch (InterruptedException e) {
            Log.w(getClass().getName(), "Measurement was interrupted");
        }

        sensorManager.unregisterListener(this);

        if (!measurement.isCompleted()) {
            if (isCancelled()) {
                publishProgress(0);
            }
            // Something went wrong or the timeout expired
            Log.w(getClass().getName(), "Did not create a complete measurement");
            return IMeasurement.INVALID_MEASUREMENT;
        }
        else {
            measurement.logCurrentNext();
            return measurement.getCurrentWindow();
        }
    }
}
