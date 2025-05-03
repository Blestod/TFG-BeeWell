package com.example.tfg_beewell_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Mostrar un layout simple para evitar pantalla negra
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> {
            SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
            String userEmail = prefs.getString("user_email", null);

            if (userEmail == null) {
                Toast.makeText(this, "🔒 Usuario no logueado", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
            } else {
                Toast.makeText(this, "✅ Usuario logueado", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, MainActivity.class));
            }

            finish();
        }, 1000);  // espera 1 segundo para permitir ver que está en Splash
    }
}
