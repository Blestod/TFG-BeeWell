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

public class ProfileActivity extends AppCompatActivity {

    EditText birthdateInput, insulinTypeInput;
    Spinner sexSpinner;
    Button saveButton, changeEmailButton, changePasswordButton, deleteProfileButton;
    ImageView backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Bind views
        birthdateInput = findViewById(R.id.birthdate);
        sexSpinner = findViewById(R.id.sexSpinner);
        saveButton = findViewById(R.id.saveUserDataButton);
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

        // === Load profile data from SharedPreferences ===
        SharedPreferences prefs = getSharedPreferences("user_profile", MODE_PRIVATE);
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
        saveButton.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("birthdate", birthdateInput.getText().toString());

            String selectedSex = sexSpinner.getSelectedItem().toString();
            if (!selectedSex.equals("Select your sex")) {
                editor.putString("sex", selectedSex);
            } else {
                editor.remove("sex");
            }

            editor.apply();
            Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
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
    }
}
