package com.example.tfg_beewell_app.models;

public class BgReading {
    public long timestamp;
    public double calculated_value;
    public double filtered_calculated_value;
    public double raw_calculated;
    public boolean ignoreForStats;

    public boolean isBackfilled() {
        return false;
    }

    public boolean isRemote() {
        return false;
    }
}
