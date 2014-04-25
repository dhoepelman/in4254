package nl.tudelft.smartphonesensing.labs;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends Activity implements SensorEventListener {

    private static final String LOGTAG = "SensorActivity";

    private SensorManager smanager;
    private Sensor accelerometer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up accelerometer
        smanager = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = smanager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void onResume() {
        super.onResume();
        smanager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
    }
    public void onPause() {
        super.onPause();
        smanager.unregisterListener(this);
    }

    private double[] currentValues = new double[] {0.0, 0.0, 0.0};
    private final double filter_modifier = 0.4;

    private final int bufferLength = 5;
    // Create empty buffers
    private ArrayList<ArrayList<Float>> storedValues = new ArrayList<ArrayList<Float>>(3);

    private int currentPos = 0;
    public void onSensorChanged(SensorEvent event) {
        // We received new data from one of the sensors

        //Log.d(LOGTAG, "Type: " + event.sensor.getType());

        if(storedValues.size() < 3) {
            for(int i=0;i<3;i++) {
                ArrayList<Float> list = new ArrayList<Float>(bufferLength);
                list.add(0f);
                list.add(0f);
                list.add(0f);
                list.add(0f);
                list.add(0f);

                storedValues.add(list);
            }
        }

        switch(event.sensor.getType()){
            case Sensor.TYPE_ACCELEROMETER:
            case Sensor.TYPE_LINEAR_ACCELERATION:
                for(int i=0;i<3;i++) {
                    currentValues[i] = filter_modifier * event.values[i] + (1-filter_modifier) * event.values[i];
                }
                for(int i=0;i<3;i++) {
                    storedValues.get(i).set((currentPos++ % 5), event.values[i]);
                    Collections.sort(storedValues.get(i));
                }

                DecimalFormat f = new DecimalFormat("#.##");

                //((TextView) findViewById(R.id.ValX)).setText(f.format(currentValues[0]));
                //((TextView)findViewById(R.id.ValY)).setText(f.format(currentValues[1]));
                //((TextView)findViewById(R.id.ValZ)).setText(f.format(currentValues[2]));
                // Get the median values
                ((TextView) findViewById(R.id.ValX)).setText(f.format(storedValues.get(0).get(bufferLength/2+1)));
                ((TextView)findViewById(R.id.ValY)).setText(f.format(storedValues.get(1).get(bufferLength/2+1)));
                ((TextView)findViewById(R.id.ValZ)).setText(f.format(storedValues.get(2).get(bufferLength/2+1)));

                break;
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Ignore
    }

}
