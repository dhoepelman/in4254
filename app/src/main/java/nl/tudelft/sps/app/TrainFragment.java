package nl.tudelft.sps.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import nl.tudelft.sps.app.activity.ACTIVITY;
import nl.tudelft.sps.app.activity.IMeasurement;
import nl.tudelft.sps.app.activity.Measurement;
import nl.tudelft.sps.app.activity.MeasurerHeadlessFragment;

/**
 * Fragment to train the activity detection
 */
public class TrainFragment extends Fragment implements MeasurerHeadlessFragment.Callback {

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
            .put(R.id.but_sitting, ACTIVITY.SITTING)
            .put(R.id.but_walking, ACTIVITY.WALKING)
            .put(R.id.but_running, ACTIVITY.RUNNING)
            .put(R.id.but_stairsup, ACTIVITY.STAIRS_UP)
            .put(R.id.but_stairsdown, ACTIVITY.STAIRS_DOWN)
            .build();
    private View rootView;
    private TextView valueMeasurement;
    private TextView valueNumberOfWindows;

    private long measurementStart;

    private ACTIVITY selectedActivity;
    public static final String TAG_MEASURER_FRAGMENT = "trainfragment_measurer";
    private MeasurerHeadlessFragment measurerFragment;

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

        // Try to get or create the measurer fragment
        FragmentManager fm = getFragmentManager();
        measurerFragment = (MeasurerHeadlessFragment) fm.findFragmentByTag(TAG_MEASURER_FRAGMENT);
        if(measurerFragment == null) {
            // Fragment doesn't exist, create
            measurerFragment = new MeasurerHeadlessFragment();
            fm.beginTransaction().add(measurerFragment, TAG_MEASURER_FRAGMENT).commit();
        }
        measurerFragment.setOwner(this);

        if(savedInstanceState != null) {
            measurements = (Collection<IMeasurement>)savedInstanceState.getSerializable("measurements");
            measurementStart = savedInstanceState.getLong("measurementStart");
        }

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(measurements != null && measurements.size() > 0) {
            // We are currently measuring, save the measurements and starting time
            outState.putSerializable("measurements", (Serializable)measurements);
            outState.putLong("measurementStart", measurementStart);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        measurerFragment.setOwner(null);
    }

    private Collection<IMeasurement> measurements;
    private void startMeasuring() {
        measurementStart = System.currentTimeMillis();
        measurements = new ArrayList<>();
        measurerFragment.startMeasuring();
    }

    private void stopMeasuring() {
        measurerFragment.stopMeasuring();
        writeBuffer();
    }

    private void writeBuffer() {
        try {
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                    && !Environment.MEDIA_MOUNTED_READ_ONLY.equals(Environment.getExternalStorageState())) {

                final File resultsFile = new File(RESULTS_FILE_PATH);
                String header = null;
                if (!resultsFile.exists()) {
                    resultsFile.createNewFile();
                    header = "ACTIVITY,MeanX,MeanY,MeanZ,StdDevX,StdDevY,StdDevZ,CorrXY,CorrYZ,CorrZX\n";
                }

                final PrintWriter writer = new PrintWriter(new FileOutputStream(resultsFile, true));
                if (header != null) {
                    writer.append(header);
                }

                for (IMeasurement measurement : measurements) {
                    writer.append(selectedActivity.name());
                    writer.append(",");
                    writer.append(Doubles.join(",", measurement.getFeatureVector()));
                    writer.append("\n");
                }
                measurements.clear();
                writer.close();

                final String msg = String.format("Succesfully written buffer to %s", RESULTS_FILE_PATH);
                Log.i(LOG_TAG, msg);
                Toast.makeText(getActivity().getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            } else {
                logAndDisplayToast("Could not write buffer: external storage not mounted");
            }
        } catch (IOException e) {
            logAndDisplayToast(String.format("Could not create or write results file %s", RESULTS_FILE_PATH));
            Log.d(LOG_TAG, Throwables.getStackTraceAsString(e));
        }
    }

    public void onSelectActivityButtonClick(final View view) {
        selectedActivity = activity_buttons.get(view.getId());
        colorSelectedButton();
    }

    public void onStartTrainingButtonClick(final View view) {
        if (selectedActivity == null) {
            displayToast("Select an activity first");
        } else {
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

    /**
     * Log the message to LOG_TAG and display it as a toast in the
     * current activity.
     */
    private void logAndDisplayToast(String message) {
        Log.w(LOG_TAG, message);
        displayToast(message);
    }

    /**
     * Display the message as a toast
     */
    private void displayToast(String message) {
        Toast.makeText(getActivity().getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCompletedMeasurement(IMeasurement m) {
        measurements.add(m);
        final double value = (System.currentTimeMillis() - measurementStart) / 1000.0;
        valueMeasurement.setText(String.format("%.1f", value));
        valueNumberOfWindows.setText(Integer.toString(measurements.size()));
    }
}
