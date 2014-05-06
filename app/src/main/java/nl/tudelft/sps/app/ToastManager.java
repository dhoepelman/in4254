package nl.tudelft.sps.app;

import android.content.Context;
import android.widget.Toast;

import java.util.concurrent.atomic.AtomicReference;

public class ToastManager {

    private final Context context;

    private final AtomicReference<Toast> currentToast = new AtomicReference<Toast>(null);

    public ToastManager(Context context) {
        this.context = context;
    }

    public void showText(CharSequence message, int duration) {
        final Toast newToast = Toast.makeText(context, message, duration);

        // Hide previous toast
        final Toast oldToast = currentToast.getAndSet(newToast);
        if (oldToast != null) {
            oldToast.cancel();
        }

        // Show new toast
        newToast.show();
    }

}
