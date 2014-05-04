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
public class MeasurementTask extends AsyncTask<Activity, Double, IMeasurement> implements SensorEventListener {

    private final Measurement m = new Measurement();

    /**
     * Object on which doInBackground can wait for the measurement to complete
     */
    private final Object gate = new Object();

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(!m.isCompleted()) {
            m.addToMeasurement(sensorEvent.values);
            publishProgress(m.percentageCompleted());
        } else {
            // We're done
            gate.notify();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // ignore
    }

    @Override
    protected IMeasurement doInBackground(Activity... activities) {
        SensorManager sensorManager = (SensorManager) activities[0].getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);

        try {
            // The measurement should be done in well under 10 seconds.
            gate.wait(10*1000);
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
