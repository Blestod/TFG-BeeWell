/* LogbookDao.java */
package com.example.tfg_beewell_app.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LogbookDao {

    /* ────────── UPSERTS ────────── */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addInsulin(LocalInsulinEntry e);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addMeal(LocalMealEntry e);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addAct(LocalActivityEntry e);


    /* ────────── “LAST-EVENT” HELPERS (for PredictionManager) ────────── */

    /** Latest rapid- or slow-acting insulin since {@code sinceSec}. */
    @Query("SELECT * FROM insulin_local " +
            "WHERE kind = :kind AND timestampSec > :sinceSec " +
            "ORDER BY timestampSec DESC LIMIT 1")
    LocalInsulinEntry getLastInsulin(String kind, long sinceSec);

    @Query("SELECT * FROM meal_local " +
            "WHERE timestampSec > :sinceSec " +
            "ORDER BY timestampSec DESC LIMIT 1")
    LocalMealEntry getLastMeal(long sinceSec);

    @Query("SELECT * FROM activity_local " +
            "WHERE timestampSec > :sinceSec " +
            "ORDER BY timestampSec DESC LIMIT 1")
    LocalActivityEntry getLastActivity(long sinceSec);

    /* Convenience wrappers */
    default LocalInsulinEntry getLastRapidInsulin(long sinceSec){
        return getLastInsulin("rapid-acting", sinceSec);
    }
    default LocalInsulinEntry getLastSlowInsulin(long sinceSec){
        return getLastInsulin("slow-acting", sinceSec);
    }


    /* ────────── RANGE QUERIES (for chart markers) ────────── */

    /** All meals whose timestamp is **between** {@code fromSec} (inclusive) and {@code toSec} (inclusive). */
    @Query("SELECT * FROM meal_local " +
            "WHERE timestampSec BETWEEN :fromSec AND :toSec")
    List<LocalMealEntry> mealsBetween(long fromSec, long toSec);

    /** All insulin injections in the given window. */
    @Query("SELECT * FROM insulin_local " +
            "WHERE timestampSec BETWEEN :fromSec AND :toSec")
    List<LocalInsulinEntry> insulinBetween(long fromSec, long toSec);

    /** All activities in the given window. */
    @Query("SELECT * FROM activity_local " +
            "WHERE timestampSec BETWEEN :fromSec AND :toSec")
    List<LocalActivityEntry> actsBetween(long fromSec, long toSec);
}
