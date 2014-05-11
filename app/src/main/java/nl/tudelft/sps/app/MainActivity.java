package nl.tudelft.sps.app;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import nl.tudelft.sps.app.activity.TrainingPoint;
import nl.tudelft.sps.app.activity.kNNClassifier;
import nl.tudelft.sps.app.activity.IClassifier;

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
        Fragment fragment = null;
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

    public void resetClassifier() {
        classifier = new kNNClassifier();
        readTrainingData();
    }

    /**
     * Read from the file containing the training measurements and
     * use them to train the classifier.
     */
    private void readTrainingData() {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            final File resultsFile = new File(TrainFragment.RESULTS_FILE_PATH);
            if (resultsFile.exists()) {
                try {
                    classifier.train(TrainingPoint.fromCSV(resultsFile));
                }
                catch (IOException exception) {
                    displayToast("Failed to read training data");
                }
            }
            else {
                displayToast(String.format("%s does not exist", resultsFile.getName()));
            }
        }
        else {
            displayToast("External storage not mounted");
        }
    }

    /**
     * Display the message as a toast
     */
    private void displayToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

}
