package com.example.tfg_beewell_app.utils;

import androidx.annotation.NonNull;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;                 // ➊ nuevo
import okhttp3.logging.HttpLoggingInterceptor;
import org.json.JSONObject;                 // ➋ nuevo
import retrofit2.*;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

/** Repository que envía { "task": ... } a BeeWell-Core y devuelve la respuesta */
public class ChatService {

    private static final String BASE_URL = "https://beewell.core.sciling.com/";

    private static ChatService INSTANCE;
    private final KBApi api;

    private ChatService() {

        HttpLoggingInterceptor log = new HttpLoggingInterceptor();
        log.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient ok = new OkHttpClient.Builder()
                .addInterceptor(log)
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout  (30, TimeUnit.SECONDS)
                .readTimeout   (120, TimeUnit.SECONDS)   // 2 min de espera
                .build();

        Retrofit r = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(ok)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = r.create(KBApi.class);
    }

    public static ChatService getInstance() {
        if (INSTANCE == null) INSTANCE = new ChatService();
        return INSTANCE;
    }

    /* ------------ Retrofit interface ------------ */
    interface KBApi {
        @POST("api/knowledgebase")                       // SIN barra final
        Call<ResponseBody> ask(@Body KBRequest req);     // ➌ ahora ResponseBody
    }

    /* ------------ DTO ------------ */
    public static class KBRequest {
        final String task;
        public KBRequest(String t){ task = t; }
    }

    /* ------------ fachada pública ------------ */
    public interface BotCallback { void onAnswer(String txt); }

    public void askBot(String prompt, BotCallback cb) {

        api.ask(new KBRequest(prompt)).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call,
                                   @NonNull Response<ResponseBody> resp) {

                if (!resp.isSuccessful() || resp.body()==null) {
                    cb.onAnswer("(sin respuesta)");
                    return;
                }
                try {
                    String raw = resp.body().string();         // JSON completo
                    String ans = new JSONObject(raw)
                            .optString("answer","(sin respuesta)");
                    cb.onAnswer(ans);
                } catch (Exception e) {
                    cb.onAnswer("Error parse: "+e.getMessage());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call,
                                  @NonNull Throwable t) {
                cb.onAnswer("Error: " + t.getMessage());
            }
        });
    }
}
