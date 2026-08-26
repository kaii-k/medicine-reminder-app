package com.example.medicinereminderapp;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

/**
 * Re-schedules every active medicine's alarm from the current database state.
 * Used both after a device reboot (BootReceiver) and every time the app is
 * opened (MainActivity) - the latter means an alarm that was created or left
 * broken by an older/buggy build of the app "heals" itself the next time the
 * user opens the app, instead of requiring them to manually delete and
 * re-add every medicine after an update.
 *
 * Safe to call repeatedly: AlarmScheduler uses FLAG_UPDATE_CURRENT, so this
 * just refreshes each medicine's existing PendingIntent with current data.
 */
public class AlarmRescheduler {
    private static final String TAG = "AlarmRescheduler";

    public static void rescheduleAll(Context context) {
        DatabaseHelper db = null;
        Cursor c = null;

        try {
            db = new DatabaseHelper(context);
            c = db.getAllMedicines();

            while (c != null && c.moveToNext()) {
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

                // Stored time is a 12-hour display string like "8:30 AM" / "11:45 PM",
                // not 24-hour "HH:MM" - must be converted to 24-hour form.
                int h, m;
                try {
                    String[] timeAndPeriod = time.trim().split(" ");
                    String[] parts = timeAndPeriod[0].split(":");
                    int hour12 = Integer.parseInt(parts[0].trim());
                    m = Integer.parseInt(parts[1].trim());

                    boolean isAM = timeAndPeriod.length < 2 || timeAndPeriod[1].trim().equalsIgnoreCase("AM");
                    if (isAM) {
                        h = (hour12 == 12) ? 0 : hour12;
                    } else {
                        h = (hour12 == 12) ? 12 : hour12 + 12;
                    }
                } catch (Exception ex) {
                    Log.w(TAG, "Skipping medicine id=" + id + " due to parse error: " + time, ex);
                    continue;
                }

                Log.i(TAG, "Rescheduling id=" + id + " name=" + name + " at " + h + ":" + m);
                AlarmScheduler.scheduleExactAlarm(context, id, name, dose, notes, h, m);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error rescheduling alarms", e);
        } finally {
            if (c != null) try { c.close(); } catch (Exception ignored) {}
            if (db != null) try { db.close(); } catch (Exception ignored) {}
        }
    }
}
