package nl.tudelft.jemoetgaanapp.app;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.Assert;

public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks, SensorEventListener {

    private SensorManager smanager;
    private Sensor accelerometer;

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
            getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer,
            (DrawerLayout) findViewById(R.id.drawer_layout));

        // Set up accelerometer
        smanager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = smanager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // Update the main content by replacing fragments
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
            .commit();
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
        }
    }

    public void restoreActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        final int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            final Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);

            final PlaceholderFragment fragment = new PlaceholderFragment();
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            final TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            textView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));
            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }

    protected void onResume() {
        super.onResume();
        smanager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    public void onPause() {
        super.onPause();
        smanager.unregisterListener(this);
    }

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
                list.add(0f);
                list.add(0f);
                list.add(0f);
                list.add(0f);
                list.add(0f);
                storedValues.add(list);
            }
        }

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
            case Sensor.TYPE_LINEAR_ACCELERATION:
                // TODO multiply 1 - filter_modifier with event.values[i-1] for i > 0?
//                for (int i = 0; i < 3; i++) {
//                    currentValues[i] = filter_modifier * event.values[i] + (1 - filter_modifier) * event.values[i];
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

                // TODO Is this callback method the best place to get GUI elements?
                final TextView valueX = (TextView) findViewById(R.id.ValX);
                final TextView valueY = (TextView) findViewById(R.id.ValY);
                final TextView valueZ = (TextView) findViewById(R.id.ValZ);

                if (valueX == null || valueY == null || valueZ == null) {
                    // First keelhaul all the androids, then abandon ship!
                    return;
                }

                // Get the median values
                // TODO If Java rounds down with integer division than bufferLength/2+1 = 3, but
                // needs to be 2 (zero-based index)
                valueX.setText(f.format(storedValues.get(0).get(bufferLength / 2)));
                valueY.setText(f.format(storedValues.get(1).get(bufferLength / 2)));
                valueZ.setText(f.format(storedValues.get(2).get(bufferLength / 2)));

                break;
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Ignore
    }

}
