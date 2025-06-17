package com.example.tfg_beewell_app;

import android.content.Context;
import android.content.SharedPreferences;
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

import com.example.tfg_beewell_app.local.LocalMealEntry;
import com.example.tfg_beewell_app.local.Persist;

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
    private EditText             gramsInput;
    private Button               saveBtn;

    private ArrayAdapter<String> adapter;
    private final Map<String,Integer> foodMap = new HashMap<>();
    private Integer selectedFoodId = null;

    private String email;

    private final Handler handler = new Handler();
    private Runnable searchRunnable;

    /* ---------------- life-cycle ---------------- */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_meal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        foodSearchInput = v.findViewById(R.id.foodSearchInput);
        gramsInput      = v.findViewById(R.id.gramsInput);
        saveBtn         = v.findViewById(R.id.saveMealBtn);

        SharedPreferences prefs = requireContext()
                .getSharedPreferences("user_session", Context.MODE_PRIVATE);
        email = prefs.getString("user_email", null);
        if (email == null) {
            Toast.makeText(requireContext(),"No user session found.",Toast.LENGTH_SHORT).show();
            requireActivity().finish(); return;
        }

        adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line);
        foodSearchInput.setAdapter(adapter);

        /* ---- live search ---- */
        foodSearchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int a,int b,int c){}
            @Override public void onTextChanged(CharSequence s,int a,int b,int c){
                if (searchRunnable!=null) handler.removeCallbacks(searchRunnable);
                searchRunnable = () -> searchFood(s.toString());
                handler.postDelayed(searchRunnable,300);
            }
            @Override public void afterTextChanged(Editable s){
                String typed = s.toString().trim();
                selectedFoodId = foodMap.get(typed); // will be null if not found
            }
        });
        foodSearchInput.setOnItemClickListener((p1, p2, pos, id)->{
            String selected = adapter.getItem(pos);
            selectedFoodId  = foodMap.get(selected);
        });

        /* ---- UX tweaks ---- */
        foodSearchInput.setOnFocusChangeListener((v1,has)->{
            if (has) {
                InputMethodManager imm=(InputMethodManager)requireContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm!=null) imm.hideSoftInputFromWindow(foodSearchInput.getWindowToken(),0);
                handler.postDelayed(()->{
                    if (foodSearchInput.getText().length()>0) foodSearchInput.showDropDown();
                },200);
            }
        });

        saveBtn.setOnClickListener(x->saveMeal());
    }

    /* ---------------- REST search ---------------- */
    private void searchFood(String q){
        if (q.length()<2) return;

        new Thread(()->{
            try {
                URL url = new URL(Constants.BASE_URL + "/food/search?q=" +
                        URLEncoder.encode(q,"UTF-8"));
                HttpURLConnection c=(HttpURLConnection)url.openConnection();
                c.setRequestMethod("GET");

                BufferedReader r=new BufferedReader(
                        new InputStreamReader(c.getInputStream()));
                StringBuilder sb=new StringBuilder(); String line;
                while((line=r.readLine())!=null) sb.append(line);
                r.close();

                JSONArray arr=new JSONArray(sb.toString());
                foodMap.clear(); List<String> sugg=new ArrayList<>();
                for(int i=0;i<arr.length();i++){
                    JSONObject it=arr.getJSONObject(i);
                    String name=it.getString("food_name");
                    int id     =it.getInt("food_id");
                    sugg.add(name); foodMap.put(name,id);
                }

                requireActivity().runOnUiThread(()->{
                    adapter.clear(); adapter.addAll(sugg); adapter.notifyDataSetChanged();
                    handler.postDelayed(foodSearchInput::showDropDown,100);
                });
            }catch(Exception e){ Log.e("FOOD_SEARCH","Error",e);}
        }).start();
    }

    /* ---------------- save Meal ---------------- */
    private void saveMeal(){
        if (selectedFoodId==null){
            Toast.makeText(getContext(),"Please select a food",Toast.LENGTH_SHORT).show();return;}

        String gramsTxt=gramsInput.getText().toString().trim();
        if (gramsTxt.isEmpty()){
            Toast.makeText(getContext(),"Please enter grams",Toast.LENGTH_SHORT).show();return;}

        float grams = Float.parseFloat(gramsTxt);
        long  tsSec = System.currentTimeMillis()/1000;

        JSONObject body=new JSONObject();
        try{
            body.put("user_email",email);
            body.put("meal_time", tsSec);
            body.put("grams",     grams);
            body.put("food_id",   selectedFoodId);
        }catch(JSONException e){ e.printStackTrace(); return; }

        new Thread(()->{
            try{
                URL url=new URL(Constants.BASE_URL + "/meal");
                HttpURLConnection c=(HttpURLConnection)url.openConnection();
                c.setRequestMethod("POST");
                c.setRequestProperty("Content-Type","application/json");
                c.setDoOutput(true);
                OutputStream os=c.getOutputStream();
                os.write(body.toString().getBytes("UTF-8")); os.close();
                int code=c.getResponseCode();

                requireActivity().runOnUiThread(()->{
                    if(code==200||code==201){
                        Toast.makeText(getContext(),"Meal saved!",Toast.LENGTH_SHORT).show();

                        /*  LOCAL CACHE  */
                        Persist.meal(requireContext(),
                                new LocalMealEntry(
                                        tsSec,
                                        grams,
                                        selectedFoodId,
                                        foodSearchInput.getText().toString().trim()));

                        foodSearchInput.setText(""); gramsInput.setText("");
                    }else{
                        Toast.makeText(getContext(),"Failed to save meal",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }catch(Exception e){ Log.e("SAVE_MEAL","Failed",e);}
        }).start();
    }
}
