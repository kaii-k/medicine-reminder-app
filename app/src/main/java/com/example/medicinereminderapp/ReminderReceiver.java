package com.example.medicinereminderapp;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;

/**
 * ReminderReceiver: shows a high-priority notification with a fullScreenIntent
 * so AlarmActivity can be launched even when the app is backgrounded/locked.
 */
public class ReminderReceiver extends BroadcastReceiver {
    private static final String TAG = "ReminderReceiver";

    public static final String EXTRA_REQUEST_CODE = "requestCode";
    public static final String EXTRA_MED_ID = "medicineId";
    public static final String EXTRA_MED_NAME = "medicineName";
    public static final String EXTRA_DOSE = "dose";
    public static final String EXTRA_SCHEDULED_TIME = "scheduledTime";

    // Notification / alarm constants (keep in sync with AlarmActionReceiver)
    private static final int NOTIF_BASE = 2000;
    private static final int MISSED_OFFSET = 200000;
    public static final int SNOOZE_OFFSET = 100000;

    // Missed-check delay (minutes) after scheduled time
    private static final int DEFAULT_MISSED_DELAY_MIN = 5;

    // Notification channel
    private static final String CHANNEL_ID = "med_reminder_channel";
    private static final String CHANNEL_NAME = "Medicine reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        int requestCode = intent.getIntExtra(EXTRA_REQUEST_CODE, -1);
        int medId = intent.getIntExtra(EXTRA_MED_ID, -1);
        String medName = intent.getStringExtra(EXTRA_MED_NAME);
        String dose = intent.getStringExtra(EXTRA_DOSE);
        long scheduledTime = intent.getLongExtra(EXTRA_SCHEDULED_TIME, System.currentTimeMillis());

        Log.d(TAG, "onReceive - reqCode=" + requestCode + " medId=" + medId + " medName=" + medName + " sched=" + scheduledTime);

        // 1) Create notification channel (safe to call on every run)
        createNotificationChannelIfNeeded(context);

        // 2) Build PendingIntents for actions: DONE and SNOOZE
        Intent doneIntent = new Intent(context, AlarmActionReceiver.class);
        doneIntent.setAction(AlarmActionReceiver.ACTION_DONE);
        doneIntent.putExtra("requestCode", requestCode);
        doneIntent.putExtra("medicineId", medId);
        doneIntent.putExtra("medicineName", medName);
        doneIntent.putExtra("dose", dose);
        doneIntent.putExtra("scheduledTime", scheduledTime);

        PendingIntent donePI = PendingIntent.getBroadcast(
                context,
                requestCode,
                doneIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent snoozeIntent = new Intent(context, AlarmActionReceiver.class);
        snoozeIntent.setAction(AlarmActionReceiver.ACTION_SNOOZE);
        snoozeIntent.putExtra("requestCode", requestCode);
        snoozeIntent.putExtra("medicineId", medId);
        snoozeIntent.putExtra("medicineName", medName);
        snoozeIntent.putExtra("dose", dose);
        snoozeIntent.putExtra("scheduledTime", scheduledTime);

        PendingIntent snoozePI = PendingIntent.getBroadcast(
                context,
                requestCode + SNOOZE_OFFSET,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 3) Build the full-screen intent to launch AlarmActivity
        Intent alarmActivityIntent = new Intent(context, AlarmActivity.class);
        alarmActivityIntent.putExtra(EXTRA_REQUEST_CODE, requestCode);
        alarmActivityIntent.putExtra(EXTRA_MED_ID, medId);
        alarmActivityIntent.putExtra(EXTRA_MED_NAME, medName);
        alarmActivityIntent.putExtra(EXTRA_DOSE, dose);
        alarmActivityIntent.putExtra(EXTRA_SCHEDULED_TIME, scheduledTime);

        // Required flags: allow starting activity from receiver and reuse if already open
        alarmActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                1_000_000 + Math.max(0, requestCode), // unique id for activity pending intent
                alarmActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 4) Build the notification (HIGH priority + fullScreenIntent)
        NotificationCompat.Builder nb = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // change to your icon if you prefer
                .setContentTitle(medName != null ? medName : "Medicine reminder")
                .setContentText(dose != null ? dose : "Time to take your medicine")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Done", donePI)
                .addAction(android.R.drawable.ic_menu_recent_history, "Snooze", snoozePI)
                .setFullScreenIntent(fullScreenPendingIntent, true); // <- shows activity on top for alarms

