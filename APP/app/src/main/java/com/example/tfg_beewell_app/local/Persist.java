package com.example.tfg_beewell_app.local;

import android.content.Context;

import java.util.concurrent.Executors;

/**
 * Static helpers to store *and* retrieve log-book events in the Room DB
 * created by {@link GlucoseDB}.
 */
public final class Persist {

    /* ---------- INSERT helpers (you already had these) ---------- */
    private static void io(Context ctx, Runnable r) {
        Executors.newSingleThreadExecutor().execute(r);
    }
    public static void insulin (Context c, LocalInsulinEntry  e){ io(c,() -> GlucoseDB.getInstance(c).logbookDao().addInsulin(e)); }
    public static void meal    (Context c, LocalMealEntry     e){ io(c,() -> GlucoseDB.getInstance(c).logbookDao().addMeal(e));    }
    public static void act     (Context c, LocalActivityEntry e){ io(c,() -> GlucoseDB.getInstance(c).logbookDao().addAct(e));     }

    /* ---------- NEW — quick accessor for queries ---------- */
    private static LogbookDao dao(Context ctx){
        return GlucoseDB.getInstance(ctx).logbookDao();
    }

    /* ---------- LOAD latest items within a time-window ---------- */
    public static LocalMealEntry getLastMeal(Context ctx, long afterMs){
        return dao(ctx).getLastMeal(afterMs/1000L);                 // DB stores seconds
    }
    public static LocalInsulinEntry getLastRapid(Context ctx, long afterMs){
        return dao(ctx).getLastInsulin("rapid-acting", afterMs/1000L);
    }
    public static LocalInsulinEntry getLastBasal(Context ctx, long afterMs){
        return dao(ctx).getLastInsulin("slow-acting",  afterMs/1000L);
    }
    public static LocalActivityEntry getLastAct(Context ctx,long afterMs){
        return dao(ctx).getLastActivity(afterMs/1000L);
    }

    /* prevent instantiation */
    private Persist(){}
}
