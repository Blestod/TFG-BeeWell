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
                // Parse the outer wrapper
                JSONObject wrapper = new JSONObject(wrapperJson);
                // Extract the real payload
                JSONObject inner = wrapper.getJSONObject("patient_data");

                // Copy the fields your endpoint expects:
                patient_data.put("user_email",   inner.optString("user_email"));
                patient_data.put("health_data",  inner.getJSONObject("health_data"));
                patient_data.put("events",       inner.getJSONArray("events"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void fetchHomeInsights(JSONObject fullWrapper, HomeCallback callback) {
        HomeRequestBody body = new HomeRequestBody(fullWrapper.toString());

        api.getHomeInsights(body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call,
                                   @NonNull Response<ResponseBody> resp) {
                if (!resp.isSuccessful() || resp.body() == null) {
                    callback.onResult("(no vitals)", "(no predictions)");
                    return;
                }
                try {
                    String jsonStr = resp.body().string();
                    JSONObject j      = new JSONObject(jsonStr);
                    JSONObject recos  = j.optJSONObject("recommendations");

                    String vitalsAdvice = recos != null
                            ? recos.optString("vitals_recommendation", "(no vitals)")
                            : "(no vitals)";
                    String predAdvice   = recos != null
                            ? recos.optString("prediction_recommendation", "(no predictions)")
                            : "(no predictions)";

                    callback.onResult(vitalsAdvice, predAdvice);
                } catch (Exception e) {
                    callback.onResult("Parse error:" + e.getMessage(), "");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call,
                                  @NonNull Throwable t) {
                callback.onResult("Error:" + t.getMessage(), "");
            }
        });
    }
}
