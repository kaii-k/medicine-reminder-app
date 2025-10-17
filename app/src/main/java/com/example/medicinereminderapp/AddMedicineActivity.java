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

            if (db.insertMedicine(name, dose, time, notes)) {
                String[] parts = time.split(":");
                int hour = Integer.parseInt(parts[0]);
                int minute = Integer.parseInt(parts[1]);

                int reqCode = (name + time).hashCode();
                AlarmScheduler.scheduleDailyExact(this, reqCode, name, dose, notes, hour, minute);
                Toast.makeText(this, "Medicine saved & daily alarm set!", Toast.LENGTH_LONG).show();
                finish();
            } else {
                Toast.makeText(this, "Error saving medicine!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}