package com.example.tfg_beewell_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    EditText registerEmailInput, registerPasswordInput, registerConfirmPasswordInput;
    Button registerButton;
    TextView loginRedirect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register); // Use the actual XML file name

        registerEmailInput = findViewById(R.id.registerEmailInput);
        registerPasswordInput = findViewById(R.id.registerPasswordInput);
        registerConfirmPasswordInput = findViewById(R.id.registerConfirmPasswordInput);
        registerButton = findViewById(R.id.registerButton);
        loginRedirect = findViewById(R.id.loginRedirect);

        registerButton.setOnClickListener(v -> {
            String email = registerEmailInput.getText().toString();
            String password = registerPasswordInput.getText().toString();
            String confirmPassword = registerConfirmPasswordInput.getText().toString();

            // TODO: Validate and register user with Firebase or your backend
            if (!email.isEmpty() && password.equals(confirmPassword)) {
                // For now, go back to login
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            }
        });

        loginRedirect.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
        });
    }
}
