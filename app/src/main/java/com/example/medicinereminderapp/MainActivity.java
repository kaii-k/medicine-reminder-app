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
    Button addBtn, viewBtn, testBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        addBtn = findViewById(R.id.addBtn);
        viewBtn = findViewById(R.id.viewBtn);
        testBtn = findViewById(R.id.testBtn);

        addBtn.setOnClickListener(v -> startActivity(new Intent(this, AddMedicineActivity.class)));
        viewBtn.setOnClickListener(v -> startActivity(new Intent(this, MedicineListActivity.class)));

        // Test 1-minute alarm
        testBtn.setOnClickListener(v -> testSimpleAlarm());
    }

    private void testSimpleAlarm() {
        Log.d("AlarmTest", "=== SETTING 1-MINUTE TEST ALARM ===");

        int requestCode = (int) System.currentTimeMillis();
        String medicineName = "Test Medicine";
        String dose = "1 tablet";
        String notes = "Test alarm";

        Log.d("AlarmTest", "Will ring in 1 minute");

        // Schedule the alarm - using 0,0 for hour/minute but the method uses 1 minute from now
        AlarmScheduler.scheduleDailyExact(this, requestCode, medicineName, dose, notes, 0, 0);

        Toast.makeText(this, "Alarm set for 1 minute from now", Toast.LENGTH_LONG).show();
    }

    private void testAlarmDirectly() {
        Log.d("AlarmTest", "=== TESTING DIRECT BROADCAST ===");

        // Test if receiver works at all
        Intent testIntent = new Intent(this, ReminderReceiver.class);
        testIntent.putExtra("name", "Direct Test");
        testIntent.putExtra("dose", "1 tablet");
        testIntent.putExtra("notes", "Direct trigger");

        Log.d("AlarmTest", "Sending broadcast directly...");

        // Send broadcast directly - this should work immediately
        sendBroadcast(testIntent);

        Toast.makeText(this, "Direct broadcast sent", Toast.LENGTH_SHORT).show();
    }
}