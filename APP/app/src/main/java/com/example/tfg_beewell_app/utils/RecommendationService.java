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

public class RecommendationService {

    private static final String BASE_URL = "https://beewell.core.sciling.com/";
    private static RecommendationService INSTANCE;
    private final RecoApi api;

    private RecommendationService() {
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

        api = retrofit.create(RecoApi.class);
    }

    public static RecommendationService getInstance() {
        if (INSTANCE == null) INSTANCE = new RecommendationService();
        return INSTANCE;
    }

    interface RecoApi {
        @POST("api/recommendation")
        Call<ResponseBody> getRecommendation(@Body RequestBody req);
    }

    public interface RecoCallback { void onResult(String diet, String exercise); }

    public void fetchReco(JSONObject patientData, RecoCallback callback) {
        RequestBody request = new RequestBody(patientData.toString());

        api.getRecommendation(request).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call,
                                   @NonNull Response<ResponseBody> resp) {
                if (!resp.isSuccessful() || resp.body() == null) {
                    callback.onResult("(no diet)", "(no exercise)");
                    return;
                }
                try {
                    String jsonStr = resp.body().string();
                    JSONObject j = new JSONObject(jsonStr);
                    JSONObject recos = new JSONObject(j.getString("recommendations"));

                    String diet = recos.optString("diet", "(sin dieta)");
                    String exercise = recos.optString("exercise", "(sin ejercicio)");

                    callback.onResult(diet, exercise);
                } catch (Exception e) {
                    callback.onResult("Parse error: " + e.getMessage(), "");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call,
                                  @NonNull Throwable t) {
                callback.onResult("Error: " + t.getMessage(), "");
            }
        });
    }

    public static class RequestBody {
        final JSONObject patient_data;
        public RequestBody(String patientJson) {
            this.patient_data = new JSONObject();
            try {
                this.patient_data.put("events", new JSONObject(patientJson).getJSONArray("events"));
                this.patient_data.put("health_data", new JSONObject(patientJson).getJSONObject("health_data"));
            } catch (Exception e) {
                // fallback
            }
        }
    }
}
