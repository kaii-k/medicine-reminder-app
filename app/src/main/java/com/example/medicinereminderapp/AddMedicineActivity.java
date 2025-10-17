package com.example.medicinereminderapp;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class AddMedicineActivity extends AppCompatActivity {

    EditText nameInput, doseInput, timeInput, notesInput;
    Button saveBtn;
    DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_medicine);

        nameInput = findViewById(R.id.nameInput);
        doseInput = findViewById(R.id.doseInput);
        timeInput = findViewById(R.id.timeInput);
        notesInput = findViewById(R.id.notesInput);
        saveBtn = findViewById(R.id.saveBtn);
        db = new DatabaseHelper(this);

        saveBtn.setOnClickListener(v -> {
            String name = nameInput.getText().toString();
            String dose = doseInput.getText().toString();
            String time = timeInput.getText().toString();
            String notes = notesInput.getText().toString();

            if (name.isEmpty() || dose.isEmpty() || time.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Add this safety check before the time parsing
            if (!time.matches("\\d{1,2}:\\d{2}")) {
                Toast.makeText(this, "Please enter time in HH:MM format (e.g., 14:30)", Toast.LENGTH_SHORT).show();
                return;
            }

            String[] parts = time.split(":");
            if (parts.length != 2) {
                Toast.makeText(this, "Invalid time format", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int hour = Integer.parseInt(parts[0]);
                int minute = Integer.parseInt(parts[1]);

                // Validate time range
                if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                    Toast.makeText(this, "Please enter valid time (00:00-23:59)", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Now save to database and schedule alarm
                if (db.insertMedicine(name, dose, time, notes)) {
                    int reqCode = (name + time).hashCode();
                    AlarmScheduler.scheduleDailyExact(this, reqCode, name, dose, notes, hour, minute);
                    Toast.makeText(this, "Medicine saved & daily alarm set!", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(this, "Error saving medicine!", Toast.LENGTH_SHORT).show();
                }

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid time numbers", Toast.LENGTH_SHORT).show();
                return;
            }
        });
    }
}