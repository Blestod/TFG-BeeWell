/* LocalInsulinEntry.java */
package com.example.tfg_beewell_app.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "insulin_local")
public class LocalInsulinEntry {
    @PrimaryKey(autoGenerate = true) public long id;
    public int injected_id;
    public long   timestampSec;
    public double units;
    public String kind;      // "rapid-acting" | "slow-acting"
    public String spot;

    /** ← ➊ empty ctor required by Room */
    public LocalInsulinEntry() {}

    /** ← ➋ your convenient ctor (ignored by Room) */
    @Ignore
    public LocalInsulinEntry(long ts, double units, String kind, String spot) {
        this.timestampSec = ts;
        this.units        = units;
        this.kind         = kind;
        this.spot         = spot;
    }
}
