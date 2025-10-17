package com.example.medicinereminderapp;

import android.app.*;
import android.content.*;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class ReminderReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "smartmed_alarm_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        String name = intent.getStringExtra("name");
        String dose = intent.getStringExtra("dose");
        String notes = intent.getStringExtra("notes");
        int hour = intent.getIntExtra("hour", 9);
        int minute = intent.getIntExtra("minute", 0);

        createChannels(context);

        // Full-screen intent to AlarmActivity
        Intent alarm = new Intent(context, AlarmActivity.class);
        alarm.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        alarm.putExtras(intent.getExtras());

        PendingIntent fullScreenPI = PendingIntent.getActivity(
                context, (name + hour + ":" + minute).hashCode(),
                alarm, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        // Build high-priority notification
        NotificationCompat.Builder nb = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Time to take " + name)
                .setContentText("Dose: " + dose)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setFullScreenIntent(fullScreenPI, true);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify((int) System.currentTimeMillis(), nb.build());

        // Launch the full-screen activity immediately
        context.startActivity(alarm);

        // Re-schedule for tomorrow
        int req = (name + hour + ":" + minute).hashCode();
        AlarmScheduler.rescheduleNextDay(context, req, intent);
    }

    private void createChannels(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            android.media.AudioAttributes attrs = new android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "SmartMed Alarms", NotificationManager.IMPORTANCE_HIGH);
            ch.enableVibration(true);
            ch.setSound(android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI, attrs);
            nm.createNotificationChannel(ch);
        }
    }
}