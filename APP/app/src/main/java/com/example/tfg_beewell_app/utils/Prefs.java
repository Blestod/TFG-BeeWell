package com.example.tfg_beewell_app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Prefs {

    /* small dialog-flag you already had --------------------------- */
    private static final String FILE       = "beewell_prefs";
    private static final String KEY_SHOWN  = "hc_perm_dialog_shown";

    public static boolean wasShown(Context ctx){
        return ctx.getSharedPreferences(FILE,Context.MODE_PRIVATE)
                .getBoolean(KEY_SHOWN,false);
    }
    public static void markShown(Context ctx){
        ctx.getSharedPreferences(FILE,Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_SHOWN,true).apply();
    }

    /* user-variables (ISF, CR, CAR) ------------------------------- */
    private static final String KEY_INSULIN_SENSITIVITY = "insulin_sensitivity";
    private static final String KEY_CARB_RATIO          = "carb_ratio";
    private static final String KEY_CARB_ABS_RATE       = "carb_absorption_rate";

    /* ----- getters (unchanged defaults) ----- */
    public static double getInsulinSensitivity(Context ctx){
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(ctx);
        return p.getFloat(KEY_INSULIN_SENSITIVITY,120f);     // mg/dL · U⁻¹
    }
    public static double getCarbRatio(Context ctx){
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(ctx);
        return p.getFloat(KEY_CARB_RATIO,20f);               // g CHO · U⁻¹
    }
    public static double getCarbAbsorptionRate(Context ctx){
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(ctx);
        return p.getFloat(KEY_CARB_ABS_RATE,35f);            // g CHO · h⁻¹
    }

    /* ----- setters (NEW) ----- */
    public static void setInsulinSensitivity(Context ctx,double v){
        PreferenceManager.getDefaultSharedPreferences(ctx)
                .edit().putFloat(KEY_INSULIN_SENSITIVITY,(float)v).apply();
    }
    public static void setCarbRatio(Context ctx,double v){
        PreferenceManager.getDefaultSharedPreferences(ctx)
                .edit().putFloat(KEY_CARB_RATIO,(float)v).apply();
    }
    public static void setCarbAbsorptionRate(Context ctx,double v){
        PreferenceManager.getDefaultSharedPreferences(ctx)
                .edit().putFloat(KEY_CARB_ABS_RATE,(float)v).apply();
    }

    // Turorial flag
    private static final String KEY_TUTORIAL_SHOWN = "tutorial_shown";

    public static boolean wasTutorialShown(Context ctx) {
        return ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .getBoolean(KEY_TUTORIAL_SHOWN, false);
    }

    public static void markTutorialShown(Context ctx) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_TUTORIAL_SHOWN, true).apply();
    }


}
