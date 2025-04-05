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

        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        String userEmail = prefs.getString("user_email", null);

        if (userEmail != null) {
            // El usuario ya está logueado → ir a MainActivity
            startActivity(new Intent(this, MainActivity.class));
        } else {
            // No hay sesión → ir a LoginActivity
            startActivity(new Intent(this, LoginActivity.class));
        }

        finish();
    }
}

