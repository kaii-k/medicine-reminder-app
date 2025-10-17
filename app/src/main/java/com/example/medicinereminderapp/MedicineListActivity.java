package com.example.medicinereminderapp;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class MedicineListActivity extends AppCompatActivity {
    ListView list;
    DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medicine_list);

        list = findViewById(R.id.medicineList);
        db = new DatabaseHelper(this);
        loadData();
    }

    private void loadData() {
        Cursor c = db.getAllMedicines();
        ArrayList<String> items = new ArrayList<>();

        while (c.moveToNext()) {
            items.add(
                    "💊 " + c.getString(1) + "\n" +
                            "Dose: " + c.getString(2) + "\n" +
                            "Time: " + c.getString(3) + "\n" +
                            "Notes: " + c.getString(4) + "\n" +
                            "Status: " + c.getString(5)
            );
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
        list.setAdapter(adapter);
        c.close();
    }
}