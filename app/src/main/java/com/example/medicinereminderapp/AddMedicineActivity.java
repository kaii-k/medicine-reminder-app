package com.example.medicinereminderapp;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import java.util.Random;

public class AddMedicineActivity extends AppCompatActivity {

    EditText nameInput, doseInput, hourInput, minuteInput, notesInput;
    Button saveBtn, ampmBtn;
    Spinner repeatSpinner, durationSpinner;
    DatabaseHelper db;
    private final Random random = new Random();
    private boolean isAM = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_medicine);

        initializeViews();
        setupSpinners();
        db = new DatabaseHelper(this);

        // AM/PM Toggle Button
        ampmBtn.setOnClickListener(v -> toggleAmPm());

        saveBtn.setOnClickListener(v -> saveMedicine());
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

    private void saveMedicine() {
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

            // Convert to 24-hour format for alarm scheduling
            int hour24 = convertTo24Hour(hour, isAM);

            // Format time for display
            String displayTime = formatTimeForDisplay(hour, minute, isAM);

            // Calculate end date based on duration
            long endDate = calculateEndDate(duration);

            // Save to database using the enhanced method
            long medicineId = db.insertMedicineWithDetails(name, dose, displayTime, notes,
                    repeatType, "All", duration, endDate);

            if (medicineId != -1) {
                // Schedule alarm using your existing AlarmScheduler
                int reqCode = generateUniqueRequestCode(name, displayTime);

                // Use your existing schedule method
                AlarmScheduler.scheduleDailyExact(this, reqCode, name, dose, notes, hour24, minute);

                // Record the first dose in history
                db.recordDoseHistory((int) medicineId, name, dose,
                        System.currentTimeMillis(), 0, "scheduled");

                Toast.makeText(this, "Medicine saved & alarm set!", Toast.LENGTH_LONG).show();
                finish();
            } else {
                Toast.makeText(this, "Error saving medicine!", Toast.LENGTH_SHORT).show();
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
            case "Ongoing": return currentTime + (365 * millisInDay); // 1 year as "ongoing"
            default: return currentTime + (365 * millisInDay);
        }
    }

    private int convertTo24Hour(int hour12, boolean isAM) {
        if (isAM) {
            return (hour12 == 12) ? 0 : hour12;
        } else {
            return (hour12 == 12) ? 12 : hour12 + 12;
        }
    }

    private String formatTimeForDisplay(int hour, int minute, boolean isAM) {
        String minuteStr = (minute < 10) ? "0" + minute : String.valueOf(minute);
        String period = isAM ? "AM" : "PM";
        return hour + ":" + minuteStr + " " + period;
    }

    private int generateUniqueRequestCode(String name, String time) {
        return (name + time + System.currentTimeMillis() + random.nextInt(1000)).hashCode();
    }
}