package com.example.tfg_beewell_app.local;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "local_glucose_data")
public class LocalGlucoseEntry {

    @PrimaryKey(autoGenerate = true)
    public int id;
    public long   timestamp;
    public double glucoseValue;

    /** ❶ Constructor vacío  */
    public LocalGlucoseEntry() { }

    /** ❷ Constructor práctico para el Worker        */
    @Ignore
    public LocalGlucoseEntry(int id, long ts, double gv){
        this.id = id;
        this.timestamp = ts;
        this.glucoseValue = gv;
    }
}
