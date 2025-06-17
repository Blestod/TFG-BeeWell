package com.example.tfg_beewell_app.utils;

import android.content.Context;
import android.util.Log;

import com.example.tfg_beewell_app.Forecast;
import com.example.tfg_beewell_app.local.*;
import com.example.tfg_beewell_app.models.BgReading;

import java.util.*;

import lecho.lib.hellocharts.model.PointValue;

/** 1-hour glucose forecast, adjusted with latest local events. */
public final class PredictionManager {

    /* ───── physiology constants ───── */
    private static final int RAPID_PEAK_MIN = 60,  RAPID_DUR_MIN = 240;
    private static final int BASAL_PEAK_MIN = 120, BASAL_DUR_MIN = 720;

    private static final int MEAL_WINDOW_H     = 5;
    private static final int ACTIVITY_WINDOW_H = 12;

    /* ───── helpers ───── */
    private static double activityEffect(String intensity, int durMin) {
        int lvl;   // 0-3 → maps to 0, 0.5, 1.0, 2.0 mg/dL·min⁻¹
        if (intensity == null) {
            lvl = 1;                            // default “moderate”
        } else {
            switch (intensity.toLowerCase()) {
                case "low":        lvl = 0; break;
                case "moderate":   lvl = 1; break;
                case "high":       lvl = 2; break;
                case "very high":
                case "very_high":  lvl = 3; break;
                default:           lvl = 1;     // treat unknown as moderate
            }
        }
        double[] base = {0.0, 0.5, 1.0, 2.0};
        return base[lvl] * durMin;
    }

    private static double activeIns(String kind,double u,double min,double isf){
        int pk  = kind.startsWith("rapid")? RAPID_PEAK_MIN : BASAL_PEAK_MIN;
        int dur = kind.startsWith("rapid")? RAPID_DUR_MIN  : BASAL_DUR_MIN;
        double eff = kind.startsWith("rapid")? isf : isf*0.6;
        if(min<0||min>dur) return 0;
        return (min<=pk)? u*eff*(min/pk)
                : u*eff*(1-(min-pk)/(double)(dur-pk));
    }

    /* meals – we only know grams, so use carbs≈grams */
    private static double mealEffect(float grams,double min,double cr,double carbh){
        double gPerHr = Math.min(grams, carbh);
        double dur    = grams/gPerHr*60.0;
        if(min<0||min>dur) return 0;
        return (gPerHr/60*min)/cr*50.0;
    }

    /* ───── public ───── */
    public static List<PointValue> getPredictionForNextHour(Context ctx){
        long now = System.currentTimeMillis();

        /* 1️⃣  last 8-hour CGM ---------------------------------------- */
        List<LocalGlucoseEntry> rows = GlucoseDB.getInstance(ctx)
                .glucoseDao()
                .getLast8Hours(now-8*3600_000L);
        List<BgReading> cgm = new ArrayList<>();
        for(LocalGlucoseEntry e:rows){
            BgReading r=new BgReading();
            r.timestamp=e.timestamp;
            r.calculated_value=e.glucoseValue;
            cgm.add(r);
        }

        /* 2️⃣  raw polynomial forecast -------------------------------- */
        List<PointValue> curve = new Forecast.GlucoseForecast()
                .predictNextHour(cgm);
        if(curve==null||curve.isEmpty()) return curve;

        /* 3️⃣  latest local events ------------------------------------ */
        LocalMealEntry    meal  = Persist.getLastMeal (ctx, now-MEAL_WINDOW_H*3600_000L);
        LocalInsulinEntry rapid = Persist.getLastRapid(ctx, now-4*3600_000L);
        LocalInsulinEntry basal = Persist.getLastBasal(ctx, now-24*3600_000L);
        LocalActivityEntry act  = Persist.getLastAct  (ctx, now-ACTIVITY_WINDOW_H*3600_000L);

        /* 4️⃣  user factors ------------------------------------------- */
        double isf = Prefs.getInsulinSensitivity(ctx);
        double cr  = Prefs.getCarbRatio(ctx);
        double carbh = Prefs.getCarbAbsorptionRate(ctx);

        /* 5️⃣  adjust every point ------------------------------------- */
        for(PointValue p:curve){
            long tMs   = (long)(p.getX()*Forecast.FUZZER);
            double y   = p.getY();

            if(meal!=null){
                double dt = (tMs - meal.timestampSec*1000L)/60_000.0;
                y += mealEffect(meal.grams, dt, cr, carbh);
            }
            if(rapid!=null){
                double dt = (tMs - rapid.timestampSec*1000L)/60_000.0;
                y -= activeIns(rapid.kind, rapid.units, dt, isf);
            }
            if(basal!=null){
                double dt = (tMs - basal.timestampSec*1000L)/60_000.0;
                y -= activeIns(basal.kind, basal.units, dt, isf);
            }
            if(act!=null){
                double dt = (tMs - act.timestampSec*1000L)/60_000.0;
                if(dt>=0 && dt<=ACTIVITY_WINDOW_H*60)
                    y -= activityEffect(act.intensity, act.durationMin);
            }
            p.set(p.getX(), (float)y);
        }

        Log.d("PredManager","Forecast adjusted ✔");
        return curve;
    }

    private PredictionManager(){}
}
