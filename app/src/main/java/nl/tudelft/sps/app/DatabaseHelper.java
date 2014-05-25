package nl.tudelft.sps.app;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.sql.SQLException;

import nl.tudelft.sps.app.activity.Measurement;
import nl.tudelft.sps.app.activity.Sample;
import nl.tudelft.sps.app.localization.FFTResult;
import nl.tudelft.sps.app.localization.LocalizationOfflineProcessor;
import nl.tudelft.sps.app.localization.WifiResult;
import nl.tudelft.sps.app.localization.WifiResultCollection;

public class DatabaseHelper extends OrmLiteSqliteOpenHelper {
    private static final String DATABASE_NAME = "sps.db";
    private static final int DATABASE_VERSION = 4;
    public RuntimeExceptionDao<Measurement, Long> measurementDao;
    public RuntimeExceptionDao<Sample, Void> sampleDao;
    private RuntimeExceptionDao<WifiResult, Long> wifiResultDao;
    private RuntimeExceptionDao<WifiResultCollection, Long> wifiResultCollectionDao;
    private RuntimeExceptionDao<FFTResult, Long> FFTResultDao;
    private RuntimeExceptionDao<LocalizationOfflineProcessor.LocalizationOfflineProcessingResult, Void> localizationOfflineProcessingResultDao;

    public DatabaseHelper(Context context) {
        // TODO: Optimize database initialization speed. See http://ormlite.com/javadoc/ormlite-core/doc-files/ormlite_4.html#Config-Optimization
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Import DATABASE_NAME.import file if it is on the SD card
     * From: http://stackoverflow.com/a/6542214/572635
     */
    public static void importDatabase() throws FileNotFoundException, IOException {
        File importDB = new File(Environment.getExternalStorageDirectory(), DATABASE_NAME + ".import");
        if (!importDB.exists()) {
            throw new FileNotFoundException(String.format("File %s doesn't exist", importDB.getPath()));
        }
        File dbFile = new File(Environment.getDataDirectory(), "/data/nl.tudelft.sps.app/databases/" + DATABASE_NAME);

        FileChannel src = new FileInputStream(importDB).getChannel();
        FileChannel dest = new FileOutputStream(dbFile).getChannel();

        src.transferTo(0, src.size(), dest);
        src.close();
        dest.close();

        importDB.delete();

        Log.i(DatabaseHelper.class.getName(), "Successfully imported database");
    }

    public static void exportDatabaseFile(Context context) {
        exportDatabaseFile(DATABASE_NAME, context);
    }

    public static void backupDatabaseFile(Context context) {
        exportDatabaseFile(DATABASE_NAME + ".backup", context);
    }

    /**
     * Export database file to sdcard
     */
    public static void exportDatabaseFile(String filename, Context context) {
        //http://stackoverflow.com/a/2661882/572635
        try {
            File sd = Environment.getExternalStorageDirectory();
            File data = Environment.getDataDirectory();

            String dbPath = "/data/nl.tudelft.sps.app/databases/" + DATABASE_NAME;
            FileChannel src = new FileInputStream(new File(data, dbPath)).getChannel();
            final File outputfile = new File(sd, filename);
            FileChannel dest = new FileOutputStream(outputfile).getChannel();
            dest.transferFrom(src, 0, src.size());
            src.close();
            dest.close();

            if (context != null) {
                // Make the file known to android so it'll show it to the computer
                // http://www.grokkingandroid.com/adding-files-to-androids-media-library-using-the-mediascanner/
                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                intent.setData(Uri.fromFile(outputfile));
                context.sendBroadcast(intent);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        }
        return wifiResultDao;
    }

    public RuntimeExceptionDao<FFTResult, Long> getFFTResultDao() {
        if (FFTResultDao == null) {
            FFTResultDao = getRuntimeExceptionDao(FFTResult.class);

            try {
                TableUtils.createTableIfNotExists(FFTResultDao.getConnectionSource(), FFTResult.class);
                Log.i(DatabaseHelper.class.getName(), "Table for " + String.valueOf(FFTResult.class) + " succesfully created");
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        }
        return FFTResultDao;
    }

    public RuntimeExceptionDao<Sample, Void> getSampleDao() {
        if (sampleDao == null) {
            sampleDao = getRuntimeExceptionDao(Sample.class);
        }
        return sampleDao;
    }

    public RuntimeExceptionDao<WifiResultCollection, Long> getWifiResultCollectionDao() {
        if (wifiResultCollectionDao == null) {
            wifiResultCollectionDao = getRuntimeExceptionDao(WifiResultCollection.class);
        }
        return wifiResultCollectionDao;
    }

    public RuntimeExceptionDao<LocalizationOfflineProcessor.LocalizationOfflineProcessingResult, Void> getLocalizationOfflineProcessingResultDao() {
        if (localizationOfflineProcessingResultDao == null) {
            localizationOfflineProcessingResultDao = getRuntimeExceptionDao(LocalizationOfflineProcessor.LocalizationOfflineProcessingResult.class);
        }
        return localizationOfflineProcessingResultDao;
    }

    @Override
    public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
        try {
            TableUtils.createTableIfNotExists(connectionSource, Measurement.class);
            TableUtils.createTableIfNotExists(connectionSource, Sample.class);
            TableUtils.createTableIfNotExists(connectionSource, WifiResult.class);
            TableUtils.createTableIfNotExists(connectionSource, WifiResultCollection.class);
            TableUtils.createTableIfNotExists(connectionSource, FFTResult.class);
            TableUtils.createTableIfNotExists(connectionSource, LocalizationOfflineProcessor.LocalizationOfflineProcessingResult.class);
        } catch (SQLException e) {
            Log.e(DatabaseHelper.class.getName(), "Can't create database", e);
            throw new RuntimeException(e);
        }
        Log.i(DatabaseHelper.class.getName(), "Database successfully created/updated");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        // Backup database
        backupDatabaseFile(null);

        // Check for compatibility
        if (oldVersion == 2 && newVersion == 3) {
            getWifiResultDao().executeRawNoArgs("ALTER TABLE wifiresult ADD COLUMN scan");
        } else if (oldVersion == 3 && newVersion == 4) {

        } else {
            //throw new RuntimeException("Old database version detected. Please manually delete the old database (sps.db) to avoid loss of data.");
            try {
                TableUtils.dropTable(connectionSource, Measurement.class, true);
                TableUtils.dropTable(connectionSource, Sample.class, true);
                TableUtils.dropTable(connectionSource, WifiResult.class, true);
                TableUtils.dropTable(connectionSource, WifiResultCollection.class, true);
                TableUtils.dropTable(connectionSource, FFTResult.class, true);
                TableUtils.dropTable(connectionSource, LocalizationOfflineProcessor.LocalizationOfflineProcessingResult.class, true);
            } catch (SQLException e) {
                Log.e(DatabaseHelper.class.getName(), "Couldn't upgrade database");
                throw new RuntimeException(e);
            }
        }
        onCreate(db, connectionSource);

    }

    /**
     * Close the database connections and clear any cached DAOs.
     */
    @Override
    public void close() {
        super.close();
        sampleDao = null;
        measurementDao = null;
        wifiResultDao = null;
        wifiResultCollectionDao = null;
        FFTResultDao = null;
    }

}
