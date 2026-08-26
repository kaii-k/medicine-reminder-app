package com.example.medicinereminderapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.GridLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Report screen:
 *  - "All Medicines" -> aggregate Taken vs Missed vs Skipped pie chart.
 *  - A specific medicine -> a day-by-day adherence calendar covering its
 *    whole course (from first reminder to end date / today), so a doctor
 *    can see exactly which dates were missed.
 * Listens for local broadcast "com.example.medicinereminderapp.REPORT_UPDATED"
 * so it updates live when user marks Done/Missed.
 */
public class ReportActivity extends AppCompatActivity {

    private Spinner medicineSpinner;
    private PieChart pieChart;
    private View calendarScroll;
    private TextView emptyText;
    private TextView adherenceSummaryText;
    private GridLayout calendarGrid;
    private DatabaseHelper db;

    private final List<Integer> spinnerMedicineIds = new ArrayList<>();

    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            renderForSelection();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        medicineSpinner = findViewById(R.id.medicineSpinner);
        pieChart = findViewById(R.id.reportPieChart);
        calendarScroll = findViewById(R.id.calendarScroll);
        emptyText = findViewById(R.id.reportEmptyText);
        adherenceSummaryText = findViewById(R.id.adherenceSummaryText);
        calendarGrid = findViewById(R.id.calendarGrid);
        db = new DatabaseHelper(this);

        findViewById(R.id.backBtn).setOnClickListener(v -> finish());

        setupMedicineSpinner();

        LocalBroadcastManager.getInstance(this).registerReceiver(updateReceiver,
                new IntentFilter("com.example.medicinereminderapp.REPORT_UPDATED"));

        renderForSelection();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updateReceiver);
    }

    private void setupMedicineSpinner() {
        List<String> labels = new ArrayList<>();
        labels.add("All Medicines (Overview)");
        spinnerMedicineIds.clear();
        spinnerMedicineIds.add(-1);

        for (DatabaseHelper.MedicineBasic m : db.getAllMedicinesBasic()) {
            labels.add(m.name);
            spinnerMedicineIds.add(m.id);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        medicineSpinner.setAdapter(adapter);
        medicineSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                renderForSelection();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void renderForSelection() {
        int position = medicineSpinner.getSelectedItemPosition();
        int medicineId = (position >= 0 && position < spinnerMedicineIds.size())
                ? spinnerMedicineIds.get(position) : -1;

        if (medicineId == -1) {
            calendarScroll.setVisibility(View.GONE);
            renderOverallPieChart();
        } else {
            pieChart.setVisibility(View.GONE);
            renderCalendarForMedicine(medicineId);
        }
    }

    // ----- "All Medicines" overview -----

    private void renderOverallPieChart() {
        android.database.sqlite.SQLiteDatabase rdb = db.getReadableDatabase();
        String sql = "SELECT " +
                "SUM(CASE WHEN status = 'taken' THEN 1 ELSE 0 END) as taken_count, " +
                "SUM(CASE WHEN status = 'missed' THEN 1 ELSE 0 END) as missed_count, " +
                "SUM(CASE WHEN status = 'skipped' THEN 1 ELSE 0 END) as skipped_count, " +
                "COUNT(*) as total_count " +
                "FROM dose_history";
        Cursor cursor = rdb.rawQuery(sql, null);

        int taken = 0, missed = 0, skipped = 0, total = 0;
        if (cursor.moveToFirst()) {
            taken = cursor.getInt(0);
            missed = cursor.getInt(1);
            skipped = cursor.getInt(2);
            total = cursor.getInt(3);
        }
        cursor.close();

        if (total == 0) {
            pieChart.setVisibility(View.GONE);
            emptyText.setVisibility(View.VISIBLE);
            emptyText.setText("No recorded doses yet.");
            return;
        }

        emptyText.setVisibility(View.GONE);
        pieChart.setVisibility(View.VISIBLE);

        List<PieEntry> entries = new ArrayList<>();
        if (taken > 0) entries.add(new PieEntry(taken, "Taken"));
        if (missed > 0) entries.add(new PieEntry(missed, "Missed"));
        if (skipped > 0) entries.add(new PieEntry(skipped, "Skipped"));

        PieDataSet set = new PieDataSet(entries, "");
        PieData data = new PieData(set);
        data.setDrawValues(true);

        Description desc = new Description();
        desc.setText("");
        pieChart.setDescription(desc);
        pieChart.setData(data);
        pieChart.invalidate();
    }

    // ----- Per-medicine adherence calendar -----

    private void renderCalendarForMedicine(int medicineId) {
        long[] range = db.getMedicineCalendarRange(medicineId);
        if (range == null) {
            calendarScroll.setVisibility(View.GONE);
            emptyText.setVisibility(View.VISIBLE);
            emptyText.setText("No dose history recorded yet for this medicine.");
            return;
        }

        emptyText.setVisibility(View.GONE);
        calendarScroll.setVisibility(View.VISIBLE);

        List<DatabaseHelper.DayStatus> statuses = db.getDailyStatus(medicineId, range[0], range[1]);

        int taken = 0, missed = 0, skipped = 0, tracked = 0;
        for (DatabaseHelper.DayStatus s : statuses) {
            switch (s) {
                case TAKEN: taken++; tracked++; break;
                case MISSED: missed++; tracked++; break;
                case SKIPPED: skipped++; tracked++; break;
                case NONE: break;
            }
        }

        String pct = tracked == 0 ? "0%" : Math.round(taken * 100.0 / tracked) + "%";
        StringBuilder summary = new StringBuilder(taken + " / " + tracked + " doses taken (" + pct + ")");
        if (missed > 0) summary.append(" — ").append(missed).append(" missed");
        if (skipped > 0) summary.append(" — ").append(skipped).append(" skipped");
        adherenceSummaryText.setText(summary.toString());

        calendarGrid.removeAllViews();
        Calendar day = Calendar.getInstance();
        day.setTimeInMillis(range[0]);
        SimpleDateFormat dayFmt = new SimpleDateFormat("d\nMMM", Locale.getDefault());

        for (DatabaseHelper.DayStatus status : statuses) {
            calendarGrid.addView(buildDayCell(day.getTime(), status, dayFmt));
            day.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    private TextView buildDayCell(java.util.Date date, DatabaseHelper.DayStatus status, SimpleDateFormat dayFmt) {
        TextView cell = new TextView(this);
        int sizePx = (int) (44 * getResources().getDisplayMetrics().density);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = sizePx;
        params.height = sizePx;
        params.setMargins(4, 4, 4, 4);
        cell.setLayoutParams(params);

        cell.setText(dayFmt.format(date));
        cell.setTextSize(10);
        cell.setGravity(Gravity.CENTER);
        cell.setTextColor(Color.WHITE);

        int colorRes;
        switch (status) {
            case TAKEN: colorRes = R.color.dose_taken; break;
            case MISSED: colorRes = R.color.dose_missed; break;
            case SKIPPED: colorRes = R.color.dose_skipped; break;
            default: colorRes = R.color.dose_none; break;
        }
        cell.setBackgroundColor(ContextCompat.getColor(this, colorRes));

        return cell;
    }
}
