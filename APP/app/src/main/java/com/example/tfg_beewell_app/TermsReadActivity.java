package com.example.tfg_beewell_app;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class TermsReadActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_termsread);

        ImageView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            finish();
        });
    }
}
