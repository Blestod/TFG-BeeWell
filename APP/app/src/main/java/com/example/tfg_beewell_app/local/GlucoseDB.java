package com.example.tfg_beewell_app.local;

import android.content.Context;

import androidx.room.Room;

public class GlucoseDB {
    private static LocalGlucoseDatabase instance;

    public static LocalGlucoseDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    LocalGlucoseDatabase.class,
                    "glucose_local_db"
            ).build();
        }
        return instance;
    }
}

