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

    private final Object windowGate = new Object();

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
            final Timer timer = new Timer(true);
            final TimerTask timerTask = new TimerTask() {
                public void cancelAndPurge(boolean valid) {
                    // Thank you for your service, you have been terminated
                    timer.cancel();
                    timer.purge();

                    if (!valid) {
                        window.setInvalid();
                    }

                    // We're done
                    synchronized (windowGate) {
                        windowGate.notify();
                    }
                }
                @Override
                public void run() {
                    final long currentTimestamp = System.currentTimeMillis();

                    if (!wifiManager.startScan()) {
                        Log.w(getClass().getName(), "Scan not started");
                        cancelAndPurge(false);
                    }

                    try {
                        // The scan should be done in well under 1 seconds.
                        synchronized (gate) {
                            gate.wait(1 * 1000L);
                        }

                        Log.w(getClass().getName(), "WIFI SCAN FINISHED");

                        window.addMeasurement(accessPoints);
                        publishProgress(window.getProgress());
                    }
                    catch (InterruptedException exception) {
                        Log.w(getClass().getName(), "Scan was interrupted");
                        cancelAndPurge(false);
                    }

                    if (window.isCompleted()) {
                        cancelAndPurge(true);
                    }

                    final long duration = System.currentTimeMillis() - currentTimestamp;
                    Log.w(getClass().getName(), "WIFI SCAN RUN " + String.valueOf(window.getProgress()) + " in " + String.valueOf(duration) + " ms");
                }
            };
            timer.scheduleAtFixedRate(timerTask, 0, 1000L / WifiMeasurementsWindow.MEASUREMENTS_PER_SEC);

            try {
                // Give Java one extra second to notify us after the window
                // has been filled
                synchronized (windowGate) {
                    windowGate.wait(1 * 1000L * (WifiMeasurementsWindow.WINDOW_SIZE + 1));
                }

                Log.w(getClass().getName(), "WIFI WINDOW FINISHED");
            }
            catch (InterruptedException exception) {
                Log.w(getClass().getName(), "Window scan was interrupted");
                return null;
            }

            if (window.getValid()) {
                return window;
            }
            else {
                return null;
            }
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
