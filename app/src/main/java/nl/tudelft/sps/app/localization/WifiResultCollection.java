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
    private long id;

    @ForeignCollectionField
    private ForeignCollection<WifiResult> wifiResults;
    @DatabaseField
    private Room room;
    @DatabaseField
    private long timestamp;
    @DatabaseField(columnName = "numap")
    private int numAP;

    public WifiResultCollection(long timestamp, Room room, int numAP) {
        this.timestamp = timestamp;
        this.room = room;

        this.numAP = numAP;
    }

    public WifiResultCollection() {
        // ORMlite
    }

    public int getNumAP() {
        return numAP;
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
