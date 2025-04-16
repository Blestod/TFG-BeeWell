package com.example.tfg_beewell_app.ui;

public class VitalData {
    private Integer glucoseValue;
    private Float heartRate;
    private Float temperature;
    private Float calories;
    private Float diastolic;
    private Float systolic;
    private Boolean isSleeping;
    private Long vitalTime;
    private String userEmail;

    // Getters
    public Integer getGlucoseValue() { return glucoseValue; }
    public Float getHeartRate() { return heartRate; }
    public Float getTemperature() { return temperature; }
    public Float getCalories() { return calories; }
    public Float getDiastolic() { return diastolic; }
    public Float getSystolic() { return systolic; }
    public Boolean getIsSleeping() { return isSleeping; }
    public Long getVitalTime() { return vitalTime; }
    public String getUserEmail() { return userEmail; }

    // Setters
    public void setGlucoseValue(Integer glucoseValue) { this.glucoseValue = glucoseValue; }
    public void setHeartRate(Float heartRate) { this.heartRate = heartRate; }
    public void setTemperature(Float temperature) { this.temperature = temperature; }
    public void setCalories(Float calories) { this.calories = calories; }
    public void setDiastolic(Float diastolic) { this.diastolic = diastolic; }
    public void setSystolic(Float systolic) { this.systolic = systolic; }
    public void setIsSleeping(Boolean isSleeping) { this.isSleeping = isSleeping; }
    public void setVitalTime(Long vitalTime) { this.vitalTime = vitalTime; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
}
