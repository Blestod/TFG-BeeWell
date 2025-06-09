package com.example.tfg_beewell_app.local;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.tfg_beewell_app.local.LocalGlucoseHistoryEntry;

import java.util.List;

@Dao
public interface LocalGlucoseHistoryDao {
    @Query("SELECT COUNT(*) FROM local_glucose_history")
    long count();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(List<LocalGlucoseHistoryEntry> rows);

    @Query("DELETE FROM local_glucose_history")
    void deleteAll();

    @Transaction
    default void replaceAll(List<LocalGlucoseHistoryEntry> rows) {
        deleteAll();
        insert(rows);
    }

    @Query("SELECT * FROM local_glucose_history WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp ASC")
    List<LocalGlucoseHistoryEntry> range(long from, long to);

    // ─── NEW ───
    @Query("SELECT MIN(timestamp) FROM local_glucose_history")
    long getMinTimestamp();

    @Query("SELECT MAX(timestamp) FROM local_glucose_history")
    long getMaxTimestamp();

    @Query("SELECT * FROM local_glucose_history ORDER BY timestamp ASC")
    List<LocalGlucoseHistoryEntry> getAll();
}

