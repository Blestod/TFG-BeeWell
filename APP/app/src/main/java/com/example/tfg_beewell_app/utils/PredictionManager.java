package com.example.tfg_beewell_app.utils;


import android.content.Context;

import com.example.tfg_beewell_app.Forecast;
import com.example.tfg_beewell_app.local.GlucoseDB;
import com.example.tfg_beewell_app.local.LocalGlucoseEntry;
import com.example.tfg_beewell_app.models.BgReading;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import android.util.Log;
import lecho.lib.hellocharts.model.PointValue;
import com.example.tfg_beewell_app.utils.Prefs;

public class PredictionManager {

    public static double getActivityEffect(int activityLevel, int duration) {
        // Coeficiente base de reducción de glucosa (mg/dL por minuto)
        Map<Integer, Double> baseReduction = new HashMap<>();
        baseReduction.put(0, 0.0);   // Baja intensidad
        baseReduction.put(1, 0.5);   // Moderada
        baseReduction.put(2, 1.0);   // Alta
        baseReduction.put(3, 2.0);   // Muy alta

        // Efecto acumulativo basado en duración e intensidad
        double activityEffect = baseReduction.getOrDefault(activityLevel, 0.0) * duration;

        return activityEffect;
    }

    public static double getActiveInsulin(String insulinType, double insulinDose, double elapsedTime, Double effect, int[] duration, double insulinSensitivity) {
        double RAPID_INSULIN_EFFECT = insulinSensitivity;
        double SLOW_INSULIN_EFFECT = insulinSensitivity * 0.6;
        int[] RAPID_INSULIN_DURATION = {60, 240};
        int[] SLOW_INSULIN_DURATION = {120, 720};

        if (effect == null) {
            effect = insulinType.equals("rapid") ? RAPID_INSULIN_EFFECT : SLOW_INSULIN_EFFECT;
        }
        if (duration == null) {
            duration = insulinType.equals("rapid") ? RAPID_INSULIN_DURATION : SLOW_INSULIN_DURATION;
        }

        int peakTime = duration[0];
        int effectDuration = duration[1];

        if (0 <= elapsedTime && elapsedTime <= peakTime) {
            return Math.max(insulinDose * (effect * (elapsedTime / peakTime)), 0);  // Pico lineal hasta el tiempo de pico
        } else if (peakTime < elapsedTime && elapsedTime <= effectDuration) {
            return Math.max(insulinDose * (effect * (1 - (elapsedTime - peakTime) / (effectDuration - peakTime))), 0);  // Disminución lineal hasta la duración del efecto
        }
        return 0;
    }

    public static double simulateFoodEffect(double carbs, double fats, double proteins, double elapsedTime, double carbRatio, double carbAbsorptionRate) {
        double carbsEffect = 0;
        double fatsEffect = 0;
        double proteinsEffect = 0;

        double carbsPerHour = Math.min(carbs, carbAbsorptionRate);
        double carbDuration = carbs / carbsPerHour * 60;
        if (0 <= elapsedTime && elapsedTime <= carbDuration) {
            carbsEffect = (carbsPerHour / 60 * elapsedTime) / carbRatio * 50;  // Assuming 50 mg/dL per unit of insulin
        }

        // Grasas (lento)
        if (0 <= elapsedTime && elapsedTime <= 240) {
            fatsEffect = fats * (1 * (elapsedTime / 240));  // Incremento gradual hasta 4 horas
        } else if (240 < elapsedTime && elapsedTime <= 480) {
            fatsEffect = fats * (1 * (1 - (elapsedTime - 240) / 240));  // Disminución entre 4-8 horas
        }

        // Proteínas (sostenido)
        if (0 <= elapsedTime && elapsedTime <= 120) {
            proteinsEffect = proteins * (1 * (elapsedTime / 120));  // Incremento hasta 2 horas
        } else if (120 < elapsedTime && elapsedTime <= 240) {
            proteinsEffect = proteins * (1 * (1 - (elapsedTime - 120) / 120));  // Disminución entre 2-4 horas
        }

        // Sumar todos los efectos y retornar el resultado en mg/dL
        return carbsEffect + fatsEffect + proteinsEffect;
    }

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
            System.out.println("time: " + e.timestamp + " bg: "+ e.glucoseValue);
        }

        // 3. Usar el modelo para predecir
        Log.d("PredictionManager", "Get prediction");
        Forecast.GlucoseForecast model = new Forecast.GlucoseForecast();
        List<PointValue> results = model.predictNextHour(readings);

        double rapidInsulinDose = 5; // Ejemplo: dosis de insulina rápida
        double slowInsulinDose = 10; // Ejemplo: dosis de insulina lenta
        double carbsIntake = 30; // Ejemplo: ingesta de carbohidratos
        double fatsIntake = 10; // Ejemplo: ingesta de grasas
        double proteinsIntake = 20; // Ejemplo: ingesta de proteinas
        int activityLevel = 1; // Ejemplo: nivel de activad
        int activityDuration = 0; // Ejemplo: duración de la actividad en minutos
        long rapidInsulinTime = System.currentTimeMillis() - 30 * 60 * 1000; // Ejemplo: insulina rápida administrada hace 30 minutos
        long slowInsulinTime = System.currentTimeMillis() - 120 * 60 * 1000; // Ejemplo: insulina lenta administrada hace 2 horas
        long foodIntakeTime = System.currentTimeMillis() - 45 * 60 * 1000; // Ejemplo: comida ingerida hace 45 minutos

        double insulinSensitivity = Prefs.getInsulinSensitivity(context);
        double carbRatio = Prefs.getCarbRatio(context);
        double carbAbsorptionRate = Prefs.getCarbAbsorptionRate(context);
        for (int i = 0; i < results.size(); i++) {
            PointValue value = results.get(i);
            float elapsedTime = value.getX() - results.get(0).getX();

            double foodEffect = simulateFoodEffect(carbsIntake, fatsIntake, proteinsIntake, foodIntakeTime + elapsedTime, carbRatio, carbAbsorptionRate);
            double rapidInsulinEffect = getActiveInsulin("rapid", rapidInsulinDose, rapidInsulinTime + elapsedTime, null, null, insulinSensitivity);
            double slowInsulinEffect = getActiveInsulin("slow", slowInsulinDose, slowInsulinTime + elapsedTime, null, null, insulinSensitivity);

            // Calcular efecto del ejercicio
            double activityEffect = getActivityEffect(activityLevel, activityDuration);
            // Aplicar ajustes
            System.out.println("Predicción no ajustada: " + value.toString());
            value.set(value.getX(), (float) (value.getY() + foodEffect - rapidInsulinEffect - slowInsulinEffect - activityEffect));
            System.out.println("Predicción ajustada: " + value.toString());
        }
        Log.d("PredictionManager", "Predicción ajustada obtenida");
        return results;
    }
}
