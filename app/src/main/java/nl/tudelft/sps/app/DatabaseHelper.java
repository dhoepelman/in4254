package nl.tudelft.sps.app;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.sql.SQLException;

import nl.tudelft.sps.app.activity.Measurement;
import nl.tudelft.sps.app.activity.Sample;
import nl.tudelft.sps.app.localization.WifiResult;

public class DatabaseHelper extends OrmLiteSqliteOpenHelper {
    private static final String DATABASE_NAME = "sps.db";
    private static final int DATABASE_VERSION = 2;

    public RuntimeExceptionDao<Measurement, Long> measurementDao;
    public RuntimeExceptionDao<Sample, Void> sampleDao;

    private RuntimeExceptionDao<WifiResult, Long> wifiResultDao;

    public DatabaseHelper(Context context) {
        // TODO: Optimize database initialization speed. See http://ormlite.com/javadoc/ormlite-core/doc-files/ormlite_4.html#Config-Optimization
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public RuntimeExceptionDao<Measurement, Long> getMeasurementDao() {
        if (measurementDao == null) {
            measurementDao = getRuntimeExceptionDao(Measurement.class);
        }
        return measurementDao;
    }

    public RuntimeExceptionDao<WifiResult, Long> getWifiResultDao() {
        if (wifiResultDao == null) {
            wifiResultDao = getRuntimeExceptionDao(WifiResult.class);

            try {
                TableUtils.createTableIfNotExists(wifiResultDao.getConnectionSource(), WifiResult.class);
                Log.i(DatabaseHelper.class.getName(), "Table for " + String.valueOf(WifiResult.class) + " succesfully created");
            }
            catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        }
        return wifiResultDao;
    }

    public RuntimeExceptionDao<Sample, Void> getSampleDao() {
        if (sampleDao == null) {
            sampleDao = getRuntimeExceptionDao(Sample.class);
        }
        return sampleDao;
    }

    @Override
    public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
        try {
            TableUtils.createTable(connectionSource, Measurement.class);
            TableUtils.createTable(connectionSource, Sample.class);
            TableUtils.createTable(connectionSource, WifiResult.class);
        } catch (SQLException e) {
            Log.e(DatabaseHelper.class.getName(), "Can't create database", e);
            throw new RuntimeException(e);
        }
        Log.i(DatabaseHelper.class.getName(), "Database succesfully created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        //throw new RuntimeException("Old database version detected. Please manually move or delete the old database (sps.db) to avoid loss of data.");
        try {
            TableUtils.dropTable(connectionSource, Measurement.class, true);
            TableUtils.dropTable(connectionSource, Sample.class, true);
            TableUtils.dropTable(connectionSource, WifiResult.class, true);
        } catch (SQLException e) {
            Log.e(DatabaseHelper.class.getName(), "Couldn't upgrade database");
            throw new RuntimeException(e);
        }
        onCreate(db, connectionSource);
    }

    /**
     * Close the database connections and clear any cached DAOs.
     */
    @Override
    public void close() {
        exportDatabaseFile();
        super.close();
        sampleDao = null;
        measurementDao = null;
        wifiResultDao = null;
    }

    /**
     * Export database file to sdcard
     */
    public void exportDatabaseFile() {
        //http://stackoverflow.com/a/2661882/572635
        try {
            File sd = Environment.getExternalStorageDirectory();
            File data = Environment.getDataDirectory();

            String dbPath = "/data/nl.tudelft.sps.app/databases/" + DATABASE_NAME;
            FileChannel src = new FileInputStream(new File(data, dbPath)).getChannel();
            FileChannel dest = new FileOutputStream(new File(sd, DATABASE_NAME)).getChannel();
            dest.transferFrom(src, 0, src.size());
            src.close();
            dest.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
