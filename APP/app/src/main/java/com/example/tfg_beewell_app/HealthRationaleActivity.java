package com.example.tfg_beewell_app;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class HealthRationaleActivity extends AppCompatActivity {   // ⬅ mismo nombre
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView view = new TextView(this);
        view.setPadding(48, 72, 48, 72);
        view.setTextSize(16);
        view.setText(
                "Esta aplicación necesita acceder a tus datos de salud " +
                        "para poder ofrecer predicciones, consejos personalizados " +
                        "y mostrar tus constantes (glucosa, ritmo cardiaco, etc.).\n\n" +
                        "Estos datos se guardan de forma local y nunca se compartirán " +
                        "con terceros sin tu consentimiento."
        );
        setContentView(view);
    }
}
