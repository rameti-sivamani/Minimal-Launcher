package com.minimalist.launcher.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Single place for launcher preference keys and defaults
 */
public final class Prefs {

    public static final String KEY_SHOW_ICONS = "show_icons";
    public static final String KEY_WARNINGS_ENABLED = "warnings_enabled";
    public static final String KEY_WARNING_THRESHOLD = "warning_threshold";
    public static final String KEY_APP_SORT_ORDER = "app_sort_order";
    public static final String KEY_SHOW_DAY_OF_WEEK = "show_day_of_week";
    public static final String KEY_SHOW_QUICK_INFO = "show_quick_info";
    public static final String KEY_SHOW_SCREEN_TIME = "show_screen_time";
    public static final String KEY_FONT_SIZE = "font_size";

    public static final int SORT_ALPHABETICAL = 0;
    public static final int SORT_MOST_USED = 1;
    public static final int SORT_RECENTLY_INSTALLED = 2;

    public static final int DEFAULT_WARNING_THRESHOLD = 10;

    private Prefs() {
    }

    @SuppressWarnings("deprecation") // Keeps the existing preference file used by installed versions
    public static SharedPreferences get(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    }

    public static boolean showIcons(Context context) {
        return get(context).getBoolean(KEY_SHOW_ICONS, false);
    }

    public static boolean warningsEnabled(Context context) {
        return get(context).getBoolean(KEY_WARNINGS_ENABLED, true);
    }

    public static int warningThreshold(Context context) {
        return get(context).getInt(KEY_WARNING_THRESHOLD, DEFAULT_WARNING_THRESHOLD);
    }

    public static int sortOrder(Context context) {
        return get(context).getInt(KEY_APP_SORT_ORDER, SORT_ALPHABETICAL);
    }

    public static boolean showDayOfWeek(Context context) {
        return get(context).getBoolean(KEY_SHOW_DAY_OF_WEEK, true);
    }

    public static boolean showQuickInfo(Context context) {
        return get(context).getBoolean(KEY_SHOW_QUICK_INFO, false);
    }

    public static boolean showScreenTime(Context context) {
        return get(context).getBoolean(KEY_SHOW_SCREEN_TIME, true);
    }

    public static int fontSize(Context context) {
        return get(context).getInt(KEY_FONT_SIZE, 1);
    }
}
