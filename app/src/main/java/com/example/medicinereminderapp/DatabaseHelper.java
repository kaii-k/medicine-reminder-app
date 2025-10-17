package com.example.medicinereminderapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "smartmed.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE_MEDICINES = "medicines";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_MEDICINES + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT," +
                "dose TEXT," +
                "time TEXT," +
                "notes TEXT," +
                "status TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEDICINES);
        onCreate(db);
    }

    public boolean insertMedicine(String name, String dose, String time, String notes) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("dose", dose);
        cv.put("time", time);
        cv.put("notes", notes);
        cv.put("status", "Pending");
        return db.insert(TABLE_MEDICINES, null, cv) != -1;
    }

    public Cursor getAllMedicines() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_MEDICINES, null);
    }

    public void updateStatus(int id, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("status", status);
        db.update(TABLE_MEDICINES, cv, "id=?", new String[]{String.valueOf(id)});
    }
    // ADD THESE METHODS TO YOUR DatabaseHelper.java:

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
}