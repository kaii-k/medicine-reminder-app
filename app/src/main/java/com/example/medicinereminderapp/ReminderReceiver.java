package com.example.medicinereminderapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("ReminderReceiver", "=== 🎉 ALARM TRIGGERED! ===");
        Toast.makeText(context, "ALARM TRIGGERED!", Toast.LENGTH_LONG).show();

        String name = intent.getStringExtra("name");
        String dose = intent.getStringExtra("dose");
        String notes = intent.getStringExtra("notes");

        Log.d("ReminderReceiver", "Medicine: " + name);

        // Start AlarmActivity
        Intent alarmIntent = new Intent(context, AlarmActivity.class);
        alarmIntent.putExtra("name", name);
        alarmIntent.putExtra("dose", dose);
        alarmIntent.putExtra("notes", notes);
        alarmIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        Log.d("ReminderReceiver", "Starting AlarmActivity...");
        context.startActivity(alarmIntent);
    }
}