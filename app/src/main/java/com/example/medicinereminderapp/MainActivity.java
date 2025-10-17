package com.example.medicinereminderapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    Button addBtn, viewBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        addBtn = findViewById(R.id.addBtn);
        viewBtn = findViewById(R.id.viewBtn);

        addBtn.setOnClickListener(v -> startActivity(new Intent(this, AddMedicineActivity.class)));
        viewBtn.setOnClickListener(v -> startActivity(new Intent(this, MedicineListActivity.class)));
    }
}