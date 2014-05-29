package nl.tudelft.sps.app.localization;


import android.net.wifi.ScanResult;
import android.util.Log;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;
import com.j256.ormlite.dao.CloseableIterator;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import nl.tudelft.sps.app.activity.ACTIVITY;

/**
 * Implements a Locator according using bayesian interference and a normally distributed RSS probability based on the training data
 */
public class BayesianLocator implements ILocator {

    public static final Collection<String> ignoredSSIDs = ImmutableSet.of("TUvisitor", "tudelft-dastud", "Conferentie-TUD");
    /**
     * The minimum stddev of a probability distribution, so the distribution doesn't become too narrow
     */
    public static final double MINIMUM_DEVIATION = 1.0;
    /**
     * Threshold for certainty when processing further scans is deemed useless.
     * Note: the strongest AP signal will always be processed
     */
    private static final double ACCURACY_THRESHOLD = 0.95;
    /**
     * The "zero" probability. We never want to remove a room completely so this is the smallest we'll multiply with
     */
    private static final double PROBABILITY_EPSILON = 0.002;
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
    /**
     * Map with the distribution of number of APs visible in every Room.
     */
    private Map<Room, NormalDistribution> numberAPdistribution;

    public BayesianLocator() {
        initialLocation();
    }

    /**
     * Normalize the probabilities of the current location so that they add up to 1
     */
    private synchronized void normalize() {
        Double sum = 0.0;
        // Add from smallest to largest to prevent floating-point errors
        List<Double> values = new ArrayList<>(currentLocation.values());
        Collections.sort(values);
        for (Double p : values) {
            sum += p;
        }
        // Normalize them to (almost) 1, while preventing entries from going lower than PROBABILITY_EPSILON
        double secondSum = 0.0;
        for (Map.Entry<Room, Double> entry : currentLocation.entrySet()) {
            final double value = entry.getValue() / sum;
            if (value >= PROBABILITY_EPSILON) {
                entry.setValue(value);
            } else {
                entry.setValue(PROBABILITY_EPSILON);
                // We added (PROBABILITY_EPSILON-value) to the secondsum
                secondSum += PROBABILITY_EPSILON - value;
            }
        }
        // Normalize to exactly 1 (excluding floating point rounding errors)
        if (secondSum > 0.0) {
            secondSum += 1.0;
            for (Map.Entry<Room, Double> entry : currentLocation.entrySet()) {
                entry.setValue(entry.getValue() / secondSum);
            }
        }
        // Really normalize to 1
    }

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

    public synchronized int adjustLocation(Iterable<?> currentScan) {
        // Sort the scanresults from strongest level to weakest
        final List<Scan> signals = new ArrayList<>();
        for (Object entry : currentScan) {
            signals.add(new Scan(entry));
        }
        Collections.sort(signals, new Comparator<Scan>() {
            @Override
            public int compare(Scan o1, Scan o2) {
                // Integer.compare doesn't exists in Java6 / Android API < 17 yet :(
                return (o1.level() < o2.level()) ? -1 : (o1.level() == o2.level() ? 0 : 1);
            }
        });
        int iterations = 0;
        // Go through all the scans and calculate the new probability
        for (Scan scan : signals) {
            iterations++;
            if (ignoreSSID(scan.SSID())) {
                continue;
            }
            // Check if we know this AP. If we do not, ignore it
            if (!trainingsData.containsRow(scan.BSSID())) {
                Log.d(BayesianLocator.class.getName(), String.format("Skipping BSSID %s", scan.BSSID()));
                continue;
            }
            // Adjust the probability of each room
            for (Room room : Room.values()) {
                NormalDistribution distribution = trainingsData.get(scan.BSSID(), room);
                double p;
                if (distribution == null) {
                    p = 0;
                } else {
                    // Calculate the probability that for this level, the scan was done in the current room
                    p = Math.max(0, distribution.probability(scan.level() - 0.5, scan.level() + 0.5));
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
        // If we're not certain, take the number of AP's into account
        if (numberAPdistribution != null && !isLocationCertain()) {
            for (Room room : Room.values()) {
                NormalDistribution distribution = numberAPdistribution.get(room);
                currentLocation.put(room, currentLocation.get(room) * distribution.probability(iterations - 0.5, iterations + 0.5));
            }
            normalize();
            iterations++;
        }
        return iterations;
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

    public void trainNumberAPs(Iterator<WifiResultCollection> trainingData) {
        Map<Room, SummaryStatistics> numberAP = new HashMap<>();

        // Count the number of Access Points in every scan
        while (trainingData.hasNext()) {
            WifiResultCollection scan = trainingData.next();

            SummaryStatistics stats = numberAP.get(scan.getRoom());
            if (stats == null) {
                stats = new SummaryStatistics();
                numberAP.put(scan.getRoom(), stats);
            }
            stats.addValue(scan.getNumAP());
        }
        if (trainingData instanceof CloseableIterator) {
            ((CloseableIterator) trainingData).closeQuietly();
        }
        numberAPdistribution = new HashMap<>();
        // Transform the counts into a normal distribution
        for (Room room : Room.values()) {
            SummaryStatistics stats = numberAP.get(room);
            if (stats == null) {
                stats = new SummaryStatistics();
            }
            numberAPdistribution.put(room, new NormalDistribution(stats.getMean(), Math.max(1, stats.getStandardDeviation())));
        }
    }

    @Override
    public void train(Iterator<WifiResult> trainingData) {
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
        if (trainingData instanceof CloseableIterator) {
            ((CloseableIterator) trainingData).closeQuietly();
        }


        // Calculate the distributions from the lists of measurements
        trainingsData = HashBasedTable.create(values.rowKeySet().size(), Room.values().length);
        for (Table.Cell<String, Room, SummaryStatistics> cell : values.cellSet()) {
            final double mean = cell.getValue().getMean();
            final double standardDeviation = Math.max(MINIMUM_DEVIATION, cell.getValue().getStandardDeviation());
            trainingsData.put(cell.getRowKey(), cell.getColumnKey(), new NormalDistribution(mean, standardDeviation));
        }
    }

    @Override
    public synchronized void addMovement(int steps) {
        // TODO: Improve movement model, I just made something up
        if (steps > 0) {
            final Map<Room, Double> previousLocation = new HashMap<>(currentLocation);

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

    private class Scan {
        private final ScanResult a;
        private final WifiResult b;

        public Scan(Object unknown) {
            this.a = (unknown instanceof ScanResult) ? (ScanResult) unknown : null;
            this.b = (unknown instanceof WifiResult) ? (WifiResult) unknown : null;
            if (a == null && b == null) {
                throw new IllegalArgumentException("Illegal type");
            }
        }

        public String SSID() {
            return (a != null ? a.SSID : b.SSID);
        }

        public String BSSID() {
            return (a != null ? a.BSSID : b.BSSID);
        }

        public int level() {
            return (a != null ? a.level : b.level);
        }
    }
}
