package com.example.medicinereminderapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        DatabaseHelper db = new DatabaseHelper(context);
        Cursor c = db.getAllMedicines();
        while (c.moveToNext()) {
            String name = c.getString(c.getColumnIndexOrThrow("name"));
            String dose = c.getString(c.getColumnIndexOrThrow("dose"));
            String notes = c.getString(c.getColumnIndexOrThrow("notes"));
            String time = c.getString(c.getColumnIndexOrThrow("time"));
            if (time == null) continue;
            String[] parts = time.split(":");
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            int req = (name + time).hashCode();
            AlarmScheduler.scheduleDailyExact(context, req, name, dose, notes, h, m);
        }
        c.close();
    }
}