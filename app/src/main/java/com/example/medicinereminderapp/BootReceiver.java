package com.example.medicinereminderapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import java.util.Calendar;

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

        DatabaseHelper db = null;
        Cursor c = null;

        try {
            db = new DatabaseHelper(context);
            c = db.getAllMedicines();

            while (c != null && c.moveToNext()) {
                // Use the numeric id from DB as request code (more robust than hashCode)
                int id = c.getInt(c.getColumnIndexOrThrow("id"));
                String name = c.getString(c.getColumnIndexOrThrow("name"));
                String dose = c.getString(c.getColumnIndexOrThrow("dose"));
                String notes = c.getString(c.getColumnIndexOrThrow("notes"));
                String time = c.getString(c.getColumnIndexOrThrow("time"));
                long endDate = 0;
                try {
                    endDate = c.getLong(c.getColumnIndexOrThrow("end_date"));
                } catch (Exception ignore) {}

                // Skip if ended (end_date stored as epoch millis > 0)
                if (endDate > 0 && System.currentTimeMillis() > endDate) {
                    Log.i(TAG, "Skipping ended medicine id=" + id + " name=" + name);
                    continue;
                }

                if (time == null || !time.contains(":")) {
                    Log.w(TAG, "Skipping medicine id=" + id + " due to invalid time: " + time);
                    continue;
                }

                String[] parts = time.split(":");
                int h, m;
                try {
                    h = Integer.parseInt(parts[0]);
                    m = Integer.parseInt(parts[1]);
                } catch (Exception ex) {
                    Log.w(TAG, "Skipping medicine id=" + id + " due to parse error: " + time, ex);
                    continue;
                }

                // Compute next trigger time (today at h:m or tomorrow if already passed)
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                cal.set(Calendar.HOUR_OF_DAY, h);
                cal.set(Calendar.MINUTE, m);

                if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
                    // already passed today -> schedule for tomorrow
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                }

                long triggerAt = cal.getTimeInMillis();

                // If your app supports repeat_type / selected_days, you should compute the next occurrence accordingly.
                // Here we reschedule a daily alarm at the stored time.
                Log.i(TAG, "Rescheduling id=" + id + " name=" + name + " at " + cal.getTime());

                // IMPORTANT: AlarmScheduler.scheduleDailyExact must use AlarmManager.setExactAndAllowWhileIdle and a broadcast PendingIntent
                AlarmScheduler.scheduleDailyExact(context, id, name, dose, notes, h, m);
                // OR if you have a scheduleReminder with a trigger timestamp:
                // ReminderReceiver.scheduleReminder(context, id, id, triggerAt, name, dose);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error rescheduling alarms on boot", e);
        } finally {
            if (c != null) try { c.close(); } catch (Exception ignored) {}
            if (db != null) try { db.close(); } catch (Exception ignored) {}
        }
    }
}
