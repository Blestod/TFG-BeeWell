package com.example.tfg_beewell_app.utils;

import android.content.Context;
import android.util.Log;

import com.example.tfg_beewell_app.Forecast;
import com.example.tfg_beewell_app.local.*;
import com.example.tfg_beewell_app.models.BgReading;

import java.util.*;

import lecho.lib.hellocharts.model.PointValue;

/**
 * PredictionManager class for 1-hour glucose forecast, adjusted with latest local events.
 * This class is used in the context of diabetes management.
 */
public final class PredictionManager {

    /** Constants for rapid-acting insulin */
    private static final int RAPID_PEAK_MIN = 60, RAPID_DUR_MIN = 240;

    /** Constants for basal insulin */
    private static final int BASAL_PEAK_MIN = 120, BASAL_DUR_MIN = 720;

    /** Time window for considering meal effects */
    private static final int MEAL_WINDOW_H = 5;

    /** Time window for considering activity effects */
    private static final int ACTIVITY_WINDOW_H = 12;

    /**
     * Calculates the effect of physical activity on blood glucose.
     * @param intensity The intensity level of the activity.
     * @param durMin The duration of the activity in minutes.
     * @return The calculated effect on blood glucose.
     */
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

    /**
     * Calculates the *instantaneous* glucose-lowering effect of active insulin 
     * considering onset delay.
     * @param kind Type of insulin ("rapid" or "basal").
     * @param units Units of insulin administered.
     * @param min Minutes since administration.
     * @param isf Insulin sensitivity factor (mg/dL per unit).
     * @return Instantaneous glucose-lowering effect (mg/dL).
     */
    private static double insulinInstantEffect(String kind, double units, double min, double isf) {
        final int dur = kind.startsWith("rapid") ? RAPID_DUR_MIN : BASAL_DUR_MIN;
        final int onset = kind.startsWith("rapid") ? 10 : 30;  // onset delay (min)
        final double eff = kind.startsWith("rapid") ? isf : isf * 0.6;

        if (min < onset || min > dur || units <= 0 || isf <= 0) return 0;

        // Tiempo normalizado ajustado al retardo
        double t = (min - onset) / (dur - onset);

        // Derivada de la curva cúbica de Walsh (t ∈ [0,1])
        double dIOB_dt = (6 * t / (dur - onset)) - (6 * t * t / (dur - onset));

        // Efecto instantáneo en glucosa
        double instantEffect = units * eff * dIOB_dt;

        Log.d("PredManager", String.format(
            "Instant insulin effect: %.2f (units: %.2f, eff: %.2f, min: %.1f, t: %.2f)",
            instantEffect, units, eff, min, t
        ));

        return instantEffect;
    }


    /**
     * Calculates the *instantaneous* effect of carbohydrate absorption on blood glucose.
     * @param grams Grams of carbohydrates ingested.
     * @param min Minutes since meal ingestion.
     * @param cr Carb ratio (grams per unit of insulin).
     * @param carbh Carbohydrate absorption rate (grams per hour).
     * @return Instantaneous glucose *increase* in mg/dL.
     */
    private static double mealInstantEffect(float grams, double min, double cr, double carbh) {
        final int mealOnsetMin = 5; // delay before carbs start affecting glucose
        final double gPerHr = Math.min(carbh, Math.max(1, grams));  // prevent division by 0
        final double dur = (grams / gPerHr) * 60.0;

        if (min < mealOnsetMin || min > dur + mealOnsetMin || grams <= 0 || cr <= 0) return 0;

        // Tiempo normalizado de absorción
        double t = (min - mealOnsetMin) / dur;
        if (t < 0 || t > 1) return 0;

        // Derivada de curva cúbica de Walsh para absorción
        double dCOB_dt = (6 * t / dur) - (6 * t * t / dur);

        // Efecto sobre la glucosa (carbs absorbidos / ratio)
        double effect = grams * dCOB_dt / cr * 50.0;  // 50 mg/dL por unidad de insulina aprox.

        Log.d("PredManager", String.format(
            "Meal instant effect: %.2f (grams: %.1f, cr: %.1f, min: %.1f, t: %.2f, dur: %.1f)",
            effect, grams, cr, min, t, dur
        ));

        return effect;
    }



