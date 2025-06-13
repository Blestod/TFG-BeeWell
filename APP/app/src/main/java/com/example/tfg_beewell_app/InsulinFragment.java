package com.example.tfg_beewell_app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

/**
 *  Fragment que permite registrar una dosis de insulina
 *  • Desplegable y entrada de texto para unidades
 *  • Envío a tu backend vía Volley
 */
public class InsulinFragment extends Fragment {

    private EditText insulinInput;
    private AutoCompleteTextView insulinTypeField;
    private AutoCompleteTextView injectionSpotDropdown;

    private Button saveBtn;
    private String email;

    /*–––––––––––––––––––––––––––––––––––––––––––––––*/
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Usa el layout con el Exposed Drop-down que te pasé
        return inflater.inflate(R.layout.fragment_insulin, container, false);
    }

    /*–––––––––––––––––––––––––––––––––––––––––––––––*/
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences prefs = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE);
        email = prefs.getString("user_email", null);

        if (email == null) {
            Toast.makeText(requireContext(), "No user session found.", Toast.LENGTH_SHORT).show();
            requireActivity().finish(); // optional: close the screen if no user
            return;
        }

        insulinInput = view.findViewById(R.id.insulinInput);
        insulinTypeField = view.findViewById(R.id.insulinTypeField);
        injectionSpotDropdown = view.findViewById(R.id.injectionSpotDropdown);

        saveBtn = view.findViewById(R.id.saveInsulinBtn);

        setupDropdown();

        saveBtn.setOnClickListener(v -> sendInsulin());
    }


    /*-------------------------------------------------
     *  Carga los tipos de insulina en el desplegable
     *------------------------------------------------*/
    private void setupDropdown() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.insulin_types_array,
                android.R.layout.simple_list_item_1);
        insulinTypeField.setAdapter(adapter);

        ArrayAdapter<CharSequence> spotAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.injection_spots_array,
                android.R.layout.simple_list_item_1);
        injectionSpotDropdown.setAdapter(spotAdapter);
    }


    /*-------------------------------------------------
     *  Envía la dosis al servidor
     *------------------------------------------------*/
    //-----------  Pega esto en tu fragment  -----------
    private void sendInsulin() {

        String unitsStr = insulinInput.getText().toString().trim();
        String insulinType = insulinTypeField.getText().toString().trim();


        if (unitsStr.isEmpty()) {
            Toast.makeText(getContext(),
                    "Add Units",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (insulinType.isEmpty()) {
            Toast.makeText(getContext(),
                    "Select Type",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String injectionSpot = injectionSpotDropdown.getText().toString().trim();
        if (injectionSpot.isEmpty()) {
            Toast.makeText(getContext(), "Select injection spot", Toast.LENGTH_SHORT).show();
            return;
        }



        try {
            long epochSeconds = System.currentTimeMillis() / 1_000L; // INT
            double units = Double.parseDouble(unitsStr);        // FLOAT

            JSONObject body = new JSONObject();
            body.put("user_email", email);              // String
            body.put("insulin_time", epochSeconds);     // INT
            body.put("insulin_value", units);         // FLOAT
            body.put("insulin_type", insulinType);   // String
            body.put("in_spot", injectionSpot);     // Where


            JsonObjectRequest req = new JsonObjectRequest(
                    Request.Method.POST,
                    Constants.BASE_URL + "/insulin",
                    body,
                    rsp -> Toast.makeText(getContext(),
                            "Insulin saved!",
                            Toast.LENGTH_SHORT).show(),
                    err -> {
                        if (err.networkResponse != null) {
                            Log.e("INSULIN",
                                    "HTTP " + err.networkResponse.statusCode + " → "
                                            + new String(err.networkResponse.data));
                        }
                        Toast.makeText(getContext(),
                                "Error saving",
                                Toast.LENGTH_SHORT).show();
                    });

            Volley.newRequestQueue(requireContext()).add(req);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(),
                    "Error preparing data",
                    Toast.LENGTH_SHORT).show();
        }
    }

}
