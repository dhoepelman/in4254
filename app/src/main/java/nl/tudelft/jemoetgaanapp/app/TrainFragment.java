package nl.tudelft.jemoetgaanapp.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

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

    private View rootView;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_train, container, false);
        //final TextView textView = (TextView) rootView.findViewById(R.id.section_label);
        //textView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));

        // Register the button listeners
        for(int b : ACTIVITY.activity_buttons) {
            ((ImageButton)rootView.findViewById(b)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { selectActivityButtonClick(v);  }
            });
        }
        rootView.findViewById(R.id.but_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { startTrainingButtonClick(v); }
        });
        rootView.findViewById(R.id.but_stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { stopTrainingButtonClick(v); }
        });
        // Make all activity buttons gray
        colorSelectedButton();

        // Initialize debug text output
        ((TextView)rootView.findViewById(R.id.val_measure_output)).setMovementMethod(new ScrollingMovementMethod());

        // Set up accelerometer
        smanager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        accelerometer = smanager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

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
        public String toString() {
            return String.format("%s,%d,%.4f,%.4f,%.4f", activity.name(), timestamp, values[0], values[1], values[2]);
        }
    }

    public void onSensorChanged(SensorEvent event) {
        // We received new data from one of the sensors

        //Log.d(LOGTAG, "Type: " + event.sensor.getType());

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
            case Sensor.TYPE_LINEAR_ACCELERATION:
                buffer.add(new Measurement(selectedactivity, event.timestamp, event.values));
                ((TextView)rootView.findViewById(R.id.val_measuring)).setText(String.format("%.1f", (System.currentTimeMillis() - measurement_start)/100.0));
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

    private long measurement_start;
    private void startMeasuring() {
        smanager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        measurement_start = System.currentTimeMillis();
    }


    private void stopMeasuring() {
        try {
            smanager.unregisterListener(this);
        } catch(NullPointerException e) {
            // Listener was already unregistered or never registered
        }
        writeBuffer();
    }

    private void writeBuffer() {
        final TextView output = (TextView)rootView.findViewById(R.id.val_measure_output);
        if(output == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for(Measurement m : buffer) {
            sb.append(m);
            sb.append("\n");
        }
        output.setText(sb.toString());
        buffer.clear();
    }

    private ACTIVITY selectedactivity;
    public void selectActivityButtonClick(final View v) {
        selectedactivity = ACTIVITY.buttonToActivity(v.getId());
        colorSelectedButton();
    }

    public void startTrainingButtonClick(final View v) {
        if(selectedactivity == null) {
            Toast.makeText(getActivity().getApplicationContext(), "Select activity first", Toast.LENGTH_SHORT).show();
        } else {
            v.setEnabled(false);
            ((Button)getView().findViewById(R.id.but_stop)).setEnabled(true);
            startMeasuring();
        }
    }

    public void stopTrainingButtonClick(final View v) {
        v.setEnabled(false);
        ((Button)getView().findViewById(R.id.but_start)).setEnabled(true);
        stopMeasuring();
    }

    /**
     * (Visually) select the right button
     */
    private void colorSelectedButton() {
        for(int b : ACTIVITY.activity_buttons) {
            rootView.findViewById(b).setBackgroundColor(Color.GRAY);
        }
        if(selectedactivity != null) {
            rootView.findViewById(selectedactivity.button_id).setBackgroundColor(Color.BLACK);
        }
    }

    private enum ACTIVITY {
        SITTING(R.id.but_sitting),
        WALKING(R.id.but_walking),
        RUNNING(R.id.but_running),
        JUMPING(R.id.but_jumping);

        public final static int[] activity_buttons = new int[] { R.id.but_sitting, R.id.but_walking, R.id.but_running, R.id.but_jumping };

        public final int button_id;

        private ACTIVITY(final int butid) {
            this.button_id = butid;
        }

        public static ACTIVITY buttonToActivity(int butid) {
            ACTIVITY selected = null;
            switch(butid) {
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
            return selected;
        }
    }
}
