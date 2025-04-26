package com.example.tfg_beewell_app.utils;

import android.content.Context;
import android.content.SharedPreferences;

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
}
