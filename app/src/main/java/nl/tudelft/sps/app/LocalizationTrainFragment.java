package nl.tudelft.sps.app;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.net.wifi.ScanResult;

import com.j256.ormlite.dao.RuntimeExceptionDao;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import nl.tudelft.sps.app.localization.AccessPointLevels;
import nl.tudelft.sps.app.localization.WifiMeasurement;
import nl.tudelft.sps.app.localization.WifiMeasurementsWindow;
import nl.tudelft.sps.app.localization.WifiResult;
import nl.tudelft.sps.app.localization.WifiScanTask;
import nl.tudelft.sps.app.localization.Room;

public class LocalizationTrainFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";

    private TextView valueResults;
    private boolean firstResult;
    private ProgressBar progressBarWindow;

    private ToastManager toastManager;

    private Room selectedRoom;

    private final WifiScanTask.ResultProcessor wifiScanResultProcessor = new WifiScanTask.ResultProcessor() {
        @Override
        public void result(WifiMeasurementsWindow results) {
            if (results != null) {
                // Display average dBm on the screen
                final StringBuilder builder = new StringBuilder();
                for (Entry<String, AccessPointLevels> entry : results.getAccessPointLevels().entrySet()) {
                    final AccessPointLevels apLevels = entry.getValue();
                    final List<Integer> levels = apLevels.getLevels();

                    // Build statistics for mean
                    final DescriptiveStatistics statistics = new DescriptiveStatistics(levels.size());
                    for (Integer level : levels) {
                        statistics.addValue(level);
                    }

                    if (firstResult) {
                        firstResult = false;
                    }
                    else {
                        builder.append("\n");
                    }
                    builder.append(String.format("%s %s\n%.2f dBm (%d samples)", apLevels.SSID, apLevels.BSSID, statistics.getMean(), levels.size()));
                }
                valueResults.setText(builder);


                // Create a list of table rows so we can tell the user
                // how many rows will be created
                final List<WifiResult> tableRows = new ArrayList<WifiResult>();

                final Room measuredInRoom = results.getMeasuredInRoom();
                for (WifiMeasurement measurement : results.getMeasurements()) {
                    final long timestamp = measurement.getTimestamp();
                    for (ScanResult scanResult : measurement.getResults()) {
                        tableRows.add(new WifiResult(measuredInRoom, scanResult.BSSID, scanResult.SSID, scanResult.level, timestamp));
                    }
                }

                toastManager.showText(String.format("Going to write %d rows to database...", tableRows.size()), Toast.LENGTH_LONG);
                Log.w(getClass().getName(), String.format("WIFI RESULTS %d", tableRows.size()));

                // Log raw results to database
                final MainActivity mainActivity = (MainActivity) getActivity();
                final RuntimeExceptionDao<WifiResult, Long> dao = mainActivity.getDatabaseHelper().getWifiResultDao();

                int rowsCreated = 0;
                for (WifiResult resultRow : tableRows) {
                    final int createResult = dao.create(resultRow);
                    if (createResult == 1) {
                        rowsCreated++;
                    }
                    Log.w(getClass().getName(), String.format("WIFI DB CREATE %d", createResult));
                }

                toastManager.showText(String.format("%d results written to database", rowsCreated), Toast.LENGTH_LONG);
                Log.w(getClass().getName(), String.format("WIFI DB CREATED ROWS %d", rowsCreated));
            }
            else {
                valueResults.setText("Error :(");
            }
        }
    };

    private final WifiScanTask.ProgressUpdater wifiScanProgressUpdater = new WifiScanTask.ProgressUpdater() {
        @Override
        public void update(Integer progress) {
            progressBarWindow.setProgress(progress);
        }
    };

    public static LocalizationTrainFragment newInstance(int sectionNumber) {
        final Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);

        final LocalizationTrainFragment fragment = new LocalizationTrainFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_localization_train, container, false);

        toastManager = new ToastManager(getActivity());

        valueResults = (TextView) rootView.findViewById(R.id.text_results);
        firstResult = true;

        progressBarWindow = (ProgressBar) rootView.findViewById(R.id.progressBar_wifiMeasurement);
        progressBarWindow.setMax(WifiMeasurementsWindow.WINDOW_SIZE);

        setHasOptionsMenu(true);

        // Connect click listener to buttons
        for (Room room : Room.values()) {
            if (!Room.Unknown.equals(room)) {
                final Button button = (Button) rootView.findViewById(room.getIdentifier());
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        selectedRoom = Room.getEnum(view.getId());
                        toastManager.showText(String.valueOf(selectedRoom), Toast.LENGTH_SHORT);
                    }
                });
            }
        }

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, 0, 0, "Scan");
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (selectedRoom == null) {
            toastManager.showText("Select a room first", Toast.LENGTH_LONG);
        }
        else {
            final WifiScanTask wifiScanTask = new WifiScanTask(wifiScanResultProcessor, wifiScanProgressUpdater, getActivity(), toastManager);
            wifiScanTask.execute(selectedRoom);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Update title in navigation bar
        ((MainActivity) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
    }

}