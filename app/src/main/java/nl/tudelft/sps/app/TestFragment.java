package nl.tudelft.sps.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Throwables;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import nl.tudelft.sps.app.activity.ACTIVITY;
import nl.tudelft.sps.app.activity.IMeasurement;
import nl.tudelft.sps.app.activity.MeasurementTask;

/**
 * Fragment for testing the classifier
 */
public class TestFragment extends Fragment {

    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    private final MeasurementTask.ResultProcessor measurementResultProcessor = new MeasurementTask.ResultProcessor() {
        @Override
        public void result(IMeasurement result) {
            try {
                final MainActivity mainActivity = (MainActivity) getActivity();
                if (mainActivity != null) {
                    final ACTIVITY actualActivity = mainActivity.getClassifier().classify(result);
                    valueResults.append(String.format("Actual: %s\tExpected: %s\n", actualActivity.name(), selectedActivity.name()));

                    // Log the result so it can be processed later to create a confusion matrix
                    writeResult(actualActivity, selectedActivity); // TODO write results asynchronous (it seems to slow down the start of the next measurement)

                    // Do more tests if needed
                    doTest();
                }
            }
            catch (IllegalStateException e) {
                toastManager.showText("Please train the classifier first", Toast.LENGTH_LONG);
            }
        }
    };

    private final MeasurementTask.ProgressUpdater measurementProgressUpdater = new MeasurementTask.ProgressUpdater() {
        @Override
        public void update(Integer progress) {
            ((ProgressBar) rootView.findViewById(R.id.progressBar_measurement)).setProgress(progress);
        }
    };

    private View rootView;
    private TextView valueResults;

    // Race conditions should not occur, but better safe than sorry
    private final AtomicInteger timesLeft = new AtomicInteger(0);
    private final AtomicBoolean doInfinitely = new AtomicBoolean(false);

    /**
     * A reference to keep track of the current task. It is used to
     * clean up the current task when creating and executing a new task,
     * because otherwise the user can spawn many executing tasks and
     * the progress bar will go haywire.
     */
    private final AtomicReference<MeasurementTask> currentTask = new AtomicReference<MeasurementTask>(null);

    private ToastManager toastManager;

    /**
     * A Lock to make sure that the updates of both timesLeft and
     * doInfinitely happen atomically.
     */
    private final Object updateLock = new Object();

    private int measureTimes = 1;
    private boolean measureInfinitely = false;

    private int[] RBTimesList = new int[] {
        R.id.rb_times_1,
        R.id.rb_times_5,
        R.id.rb_times_10,
        R.id.rb_times_50,
        R.id.rb_times_infinite
    };

    private BiMap<Integer, ACTIVITY> activity_buttons = new ImmutableBiMap.Builder<Integer, ACTIVITY>()
        .put(R.id.but_test_sitting, ACTIVITY.SITTING)
        .put(R.id.but_test_walking, ACTIVITY.WALKING)
        .put(R.id.but_test_running, ACTIVITY.RUNNING)
        .put(R.id.but_test_stairsup, ACTIVITY.STAIRS_UP)
        .put(R.id.but_test_stairsdown, ACTIVITY.STAIRS_DOWN)
        .build();

    private ACTIVITY selectedActivity;

    private static final String LOG_TAG = TestFragment.class.toString();

    /**
     * File to which the measured acceleration values are written
     */
    public static final String RESULTS_FILE_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/testing.csv";

    /**
     * Returns a new instance of this fragment for the given section number
     */
    public static TestFragment newInstance(int sectionNumber) {
        final Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);

        final TestFragment fragment = new TestFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private void writeResult(ACTIVITY actual, ACTIVITY expected) {
        final long timestamp = System.currentTimeMillis() / 1000L;

        try {
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                    && !Environment.MEDIA_MOUNTED_READ_ONLY.equals(Environment.getExternalStorageState())) {

                final File resultsFile = new File(RESULTS_FILE_PATH);

                if (!resultsFile.exists()) {
                    resultsFile.createNewFile();
                }

                // Write the data to RESULTS_FILE_PATH
                final PrintWriter writer = new PrintWriter(new FileOutputStream(resultsFile, true));
                writer.append(String.format("%d,%s,%s\n", timestamp, actual.name(), expected.name()));
                writer.close();

                final String message = String.format("Succesfully written results to %s", RESULTS_FILE_PATH);
                toastManager.showText(message, Toast.LENGTH_SHORT);
                Log.i(LOG_TAG, message);
            }
            else {
                final String message = "Could not write buffer: external storage not mounted";
                toastManager.showText(message, Toast.LENGTH_SHORT);
                Log.w(LOG_TAG, message);
            }
        }
        catch (IOException e) {
            final String message = String.format("Could not create or write results file %s", RESULTS_FILE_PATH);
            toastManager.showText(message, Toast.LENGTH_LONG);
            Log.w(LOG_TAG, message);
            Log.d(LOG_TAG, Throwables.getStackTraceAsString(e));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_test, container, false);
        assert rootView != null;

