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
import com.example.tfg_beewell_app.local.LocalActivityEntry;
import com.example.tfg_beewell_app.local.Persist;

import org.json.JSONObject;

public class ExerciseFragment extends Fragment {

    private EditText nameInput, durationInput;
    private AutoCompleteTextView intensityDropdown, typeDropdown;
    private Button saveBtn;
    private String email;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater i,
                             @Nullable ViewGroup c,
                             @Nullable Bundle s) {
        return i.inflate(R.layout.fragment_exercise, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        SharedPreferences p = requireContext()
                .getSharedPreferences("user_session", Context.MODE_PRIVATE);
        email = p.getString("user_email", null);
        if (email == null) {
            Toast.makeText(requireContext(),"No user session found.",
                    Toast.LENGTH_SHORT).show();
            requireActivity().finish(); return;
        }

        nameInput        = v.findViewById(R.id.exerciseName);
        durationInput    = v.findViewById(R.id.exerciseDuration);
        intensityDropdown= v.findViewById(R.id.exerciseIntensity);
        typeDropdown     = v.findViewById(R.id.exerciseType);
        saveBtn          = v.findViewById(R.id.saveExerciseButton);

        setupDropdowns();
        saveBtn.setOnClickListener(x->sendExercise());
    }

    /* ---------------- helpers ---------------- */
    private void setupDropdowns(){
        intensityDropdown.setAdapter(ArrayAdapter.createFromResource(
                requireContext(), R.array.exercise_intensity_options,
                android.R.layout.simple_list_item_1));

        typeDropdown.setAdapter(ArrayAdapter.createFromResource(
                requireContext(), R.array.exercise_type_options,
                android.R.layout.simple_list_item_1));
    }

    /* ---------------- save Activity ---------------- */
    private void sendExercise(){
        String name       = nameInput.getText().toString().trim();
        String durStr     = durationInput.getText().toString().trim();
        String intensity  = intensityDropdown.getText().toString().trim();
        String type       = typeDropdown.getText().toString().trim();

        if(durStr.isEmpty()||intensity.isEmpty()){
            Toast.makeText(getContext(),"Duration and intensity required",
                    Toast.LENGTH_SHORT).show(); return; }

        try{
            int  duration = Integer.parseInt(durStr);
            long tsSec    = System.currentTimeMillis()/1000L;

            JSONObject body=new JSONObject();
            body.put("user_email",    email);
            body.put("act_name",      name.isEmpty()? JSONObject.NULL : name);
            body.put("duration_min",  duration);
            body.put("intensity",     intensity);
            body.put("act_time",      tsSec);
            body.put("activity_type", type.isEmpty()? JSONObject.NULL : type);

            JsonObjectRequest req = new JsonObjectRequest(
                    Request.Method.POST, Constants.BASE_URL + "/activity", body,
                    rsp -> {
                        Toast.makeText(getContext(),"Activity saved!",
                                Toast.LENGTH_SHORT).show();

                        /*  LOCAL CACHE  */
                        Persist.act(requireContext(),
                                new LocalActivityEntry(tsSec,duration,intensity,
                                        name.isEmpty()?null:name,
                                        type.isEmpty()?null:type));

                        durationInput.setText(""); nameInput.setText("");
                    },
                    err -> {
                        if(err.networkResponse!=null){
                            Log.e("EXERCISE","HTTP "+err.networkResponse.statusCode+" → "+
                                    new String(err.networkResponse.data));
                        }
                        Toast.makeText(getContext(),"Error saving activity",
                                Toast.LENGTH_SHORT).show();
                    });

            Volley.newRequestQueue(requireContext()).add(req);

        }catch(Exception e){
            e.printStackTrace();
            Toast.makeText(getContext(),"Error preparing activity data",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
