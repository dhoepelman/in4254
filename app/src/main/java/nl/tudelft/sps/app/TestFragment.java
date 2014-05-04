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

import java.util.concurrent.atomic.AtomicInteger;

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
    private final MeasurementTask.ResultProcessor MeasurementResultProcessor = new MeasurementTask.ResultProcessor() {
        @Override
        public void result(IMeasurement result) {
            ACTIVITY res = ((MainActivity) getActivity()).getClassifier().classify(result);
            ((TextView) rootView.findViewById(R.id.val_testresults)).append(res.name() + " from " + result.toString() + "\n");
            doTest(); // Do more tests if needed
        }
    };
    private final MeasurementTask.ProgressUpdater MeasurementProgressUpdater = new MeasurementTask.ProgressUpdater() {
        @Override
        public void update(Integer progress) {
            ((ProgressBar) rootView.findViewById(R.id.progressBar_measurement)).setProgress(progress);
        }
    };
    private View rootView;
    // Race conditions shouldn't occur but better safe than sorry
    private AtomicInteger times_left = new AtomicInteger(0);
    private int measure_times = 1;
    private int[] RBTimesList = new int[]{R.id.rb_times_1, R.id.rb_times_5, R.id.rb_times_10, R.id.rb_times_50};

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
        for (int rb : RBTimesList) {
            ((RadioButton) rootView.findViewById(rb)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onRBTimesClicked(view);
                }
            });
        }
        ((Button) rootView.findViewById(R.id.but_test_start)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onStartTestsButtonClick(view);
            }
        });

        return rootView;
    }

    public void onStartTestsButtonClick(View view) {
        times_left.set(measure_times);
        doTest();
    }

    public void doTest() {
        if (times_left.getAndDecrement() > 0) {
            MeasurementTask t = new MeasurementTask(MeasurementResultProcessor, MeasurementProgressUpdater);
            t.execute(getActivity());
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
    }

    public void onRBTimesClicked(View view) {
        if (((RadioButton) view).isChecked()) {
            switch (view.getId()) {
                default:
                case R.id.rb_times_1:
                    measure_times = 1;
                    break;
                case R.id.rb_times_5:
                    measure_times = 5;
                    break;
                case R.id.rb_times_10:
                    measure_times = 10;
                    break;
                case R.id.rb_times_50:
                    measure_times = 50;
                    break;
            }
        }
    }
}
