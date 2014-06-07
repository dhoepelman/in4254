package nl.tudelft.sps.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.Collections;
import java.util.Map;

import nl.tudelft.sps.app.localization.Room;

public class LocatorNSAFragment extends Fragment {

    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    ToastManager toastManager;
    View rootView;

    private Map<Room, Double> location = Collections.EMPTY_MAP;
    private DeviceFollowingRunnable follower;

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

    public void connectToDevice(final String ip, final int port) {
        if (follower != null) {
            follower.stop();
        }
        follower = new DeviceFollowingRunnable(ip, port);
        new Thread(follower).start();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_locator_nsa, container, false);
        assert rootView != null;

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
        location = Collections.EMPTY_MAP;
        toastManager.showText("Initial belief set", Toast.LENGTH_SHORT);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Update title in navigation bar
        ((MainActivity) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        /*try {
            if (followingSocket != null && !followingSocket.isClosed()) {
                followingSocket.close();
            }
        } catch (IOException e) {
            Log.w(LocatorNSAFragment.class.getName(), e);
        }*/
    }

    /**
     * Class that connects to target device and listens for location updates
     */
    private class DeviceFollowingRunnable implements Runnable {

        private final String ip;
        private final int port;
        Socket followingSocket;
        private boolean keepRunning = true;

        public DeviceFollowingRunnable(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }

        public void stop() {
            keepRunning = false;
            if (followingSocket != null && !followingSocket.isClosed()) {
                try {
                    followingSocket.close();
                } catch (IOException e) {
                    final String msg = String.format("Could not close socket %s:%d", ip, port);
                    Log.w(LocatorNSAFragment.class.getName(), msg, e);
                }
            }
        }

        @Override
        public void run() {

            // Connect to the target device
            try {
                followingSocket = new Socket(ip, port);
            } catch (IOException e) {
                final String msg = String.format("Could not connect to %s:%d", ip, port);
                Log.e(LocatorNSAFragment.class.getName(), msg, e);
                toastManager.showText(msg, Toast.LENGTH_LONG);
                return;
            }

            // Try-finally block to ensure socket will be closed
            try {
                // Acquire an object input stream
                ObjectInputStream ois;
                try {
                    ois = new ObjectInputStream(followingSocket.getInputStream());
                } catch (IOException e) {
                    final String msg = String.format("Could not open inputstream of socket");
                    Log.e(LocatorNSAFragment.class.getName(), msg, e);
                    toastManager.showText(msg, Toast.LENGTH_LONG);
                    return;
                }

                // Continue receiving locations while we're not interrupted
                while (keepRunning && !Thread.currentThread().isInterrupted()) {
                    Map<Room, Double> receivedLocation = null;
                    try {
                        receivedLocation = (Map<Room, Double>) ois.readObject();
                    } catch (ClassCastException e) {
                        Log.e(LocatorNSAFragment.class.getName(), "Unexpected input (not a map) received over socket", e);
                        return;
                    } catch (ClassNotFoundException e) {
                        Log.e(LocatorNSAFragment.class.getName(), "ClassNotFoundException while deserializing socket input", e);
                        return;
                    } catch (IOException e) {
                        // This will be thrown when the socket it closed. There's no other way to unblock ois.readObject() :(
                        Log.w(LocatorNSAFragment.class.getName(), "Error while reading socket input. This is expected behavior when listening stops", e);
                    }
                    if (receivedLocation != null) {
                        // Ugh, this wouldn't be neccesary with Java 8's real lambda's...
                        final Map<Room, Double> newLocation = receivedLocation;
                        // Update location text on UI thread
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                location = newLocation;
                                updateLocationText();
                            }
                        });
                    }
                }

                toastManager.showText(String.format("Stopped listening to %s:%d", ip, port), Toast.LENGTH_SHORT);
            } finally {
                stop();
            }
        }
    }
}
