package com.example.tfg_beewell_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MainEntryActivity extends AppCompatActivity {

    private static final String PREF_NAME = "BeeWellPrefs";
    private static final String KEY_FIRST_LAUNCH = "isFirstLaunch";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean isFirstLaunch = prefs.getBoolean(KEY_FIRST_LAUNCH, true);

        if (isFirstLaunch) {
            // Go to LoginActivity
            startActivity(new Intent(this, LoginActivity.class));

            // Update flag
            prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
        } else {
            // Go directly to your MainActivity
            startActivity(new Intent(this, MainActivity.class));
        }

        finish(); // Prevent this activity from showing again
    }
}
