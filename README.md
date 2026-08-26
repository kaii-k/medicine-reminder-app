# Medicine Reminder App

<img src="app/src/main/ic_launcher-playstore.png" alt="App icon" width="96" height="96" />

An Android application that helps users manage and track their medication schedules with exact-time alarms, snooze/done actions, and adherence reports.

## Download
[**Download the APK**](https://github.com/kaii-k/medicine-reminder-app/releases/latest) from the latest release and install it directly on an Android 7.0+ (API 24+) device (enable "Install unknown apps" for your browser/file manager first). This is a debug-signed build, intended for direct install/testing rather than Play Store distribution.

## Features
- Add, edit, and delete medicine reminders (name, dose, time, notes, repeat type, duration)
- Exact-time alarms with a full-screen alert, snooze, and mark-as-done actions
- Automatic missed-dose detection if a reminder isn't acknowledged
- Reminders persist across device reboots
- Adherence reports (taken vs. missed vs. skipped) with a pie chart

## Technology Stack
- Java
- Android SDK (`AlarmManager`, `Notification`, `SQLiteOpenHelper`)
- SQLite Database
- [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart) for report charts

## Requirements
- Android Studio (recent version) with SDK Platform 36 and Build-Tools installed
- Android 7.0 (API 24) or higher on device/emulator
- JDK 21 (bundled with recent Android Studio releases)

## Installation
1. Clone this repository:
   ```
   git clone https://github.com/kaii-k/medicine-reminder-app.git
   ```
2. Open the project folder in Android Studio and let Gradle sync.
3. Build and run on a device or emulator (`Run > Run 'app'`), or from the command line:
   ```
   ./gradlew :app:assembleDebug
   ```

## Permissions
The app requests the following at install/runtime:
- `POST_NOTIFICATIONS` – to show reminder notifications (Android 13+)
- `SCHEDULE_EXACT_ALARM` – to fire reminders at the exact scheduled time
- `RECEIVE_BOOT_COMPLETED` – to reschedule reminders after a device reboot
- `VIBRATE`, `FOREGROUND_SERVICE` – for the alarm sound/vibration while ringing

On first launch, allow notifications and, if prompted, allow exact alarms in system settings so reminders fire reliably.
