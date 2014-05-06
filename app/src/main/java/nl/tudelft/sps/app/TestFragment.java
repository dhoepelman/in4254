package nl.tudelft.sps.app;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

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
                    final ACTIVITY activity = mainActivity.getClassifier().classify(result);
                    valueResults.append(activity.name() + "\n");

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

        return rootView;
    }

    public void onStartTestsButtonClick(View view) {
        synchronized (updateLock) {
            doInfinitely.set(measureInfinitely);
            timesLeft.set(measureTimes);
        }

        doTest();
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
                toastManager.showText(String.format("%d remaining tests", timesRemaining), Toast.LENGTH_SHORT);
            }
            else {
                toastManager.showText("More, more and more nananana!", Toast.LENGTH_SHORT);
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

    public void onRBTimesClicked(View view) {
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
