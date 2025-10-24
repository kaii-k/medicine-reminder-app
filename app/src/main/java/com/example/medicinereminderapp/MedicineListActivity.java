package com.example.medicinereminderapp;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class MedicineListActivity extends AppCompatActivity {
    ListView list;
    DatabaseHelper db;
    ArrayList<Integer> medicineIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medicine_list);

        list = findViewById(R.id.medicineList);
        db = new DatabaseHelper(this);
        medicineIds = new ArrayList<>();

        Button backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(v -> finish());

        loadData();
    }

    private void loadData() {
        try {
            Cursor c = db.getAllMedicines();
            ArrayList<String> items = new ArrayList<>();
            medicineIds.clear();

            if (c != null && c.moveToFirst()) {
                do {
                    int id = c.getInt(0);
                    medicineIds.add(id);

                    String name = c.getString(1);
                    String dose = c.getString(2);
                    String time = c.getString(3);
                    String notes = c.getString(4);
                    String status = c.getString(5);

                    String medicineInfo =
                            "💊 " + name + "\n" +
                                    "Dose: " + dose + "\n" +
                                    "Time: " + time + "\n" +
                                    "Status: " + status;

                    if (notes != null && !notes.isEmpty()) {
                        medicineInfo += "\nNotes: " + notes;
                    }

                    items.add(medicineInfo);
                } while (c.moveToNext());
                c.close();
            } else {
                items.add("No medicines found\nAdd some medicines first!");
                medicineIds.add(-1);
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
            list.setAdapter(adapter);

            // CLICK to EDIT medicine
            list.setOnItemClickListener((parent, view, position, id) -> {
                if (position >= medicineIds.size() || medicineIds.get(position) == -1) {
                    Toast.makeText(MedicineListActivity.this, "No medicine selected", Toast.LENGTH_SHORT).show();
                    return;
                }

                int medicineId = medicineIds.get(position);
                try {
                    Intent intent = new Intent(MedicineListActivity.this, EditMedicineActivity.class);
                    intent.putExtra("MEDICINE_ID", medicineId);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MedicineListActivity.this, "Error opening edit screen", Toast.LENGTH_SHORT).show();
                }
            });

            // LONG PRESS to DELETE medicine
            list.setOnItemLongClickListener((parent, view, position, id) -> {
                if (position >= medicineIds.size() || medicineIds.get(position) == -1) {
                    return false;
                }

                int medicineId = medicineIds.get(position);
                String medicineName = "this medicine";
                try {
                    medicineName = items.get(position).split("\n")[0].replace("💊 ", "");
                } catch (Exception e) {
                    // Use default name if parsing fails
                }

                new android.app.AlertDialog.Builder(this)
                        .setTitle("Delete Medicine")
                        .setMessage("Delete " + medicineName + "?")
                        .setPositiveButton("DELETE", (dialog, which) -> {
                            if (db.deleteMedicine(medicineId)) {
                                Toast.makeText(this, "Medicine deleted", Toast.LENGTH_SHORT).show();
                                loadData(); // Refresh the list
                            } else {
                                Toast.makeText(this, "Error deleting medicine", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();

                return true;
            });

        } catch (Exception e) {
            Toast.makeText(this, "Error loading medicines: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the list when returning to this activity
        loadData();
    }
}