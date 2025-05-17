package com.example.tfg_beewell_app.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LocalGlucoseDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(LocalGlucoseEntry entry);

    @Query("DELETE FROM local_glucose_data WHERE timestamp < :cutoff")
    void deleteOlderThan(long cutoff);

    @Query("SELECT * FROM local_glucose_data WHERE timestamp >= :cutoff ORDER BY timestamp ASC")
    List<LocalGlucoseEntry> getLast8Hours(long cutoff);
}

