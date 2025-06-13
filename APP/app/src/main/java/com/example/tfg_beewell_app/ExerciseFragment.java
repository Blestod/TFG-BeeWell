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
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import org.json.JSONObject;

public class ExerciseFragment extends Fragment {

        private EditText nameInput, durationInput;
        private AutoCompleteTextView intensityDropdown, typeDropdown;
        private Button saveBtn;
        private String email;

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater,
                                 @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_exercise, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View view,
                                  @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            SharedPreferences prefs = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE);
            email = prefs.getString("user_email", null);
            if (email == null) {
                Toast.makeText(requireContext(), "No user session found.", Toast.LENGTH_SHORT).show();
                requireActivity().finish();
                return;
            }

            nameInput = view.findViewById(R.id.exerciseName);
            durationInput = view.findViewById(R.id.exerciseDuration);
            intensityDropdown = view.findViewById(R.id.exerciseIntensity);
            typeDropdown = view.findViewById(R.id.exerciseType);
            saveBtn = view.findViewById(R.id.saveExerciseButton);

            setupDropdowns();
            saveBtn.setOnClickListener(v -> sendExercise());
        }

        private void setupDropdowns() {
            ArrayAdapter<CharSequence> intensityAdapter = ArrayAdapter.createFromResource(
                    requireContext(), R.array.exercise_intensity_options, android.R.layout.simple_list_item_1);
            intensityDropdown.setAdapter(intensityAdapter);

            ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(
                    requireContext(), R.array.exercise_type_options, android.R.layout.simple_list_item_1);
            typeDropdown.setAdapter(typeAdapter);
        }

        private void sendExercise() {
            String name = nameInput.getText().toString().trim();
            String durationStr = durationInput.getText().toString().trim();
            String intensity = intensityDropdown.getText().toString().trim();
            String type = typeDropdown.getText().toString().trim();

            if (durationStr.isEmpty() || intensity.isEmpty()) {
                Toast.makeText(getContext(), "Duration and intensity are required", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int duration = Integer.parseInt(durationStr);
                long epochSeconds = System.currentTimeMillis() / 1_000L;

                JSONObject body = new JSONObject();
                body.put("user_email", email);
                body.put("act_name", name.isEmpty() ? JSONObject.NULL : name);
                body.put("duration_min", duration);
                body.put("intensity", intensity);
                body.put("act_time", epochSeconds);
                body.put("activity_type", type.isEmpty() ? JSONObject.NULL : type);

                JsonObjectRequest req = new JsonObjectRequest(
                        Request.Method.POST,
                        Constants.BASE_URL + "/activity",
                        body,
                        rsp -> Toast.makeText(getContext(), "Activity saved!", Toast.LENGTH_SHORT).show(),
                        err -> {
                            if (err.networkResponse != null) {
                                Log.e("EXERCISE", "HTTP " + err.networkResponse.statusCode + " → " + new String(err.networkResponse.data));
                            }
                            Toast.makeText(getContext(), "Error saving activity", Toast.LENGTH_SHORT).show();
                        });

                Volley.newRequestQueue(requireContext()).add(req);

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Error preparing activity data", Toast.LENGTH_SHORT).show();
            }
        }
}
