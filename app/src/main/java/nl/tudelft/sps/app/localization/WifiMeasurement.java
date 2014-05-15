package nl.tudelft.sps.app.localization;

import android.net.wifi.ScanResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents one attempt to scan for surrounding Wi-Fi access points. It
 * contains a list with the ScanResults and a timestamp at which the
 * measurement occurred.
 */
public class WifiMeasurement {

    private final List<ScanResult> measurement = new ArrayList<ScanResult>();

    private final long timestamp;

    public WifiMeasurement(List<ScanResult> accessPoints) {
        timestamp = System.currentTimeMillis();
        measurement.addAll(accessPoints);
    }

    /**
     * Return the timestamp at which the measurement was taken. This method
     * is used because ScanResult.timestamp only exists in Android 4.2 and
     * higher.
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Return the list with ScanResults. Each detected access point is
     * represented by one ScanResult. In Android 4.1.1 or lower ScanResult
     * does not contain a timestamp field, so in order to get timestamp
     * you need to call getTimestamp().
     */
    public List<ScanResult> getResults() {
        return Collections.unmodifiableList(measurement);
    }
}
