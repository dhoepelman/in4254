package nl.tudelft.sps.app.localization;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import nl.tudelft.sps.app.ToastManager;

public class WifiScanTask extends AsyncTask<Room, Integer, WifiMeasurementsWindow> {

    private final ResultProcessor resultProcessor;
    private final ProgressUpdater progressUpdater;
    private final WifiManager wifiManager;
    private final Activity activity;

    /**
     * Last list of measured AP's. It is considered to be one measurement
     * and added to the WifiMeasurementsWindow, which is then returned
     * via the ResultProcessor when the task completes.
     */
    private List<ScanResult> accessPoints;

    /**
     * Object on which doInBackground can wait for the scan to complete
     */
    private final Object gate = new Object();

    private int numberOfMeasurements;

    /**
     * Creates a task that asynchronously performs one or more Wi-Fi
     * measurements and returns the result via the given ResultProcessor.
     * ProgressUpdater may be null if we are not interested in being
     * kept up to date about the progress of the measuring.
     *
     * @param window True if the window needs to be filled completely
     *               with multiple measurements, or false if just one
     *               measurement is needed
     */
    public WifiScanTask(ResultProcessor processor, ProgressUpdater updater, Activity activity, ToastManager toastManager, boolean window) {
        if (processor == null) {
            throw new RuntimeException("ResultProcessor not allowed to be null");
        }
        resultProcessor = processor;
        progressUpdater = updater;
        this.activity = activity;

        if (window) {
            numberOfMeasurements = WifiMeasurementsWindow.WINDOW_SIZE;
        }
        else {
            numberOfMeasurements = 1;
        }

        wifiManager = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);

        // Try to enable Wi-Fi if it is currently turned off
        if (!wifiManager.isWifiEnabled()) {
            if (!wifiManager.setWifiEnabled(true)) {
                toastManager.showText("Enable Wi-Fi before starting training", Toast.LENGTH_LONG);
            }
        }
    }

    @Override
    protected void onPostExecute(WifiMeasurementsWindow result) {
        resultProcessor.result(result);
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        if (progressUpdater != null) {
            progressUpdater.update(values[0]);
        }
    }

    @Override
    protected WifiMeasurementsWindow doInBackground(Room... rooms) {
        final ScanReceiver receiver = new ScanReceiver();
        activity.registerReceiver(receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        // rooms[0] might be null, but that is not this tasks' concern
        final WifiMeasurementsWindow window = new WifiMeasurementsWindow(numberOfMeasurements, rooms[0]);

        try {
            for (int i = 0; i < numberOfMeasurements; i++) {
                final long currentTimestamp = System.currentTimeMillis();

                if (!wifiManager.startScan()) {
                    Log.w(getClass().getName(), "Scan not started");
                    return null;
                }

                // The scan should be done in well under 5 seconds.
                synchronized (gate) {
                    gate.wait(5 * 1000L);
                }

                if (accessPoints != null) {
                    window.addMeasurement(accessPoints);

                    publishProgress(window.getProgress());
                    Log.w(getClass().getName(), "WIFI SCAN FINISHED");
                }
                else {
                    Log.w(getClass().getName(), "Scan went horribly wrong");
                    return null;
                }

                final long duration = System.currentTimeMillis() - currentTimestamp;
                Log.w(getClass().getName(), "WIFI SCAN RUN " + String.valueOf(window.getProgress()) + " in " + String.valueOf(duration) + " ms");
            }

            Log.w(getClass().getName(), "WIFI WINDOW FINISHED");

            if (!window.isCompleted()) {
                throw new AssertionError("Window should have been completed");
            }

            return window;
        }
        catch (InterruptedException exception) {
            Log.w(getClass().getName(), "Window scan was interrupted");
            return null;
        }
        finally {
            activity.unregisterReceiver(receiver);
        }
    }

    public static interface ResultProcessor {
        public void result(WifiMeasurementsWindow result);
    }

    public static interface ProgressUpdater {
        public void update(Integer progress);
    }

    public class ScanReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            accessPoints = wifiManager.getScanResults();

            Log.w(getClass().getName(), "WIFI RESULTS: " + String.valueOf(accessPoints.size()));

            // We're done
            synchronized (gate) {
                gate.notify();
            }
        }
    }
}
