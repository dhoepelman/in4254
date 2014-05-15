package nl.tudelft.sps.app;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

public class MiscFragment extends Fragment {


    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    public MiscFragment() {
        // Required empty public constructor
    }

    /**
     * Returns a new instance of this fragment for the given section number
     */
    public static MiscFragment newInstance(int sectionNumber) {
        final Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);

        final MiscFragment fragment = new MiscFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_misc, container, false);
        assert rootView != null;

        final ToastManager toastManager = new ToastManager(getActivity());

        // Connect click listener to start button
        (rootView.findViewById(R.id.but_exportdb)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainActivity) getActivity()).getDatabaseHelper().exportDatabaseFile();
                toastManager.showText("Database exported to SD card", Toast.LENGTH_LONG);
            }
        });

        return rootView;
    }

}