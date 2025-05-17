package com.example.tfg_beewell_app.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "local_glucose_data")
public class LocalGlucoseEntry {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public long timestamp;
    public double glucoseValue;
}

