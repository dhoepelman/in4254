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

public class WifiScanTask extends AsyncTask<Void, Integer, WifiMeasurementsWindow> {

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

    public WifiScanTask(ResultProcessor processor, ProgressUpdater updater, Activity activity, ToastManager toastManager) {
        resultProcessor = processor;
        progressUpdater = updater;
        this.activity = activity;

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
    protected WifiMeasurementsWindow doInBackground(Void... nothing) {
        final ScanReceiver receiver = new ScanReceiver();
        activity.registerReceiver(receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        final WifiMeasurementsWindow window = new WifiMeasurementsWindow();

        try {
            for (int i = 0; i < WifiMeasurementsWindow.WINDOW_SIZE; i++) {
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
