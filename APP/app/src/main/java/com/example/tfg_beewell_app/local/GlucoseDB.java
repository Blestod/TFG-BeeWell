package com.example.tfg_beewell_app.local;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.room.Room;

import java.util.HashMap;
import java.util.Map;

public class GlucoseDB {
    private static final Map<String, LocalGlucoseDatabase> instances = new HashMap<>();

    // ✅ Default: get from SharedPreferences
    public static synchronized LocalGlucoseDatabase getInstance(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences("user_session", Context.MODE_PRIVATE);
        String email = prefs.getString("user_email", null);
        if (email == null) {
            throw new IllegalStateException("❌ No user_email in session");
        }
        return getInstance(ctx, email);
    }

    // ✅ Explicit: provide email
    public static synchronized LocalGlucoseDatabase getInstance(Context ctx, String email) {
        if (email == null) {
            throw new IllegalArgumentException("❌ Email must not be null");
        }

        if (!instances.containsKey(email)) {
            String dbName = "glucose_db_" + email.replace("@", "_").replace(".", "_");
            LocalGlucoseDatabase db = Room.databaseBuilder(
                    ctx.getApplicationContext(),
                    LocalGlucoseDatabase.class,
                    dbName
            ).fallbackToDestructiveMigration().build();

            instances.put(email, db);
        }

        return instances.get(email);
    }

    public static synchronized void clearAllInstances() {
        instances.clear();
    }
}
