package nl.tudelft.sps.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Throwables;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.primitives.Doubles;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import nl.tudelft.sps.app.activity.ACTIVITY;
import nl.tudelft.sps.app.activity.IMeasurement;
import nl.tudelft.sps.app.activity.Measurement;

/**
 * Fragment to train the activity detection
 */
public class TrainFragment extends Fragment implements SensorEventListener {

    /**
     * File to which the measured acceleration values are written
     */
    public static final String RESULTS_FILE_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/features.csv";
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    private static final String LOG_TAG = TrainFragment.class.toString();
    private BiMap<Integer, ACTIVITY> activity_buttons = new ImmutableBiMap.Builder<Integer, ACTIVITY>()
            .put(R.id.but_sitting, ACTIVITY.Sitting)
            .put(R.id.but_walking, ACTIVITY.Walking)
            .put(R.id.but_running, ACTIVITY.Running)
            .put(R.id.but_stairsup, ACTIVITY.Stairs_Up)
            .put(R.id.but_stairsdown, ACTIVITY.Stairs_Down)
            .build();
    private View rootView;
    private TextView valueMeasurement;
    private TextView valueNumberOfWindows;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long measurementStart;

    private ACTIVITY selectedActivity;
    private Measurement.TrainHelper measurementHelper;

    /**
     * Returns a new instance of this fragment for the given section number
     */
    public static TrainFragment newInstance(int sectionNumber) {
        final Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);

        final TrainFragment fragment = new TrainFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_train, container, false);
        assert rootView != null;
        valueMeasurement = (TextView) rootView.findViewById(R.id.val_measuring);
        valueNumberOfWindows = (TextView) rootView.findViewById(R.id.val_num_windows);

        // Register the button listeners
        for (int buttonId : activity_buttons.keySet()) {
            rootView.findViewById(buttonId).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onSelectActivityButtonClick(view);
                }
            });
        }

        // Register the start button
        rootView.findViewById(R.id.but_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onStartTrainingButtonClick(view);
            }
        });

        // Register the stop button
        rootView.findViewById(R.id.but_stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onStopTrainingButtonClick(view);
            }
        });

        // Make all activity buttons gray
        colorSelectedButton();

        // Set-up accelerometer
        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
    }

    public void onSensorChanged(SensorEvent event) {
        // We received new data from one of the sensors

        //Log.d(LOG_TAG, "Type: " + event.sensor.getType());

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
            case Sensor.TYPE_LINEAR_ACCELERATION:
                measurementHelper.addMeasurement(event.values, event.timestamp);

                final double value = (System.currentTimeMillis() - measurementStart) / 1000.0;
                valueMeasurement.setText(String.format("%.1f", value));
                valueNumberOfWindows.setText(Integer.toString(measurementHelper.getNumberOfFullWindows()));
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Ignore
    }

    private void startMeasuring() {
        measurementStart = System.currentTimeMillis();

        // Create an empty helper
        measurementHelper = new Measurement.TrainHelper(selectedActivity);

        // Start listening
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    private void stopMeasuring() {
        try {
            sensorManager.unregisterListener(this);
        } catch (NullPointerException e) {
            // Listener was already unregistered or never registered
        }
        measurementHelper.removeIncompleteMeasurements();
        writeBuffer();
        ((MainActivity)getActivity()).resetClassifier();
    }

    private void writeBuffer() {
        try {
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                    && !Environment.MEDIA_MOUNTED_READ_ONLY.equals(Environment.getExternalStorageState())) {

                final File resultsFile = new File(RESULTS_FILE_PATH);

                if (!resultsFile.exists()) {
                    resultsFile.createNewFile();
                }

                final PrintWriter writer = new PrintWriter(new FileOutputStream(resultsFile, true));

                for (IMeasurement measurement : measurementHelper.getMeasurements()) {
                    writer.append(selectedActivity.name());
                    writer.append(",");
                    writer.append(Doubles.join(",", measurement.getFeatureVector()));
                    writer.append("\n");
                }
                writer.close();

                final String message = String.format("Succesfully written buffer to %s", RESULTS_FILE_PATH);
                Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
                Log.i(LOG_TAG, message);
            } else {
                final String message = "Could not write buffer: external storage not mounted";
                Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
                Log.w(LOG_TAG, message);
            }
        } catch (IOException e) {
            final String message = String.format("Could not create or write results file %s", RESULTS_FILE_PATH);
            Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
            Log.w(LOG_TAG, message);
            Log.d(LOG_TAG, Throwables.getStackTraceAsString(e));
        }
    }

    public void onSelectActivityButtonClick(final View view) {
        selectedActivity = activity_buttons.get(view.getId());
        colorSelectedButton();
    }

    public void onStartTrainingButtonClick(final View view) {
        if (selectedActivity == null) {
            Toast.makeText(getActivity(), "Select an activity first", Toast.LENGTH_SHORT).show();
        }
        else {
            // Disable the start button
            view.setEnabled(false);

            startMeasuring();

            // Enable the stop button
            final View stopButton = getView().findViewById(R.id.but_stop);
            stopButton.setEnabled(true);
        }
    }

    public void onStopTrainingButtonClick(final View view) {
        // Disable the stop button
        view.setEnabled(false);

        stopMeasuring();

        // Enable the start button
        final View startButton = getView().findViewById(R.id.but_start);
        startButton.setEnabled(true);
    }


    /**
     * (Visually) select the right button
     */
    private void colorSelectedButton() {
        for (int buttonId : activity_buttons.keySet()) {
            rootView.findViewById(buttonId).setBackgroundColor(Color.GRAY);
        }
        if (selectedActivity != null) {
            rootView.findViewById(activity_buttons.inverse().get(selectedActivity)).setBackgroundColor(Color.BLACK);
        }
    }

}
