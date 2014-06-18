package nl.tudelft.sps.app.activity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import nl.tudelft.sps.app.LocatorTestFragment;
import nl.tudelft.sps.app.MainActivity;
import nl.tudelft.sps.app.localization.ILocator;

public class StepsCounter implements Runnable, SensorEventListener {

    public static final int STEPS_PER_UPDATE = 8;

    /**
     * 60 scans takes about 800 ms on Galaxy S
     */
    public static final int WINDOW_SIZE = MeasurementWindow.WINDOW_SIZE;// / 4;
    private final MeasurementWindow.MonitorHelper measurement = new MeasurementWindow.MonitorHelper(WINDOW_SIZE);
    private final SensorManager sensorManager;
    private final Sensor accelerometer;
    /**
     * A lock used by onSensorChanged() to signal to run() that a window
     * has been filled
     */
    private final Object gate = new Object();
    private final MainActivity activity;
    private final LocatorTestFragment fragment;
    private final ILocator locator;
    /**
     * A variable used to indicate that the thread should stop running
     */
    private boolean keepRunning = true;

    public StepsCounter(LocatorTestFragment fragment) {
        super();

        this.fragment = fragment;
        this.activity = (MainActivity) fragment.getActivity();
        this.locator = activity.getLocator();

        // Set-up sensor manager
        sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
    }

    public void stopMeasuring() {
        keepRunning = false;
    }

    public void run() {
        int steps = 0;

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);

        // Loop to collect windows
        try {
            while (keepRunning) {
                try {
                    // Collecting a window should be done in less than a second
                    synchronized (gate) {
                        gate.wait(5 * 1000L);
                    }

                    if (!keepRunning) {
                        break;
                    }

                    // Quickly get the current window and then make a new one to make
                    // onSensorChanged() happy
                    final IMeasurement result = measurement.getCurrentWindow();
                    measurement.addNewWindowIfFull();

                    // For reach window, determine the user is idle or took a
                    final ACTIVITY actualActivity = activity.getClassifier(WINDOW_SIZE).classify(result);

                    System.out.println(String.format("Measured activity %s", actualActivity.name()));



                    if (ACTIVITY.Sitting == actualActivity) {
                        if (steps > 0) {
                            fragment.doMovementDetection(steps);
                            steps = 0;
                        }
                    }
                    else {
                        // If the user took a step, increment the step counter
                        steps += 4;

                        if ((steps % STEPS_PER_UPDATE) == 0) {
                            fragment.doMovementDetection(steps);
                            steps = 0;
                        }
                    }
                }
                catch (InterruptedException exception) {
                    // Do nothing if we got interrupted while waiting for the gate lock
                    keepRunning = false;
                }
            }
        }
        finally {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // The first time, the classifier takes a long time (no overlapping windows yet),
        // which causes a lot of events to be dropped on the floor
        if (!measurement.isCompleted() && keepRunning) {
            measurement.addToMeasurement(event.values);
        }
        else {
            synchronized (gate) {
                gate.notify();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // Ignore
    }

}
