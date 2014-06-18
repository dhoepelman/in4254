package nl.tudelft.sps.app;

import android.app.Activity;
import android.graphics.Color;
import android.net.wifi.ScanResult;
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
import android.widget.TextView;
import android.widget.Toast;

import com.j256.ormlite.dao.RuntimeExceptionDao;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import nl.tudelft.sps.app.activity.StepsCounter;
import nl.tudelft.sps.app.localization.ILocator;
import nl.tudelft.sps.app.localization.Room;
import nl.tudelft.sps.app.localization.WifiMeasurementsWindow;
import nl.tudelft.sps.app.localization.WifiResult;
import nl.tudelft.sps.app.localization.WifiResultCollection;
import nl.tudelft.sps.app.localization.WifiScanTask;

public class LocatorTestFragment extends Fragment {

    // A random port not in wikipedia's list of registered port numbers
    public static final int SOCKET_PORT = 7727;
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";
    /**
     * A synchronized list of devices listening to this phone's location.
     */
    final List<Socket> locationListeners = Collections.synchronizedList(new ArrayList<Socket>());
    private final WifiScanTask.ResultProcessor wifiScanResultProcessor = new WifiScanTask.ResultProcessor() {
        @Override
        public void result(WifiMeasurementsWindow results) {
            if (results != null) {
                final List<ScanResult> scanResults = results.getMeasurements().get(0).getResults();
                final int iterations = locator.adjustLocation(scanResults);
                updateLocationText(iterations, scanResults.size());
            } else {
                toastManager.showText("Error: received null wifi scan from Android", Toast.LENGTH_LONG);
            }
        }
    };
    private final int stepsCounterMenuItemID = 0;
    private final int showIPMenuItemID = 1;
    ToastManager toastManager;
    View rootView;
    /**
     * Sockets where other phones can connect to to follow this phone's location
     *
     * @see nl.tudelft.sps.app.LocatorNSAFragment
     */
    private ServerSocket serverSocket;
    private StepsCounter stepsCounter;
    private Thread stepsCounterThread;
    private MenuItem stepsCounterMenuItem;
    private MenuItem showIPMenuItem;
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

        // Start the server thread
        new Thread(new SocketAcceptThread()).start();

        updateLocationText();

        setHasOptionsMenu(true);

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
            if (scan == null) {
                throw new SQLException("Could not find random room " + r + " in database");
            }
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

    /**
     * The user pressed the "Movement" button
     */
    public void doMovementDetection() {
        // Let us pretend that we walked one step
        doMovementDetection(1);
    }

    public void doMovementDetection(final int steps) {
        System.err.println(String.format("Steps: %d", steps));

        locator.addMovement(steps);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateLocationText();
                //toastManager.showText(String.format("Updated movement (%d steps)", steps), Toast.LENGTH_SHORT);
            }
        });
    }

    private void updateLocationText(int iterations, int aps) {
        updateLocationText();

        final TextView valueIter = (TextView) rootView.findViewById(R.id.val_num_iter);
        valueIter.setText(String.format("%d", iterations));

        final TextView valueAPs = (TextView) rootView.findViewById(R.id.val_num_aps);
        valueAPs.setText(String.format("%d", aps));
    }

    /**
     * Format the current location nicely
     */
    private void updateLocationText() {
        final Map<Room, Double> location = locator.getLocation();

        if (locationListeners.size() > 0) {
            // Update listeners with a copy of the map so changes to the location won't affect it if it is still sending
            updateLocationListeners(new HashMap<>(location));
        }

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

    private void updateLocationListeners(final HashMap<Room, Double> location) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Getting the lock is neccesary when iterating over the synchronized list
                synchronized (locationListeners) {
                    Iterator<Socket> iterator = locationListeners.iterator();
                    while (iterator.hasNext()) {
                        Socket listener = iterator.next();
                        if (listener.isClosed() || !listener.isConnected()) {
                            // Socket isn't open, remove it from the list
                            iterator.remove();
                        } else {
                            try {
                                ObjectOutputStream oos = new ObjectOutputStream(listener.getOutputStream());
                                oos.writeObject(location);
                            } catch (IOException e) {
                                Log.e(LocatorTestFragment.class.getName(), String.format("Could not send location to socket %s", listener.getRemoteSocketAddress().toString()), e);
                            }
                        }
                    }
                }
            }
        }).start();
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

        if (serverSocket != null) {
            // Close the socket
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.w(LocatorTestFragment.class.getName(), e);
            }
        }

        // Kill the steps counter if it is running
        stopStepsCounter();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        stepsCounterMenuItem = menu.add(Menu.NONE, stepsCounterMenuItemID, stepsCounterMenuItemID, "Start steps counter");
        showIPMenuItem = menu.add(Menu.NONE, showIPMenuItemID, showIPMenuItemID, "Show IP");
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case stepsCounterMenuItemID:
                if (stepsCounterThread == null || !stepsCounterThread.isAlive()) {
                    // Start the steps counter
                    startStepsCounter();
                } else {
                    stopStepsCounter();
                }
                break;
            case showIPMenuItemID:
                boolean success = false;
                String ip = null;
                try {
                    ip = getIPAddress();
                    success = !ip.equals("");
                } catch (SocketException e) {
                    Log.w(LocatorTestFragment.class.getName(), e);
                }
                toastManager.showText((success) ? ip : "Could not get IP address", Toast.LENGTH_SHORT);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private String getIPAddress() throws SocketException {
        // Based on http://stackoverflow.com/questions/6064510/how-to-get-ip-address-of-the-device
        List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
        for (NetworkInterface intf : interfaces) {
            List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
            for (InetAddress addr : addrs) {
                if (!addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
                    return addr.getHostAddress().toUpperCase();
                }
            }
        }
        return "";
    }

    private void startStepsCounter() {
        stepsCounter = new StepsCounter(this);
        stepsCounterThread = new Thread(stepsCounter);
        stepsCounterThread.start();

        stepsCounterMenuItem.setTitle("Stop steps counter");
        toastManager.showText("Started steps counter", Toast.LENGTH_SHORT);
    }

    private void stopStepsCounter() {
        if (stepsCounterThread != null && stepsCounterThread.isAlive()) {
            stepsCounter.stopMeasuring();

            stepsCounterMenuItem.setTitle("Start steps counter");
            toastManager.showText("Stopped steps counter", Toast.LENGTH_SHORT);
        }
    }

    private class SocketAcceptThread implements Runnable {

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(SOCKET_PORT);

                // This is not busy waiting, as ServerSocket.accept() blocks until a connection is made
                while (!Thread.currentThread().isInterrupted() && !serverSocket.isClosed()) {
                    try {
                        locationListeners.add(serverSocket.accept());
                    } catch (IOException e) {
                        Log.w(LocatorTestFragment.class.getName(), "Could not accept incoming socket connection or serversocket was closed", e);
                    }
                }
            }
            catch (IOException e) {
                Log.e(LocatorTestFragment.class.getName(), "Could not open server socket", e);
            }
        }
    }
}
