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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.List;
import java.util.Map.Entry;

import nl.tudelft.sps.app.localization.AccessPointLevels;
import nl.tudelft.sps.app.localization.WifiMeasurementsWindow;
import nl.tudelft.sps.app.localization.WifiScanTask;

public class LocalizationTrainFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";

    private TextView valueResults;
    private ProgressBar progressBarWindow;

    private ToastManager toastManager;

    private final WifiScanTask.ResultProcessor wifiScanResultProcessor = new WifiScanTask.ResultProcessor() {
        @Override
        public void result(WifiMeasurementsWindow results) {
            if (results != null) {
                final StringBuilder builder = new StringBuilder();
                for (Entry<String, AccessPointLevels> entry : results.getAllResults().entrySet()) {
                    final AccessPointLevels apLevels = entry.getValue();
                    final List<Integer> levels = apLevels.getLevels();

                    // Build statistics for mean
                    final DescriptiveStatistics statistics = new DescriptiveStatistics(levels.size());
                    for (Integer level : levels) {
                        statistics.addValue(level);
                    }

                    builder.append(String.format("%s %s\n%.2f dBm (%d samples)\n", apLevels.SSID, apLevels.BSSID, statistics.getMean(), levels.size()));
                }
                valueResults.setText(builder);
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

        progressBarWindow = (ProgressBar) rootView.findViewById(R.id.progressBar_wifiMeasurement);
        progressBarWindow.setMax(WifiMeasurementsWindow.WINDOW_SIZE);

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
        final WifiScanTask wifiScanTask = new WifiScanTask(wifiScanResultProcessor, wifiScanProgressUpdater, getActivity(), toastManager);
        wifiScanTask.execute();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // TODO is this ((MainActivity) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER)); necessary?
    }

}
