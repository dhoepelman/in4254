package nl.tudelft.sps.app;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;

import java.sql.SQLException;

import nl.tudelft.sps.app.activity.IClassifier;
import nl.tudelft.sps.app.activity.Measurement;
import nl.tudelft.sps.app.activity.kNNClassifier;
import nl.tudelft.sps.app.localization.BayesianLocator;
import nl.tudelft.sps.app.localization.ILocator;
import nl.tudelft.sps.app.localization.WifiResult;

public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    private final static String LOGTAG = MainActivity.class.toString();

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    private IClassifier classifier;
    private ILocator locator;
    private DatabaseHelper databaseHelper = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        Fragment fragment;
        switch (position) {
            default:
            case 0:
                fragment = TestFragment.newInstance(position);
                break;
            case 1:
                fragment = TrainFragment.newInstance(position);
                break;
            case 2:
                fragment = LocalizationTrainFragment.newInstance(position);
                break;
            case 3:
                fragment = MiscFragment.newInstance(position);
                break;
        }
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();
        } else {
            final Context applicationContext = getApplicationContext();
            if (applicationContext != null) {
                Toast.makeText(applicationContext, "Not implemented", Toast.LENGTH_SHORT).show();
            }
            Log.d(LOGTAG, "Tried to open unimplemented fragment at menu position " + position);
        }
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 0:
                mTitle = getString(R.string.title_section_test_activity);
                break;
            case 1:
                mTitle = getString(R.string.title_section_train_activity);
                break;
            case 2:
                mTitle = getString(R.string.title_section_localization);
                break;
            case 3:
                mTitle = getString(R.string.title_section_misc);
                break;
        }
    }

    public void restoreActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        final int id = item.getItemId();
        //if (id == R.id.action_settings) {
        //    return true;
        //}
        return super.onOptionsItemSelected(item);
    }

    /**
     * Get the classifier for Activity from measurements
     */
    public IClassifier getClassifier() {
        if (classifier == null || !classifier.isTrained()) {
            resetClassifier();
        }
        return classifier;
    }

    public ILocator getLocator() {
        if (locator == null) {
            resetLocator();
        }
        return locator;
    }

    public void resetLocator() {
        locator = new BayesianLocator();
        RuntimeExceptionDao<WifiResult, Long> dao = getDatabaseHelper().getWifiResultDao();
        // Build a query that does not return the ignored SSIDs
        PreparedQuery<WifiResult> q;
        try {
            QueryBuilder<WifiResult, Long> queryBuilder = dao.queryBuilder();
            queryBuilder.where().notIn("SSID", BayesianLocator.ignoredSSIDs);
            q = queryBuilder.prepare();
        } catch (SQLException e) {
            throw new RuntimeException("Could not build the query to retrieve stored wifi data");
        }
        locator.train(dao.iterator(q));
    }

    public void resetClassifier() {
        classifier = new kNNClassifier();
        readTrainingData();
    }

    /**
     * Read from the database containing the training measurements and
     * use them to train the classifier.
     */
    private void readTrainingData() {
        CloseableIterator<Measurement> measurementIt = getDatabaseHelper().getMeasurementDao().iterator();
        while (measurementIt.hasNext()) {
            Measurement m = measurementIt.next();
            classifier.train(m.getActivity(), m);
        }
        measurementIt.closeQuietly();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseHelper != null) {
            OpenHelperManager.releaseHelper();
            databaseHelper = null;
        }
    }

    public DatabaseHelper getDatabaseHelper() {
        if (databaseHelper == null) {
            databaseHelper = OpenHelperManager.getHelper(this, DatabaseHelper.class);
        }
        return databaseHelper;
    }

}
