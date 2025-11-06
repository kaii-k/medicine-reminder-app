package com.example.medicinereminderapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple report screen: Taken vs Missed vs Skipped (all medicines combined).
 * Listens for local broadcast "com.example.medicinereminderapp.REPORT_UPDATED"
 * so it updates live when user marks Done/Missed.
 */
public class ReportActivity extends AppCompatActivity {

    private PieChart pieChart;
    private TextView emptyText;
    private DatabaseHelper db;

    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadAndRender();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        pieChart = findViewById(R.id.reportPieChart);
        emptyText = findViewById(R.id.reportEmptyText);
        db = new DatabaseHelper(this);

        // register broadcast for live updates
        LocalBroadcastManager.getInstance(this).registerReceiver(updateReceiver,
                new IntentFilter("com.example.medicinereminderapp.REPORT_UPDATED"));

        loadAndRender();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updateReceiver);
    }

    // fetch totals from dose_history and render chart or empty state
    private void loadAndRender() {
        // Quick aggregated query using your helper methods is not available, so query directly.
        // We'll count taken / missed / skipped from dose_history.
        android.database.sqlite.SQLiteDatabase rdb = db.getReadableDatabase();
        String sql = "SELECT " +
                "SUM(CASE WHEN status = 'taken' THEN 1 ELSE 0 END) as taken_count, " +
                "SUM(CASE WHEN status = 'missed' THEN 1 ELSE 0 END) as missed_count, " +
                "SUM(CASE WHEN status = 'skipped' THEN 1 ELSE 0 END) as skipped_count, " +
                "COUNT(*) as total_count " +
                "FROM dose_history";
        android.database.Cursor cursor = rdb.rawQuery(sql, null);

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
        } else {
            emptyText.setVisibility(View.GONE);
            pieChart.setVisibility(View.VISIBLE);
        }

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
}
