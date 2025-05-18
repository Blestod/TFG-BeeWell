package com.example.tfg_beewell_app.utils;

import androidx.annotation.NonNull;
import retrofit2.*;
import retrofit2.converter.gson.GsonConverterFactory;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.http.Body;
import retrofit2.http.POST;


public class ChatService {

    private static final String BASE_URL = "http://10.0.2.2:8000/";   // emulador→PC
    private static ChatService INSTANCE;
    private final KBApi api;

    private ChatService() {
        Retrofit r = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(new OkHttpClient.Builder()
                        .addInterceptor(chain -> {          // opcional: log
                            return chain.proceed(chain.request());
                        }).build())
                .build();
        api = r.create(KBApi.class);
    }

    public static ChatService getInstance() {
        if (INSTANCE == null) INSTANCE = new ChatService();
        return INSTANCE;
    }

    /* ---------- API propia ---------- */
    interface KBApi {
        @POST("api/knowledgebase")
        Call<KBAnswer> ask(@Body KBRequest req);
    }

    /* ---------- DTO ---------- */
    static class KBRequest {
        final String task;
        KBRequest(String t) { task = t; }
    }
    /* tu backend devuelve por ejemplo { "answer": "texto" } */
    static class KBAnswer { String answer; }

    /* ---------- fachada ---------- */
    public interface BotCallback { void onAnswer(String txt); }

    public void askBot(String prompt, BotCallback cb) {
        api.ask(new KBRequest(prompt)).enqueue(new Callback<KBAnswer>() {
            @Override public void onResponse(Call<KBAnswer> c, Response<KBAnswer> r) {
                if (r.isSuccessful() && r.body()!=null && r.body().answer!=null)
                    cb.onAnswer(r.body().answer);
                else cb.onAnswer("(sin respuesta)");
            }
            @Override public void onFailure(Call<KBAnswer> c, Throwable t) {
                cb.onAnswer("Error: "+t.getMessage());
            }
        });
    }
}
