package nl.tudelft.sps.app.localization;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import nl.tudelft.sps.app.activity.ACTIVITY;

public interface ILocator {

    /**
     * Get the current location probability distribution as a view.
     */
    public Map<Room, Double> getLocation();

    /**
     * get the rooms sorted from most likely to least likely
     */
    public List<Room> getSortedLocation();


    /**
     * Get the most likely Room
     */
    public Room getMostLikelyRoom();

    /**
     * Get the probability for a given room
     */
    public Double getProbability(Room room);

    /**
     * Adjust the location according to a scan result.
     *
     * @param currentScan Iterable of either ScanResult or WifiResult
     * @return the number of iterations (wifi access points) that was necessary to perform the localization
     */
    public int adjustLocation(Iterable<?> currentScan);

    /**
     * Train the locator with collected scan results containing the room, bssid and level.
     * Must contain all the data points, previously trained ones will not be remembered
     */
    public void train(Iterator<WifiResult> trainingData);

    /**
     * Train the number of AP's found in a scan. Optional
     */
    public void trainNumberAPs(Iterator<WifiResultCollection> trainingsData);

    /**
     * Add a detected movement to the location
     */
    public void addMovement(ACTIVITY currentActivity);

    /**
     * Reset the locator to the initial location
     */
    public void initialLocation();
}
