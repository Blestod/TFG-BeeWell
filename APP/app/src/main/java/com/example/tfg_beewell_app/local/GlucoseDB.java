package com.example.tfg_beewell_app.local;

import android.content.Context;

import androidx.room.Room;

public class GlucoseDB {
    private static LocalGlucoseDatabase instance;
    public static LocalGlucoseDatabase getInstance(Context ctx) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            ctx.getApplicationContext(),
                            LocalGlucoseDatabase.class,
                            "glucose_local_db"
                    )
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}