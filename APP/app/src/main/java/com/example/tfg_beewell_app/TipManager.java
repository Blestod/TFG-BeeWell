package com.example.tfg_beewell_app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class TipManager {

    private static final String PREFS_NAME = "tip_pref";
    private static final String LAST_SHOWN_KEY = "last_shown";
    private static final String LAST_TIP_KEY = "last_tip";
    private static final String SHOWN_DATE_KEY = "shown_date";
    private static final String USED_CATEGORIES_KEY = "used_categories";
    private static final String LAST_SLOT_KEY = "last_slot";

    public static String getDailyTip(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastShown = prefs.getLong(LAST_SHOWN_KEY, 0);
        String lastTip = prefs.getString(LAST_TIP_KEY, null);
        long now = System.currentTimeMillis();

        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int day = cal.get(Calendar.DAY_OF_YEAR);
        int year = cal.get(Calendar.YEAR);
        String todayKey = year + "-" + day;

        String currentSlot;
        if (hour >= 7 && hour < 10) {
            currentSlot = "morning";
        } else if (hour >= 13 && hour < 15) {
            currentSlot = "afternoon";
        } else if (hour >= 20 && hour < 22) {
            currentSlot = "night";
        } else {
            currentSlot = "none";
        }

        String savedDate = prefs.getString(SHOWN_DATE_KEY, "");
        String lastSlot = prefs.getString(LAST_SLOT_KEY, "");

        String used = prefs.getString(USED_CATEGORIES_KEY, "");
        String[] usedArray = used.split(",");
        List<String> usedList = new ArrayList<>();
        for (String u : usedArray) if (!u.isEmpty()) usedList.add(u);

        if (currentSlot.equals(lastSlot) && savedDate.equals(todayKey) && lastTip != null) {
            return lastTip;
        }

        try {
            Resources res = context.getResources();
            InputStream input = res.openRawResource(R.raw.tips);
            byte[] buffer = new byte[input.available()];
            input.read(buffer);
            input.close();

            String json = new String(buffer, "UTF-8");
            JSONObject obj = new JSONObject(json);

            if (!savedDate.equals(todayKey)) {
                usedList.clear();
            }

            Iterator<String> keys = obj.keys();
            List<String> available = new ArrayList<>();
            while (keys.hasNext()) {
                String cat = keys.next();
                if (!usedList.contains(cat)) {
                    available.add(cat);
                }
            }

            if (available.isEmpty()) {
                return lastTip != null ? lastTip : "💡 Sigue cuidándote cada día.";
            }

            String randomCategory = available.get(new Random().nextInt(available.size()));
            JSONArray tipsArray = obj.getJSONArray(randomCategory);
            String newTip = "💡 " + tipsArray.getString(new Random().nextInt(tipsArray.length()));

            usedList.add(randomCategory);
            StringBuilder updatedCats = new StringBuilder();
            for (String c : usedList) {
                updatedCats.append(c).append(",");
            }

            prefs.edit()
                    .putLong(LAST_SHOWN_KEY, now)
                    .putString(LAST_TIP_KEY, newTip)
                    .putString(SHOWN_DATE_KEY, todayKey)
                    .putString(USED_CATEGORIES_KEY, updatedCats.toString())
                    .putString(LAST_SLOT_KEY, currentSlot)
                    .apply();

            return newTip;

        } catch (Exception e) {
            Log.e("TipManager", "Error loading tip: " + e.getMessage());
            return "💡 Sigue cuidándote cada día.";
        }
    }
}
