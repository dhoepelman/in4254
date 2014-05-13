package nl.tudelft.sps.app.localization;

import android.net.wifi.ScanResult;

import java.util.List;

public class WifiMeasurement {

    private final List<ScanResult> measurement;

    public WifiMeasurement(List<ScanResult> accessPoints) {
        // TODO This is a silly stub
        measurement = accessPoints;
    }

    /**
     * TODO Temporary method in order to show something on the screen
     */
    public List<ScanResult> getResults() {
        return measurement;
    }
}
