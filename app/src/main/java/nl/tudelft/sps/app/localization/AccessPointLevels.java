package nl.tudelft.sps.app.localization;

import com.google.common.collect.ImmutableList;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

/**
 * Contains the BSSID, SSID, and a list of measured levels. An instance of
 * this class can be used to get the measured levels of its BSSID, or to
 * compute the mean and standard deviation of of the levels.
 */
public class AccessPointLevels {

    public final String SSID;
    public final String BSSID;

    private final List<Integer> levels = new ArrayList<Integer>();

    public AccessPointLevels(String SSID, String BSSID) {
        this.SSID = SSID;
        this.BSSID = BSSID;
    }

    public void addLevel(int level) {
        levels.add(level);
    }

    public List<Integer> getLevels() {
        return Collections.unmodifiableList(levels);
    }

    /**
     * Compute and return the mean and standard deviation of the measured levels
     *
     * @return a 2-tuple with the mean and standard deviation
     */
    public double[] computeMeanAndStdDev() {
        // Build statistics for mean
        final DescriptiveStatistics statistics = new DescriptiveStatistics(levels.size());
        for (Integer level : levels) {
            statistics.addValue(level);
        }

        return new double[] {
            statistics.getMean(),
            statistics.getStandardDeviation()
        };
    }

}
