package nl.tudelft.sps.app.localization;

import com.j256.ormlite.field.DatabaseField;

/**
 * A simple data structure that represents one Wi-Fi scan of a particular
 * access point. Instances of this class are stored to a SQL database by
 * ORMLite or created by reading from it.
 */
public class WifiResult {

    @DatabaseField(generatedId = true)
    long id;

    @DatabaseField(unknownEnumName = "Unknown")
    Room room;

    @DatabaseField
    String BSSID;

    @DatabaseField
    String SSID;

    @DatabaseField
    int level;

    @DatabaseField
    long timestamp;

    public WifiResult(Room room, String BSSID, String SSID, int level, long timestamp) {
        this.room = room;
        this.BSSID = BSSID;
        this.SSID = SSID;
        this.level = level;
        this.timestamp = timestamp;
    }

    public WifiResult() {
        // This constructor solely exists to make ORMLite happy
    }
}
