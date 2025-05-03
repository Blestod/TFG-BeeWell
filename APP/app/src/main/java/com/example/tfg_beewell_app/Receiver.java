package com.example.tfg_beewell_app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import java.text.DateFormat;
import java.util.Date;

public class Receiver extends BroadcastReceiver {
    private static final String ACTION = "glucodata.Minute";
    private static final String ALARM = "glucodata.Minute.Alarm";
    private static final String GLUCOSECUSTOM = "glucodata.Minute.glucose";
    private static final String LOG_ID = "Receiver";
    private static final String MGDL = "glucodata.Minute.mgdl";
    private static final String RATE = "glucodata.Minute.Rate";
    private static final String SERIAL = "glucodata.Minute.SerialNumber";
    private static final String TIME = "glucodata.Minute.Time";
    static DateFormat dateformat = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT);

static  String getdexcomlabel(float rate ) {
        if (rate >= 3.0f) return "DoubleUp";
        if (rate >= 2.0f) return "SingleUp";
        if (rate >= 1.0f) return "FortyFiveUp";
        if (rate > -1.0f) return "Flat";
        if (rate > -2.0f) return "FortyFiveDown";
        if (rate > -3.0f) return "SingleDown";
        if(java.lang.Float.isNaN(rate)) return "";
        return "DoubleDown";
    }


static     String librelabel(float rate) {
        if (rate <= -2.0f) {
            return "Falling Quickly".intern();
        }
        if (rate <= -1.0f) {
            return "Falling".intern();
        }
        if (rate <= 1.0f) {
            return "Steady".intern();
        }
        if (rate <= 2.0f) {
            return "Rising".intern();
        }
        if (Float.isNaN(rate)) {
            return "Undetermined".intern();
        }
        return "Rising Quickly".intern();
    }

    void showalarm(int alarm) {
        if ((alarm & 8) != 0) {
            System.out.print("Alarm ");
        }
        int withoutalarm = alarm & 7;
        switch (withoutalarm) {
            case 4:
                System.out.println("Hightest");
                return;
            case 5:
                System.out.println("Lowest");
                return;
            case 6:
                System.out.println("too high");
                return;
            case 7:
                System.out.println("too low");
                return;
            default:
                return;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!action.equals(ACTION)) {
            Log.e(LOG_ID, "action=" + action + " != " + ACTION);
            return;
        }
        Bundle extras = intent.getExtras();
        if (extras != null) {
            for (String key : extras.keySet()) {
                Object value = extras.get(key);
                Log.d("ExtrasDebug", "Key: " + key + ", Value: " + value + " (" + (value != null ? value.getClass().getSimpleName() : "null") + ")");
            }
        } else {
            Log.d("ExtrasDebug", "No extras received");
        }
        String name = extras.getString(SERIAL);
        int mgdl = extras.getInt(MGDL);
        float glucose = extras.getFloat(GLUCOSECUSTOM);
        float rate = extras.getFloat(RATE);
        int alarm = extras.getInt(ALARM);
        long time = extras.getLong(TIME);
        showalarm(alarm);
        System.out.println(name + " glucose=" + glucose + "(mgdL=" + mgdl + ") rate=" + rate + " (libreLabel=" + librelabel(rate) + ", dexcomlabel="+getdexcomlabel(rate)+") time=" + dateformat.format(new Date(time)));
    }
}
