package com.example.medicinereminderapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import java.util.Calendar;

public class AlarmScheduler {

    private static final String TAG = "AlarmScheduler";

    public static void scheduleDailyExact(Context context, int requestCode,
                                          String medicineName, String dose,
                                          String notes, int hour24, int minute) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, ReminderReceiver.class);
            intent.putExtra("medicineName", medicineName);
            intent.putExtra("dose", dose);
            intent.putExtra("notes", notes);
            intent.putExtra("requestCode", requestCode);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                    requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            // Create calendar with EXACT time
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour24);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            long alarmTime = calendar.getTimeInMillis();
            long currentTime = System.currentTimeMillis();

            Log.d(TAG, "Setting alarm for: " + hour24 + ":" + minute);
            Log.d(TAG, "Alarm time in millis: " + alarmTime);
            Log.d(TAG, "Current time in millis: " + currentTime);

            // If the time has passed for today, set for tomorrow
            if (alarmTime <= currentTime) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                alarmTime = calendar.getTimeInMillis();
                Log.d(TAG, "Alarm time passed, setting for tomorrow: " + calendar.getTime());
            }

            Log.d(TAG, "Final alarm time: " + calendar.getTime());

            if (alarmManager != null) {
                // For Android 6.0+ (Marshmallow), use setExactAndAllowWhileIdle
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                            alarmTime, pendingIntent);
                    Log.d(TAG, "Used setExactAndAllowWhileIdle");
                }
                // For Android 4.4+ (KitKat), use setExact
                else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP,
                            alarmTime, pendingIntent);
                    Log.d(TAG, "Used setExact");
                }
                // For older versions, use set
                else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP,
                            alarmTime, pendingIntent);
                    Log.d(TAG, "Used set");
                }

                Log.d(TAG, "Alarm scheduled successfully!");
                Log.d(TAG, "Will trigger at: " + calendar.getTime());
            } else {
                Log.e(TAG, "AlarmManager is null - cannot schedule alarm");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error scheduling alarm: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Method to test alarm in 1 minute (for debugging)
    public static void scheduleTestAlarm(Context context, String medicineName) {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, 1); // 1 minute from now
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            int hour24 = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            int requestCode = (medicineName + "test").hashCode();

            Log.d(TAG, "Setting TEST alarm for 1 minute from now: " + hour24 + ":" + minute);

            scheduleDailyExact(context, requestCode, medicineName, "Test Dose", "Test Alarm", hour24, minute);

        } catch (Exception e) {
            Log.e(TAG, "Error setting test alarm: " + e.getMessage());
        }
    }
}