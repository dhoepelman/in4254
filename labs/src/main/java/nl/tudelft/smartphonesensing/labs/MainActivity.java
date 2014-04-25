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
        smanager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }
    public void onPause() {
        super.onPause();
        smanager.unregisterListener(this);
    }

    public void onSensorChanged(SensorEvent event) {
        // We received new data from one of the sensors

        switch(event.sensor.getType()){
            case Sensor.TYPE_ACCELEROMETER:
                Float x = event.values[0];
                Float y = event.values[1];
                Float z = event.values[2];

                ((TextView)findViewById(R.id.ValX)).setText(x.toString());
                ((TextView)findViewById(R.id.ValY)).setText(y.toString());
                ((TextView)findViewById(R.id.ValZ)).setText(z.toString());

                break;
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Ignore
    }

}
