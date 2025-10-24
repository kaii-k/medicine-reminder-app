package com.example.medicinereminderapp;

public class MedicineReport {
    public int medicineId;
    public String medicineName;
    public String dose;
    public int takenCount;
    public int missedCount;
    public int totalDoses;

    public double getAdherenceRate() {
        if (totalDoses == 0) return 0.0;
        return (takenCount * 100.0) / totalDoses;
    }

    public String getAdherenceRateFormatted() {
        return String.format("%.1f%%", getAdherenceRate());
    }
}