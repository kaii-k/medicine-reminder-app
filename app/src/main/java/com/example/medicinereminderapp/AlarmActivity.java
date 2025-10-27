package com.example.medicinereminderapp;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

public class AlarmActivity extends AppCompatActivity {
    private static final String TAG = "AlarmActivity";
    private Vibrator vibrator;
    private boolean isAlarmActive = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called — requestCode=" + getIntent().getIntExtra("requestCode", -1));

        wakeUpDevice();
        setContentView(R.layout.activity_alarm);

        // Get details from intent
        String medicineName = getIntent().getStringExtra("name");
        String dose = getIntent().getStringExtra("dose");
        String notes = getIntent().getStringExtra("notes");

        setupViews(medicineName, dose, notes);
        startAlarm();
    }

    private void wakeUpDevice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(true);
            setShowWhenLocked(true);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            );
        }

        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (km != null && km.isKeyguardLocked()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                km.requestDismissKeyguard(this, null);
        }
    }

    private void setupViews(String medName, String dose, String notes) {
        TextView medNameTxt = findViewById(R.id.medNameTxt);
        TextView doseTxt = findViewById(R.id.doseTxt);
        TextView notesTxt = findViewById(R.id.notesTxt);
        Button snoozeBtn = findViewById(R.id.snoozeBtn);
        Button dismissBtn = findViewById(R.id.dismissBtn);

        medNameTxt.setText(medName);
        doseTxt.setText("Dose: " + dose);
        notesTxt.setText(notes != null && !notes.isEmpty() ? "Notes: " + notes : "No additional notes");

        snoozeBtn.setOnClickListener(v -> {
            stopAlarm();
            snoozeAlarm();
            finish();
        });

        dismissBtn.setOnClickListener(v -> {
            stopAlarm();
            finish();
        });
    }

    private void startAlarm() {
        try {
            Intent svc = new Intent(this, AlarmService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(svc);
            else startService(svc);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start AlarmService: " + e.getMessage(), e);
        }

        // vibrate a bit while service starts
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 500, 500};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
            else vibrator.vibrate(pattern, 0);
        }
    }

    private void stopAlarm() {
        isAlarmActive = false;

        try {
            Intent stop = new Intent(this, AlarmService.class);
            stop.setAction(AlarmService.ACTION_STOP);
            startService(stop);
        } catch (Exception e) {
            Log.e(TAG, "Error stopping AlarmService", e);
        }

        if (vibrator != null) vibrator.cancel();
    }

    private void snoozeAlarm() {
        String name = getIntent().getStringExtra("name");
        String dose = getIntent().getStringExtra("dose");
        String notes = getIntent().getStringExtra("notes");

        long snoozeTime = System.currentTimeMillis() + (10 * 60 * 1000);
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(snoozeTime);
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        int reqCode = (name + "snooze" + System.currentTimeMillis()).hashCode();
        AlarmScheduler.scheduleDailyExact(this, reqCode, name, dose, notes, hour, minute);

        Toast.makeText(this, "Snoozed for 10 minutes", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isAlarmActive) stopAlarm();
    }
}
