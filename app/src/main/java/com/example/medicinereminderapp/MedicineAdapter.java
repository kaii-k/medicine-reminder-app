package com.example.medicinereminderapp;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class MedicineAdapter extends CursorAdapter {
    private DatabaseHelper dbHelper;

    public MedicineAdapter(Context context, Cursor cursor, DatabaseHelper db) {
        super(context, cursor, 0);
        this.dbHelper = db;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.medicine_list_item, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Get medicine data
        int id = cursor.getInt(0);
        String name = cursor.getString(1);
        String dose = cursor.getString(2);
        String time = cursor.getString(3);
        String notes = cursor.getString(4);
        String status = cursor.getString(5);

        // Find views
        TextView nameView = view.findViewById(R.id.medicineName);
        TextView doseView = view.findViewById(R.id.medicineDose);
        TextView timeView = view.findViewById(R.id.medicineTime);
        TextView statusView = view.findViewById(R.id.medicineStatus);
        Button editBtn = view.findViewById(R.id.editBtn);
        Button deleteBtn = view.findViewById(R.id.deleteBtn);

        // Set data
        nameView.setText(name);
        doseView.setText("Dose: " + dose);
        timeView.setText("Time: " + time);
        statusView.setText("Status: " + status);

        // Set status color
        if ("Taken".equals(status)) {
            statusView.setTextColor(0xFF4CAF50); // Green
        } else {
            statusView.setTextColor(0xFFFF9800); // Orange
        }

        // Edit button click
        editBtn.setOnClickListener(v -> {
            Toast.makeText(context, "Edit: " + name, Toast.LENGTH_SHORT).show();
            // We'll implement edit functionality next
        });

        // Delete button click
        deleteBtn.setOnClickListener(v -> {
            if (dbHelper.deleteMedicine(id)) {
                AlarmScheduler.cancelAlarm(context, id);
                Toast.makeText(context, "Medicine deleted", Toast.LENGTH_SHORT).show();
                // Refresh the list
                refreshData();
            } else {
                Toast.makeText(context, "Error deleting medicine", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void refreshData() {
        Cursor newCursor = dbHelper.getAllMedicines();
        this.changeCursor(newCursor);
        this.notifyDataSetChanged();
    }
}