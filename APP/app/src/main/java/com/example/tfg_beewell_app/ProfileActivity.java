package com.example.tfg_beewell_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

public class ProfileActivity extends AppCompatActivity {
    String email;

    EditText heightInput, weightInput;

    EditText birthdateInput;
    Spinner sexSpinner;
    Button saveUserInfoButton, saveHeightWeightButton, changeEmailButton, changePasswordButton, deleteProfileButton;
    ImageView backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // ✅ Get email
        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        email = prefs.getString("user_email", null);

        // Bind views
        birthdateInput = findViewById(R.id.birthdate);
        sexSpinner = findViewById(R.id.sexSpinner);
        heightInput = findViewById(R.id.height);
        weightInput = findViewById(R.id.weight);
        saveUserInfoButton = findViewById(R.id.saveUserDataButton);
        saveHeightWeightButton = findViewById(R.id.saveUserVariablesButton);
        changeEmailButton = findViewById(R.id.changeEmailButton);
        changePasswordButton = findViewById(R.id.changePasswordButton);
        deleteProfileButton = findViewById(R.id.deleteProfileButton);
        backButton = findViewById(R.id.backButton);

        // === Set up Spinner with custom dropdown ===
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.sex_options,
                R.layout.spinner_item // selected item view
        );
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item); // dropdown style
        sexSpinner.setAdapter(adapter);

        birthdateInput.setText(prefs.getString("birthdate", ""));

        // Restore sex (if saved)
        String savedSex = prefs.getString("sex", "");
        if (!savedSex.isEmpty()) {
            int spinnerPosition = adapter.getPosition(savedSex);
            if (spinnerPosition >= 0) sexSpinner.setSelection(spinnerPosition);
        } else {
            sexSpinner.setSelection(0); // Hint "Select your sex"
        }

        // === Save profile data ===
        saveUserInfoButton.setOnClickListener(v -> {
            String birthdateStr = birthdateInput.getText().toString();
            String selectedSex = sexSpinner.getSelectedItem().toString();

            JSONObject body = new JSONObject();
            boolean isSomethingSent = false;

            try {
                if (!birthdateStr.isEmpty()) {
                    body.put("birth_date", Integer.parseInt(birthdateStr));
                    isSomethingSent = true;
                }
                if (selectedSex.equals("Male")) {
                    body.put("sex", false);
                    isSomethingSent = true;
                } else if (selectedSex.equals("Female")) {
                    body.put("sex", true);
                    isSomethingSent = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!isSomethingSent) {
                Toast.makeText(this, "Nothing to update", Toast.LENGTH_SHORT).show();
                return;
            }

            String url = Constants.BASE_URL + "/user/" + email;

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.PUT, url, body,
                    response -> {
                        Toast.makeText(this, "User info updated", Toast.LENGTH_SHORT).show();
                    },
                    error -> Toast.makeText(this, "Error updating user info", Toast.LENGTH_SHORT).show()
            );

            Volley.newRequestQueue(this).add(request);
        });


        saveHeightWeightButton.setOnClickListener(v -> {
            String heightStr = heightInput.getText().toString();
            String weightStr = weightInput.getText().toString();

            if (heightStr.isEmpty() && weightStr.isEmpty()) {
                Toast.makeText(this, "Nothing to update", Toast.LENGTH_SHORT).show();
                return;
            }

            JSONObject body = new JSONObject();
            StringBuilder updatedFields = new StringBuilder();

            try {
                body.put("user_email", email);
                body.put("change_date_time", System.currentTimeMillis() / 1000);

                if (!heightStr.isEmpty()) {
                    body.put("height", Double.parseDouble(heightStr));
                    updatedFields.append("Height ");
                } else {
                    body.put("height", JSONObject.NULL);
                }

                if (!weightStr.isEmpty()) {
                    body.put("weight", Double.parseDouble(weightStr));
                    updatedFields.append("Weight ");
                } else {
                    body.put("weight", JSONObject.NULL);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            final String fieldsUpdated = updatedFields.toString().trim();

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST,
                    Constants.BASE_URL + "/user_variables",
                    body,
                    response -> Toast.makeText(this, fieldsUpdated + " saved", Toast.LENGTH_SHORT).show(),
                    error -> Toast.makeText(this, "Error saving user variables", Toast.LENGTH_SHORT).show()
            );

            Volley.newRequestQueue(this).add(request);
        });


        // === Placeholder logic for unimplemented features ===
        changeEmailButton.setOnClickListener(v ->
                Toast.makeText(this, "Email change not implemented", Toast.LENGTH_SHORT).show());

        changePasswordButton.setOnClickListener(v ->
                Toast.makeText(this, "Password change not implemented", Toast.LENGTH_SHORT).show());

        // === Delete profile confirmation ===
        deleteProfileButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Profile")
                    .setMessage("Are you sure you want to permanently delete your profile?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        prefs.edit().clear().apply();
                        SharedPreferences sessionPrefs = getSharedPreferences("user_session", MODE_PRIVATE);
                        sessionPrefs.edit().clear().apply();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // === Back Button ===
        backButton.setOnClickListener(v -> finish());

        loadUserInfo(email);
        loadUserVariables(email);
    }

    private void loadUserInfo(String email) {
        String url = Constants.BASE_URL + "/user/" + email;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    int birthDate = response.optInt("birth_date", 0); // 0 = null
                    boolean hasSex = response.has("sex") && !response.isNull("sex");
                    boolean sexValue = response.optBoolean("sex", false);

                    if (birthDate > 0) {
                        birthdateInput.setText(String.valueOf(birthDate));
                    } else {
                        birthdateInput.setText(""); // shows hint
                    }

                    if (hasSex) {
                        if (sexValue) {
                            sexSpinner.setSelection(2); // Female
                        } else {
                            sexSpinner.setSelection(1); // Male
                        }
                    } else {
                        sexSpinner.setSelection(0); // "Choose an option"
                    }

                },
                error -> Toast.makeText(this, "Error loading user data", Toast.LENGTH_SHORT).show()
        );

        Volley.newRequestQueue(this).add(request);
    }

    private void loadUserVariables(String email) {
        String url = Constants.BASE_URL + "/user_variables/last/" + email;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    int height = response.optInt("height", -1);
                    int weight = response.optInt("weight", -1);

                    if (height > 0) {
                        heightInput.setText(String.valueOf(height));
                    } else {
                        heightInput.setText("");
                    }

                    if (weight > 0) {
                        weightInput.setText(String.valueOf(weight));
                    } else {
                        weightInput.setText("");
                        }
                },
                error -> Toast.makeText(this, "Error loading user data", Toast.LENGTH_SHORT).show()
        );

        Volley.newRequestQueue(this).add(request);
    }

}