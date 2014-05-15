package nl.tudelft.sps.app.localization;

import android.net.wifi.ScanResult;

import java.util.Collection;
import java.util.Map;

public interface ILocator {

    /**
     * Get the current location probability distribution
     */
    public Map<Room, Double> getLocation();

    /**
     * Adjust the location according to a scan result.
     *
     * @return the current location probability distribution
     */
    public Map<Room, Double> adjustLocation(Collection<ScanResult> currentScan);

    /**
     * Train the locator with collected scan results containing the room, bssid and level.
     */
    public void train(Collection<WifiResult> trainingData);
}
