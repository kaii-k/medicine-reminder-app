package com.example.medicinereminderapp;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.Manifest;

import androidx.core.content.ContextCompat;

/**
 * Central place for the runtime permissions the alarm/notification pipeline
 * needs. Without all three of these, a reminder can silently fail to make
 * any sound or show anything on modern Android versions:
 *  - POST_NOTIFICATIONS   (API 33+) or the notification never posts at all
 *  - SCHEDULE_EXACT_ALARM (API 31+) or alarms fall back to inexact/delayed
 *  - USE_FULL_SCREEN_INTENT (API 34+) or the full-screen alarm page (and the
 *    alarm sound it starts) never launches, even though the plain
 *    notification banner still shows
 *
 * ensureExactAlarmsAllowed/ensureFullScreenIntentAllowed take a Runnable
 * "onProceed" callback invoked once their dialog is dismissed (whichever
 * button was tapped), so callers can chain prompts one at a time instead of
 * firing several AlertDialogs on top of each other in the same onCreate.
 */
public class AlarmPermissions {

    public static boolean isNotificationPermissionNeeded(Activity activity) {
        return Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED;
    }

    public static void ensureExactAlarmsAllowed(Activity activity) {
        ensureExactAlarmsAllowed(activity, null);
    }

    public static void ensureExactAlarmsAllowed(Activity activity, Runnable onProceed) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) activity.getSystemService(Activity.ALARM_SERVICE);
            if (am != null && !am.canScheduleExactAlarms()) {
                AlertDialog dialog = new AlertDialog.Builder(activity)
                        .setTitle("Allow exact alarms")
                        .setMessage("To reliably notify you at the exact medicine time, please allow exact alarms for this app in system settings.")
                        .setPositiveButton("Open settings", (d, which) -> {
                            Intent i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                            if (i.resolveActivity(activity.getPackageManager()) != null) {
                                activity.startActivity(i);
                            } else {
                                openAppSettings(activity);
                            }
                        })
                        .setNegativeButton("Not now", null)
                        .create();
                if (onProceed != null) dialog.setOnDismissListener(d -> onProceed.run());
                dialog.show();
                return;
            }
        }
        if (onProceed != null) onProceed.run();
    }

    public static void ensureFullScreenIntentAllowed(Activity activity) {
        ensureFullScreenIntentAllowed(activity, null);
    }

    public static void ensureFullScreenIntentAllowed(Activity activity, Runnable onProceed) {
        if (Build.VERSION.SDK_INT >= 34) {
            NotificationManager nm = (NotificationManager) activity.getSystemService(Activity.NOTIFICATION_SERVICE);
            if (nm != null && !nm.canUseFullScreenIntent()) {
                AlertDialog dialog = new AlertDialog.Builder(activity)
                        .setTitle("Allow full-screen alarms")
                        .setMessage("To show the alarm screen and play a sound when it's time for your medicine, please allow full-screen notifications for this app in system settings.")
                        .setPositiveButton("Open settings", (d, which) -> {
                            Intent i = new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT);
                            i.setData(Uri.fromParts("package", activity.getPackageName(), null));
                            if (i.resolveActivity(activity.getPackageManager()) != null) {
                                activity.startActivity(i);
                            } else {
                                openAppSettings(activity);
                            }
                        })
                        .setNegativeButton("Not now", null)
                        .create();
                if (onProceed != null) dialog.setOnDismissListener(d -> onProceed.run());
                dialog.show();
                return;
            }
        }
        if (onProceed != null) onProceed.run();
    }

    private static void openAppSettings(Activity activity) {
        Intent appSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        appSettings.setData(Uri.fromParts("package", activity.getPackageName(), null));
        activity.startActivity(appSettings);
    }
}
