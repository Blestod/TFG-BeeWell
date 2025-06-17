/* LocalMealEntry.java */
package com.example.tfg_beewell_app.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "meal_local")
public class LocalMealEntry {
    @PrimaryKey(autoGenerate = true) public long id;
    public long  timestampSec;
    public float grams;
    public int   foodId;
    public String foodName;     // nullable

    public LocalMealEntry() {}          // ➊

    @Ignore                             // ➋
    public LocalMealEntry(long ts, float g, int id, String name) {
        this.timestampSec = ts;
        this.grams        = g;
        this.foodId       = id;
        this.foodName     = name;
    }
}
