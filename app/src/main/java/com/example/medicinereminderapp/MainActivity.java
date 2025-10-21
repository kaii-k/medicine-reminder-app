package com.example.medicinereminderapp;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    Button addBtn, viewBtn, testBtn; // Added test button

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        addBtn = findViewById(R.id.addBtn);
        viewBtn = findViewById(R.id.viewBtn);
        testBtn = findViewById(R.id.testBtn); // Make sure to add this button to your layout

        addBtn.setOnClickListener(v -> startActivity(new Intent(this, AddMedicineActivity.class)));
        viewBtn.setOnClickListener(v -> startActivity(new Intent(this, MedicineListActivity.class)));

        // Test alarm button - use debug version
        testBtn.setOnClickListener(v -> debugAlarmSystem());
    }

    private void debugAlarmSystem() {
        Log.d("AlarmDebug", "=== DEBUGGING ALARM SYSTEM ===");

        // Check if ReminderReceiver is properly registered
        Log.d("AlarmDebug", "1. Checking ReminderReceiver...");

        // Test if we can create a PendingIntent
        Intent testIntent = new Intent(this, ReminderReceiver.class);
        testIntent.putExtra("debug", true);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                999,
                testIntent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );

        if (pendingIntent != null) {
            Log.d("AlarmDebug", "✅ ReminderReceiver is accessible");
            pendingIntent.cancel();
        } else {
            Log.d("AlarmDebug", "❌ ReminderReceiver cannot be accessed");
        }

        // Test immediate alarm
        testAlarmNow();
    }

    private void testAlarmNow() {
        Log.d("AlarmTest", "=== TESTING ALARM NOW ===");

        // Set alarm for 10 seconds from now (not 1 minute)
        long triggerTime = System.currentTimeMillis() + (10 * 1000); // 10 seconds

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(triggerTime);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);

        int requestCode = 99999; // Fixed request code for testing
        String medicineName = "Test Medicine";
        String dose = "1 tablet";
        String notes = "Test alarm";

        Log.d("AlarmTest", "Setting alarm for: " + hour + ":" + minute + ":" + second);
        Log.d("AlarmTest", "Current time: " + Calendar.getInstance().getTime());
        Log.d("AlarmTest", "Alarm will trigger in 10 seconds");

        // Schedule the alarm
        AlarmScheduler.scheduleDailyExact(this, requestCode, medicineName, dose, notes, hour, minute);

        Toast.makeText(this, "Alarm set for 10 seconds from now", Toast.LENGTH_LONG).show();

        // Check if scheduled
        checkIfAlarmScheduled(requestCode);
    }

    // Method to verify alarm was scheduled
    private void checkIfAlarmScheduled(int requestCode) {
        Intent checkIntent = new Intent(this, ReminderReceiver.class);
        PendingIntent testPendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode,
                checkIntent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );

        if (testPendingIntent != null) {
            Log.d("AlarmTest", "✅ Alarm is SCHEDULED in system");
            Toast.makeText(this, "Alarm scheduled successfully", Toast.LENGTH_SHORT).show();
        } else {
            Log.d("AlarmTest", "❌ Alarm is NOT scheduled in system");
            Toast.makeText(this, "Alarm NOT scheduled - check permissions", Toast.LENGTH_LONG).show();
        }
    }
}