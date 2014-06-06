package nl.tudelft.sps.app.localization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Locator that listen
 */
public class NSALocator implements ILocator {
    /**
     * The current location as a map from Room to a probability that the user is in that room
     */
    private final Map<Room, Double> currentLocation = new HashMap<>();
    private final Map<Room, Double> currentLocationRO = Collections.unmodifiableMap(currentLocation);

    @Override
    public Map<Room, Double> getLocation() {
        return currentLocationRO;
    }

    @Override
    public List<Room> getSortedLocation() {
        List<Room> ret = new ArrayList<>(getLocation().keySet());
        Collections.sort(ret, new Comparator<Room>() {
            @Override
            public int compare(Room lhs, Room rhs) {
                return getLocation().get(rhs).compareTo(getLocation().get(lhs));
            }
        });
        return ret;
    }

    @Override
    public Room getMostLikelyRoom() {
        return getSortedLocation().get(0);
    }

    @Override
    public Double getProbability(Room room) {
        return getLocation().get(room);
    }

    @Override
    public int adjustLocation(Iterable<?> currentScan) {
        return 0;
    }

    @Override
    public void train(Iterator<WifiResult> trainingData) {
    }

    @Override
    public void trainNumberAPs(Iterator<WifiResultCollection> trainingsData) {
    }

    @Override
    public void addMovement(int steps) {
    }

    @Override
    public void initialLocation() {
        currentLocation.clear();
    }
}
