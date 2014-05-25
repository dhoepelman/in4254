package nl.tudelft.sps.app;

import android.app.Activity;
import android.graphics.Color;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.j256.ormlite.dao.RuntimeExceptionDao;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import nl.tudelft.sps.app.activity.ACTIVITY;
import nl.tudelft.sps.app.activity.StepsCounter;
import nl.tudelft.sps.app.localization.ILocator;
import nl.tudelft.sps.app.localization.Room;
import nl.tudelft.sps.app.localization.WifiMeasurementsWindow;
import nl.tudelft.sps.app.localization.WifiResult;
import nl.tudelft.sps.app.localization.WifiResultCollection;
import nl.tudelft.sps.app.localization.WifiScanTask;

public class LocatorTestFragment extends Fragment {

    private StepsCounter stepsCounter;
    private Thread stepsCounterThread;

    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    private final WifiScanTask.ResultProcessor wifiScanResultProcessor = new WifiScanTask.ResultProcessor() {
        @Override
        public void result(WifiMeasurementsWindow results) {
            if (results != null) {
                final List<ScanResult> scanResults = results.getMeasurements().get(0).getResults();
                int iterations = locator.adjustLocation(scanResults);
                toastManager.showText(String.format("Used %d out of %d AP's", iterations, scanResults.size()), Toast.LENGTH_SHORT);
                updateLocationText();
            } else {
                toastManager.showText("Something went horribly wrong", Toast.LENGTH_LONG);
            }
        }
    };
    ToastManager toastManager;
    View rootView;
    private ILocator locator;

    public LocatorTestFragment() {
        // Required empty public constructor
    }

    /**
     * Returns a new instance of this fragment for the given section number
     */
    public static LocatorTestFragment newInstance(int sectionNumber) {
        final Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);

        final LocatorTestFragment fragment = new LocatorTestFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_locator_test, container, false);
        assert rootView != null;

        locator = ((MainActivity) getActivity()).getLocator();

        toastManager = new ToastManager(getActivity());

        // Connect click listener to start button
        (rootView.findViewById(R.id.but_initial)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setInitialLocation();
            }
        });
        (rootView.findViewById(R.id.but_scan)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doWifiScan();
            }
        });
        (rootView.findViewById(R.id.but_movement)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doMovementDetection();
            }
        });
        (rootView.findViewById(R.id.but_fakescan)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new RoomChoiceDialog(new RoomChoiceDialog.ChoiceListener() {
                    @Override
                    public void onChosen(Room r) {
                        doFakeScan(r);
                    }
                }).show(getFragmentManager(), "roomchoise");
            }
        });

        // Make buttons not clickable
        for (Room room : Room.values()) {
            final Button button = (Button) rootView.findViewById(room.getTestIdentifier());
            button.setClickable(false);
        }
        updateLocationText();

        stepsCounter = new StepsCounter(getActivity());
        stepsCounterThread = new Thread(stepsCounter);
        stepsCounterThread.start();

        return rootView;
    }

    private void setInitialLocation() {
        locator.initialLocation();
        updateLocationText();
        toastManager.showText("Initial belief set", Toast.LENGTH_SHORT);
    }

    private void doFakeScan(Room r) {
        // Select a random scan from the database
        final RuntimeExceptionDao<WifiResultCollection, Long> dao = ((MainActivity) getActivity()).getDatabaseHelper().getWifiResultCollectionDao();
        try {
            final WifiResultCollection scan = dao.queryBuilder().orderByRaw("RANDOM()").limit(1L).where().eq("room", r).queryForFirst();
            // TODO: Figure out how to get ForeignCollection to work properly
            //locator.adjustLocation(scan.getWifiResults());
            List<WifiResult> blah = ((MainActivity) getActivity()).getDatabaseHelper().getWifiResultDao().queryForEq("scan", scan.getId());
            int iterations = locator.adjustLocation(blah);
            toastManager.showText(String.format("Fake scan for %s took %d AP's into account", scan.getRoom().name(), iterations), Toast.LENGTH_LONG);
            updateLocationText();
        } catch (SQLException e) {
            toastManager.showText("Something went wrong while querying the database", Toast.LENGTH_SHORT);
            Log.e(LocatorTestFragment.class.getName(), "Error while querying database", e);
        }
    }

    private void doWifiScan() {
        final WifiScanTask wifiScanTask = new WifiScanTask(wifiScanResultProcessor, null, getActivity(), toastManager, false);
        wifiScanTask.execute((Room) null);
    }

    private void doMovementDetection() {
        // TODO: Implement
        locator.addMovement(ACTIVITY.Walking);
        updateLocationText();
    }

    /**
     * Format the current location nicely
     */
    private void updateLocationText() {
        final Map<Room, Double> location = locator.getLocation();

        for (Room room : Room.values()) {
            final long percent = Math.round(location.get(room) * 100L);
            final Button button = (Button) rootView.findViewById(room.getTestIdentifier());

            // Set text
            final String label = getString(room.getLabelIdentifier());
            button.setText(String.format("%s (%d%%)", label, percent));

            // Set color
            if (percent >= 50) {
                button.setBackgroundColor(Color.GREEN);
            } else if (percent >= 20) {
                button.setBackgroundColor(Color.YELLOW);
            } else {
                button.setBackgroundColor(Color.LTGRAY);
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Update title in navigation bar
        ((MainActivity) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));

        if (stepsCounterThread != null && stepsCounterThread.isAlive()) {
            stepsCounter.stopMeasuring();
        }

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
