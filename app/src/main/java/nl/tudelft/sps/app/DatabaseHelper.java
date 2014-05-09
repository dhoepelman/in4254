package nl.tudelft.sps.app;

import android.content.Context;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;

import nl.tudelft.sps.app.activity.Measurement;
import nl.tudelft.sps.app.activity.Sample;

public class DatabaseHelper extends OrmLiteSqliteOpenHelper {
    private static final String DATABASE_NAME = "sps.db";
    private static final int DATABASE_VERSION = 1;

    public final Dao<Measurement, Integer> measurementDao;
    public final Dao<Sample, >

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION, R.raw.ormlite_config);
    }
}
