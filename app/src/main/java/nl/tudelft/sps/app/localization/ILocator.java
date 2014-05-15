package nl.tudelft.sps.app.localization;

import android.net.wifi.ScanResult;

import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;

import nl.tudelft.sps.app.activity.ACTIVITY;

public interface ILocator {

    /**
     * Get the current location probability distribution as a view.
     */
    public Map<Room, Double> getLocation();

    /**
     * Gets a immutable copy of the current probability distribution for location, sorted from most likely to least likely
     */
    public SortedMap<Room, Double> getSortedLocation();

    /**
     * Adjust the location according to a scan result.
     * @return the current location probability distribution
     */
    public Map<Room, Double> adjustLocation(Collection<ScanResult> currentScan);

    /**
     * Train the locator with collected scan results containing the room, bssid and level.
     * Must contain all the data points, previously trained ones will not be remembered
     */
    public void train(Collection<WifiResult> trainingData);

    /**
     * Add a detected movement to the location
     */
    public void addMovement(ACTIVITY currentActivity);

    /**
     * Reset the locator to the initial location
     */
    public void initialLocation();
}
