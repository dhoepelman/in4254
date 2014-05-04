package nl.tudelft.sps.app;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;

/**
 * Fragment for testing the classifier
 */
public class TestFragment extends Fragment {

    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

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
        View rootView = inflater.inflate(R.layout.fragment_test, container, false);
        assert rootView != null;

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
    }

    private int measure_times = 1;
    public void onRBTimesClicked(View view) {
        if(((RadioButton)view).isChecked()) {
            switch(view.getId()) {
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
