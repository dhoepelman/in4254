package nl.tudelft.sps.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.Patterns;
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
    private View but_start;
    private View but_stop;

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
        stopFollower();

        follower = new DeviceFollowingRunnable(ip, port);
        new Thread(follower).start();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_locator_nsa, container, false);
        assert rootView != null;

        toastManager = new ToastManager(getActivity());

        but_start = rootView.findViewById(R.id.but_nsa_start);
        but_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startListening();
            }
        });

        but_stop = rootView.findViewById(R.id.but_nsa_stop);
        but_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopListening();
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

    private void startListening() {
        new IPInputDialog(new IPInputDialog.IPInputDialogListener() {
            @Override
            public void onIPInput(DialogFragment dialog, String ip) {
                if (!Patterns.IP_ADDRESS.matcher(ip).matches()) {
                    toastManager.showText(String.format("Invalid IP %s", ip), Toast.LENGTH_SHORT);
                } else {
                    but_start.setEnabled(false);
                    but_stop.setEnabled(true);
                    connectToDevice(ip, LocatorTestFragment.SOCKET_PORT);
                }
            }
        }).show(getActivity().getSupportFragmentManager(), "ipinput");
    }

    private void stopListening() {
        stopFollower();
        but_stop.setEnabled(false);
        but_start.setEnabled(true);
    }

    private void stopFollower() {
        if (follower != null) {
            follower.stop();
            follower = null;
        }
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
        updateLocationText();
        toastManager.showText("Initial belief set", Toast.LENGTH_SHORT);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Update title in navigation bar
        ((MainActivity) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
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

        private void showToast(final String msg, final int duration) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    toastManager.showText(msg, duration);
                }
            });
        }

        @Override
        public void run() {

            // Connect to the target device
            try {
                followingSocket = new Socket(ip, port);
            } catch (IOException e) {
                final String msg = String.format("Could not connect to %s:%d", ip, port);
                Log.e(LocatorNSAFragment.class.getName(), msg, e);
                showToast(msg, Toast.LENGTH_LONG);
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
                    showToast(msg, Toast.LENGTH_LONG);
                    return;
                }

                showToast(String.format("Now listening to %s:%d", ip, port), Toast.LENGTH_SHORT);

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
                                toastManager.showText("New location received", Toast.LENGTH_SHORT);
                                location = newLocation;
                                updateLocationText();
                            }
                        });
                    }
                }

                showToast(String.format("Stopped listening to %s:%d", ip, port), Toast.LENGTH_SHORT);
            } finally {
                stop();
            }
        }
    }
}
