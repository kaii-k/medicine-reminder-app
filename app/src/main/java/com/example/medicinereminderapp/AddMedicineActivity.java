package com.example.medicinereminderapp;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import java.util.Random;

public class AddMedicineActivity extends AppCompatActivity {

    EditText nameInput, doseInput, hourInput, minuteInput, notesInput;
    Button saveBtn, ampmBtn;
    DatabaseHelper db;
    private final Random random = new Random(); // Made final
    private boolean isAM = true; // Default to AM

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_medicine);

        // Initialize views - FIXED: Using correct IDs
        nameInput = findViewById(R.id.nameInput);
        doseInput = findViewById(R.id.doseInput);
        hourInput = findViewById(R.id.hourInput);
        minuteInput = findViewById(R.id.minuteInput);
        notesInput = findViewById(R.id.notesInput);
        saveBtn = findViewById(R.id.saveBtn);
        ampmBtn = findViewById(R.id.ampmBtn);
        db = new DatabaseHelper(this);

        // AM/PM Toggle Button
        ampmBtn.setOnClickListener(v -> {
            isAM = !isAM;
            ampmBtn.setText(isAM ? "AM" : "PM");

            // Use ContextCompat to fix deprecated method
            int color = ContextCompat.getColor(this,
                    isAM ? android.R.color.holo_blue_light : android.R.color.holo_red_light);
            ampmBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        });

        saveBtn.setOnClickListener(v -> {
            String name = nameInput.getText().toString();
            String dose = doseInput.getText().toString();
            String hourStr = hourInput.getText().toString();
            String minuteStr = minuteInput.getText().toString();
            String notes = notesInput.getText().toString();

            if (name.isEmpty() || dose.isEmpty() || hourStr.isEmpty() || minuteStr.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int hour = Integer.parseInt(hourStr);
                int minute = Integer.parseInt(minuteStr);

                // Validate hour (1-12 for 12-hour format)
                if (hour < 1 || hour > 12) {
                    Toast.makeText(this, "Please enter hour between 1-12", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Validate minute (0-59)
                if (minute < 0 || minute > 59) {
                    Toast.makeText(this, "Please enter minute between 00-59", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Convert to 24-hour format for alarm scheduling
                int hour24 = convertTo24Hour(hour, isAM);

                // Format time for display (HH:MM AM/PM)
                String displayTime = formatTimeForDisplay(hour, minute, isAM);

                // Now save to database and schedule alarm
                if (db.insertMedicine(name, dose, displayTime, notes)) {
                    // Generate UNIQUE request code to prevent conflicts
                    int reqCode = generateUniqueRequestCode(name, displayTime);

                    AlarmScheduler.scheduleDailyExact(this, reqCode, name, dose, notes, hour24, minute);
                    Toast.makeText(this, "Medicine saved & daily alarm set!", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(this, "Error saving medicine!", Toast.LENGTH_SHORT).show();
                }

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter valid numbers for time", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Convert 12-hour to 24-hour format
    private int convertTo24Hour(int hour12, boolean isAM) {
        if (isAM) {
            return (hour12 == 12) ? 0 : hour12; // 12 AM = 0, 1-11 AM = 1-11
        } else {
            return (hour12 == 12) ? 12 : hour12 + 12; // 12 PM = 12, 1-11 PM = 13-23
        }
    }

    // Format time for display (e.g., "08:30 PM")
    private String formatTimeForDisplay(int hour, int minute, boolean isAM) {
        String minuteStr = (minute < 10) ? "0" + minute : String.valueOf(minute);
        String period = isAM ? "AM" : "PM";
        return hour + ":" + minuteStr + " " + period;
    }

    // Generate unique request code to prevent alarm conflicts
    private int generateUniqueRequestCode(String name, String time) {
        return (name + time + System.currentTimeMillis() + random.nextInt(1000)).hashCode();
    }
}