package com.example.tfg_beewell_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    EditText registerEmailInput, registerPasswordInput, registerConfirmPasswordInput;
    Button registerButton;
    TextView loginRedirect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register); // Your layout file

        registerEmailInput = findViewById(R.id.registerEmailInput);
        registerPasswordInput = findViewById(R.id.registerPasswordInput);
        registerConfirmPasswordInput = findViewById(R.id.registerConfirmPasswordInput);
        registerButton = findViewById(R.id.registerButton);
        loginRedirect = findViewById(R.id.loginRedirect);

        registerButton.setOnClickListener(v -> {
            String email = registerEmailInput.getText().toString();
            String password = registerPasswordInput.getText().toString();
            String confirmPassword = registerConfirmPasswordInput.getText().toString();

            if (!email.isEmpty() && password.equals(confirmPassword)) {
                // 👉 Redirect to TermsActivity with user input
                Intent intent = new Intent(RegisterActivity.this, TermsActivity.class);
                intent.putExtra("email", email);
                intent.putExtra("password", password);
                startActivity(intent);
                finish(); // Optional: remove this if you want user to go "Back"
            } else {
                Toast.makeText(this, "Please check your inputs and try again.", Toast.LENGTH_SHORT).show();
            }
        });

        loginRedirect.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
        });
    }
}
