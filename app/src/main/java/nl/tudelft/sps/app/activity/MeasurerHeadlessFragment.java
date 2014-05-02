package nl.tudelft.sps.app.activity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Headless fragment that takes measurements. Either a predefined number or until you tell it to stop
 */
public class MeasurerHeadlessFragment extends Fragment implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private volatile Callback owner;
    private int targetNumberOfMeasurements = 0;
    private int currentNumberOfMeasurements = 0;
    private Measurement.Helper measurementHelper;

    public void setOwner(Callback owner) {
        this.owner = owner;
        if(bufferedMeasurements.size() > 0) {
            for(IMeasurement m : bufferedMeasurements) {
                deliverMeasurement(m);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retain this fragment across configuration changes.
        setRetainInstance(true);

        // Set-up accelerometer
        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
    }

    private void startMeasuring(int number) {
        // Create an empty helper
        measurementHelper = new Measurement.Helper();
        targetNumberOfMeasurements = number;
        currentNumberOfMeasurements = 0;
        // Start listening
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);

    }

    /**
     * Start measuring indefinitly untill stopMeasuring() is called
     */
    public void startMeasuring() {
        startMeasuring(0);
    }

    /**
     * Take a specific number of measurements
     *
     * @param number The number of measurements to make
     */
    public void takeMeasurements(int number) {
        startMeasuring(number);
    }

    /**
     * Take a measurement
     */
    public void takeMeasurement() {
        takeMeasurements(1);
    }

    /**
     * Stop measuring and return the collected measurements
     */
    public void stopMeasuring() {
        try {
            sensorManager.unregisterListener(this);
        } catch (NullPointerException e) {
            // Listener was already unregistered or never registered
        }
        measurementHelper = null;
    }

    public void onSensorChanged(SensorEvent event) {
        // We received new data from one of the sensors

        if (targetNumberOfMeasurements != 0 && currentNumberOfMeasurements >= targetNumberOfMeasurements) {
            stopMeasuring();
            return;
        }

        //Log.d(LOG_TAG, "Type: " + event.sensor.getType());

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
            case Sensor.TYPE_LINEAR_ACCELERATION:
                Measurement m = measurementHelper.addMeasurement(event.values);
                if (m.isCompleted()) {
                    currentNumberOfMeasurements++;
                    if(owner != null) {
                        deliverMeasurement(m);
                    } else {
                        bufferedMeasurements.add(m);
                    }
                }
        }
    }

    private Collection<IMeasurement> bufferedMeasurements = new ArrayList<>();
    public void deliverMeasurement(IMeasurement m) {
        // Dispatch completed measurement to owner
        owner.onCompletedMeasurement(m);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Ignore
    }

    public static interface Callback {
        void onCompletedMeasurement(IMeasurement m);
    }


}
