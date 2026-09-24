package com.minimalist.launcher.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import com.minimalist.launcher.R;

/**
 * Manages theme settings and provides theme utilities
 */
public class ThemeManager {

    public static final int THEME_OLED_BLACK = 0;
    public static final int THEME_DARK_GRAY = 1;
    public static final int THEME_LIGHT = 2;
    public static final int THEME_AUTO = 3;

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
     * Save the theme. Other screens re-apply it in onResume.
     */
    public void setTheme(int theme) {
        prefs.edit().putInt(KEY_THEME, theme).apply();
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
        String[] names = context.getResources().getStringArray(R.array.themes);
        return theme >= 0 && theme < names.length ? names[theme] : names[THEME_OLED_BLACK];
    }
}
