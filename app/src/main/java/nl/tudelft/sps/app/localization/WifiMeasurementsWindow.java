package nl.tudelft.sps.app.localization;

import android.net.wifi.ScanResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WifiMeasurementsWindow {

    /**
     * Number of measurements in a window. Each measurement is a scan
     * and takes on average about 640 ms on a Galaxy S,
     */
    public static final int WINDOW_SIZE = 60;

    private final List<WifiMeasurement> measurements = new ArrayList<WifiMeasurement>();

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

    public Map<String, AccessPointLevels> getAllResults() {
        final Map<String, AccessPointLevels> accessPointLevels = new HashMap<String, AccessPointLevels>();

        for (WifiMeasurement measurement : measurements) {
            for (ScanResult result : measurement.getResults()) {
                if (!accessPointLevels.containsKey(result.BSSID)) {
                    // We only store the levels, we don't use measurement.getTimestamp() for now
                    accessPointLevels.put(result.BSSID, new AccessPointLevels(result.SSID, result.BSSID));
                }
                accessPointLevels.get(result.BSSID).addLevel(result.level);
            }
        }

        return accessPointLevels;
    }

}