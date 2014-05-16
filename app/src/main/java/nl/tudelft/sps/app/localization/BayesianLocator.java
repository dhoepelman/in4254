package nl.tudelft.sps.app.localization;


import android.net.wifi.ScanResult;

import com.google.common.base.Functions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Table;
import com.j256.ormlite.dao.CloseableIterator;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import nl.tudelft.sps.app.activity.ACTIVITY;

/**
 * Implements a Locator according using bayesian interference and a normally distributed RSS probability based on the training data
 */
public class BayesianLocator implements ILocator {

    public static final Collection<String> ignoredSSIDs = ImmutableSet.of("TUvisitor", "tudelft-dastud", "Conferentie-TUD");
    /**
     * Threshold for certainty when processing further scans is deemed useless.
     * Note: the strongest AP signal will always be processed
     */
    private static final double ACCURACY_THRESHOLD = 0.95;
    /**
     * The "zero" probability. We never want to remove a room completely so this is the smallest we'll multiply with
     */
    private static final double PROBABILITY_EPSILON = 0.001;
    /**
     * The current location as a map from Room to a probability that the user is in that room
     */
    private final Map<Room, Double> currentLocation = new HashMap<>();
    private final Map<Room, Double> currentLocationRO = Collections.unmodifiableMap(currentLocation);
    /**
     * Contains the trainingsdata as a table.
     * Each BSSID (row) and Room (column) combination has a probability distribution.
     * If the value is null then no probability distribution is available
     */
    private Table<String, Room, NormalDistribution> trainingsData;

    public BayesianLocator() {
        initialLocation();
    }

    /**
     * Normalize the probabilities of the current location so that they add up to 1
     */
    private void normalize() {
        Double sum = 0.0;
        // Adding from smallest to greatest would be best, but I don't think it's going to be a problem
        for (Double p : currentLocation.values()) {
            sum += p;
        }
        // Normalize them to 1
        for (Map.Entry<Room, Double> entry : currentLocation.entrySet()) {
            entry.setValue(entry.getValue() / sum);
        }
    }

    @Override
    public Map<Room, Double> getLocation() {
        return currentLocationRO;
    }

    @Override
    public SortedMap<Room, Double> getSortedLocation() {
        // Nicked from http://stackoverflow.com/a/3420912/572635
        return ImmutableSortedMap.copyOf(currentLocation, Ordering.natural().onResultOf(Functions.forMap(currentLocation)));
    }

    @Override
    public Map<Room, Double> adjustLocation(Collection<ScanResult> currentScan) {
        // Sort the scanresults from strongest level to weakest
        final List<ScanResult> signals = new ArrayList<>(currentScan);
        Collections.sort(signals, new Comparator<ScanResult>() {
            @Override
            public int compare(ScanResult o1, ScanResult o2) {
                // Integer.compare doesn't exists in Java6 / Android API < 17 yet :(
                return (o1.level < o2.level) ? -1 : (o1.level == o2.level ? 0 : 1);
            }
        });
        // Go through all the scans and calculate the new probability
        for (ScanResult scan : signals) {
            if (ignoreSSID(scan.SSID)) {
                continue;
            }
            // Check if we know this AP. If we do not, ignore it
            if (!trainingsData.containsColumn(scan.BSSID)) {
                continue;
            }
            // Adjust the probability of each room
            for (Room room : Room.values()) {
                NormalDistribution distribution = trainingsData.get(room, scan.BSSID);
                double p;
                if (distribution == null) {
                    // BSSID was never measured in this room, multiply its probability to something very small
                    p = PROBABILITY_EPSILON;
                } else {
                    // Calculate the probability that for this level, the scan was done in the current room
                    p = Math.max(PROBABILITY_EPSILON, distribution.probability(scan.level - 0.5, scan.level + 0.5));
                }
                // Adjust the location estimate for this room according to the distribution
                currentLocation.put(room, currentLocation.get(room) * p);
            }
            // Normalize the probabilities
            normalize();
            if (isLocationCertain()) {
                break;
            }
            // TODO: Add detection of "osscilization" (i.e. new AP's not adding any more value) so that we can stop if that occurs
        }
        return getLocation();
    }

    /**
     * Check whether we are certain about our location
     * The threshold for certainty is ACCURACY_THRESHOLD
     */
    private boolean isLocationCertain() {
        boolean answer = false;
        for (Double p : currentLocation.values()) {
            if (p >= ACCURACY_THRESHOLD) {
                answer = true;
                break;
            }
        }
        return answer;
    }

    @Override
    public void train(CloseableIterator<WifiResult> trainingData) {
        Table<String, Room, SummaryStatistics> values = HashBasedTable.create();
        // Go through all of the wifiResults and group them by BSSID and Room
        while (trainingData.hasNext()) {
            final WifiResult wifiResult = trainingData.next();
            if (ignoreSSID(wifiResult.SSID)) {
                continue;
            }
            // Normalize level between 0 and -100 dBm (although in practice values above -30 or below -90 dBm are rare)
            final int level = Math.max(-100, Math.min(wifiResult.level, 0));
            SummaryStatistics cell = values.get(wifiResult.BSSID, wifiResult.room);
            // Cell doesn't exists, create it
            if (cell == null) {
                cell = new SummaryStatistics();
                values.put(wifiResult.BSSID, wifiResult.room, cell);
            }
            cell.addValue(level);
        }
        trainingData.closeQuietly();

        // Calculate the distributions from the lists of measurements
        trainingsData = HashBasedTable.create(values.rowKeySet().size(), Room.values().length);
        for (Table.Cell<String, Room, SummaryStatistics> cell : values.cellSet()) {
            trainingsData.put(cell.getRowKey(), cell.getColumnKey(), new NormalDistribution(cell.getValue().getMean(), cell.getValue().getStandardDeviation()));
        }
    }

    @Override
    public void addMovement(ACTIVITY currentActivity) {
        Map<Room, Double> previousLocation = new HashMap<>(currentLocation);
        // TODO: Improve movement model, I just made something up
        switch (currentActivity) {
            case Walking:
            case Running:
            case Stairs_Down:
            case Stairs_Up:
                // TODO 1) We need to find out s
                //      2) We need to know size of cell in aisle (and assume position in center)
                //      3) Calculate cells that fall within the boundaries:
                // s = number of steps
                // Location distribution:
                // -1.2s --- -0.5s   x   0.5s --- 1.2s
                // ^^^^^^^^^^^^^^^       ^^^^^^^^^^^^^
                //     uniform              uniform

                // Give 10% of the current probability to each of the adjacent rooms
                for (Map.Entry<Room, Double> locationProbability : previousLocation.entrySet()) {
                    for (Room room : locationProbability.getKey().getAdjacentRooms()) {
                        currentLocation.put(room, currentLocation.get(room) + 0.1 * locationProbability.getValue());
                    }
                }
                normalize();
                break;
            case Sitting:
            default:
                // Do nothing
                break;
        }


    }

    /**
     * Filter "TUvisitor", "tudelft-dastud" and "Conferentie-TUD", because the eduroam AP's send out 3 networks with different SSID's
     * But because they are the same AP this will not add accuracy, so we ignore these
     * // TODO: More elegant way to do this, maybe somewhere else?
     */
    private boolean ignoreSSID(String SSID) {
        return ignoredSSIDs.contains(SSID);
    }

    @Override
    public void initialLocation() {
        for (Room room : Room.values()) {
            // Set all locations to have equal probability
            currentLocation.put(room, 1.0);
        }
        normalize();
    }

}
