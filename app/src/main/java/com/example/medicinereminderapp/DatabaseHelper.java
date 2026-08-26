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

    // Per-day adherence outcome shown in the report calendar.
    public enum DayStatus { TAKEN, MISSED, SKIPPED, NONE }

    /**
     * Per-day adherence status for a medicine across [startOfFirstDay, startOfLastDay]
     * (both are day-start timestamps, oldest -> newest, inclusive). A day with both a
     * taken and a missed/skipped record (e.g. multiple doses that day) reports TAKEN,
     * since that's the more useful signal for a quick adherence overview.
     *
     * Uses scheduled_time column (milliseconds since epoch).
     */
    public List<DayStatus> getDailyStatus(int medicineId, long startOfFirstDay, long startOfLastDay) {
        List<DayStatus> result = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(startOfFirstDay);

        while (cal.getTimeInMillis() <= startOfLastDay) {
            long startTs = cal.getTimeInMillis();
            java.util.Calendar dayEnd = (java.util.Calendar) cal.clone();
            dayEnd.add(java.util.Calendar.DAY_OF_YEAR, 1);
            long endTs = dayEnd.getTimeInMillis() - 1;

            String query = "SELECT status, COUNT(*) FROM " + TABLE_DOSE_HISTORY +
                    " WHERE medicine_id = ? AND scheduled_time BETWEEN ? AND ? GROUP BY status";
            Cursor c = db.rawQuery(query, new String[]{
                    String.valueOf(medicineId), String.valueOf(startTs), String.valueOf(endTs)
            });
            boolean hasTaken = false, hasMissed = false, hasSkipped = false;
            while (c.moveToNext()) {
                String status = c.getString(0);
                int count = c.getInt(1);
                if (count <= 0 || status == null) continue;
                if (status.equals("taken")) hasTaken = true;
                else if (status.equals("missed")) hasMissed = true;
                else if (status.equals("skipped")) hasSkipped = true;
            }
            c.close();

            if (hasTaken) result.add(DayStatus.TAKEN);
            else if (hasMissed) result.add(DayStatus.MISSED);
            else if (hasSkipped) result.add(DayStatus.SKIPPED);
            else result.add(DayStatus.NONE);

            cal.add(java.util.Calendar.DAY_OF_YEAR, 1);
        }

        return result;
    }

    // Simple id+name pair used to populate the medicine picker on the report screen
    public static class MedicineBasic {
        public int id;
        public String name;
    }

    public List<MedicineBasic> getAllMedicinesBasic() {
        List<MedicineBasic> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id, name FROM " + TABLE_MEDICINES + " ORDER BY name ASC", null);
        while (c.moveToNext()) {
            MedicineBasic m = new MedicineBasic();
            m.id = c.getInt(0);
            m.name = c.getString(1);
            list.add(m);
        }
        c.close();
        return list;
    }

    /**
     * Day-start timestamps [firstDay, lastDay] to render in the adherence calendar
     * for a medicine: from its first ever scheduled dose to min(end_date, today).
     * Returns null if the medicine has no dose_history yet (nothing to show).
     */
    public long[] getMedicineCalendarRange(int medicineId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT MIN(scheduled_time) FROM " + TABLE_DOSE_HISTORY + " WHERE medicine_id = ?",
                new String[]{String.valueOf(medicineId)});
        long firstScheduled = 0;
        if (c.moveToFirst() && !c.isNull(0)) {
            firstScheduled = c.getLong(0);
        }
        c.close();
        if (firstScheduled <= 0) return null;

        long endDate = 0;
        Cursor m = getMedicineById(medicineId);
        if (m != null && m.moveToFirst()) {
            try {
                endDate = m.getLong(m.getColumnIndexOrThrow("end_date"));
            } catch (Exception ignored) {}
            m.close();
        }

        long now = System.currentTimeMillis();
        long lastTs = (endDate > 0 && endDate < now) ? endDate : now;

        java.util.Calendar firstCal = java.util.Calendar.getInstance();
        firstCal.setTimeInMillis(firstScheduled);
        normalizeToStartOfDay(firstCal);

        java.util.Calendar lastCal = java.util.Calendar.getInstance();
        lastCal.setTimeInMillis(lastTs);
        normalizeToStartOfDay(lastCal);

        // Cap the range to the most recent 120 days so the calendar stays a
        // reasonable size for very long "Ongoing" durations.
        java.util.Calendar cappedFirstCal = (java.util.Calendar) lastCal.clone();
        cappedFirstCal.add(java.util.Calendar.DAY_OF_YEAR, -119);
        if (firstCal.before(cappedFirstCal)) {
            firstCal = cappedFirstCal;
        }

        if (firstCal.after(lastCal)) return null;
        return new long[]{firstCal.getTimeInMillis(), lastCal.getTimeInMillis()};
    }

    private void normalizeToStartOfDay(java.util.Calendar cal) {
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
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