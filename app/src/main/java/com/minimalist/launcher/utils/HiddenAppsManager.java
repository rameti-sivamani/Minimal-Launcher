package com.minimalist.launcher.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Helper class to manage hidden apps using SharedPreferences
 */
public class HiddenAppsManager {

    private static final String PREFS_NAME = "hidden_apps_prefs";
    private static final String KEY_HIDDEN_APPS = "hidden_packages";

    private final SharedPreferences prefs;

    public HiddenAppsManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Get list of hidden package names
     */
    public List<String> getHiddenApps() {
        Set<String> hiddenSet = prefs.getStringSet(KEY_HIDDEN_APPS, new HashSet<>());
        return new ArrayList<>(hiddenSet);
    }

    /**
     * Hide an app
     */
    public void hideApp(String packageName) {
        Set<String> hidden = new HashSet<>(prefs.getStringSet(KEY_HIDDEN_APPS, new HashSet<>()));
        hidden.add(packageName);
        prefs.edit().putStringSet(KEY_HIDDEN_APPS, hidden).apply();
    }

    /**
     * Unhide an app
     */
    public void unhideApp(String packageName) {
        Set<String> hidden = new HashSet<>(prefs.getStringSet(KEY_HIDDEN_APPS, new HashSet<>()));
        hidden.remove(packageName);
        prefs.edit().putStringSet(KEY_HIDDEN_APPS, hidden).apply();
    }

    /**
     * Check if an app is hidden
     */
    public boolean isHidden(String packageName) {
        Set<String> hidden = prefs.getStringSet(KEY_HIDDEN_APPS, new HashSet<>());
        return hidden.contains(packageName);
    }

    /**
     * Toggle app visibility
     */
    public boolean toggleVisibility(String packageName) {
        if (isHidden(packageName)) {
            unhideApp(packageName);
            return false; // Now visible
        } else {
            hideApp(packageName);
            return true; // Now hidden
        }
    }

    /**
     * Clear all hidden apps
     */
    public void clearAll() {
        prefs.edit().remove(KEY_HIDDEN_APPS).apply();
    }

    /**
     * Get count of hidden apps
     */
    public int getHiddenCount() {
        return prefs.getStringSet(KEY_HIDDEN_APPS, new HashSet<>()).size();
    }
}
