package nl.tudelft.sps.app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import nl.tudelft.sps.app.localization.Room;

public class RoomChoiceDialog extends DialogFragment {

    private static final String[] rooms = Room.asStringArray();
    private final ChoiceListener callback;

    public RoomChoiceDialog() {
        this(null);
    }

    public RoomChoiceDialog(ChoiceListener callback) {
        super();
        this.callback = callback;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.pick_room)
                .setItems(rooms, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (callback != null) {
                            callback.onChosen(Room.valueOf(rooms[which]));
                        }
                    }
                });
        return builder.create();
    }

    public static interface ChoiceListener {
        public void onChosen(Room r);
    }
}
