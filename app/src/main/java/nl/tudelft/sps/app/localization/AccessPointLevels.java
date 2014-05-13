package nl.tudelft.sps.app.localization;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

/**
 * Contains the BSSID, SSID, and a list of measured levels.
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
}
