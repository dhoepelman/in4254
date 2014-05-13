package nl.tudelft.sps.app.localization;

import java.util.List;

public class AlphaFilter {

    public static final double ALPHA_FACTOR = 0.2;

    public static List<WifiMeasurement> getFiltered(List<WifiMeasurement> measurements) {
        final double removalSize = 0.2 / 2 * (double) measurements.size();

        // TODO Sort measurements

        // TODO Remove removalSize on the head and removalSize on the tail of the list
        // TODO Compute and return average

        return measurements;
    }
}
