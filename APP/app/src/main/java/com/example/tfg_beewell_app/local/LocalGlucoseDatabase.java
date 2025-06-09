package com.example.tfg_beewell_app.local;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = {
                LocalGlucoseEntry.class,
                LocalGlucoseHistoryEntry.class
        },
        version = 3,
        exportSchema = false
)
public abstract class LocalGlucoseDatabase extends RoomDatabase {

    private static LocalGlucoseDatabase instance;

    public abstract LocalGlucoseDao glucoseDao();
    public abstract LocalGlucoseHistoryDao historyDao();

    public static synchronized LocalGlucoseDatabase getInstance(Context context) {
        if (instance == null) {
            SharedPreferences prefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE);
            String email = prefs.getString("user_email", null);
            if (email == null) {
                throw new IllegalStateException("No user email found in session");
            }

            // Create a user-specific database name
            String dbName = "glucose_db_" + email.replace("@", "_").replace(".", "_");

            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            LocalGlucoseDatabase.class,
                            dbName
                    )
                    .fallbackToDestructiveMigration()
                    .build();
        }

        return instance;
    }

    public static synchronized void destroyInstance() {
        instance = null;
    }
}
