package com.example.tfg_beewell_app.ui;

public class VitalData {
    private Integer glucoseValue;
    private Float heartRate;
    private Float temperature;
    private Float calories;
    private Integer sleepDuration;
    private Float oxygenSaturation;
    private Integer vitalTime = (int)(System.currentTimeMillis() / 1000);
    private String userEmail;

    // Getters
    public Integer getGlucoseValue() { return glucoseValue; }
    public Float getHeartRate() { return heartRate; }
    public Float getTemperature() { return temperature; }
    public Float getCalories() { return calories; }

    public Float getOxygenSaturation() { return oxygenSaturation; }
    public Integer getSleepDuration() { return sleepDuration; }
    public Integer getVitalTime() { return vitalTime; }
    public String getUserEmail() { return userEmail; }

    // Setters
    public void setGlucoseValue(Integer glucoseValue) { this.glucoseValue = glucoseValue; }
    public void setHeartRate(Float heartRate) { this.heartRate = heartRate; }
    public void setTemperature(Float temperature) { this.temperature = temperature; }
    public void setCalories(Float calories) { this.calories = calories; }

    public void setOxygenSaturation(Float oxygenSaturation) { this.oxygenSaturation = oxygenSaturation; }
    public void setSleepDuration(Integer sleepDuration) { this.sleepDuration = sleepDuration; }
    public void setVitalTime(Integer vitalTime) { this.vitalTime = vitalTime; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
}
