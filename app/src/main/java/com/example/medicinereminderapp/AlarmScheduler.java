package com.example.medicinereminderapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.Calendar;

public class AlarmScheduler {

    public static void scheduleDailyExact(Context ctx, int requestCode,
                                          String name, String dose, String notes,
                                          int hour24, int minute) {

        long triggerAt = nextTriggerMillis(hour24, minute);

        Intent i = new Intent(ctx, ReminderReceiver.class);
        i.putExtra("name", name);
        i.putExtra("dose", dose);
        i.putExtra("notes", notes);
        i.putExtra("hour", hour24);
        i.putExtra("minute", minute);

        PendingIntent pi = PendingIntent.getBroadcast(
                ctx, requestCode, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }

    public static void rescheduleNextDay(Context ctx, int requestCode, Intent prevIntent) {
        int hour = prevIntent.getIntExtra("hour", 9);
        int minute = prevIntent.getIntExtra("minute", 0);
        String name = prevIntent.getStringExtra("name");
        String dose = prevIntent.getStringExtra("dose");
        String notes = prevIntent.getStringExtra("notes");
        scheduleDailyExact(ctx, requestCode, name, dose, notes, hour, minute);
    }

    public static long nextTriggerMillis(int hour24, int minute) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.set(Calendar.HOUR_OF_DAY, hour24);
        c.set(Calendar.MINUTE, minute);
        if (c.getTimeInMillis() <= System.currentTimeMillis()) {
            c.add(Calendar.DATE, 1);
        }
        return c.getTimeInMillis();
    }
}