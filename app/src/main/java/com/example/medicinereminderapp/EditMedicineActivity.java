package com.example.medicinereminderapp;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.*;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import java.util.Random;

public class EditMedicineActivity extends AppCompatActivity {

    EditText nameInput, doseInput, hourInput, minuteInput, notesInput;
    Button saveBtn, ampmBtn;
    Spinner repeatSpinner, durationSpinner;
    DatabaseHelper db;
    private final Random random = new Random();
    private boolean isAM = true;
    private int medicineId;
    private String originalTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_medicine); // Using same layout as add

        // Get medicine ID from intent
        medicineId = getIntent().getIntExtra("MEDICINE_ID", -1);
        if (medicineId == -1) {
            Toast.makeText(this, "Error: Medicine not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupSpinners();
        loadMedicineData();

        // Modern back button handling
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    private void initializeViews() {
        nameInput = findViewById(R.id.nameInput);
        doseInput = findViewById(R.id.doseInput);
        hourInput = findViewById(R.id.hourInput);
        minuteInput = findViewById(R.id.minuteInput);
        notesInput = findViewById(R.id.notesInput);
        saveBtn = findViewById(R.id.saveBtn);
        ampmBtn = findViewById(R.id.ampmBtn);

        // Initialize spinners
        repeatSpinner = findViewById(R.id.repeatSpinner);
        durationSpinner = findViewById(R.id.durationSpinner);

        db = new DatabaseHelper(this);

        // Change button text to "Update"
        saveBtn.setText("Update Medicine");
        saveBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(android.R.color.holo_orange_dark)));

        // AM/PM Toggle Button
        ampmBtn.setOnClickListener(v -> toggleAmPm());

        // Save/Update button
        saveBtn.setOnClickListener(v -> updateMedicine());
    }

    private void setupSpinners() {
        // Repeat options
        String[] repeatOptions = {"Daily", "Weekly", "Monthly"};
        ArrayAdapter<String> repeatAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, repeatOptions);
        repeatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        repeatSpinner.setAdapter(repeatAdapter);

        // Duration options
        String[] durationOptions = {"1 week", "2 weeks", "1 month", "3 months", "Ongoing"};
        ArrayAdapter<String> durationAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, durationOptions);
        durationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        durationSpinner.setAdapter(durationAdapter);
    }

    private void toggleAmPm() {
        isAM = !isAM;
        ampmBtn.setText(isAM ? "AM" : "PM");
        int color = ContextCompat.getColor(this,
                isAM ? android.R.color.holo_blue_light : android.R.color.holo_red_light);
        ampmBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
    }

    private void loadMedicineData() {
        Cursor cursor = db.getMedicineById(medicineId);
        if (cursor != null && cursor.moveToFirst()) {
            nameInput.setText(cursor.getString(1)); // name
            doseInput.setText(cursor.getString(2)); // dose

            String time = cursor.getString(3); // time in format "8:30 AM"
            originalTime = time;

            // Parse the time and set hour, minute, and AM/PM
            parseAndSetTime(time);

            notesInput.setText(cursor.getString(4)); // notes

            // Set repeat type and duration if available
            try {
                String repeatType = cursor.getString(5); // repeat_type
                String duration = cursor.getString(7); // duration

                if (repeatType != null) {
                    setSpinnerSelection(repeatSpinner, repeatType);
                }
                if (duration != null) {
                    setSpinnerSelection(durationSpinner, duration);
                }
            } catch (Exception e) {
                // Ignore if columns don't exist
            }

            cursor.close();
        }
    }

    private void parseAndSetTime(String time) {
        try {
            // Time format: "8:30 AM" or "11:45 PM"
            String[] parts = time.split(" ");
            if (parts.length == 2) {
                String timePart = parts[0];
                String period = parts[1];

                String[] timeParts = timePart.split(":");
                if (timeParts.length == 2) {
                    int hour = Integer.parseInt(timeParts[0]);
                    int minute = Integer.parseInt(timeParts[1]);

                    hourInput.setText(String.valueOf(hour));
                    minuteInput.setText(String.valueOf(minute));

                    isAM = period.equals("AM");
                    ampmBtn.setText(isAM ? "AM" : "PM");

                    int color = ContextCompat.getColor(this,
                            isAM ? android.R.color.holo_blue_light : android.R.color.holo_red_light);
                    ampmBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error parsing time", Toast.LENGTH_SHORT).show();
        }
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equals(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void updateMedicine() {
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

            // Get repeat type and duration
            String repeatType = repeatSpinner.getSelectedItem().toString();
            String duration = durationSpinner.getSelectedItem().toString();

            // Format time for display
            String displayTime = formatTimeForDisplay(hour, minute, isAM);

            // Calculate end date based on duration
            long endDate = calculateEndDate(duration);

            // Update medicine in database using enhanced method
            boolean success = db.updateMedicineWithDetails(medicineId, name, dose, displayTime, notes,
                    repeatType, "All", duration, endDate);

            if (success) {
                Toast.makeText(this, "Medicine updated successfully!", Toast.LENGTH_SHORT).show();

                // Send broadcast to refresh the list
                Intent refreshIntent = new Intent("MEDICINE_UPDATED");
                sendBroadcast(refreshIntent);

                finish();
            } else {
                Toast.makeText(this, "Error updating medicine!", Toast.LENGTH_SHORT).show();
            }

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers for time", Toast.LENGTH_SHORT).show();
        }
    }

    private long calculateEndDate(String duration) {
        long currentTime = System.currentTimeMillis();
        long millisInDay = 24 * 60 * 60 * 1000L;

        switch (duration) {
            case "1 week": return currentTime + (7 * millisInDay);
            case "2 weeks": return currentTime + (14 * millisInDay);
            case "1 month": return currentTime + (30 * millisInDay);
            case "3 months": return currentTime + (90 * millisInDay);
            case "Ongoing": return currentTime + (365 * millisInDay);
            default: return currentTime + (365 * millisInDay);
        }
    }

    private String formatTimeForDisplay(int hour, int minute, boolean isAM) {
        String minuteStr = (minute < 10) ? "0" + minute : String.valueOf(minute);
        String period = isAM ? "AM" : "PM";
        return hour + ":" + minuteStr + " " + period;
    }
}