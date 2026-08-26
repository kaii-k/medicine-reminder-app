package com.example.medicinereminderapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> runAlarmPermissionChain());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ask for everything the alarm/notification pipeline needs up front,
        // instead of only when adding a medicine - a user who denies/misses
        // these never gets asked again otherwise, and reminders silently
        // stop working. Each prompt only appears after the previous one is
        // dismissed, instead of stacking on top of each other.
        if (AlarmPermissions.isNotificationPermissionNeeded(this)) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
        } else {
            runAlarmPermissionChain();
        }

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

    private void runAlarmPermissionChain() {
        AlarmPermissions.ensureExactAlarmsAllowed(this,
                () -> AlarmPermissions.ensureFullScreenIntentAllowed(this, null));
    }
}
