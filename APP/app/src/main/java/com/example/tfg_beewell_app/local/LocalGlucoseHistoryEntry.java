package com.example.tfg_beewell_app.local;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "local_glucose_history")
public class LocalGlucoseHistoryEntry {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int timestamp;
    public int glucoseValue;

    /** Room necesita un constructor vacío */
    public LocalGlucoseHistoryEntry() { }

    /** Constructor de conveniencia */
    @Ignore
    public LocalGlucoseHistoryEntry(int id, int timestamp, int glucoseValue) {
        this.id = id;
        this.timestamp = timestamp;
        this.glucoseValue = glucoseValue;
    }
}
