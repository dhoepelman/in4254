package nl.tudelft.jemoetgaanapp.app;

import android.app.Activity;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by David on 1-5-14.
 */
public class TrainFragment extends Fragment implements SensorEventListener {

    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final int[] activity_buttons = new int[] { R.id.but_sitting, R.id.but_walking, R.id.but_running, R.id.but_jumping };

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static TrainFragment newInstance(int sectionNumber) {
        final Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);

        final TrainFragment fragment = new TrainFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public TrainFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_train, container, false);
        //final TextView textView = (TextView) rootView.findViewById(R.id.section_label);
        //textView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));

        // Register the button listeners
        for(int b : activity_buttons) {
            ((ImageButton)rootView.findViewById(b)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { selectActivity(v);  }
            });
        }
        // Make all buttons gray
        unSelectButtons();

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
    }

    private SensorManager smanager;
    private Sensor accelerometer;

    //    private double[] currentValues = new double[] {0.0, 0.0, 0.0};
    private final double filter_modifier = 0.4;

    private final int bufferLength = 5;
    // Create empty buffers
    private final List<List<Float>> storedValues = new ArrayList<List<Float>>(3);

    private int currentPos = 0;

    ArrayList<Measurement> buffer = new ArrayList<>();

    private class Measurement {
        public final long timestamp;
        public final ACTIVITY activity;
        public final float[] values;
        public Measurement(ACTIVITY activity,long timestamp, float[] values) {
            this.activity = activity;
            this.timestamp = timestamp;
            this.values = values;
        }
    }
    private enum ACTIVITY {
        SITTING,
        WALKING,
        RUNNING,
        JUMPING,
    }

    public void onSensorChanged(SensorEvent event) {
        // We received new data from one of the sensors

        //Log.d(LOGTAG, "Type: " + event.sensor.getType());

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
            case Sensor.TYPE_LINEAR_ACCELERATION:
                buffer.add(new Measurement(selectedactivity, event.timestamp, event.values));
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Ignore
    }

    @Override
    public void onPause() {
        super.onPause();
        stopMeasuring();
    }

    private void startMeasuring() {
        smanager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }


    private void stopMeasuring() {
        try {
            smanager.unregisterListener(this);
        } catch(NullPointerException e) {
            // Listener was already unregistered
        }
    }

    private ACTIVITY selectedactivity;
    public void selectActivity(final View v) {
        final int selected_button = v.getId();

        ACTIVITY selected = null;
        switch(selected_button) {
            case R.id.but_sitting:
                selected = ACTIVITY.SITTING;
                break;
            case R.id.but_walking:
                selected = ACTIVITY.WALKING;
                break;
            case R.id.but_running:
                selected = ACTIVITY.RUNNING;
                break;
            case R.id.but_jumping:
                selected = ACTIVITY.JUMPING;
                break;
        }
        selectedactivity = selected;
        // (Visually) select the right button
        unSelectButtons();
        v.setBackgroundColor(Color.BLACK);
    }

    private void unSelectButtons() {
        for(int b : activity_buttons) {
            getView().findViewById(b).setBackgroundColor(Color.GRAY);
        }
    }
}
