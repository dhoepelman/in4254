package nl.tudelft.sps.app.activity;

import android.provider.BaseColumns;

import nl.tudelft.sps.app.data.DAO;

/**
* Enum for different type of activities
*/
public enum ACTIVITY implements BaseColumns {
    SITTING(0),
    WALKING(1),
    RUNNING(2),
    STAIRS_UP(3),
    STAIRS_DOWN(4),
    UNKNOWN(99);

    public final int id;

    ACTIVITY(int id) {
        this.id = id;
    }

    public static final String TABLE_NAME = "act";
    public static final String COLUMN_NAME = "name";    public static final String TABLE_DROP = "DROP TABLE IF EXISTS " + TABLE_NAME + ";";
    public static final String TABLE_CREATE = "CREATE TABLE " + TABLE_NAME + " (" +
            _ID + " INTEGER PRIMARY KEY," +
            COLUMN_NAME + " TEXT)";
}
