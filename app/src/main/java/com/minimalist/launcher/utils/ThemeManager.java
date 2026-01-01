package com.minimalist.launcher.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;

/**
 * Manages theme settings and provides theme utilities
 */
public class ThemeManager {

    public static final int THEME_OLED_BLACK = 0;
    public static final int THEME_DARK_GRAY = 1;
    public static final int THEME_LIGHT = 2;
    public static final int THEME_AUTO = 3;

    // Broadcast action for theme changes
    public static final String ACTION_THEME_CHANGED = "com.minimalist.launcher.THEME_CHANGED";

    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME = "selected_theme";

    private final SharedPreferences prefs;
    private final Context context;

    public ThemeManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Get current theme
     */
    public int getCurrentTheme() {
        return prefs.getInt(KEY_THEME, THEME_OLED_BLACK);
    }

    /**
     * Set theme and notify all activities
     */
    public void setTheme(int theme) {
        prefs.edit().putInt(KEY_THEME, theme).apply();
        notifyThemeChanged();
    }

    /**
     * Broadcast theme change to all listening activities
     */
    private void notifyThemeChanged() {
        Intent intent = new Intent(ACTION_THEME_CHANGED);
        context.sendBroadcast(intent);
    }

    /**
     * Get effective theme (handles auto theme)
     */
    public int getEffectiveTheme() {
        int theme = getCurrentTheme();
        if (theme == THEME_AUTO) {
            // Check system theme
            int nightMode = context.getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK;
            if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
                return THEME_OLED_BLACK;
            } else {
                return THEME_LIGHT;
            }
        }
        return theme;
    }

    /**
     * Get background color for current theme
     */
    public int getBackgroundColor() {
        switch (getEffectiveTheme()) {
            case THEME_OLED_BLACK:
                return 0xFF000000; // Pure black
            case THEME_DARK_GRAY:
                return 0xFF121212; // Dark gray
            case THEME_LIGHT:
                return 0xFFFFFFFF; // White
            default:
                return 0xFF000000;
        }
    }

    /**
     * Get primary text color for current theme
     */
    public int getTextColor() {
        switch (getEffectiveTheme()) {
            case THEME_OLED_BLACK:
            case THEME_DARK_GRAY:
                return 0xFFFFFFFF; // White text on dark
            case THEME_LIGHT:
                return 0xFF000000; // Black text on light
            default:
                return 0xFFFFFFFF;
        }
    }

    /**
     * Get secondary text color for current theme
     */
    public int getSecondaryTextColor() {
        switch (getEffectiveTheme()) {
            case THEME_OLED_BLACK:
            case THEME_DARK_GRAY:
                return 0xFFAAAAAA; // Light gray on dark
            case THEME_LIGHT:
                return 0xFF666666; // Dark gray on light
            default:
                return 0xFFAAAAAA;
        }
    }

    /**
     * Check if current theme is dark
     */
    public boolean isDarkTheme() {
        int theme = getEffectiveTheme();
        return theme == THEME_OLED_BLACK || theme == THEME_DARK_GRAY;
    }

    /**
     * Get theme name
     */
    public String getThemeName(int theme) {
        switch (theme) {
            case THEME_OLED_BLACK:
                return "OLED Black";
            case THEME_DARK_GRAY:
                return "Dark Gray";
            case THEME_LIGHT:
                return "Light";
            case THEME_AUTO:
                return "Auto";
            default:
                return "Unknown";
        }
    }
}
