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

import nl.tudelft.sps.app.ToastManager;

public class WifiScanTask extends AsyncTask<Void, Void, WifiMeasurementsWindow> {

    private final ResultProcessor resultProcessor;
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

    public WifiScanTask(ResultProcessor processor, Activity activity, ToastManager toastManager) {
        resultProcessor = processor;
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
    protected WifiMeasurementsWindow doInBackground(Void... nothing) {
        final ScanReceiver receiver = new ScanReceiver();
        activity.registerReceiver(receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        final WifiMeasurementsWindow window = new WifiMeasurementsWindow();

        try {
            while (!window.isCompleted()) {
                window.setStartScan();

                if (!wifiManager.startScan()) {
                    Log.w(getClass().getName(), "Scan not started");
                    return null;
                }

                try {
                    // The scan should be done in well under 10 seconds.
                    synchronized (gate) {
                        gate.wait(10 * 1000);
                    }

                    Log.w(getClass().getName(), "WIFI RESULTS FINISHED");

                    window.addMeasurement(accessPoints);
                }
                catch (InterruptedException exception) {
                    Log.w(getClass().getName(), "Scan was interrupted");
                    return null;
                }

                window.delayUntilNextStart();
            }

            return window;
        }
        finally {
            activity.unregisterReceiver(receiver);
        }
    }

    public static interface ResultProcessor {
        public void result(WifiMeasurementsWindow result);
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