        toastManager = new ToastManager(getActivity());

        valueResults = (TextView) rootView.findViewById(R.id.val_testresults);

        // Connect click listener to radio buttons
        for (int rb : RBTimesList) {
            ((RadioButton) rootView.findViewById(rb)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onRBTimesClicked(view);
                }
            });
        }

        // Connect click listener to start button
        ((Button) rootView.findViewById(R.id.but_test_start)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onStartTestsButtonClick(view);
            }
        });

        // Connect click listener to stop button
        ((Button) rootView.findViewById(R.id.but_test_stop)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onStopTestsButtonClick(view);
            }
        });

        // Register the button listeners
        for (int buttonId : activity_buttons.keySet()) {
            rootView.findViewById(buttonId).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onClickActivityButton(view);
                }
            });
        }

        // Make all activity buttons gray
        colorSelectedButton();

        return rootView;
    }

    public void onClickActivityButton(final View view) {
        selectedActivity = activity_buttons.get(view.getId());
        colorSelectedButton();
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

    public void onStartTestsButtonClick(final View view) {
        if (selectedActivity == null) {
            toastManager.showText("Select an activity first", Toast.LENGTH_SHORT);
        }
        else {
            synchronized (updateLock) {
                doInfinitely.set(measureInfinitely);
                timesLeft.set(measureTimes);
            }

            doTest();
        }
    }

    public void onStopTestsButtonClick(final View view) {
        final MeasurementTask oldTask = currentTask.get();
        if (oldTask != null) {
            oldTask.cancel(false);
        }
    }

    public void doTest() {
        final boolean infinitelyRemaining;
        final int timesRemaining;

        // Update the two variables atomically
        synchronized (updateLock) {
            infinitelyRemaining = doInfinitely.get();
            timesRemaining = timesLeft.getAndDecrement();
        }

        if (infinitelyRemaining || timesRemaining > 0) {
            if (!infinitelyRemaining) {
                // TODO for some reasons the 2nd .. nth toast gets shown only after half way during the measurement (1st gets displayed immediately)
                toastManager.showText(String.format("%d remaining tests", timesRemaining), Toast.LENGTH_SHORT);
            }
            else {
                // TODO for some reasons the 2nd .. nth toast gets shown only after half way during the measurement (1st gets displayed immediately)
                toastManager.showText("Many more remaining", Toast.LENGTH_SHORT);
            }

            final MeasurementTask task = new MeasurementTask(measurementResultProcessor, measurementProgressUpdater);

            // Clean up the old task to prevent building up many running
            // tasks if the user hits the start button many times
            final MeasurementTask oldTask = currentTask.getAndSet(task);
            if (oldTask != null) {
                oldTask.cancel(false);
            }

            // Start executing the new task
            task.execute(getActivity());
        }
        else {
            toastManager.showText("Finished testing", Toast.LENGTH_LONG);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
    }

    public void onRBTimesClicked(final View view) {
        final RadioButton radioButton = (RadioButton) view;
        if (radioButton.isChecked()) {
            switch (view.getId()) {
                default:
                case R.id.rb_times_1:
                    measureTimes = 1;
                    measureInfinitely = false;
                    break;
                case R.id.rb_times_5:
                    measureTimes = 5;
                    measureInfinitely = false;
                    break;
                case R.id.rb_times_10:
                    measureTimes = 10;
                    measureInfinitely = false;
                    break;
                case R.id.rb_times_50:
                    measureTimes = 50;
                    measureInfinitely = false;
                    break;
                case R.id.rb_times_infinite:
                    measureTimes = 1;
                    measureInfinitely = true;
                    break;
            }

            // Android is unable to uncheck the other radio buttons by
            // itself if the radio group contains linear layouts
            for (int rb : RBTimesList) {
                final RadioButton otherRadioButton = (RadioButton) rootView.findViewById(rb);
                if (!radioButton.equals(otherRadioButton)) {
                    otherRadioButton.setChecked(false);
                }
            }
        }
    }
}
