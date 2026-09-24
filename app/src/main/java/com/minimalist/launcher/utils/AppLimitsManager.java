package com.minimalist.launcher.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Map;
import java.util.TreeMap;

/**
 * Per-app daily time limits in minutes (0 / absent = no limit)
 */
public class AppLimitsManager {

    private static final String PREFS_NAME = "app_limits_prefs";

    /** Choices offered in the UI; 0 removes the limit */
    public static final int[] LIMIT_OPTIONS_MINUTES = { 0, 15, 30, 45, 60, 90, 120, 180 };

    private final SharedPreferences prefs;

    public AppLimitsManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public int getLimitMinutes(String packageName) {
        return prefs.getInt(packageName, 0);
    }

    public void setLimitMinutes(String packageName, int minutes) {
        if (minutes <= 0) {
            prefs.edit().remove(packageName).apply();
        } else {
            prefs.edit().putInt(packageName, minutes).apply();
        }
    }

    /** Package name -> limit in minutes, sorted by package name */
    public Map<String, Integer> getAllLimits() {
        Map<String, Integer> limits = new TreeMap<>();
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            if (entry.getValue() instanceof Integer && (Integer) entry.getValue() > 0) {
                limits.put(entry.getKey(), (Integer) entry.getValue());
            }
        }
        return limits;
    }

    public int getLimitedAppCount() {
        return getAllLimits().size();
    }
}
