package com.example.medicinereminderapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_POST_NOTIFICATIONS = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestNotificationPermissionIfNeeded();

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

    // On Android 13+ (API 33+) POST_NOTIFICATIONS must be granted at runtime,
    // otherwise reminder notifications are silently dropped by the system.
    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_POST_NOTIFICATIONS);
            }
        }
    }
}
