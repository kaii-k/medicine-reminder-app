package com.example.medicinereminderapp;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;

/**
 * Handles Done / Snooze / Missed actions for medicine reminders.
 */
public class AlarmActionReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmActionReceiver";

    public static final String ACTION_SNOOZE = "com.example.medicinereminderapp.ACTION_SNOOZE";
    public static final String ACTION_DONE = "com.example.medicinereminderapp.ACTION_DONE";
    public static final String ACTION_MISSED_CHECK = "com.example.medicinereminderapp.ACTION_MISSED_CHECK";

    // Offsets must match ReminderReceiver
    private static final int SNOOZE_OFFSET = 100000;
    private static final int MISSED_OFFSET = 200000;

    private static final int SNOOZE_MINUTES = 10; // default snooze

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        Log.d(TAG, "Received action: " + action);

        int requestCode = intent.getIntExtra("requestCode", -1);
        String medName = intent.getStringExtra("medicineName");
        String dose = intent.getStringExtra("dose");

        DatabaseHelper db = null;
        try {
            db = new DatabaseHelper(context);
            switch (action) {
                case ACTION_DONE:
                    handleDone(context, db, requestCode, medName, dose);
                    break;

                case ACTION_SNOOZE:
                    handleSnooze(context, db, requestCode, medName, dose);
                    break;

                case ACTION_MISSED_CHECK:
                    handleMissed(context, db, requestCode, medName, dose);
                    break;

                default:
                    Log.w(TAG, "Unknown action: " + action);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in AlarmActionReceiver: " + e.getMessage(), e);
        } finally {
            if (db != null) {
                // close DB if your DatabaseHelper has a close method
                try { db.close(); } catch (Exception ignored) {}
            }
        }
    }

    private void handleDone(Context context, DatabaseHelper db, int requestCode, String medName, String dose) {
        Log.d(TAG, "Marking DONE for reqCode=" + requestCode);
        try {
            // recordDoseHistory(medId, name, dose, timestamp, skippedFlag, status)
            db.recordDoseHistory(requestCode, medName, dose, System.currentTimeMillis(), 0, "taken");
        } catch (Exception e) {
            Log.e(TAG, "DB error marking done: " + e.getMessage(), e);
        }

        // cancel any pending missed-check for this alarm
        cancelPendingMissCheck(context, requestCode);

        // cancel notification
        cancelNotification(context, requestCode);
    }

    private void handleSnooze(Context context, DatabaseHelper db, int requestCode, String medName, String dose) {
        Log.d(TAG, "Snooze requested for reqCode=" + requestCode);

        // Cancel missed-check and current notification
        cancelPendingMissCheck(context, requestCode);
        cancelNotification(context, requestCode);

        // Schedule snooze one-shot
        scheduleSnooze(context, requestCode, medName, dose, SNOOZE_MINUTES);
    }

    private void handleMissed(Context context, DatabaseHelper db, int requestCode, String medName, String dose) {
        Log.d(TAG, "Missed check for reqCode=" + requestCode);
        try {
            db.recordDoseHistory(requestCode, medName, dose, System.currentTimeMillis(), 1, "missed");
            Log.d(TAG, "Marked dose missed for reqCode=" + requestCode);
        } catch (Exception e) {
            Log.e(TAG, "DB error marking missed: " + e.getMessage(), e);
        }
        cancelNotification(context, requestCode);
    }

    // schedule a snooze one-shot alarm (does not replace the daily alarm)
    private void scheduleSnooze(Context ctx, int originalReqCode, String medName, String dose, int minutes) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        long triggerAt = System.currentTimeMillis() + minutes * 60L * 1000L;

        Intent intent = new Intent(ctx, ReminderReceiver.class);
        intent.putExtra("requestCode", originalReqCode);
        intent.putExtra("medicineName", medName);
        intent.putExtra("dose", dose);

        int snoozePendingId = originalReqCode + SNOOZE_OFFSET;
        PendingIntent pi = PendingIntent.getBroadcast(ctx, snoozePendingId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        try {
            if (am != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                } else {
                    am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                }
                Log.d(TAG, "Snooze scheduled for reqCode=" + originalReqCode + " in " + minutes + " mins (pendingId=" + snoozePendingId + ")");
            } else {
                Log.e(TAG, "AlarmManager is null - cannot schedule snooze");
            }
        } catch (SecurityException se) {
            Log.e(TAG, "SecurityException scheduling snooze: " + se.getMessage(), se);
        }
    }

    private void cancelPendingMissCheck(Context ctx, int originalReqCode) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(ctx, AlarmActionReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, originalReqCode + MISSED_OFFSET, i,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pi != null && am != null) {
            am.cancel(pi);
            pi.cancel();
            Log.d(TAG, "Cancelled miss-check pending for reqCode=" + originalReqCode);
        }
    }

    private void cancelNotification(Context ctx, int requestCode) {
        // Use NotificationManager to cancel by id (2000 + requestCode matches ReminderReceiver's notif id)
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            try {
                nm.cancel(2000 + requestCode);
            } catch (Exception e) {
                // fallback to compat
                NotificationManagerCompat.from(ctx).cancel(2000 + requestCode);
            }
        } else {
            NotificationManagerCompat.from(ctx).cancel(2000 + requestCode);
        }
    }
}
