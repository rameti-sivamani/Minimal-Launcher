package com.minimalist.launcher.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.minimalist.launcher.data.wellbeing.WellbeingStore;

import java.util.HashSet;
import java.util.Set;

/**
 * Lock-in focus sessions: for a set time the launcher only shows and opens the apps
 * chosen for the session. State lives in SharedPreferences so it survives the launcher
 * being restarted; a session simply ends when its end time passes.
 */
public class LockInManager {

    private static final String PREFS_NAME = "lock_in_prefs";
    private static final String KEY_END = "end_at";
    private static final String KEY_START = "start_at";
    private static final String KEY_LABEL = "label";
    private static final String KEY_APPS = "allowed_apps";
    private static final String KEY_COUNTED = "counted";
    private static final String PREFIX_MINUTES = "minutes_";

    public static final int[] DURATION_OPTIONS_MINUTES = { 15, 25, 45, 60, 90 };

    private final SharedPreferences prefs;

    public LockInManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void start(int minutes, String label, Set<String> allowedPackages) {
        long now = System.currentTimeMillis();
        prefs.edit()
                .putLong(KEY_START, now)
                .putLong(KEY_END, now + minutes * 60_000L)
                .putString(KEY_LABEL, label == null ? "" : label.trim())
                .putStringSet(KEY_APPS, new HashSet<>(allowedPackages))
                .putBoolean(KEY_COUNTED, false)
                .apply();
    }

    /** End the session now (only the minutes actually focused are counted) */
    public void stop() {
        countFocusedMinutes(System.currentTimeMillis());
        prefs.edit().putLong(KEY_END, 0).apply();
    }

    public boolean isActive() {
        long end = prefs.getLong(KEY_END, 0);
        if (end == 0) {
            return false;
        }
        if (System.currentTimeMillis() >= end) {
            countFocusedMinutes(end); // Finished on its own: count the full session once
            return false;
        }
        return true;
    }

    public long getRemainingMillis() {
        return Math.max(0, prefs.getLong(KEY_END, 0) - System.currentTimeMillis());
    }

    public long getTotalMillis() {
        return Math.max(1, prefs.getLong(KEY_END, 0) - prefs.getLong(KEY_START, 0));
    }

    public String getLabel() {
        return prefs.getString(KEY_LABEL, "");
    }

    public Set<String> getAllowedApps() {
        return new HashSet<>(prefs.getStringSet(KEY_APPS, new HashSet<>()));
    }

    /** Whether this app may be opened right now */
    public boolean isAllowed(String packageName) {
        return !isActive() || getAllowedApps().contains(packageName);
    }

    /** Minutes of completed focus on the day {@code daysAgo} before today */
    public int getFocusMinutes(int daysAgo) {
        return prefs.getInt(PREFIX_MINUTES + WellbeingStore.dateKey(daysAgo), 0);
    }

    private void countFocusedMinutes(long until) {
        if (prefs.getBoolean(KEY_COUNTED, true)) {
            return;
        }
        long start = prefs.getLong(KEY_START, until);
        int minutes = (int) Math.max(0, (Math.min(until, prefs.getLong(KEY_END, until)) - start) / 60_000L);
        String key = PREFIX_MINUTES + WellbeingStore.dateKey(0);
        prefs.edit()
                .putInt(key, prefs.getInt(key, 0) + minutes)
                .putBoolean(KEY_COUNTED, true)
                .apply();
    }
}
