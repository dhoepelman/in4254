package nl.tudelft.sps.app;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Environment;

import java.io.*;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nl.tudelft.sps.app.activity.*;

public class MeasureFragment extends Fragment implements SensorEventListener {

    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    private View rootView;

    private TextView labelWindows;
    private TextView labelActivity;

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private final Measurement.MonitorHelper measurementHelper = new Measurement.MonitorHelper();

    private final ActivityClassifier classifier = new ActivityClassifier();

    private int numberOfWindows = 0;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static MeasureFragment newInstance(int sectionNumber) {
        final Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);

        final MeasureFragment fragment = new MeasureFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_measure, container, false);
        assert rootView != null;

        labelWindows = (TextView) rootView.findViewById(R.id.label_windows);
        labelActivity = (TextView) rootView.findViewById(R.id.label_activity);

        labelWindows.setText(String.format("%d", numberOfWindows));
        labelActivity.setText("TEST");

        // Set up accelerometer
        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        readTrainingData();

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
    }

    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
            case Sensor.TYPE_LINEAR_ACCELERATION:
                if (measurementHelper.addMeasurement(event.values)) {
                    // Process current window
                    numberOfWindows++;
                    labelWindows.setText(String.format("%d", numberOfWindows));

                    // Perform the classification
                    final ACTIVITY classifiedActivity = classifier.classify(measurementHelper.getCurrentWindow());
//                    final ACTIVITY classifiedActivity = ACTIVITY.RUNNING;

                    // Print classifiedActivity on the screen
                    labelActivity.setText(String.valueOf(classifiedActivity));

                    // Clean the window so that it is empty when the
                    // first future sensor data is added
                    measurementHelper.cleanWindow();
                }
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Ignore
    }

    @Override
    public void onResume() {
        super.onResume();
        startMeasuring();
    }

    private void startMeasuring() {
        // Clean the window
        measurementHelper.cleanWindow();

        // Start listening
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            sensorManager.unregisterListener(this);
        } catch (NullPointerException e) {
            // Listener was already unregistered or never registered
        }
    }

    private void readTrainingData() {
        // Read TrainFragment.RESULTS_FILE_PATH and do classifier.train(measurement_of_current_line)
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            final File resultsFile = new File(TrainFragment.RESULTS_FILE_PATH);
            if (resultsFile.exists()) {
                try {
                    final BufferedReader reader = new BufferedReader(new FileReader(resultsFile));

                    // Read every line of the file
                    String line;
                    int processedMeasurements = 0;
                    while ((line = reader.readLine()) != null) {
                        final String[] values = line.split(",", 2);
                        // Extract the features and put the data from each line into its own IMeasurement instance
                        final IMeasurement measurement = new TrainedMeasurement(values[1]);
                        classifier.train(ACTIVITY.valueOf(values[0]), measurement);
                        processedMeasurements++;
                    }
                    displayToast(String.format("Added %d measurements to the classifier", processedMeasurements));
                }
                catch (IOException exception) {
                    displayToast("Failed to read training data");
                }
            }
            else {
                displayToast(String.format("%s does not exist", resultsFile.getName()));
            }
        }
        else {
            displayToast("External storage not mounted");
        }
    }

    /**
     * Display the message as a toast
     */
    private void displayToast(String message) {
        Toast.makeText(getActivity().getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

}
