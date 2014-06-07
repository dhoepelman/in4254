package nl.tudelft.sps.app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.EditText;

public class IPInputDialog extends DialogFragment {
    private final IPInputDialogListener callback;

    public IPInputDialog() {
        this(null);
    }

    public IPInputDialog(IPInputDialogListener callback) {
        this.callback = callback;
    }

    public Dialog onCreateDialog(Bundle saved) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        final View dialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_ip, null);
        dialogBuilder.setView(dialogView)
                .setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final String ip = ((EditText) dialogView.findViewById(R.id.val_ip)).getText().toString();
                        callback.onIPInput(IPInputDialog.this, ip);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
        ;
        return dialogBuilder.create();
    }

    public interface IPInputDialogListener {
        public void onIPInput(DialogFragment dialog, String IP);
    }
}
