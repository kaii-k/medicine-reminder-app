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
            Intent intent = new Intent(MainActivity.this, ReportsActivity.class);
            startActivity(intent);
        });
    }
}