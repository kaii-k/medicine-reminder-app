package com.example.medicinereminderapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ReportsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        TextView weeklyReportText = findViewById(R.id.weeklyReportText);
        TextView monthlyReportText = findViewById(R.id.monthlyReportText);
        TextView overallReportText = findViewById(R.id.overallReportText);

        Button backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(v -> finish());

        // Simple test content
        weeklyReportText.setText("📅 WEEKLY REPORT\n\n✅ Taken: 5\n❌ Missed: 1\n📊 Adherence: 83.3%");
        monthlyReportText.setText("📅 MONTHLY REPORT\n\n✅ Taken: 20\n❌ Missed: 4\n📊 Adherence: 83.3%");
        overallReportText.setText("📊 OVERALL SUMMARY\n\n💊 Medicine 1: 85%\n💊 Medicine 2: 90%\n📈 Overall: 87.5%");
    }
}