package com.example.tfg_beewell_app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Prefs {

    private static final String FILE = "beewell_prefs";
    private static final String KEY_SHOWN = "hc_perm_dialog_shown";

    public static boolean wasShown(Context ctx) {
        return ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .getBoolean(KEY_SHOWN, false);
    }

    public static void markShown(Context ctx) {
        SharedPreferences.Editor ed =
                ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit();
        ed.putBoolean(KEY_SHOWN, true);
        ed.apply();
    }
    private static final String KEY_INSULIN_SENSITIVITY = "insulin_sensitivity";
    private static final String KEY_CARB_RATIO = "carb_ratio";
    private static final String KEY_CARB_ABSORPTION_RATE = "carb_absorption_rate";

    public static double getInsulinSensitivity(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getFloat(KEY_INSULIN_SENSITIVITY, 120.0f);  // Default 120 mg/dL per unit
    }

    public static double getCarbRatio(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getFloat(KEY_CARB_RATIO, 20.0f);  // Default 20g carbs per unit insulin
    }

    public static double getCarbAbsorptionRate(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getFloat(KEY_CARB_ABSORPTION_RATE, 35.0f);  // Default 35g per hour
    }
}