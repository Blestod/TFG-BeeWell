package com.example.tfg_beewell_app.utils;

import androidx.annotation.NonNull;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import org.json.JSONObject;
import retrofit2.*;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

public class HomeService {
    private static final String BASE_URL = "https://beewell.core.sciling.com/";
    private static HomeService INSTANCE;
    private final HomeApi api;
    private long lastFetchTime = 0;
    private static final long CACHE_DURATION_MS = 20 * 60 * 1000; // 20 minutes
    private String cachedVitals = null;
    private String cachedPrediction = null;


    private HomeService() {
        HttpLoggingInterceptor log = new HttpLoggingInterceptor();
        log.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(log)
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(HomeApi.class);
    }

    public static HomeService getInstance() {
        if (INSTANCE == null) INSTANCE = new HomeService();
        return INSTANCE;
    }

    interface HomeApi {
        @POST("api/home")
        Call<ResponseBody> getHomeInsights(@Body HomeRequestBody req);
    }

    public interface HomeCallback {
        void onResult(String vitalsAdvice, String predictionAdvice);
    }

    public static class HomeRequestBody {
        final JSONObject patient_data;

        public HomeRequestBody(String wrapperJson) {
            this.patient_data = new JSONObject();
            try {
                JSONObject wrapper = new JSONObject(wrapperJson);
                JSONObject inner = wrapper.getJSONObject("patient_data");

                patient_data.put("user_email",   inner.optString("user_email"));
                patient_data.put("health_data",  inner.getJSONObject("health_data"));
                patient_data.put("events",       inner.getJSONArray("events"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public void fetchHomeInsights(JSONObject fullWrapper, HomeCallback callback) {
        long now = System.currentTimeMillis();

        if (now - lastFetchTime < CACHE_DURATION_MS && cachedVitals != null && cachedPrediction != null) {
            callback.onResult(cachedVitals, cachedPrediction);
            return;
        }

        HomeRequestBody body = new HomeRequestBody(fullWrapper.toString());

        api.getHomeInsights(body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> resp) {
                if (!resp.isSuccessful() || resp.body() == null) {
                    callback.onResult("(no vitals)", "(no predictions)");
                    return;
                }

                try {
                    String jsonStr = resp.body().string();
                    JSONObject raw = new JSONObject(jsonStr);
                    Object responseField = raw.opt("response");

                    JSONObject inner = null;

                    if (responseField instanceof String) {
                        String str = (String) responseField;
                        try {
                            inner = new JSONObject(str); // Try to parse it as JSON
                        } catch (Exception e) {
                            // Not a JSON string, treat as plain fallback
                            callback.onResult(str, "");
                            return;
                        }
                    } else if (responseField instanceof JSONObject) {
                        inner = (JSONObject) responseField;
                    }

                    if (inner == null) {
                        callback.onResult("(invalid response)", "(invalid prediction)");
                        return;
                    }

                    String vitalsAdvice = inner.optString("recommendation_vitals", "(no vitals)");
                    String predAdvice   = inner.optString("recommendation_prediction", "(no predictions)");

                    cachedVitals = vitalsAdvice;
                    cachedPrediction = predAdvice;
                    lastFetchTime = System.currentTimeMillis();

                    callback.onResult(vitalsAdvice, predAdvice);

                } catch (Exception e) {
                    callback.onResult("Parse error: " + e.getMessage(), "");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                callback.onResult("Error: " + t.getMessage(), "");
            }
        });
    }


}
