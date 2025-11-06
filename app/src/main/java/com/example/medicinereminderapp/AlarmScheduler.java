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
     * Schedule ONE exact alarm for the given hour:minute using a stable numeric requestCode (medicineId).
     *
     * @param context      app context
     * @param medicineId   stable numeric id (use DB row id)
     * @param medicineName visible label in notification
     * @param dose         dose text
     * @param notes        extra notes
     * @param hour24       hour of day (0-23)
     * @param minute       minute (0-59)
     */
    public static void scheduleExactAlarm(Context context,
                                          int medicineId,
                                          String medicineName,
                                          String dose,
                                          String notes,
                                          int hour24,
                                          int minute) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null. Cannot schedule alarm.");
            return;
        }

        // Use medicineId as stable requestCode
        int requestCode = medicineId;

        // Build the intent using the same extras keys ReminderReceiver expects
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra(ReminderReceiver.EXTRA_REQUEST_CODE, requestCode);
        intent.putExtra(ReminderReceiver.EXTRA_MED_ID, medicineId);
        intent.putExtra(ReminderReceiver.EXTRA_MED_NAME, medicineName);
        intent.putExtra(ReminderReceiver.EXTRA_DOSE, dose);
        // Put scheduled time later after we compute it
        // Also include hour/minute if you want ReminderReceiver to reschedule next day
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

        Log.d(TAG, "Scheduling alarm (medicineId=" + medicineId + ") for: " + calendar.getTime());

        // For API 31+ check if exact alarms allowed. Caller should prompt user if needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Log.w(TAG, "Exact alarms not allowed on this device for our app. Scheduling fallback inexact alarm.");
                    try {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
                        Log.d(TAG, "Fallback inexact alarm scheduled (may be delayed).");
                    } catch (SecurityException se) {
                        Log.e(TAG, "SecurityException scheduling fallback inexact alarm: " + se.getMessage(), se);
                    }
                    return;
                }
            } catch (SecurityException se) {
                Log.e(TAG, "SecurityException calling canScheduleExactAlarms(): " + se.getMessage(), se);
                // fall through to fallback scheduling below
            }
        }

        // Use exact scheduling API where available
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
            // fallback to inexact
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
                Log.d(TAG, "Fallback inexact alarm scheduled after SecurityException.");
            } catch (Exception e) {
                Log.e(TAG, "Failed fallback scheduling after SecurityException: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Convenience wrapper used by previous code
     */
    public static void scheduleDailyExact(Context context,
                                          int requestCode,
                                          String medicineName,
                                          String dose,
                                          String notes,
                                          int hour24,
                                          int minute) {
        // treat requestCode as medicineId here for backward compatibility
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
     * Make a stable positive request code (if needed).
     */
    public static int makeRequestCode(String medicineName, int hour24, int minute) {
        return Math.abs(Objects.hash(medicineName, hour24, minute));
    }
}
