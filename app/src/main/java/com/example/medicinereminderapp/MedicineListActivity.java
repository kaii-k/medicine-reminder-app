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
        Cursor c = db.getAllMedicines();
        ArrayList<String> items = new ArrayList<>();
        medicineIds.clear();

        if (c != null && c.moveToFirst()) {
            do {
                int id = c.getInt(0);
                medicineIds.add(id);

                String medicineInfo =
                        "💊 " + c.getString(1) + "\n" +
                                "Dose: " + c.getString(2) + "\n" +
                                "Time: " + c.getString(3) + "\n" +
                                "Notes: " + c.getString(4) + "\n" +
                                "Status: " + c.getString(5);

                items.add(medicineInfo);
            } while (c.moveToNext());
            c.close();
        } else {
            items.add("No medicines found");
            medicineIds.add(-1);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
        list.setAdapter(adapter);

        // CLICK to EDIT medicine
        list.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= medicineIds.size() || medicineIds.get(position) == -1) {
                return;
            }

            int medicineId = medicineIds.get(position);
            // Open Edit Activity
            Intent intent = new Intent(MedicineListActivity.this, EditMedicineActivity.class);
            intent.putExtra("MEDICINE_ID", medicineId);
            startActivity(intent);
        });

        // LONG PRESS to DELETE medicine
        list.setOnItemLongClickListener((parent, view, position, id) -> {
            if (position >= medicineIds.size() || medicineIds.get(position) == -1) {
                return false;
            }

            int medicineId = medicineIds.get(position);
            String medicineName = items.get(position).split("\n")[0].replace("💊 ", "");

            new android.app.AlertDialog.Builder(this)
                    .setTitle("Delete Medicine")
                    .setMessage("Delete " + medicineName + "?")
                    .setPositiveButton("DELETE", (dialog, which) -> {
                        if (db.deleteMedicine(medicineId)) {
                            Toast.makeText(this, "Medicine deleted", Toast.LENGTH_SHORT).show();
                            loadData();
                        } else {
                            Toast.makeText(this, "Error deleting medicine", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();

            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }
}