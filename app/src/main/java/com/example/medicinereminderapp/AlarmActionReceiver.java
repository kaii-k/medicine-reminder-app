package com.example.medicinereminderapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Objects;

/**
 * Central receiver for alarm actions:
 *  - ACTION_DONE        : user marked reminder as taken (from notification action)
 *  - ACTION_SNOOZE      : user snoozed the reminder
 *  - ACTION_MISSED_CHECK: scheduled follow-up that marks dose missed if not done
 *
 * It records entries to your dose_history table using DatabaseHelper.recordDoseHistory(...)
 * and broadcasts "com.example.medicinereminderapp.REPORT_UPDATED" for live UI updates.
 */
public class AlarmActionReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmActionReceiver";

    public static final String ACTION_SNOOZE = "com.example.medicinereminderapp.ACTION_SNOOZE";
    public static final String ACTION_DONE = "com.example.medicinereminderapp.ACTION_DONE";
    public static final String ACTION_MISSED_CHECK = "com.example.medicinereminderapp.ACTION_MISSED_CHECK";

    // Offsets used to create unique pending-intent ids for snooze/miss-check derived from original requestCode
    private static final int SNOOZE_OFFSET = 100000;
    private static final int MISSED_OFFSET = 200000;

    // Default snooze (minutes) if you don't override
    private static final int DEFAULT_SNOOZE_MINUTES = 10;

    // Notification id base used in your app (you used 2000 + requestCode when cancelling earlier) - keep consistent
    private static final int NOTIF_ID_BASE = 2000;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (action == null) return;

        Log.d(TAG, "onReceive action=" + action);

        // Common extras used across actions
        int requestCode = intent.getIntExtra("requestCode", -1);
        int medicineId = intent.getIntExtra("medicineId", -1);
        String medName = intent.getStringExtra("medicineName");
        String dose = intent.getStringExtra("dose");
        long scheduledTime = intent.getLongExtra("scheduledTime", System.currentTimeMillis());

        DatabaseHelper db = null;
        try {
            db = new DatabaseHelper(context);

            switch (action) {
                case ACTION_DONE:
                    handleDone(context, db, requestCode, medicineId, medName, dose, scheduledTime);
                    break;

                case ACTION_SNOOZE:
                    handleSnooze(context, db, requestCode, medicineId, medName, dose, scheduledTime);
                    break;

                case ACTION_MISSED_CHECK:
                    handleMissed(context, db, requestCode, medicineId, medName, dose, scheduledTime);
                    break;

                default:
                    Log.w(TAG, "Unhandled action: " + action);
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in AlarmActionReceiver: " + e.getMessage(), e);
        } finally {
            if (db != null) {
                try { db.close(); } catch (Exception ignored) {}
            }
        }
    }

    // User pressed Done on the notification -> record 'taken', cancel missed-check, cancel notification
    private void handleDone(Context context, DatabaseHelper db, int requestCode, int medicineId,
                            String medName, String dose, long scheduledTime) {
        Log.d(TAG, "handleDone req=" + requestCode + " medId=" + medicineId);
        try {
            long takenTime = System.currentTimeMillis();
            int idForDb = medicineId >= 0 ? medicineId : requestCode; // fallback to requestCode if medicineId missing
            db.recordDoseHistory(idForDb, medName == null ? "" : medName, dose == null ? "" : dose,
                    scheduledTime, takenTime, "taken");

            // broadcast live update for report UI
            sendReportUpdatedBroadcast(context);

        } catch (Exception e) {
            Log.e(TAG, "Error recording DONE: " + e.getMessage(), e);
        }

        // cancel any pending missed-check for this alarm
        cancelPendingMissCheck(context, requestCode);

        // cancel actual notification
        cancelNotification(context, requestCode);
    }

    // User pressed Snooze -> cancel current missed-check + notification and schedule a one-shot snooze alarm
    private void handleSnooze(Context context, DatabaseHelper db, int requestCode, int medicineId,
                              String medName, String dose, long scheduledTime) {
        Log.d(TAG, "handleSnooze req=" + requestCode);
        // cancel pending missed-check (we'll reschedule one for snooze if desired)
        cancelPendingMissCheck(context, requestCode);
        cancelNotification(context, requestCode);

        // schedule a one-shot snooze alarm (will re-trigger ReminderReceiver)
        scheduleSnooze(context, requestCode, medicineId, medName, dose, DEFAULT_SNOOZE_MINUTES);
    }

    // Missed check fired -> record 'missed', cancel notification
    private void handleMissed(Context context, DatabaseHelper db, int requestCode, int medicineId,
                              String medName, String dose, long scheduledTime) {
        Log.d(TAG, "handleMissed req=" + requestCode + " medId=" + medicineId);
        try {
            int idForDb = medicineId >= 0 ? medicineId : requestCode;
            // taken_time = 0 indicates missed (as your schema used)
            db.recordDoseHistory(idForDb, medName == null ? "" : medName, dose == null ? "" : dose,
                    scheduledTime, 0L, "missed");

            // broadcast live update for report UI
            sendReportUpdatedBroadcast(context);
        } catch (Exception e) {
            Log.e(TAG, "Error recording MISSED: " + e.getMessage(), e);
        }

        // cancel any notification still showing
        cancelNotification(context, requestCode);
    }

    // ------------ utility helpers ------------

    private void sendReportUpdatedBroadcast(Context context) {
        try {
            Intent update = new Intent("com.example.medicinereminderapp.REPORT_UPDATED");
            LocalBroadcastManager.getInstance(context).sendBroadcast(update);
            Log.d(TAG, "Broadcasted REPORT_UPDATED");
        } catch (Exception e) {
            Log.w(TAG, "Unable to send REPORT_UPDATED broadcast: " + e.getMessage());
        }
    }

    // Cancel a pending missed-check PendingIntent (created with originalReq + MISSED_OFFSET)
    private void cancelPendingMissCheck(Context ctx, int originalReqCode) {
        try {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            Intent missIntent = new Intent(ctx, AlarmActionReceiver.class);
            missIntent.setAction(ACTION_MISSED_CHECK);
            // match extras if you used extras when creating the pending intent earlier
            PendingIntent pi = PendingIntent.getBroadcast(ctx, originalReqCode + MISSED_OFFSET, missIntent,
                    PendingIntent.FLAG_NO_CREATE | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));
            if (pi != null && am != null) {
                am.cancel(pi);
                pi.cancel();
                Log.d(TAG, "Cancelled miss-check pending for req=" + originalReqCode);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to cancel pending miss-check: " + e.getMessage());
        }
    }

    // Cancel notification by id (your app uses NOTIF_ID_BASE + requestCode)
    private void cancelNotification(Context ctx, int requestCode) {
        int notifId = NOTIF_ID_BASE + Math.max(0, requestCode);
        try {
            NotificationManagerCompat.from(ctx).cancel(notifId);
            Log.d(TAG, "Cancelled notification id=" + notifId);
        } catch (Exception e) {
            Log.w(TAG, "Failed to cancel notification: " + e.getMessage());
        }
    }

    // Schedule one-shot snooze (uses ReminderReceiver to re-show the alarm)
    private void scheduleSnooze(Context ctx, int originalReqCode, int medicineId, String medName, String dose, int minutes) {
        try {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            long triggerAt = System.currentTimeMillis() + minutes * 60L * 1000L;

            // Create intent that ReminderReceiver expects (reuse your existing ReminderReceiver)
            Intent intent = new Intent(ctx, ReminderReceiver.class);
            intent.putExtra("requestCode", originalReqCode);
            intent.putExtra("medicineId", medicineId);
            intent.putExtra("medicineName", medName);
            intent.putExtra("dose", dose);
            intent.putExtra("scheduledTime", triggerAt); // snooze scheduledTime is the new trigger

            int snoozePendingId = originalReqCode + SNOOZE_OFFSET;
            PendingIntent pi = PendingIntent.getBroadcast(ctx, snoozePendingId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));

            if (am != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                } else {
                    am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                }
                Log.d(TAG, "Snooze scheduled for req=" + originalReqCode + " pendingId=" + snoozePendingId + " at " + triggerAt);
            } else {
                Log.w(TAG, "AlarmManager null - cannot schedule snooze");
            }
        } catch (SecurityException se) {
            Log.e(TAG, "SecurityException scheduling snooze: " + se.getMessage(), se);
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling snooze: " + e.getMessage(), e);
        }
    }
}
