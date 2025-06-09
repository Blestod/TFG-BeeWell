package com.example.tfg_beewell_app.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.example.tfg_beewell_app.LoginActivity;
import com.example.tfg_beewell_app.local.LocalGlucoseDatabase;

public class SessionManager {

    public static void logout(Context context) {
        // 1. Clear stored email
        SharedPreferences prefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();

        // 2. Reset the current Room DB instance
        LocalGlucoseDatabase.destroyInstance();

        // 3. Redirect to Login screen
        Intent i = new Intent(context, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(i);
    }
}
