package nl.tudelft.sps.app;

import android.content.Context;
import android.widget.Toast;

public class ToastManager {

    private final Context context;

    private Toast currentToast = null;

    public ToastManager(Context context) {
        this.context = context;
    }

    public synchronized void showText(CharSequence message, int duration) {
        final Toast newToast = Toast.makeText(context, message, duration);

        // Hide previous toast
        if (currentToast != null) {
            currentToast.cancel();
        }

        // Show new toast
        newToast.show();

        currentToast = newToast;
    }

}
