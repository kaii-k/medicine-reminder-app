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
        Button takenBtn = findViewById(R.id.takenBtn);
        Button skipBtn = findViewById(R.id.skipBtn);
        Button snoozeBtn = findViewById(R.id.snoozeBtn);

        medNameTxt.setText(medName);
        doseTxt.setText("Dose: " + dose);
        notesTxt.setText(notes != null && !notes.isEmpty() ? "Notes: " + notes : "No additional notes");

        takenBtn.setOnClickListener(v -> {
            stopAlarm();
            sendAlarmAction(AlarmActionReceiver.ACTION_DONE);
            Toast.makeText(this, "Marked as taken", Toast.LENGTH_SHORT).show();
            finish();
        });

        skipBtn.setOnClickListener(v -> {
            stopAlarm();
            sendAlarmAction(AlarmActionReceiver.ACTION_SKIP);
            Toast.makeText(this, "Dose skipped", Toast.LENGTH_SHORT).show();
            finish();
        });

        snoozeBtn.setOnClickListener(v -> {
            stopAlarm();
            sendAlarmAction(AlarmActionReceiver.ACTION_SNOOZE);
            Toast.makeText(this, "Snoozed for 10 minutes", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    /**
     * Route Taken/Skip/Snooze through the same AlarmActionReceiver used by the
     * notification actions, using the original requestCode/medicineId from the
     * intent that launched this screen. This used to be handled ad hoc here
     * (especially Snooze, which built its own random request code and called
     * AlarmScheduler directly) which:
     *  - never cancelled the pending "missed" check, so the dose still got
     *    marked missed a few minutes later even though it was snoozed;
     *  - created a permanent, untracked daily alarm under a random id that
     *    editing/deleting the medicine could never cancel.
     */
    private void sendAlarmAction(String action) {
        Intent intent = getIntent();
        Intent actionIntent = new Intent(this, AlarmActionReceiver.class);
        actionIntent.setAction(action);
        actionIntent.putExtra("requestCode", intent.getIntExtra("requestCode", -1));
        actionIntent.putExtra("medicineId", intent.getIntExtra("medicineId", -1));
        actionIntent.putExtra("medicineName", intent.getStringExtra("name"));
        actionIntent.putExtra("dose", intent.getStringExtra("dose"));
        actionIntent.putExtra("scheduledTime", intent.getLongExtra("scheduledTime", System.currentTimeMillis()));
        sendBroadcast(actionIntent);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isAlarmActive) stopAlarm();
    }
}
