package com.example.medicinereminderapp;

import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.*;
import android.speech.tts.TextToSpeech;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

public class AlarmActivity extends AppCompatActivity {

    private Ringtone ringtone;
    private Vibrator vibrator;
    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Turn screen on & show when locked
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_alarm);

        String name = getIntent().getStringExtra("name");
        String dose = getIntent().getStringExtra("dose");
        String notes = getIntent().getStringExtra("notes");
        int hour = getIntent().getIntExtra("hour", 9);
        int minute = getIntent().getIntExtra("minute", 0);

        TextView title = findViewById(R.id.alarmTitle);
        TextView detail = findViewById(R.id.alarmDetail);
        Button btnTaken = findViewById(R.id.btnTaken);
        Button btnSnooze = findViewById(R.id.btnSnooze);
        Button btnDismiss = findViewById(R.id.btnDismiss);

        title.setText("Time to take your medicine!");
        detail.setText("Name: " + name + "\nDose: " + dose +
                (notes == null || notes.isEmpty() ? "" : "\nNotes: " + notes));

        startAlarmSound();
        startVibration();
        startTTS(name, dose, notes, hour, minute);

        btnTaken.setOnClickListener(v -> {
            stopAll();
            finish();
        });

        btnSnooze.setOnClickListener(v -> {
            stopAll();
            // snooze 5 minutes
            int req = (name + hour + ":" + minute).hashCode();
            long triggerAt = System.currentTimeMillis() + 5 * 60 * 1000;
            Intent i = new Intent(this, ReminderReceiver.class);
            i.putExtra("name", name); i.putExtra("dose", dose); i.putExtra("notes", notes);
            i.putExtra("hour", hour); i.putExtra("minute", minute);
            PendingIntentUtil.setExactOneShot(this, req, i, triggerAt);
            finish();
        });

        btnDismiss.setOnClickListener(v -> {
            stopAll();
            finish();
        });
    }

    private void startAlarmSound() {
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        ringtone = RingtoneManager.getRingtone(getApplicationContext(), uri);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ringtone.setLooping(true);
        }
        ringtone.play();
    }

    private void startVibration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = vm.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }
        long[] pattern = {0, 800, 400, 800, 400};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect effect = VibrationEffect.createWaveform(pattern, 0);
            vibrator.vibrate(effect);
        } else {
            vibrator.vibrate(pattern, 0);
        }
    }

    private void startTTS(String name, String dose, String notes, int hour, int minute) {
        String hh = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
        String speech = "It is " + hh + ". Please take your medicine. Name: " + name + ". Dose: " + dose + ". " + (notes == null || notes.isEmpty() ? "" : "Note: " + notes + ". ");
        tts = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.ERROR) {
                tts.setLanguage(Locale.US);
                tts.speak(speech, TextToSpeech.QUEUE_FLUSH, null, "smartmed_alarm_tts");
            }
        });
    }

    private void stopAll() {
        try { if (ringtone != null && ringtone.isPlaying()) ringtone.stop(); } catch (Exception ignore) {}
        try { if (vibrator != null) vibrator.cancel(); } catch (Exception ignore) {}
        try { if (tts != null) { tts.stop(); tts.shutdown(); } } catch (Exception ignore) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAll();
    }
}