    /**
     * Generates a prediction for blood glucose levels over the next hour.
     * @param ctx The application context.
     * @return A list of PointValue objects representing the predicted glucose curve.
     */
    public static List<PointValue> getPredictionForNextHour(Context ctx) {
        long now = System.currentTimeMillis();

        List<LocalGlucoseEntry> rows = GlucoseDB.getInstance(ctx)
                .glucoseDao()
                .getLast8Hours(now - 8 * 3600_000L);
        List<BgReading> cgm = new ArrayList<>();
        for (LocalGlucoseEntry e : rows) {
            BgReading r = new BgReading();
            r.timestamp = e.timestamp;
            r.calculated_value = e.glucoseValue;
            cgm.add(r);
        }

        List<PointValue> curve = new Forecast.GlucoseForecast()
                .predictNextHour(cgm);
        if (curve == null || curve.isEmpty()) return curve;

        /* 3️⃣  latest local events ------------------------------------ */
        LocalMealEntry    meal  = Persist.getLastMeal (ctx, now-MEAL_WINDOW_H*3600_000L);
        LocalInsulinEntry rapid = Persist.getLastRapid(ctx, now-4*3600_000L);
        LocalInsulinEntry basal = Persist.getLastBasal(ctx, now-24*3600_000L);
        LocalActivityEntry act  = Persist.getLastAct  (ctx, now-ACTIVITY_WINDOW_H*3600_000L);

        /* 4️⃣  user factors ------------------------------------------- */
        double isf = Prefs.getInsulinSensitivity(ctx);
        double cr = Prefs.getCarbRatio(ctx);
        double carbh = Prefs.getCarbAbsorptionRate(ctx);

        /* 5️⃣  adjust every point ------------------------------------- */
        List<PointValue> adjustedCurve = new ArrayList<>();
        double previousGlucose = cgm.isEmpty() ? 100.0 : cgm.get(cgm.size() - 1).calculated_value;
        for(PointValue p:curve){
            long tMs   = (long)(p.getX()*Forecast.FUZZER);
            double y   = previousGlucose;
            Log.d("PredManager","------");
            
            // Time since events in minutes
            double dtMeal  = meal  != null ? (tMs - meal.timestampSec  * 1000L) / 60_000.0 : Double.NaN;
            double dtRapid = rapid != null ? (tMs - rapid.timestampSec * 1000L) / 60_000.0 : Double.NaN;
            double dtBasal = basal != null ? (tMs - basal.timestampSec * 1000L) / 60_000.0 : Double.NaN;
            double dtAct   = act   != null ? (tMs - act.timestampSec   * 1000L) / 60_000.0 : Double.NaN;

            if (!Double.isNaN(dtMeal) && dtMeal >= 0)
                y += mealInstantEffect(meal.grams, dtMeal, cr, carbh);
            if (!Double.isNaN(dtRapid) && dtRapid >= 0)
                y -= insulinInstantEffect(rapid.kind, rapid.units, dtRapid, isf);
            if (!Double.isNaN(dtBasal) && dtBasal >= 0)
                y -= insulinInstantEffect(basal.kind, basal.units, dtBasal, isf);
            if (!Double.isNaN(dtAct) && dtAct >= 0 && dtAct <= ACTIVITY_WINDOW_H * 60)
                y -= activityEffect(act.intensity, act.durationMin);
            // Adjust prediction limits to account for extreme hyper/hypoglycemia
            double lowerLimit = 40.0; // mg/dL (severe hypoglycemia)
            double upperLimit = 400.0; // mg/dL (severe hyperglycemia)
            y = Math.max(lowerLimit, Math.min(upperLimit, y));
            adjustedCurve.add(new PointValue(p.getX(), (float) y));
            previousGlucose = y; // feedback loop: use new prediction as base

            Log.d("PredManager", String.format("Time: %s, Predicted Glucose: %.2f mg/dL",
                Forecast.dateTimeText(tMs), y));
    
        }

        Log.d("PredManager", "Forecast adjusted with limits ✔");
        return adjustedCurve;
    }

    private PredictionManager() {}
}
