package com.example.medicinereminderapp;

import android.app.AlarmManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.app.AlertDialog;
import java.util.Random;
import android.util.Log;

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

        repeatSpinner = findViewById(R.id.repeatSpinner);
        durationSpinner = findViewById(R.id.durationSpinner);
    }

    private void setupSpinners() {
        String[] repeatOptions = {"Daily", "Weekly", "Monthly"};
        ArrayAdapter<String> repeatAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, repeatOptions);
        repeatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        repeatSpinner.setAdapter(repeatAdapter);

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
        String name = nameInput.getText().toString().trim();
        String dose = doseInput.getText().toString().trim();
        String hourStr = hourInput.getText().toString().trim();
        String minuteStr = minuteInput.getText().toString().trim();
        String notes = notesInput.getText().toString().trim();

        if (name.isEmpty() || dose.isEmpty() || hourStr.isEmpty() || minuteStr.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int hour = Integer.parseInt(hourStr);
            int minute = Integer.parseInt(minuteStr);

            if (hour < 1 || hour > 12) {
                Toast.makeText(this, "Please enter hour between 1-12", Toast.LENGTH_SHORT).show();
                return;
            }
            if (minute < 0 || minute > 59) {
                Toast.makeText(this, "Please enter minute between 00-59", Toast.LENGTH_SHORT).show();
                return;
            }

            String repeatType = repeatSpinner.getSelectedItem().toString();
            String duration = durationSpinner.getSelectedItem().toString();

            int hour24 = convertTo24Hour(hour, isAM);

            Log.d("AddMedicine", "User entered: " + hour + ":" + minute + " " + (isAM ? "AM" : "PM"));
            Log.d("AddMedicine", "Converted to 24h: " + hour24 + ":" + minute);
            Log.d("AddMedicine", "Repeat Type: " + repeatType + ", Duration: " + duration);

            String displayTime = formatTimeForDisplay(hour, minute, isAM);
            long endDate = calculateEndDate(duration);

            long medicineId = db.insertMedicineWithDetails(name, dose, displayTime, notes,
                    repeatType, "All", duration, endDate);

            if (medicineId != -1) {
                int reqCode = (int) medicineId; // stable requestCode using DB id
                Log.d("AddMedicine", "Scheduling alarm with requestCode: " + reqCode);
                Log.d("AddMedicine", "Calling AlarmScheduler with time: " + hour24 + ":" + minute);

                AlarmScheduler.scheduleExactAlarm(this, reqCode, name, dose, notes, hour24, minute);

                // Prompt user to allow exact alarms if needed
                ensureExactAlarmsAllowed();

                db.recordDoseHistory((int) medicineId, name, dose,
                        System.currentTimeMillis(), 0, "scheduled");

                Toast.makeText(this, "Medicine saved & alarm set for " + displayTime, Toast.LENGTH_LONG).show();
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
            case "Ongoing": return currentTime + (365 * millisInDay);
            default: return currentTime + (365 * millisInDay);
        }
    }

    private String formatTimeForDisplay(int hour, int minute, boolean isAM) {
        String minuteStr = (minute < 10) ? "0" + minute : String.valueOf(minute);
        String period = isAM ? "AM" : "PM";
        return hour + ":" + minuteStr + " " + period;
    }

    private int convertTo24Hour(int hour12, boolean isAM) {
        if (isAM) {
            return (hour12 == 12) ? 0 : hour12;
        } else {
            return (hour12 == 12) ? 12 : hour12 + 12;
        }
    }

    /**
     * If exact alarms are not allowed on this device, ask the user to enable them.
     * Call this from an Activity after scheduling the alarm (so user sees the dialog).
     */
    private void ensureExactAlarmsAllowed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (am != null && !am.canScheduleExactAlarms()) {
                new AlertDialog.Builder(this)
                        .setTitle("Allow exact alarms")
                        .setMessage("To reliably notify you at the exact medicine time, please allow exact alarms for this app in system settings.")
                        .setPositiveButton("Open settings", (dialog, which) -> {
                            Intent i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                            if (i.resolveActivity(getPackageManager()) != null) {
                                startActivity(i);
                            } else {
                                Intent appSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                appSettings.setData(android.net.Uri.fromParts("package", getPackageName(), null));
                                startActivity(appSettings);
                            }
                        })
                        .setNegativeButton("Not now", null)
                        .show();
            }
        }
    }
}
