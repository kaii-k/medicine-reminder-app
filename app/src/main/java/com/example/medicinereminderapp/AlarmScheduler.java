package com.example.medicinereminderapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import java.util.Calendar;

public class AlarmScheduler {

    public static void scheduleDailyExact(Context ctx, int requestCode,
                                          String name, String dose, String notes,
                                          int hour24, int minute) {

        // Set alarm for 1 minute from now for testing
        long triggerAt = System.currentTimeMillis() + (60 * 1000); // 1 minute from now

        Log.d("AlarmScheduler", "=== SETTING SIMPLE ALARM ===");
        Log.d("AlarmScheduler", "Will trigger in 1 minute");
        Log.d("AlarmScheduler", "Current: " + System.currentTimeMillis());
        Log.d("AlarmScheduler", "Trigger: " + triggerAt);

        Intent i = new Intent(ctx, ReminderReceiver.class);
        i.putExtra("name", name);
        i.putExtra("dose", dose);
        i.putExtra("notes", notes);
        i.putExtra("requestCode", requestCode);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pi = PendingIntent.getBroadcast(ctx, requestCode, i, flags);

        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);

        if (am != null) {
            Log.d("AlarmScheduler", "AlarmManager found, using simple set()...");

            // Use the basic set() method which should work on all devices
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi);

            Log.d("AlarmScheduler", "✅ ALARM SET WITH set() METHOD!");
        } else {
            Log.d("AlarmScheduler", "❌ AlarmManager is null!");
        }
    }

    public static void rescheduleNextDay(Context ctx, int requestCode, Intent prevIntent) {
        // Not used for testing
    }

    public static void cancelAlarm(Context ctx, int requestCode) {
        // Not used for testing
    }
}