package nl.tudelft.sps.app.localization;

import android.net.wifi.ScanResult;

import java.util.ArrayList;
import java.util.List;

public class WifiMeasurement {

    private final List<ScanResult> measurement = new ArrayList<ScanResult>();

    private final long timestamp;

    public WifiMeasurement(List<ScanResult> accessPoints) {
        timestamp = System.currentTimeMillis();
        measurement.addAll(accessPoints);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public List<ScanResult> getResults() {
        return measurement;
    }
}
