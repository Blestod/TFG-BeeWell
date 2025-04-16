package com.example.tfg_beewell_app.utils;

import com.example.tfg_beewell_app.ui.VitalData;

public class SmartwatchReader {

    public static VitalData getCurrentVitals(String email) {
        VitalData data = new VitalData();

        // Simulated data
        data.setHeartRate(72f);
        data.setTemperature(36.7f);
        data.setCalories(120f);
        data.setDiastolic(78f);
        data.setSystolic(122f);
        data.setIsSleeping(false);
        data.setVitalTime(System.currentTimeMillis());
        data.setUserEmail(email);

        return data;
    }
}
