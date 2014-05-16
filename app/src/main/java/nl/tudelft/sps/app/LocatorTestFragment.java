package nl.tudelft.sps.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map;

import nl.tudelft.sps.app.activity.ACTIVITY;
import nl.tudelft.sps.app.localization.ILocator;
import nl.tudelft.sps.app.localization.Room;

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


        return rootView;
    }

    private void setInitialLocation() {
        locator.initialLocation();
        updateLocationText();
        toastManager.showText("Initial belief set", Toast.LENGTH_SHORT);
    }

    private void doWifiScan() {

        getActivity().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                getActivity().unregisterReceiver(this);
                WifiManager wifimanager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
                locator.adjustLocation(wifimanager.getScanResults());
                updateLocationText();
            }
        }, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        ((WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE)).startScan();
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
        StringBuilder sb = new StringBuilder();
        final Map<Room, Double> location = locator.getLocation();
        for (Room r : Room.values()) {
            sb.append(r.name());
            sb.append(" : ");
            sb.append(Math.round(location.get(r) * 1000) / 1000.0);
            sb.append("\n");
        }
        ((TextView) rootView.findViewById(R.id.txt_locator_test_results)).setText(sb.toString());
    }

}
