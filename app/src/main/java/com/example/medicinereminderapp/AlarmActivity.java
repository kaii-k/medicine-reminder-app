package com.example.medicinereminderapp;

import android.app.KeyguardManager;
import android.content.Context;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.WindowManager;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Calendar;

public class AlarmActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private boolean isAlarmActive = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make it work on lock screen
        wakeUpDevice();
        setContentView(R.layout.activity_alarm);

        // Get medicine details from intent
        String medicineName = getIntent().getStringExtra("name");
        String dose = getIntent().getStringExtra("dose");
        String notes = getIntent().getStringExtra("notes");

        setupViews(medicineName, dose, notes);
        startAlarm();
    }

    private void wakeUpDevice() {
        // Turn on screen
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

        // Unlock device if possible
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager != null && keyguardManager.isKeyguardLocked()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                keyguardManager.requestDismissKeyguard(this, null);
            }
        }
    }

    private void setupViews(String medicineName, String dose, String notes) {
        // Find views - using your layout IDs
        TextView medicineNameText = findViewById(R.id.medicineName);
        TextView doseText = findViewById(R.id.doseText);
        TextView notesText = findViewById(R.id.notesText);
        Button dismissBtn = findViewById(R.id.dismissBtn);
        Button snoozeBtn = findViewById(R.id.snoozeBtn);

        // Set medicine information
        medicineNameText.setText(medicineName);
        doseText.setText("Dose: " + dose);

        if (notes != null && !notes.isEmpty()) {
            notesText.setText("Notes: " + notes);
        } else {
            notesText.setText("No additional notes");
        }

        // Dismiss button - stops alarm and closes activity
        dismissBtn.setOnClickListener(v -> {
            stopAlarm();
            finish();
        });

        // Snooze button - stops alarm and reschedules for 10 minutes later
        snoozeBtn.setOnClickListener(v -> {
            stopAlarm();
            snoozeAlarm();
            finish();
        });
    }

    private void startAlarm() {
        // Play alarm sound
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            mediaPlayer = MediaPlayer.create(this, alarmUri);
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
        } catch (Exception e) {
            // Log error
            android.util.Log.e("AlarmActivity", "Error playing alarm sound", e);
        }

        // Start vibration
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 1000, 1000}; // Wait 0, vibrate 1s, sleep 1s
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
            } else {
                vibrator.vibrate(pattern, 0);
            }
        }
    }

    private void stopAlarm() {
        isAlarmActive = false;

        // Stop sound
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        // Stop vibration
        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    private void snoozeAlarm() {
        // Get original alarm details
        String name = getIntent().getStringExtra("name");
        String dose = getIntent().getStringExtra("dose");
        String notes = getIntent().getStringExtra("notes");

        // Schedule alarm for 10 minutes from now
        long snoozeTime = System.currentTimeMillis() + (10 * 60 * 1000); // 10 minutes

        int reqCode = (name + "snooze" + System.currentTimeMillis()).hashCode();

        // Convert snooze time to hour and minute for the existing method
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(snoozeTime);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        // Use the CORRECT method name that exists in AlarmScheduler
        AlarmScheduler.scheduleDailyExact(this, reqCode, name, dose, notes, hour, minute);

        Toast.makeText(this, "Alarm snoozed for 10 minutes", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isAlarmActive) {
            stopAlarm();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Don't stop alarm when activity goes to background
        // Alarm should keep ringing until dismissed
    }
}