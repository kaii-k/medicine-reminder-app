package com.example.medicinereminderapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;
import java.util.Objects;

public class AlarmScheduler {

    private static final String TAG = "AlarmScheduler";

    /**
     * Schedule ONE exact alarm for the given hour:minute.
     * Attaches hour/minute extras so ReminderReceiver can reschedule for next day.
     *
     * requestCode must be unique per reminder (use DB id or hash).
     */
    public static void scheduleExactAlarm(Context context,
                                          int requestCode,
                                          String medicineName,
                                          String dose,
                                          String notes,
                                          int hour24,
                                          int minute) {
        AlarmManager alarmManager = null;
        try {
            alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            Intent intent = new Intent(context, ReminderReceiver.class);
            intent.putExtra("medicineName", medicineName);
            intent.putExtra("dose", dose);
            intent.putExtra("notes", notes);
            intent.putExtra("requestCode", requestCode);
            intent.putExtra("hour24", hour24);
            intent.putExtra("minute", minute);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour24);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            long alarmTime = calendar.getTimeInMillis();
            long now = System.currentTimeMillis();
            if (alarmTime <= now) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                alarmTime = calendar.getTimeInMillis();
            }

            Log.d(TAG, "Scheduling alarm (" + requestCode + ") for: " + calendar.getTime());

            if (alarmManager == null) {
                Log.e(TAG, "AlarmManager is null. Cannot schedule alarm.");
                return;
            }

            // API 31+ : check if exact alarms allowed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    if (!alarmManager.canScheduleExactAlarms()) {
                        Log.w(TAG, "Exact alarms not allowed. Scheduling fallback inexact alarm.");
                        // Fallback to inexact alarm (may be delayed)
                        try {
                            alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
                            Log.d(TAG, "Fallback inexact alarm scheduled (may be delayed).");
                        } catch (SecurityException se) {
                            Log.e(TAG, "SecurityException scheduling fallback inexact alarm: " + se.getMessage(), se);
                        }
                        return;
                    }
                } catch (SecurityException se) {
                    Log.e(TAG, "SecurityException checking canScheduleExactAlarms(): " + se.getMessage(), se);
                    // Attempt fallback inexact scheduling
                    try {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
                        Log.d(TAG, "Fallback inexact alarm scheduled after exception.");
                    } catch (Exception e) {
                        Log.e(TAG, "Failed fallback scheduling after exception: " + e.getMessage(), e);
                    }
                    return;
                }
            }

            // If we reach here, exact alarms are allowed or API < 31
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
                    Log.d(TAG, "Exact alarm scheduled with setExactAndAllowWhileIdle.");
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
                    Log.d(TAG, "Exact alarm scheduled with setExact.");
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
                    Log.d(TAG, "Alarm scheduled with set (legacy).");
                }
            } catch (SecurityException se) {
                Log.e(TAG, "SecurityException scheduling exact alarm: " + se.getMessage(), se);
                // Try fallback to inexact
                try {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
                    Log.d(TAG, "Fallback inexact alarm scheduled after SecurityException.");
                } catch (Exception e) {
                    Log.e(TAG, "Failed fallback scheduling after SecurityException: " + e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Unexpected error scheduling alarm: " + e.getMessage(), e);
        }
    }

    /**
     * Backwards-compatible wrapper for older code that called scheduleDailyExact(...)
     */
    public static void scheduleDailyExact(Context context,
                                          int requestCode,
                                          String medicineName,
                                          String dose,
                                          String notes,
                                          int hour24,
                                          int minute) {
        scheduleExactAlarm(context, requestCode, medicineName, dose, notes, hour24, minute);
    }

    /**
     * Cancel an alarm using the same requestCode you used to schedule it.
     */
    public static void cancelAlarm(Context context, int requestCode) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
            Log.d(TAG, "Cancelled alarm with requestCode: " + requestCode);
        } else {
            Log.e(TAG, "AlarmManager is null. Cannot cancel alarm.");
        }
    }

    /**
     * Simple helper to create a consistent unique requestCode for a reminder.
     */
    public static int makeRequestCode(String medicineName, int hour24, int minute) {
        return Math.abs(Objects.hash(medicineName, hour24, minute));
    }
}
