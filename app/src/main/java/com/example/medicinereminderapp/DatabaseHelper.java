package com.example.medicinereminderapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "smartmed.db";
    private static final int DB_VERSION = 2; // Incremented version
    private static final String TABLE_MEDICINES = "medicines";
    private static final String TABLE_DOSE_HISTORY = "dose_history";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create medicines table
        db.execSQL("CREATE TABLE " + TABLE_MEDICINES + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT," +
                "dose TEXT," +
                "time TEXT," +
                "notes TEXT," +
                "status TEXT," +
                "repeat_type TEXT DEFAULT 'Daily'," +
                "selected_days TEXT DEFAULT 'All'," +
                "duration TEXT DEFAULT 'Ongoing'," +
                "end_date INTEGER DEFAULT 0)");

        // Create dose history table for reporting
        db.execSQL("CREATE TABLE " + TABLE_DOSE_HISTORY + " (" +
                "history_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "medicine_id INTEGER," +
                "medicine_name TEXT," +
                "dose TEXT," +
                "scheduled_time INTEGER," +
                "taken_time INTEGER," +
                "status TEXT," + // 'taken', 'missed', 'skipped'
                "FOREIGN KEY(medicine_id) REFERENCES " + TABLE_MEDICINES + "(id))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Add new columns to medicines table
            db.execSQL("ALTER TABLE " + TABLE_MEDICINES + " ADD COLUMN repeat_type TEXT DEFAULT 'Daily'");
            db.execSQL("ALTER TABLE " + TABLE_MEDICINES + " ADD COLUMN selected_days TEXT DEFAULT 'All'");
            db.execSQL("ALTER TABLE " + TABLE_MEDICINES + " ADD COLUMN duration TEXT DEFAULT 'Ongoing'");
            db.execSQL("ALTER TABLE " + TABLE_MEDICINES + " ADD COLUMN end_date INTEGER DEFAULT 0");

            // Create dose history table
            db.execSQL("CREATE TABLE " + TABLE_DOSE_HISTORY + " (" +
                    "history_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "medicine_id INTEGER," +
                    "medicine_name TEXT," +
                    "dose TEXT," +
                    "scheduled_time INTEGER," +
                    "taken_time INTEGER," +
                    "status TEXT," +
                    "FOREIGN KEY(medicine_id) REFERENCES " + TABLE_MEDICINES + "(id))");
        }
    }

    // Enhanced insert method with repeat options
    public long insertMedicineWithDetails(String name, String dose, String time, String notes,
                                          String repeatType, String selectedDays, String duration, long endDate) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("dose", dose);
        cv.put("time", time);
        cv.put("notes", notes);
        cv.put("status", "Pending");
        cv.put("repeat_type", repeatType);
        cv.put("selected_days", selectedDays);
        cv.put("duration", duration);
        cv.put("end_date", endDate);

        return db.insert(TABLE_MEDICINES, null, cv);
    }

    // Keep your existing methods for backward compatibility
    public boolean insertMedicine(String name, String dose, String time, String notes) {
        return insertMedicineWithDetails(name, dose, time, notes, "Daily", "All", "Ongoing", 0) != -1;
    }

    // Record dose history for reporting
    public void recordDoseHistory(int medicineId, String medicineName, String dose,
                                  long scheduledTime, long takenTime, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("medicine_id", medicineId);
        cv.put("medicine_name", medicineName);
        cv.put("dose", dose);
        cv.put("scheduled_time", scheduledTime);
        cv.put("taken_time", takenTime);
        cv.put("status", status);

        db.insert(TABLE_DOSE_HISTORY, null, cv);
    }

    // Get weekly report
    public ReportData getWeeklyReport(int medicineId) {
        SQLiteDatabase db = this.getReadableDatabase();

        // Calculate start of week (7 days ago)
        long weekStart = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);

        String query = "SELECT " +
                "SUM(CASE WHEN status = 'taken' THEN 1 ELSE 0 END) as taken_count, " +
                "SUM(CASE WHEN status = 'missed' THEN 1 ELSE 0 END) as missed_count, " +
                "SUM(CASE WHEN status = 'skipped' THEN 1 ELSE 0 END) as skipped_count, " +
                "COUNT(*) as total_doses " +
                "FROM " + TABLE_DOSE_HISTORY +
                " WHERE medicine_id = ? AND scheduled_time >= ?";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(medicineId), String.valueOf(weekStart)});

        ReportData report = new ReportData();
        if (cursor.moveToFirst()) {
            report.takenCount = cursor.getInt(0);
            report.missedCount = cursor.getInt(1);
            report.skippedCount = cursor.getInt(2);
            report.totalDoses = cursor.getInt(3);
        }
        cursor.close();
        return report;
    }

    // Get monthly report
    public ReportData getMonthlyReport(int medicineId) {
        SQLiteDatabase db = this.getReadableDatabase();

        // Calculate start of month (30 days ago)
        long monthStart = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000);

        String query = "SELECT " +
                "SUM(CASE WHEN status = 'taken' THEN 1 ELSE 0 END) as taken_count, " +
                "SUM(CASE WHEN status = 'missed' THEN 1 ELSE 0 END) as missed_count, " +
                "SUM(CASE WHEN status = 'skipped' THEN 1 ELSE 0 END) as skipped_count, " +
                "COUNT(*) as total_doses " +
                "FROM " + TABLE_DOSE_HISTORY +
                " WHERE medicine_id = ? AND scheduled_time >= ?";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(medicineId), String.valueOf(monthStart)});

        ReportData report = new ReportData();
        if (cursor.moveToFirst()) {
            report.takenCount = cursor.getInt(0);
            report.missedCount = cursor.getInt(1);
            report.skippedCount = cursor.getInt(2);
            report.totalDoses = cursor.getInt(3);
        }
        cursor.close();
        return report;
    }
    // Check if any medicine exists (useful to show/hide Report button)
    public boolean hasMedicines() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_MEDICINES, null);
        boolean has = false;
        if (cursor.moveToFirst()) {
            has = cursor.getInt(0) > 0;
        }
        cursor.close();
        return has;
    }

    /**
     * Return last 7 days status for a medicine (oldest -> newest).
     * - Boolean.TRUE  -> at least one 'taken' record that day
     * - Boolean.FALSE -> at least one scheduled dose that day but none taken (so missed)
     * - null          -> no scheduled dose recorded that day
     *
     * Uses scheduled_time column (milliseconds since epoch).
     */
    public List<Boolean> getLast7DaysTakenStatus(int medicineId) {
        List<Boolean> result = new ArrayList<>(7);
        SQLiteDatabase db = this.getReadableDatabase();

        // Start from 6 days ago (oldest) to today (newest)
        java.util.Calendar cal = java.util.Calendar.getInstance();
        // normalize to start of today (midnight)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);

        // loop days: i = 6 -> 0 gives oldest -> newest
        for (int daysBack = 6; daysBack >= 0; daysBack--) {
            java.util.Calendar dayStart = (java.util.Calendar) cal.clone();
            dayStart.add(java.util.Calendar.DAY_OF_YEAR, -daysBack);
            long startTs = dayStart.getTimeInMillis();

            java.util.Calendar dayEnd = (java.util.Calendar) dayStart.clone();
            dayEnd.add(java.util.Calendar.DAY_OF_YEAR, 1);
            long endTs = dayEnd.getTimeInMillis() - 1;

            // 1) Check if there is any 'taken' record for that day
            String takenQuery = "SELECT COUNT(*) FROM " + TABLE_DOSE_HISTORY +
                    " WHERE medicine_id = ? AND scheduled_time BETWEEN ? AND ? AND status = 'taken'";
            Cursor cTaken = db.rawQuery(takenQuery, new String[]{
                    String.valueOf(medicineId),
                    String.valueOf(startTs),
                    String.valueOf(endTs)
            });
            int takenCount = 0;
            if (cTaken.moveToFirst()) takenCount = cTaken.getInt(0);
            cTaken.close();

            if (takenCount > 0) {
                result.add(Boolean.TRUE);
                continue;
            }

            // 2) If no taken, check if there was any scheduled dose that day
            String anyQuery = "SELECT COUNT(*) FROM " + TABLE_DOSE_HISTORY +
                    " WHERE medicine_id = ? AND scheduled_time BETWEEN ? AND ?";
            Cursor cAny = db.rawQuery(anyQuery, new String[]{
                    String.valueOf(medicineId),
                    String.valueOf(startTs),
                    String.valueOf(endTs)
            });
            int anyCount = 0;
            if (cAny.moveToFirst()) anyCount = cAny.getInt(0);
            cAny.close();

            if (anyCount > 0) {
                // there was a scheduled dose, but none taken -> missed
                result.add(Boolean.FALSE);
            } else {
                // no scheduled dose recorded for this day
                result.add(null);
            }
        }

        return result;
    }

    // Get all reports for doctor view
    public List<MedicineReport> getAllMedicineReports() {
        List<MedicineReport> reports = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT m.id, m.name, m.dose, " +
                "(SELECT COUNT(*) FROM " + TABLE_DOSE_HISTORY + " h WHERE h.medicine_id = m.id AND h.status = 'taken') as taken_count, " +
                "(SELECT COUNT(*) FROM " + TABLE_DOSE_HISTORY + " h WHERE h.medicine_id = m.id AND h.status = 'missed') as missed_count, " +
                "(SELECT COUNT(*) FROM " + TABLE_DOSE_HISTORY + " h WHERE h.medicine_id = m.id) as total_doses " +
                "FROM " + TABLE_MEDICINES + " m";

        Cursor cursor = db.rawQuery(query, null);

        while (cursor.moveToNext()) {
            MedicineReport report = new MedicineReport();
            report.medicineId = cursor.getInt(0);
            report.medicineName = cursor.getString(1);
            report.dose = cursor.getString(2);
            report.takenCount = cursor.getInt(3);
            report.missedCount = cursor.getInt(4);
            report.totalDoses = cursor.getInt(5);
            reports.add(report);
        }
        cursor.close();
        return reports;
    }

    // Your existing methods (keep them as they are)
    public Cursor getAllMedicines() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_MEDICINES + " ORDER BY time ASC", null);
    }

    public void updateStatus(int id, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("status", status);
        db.update(TABLE_MEDICINES, cv, "id=?", new String[]{String.valueOf(id)});
    }

    public boolean deleteMedicine(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_MEDICINES, "id=?", new String[]{String.valueOf(id)}) > 0;
    }

    public boolean updateMedicine(int id, String name, String dose, String time, String notes) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("dose", dose);
        cv.put("time", time);
        cv.put("notes", notes);
        return db.update(TABLE_MEDICINES, cv, "id=?", new String[]{String.valueOf(id)}) > 0;
    }

    public Cursor getMedicineById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_MEDICINES + " WHERE id=?", new String[]{String.valueOf(id)});
    }

    // Enhanced update method with repeat options - ONLY ONE VERSION
    public boolean updateMedicineWithDetails(int id, String name, String dose, String time, String notes,
                                             String repeatType, String selectedDays, String duration, long endDate) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("dose", dose);
        cv.put("time", time);
        cv.put("notes", notes);
        cv.put("repeat_type", repeatType);
        cv.put("selected_days", selectedDays);
        cv.put("duration", duration);
        cv.put("end_date", endDate);

        int result = db.update(TABLE_MEDICINES, cv, "id=?", new String[]{String.valueOf(id)});
        db.close();
        return result > 0;
    }
    // inside DatabaseHelper class

    // small DTO for rescheduling
    public static class ScheduledAlarm {
        public int requestCode;
        public int medicineId;
        public long triggerAtMs;
        public String medicineName;
        public String dose;
    }

    // Example: return all future scheduled alarms saved in medicines table
    public List<ScheduledAlarm> getAllScheduledAlarms() {
        List<ScheduledAlarm> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Adjust columns according to your schema: example assumes you save next_trigger (millis) and requestCode
        String q = "SELECT id, name, dose, end_date, time /* replace with your trigger/time column */ FROM " + TABLE_MEDICINES;
        Cursor c = db.rawQuery(q, null);
        while (c.moveToNext()) {
            // You'll need to compute next trigger time from your saved schedule data.
            // This is placeholder code — adapt to how you store schedule/time.
            ScheduledAlarm s = new ScheduledAlarm();
            s.medicineId = c.getInt(0);
            s.medicineName = c.getString(1);
            s.dose = c.getString(2);
            s.requestCode = c.getInt(0); // use medicine id as requestCode if you do so
            // compute triggerAtMs from your columns (time + date / repeat rules)
            s.triggerAtMs = /* compute next trigger millis for this medicine */ System.currentTimeMillis();
            list.add(s);
        }
        c.close();
        return list;
    }

}