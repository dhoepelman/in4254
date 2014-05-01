package nl.tudelft.jemoetgaanapp.app;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.common.base.Joiner;

/**
 * Service om data te vergaderen van accelerometer.
 */
public class AccelerometerDataGathererService extends Service implements SensorEventListener  {
    private final String LOGTAG = "SENSOR_COLLECTOR";
    private SensorManager smanager;
    private Sensor accelerometer;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Measure every x nanoseconds
     */
    private static final long measure_every = 1000000;
    /**
     * Write the buffer every x milliseconds
     */
    private static final long write_every = 60 * 1000;

    /**
     * File with results
     */
    public static final String results_file_path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/accelerometer.csv";

    @Override
    public void onCreate() {
        super.onCreate();

        // Set up accelerometer
        smanager = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = smanager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        smanager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        smanager.unregisterListener(this);
        writeBuffer();
    }


    /**
     * Buffer of sensor measurements which haven't been written away yet
     */
    private final SortedMap<Long, float[]> buffer = new TreeMap<>();
    private long lastWrite = System.currentTimeMillis();
    private long lastMeasurement = 0;

    public void onSensorChanged(SensorEvent event) {
        // Check if enough time has passed
        if(event.timestamp - lastMeasurement >= measure_every) {
            // We received new data from one of the sensors

            //Log.d(LOGTAG, "Type: " + event.sensor.getType());

            switch(event.sensor.getType()){
                case Sensor.TYPE_ACCELEROMETER:
                case Sensor.TYPE_LINEAR_ACCELERATION:
                    buffer.put(event.timestamp, event.values);
                    lastMeasurement = event.timestamp;
                    possiblyWriteBuffer();
            }
        }
    }

    public void possiblyWriteBuffer() {
        if(System.currentTimeMillis() - lastWrite >= write_every) {
            writeBuffer();
        }
    }

    public void writeBuffer() {
        try {
            if(buffer.size() > 0
               && Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)
               && !Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
                final File resultsFile = new File(results_file_path);
                if(!resultsFile.exists()) {
                    resultsFile.createNewFile();
                }
                PrintWriter w = new PrintWriter(new FileOutputStream(resultsFile, true));
                Iterator<Map.Entry<Long, float[]>> it = buffer.entrySet().iterator();
                while(it.hasNext()) {
                    Map.Entry<Long, float[]> e = it.next();
                    // Write this entry as a comma-separated line
                    w.write(e.getKey() + "," + Joiner.on(",").join(Arrays.asList(e.getValue())) + "\n");
                    // Remove this entry from the buffer
                    it.remove();
                }
                w.flush();
                w.close();
            }
        }
        catch(IOException e) {
            Log.e(LOGTAG, "Could not create or write results file");
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Ignore
    }
}
