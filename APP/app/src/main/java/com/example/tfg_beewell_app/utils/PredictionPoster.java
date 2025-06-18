package com.example.tfg_beewell_app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.tfg_beewell_app.Constants;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public final class PredictionPoster {

    private static final String TAG = "PredictionPoster";
    private static final String ENDPOINT = Constants.BASE_URL + "/prediction";

    private PredictionPoster() {}

    public static void post(Context ctx,
                            String forecastDesc,
                            String type,
                            Integer confidenceLvl,
                            Long forecastEpoch /* nullable */) {

        SharedPreferences sp = ctx.getSharedPreferences("user_session", Context.MODE_PRIVATE);
        String email = sp.getString("user_email", null);
        if (email == null) {
            Log.w(TAG, "No user session, skip /prediction");
            return;
        }

        try {
            JSONObject body = new JSONObject();
            body.put("user_email",     email);
            body.put("predict_time",   System.currentTimeMillis() / 1000);
            body.put("forecast_desc",  forecastDesc);
            body.put("predict_type",   type);
            if (confidenceLvl != null) body.put("confidence_lvl", confidenceLvl);
            if (forecastEpoch  != null) body.put("forecast_time",  forecastEpoch);

            new Thread(() -> {
                try {
                    HttpURLConnection c = (HttpURLConnection) new URL(ENDPOINT).openConnection();
                    c.setRequestMethod("POST");
                    c.setRequestProperty("Content-Type", "application/json");
                    c.setDoOutput(true);
                    OutputStream os = c.getOutputStream();
                    os.write(body.toString().getBytes("UTF-8"));
                    os.close();
                    Log.i(TAG, "POST /prediction → " + c.getResponseCode());
                } catch (Exception e) {
                    Log.e(TAG, "POST failed", e);
                }
            }).start();

        } catch (Exception e) {
            Log.e(TAG, "JSON build failed", e);
        }
    }
}
