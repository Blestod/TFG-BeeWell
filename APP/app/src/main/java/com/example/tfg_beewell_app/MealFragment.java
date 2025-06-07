package com.example.tfg_beewell_app;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MealFragment extends Fragment {
    private AutoCompleteTextView foodSearchInput;
    private EditText gramsInput;
    private Button saveBtn;

    private ArrayAdapter<String> adapter;
    private Map<String, Integer> foodMap = new HashMap<>();
    private Integer selectedFoodId = null;

    private final Handler handler = new Handler();
    private Runnable searchRunnable;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_meal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        foodSearchInput = view.findViewById(R.id.foodSearchInput);
        gramsInput = view.findViewById(R.id.gramsInput);
        saveBtn = view.findViewById(R.id.saveMealBtn);

        adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line);
        foodSearchInput.setAdapter(adapter);

        foodSearchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) handler.removeCallbacks(searchRunnable);
                searchRunnable = () -> searchFood(s.toString());
                handler.postDelayed(searchRunnable, 300);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        foodSearchInput.setOnItemClickListener((parent, view1, position, id) -> {
            String selected = adapter.getItem(position);
            selectedFoodId = foodMap.get(selected);
            Log.d("FOOD_SELECT", "Selected: " + selected + " → ID: " + selectedFoodId);
        });

        saveBtn.setOnClickListener(v -> saveMeal());

        View.OnFocusChangeListener focusListener = (v, hasFocus) -> {
            if (hasFocus && v.getId() == R.id.foodSearchInput) {
                // Oculta el teclado
                InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(foodSearchInput.getWindowToken(), 0);
                }

                // Muestra dropdown si hay texto
                handler.postDelayed(() -> {
                    if (foodSearchInput.getText().length() > 0) {
                        foodSearchInput.showDropDown();
                    }
                }, 200);
            }
        };

        foodSearchInput.setOnFocusChangeListener(focusListener);



        // Optional: show keyboard automatically
        foodSearchInput.requestFocus();
        foodSearchInput.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(foodSearchInput, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 200);
    }

    private void searchFood(String query) {
        if (query.length() < 2) return;

        Log.d("FOOD_SEARCH", "Searching for: " + query);

        new Thread(() -> {
            try {
                URL url = new URL("https://beewell.blestod.com/food/search?q=" + URLEncoder.encode(query, "UTF-8"));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder jsonBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) jsonBuilder.append(line);
                reader.close();

                JSONArray jsonArray = new JSONArray(jsonBuilder.toString());
                foodMap.clear();
                List<String> suggestions = new ArrayList<>();

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject item = jsonArray.getJSONObject(i);
                    String name = item.getString("food_name");
                    int id = item.getInt("food_id");
                    suggestions.add(name);
                    foodMap.put(name, id);
                }

                Log.d("FOOD_SEARCH", "Suggestions received: " + suggestions);

                requireActivity().runOnUiThread(() -> {
                    adapter.clear();
                    adapter.addAll(suggestions);
                    adapter.notifyDataSetChanged();
                    handler.postDelayed(() -> foodSearchInput.showDropDown(), 100);
                });

            } catch (Exception e) {
                Log.e("FOOD_SEARCH", "Error during food search", e);
            }
        }).start();
    }

    private void saveMeal() {
        if (selectedFoodId == null) {
            Toast.makeText(getContext(), "Please select a food", Toast.LENGTH_SHORT).show();
            return;
        }

        String gramsText = gramsInput.getText().toString().trim();
        if (gramsText.isEmpty()) {
            Toast.makeText(getContext(), "Please enter grams", Toast.LENGTH_SHORT).show();
            return;
        }

        float grams = Float.parseFloat(gramsText);
        long currentTime = System.currentTimeMillis() / 1000;

        JSONObject body = new JSONObject();
        try {
            body.put("user_email", "user"); // hardcoded
            body.put("meal_time", currentTime);
            body.put("grams", grams);
            body.put("food_id", selectedFoodId);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        new Thread(() -> {
            try {
                URL url = new URL("https://beewell.blestod.com/meal");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                requireActivity().runOnUiThread(() -> {
                    if (responseCode == 201 || responseCode == 200) {
                        Toast.makeText(getContext(), "Meal saved!", Toast.LENGTH_SHORT).show();
                        foodSearchInput.setText("");
                        gramsInput.setText("");
                    } else {
                        Toast.makeText(getContext(), "Failed to save meal", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                Log.e("SAVE_MEAL", "Failed to save meal", e);
            }
        }).start();
    }
}
