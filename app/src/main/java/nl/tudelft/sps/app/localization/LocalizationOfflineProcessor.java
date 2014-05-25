package nl.tudelft.sps.app.localization;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

import nl.tudelft.sps.app.MainActivity;
import nl.tudelft.sps.app.R;
import nl.tudelft.sps.app.ToastManager;

public class LocalizationOfflineProcessor extends Fragment {
    private static final int NUMBER_PARTITIONS = 10;
    private static final int NUMBER_TEST_PARTITIONS = 1;
    /**
     * Random seed to ensure randomness in partitions while still having predicable results between tests
     */
    private static final long SEED = 0xFBDA9573549514L;

    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final int RUN_ID = 3;

    View rootView;
    TextView output;
    ToastManager toastManager;
    MainActivity activity;
    ProgressBar progressBar;
    AsyncTask<Void, String, Void> processtask;
    private Button startButton;
    private Button abortButton;

    public LocalizationOfflineProcessor() {
        // Required empty public constructor
    }

    /**
     * Returns a new instance of this fragment for the given section number
     */
    public static LocalizationOfflineProcessor newInstance(int sectionNumber) {
        final Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);

        final LocalizationOfflineProcessor fragment = new LocalizationOfflineProcessor();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_localization_offline_processor, container, false);
        assert rootView != null;

        toastManager = new ToastManager(getActivity());

        output = (TextView) rootView.findViewById(R.id.output_offlineprocessing);
        output.setMovementMethod(new ScrollingMovementMethod());

        ((TextView) rootView.findViewById(R.id.valNumberPartitions)).setText(Integer.toString(NUMBER_PARTITIONS));
        ((TextView) rootView.findViewById(R.id.valNumberTestPartitions)).setText(Integer.toString(NUMBER_TEST_PARTITIONS));

        progressBar = (ProgressBar) rootView.findViewById(R.id.progressBar_processing);

        activity = (MainActivity) getActivity();

        startButton = (Button) rootView.findViewById(R.id.but_offlineproc_start);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startButton.setEnabled(false);
                abortButton.setEnabled(true);
                startProcessing();
            }
        });
        abortButton = (Button) rootView.findViewById(R.id.but_offlineproc_abort);
        abortButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                abortButton.setEnabled(false);
                if (processtask != null) {
                    processtask.cancel(true);
                }
                startButton.setEnabled(true);
            }
        });

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Update title in navigation bar
        ((MainActivity) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
    }

    private synchronized void output(String msg) {
        output.append(msg + "\n");
    }

    public void startProcessing() {
        processtask = new ProcessTask();
        processtask.execute();
    }

    @DatabaseTable
    public static class LocalizationOfflineProcessingResult {
        public static final String COLUMN_SCAN = "scan";
        @DatabaseField
        private Room expected;
        @DatabaseField
        private Room found;
        @DatabaseField
        private double certainty;
        @DatabaseField
        private int numAPs;
        @DatabaseField
        private int iterations;
        @DatabaseField
        private int process_id;
        @DatabaseField(foreign = true, canBeNull = true, columnName = COLUMN_SCAN)
        private WifiResultCollection scan;

        public LocalizationOfflineProcessingResult() {
            // ORMlite
        }

        public LocalizationOfflineProcessingResult(Room expected, Room found, double certainty, int numAPs, int iterations, int process_id, WifiResultCollection scan) {
            this.expected = expected;
            this.found = found;
            this.certainty = certainty;
            this.numAPs = numAPs;
            this.iterations = iterations;
            this.process_id = process_id;
            this.scan = scan;
        }

        @Override
        public String toString() {
            return "LocalizationOfflineProcessingResult{" +
                    "expected=" + expected +
                    ", found=" + found +
                    ", certainty=" + certainty +
                    ", numAPs=" + numAPs +
                    ", iterations=" + iterations +
                    '}';
        }
    }

    private class ProcessTask extends AsyncTask<Void, String, Void> {
        long lastTime = System.currentTimeMillis();
        private int progress = 0;

        @Override
        protected void onCancelled(Void aVoid) {
            onPostExecute(null);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            startButton.setEnabled(true);
            abortButton.setEnabled(false);
        }

        @Override
        protected Void doInBackground(Void... params) {
            output("Starting processing");
            // Get all the WifiResultCollections
            List<WifiResultCollection> allScans = activity.getDatabaseHelper().getWifiResultCollectionDao().queryForAll();
            // Set the size of the progress bar
            progressBar.setMax(2 * allScans.size());
            // Randomize them
            Collections.shuffle(allScans, new Random(SEED));
            // Divide them into partitions
            final int partitionSize = (int) Math.ceil(allScans.size() / (double) NUMBER_PARTITIONS);
            List<List<WifiResultCollection>> partitions = Lists.partition(allScans, partitionSize);

            // Go through all the partitions one-by-one and process each seperatly
            for (int i = 0; !isCancelled() && i < NUMBER_PARTITIONS; i++) {
                output(String.format("Using [%d,%d)+[%d,%d) as training, [%d,%d) as testing", 0, i, i + NUMBER_TEST_PARTITIONS, NUMBER_PARTITIONS, i, i + NUMBER_TEST_PARTITIONS));
                // Get the correct partitions for the training data
                Iterable<WifiResultCollection> trainingsdata = Iterables.concat(
                        Iterables.concat(partitions.subList(0, i)),
                        Iterables.concat(partitions.subList(i + NUMBER_TEST_PARTITIONS, NUMBER_PARTITIONS))
                );
                Iterable<WifiResultCollection> testData = Iterables.concat(partitions.subList(i, i + NUMBER_TEST_PARTITIONS));
                processPartition(trainingsdata, testData, partitionSize * NUMBER_TEST_PARTITIONS);
            }
            output("Done!");
            return null;
        }

        private void processPartition(Iterable<WifiResultCollection> trainingsdata, Iterable<WifiResultCollection> testData, int numTests) {
            // Create the Locator
            ILocator locator = new BayesianLocator();
            // Train the locator
            try {
                output("Now training locator");
                locator.train(activity.getDatabaseHelper().getWifiResultDao().queryBuilder().where().in(WifiResult.COLUMN_SCAN, trainingsdata).iterator());
                addProgress(numTests);
                output("Trained locator, now testing");
            } catch (SQLException e) {
                String msg = "SQL Error while training locator";
                Log.e(LocalizationOfflineProcessor.class.getName(), msg, e);
                output(msg);
                return;
            }
            // Run the locator on the test data
            final List<LocalizationOfflineProcessingResult> results = new ArrayList<>();
            for (WifiResultCollection scan : testData) {
                try {
                    if (isCancelled()) {
                        return;
                    }
                    List<WifiResult> scanAPs = activity.getDatabaseHelper().getWifiResultDao().queryForEq(WifiResult.COLUMN_SCAN, scan.getId());
                    locator.initialLocation();
                    int iterations = locator.adjustLocation(scanAPs);
                    final Room found = locator.getMostLikelyRoom();
                    final LocalizationOfflineProcessingResult result = new LocalizationOfflineProcessingResult(scan.getRoom(), found, locator.getProbability(found), scanAPs.size(), iterations, RUN_ID, scan);
                    results.add(result);
                    //Log.i(LocalizationOfflineProcessor.class.getName(), result.toString()+"\n");
                    addProgress(1);
                } catch (RuntimeException e) {
                    String msg = "Error while testing locator";
                    Log.e(LocalizationOfflineProcessor.class.getName(), msg, e);
                    output(msg);
                    return;
                }
            }
            output("Done testing, now storing in database");
            final RuntimeExceptionDao<LocalizationOfflineProcessingResult, Void> dao = activity.getDatabaseHelper().getLocalizationOfflineProcessingResultDao();
            dao.callBatchTasks(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    for (LocalizationOfflineProcessingResult result : results) {
                        dao.create(result);
                    }

                    return null;
                }
            });
            output("Done with this partition");
        }

        private void output(String msg) {
            long newTime = System.currentTimeMillis();
            publishProgress(String.format("%s (%d ms)", msg, (newTime - lastTime)));
            lastTime = newTime;
        }

        private void addProgress(int num) {
            progress += num;
            publishProgress();
        }


        @Override
        protected void onProgressUpdate(String... values) {
            if (values.length > 0) {
                LocalizationOfflineProcessor.this.output(Joiner.on("\n").join(values));
            }
            progressBar.setProgress(progress);
        }
    }
}
