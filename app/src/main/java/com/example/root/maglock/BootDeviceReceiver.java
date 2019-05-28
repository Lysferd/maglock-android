package com.example.root.maglock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootDeviceReceiver extends BroadcastReceiver {

    private static final String TAG_BOOT_BROADCAST_RECEIVER = "BOOT_BROADCAST_RECEIVER";
    private final String TAG = "Maglock." + this.getClass().getSimpleName();
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "BootDeviceReceiver onReceive, action is " + action);
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            // Start Service, by alarm(recommended?) or directly(not recommended?)
        }
    }
}
