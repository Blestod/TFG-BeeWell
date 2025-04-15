package com.example.tfg_beewell_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class TermsActivity extends AppCompatActivity {

    CheckBox checkbox;
    Button continueButton;

    String email;
    String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms);

        checkbox = findViewById(R.id.checkbox);
        continueButton = findViewById(R.id.continueButton);

        // ✅ Get email & password from intent
        email = getIntent().getStringExtra("email");
        password = getIntent().getStringExtra("password");

        continueButton.setOnClickListener(v -> {
            if (checkbox.isChecked()) {
                String email = getIntent().getStringExtra("email");
                String password = getIntent().getStringExtra("password");

                if (email == null || password == null) {
                    Toast.makeText(this, "Missing registration data.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Now make registration API call
                JSONObject jsonBody = new JSONObject();
                try {
                    jsonBody.put("email", email);
                    jsonBody.put("password", password);
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error preparing request", Toast.LENGTH_SHORT).show();
                    return;
                }

                RequestQueue queue = Volley.newRequestQueue(this);
                String url = Constants.BASE_URL + "/user";

                JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                        response -> {
                            // ✅ Success
                            SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putBoolean("accepted_terms", true);
                            editor.putString("user_email", email); // Optional
                            editor.apply();

                            startActivity(new Intent(TermsActivity.this, MainActivity.class));
                            finish();
                        },
                        error -> {
                            // ❌ Failure
                            Toast.makeText(this, "Email already registered or error occurred", Toast.LENGTH_SHORT).show();

                            Intent backToRegister = new Intent(TermsActivity.this, RegisterActivity.class);
                            backToRegister.putExtra("email", email);
                            startActivity(backToRegister);
                            finish();
                        }
                );

                queue.add(request);

            } else {
                Toast.makeText(this, "You must accept the terms and conditions to continue", Toast.LENGTH_SHORT).show();
            }
        });

    }
}
