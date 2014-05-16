package nl.tudelft.sps.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map;
import java.util.List;

import nl.tudelft.sps.app.activity.ACTIVITY;
import nl.tudelft.sps.app.localization.ILocator;
import nl.tudelft.sps.app.localization.Room;
import nl.tudelft.sps.app.localization.WifiMeasurementsWindow;
import nl.tudelft.sps.app.localization.WifiScanTask;

public class LocatorTestFragment extends Fragment {


    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";
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

        // Make buttons not clickable
        for (Room room : Room.values()) {
            final Button button = (Button) rootView.findViewById(room.getTestIdentifier());
            button.setClickable(false);
        }
        updateLocationText();

        return rootView;
    }

    private void setInitialLocation() {
        locator.initialLocation();
        updateLocationText();
        toastManager.showText("Initial belief set", Toast.LENGTH_SHORT);
    }

    private final WifiScanTask.ResultProcessor wifiScanResultProcessor = new WifiScanTask.ResultProcessor() {
        @Override
        public void result(WifiMeasurementsWindow results) {
            if (results != null) {
                final List<ScanResult> scanResults = results.getMeasurements().get(0).getResults();
                locator.adjustLocation(scanResults);
                updateLocationText();
            }
            else {
                toastManager.showText("Something went horribly wrong", Toast.LENGTH_LONG);
            }
        }
    };

    private void doWifiScan() {
        final WifiScanTask wifiScanTask = new WifiScanTask(wifiScanResultProcessor, null, getActivity(), toastManager, false);
        wifiScanTask.execute(null);
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
            }
            else if (percent >= 20) {
                button.setBackgroundColor(Color.YELLOW);
            }
            else {
                button.setBackgroundColor(Color.LTGRAY);
            }
        }
    }

}
