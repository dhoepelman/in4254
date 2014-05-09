package nl.tudelft.sps.app.activity;

import android.provider.BaseColumns;

/**
 * Features for the classifier
 */
public enum FEATURE implements BaseColumns {
    MeanX(0),
    MeanY(1),
    MeanZ(2),
    StdDevX(3),
    StdDevY(4),
    StdDevZ(5),
    CorrXY(6),
    CorrYZ(7),
    CorrZX(8);

    public final int id;

    private FEATURE(int id) {
        this.id = id;
    }

    public static final String TABLE_NAME = "act_features";
    public static final String COLUMN_NAME = "name";
    public static final String TABLE_DROP = "DROP TABLE IF EXISTS " + TABLE_NAME + ";";
    public static final String TABLE_CREATE = "CREATE TABLE " + TABLE_NAME + " (" +
                _ID + " INTEGER PRIMARY KEY," +
                COLUMN_NAME + " TEXT)";
}
