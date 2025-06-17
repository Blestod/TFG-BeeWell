package com.example.tfg_beewell_app.ui.log;

import android.util.Log;

import com.example.tfg_beewell_app.Constants;

import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.*;

public final class Net {

    /* ------------ POJOs -------------- */
    public static class InsulinLog { public int injected_id; public long in_time; public float in_units;
        public String insulin_type, in_spot; }
    public static class MealLog {
        public int    meal_id;
        public long   meal_time;
        public float  grams;
        public int    food_id;
        public String food_name;
    }

    public static class ActLog     { public int activity_id; public long act_time;  public String act_name;
        public int duration_min; public String intensity; }

    /* ------------ API ---------------- */
    public interface BeeApi {
        @GET("insulin/all/{e}")  Call<List<InsulinLog>> ins(@Path("e") String email);
        @GET("meal/all/{e}")     Call<List<MealLog>>    meals(@Path("e") String email);
        @GET("activity/all/{e}") Call<List<ActLog>>     acts(@Path("e") String email);

        @DELETE("insulin/{id}")  Call<Void> delIns(@Path("id") int id);
        @DELETE("meal/{id}")     Call<Void> delMeal(@Path("id") int id);
        @DELETE("activity/{id}") Call<Void> delAct(@Path("id") int id);
    }

    /* ------------ Retrofit singleton  */
    public static final BeeApi api;
    static {
        HttpLoggingInterceptor log = new HttpLoggingInterceptor(
                message -> Log.d("NET-RAW", message)
        ).setLevel(HttpLoggingInterceptor.Level.BODY);

        Retrofit r = new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL + "/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(new OkHttpClient.Builder().addInterceptor(log).build())
                .build();

        api = r.create(BeeApi.class);
    }

}

