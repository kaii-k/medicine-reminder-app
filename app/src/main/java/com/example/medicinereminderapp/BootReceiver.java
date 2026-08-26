package com.example.medicinereminderapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (action == null) return;

        // Only handle boot actions
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !"android.intent.action.LOCKED_BOOT_COMPLETED".equals(action)
                && !"android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            return;
        }

        Log.i(TAG, "Boot completed - rescheduling alarms");
        AlarmRescheduler.rescheduleAll(context);
    }
}
