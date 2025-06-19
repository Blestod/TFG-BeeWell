package com.example.tfg_beewell_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.tfg_beewell_app.local.GlucoseDB;
import com.example.tfg_beewell_app.utils.Prefs;        // ← NEW
import org.json.JSONObject;

import java.io.File;

public class ProfileActivity extends AppCompatActivity {

    /* ────── FIELDS ────── */
    private String email;

    private EditText heightInput, weightInput, insulinSensitivityInput,
            carbRatioInput, carbAbsorptionInput, birthdateInput;
    private Spinner  sexSpinner;
    private Button   saveUserInfoButton, saveHeightWeightButton,
            changeEmailButton, changePasswordButton, deleteProfileButton;
    private ImageView backButton;

    /* ────── LIFECYCLE ────── */
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        /* session */
        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        email = prefs.getString("user_email", null);

        /* wiring */
        birthdateInput          = findViewById(R.id.birthdate);
        sexSpinner              = findViewById(R.id.sexSpinner);
        heightInput             = findViewById(R.id.height);
        weightInput             = findViewById(R.id.weight);
        insulinSensitivityInput = findViewById(R.id.insulin_sensitivity);
        carbRatioInput          = findViewById(R.id.carb_ratio);
        carbAbsorptionInput     = findViewById(R.id.carb_absorption);

        saveUserInfoButton      = findViewById(R.id.saveUserDataButton);
        saveHeightWeightButton  = findViewById(R.id.saveUserVariablesButton);
        changeEmailButton       = findViewById(R.id.changeEmailButton);
        changePasswordButton    = findViewById(R.id.changePasswordButton);
        deleteProfileButton     = findViewById(R.id.deleteProfileButton);
        backButton              = findViewById(R.id.backButton);

