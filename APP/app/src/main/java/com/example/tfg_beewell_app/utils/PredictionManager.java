package com.example.tfg_beewell_app.utils;

import android.content.Context;

import com.example.tfg_beewell_app.Forecast;
import com.example.tfg_beewell_app.local.GlucoseDB;
import com.example.tfg_beewell_app.local.LocalGlucoseEntry;
import com.example.tfg_beewell_app.models.BgReading;

import java.util.ArrayList;
import java.util.List;

import lecho.lib.hellocharts.model.PointValue;

public class PredictionManager {

    public static List<PointValue> getPredictionForNextHour(Context context) {
        long cutoff = System.currentTimeMillis() - 8 * 60 * 60 * 1000;

        // 1. Leer desde Room
        List<LocalGlucoseEntry> entries = GlucoseDB.getInstance(context)
                .glucoseDao()
                .getLast8Hours(cutoff);

        // 2. Convertir a BgReading
        List<BgReading> readings = new ArrayList<>();
        for (LocalGlucoseEntry e : entries) {
            BgReading r = new BgReading();
            r.timestamp = e.timestamp;
            r.calculated_value = e.glucoseValue;
            readings.add(r);
        }

        // 3. Usar el modelo para predecir
        Forecast.GlucoseForecast model = new Forecast.GlucoseForecast();
        return model.predictNextHour(readings);
    }
}

