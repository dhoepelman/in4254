package nl.tudelft.sps.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import java.util.Map;

import nl.tudelft.sps.app.localization.NSALocator;
import nl.tudelft.sps.app.localization.Room;

public class LocatorNSAFragment extends Fragment {

    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    ToastManager toastManager;
    View rootView;
    private NSALocator locator;

    public LocatorNSAFragment() {
        // Required empty public constructor
    }

    /**
     * Returns a new instance of this fragment for the given section number
     */
    public static LocatorNSAFragment newInstance(int sectionNumber) {
        final Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);

        final LocatorNSAFragment fragment = new LocatorNSAFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_locator_nsa, container, false);
        assert rootView != null;

        locator = new NSALocator();

        toastManager = new ToastManager(getActivity());

        (rootView.findViewById(R.id.but_nsa_toggle)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: toggle scanning on or off
            }
        });
        (rootView.findViewById(R.id.but_initial)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setInitialLocation();
            }
        });

        // Make buttons not clickable
        for (Room room : Room.values()) {
            final Button button = (Button) rootView.findViewById(room.getTestIdentifier());
            button.setClickable(false);
        }
        updateLocationText();

        //setHasOptionsMenu(true);

        return rootView;
    }

    /**
     * Format the current location nicely
     */
    private void updateLocationText() {
        final Map<Room, Double> location = locator.getLocation();

        for (Room room : Room.values()) {
            final Long percent = (location.get(room) != null) ? Math.round(location.get(room) * 100L) : null;
            final Button button = (Button) rootView.findViewById(room.getTestIdentifier());

            // Set text
            final String label = getString(room.getLabelIdentifier());
            if (percent != null) {
                button.setText(String.format("%s (%d%%)", label, percent));
            } else {
                button.setText(String.format("%s (?)", label));
            }

            // Set color
            if (percent != null && percent >= 50) {
                button.setBackgroundColor(Color.GREEN);
            } else if (percent != null && percent >= 20) {
                button.setBackgroundColor(Color.YELLOW);
            } else {
                button.setBackgroundColor(Color.LTGRAY);
            }
        }
    }

    private void setInitialLocation() {
        locator.initialLocation();
        updateLocationText();
        toastManager.showText("Initial belief set", Toast.LENGTH_SHORT);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Update title in navigation bar
        ((MainActivity) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
    }

}
