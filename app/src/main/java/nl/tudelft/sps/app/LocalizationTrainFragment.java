package nl.tudelft.sps.app;

import android.app.Activity;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import nl.tudelft.sps.app.localization.WifiScanTask;

public class LocalizationTrainFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";

    private TextView valueResults;

    private ToastManager toastManager;

    private final WifiScanTask.ResultProcessor wifiScanResultProcessor = new WifiScanTask.ResultProcessor() {
        @Override
        public void result(List<ScanResult> results) {
            if (results != null) {
                final StringBuilder builder = new StringBuilder();
                for (ScanResult result : results) {
                    builder.append(String.format("%s %d dBm\n", result.SSID, result.level));
                }
                valueResults.setText(builder);
            }
            else {
                valueResults.setText("Error :(");
            }
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

        setHasOptionsMenu(true);

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, 0, 0, "Scan");
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final WifiScanTask wifiScanTask = new WifiScanTask(wifiScanResultProcessor, getActivity(), toastManager);
        wifiScanTask.execute();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        //((MainActivity) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
    }

}
