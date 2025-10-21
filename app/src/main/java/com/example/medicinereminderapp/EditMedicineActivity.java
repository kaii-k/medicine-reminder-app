package com.example.medicinereminderapp;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.*;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Random;

public class EditMedicineActivity extends AppCompatActivity {

    EditText nameInput, doseInput, notesInput;
    Button saveBtn;
    DatabaseHelper db;
    private final Random random = new Random();
    private int medicineId;
    private String originalTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_medicine);

        // Get medicine ID from intent
        medicineId = getIntent().getIntExtra("MEDICINE_ID", -1);
        if (medicineId == -1) {
            Toast.makeText(this, "Error: Medicine not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
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
        notesInput = findViewById(R.id.notesInput);
        saveBtn = findViewById(R.id.saveBtn);
        db = new DatabaseHelper(this);

        // Change button text to "Update"
        saveBtn.setText("Update Medicine");
        saveBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(android.R.color.holo_orange_dark)));

        // Save/Update button
        saveBtn.setOnClickListener(v -> updateMedicine());
    }

    private void loadMedicineData() {
        Cursor cursor = db.getMedicineById(medicineId);
        if (cursor != null && cursor.moveToFirst()) {
            nameInput.setText(cursor.getString(1)); // name
            doseInput.setText(cursor.getString(2)); // dose

            String time = cursor.getString(3); // time
            originalTime = time;

            notesInput.setText(cursor.getString(4)); // notes
            cursor.close();
        }
    }

    private void updateMedicine() {
        String name = nameInput.getText().toString();
        String dose = doseInput.getText().toString();
        String notes = notesInput.getText().toString();

        if (name.isEmpty() || dose.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Keep the original time (since we don't have time editing in this version)
        String displayTime = originalTime;

        // Update medicine in database
        if (db.updateMedicine(medicineId, name, dose, displayTime, notes)) {
            Toast.makeText(this, "Medicine updated successfully!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Error updating medicine!", Toast.LENGTH_SHORT).show();
        }
    }
}