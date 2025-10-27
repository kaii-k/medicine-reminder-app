package com.example.medicinereminderapp;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.Calendar;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String TAG = "ReminderReceiver";
    private static final String CHANNEL_ID = "med_reminder_channel";
    private static final int NOTIF_BASE = 2000;

    // Offsets for extra pending intents so they don't collide with the main alarm pending intent
    private static final int SNOOZE_OFFSET = 100000;
    private static final int MISSED_OFFSET = 200000;

    // Snooze window in minutes (user-friendly default)
    private static final int SNOOZE_MINUTES = 10;

    // How long to wait before marking as missed (minutes)
    private static final int MISSED_MINUTES = 30;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm received in ReminderReceiver");

        // --- Read extras (first thing) ---
        String name = intent.getStringExtra("medicineName");
        String dose = intent.getStringExtra("dose");
        String notes = intent.getStringExtra("notes");
        int requestCode = intent.getIntExtra("requestCode", 0);
        int hour = intent.getIntExtra("hour24", -1);
        int minute = intent.getIntExtra("minute", -1);

        if (name == null) name = "Medicine";
        Log.d(TAG, "Reminder for: " + name + " (reqCode=" + requestCode + ")");

        // Ensure notification channel exists (Android O+)
        createNotificationChannel(context);

        // Intent to open AlarmActivity when user taps the notification (or full-screen intent triggers)
        Intent alarmActivityIntent = new Intent(context, AlarmActivity.class);
        alarmActivityIntent.putExtra("medicineName", name);
        alarmActivityIntent.putExtra("dose", dose);
        alarmActivityIntent.putExtra("notes", notes);
        alarmActivityIntent.putExtra("requestCode", requestCode);
        alarmActivityIntent.putExtra("hour24", hour);
        alarmActivityIntent.putExtra("minute", minute);
        alarmActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                context,
                requestCode + 5000,
                alarmActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Action: DONE -> handled by AlarmActionReceiver
        Intent doneIntent = new Intent(context, AlarmActionReceiver.class);
        doneIntent.setAction(AlarmActionReceiver.ACTION_DONE);
        doneIntent.putExtra("requestCode", requestCode);
        doneIntent.putExtra("medicineName", name);
        doneIntent.putExtra("dose", dose);

        PendingIntent donePI = PendingIntent.getBroadcast(
                context,
                requestCode + 1,
                doneIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Action: SNOOZE -> handled by AlarmActionReceiver
        Intent snoozeIntent = new Intent(context, AlarmActionReceiver.class);
        snoozeIntent.setAction(AlarmActionReceiver.ACTION_SNOOZE);
        snoozeIntent.putExtra("requestCode", requestCode);
        snoozeIntent.putExtra("medicineName", name);
        snoozeIntent.putExtra("dose", dose);
        snoozeIntent.putExtra("hour24", hour);
        snoozeIntent.putExtra("minute", minute);

        PendingIntent snoozePI = PendingIntent.getBroadcast(
                context,
                requestCode + 2,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build notification with full-screen intent
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Time to take: " + name)
                .setContentText(dose == null ? "Take your medicine now" : dose)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(contentPendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Done", donePI)
                .addAction(android.R.drawable.ic_lock_idle_alarm, "Snooze", snoozePI)
                .setFullScreenIntent(contentPendingIntent, true)
                .setDefaults(Notification.DEFAULT_ALL);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            try {
                nm.notify(NOTIF_BASE + requestCode, builder.build());
                Log.d(TAG, "Notification posted for reqCode=" + requestCode);
            } catch (Exception e) {
                Log.e(TAG, "Failed to post notification: " + e.getMessage(), e);
            }
        } else {
            Log.e(TAG, "NotificationManager is null");
        }

        // Try to start AlarmActivity directly as a fallback to ensure UI appears
        try {
            Intent startIntent = new Intent(context, AlarmActivity.class);
            startIntent.putExtra("name", name);
            startIntent.putExtra("dose", dose);
            startIntent.putExtra("notes", notes);
            startIntent.putExtra("requestCode", requestCode);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(startIntent);
            Log.d(TAG, "Started AlarmActivity manually from ReminderReceiver");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start AlarmActivity from receiver: " + e.getMessage(), e);
        }

        // Schedule a missed-check in MISSED_MINUTES minutes (so if user does nothing we mark missed)
        scheduleMissCheck(context, requestCode, name, MISSED_MINUTES);

        // Reschedule next day's regular alarm (if hour/min were provided)
        try {
            if (hour >= 0 && minute >= 0) {
                AlarmScheduler.scheduleExactAlarm(context, requestCode, name, dose, notes, hour, minute);
                Log.d(TAG, "Rescheduled next day's alarm for " + name + " at " + hour + ":" + minute);
            } else {
                Log.d(TAG, "Hour/minute not provided; skip auto-reschedule.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while rescheduling next alarm: " + e.getMessage(), e);
        }
    }

    private void scheduleMissCheck(Context ctx, int requestCode, String medName, int minutesLater) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        Intent missIntent = new Intent(ctx, AlarmActionReceiver.class);
        missIntent.setAction(AlarmActionReceiver.ACTION_MISSED_CHECK);
        missIntent.putExtra("requestCode", requestCode);
        missIntent.putExtra("medicineName", medName);

        PendingIntent pi = PendingIntent.getBroadcast(ctx, requestCode + MISSED_OFFSET, missIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        long triggerAt = System.currentTimeMillis() + minutesLater * 60L * 1000L;
        try {
            if (am != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                } else {
                    am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                }
                Log.d(TAG, "Miss-check scheduled in " + minutesLater + " minutes for reqCode=" + requestCode);
            } else {
                Log.e(TAG, "AlarmManager is null - cannot schedule miss-check");
            }
        } catch (SecurityException se) {
            Log.e(TAG, "SecurityException scheduling miss-check: " + se.getMessage(), se);
        }
    }

    private void createNotificationChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            NotificationChannel channel = nm.getNotificationChannel(CHANNEL_ID);
            if (channel == null) {
                channel = new NotificationChannel(CHANNEL_ID, "Medicine Reminders", NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription("Reminder alerts for medicines");
                nm.createNotificationChannel(channel);
            }
        }
    }
}
