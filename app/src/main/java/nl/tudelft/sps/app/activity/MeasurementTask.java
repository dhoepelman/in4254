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
 * A AsyncTask that takes a single Measurement in the background
 */
public class MeasurementTask extends AsyncTask<Activity, Integer, IMeasurement> implements SensorEventListener {

    private final Measurement m = new Measurement();

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

    public MeasurementTask(ResultProcessor rp) {
        this(rp, null);
    }

    public MeasurementTask(ResultProcessor rp,ProgressUpdater pu) {
        this.progressUpdater = pu;
        this.resultProcessor = rp;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(!m.isCompleted()) {
            m.addToMeasurement(sensorEvent.values);
            publishProgress(m.getProgress());
        } else {
            // We're done
            synchronized (gate) {
                gate.notify();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // ignore
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        if(progressUpdater != null) {
            progressUpdater.update(values[0]);
        }
    }

    @Override
    protected void onPostExecute(IMeasurement result) {
        resultProcessor.result(result);
    }

    @Override
    protected IMeasurement doInBackground(Activity... activities) {
        SensorManager sensorManager = (SensorManager) activities[0].getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);

        try {
            // The measurement should be done in well under 10 seconds.
            synchronized (gate) {
                gate.wait(10 * 1000);
            }
        } catch (InterruptedException e) {
            Log.w(getClass().getName(), "Measurement was interrupted");
        }

        sensorManager.unregisterListener(this);

        if(!m.isCompleted()) {
            // Something went wrong or the timeout expired
            Log.w(getClass().getName(), "Did not create a complete measurement");
            return IMeasurement.INVALID_MEASUREMENT;
        } else {
            return m;
        }
    }
}
