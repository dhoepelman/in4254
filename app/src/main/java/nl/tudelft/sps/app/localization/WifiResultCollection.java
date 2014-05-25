package nl.tudelft.sps.app.localization;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Arrays;

/**
 * Exists solely to group wifiresults
 */
@DatabaseTable
public class WifiResultCollection {

    @DatabaseField(generatedId = true)
    long id;

    @ForeignCollectionField
    ForeignCollection<WifiResult> wifiResults;
    @DatabaseField
    Room room;
    @DatabaseField
    long timestamp;

    public WifiResultCollection(long timestamp, Room room) {
        this.timestamp = timestamp;
        this.room = room;
    }

    public WifiResultCollection() {
        // ORMlite
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Room getRoom() {
        return room;
    }

    public ForeignCollection<WifiResult> getWifiResults() {
        return wifiResults;
    }

    public long getId() {
        return id;
    }

    @Override
    public String toString() {
        return "WifiResultCollection{" +
                "id=" + id +
                ", wifiResults=" + Arrays.toString(wifiResults.toArray()) +

                ", room=" + room +
                ", timestamp=" + timestamp +
                '}';
    }
}
