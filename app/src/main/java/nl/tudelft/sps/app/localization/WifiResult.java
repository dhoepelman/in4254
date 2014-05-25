package nl.tudelft.sps.app.localization;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * A simple data structure that represents one Wi-Fi scan of a particular
 * access point. Instances of this class are stored to a SQL database by
 * ORMLite or created by reading from it.
 */
@DatabaseTable
public class WifiResult {

    public static final String COLUMN_SCAN = "scan";

    @DatabaseField(generatedId = true)
    long id;
    @DatabaseField
    Room room;
    @DatabaseField
    String BSSID;
    @DatabaseField
    String SSID;
    @DatabaseField
    int level;
    @DatabaseField
    long timestamp;
    @DatabaseField(foreign = true, canBeNull = true, columnName = COLUMN_SCAN)
    private WifiResultCollection scan;

    public WifiResult(WifiResultCollection scan, Room room, String BSSID, String SSID, int level, long timestamp) {
        this.room = room;
        this.BSSID = BSSID;
        this.SSID = SSID;
        this.level = level;
        this.timestamp = timestamp;
        this.scan = scan;
    }

    public WifiResult() {
        // This constructor solely exists to make ORMLite happy
    }
}
