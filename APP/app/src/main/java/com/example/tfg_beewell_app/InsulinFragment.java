package com.example.tfg_beewell_app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.tfg_beewell_app.local.LocalInsulinEntry;
import com.example.tfg_beewell_app.local.Persist;          // ← NEW

import org.json.JSONObject;

public class InsulinFragment extends Fragment {

    private EditText insulinInput;
    private AutoCompleteTextView insulinTypeField;
    private AutoCompleteTextView injectionSpotDropdown;
    private Button saveBtn;
    private String email;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_insulin, container, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        SharedPreferences p = requireContext()
                .getSharedPreferences("user_session", Context.MODE_PRIVATE);
        email = p.getString("user_email", null);
        if (email == null) { Toast.makeText(requireContext(),
                "No user session found.", Toast.LENGTH_SHORT).show();
            requireActivity().finish(); return; }

        insulinInput          = v.findViewById(R.id.insulinInput);
        insulinTypeField      = v.findViewById(R.id.insulinTypeField);
        injectionSpotDropdown = v.findViewById(R.id.injectionSpotDropdown);
        saveBtn               = v.findViewById(R.id.saveInsulinBtn);

        setupDropdown();
        saveBtn.setOnClickListener(x -> sendInsulin());
    }

    /* ---------------- helpers ---------------- */
    private void setupDropdown() {
        insulinTypeField.setAdapter(ArrayAdapter.createFromResource(
                requireContext(), R.array.insulin_types_array,
                android.R.layout.simple_list_item_1));

        injectionSpotDropdown.setAdapter(ArrayAdapter.createFromResource(
                requireContext(), R.array.injection_spots_array,
                android.R.layout.simple_list_item_1));
    }

    private void sendInsulin() {
        String unitsStr     = insulinInput.getText().toString().trim();
        String insulinType  = insulinTypeField.getText().toString().trim();
        String injectionPos = injectionSpotDropdown.getText().toString().trim();

        if (unitsStr.isEmpty() || insulinType.isEmpty() || injectionPos.isEmpty()) {
            Toast.makeText(getContext(), "Fill all fields", Toast.LENGTH_SHORT).show(); return; }

        try {
            long   ts    = System.currentTimeMillis() / 1000L;
            double units = Double.parseDouble(unitsStr);
            String kind  = simplify(insulinType);

            JSONObject body = new JSONObject();
            body.put("user_email",   email);
            body.put("insulin_time", ts);
            body.put("insulin_value", units);
            body.put("insulin_type",  kind);
            body.put("in_spot",       injectionPos);

            JsonObjectRequest req = new JsonObjectRequest(
                    Request.Method.POST, Constants.BASE_URL + "/insulin", body,
                    rsp -> {
                        Toast.makeText(getContext(), "Insulin saved!",
                                Toast.LENGTH_SHORT).show();

                        /* ─── LOCAL SAVE ─── */
                        Persist.insulin(requireContext(),
                                new LocalInsulinEntry(ts, units, kind, injectionPos));

                        insulinInput.setText("");
                    },
                    err -> {
                        if (err.networkResponse != null) {
                            Log.e("INSULIN", "HTTP " + err.networkResponse.statusCode +
                                    " → " + new String(err.networkResponse.data));
                        }
                        Toast.makeText(getContext(), "Error saving", Toast.LENGTH_SHORT).show();
                    });

            Volley.newRequestQueue(requireContext()).add(req);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error preparing data", Toast.LENGTH_SHORT).show();
        }
    }

    private String simplify(String raw) {
        raw = raw.toLowerCase();
        if (raw.contains("fiasp") || raw.contains("apidra") || raw.contains("humalog")
                || raw.contains("lispro") || raw.contains("novorapid") || raw.contains("afrezza"))
            return "rapid-acting";
        return "slow-acting";
    }
}
