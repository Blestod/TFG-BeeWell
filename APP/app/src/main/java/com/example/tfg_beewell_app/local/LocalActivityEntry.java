/* LocalActivityEntry.java */
package com.example.tfg_beewell_app.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "activity_local")
public class LocalActivityEntry {
    @PrimaryKey(autoGenerate = true) public long id;
    public int    activity_id;
    public long   timestampSec;
    public int    durationMin;
    public String intensity;
    public String name;   // nullable
    public String type;   // nullable

    public LocalActivityEntry() {}      // ➊

    @Ignore                             // ➋
    public LocalActivityEntry(long ts,
                              int dur,
                              String intensity,
                              String name,
                              String type) {
        this.timestampSec = ts;
        this.durationMin  = dur;
        this.intensity    = intensity;
        this.name         = name;
        this.type         = type;
    }
}
