package com.example.tfg_beewell_app.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {LocalGlucoseEntry.class}, version = 1, exportSchema = false)
public abstract class LocalGlucoseDatabase extends RoomDatabase {
    public abstract LocalGlucoseDao glucoseDao();
}

