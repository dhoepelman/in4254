package nl.tudelft.sps.app.activity;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.apache.commons.math3.ml.distance.EuclideanDistance;
import org.apache.commons.math3.transform.TransformType;

import java.util.ArrayList;
import java.util.List;

public class StepsCounter implements Runnable, SensorEventListener {

    public static final int WINDOW_SIZE = 128;

    private final FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
    private final DistanceMeasure distance = new EuclideanDistance();

    private final SensorManager sensorManager;
    private final Sensor accelerometer;

    private final List<Double> samples = new ArrayList<Double>();

    /**
     * A lock used by onSensorChanged() to signal to run() that a window
     * has been filled
     */
    private final Object gate = new Object();

    /**
     * A variable used to indicate that the thread should stop running
     */
    private boolean keepRunning = true;

    public StepsCounter(Activity activity) {
        super();

        // Set-up sensor manager
        sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
    }

    public void stopMeasuring() {
        keepRunning = false;
    }

    public void run() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);

        // Loop to collect windows
        try {
            while (keepRunning) {
                try {
                    samples.clear();

                    // Collecting a window should be done in less than a second
                    synchronized (gate) {
                        gate.wait(10 * 1000L);
                    }

                    final double[] samplesArray = new double[samples.size()];
                    for (int i = 0; i < samplesArray.length; i++) {
                        samplesArray[i] = samples.get(i);
                    }

                    System.err.println(String.format("%d", samplesArray.length));

                    // Transform the acceleration magnitudes to complex transforms
                    final Complex[] complexTransforms = transformer.transform(samplesArray, TransformType.FORWARD);
                    final double[] magnitudeTransforms = new double[complexTransforms.length];

                    // Collect the magnitudes of the complex transforms
                    for (int i = 0; i < complexTransforms.length; i++) {
                        final double realPart = complexTransforms[i].getReal();
                        final double imagPart = complexTransforms[i].getImaginary();

                        magnitudeTransforms[i] = Math.sqrt(realPart * realPart + imagPart * imagPart);
                    }

                    // Print the frequencies
                    // How to use: copy the text lines from the debugger screen to a text file
                    // and then process it and run gnuplot
                    // Image generated by gnuplot-fft-lines gives:
                    //     index as x-coordinate
                    //     frequency as y-coordinate
                    // Image generated by gnuplot-fft gives:
                    //     frequency as x-coordinate
                    //     size of adjacent frequencies as y-coordinate
                    //     (adjacent frequencies are grouped together in a box,
                    //     each box is painted as a red line)
                    System.err.println("FOURIER START");
                    for (int i = 0; i < magnitudeTransforms.length; i++) {
                        System.err.println(String.format("%d %f", i, magnitudeTransforms[i]));
                        // TODO Perhaps add the data to the database so you don have to be connected to the debugger
                    }
                    System.err.println("FOURIER END");

                    // TODO Perhaps use SummaryStatistics to make a detect a step or idleness

                    // TODO For reach window, determine the user is idle or took a
                    // TODO step by looking at the frequencies in magnitudeTransforms

                    // TODO If the user took a step, increment the step counter
                    // TODO If the user is idle, call locator.addMovement()
                }
                catch (InterruptedException exception) {
                    // Do nothing if we got interrupted while waiting for the gate lock
                }
            }
        }
        finally {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        final double magnitudeEvent = distance.compute(new double[] { event.values[0], event.values[1], event.values[2]}, new double[] {0.0, 0.0, 0.0});
        samples.add(magnitudeEvent);

        if (samples.size() == WINDOW_SIZE) {
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