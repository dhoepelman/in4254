package nl.tudelft.sps.app.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

import nl.tudelft.sps.app.activity.ACTIVITY;
import nl.tudelft.sps.app.activity.FEATURE;
import nl.tudelft.sps.app.activity.Measurement;
import nl.tudelft.sps.app.activity.Sample;

public class DBHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "sps.db";

    List<Class> DAOs = Arrays.asList(new Class[] {
            ACTIVITY.class,
            FEATURE.class,
            Measurement.class,
            Sample.class
    });

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public void onCreate(SQLiteDatabase db) {
        for(Class dao : DAOs) {
            try {
                db.execSQL((String) dao.getField("CREATE_TABLE").get(null));
            } catch (IllegalAccessException|NoSuchFieldException e) {
                Log.e(getClass().getName(), dao + " is not a DAO");
                throw new IllegalArgumentException(e);
            }
        }
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("We cannot upgrade the DB");
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("We cannot downgrade the DB");
    }
}
