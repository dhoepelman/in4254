package nl.tudelft.sps.app.localization;

import android.net.wifi.ScanResult;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class WifiMeasurementsWindow {

    /**
     * Window size. Based on [1].
     */
    public static final int WINDOW_SIZE = 16;

    /**
     * Window overlap. Based on [1]
     */
    public static final int WINDOW_OVERLAP = WINDOW_SIZE / 2;

    /**
     * Measuring frequency in Hz. One scan takes on average about 640 ms on Galaxy S,
     * so frequency should be at most 1.
     * WINDOW_OVERLAP / MEASUREMENTS_PER_SEC gives duration of one window
     */
    public static final int MEASUREMENTS_PER_SEC = 1;

    private final List<WifiMeasurement> measurements = new ArrayList<WifiMeasurement>();

    private long currentTimestamp;

    public boolean isCompleted() {
        return getProgress() >= WINDOW_SIZE;
    }

    public int getProgress() {
        return measurements.size();
    }

    /**
     * Process the list of RSS's to one measurement and add that to the
     * window.
     */
    public void addMeasurement(List<ScanResult> accessPoints) {
        measurements.add(new WifiMeasurement(accessPoints));
    }

    /**
     * TODO This is just a temporary method in order to show something on the screen.
     * TODO This method should be removed when List<ScanResult> has been converted to a measurement in WifiMeasurement
     */
    public WifiMeasurement getLast() {
        return measurements.get(measurements.size() - 1);
    }

    public void setStartScan() {
        currentTimestamp = System.currentTimeMillis();
    }

    public void delayUntilNextStart() {
        final long duration = System.currentTimeMillis() - currentTimestamp;
        Log.w(getClass().getName(), "WIFI ADDED " + String.valueOf(measurements.size()) + " in " + String.valueOf(duration) + " ms");

        // TODO Delay until time is currentTimestamp + 1000 / MEASUREMENTS_PER_SEC
    }

}
