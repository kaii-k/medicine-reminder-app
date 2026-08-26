package com.example.medicinereminderapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ask for everything the alarm/notification pipeline needs up front,
        // instead of only when adding a medicine - a user who denies/misses
        // these never gets asked again otherwise, and reminders silently
        // stop working.
        AlarmPermissions.requestNotificationPermissionIfNeeded(this);
        AlarmPermissions.ensureExactAlarmsAllowed(this);
        AlarmPermissions.ensureFullScreenIntentAllowed(this);

        Button addMedicineBtn = findViewById(R.id.addMedicineBtn);
        Button viewMedicinesBtn = findViewById(R.id.viewMedicinesBtn);
        Button reportsBtn = findViewById(R.id.reportsBtn);

        // Add Medicine Button
        addMedicineBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddMedicineActivity.class);
            startActivity(intent);
        });

        // View All Medicines Button
        viewMedicinesBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MedicineListActivity.class);
            startActivity(intent);
        });

        // Reports Button
        reportsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ReportActivity.class);
            startActivity(intent);
        });

    }
}
