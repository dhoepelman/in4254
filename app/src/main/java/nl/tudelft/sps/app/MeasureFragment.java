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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by David on 1-5-14.
 */
public class MeasureFragment extends Fragment implements SensorEventListener {

    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

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

    public MeasureFragment() {
    }

    public void onCreate() {
        // Set up accelerometer
        smanager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        accelerometer = smanager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_measure, container, false);
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

    public void onSensorChanged(SensorEvent event) {
        // We received new data from one of the sensors

        //Log.d(LOGTAG, "Type: " + event.sensor.getType());

        if (storedValues.isEmpty()) {
            for (int i = 0; i < 3; i++) {
                final List<Float> list = new ArrayList<Float>(bufferLength);
                for(int j = 0; i < 5; i++) {
                    list.add(0f);
                }
                storedValues.add(list);
            }
        }

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
            case Sensor.TYPE_LINEAR_ACCELERATION:
//                for (int i = 0; i < 3; i++) {
//                    currentValues[i] = filter_modifier * event.values[i] + (1 - filter_modifier) * currentValues[i];
//                }
                final int currentIndex = currentPos++ % 5;
                for (int i = 0; i < 3; i++) {
                    storedValues.get(i).set(currentIndex, event.values[i]);
                    Collections.sort(storedValues.get(i));
                }

                final DecimalFormat f = new DecimalFormat("#.##");

                //((TextView) findViewById(R.id.ValX)).setText(f.format(currentValues[0]));
                //((TextView)findViewById(R.id.ValY)).setText(f.format(currentValues[1]));
                //((TextView)findViewById(R.id.ValZ)).setText(f.format(currentValues[2]));

                for (int i = 0; i < 3; i++) {
                    junit.framework.Assert.assertEquals(bufferLength, storedValues.get(i).size());
                }

                final TextView valueX = (TextView) getView().findViewById(R.id.ValX);
                final TextView valueY = (TextView) getView().findViewById(R.id.ValY);
                final TextView valueZ = (TextView) getView().findViewById(R.id.ValZ);

                if (valueX == null || valueY == null || valueZ == null) {
                    // First keelhaul all the androids, then abandon ship!
                    return;
                }

                // Get the median values
                valueX.setText(f.format(storedValues.get(0).get(bufferLength / 2)));
                valueY.setText(f.format(storedValues.get(1).get(bufferLength / 2)));
                valueZ.setText(f.format(storedValues.get(2).get(bufferLength / 2)));

                break;
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Ignore
    }

    @Override
    public void onResume() {
        super.onResume();
        smanager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onPause() {
        super.onPause();
        smanager.unregisterListener(this);
    }

}
