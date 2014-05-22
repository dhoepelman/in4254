package nl.tudelft.sps.app.localization;

import com.j256.ormlite.dao.CloseableIterator;

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
     * @param currentScan Iterable of either ScanResult or WifiResult
     * @return the current location probability distribution
     */
    public Map<Room, Double> adjustLocation(Iterable<? extends Object> currentScan);

    /**
     * Train the locator with collected scan results containing the room, bssid and level.
     * Must contain all the data points, previously trained ones will not be remembered
     */
    public void train(CloseableIterator<WifiResult> trainingData);

    /**
     * Add a detected movement to the location
     */
    public void addMovement(ACTIVITY currentActivity);

    /**
     * Reset the locator to the initial location
     */
    public void initialLocation();
}
