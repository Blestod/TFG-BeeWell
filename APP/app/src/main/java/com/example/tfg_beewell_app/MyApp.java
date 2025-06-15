package com.example.tfg_beewell_app;

import android.app.Application;

import com.example.tfg_beewell_app.utils.RecoScheduler;

public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        RecoScheduler.scheduleDaily(this);
    }
}
