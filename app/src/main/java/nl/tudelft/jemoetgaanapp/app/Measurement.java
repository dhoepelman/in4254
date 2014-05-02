package nl.tudelft.jemoetgaanapp.app;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * [1] Ravi, Nishkam, et al. "Activity recognition from accelerometer data." AAAI. Vol. 5. 2005.
 */
public class Measurement {
    /**
     * Window size. Based on [1]. 50Hz measurements
     */
    public static final int WINDOW_SIZE = 256;
    /**
     * Window overlap. Based on [1]
     */
    public static final int WINDOW_OVERLAP = 128;

    private final float[][] raw_measurements;

    public Measurement() {
        raw_measurements = new float[WINDOW_SIZE][];
    }

    /**
     * True if the window is full
     */
    public boolean isCompleted() {
        return raw_measurements[WINDOW_SIZE-1] != null;
    }

    /**
     * Mean of the window (separate for all 3 axes)
     */
    public float[] getMean() {
        // TODO: Implement mean
        return null;
    }

    /**
     * Standard deviation of the window (separate on all 3 axes)
     * @return
     */
    public float[] getStdDev() {
        // TODO: Implement standard deviation
    }

    /**
     * Helper to divide raw measurements into windows
     */
    public class Helper {
        private List<Measurement> measurements = new ArrayList<>();
        private Measurement current;
        private Measurement next;
        private int current_loc = 0;

        /**
         * Get all measurements
         * WARNING: if removing incomplete measurements, so do not call while still measuring
         */
        public Collection<Measurement> getMeasurements(boolean remove_incomplete) {
            if(remove_incomplete) {
                // Remove the last measurement if it's incomplete
                while(measurements.size() > 0 && !measurements.get(measurements.size()-1).isCompleted()) {
                    measurements.remove(measurements.size()-1);
                }
            }
            return measurements;
        }

        /**
         * Get all measurements
         * WARNING: do not call while still measuring
         */
        public Collection<Measurement> getMeasurements(){
            return getMeasurements(true);
        }

        public void addMeasurement(float[] values) {
            if(values.length != 3) {
                throw new IllegalArgumentException("Expected 3 values");
            }

            if(current == null || current_loc == WINDOW_SIZE) {
                if(current == null) {
                    // We don't have any measurement, start one
                    current = new Measurement();
                    current_loc = 0;
                    measurements.add(current);
                } else {
                    // Measurement was full, replace with next
                    current = next;
                    current_loc = WINDOW_OVERLAP;
                }
                // Create an empty "next" measurement
                next = new Measurement();
                measurements.add(next);
            }

            current.raw_measurements[current_loc] = values;
            if(current_loc >= WINDOW_OVERLAP) {
                next.raw_measurements[current_loc - WINDOW_OVERLAP] = values;
            }
            current_loc++;
        }
    }
}