        // 5) Post notification
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        int notifId = NOTIF_BASE + (requestCode >= 0 ? requestCode : 0);
        if (nm != null) {
            try {
                nm.notify(notifId, nb.build());
            } catch (SecurityException se) {
                Log.e(TAG, "Notification error: " + se.getMessage(), se);
            }
        }

        // 6) Schedule missed-check alarm (fires after DEFAULT_MISSED_DELAY_MIN minutes)
        scheduleMissedCheck(context, requestCode, medId, medName, dose, scheduledTime, DEFAULT_MISSED_DELAY_MIN);
    }

    private void createNotificationChannelIfNeeded(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            NotificationChannel ch = nm.getNotificationChannel(CHANNEL_ID);
            if (ch == null) {
                ch = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
                ch.setDescription("Channel for medicine reminders");
                nm.createNotificationChannel(ch);
            }
        }
    }

    /**
     * Schedule a missed-check one-shot alarm that triggers AlarmActionReceiver.ACTION_MISSED_CHECK.
     * The PendingIntent uses requestCode + MISSED_OFFSET so AlarmActionReceiver can cancel it when needed.
     */
    private void scheduleMissedCheck(Context ctx, int requestCode, int medId, String medName, String dose,
                                     long scheduledTime, int minutesAfter) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            Log.e(TAG, "AlarmManager null - cannot schedule missed-check");
            return;
        }

        long triggerAt = System.currentTimeMillis() + minutesAfter * 60L * 1000L;
        // If you prefer to schedule relative to scheduledTime: uncomment next line and comment previous
        // long triggerAt = scheduledTime + minutesAfter * 60L * 1000L;

        Intent missIntent = new Intent(ctx, AlarmActionReceiver.class);
        missIntent.setAction(AlarmActionReceiver.ACTION_MISSED_CHECK);
        missIntent.putExtra("requestCode", requestCode);
        missIntent.putExtra("medicineId", medId);
        missIntent.putExtra("medicineName", medName);
        missIntent.putExtra("dose", dose);
        missIntent.putExtra("scheduledTime", scheduledTime);

        PendingIntent missPI = PendingIntent.getBroadcast(
                ctx,
                requestCode + MISSED_OFFSET,
                missIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, missPI);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, missPI);
            }
            Log.d(TAG, "Scheduled missed-check: reqCode=" + requestCode + " triggerAt=" + triggerAt);
        } catch (SecurityException se) {
            Log.e(TAG, "SecurityException scheduling missed-check: " + se.getMessage(), se);
        }
    }

    /**
     * Public helper to schedule an AlarmManager alarm that will be received by this receiver.
     *
     * @param ctx            Context
     * @param requestCode    Unique request code for PendingIntent (used to cancel/update)
     * @param medicineId     Business ID for the medicine (can be same as requestCode)
     * @param triggerAtMs    Epoch millis when alarm should fire
     * @param medicineName   Name to show in notification
     * @param dose           Dose text to show in notification
     */
    public static void scheduleReminder(Context ctx, int requestCode, int medicineId, long triggerAtMs,
                                        String medicineName, String dose) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            Log.e(TAG, "AlarmManager null - cannot schedule reminder");
            return;
        }

        Intent i = new Intent(ctx, ReminderReceiver.class);
        i.putExtra(EXTRA_REQUEST_CODE, requestCode);
        i.putExtra(EXTRA_MED_ID, medicineId);
        i.putExtra(EXTRA_MED_NAME, medicineName);
        i.putExtra(EXTRA_DOSE, dose);
        i.putExtra(EXTRA_SCHEDULED_TIME, triggerAtMs);

        PendingIntent pi = PendingIntent.getBroadcast(
                ctx,
                requestCode,
                i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMs, pi);
            }
            Log.d(TAG, "Scheduled reminder: reqCode=" + requestCode + " at " + triggerAtMs);
        } catch (SecurityException se) {
            Log.e(TAG, "SecurityException scheduling reminder: " + se.getMessage(), se);
        }
    }
}