        /* spinner */
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.sex_options, R.layout.spinner_item);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        sexSpinner.setAdapter(adapter);

        birthdateInput.setText(prefs.getString("birthdate", ""));
        String savedSex = prefs.getString("sex", "");
        sexSpinner.setSelection(!savedSex.isEmpty()
                ? adapter.getPosition(savedSex) : 0);

        /* listeners */
        saveUserInfoButton.setOnClickListener(v -> saveUserInfo());
        saveHeightWeightButton.setOnClickListener(v -> saveUserVariables());
        changeEmailButton.setOnClickListener(v -> showChangeEmailDialog());
        changePasswordButton.setOnClickListener(v -> showChangePasswordDialog());
        deleteProfileButton.setOnClickListener(v -> showDeleteDialog());
        backButton.setOnClickListener(v -> finish());

        /* preload */
        loadUserInfo(email);
        loadUserVariables(email);
    }

    /* ────── SAVE → USER TABLE ────── */
    private void saveUserInfo() {
        String birthdateStr = birthdateInput.getText().toString();
        String selectedSex  = sexSpinner.getSelectedItem().toString();

        JSONObject body = new JSONObject();
        boolean something = false;
        try {
            if (!birthdateStr.isEmpty()) {
                body.put("birth_date", Integer.parseInt(birthdateStr));
                something = true;
            }
            if (!birthdateStr.isEmpty()) {
                Prefs.setBirthdate(this, Integer.parseInt(birthdateStr));
            }
            if (selectedSex.equals("Male") || selectedSex.equals("Female")) {
                Prefs.setSex(this, selectedSex);
            }

            if (selectedSex.equals("Male"))   { body.put("sex", false); something = true; }
            if (selectedSex.equals("Female")) { body.put("sex", true);  something = true; }
        } catch (Exception ignore) {}

        if (!something) { t("Nothing to update"); return; }

        String url = Constants.BASE_URL + "/user/" + email;
        sendJson(Request.Method.PUT, url, body,
                () -> t("User info updated"),
                () -> t("Error updating user info"));
    }

    /* ────── SAVE → USER_VARIABLES ────── */
    private void saveUserVariables() {
        String h = heightInput.getText().toString(),
                w = weightInput.getText().toString(),
                s = insulinSensitivityInput.getText().toString(),
                r = carbRatioInput.getText().toString(),
                a = carbAbsorptionInput.getText().toString();

        if (h.isEmpty() && w.isEmpty() && s.isEmpty() && r.isEmpty() && a.isEmpty()) {
            t("Nothing to update"); return;
        }

        JSONObject body = new JSONObject();
        StringBuilder updated = new StringBuilder();
        try {
            body.put("user_email", email);
            body.put("change_date_time", System.currentTimeMillis() / 1_000);

            if (!h.isEmpty()) { body.put("height",               Double.parseDouble(h)); updated.append("Height "); }
            if (!w.isEmpty()) { body.put("weight",               Double.parseDouble(w)); updated.append("Weight "); }
            if (!s.isEmpty()) { body.put("insulin_sensitivity",  Double.parseDouble(s)); updated.append("Sensitivity "); }
            if (!r.isEmpty()) { body.put("carb_ratio",           Double.parseDouble(r)); updated.append("Ratio "); }
            if (!a.isEmpty()) { body.put("carb_absorption_rate", Double.parseDouble(a)); updated.append("Absorption "); }
        } catch (Exception ignore) {}

        final String sFinal = s, rFinal = r, aFinal = a;   // effectively-final for lambda
        String url = Constants.BASE_URL + "/user_variables";
        sendJson(Request.Method.POST, url, body,
                () -> {
                    t(updated.toString().trim() + " saved");
                    /* 🔄 update Prefs so PredictionManager reads fresh values */
                    final String hFinal = h, wFinal = w;

                    if (!hFinal.isEmpty()) Prefs.setHeight(this, Double.parseDouble(hFinal));
                    if (!wFinal.isEmpty()) Prefs.setWeight(this, Double.parseDouble(wFinal));

                    if (!sFinal.isEmpty()) Prefs.setInsulinSensitivity(this,  Double.parseDouble(sFinal));
                    if (!rFinal.isEmpty()) Prefs.setCarbRatio(this,           Double.parseDouble(rFinal));
                    if (!aFinal.isEmpty()) Prefs.setCarbAbsorptionRate(this,  Double.parseDouble(aFinal));
                },
                () -> t("Error saving user variables"));
    }

    /* ────── CHANGE EMAIL ────── */
    private void showChangeEmailDialog() {
        EditText input = new EditText(this);
        input.setHint("new@email.com");

        new AlertDialog.Builder(this)
                .setTitle("Change Email")
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    String newEmail = input.getText().toString().trim();
                    if (newEmail.isEmpty()) { t("Email can’t be empty"); return; }

                    JSONObject body = new JSONObject();
                    try { body.put("new_email", newEmail); } catch (Exception ignore) {}

                    String url = Constants.BASE_URL + "/user/email/" + email;
                    sendJson(Request.Method.PUT, url, body,
                            () -> {
                                renameLocalDb(email, newEmail);
                                getSharedPreferences("user_session", MODE_PRIVATE)
                                        .edit().putString("user_email", newEmail).apply();
                                email = newEmail;
                                t("Email updated");
                            },
                            () -> t("Error updating email"));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /* ────── CHANGE PASSWORD ────── */
    private void showChangePasswordDialog() {
        View dlg = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        EditText oldPw = dlg.findViewById(R.id.oldPassword);
        EditText newPw = dlg.findViewById(R.id.newPassword);

        new AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setView(dlg)
                .setPositiveButton("Save", (d, w) -> {
                    String o = oldPw.getText().toString();
                    String n = newPw.getText().toString();
                    if (o.isEmpty() || n.isEmpty()) { t("Fill both fields"); return; }

                    JSONObject body = new JSONObject();
                    try {
                        body.put("old_password", o);
                        body.put("new_password", n);
                    } catch (Exception ignore) {}

                    String url = Constants.BASE_URL + "/user/password/" + email;
                    sendJson(Request.Method.PUT, url, body,
                            () -> t("Password updated"),
                            () -> t("Error updating password"));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /* ────── DELETE PROFILE ────── */
    private void showDeleteDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Profile")
                .setMessage("This will permanently erase your account. Continue?")
                .setPositiveButton("Yes", (d, w) -> {
                    String url = Constants.BASE_URL + "/user/" + email;
                    sendJson(Request.Method.DELETE, url, null,
                            () -> {
                                deleteLocalDb(email);
                                getSharedPreferences("user_session", MODE_PRIVATE).edit().clear().apply();
                                startActivity(new Intent(this, LoginActivity.class));
                                t("Profile deleted");
                                finish();
                            },
                            () -> t("Error deleting profile"));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /* ────── LOADERS ────── */
    private void loadUserInfo(String mail) {
        String url = Constants.BASE_URL + "/user/" + mail;
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                res -> {
                    int birth = res.optInt("birth_date", 0);
                    boolean hasSex = res.has("sex") && !res.isNull("sex");
                    boolean sexVal = res.optBoolean("sex", false);
                    if (birth > 0) Prefs.setBirthdate(this, birth);
                    if (hasSex)    Prefs.setSex(this, sexVal ? "Female" : "Male");


                    birthdateInput.setText(birth > 0 ? String.valueOf(birth) : "");
                    sexSpinner.setSelection(hasSex ? (sexVal ? 2 : 1) : 0);
                },
                err -> t("Error loading user data"));
        Volley.newRequestQueue(this).add(req);
    }

    private void loadUserVariables(String mail) {
        String url = Constants.BASE_URL + "/user_variables/last/" + mail;
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                res -> {
                    /* text fields */
                    heightInput.setText( res.optInt("height", -1) > 0 ? res.optString("height") : "" );
                    weightInput.setText( res.optInt("weight", -1) > 0 ? res.optString("weight") : "" );
                    insulinSensitivityInput.setText( res.optDouble("insulin_sensitivity",-1) > 0 ? res.optString("insulin_sensitivity") : "" );
                    carbRatioInput.setText( res.optDouble("carb_ratio",-1) > 0 ? res.optString("carb_ratio") : "" );
                    carbAbsorptionInput.setText( res.optDouble("carb_absorption_rate",-1) > 0 ? res.optString("carb_absorption_rate") : "" );

                    /* prefs sync */
                    double isf = res.optDouble("insulin_sensitivity", -1);
                    double cr  = res.optDouble("carb_ratio",           -1);
                    double car = res.optDouble("carb_absorption_rate", -1);
                    double height = res.optDouble("height", -1);
                    double weight = res.optDouble("weight", -1);

                    if (height > 0) Prefs.setHeight(this, height);
                    if (weight > 0) Prefs.setWeight(this, weight);
                    if (isf > 0) Prefs.setInsulinSensitivity(this, isf);
                    if (cr  > 0) Prefs.setCarbRatio(this, cr);
                    if (car > 0) Prefs.setCarbAbsorptionRate(this, car);
                },
                err -> t("Error loading user data"));
        Volley.newRequestQueue(this).add(req);
    }

    /* ────── HELPERS ────── */
    /** Volley wrapper that treats 204 or empty bodies as success */
    private void sendJson(int method, String url, JSONObject body,
                          Runnable ok, Runnable err) {

        JsonObjectRequest req = new JsonObjectRequest(method, url, body,
                r -> ok.run(),
                e -> err.run()) {

            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse resp) {
                if (resp != null && (resp.statusCode == 204 || resp.data == null || resp.data.length == 0)) {
                    return Response.success(new JSONObject(), HttpHeaderParser.parseCacheHeaders(resp));
                }
                return super.parseNetworkResponse(resp);
            }
        };

        Volley.newRequestQueue(this).add(req);
    }

    private void t(String msg){ Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }

    /* ─────────── LOCAL-DB UTILITIES ─────────── */
    private String dbNameFor(String mail){
        return "glucose_db_" + mail.replace("@","_").replace(".","_");
    }

    private void renameLocalDb(String oldEmail, String newEmail){
        try{ GlucoseDB.getInstance(getApplicationContext(), oldEmail).close(); }
        catch(Exception ignore){}

        GlucoseDB.clearAllInstances();

        String oldBase = dbNameFor(oldEmail);
        String newBase = dbNameFor(newEmail);

        for(String sfx : new String[]{"", "-wal", "-shm"}){
            File oldF = getDatabasePath(oldBase + sfx);
            if(oldF.exists()) {
                File newF = getDatabasePath(newBase + sfx);
                //noinspection ResultOfMethodCallIgnored
                oldF.renameTo(newF);
            }
        }
        GlucoseDB.getInstance(getApplicationContext(), newEmail); // warm-up
    }

    private void deleteLocalDb(String mail){
        try{ GlucoseDB.getInstance(getApplicationContext(), mail).close(); }
        catch(Exception ignore){}
        GlucoseDB.clearAllInstances();
        deleteDatabase(dbNameFor(mail));    // removes -wal / -shm too
    }
}
