package nl.tudelft.sps.app.localization;

import android.net.wifi.ScanResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A window containing WINDOW_SIZE WifiMeasurements. Each WifiMeasurement
 * represents a scan for surrounding Wi-Fi access points.
 *
 * If you want to simply get a list of all the ScanResults, then you need
 * to call getMeasurements() and iterate over it to get a list of
 * WifiMeasurements. For each of these WifiMeasurements you need to call
 * WifiMeasurement.getResults() to get a list of ScanResults that was
 * collected during that particular measurement. For example:
 *
 * for (WifiMeasurement measurement : window.getMeasurements()) {
 *     for (ScanResult accessPoint : measurement.getResults()) {
 *         // Do something with accessPoint here
 *     }
 * }
 *
 * You can also get a list of measured levels (in dBm) per BSSID. For example:
 *
 * for (Entry<String, AccessPointLevels> entry : window.getAccessPointLevels().entrySet()) {
 *     // Do something with entry.getKey() (which is the BSSID) here
 *     for (Integer level : entry.getValue().getLevels()) {
 *         // Do something with a measured level of the current BSSID here
 *     }
 * }
 */
public class WifiMeasurementsWindow {

    /**
     * Number of measurements in a window. Each measurement is a scan
     * and takes on average about 640 ms on a Galaxy S,
     */
    public static final int WINDOW_SIZE = 60;

    private final List<WifiMeasurement> measurements = new ArrayList<WifiMeasurement>();

    private final Room measuredInRoom;

    private final int windowSize;

    public WifiMeasurementsWindow(int size, Room room) {
        measuredInRoom = room;
        windowSize = size;
    }

    public Room getMeasuredInRoom() {
        return measuredInRoom;
    }

    public boolean isCompleted() {
        return getProgress() >= windowSize;
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
     * Return a list of WifiMeasurements. Each WifiMeasurements contains a
     * timestamp and a list of ScanResults.
     */
    public List<WifiMeasurement> getMeasurements() {
        return Collections.unmodifiableList(measurements);
    }

    /**
     * Return a map with BSSID's mapped to an AccessPointLevels. An AccessPointLevels
     * contains the BSSID, SSID, and a list of measured levels.
     */
    public Map<String, AccessPointLevels> getAccessPointLevels() {
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