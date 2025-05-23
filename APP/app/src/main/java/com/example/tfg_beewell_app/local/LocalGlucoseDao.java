package com.example.tfg_beewell_app.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;
@Dao
public interface LocalGlucoseDao {

    /* ────────── inserciones ────────── */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(LocalGlucoseEntry e);          // ya lo tenías

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(List<LocalGlucoseEntry> list); // ⚠️  NUEVO

    /* ────────── borrado ───────────────*/
    @Query("DELETE FROM local_glucose_data")
    void deleteAll();                          // ⚠️  NUEVO

    @Query("DELETE FROM local_glucose_data WHERE timestamp < :cutoff")
    void deleteOlderThan(long cutoff);         // ya lo tenías

    /* Helper atómico: borra todo y mete la lista nueva */
    @Transaction
    default void replaceAll(List<LocalGlucoseEntry> list){
        deleteAll();
        insert(list);
    }

    /* ────────── lecturas para la gráfica ────────── */
    @Query("SELECT * FROM local_glucose_data " +
            "WHERE timestamp BETWEEN :from AND :to " +
            "ORDER BY timestamp ASC")
    List<LocalGlucoseEntry> range(long from, long to);

    @Query("SELECT * FROM local_glucose_data WHERE timestamp >= :cutoff ORDER BY timestamp ASC")
    List<LocalGlucoseEntry> getLast8Hours(long cutoff); // ya lo tenías
}
