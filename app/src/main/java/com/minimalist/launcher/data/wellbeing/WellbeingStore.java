package com.minimalist.launcher.data.wellbeing;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * On-device store for the wellbeing features: daily goal, per-day screen time history
 * (Android keeps usage events for only about a week, so finished days are saved here
 * for streaks), today's intention, "I don't need it" taps and the apps that get a
 * mindful pause before opening.
 */
public class WellbeingStore {

    private static final String PREFS_NAME = "wellbeing_prefs";
    private static final String KEY_GOAL_MINUTES = "goal_minutes";
    private static final String KEY_INTENTION_TEXT = "intention_text";
    private static final String KEY_INTENTION_DATE = "intention_date";
    private static final String KEY_PAUSE_APPS = "pause_apps";
    private static final String PREFIX_TOTAL = "total_";
    private static final String PREFIX_SAID_NO = "said_no_";

    public static final int DEFAULT_GOAL_MINUTES = 120;
    public static final int[] GOAL_OPTIONS_MINUTES = { 30, 60, 90, 120, 150, 180, 240, 300 };
    private static final int HISTORY_DAYS = 60;

    private final SharedPreferences prefs;

    public WellbeingStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** yyyy-MM-dd of the day {@code daysAgo} before today */
    public static String dateKey(int daysAgo) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.getTime());
    }

    // Goal ---------------------------------------------------------------

    public int getGoalMinutes() {
        return prefs.getInt(KEY_GOAL_MINUTES, DEFAULT_GOAL_MINUTES);
    }

    public long getGoalMillis() {
        return getGoalMinutes() * 60_000L;
    }

    public void setGoalMinutes(int minutes) {
        prefs.edit().putInt(KEY_GOAL_MINUTES, minutes).apply();
    }

    // Daily totals and streak ---------------------------------------------

    public boolean hasDailyTotal(int daysAgo) {
        return prefs.contains(PREFIX_TOTAL + dateKey(daysAgo));
    }

    public void saveDailyTotal(int daysAgo, long millis) {
        prefs.edit().putLong(PREFIX_TOTAL + dateKey(daysAgo), millis).apply();
    }

    /** Null when no data was recorded for that day */
    public Long getDailyTotal(int daysAgo) {
        String key = PREFIX_TOTAL + dateKey(daysAgo);
        return prefs.contains(key) ? prefs.getLong(key, 0) : null;
    }

    /** Streak of finished days (yesterday backwards) under the goal */
    public int getPastStreak() {
        List<Long> totals = new ArrayList<>();
        for (int daysAgo = 1; daysAgo <= HISTORY_DAYS; daysAgo++) {
            totals.add(getDailyTotal(daysAgo));
        }
        return StreakCalculator.currentStreak(totals, getGoalMillis());
    }

    /** Drop history older than the window the streak looks at */
    public void pruneHistory() {
        SharedPreferences.Editor editor = prefs.edit();
        String oldest = dateKey(HISTORY_DAYS);
        for (String key : prefs.getAll().keySet()) {
            if ((key.startsWith(PREFIX_TOTAL) && key.substring(PREFIX_TOTAL.length()).compareTo(oldest) < 0)
                    || (key.startsWith(PREFIX_SAID_NO) && key.substring(PREFIX_SAID_NO.length()).compareTo(oldest) < 0)) {
                editor.remove(key);
            }
        }
        editor.apply();
    }

    // Intention -------------------------------------------------------------

    /** Today's intention, or null if none has been set today */
    public String getTodayIntention() {
        if (!dateKey(0).equals(prefs.getString(KEY_INTENTION_DATE, ""))) {
            return null;
        }
        String text = prefs.getString(KEY_INTENTION_TEXT, "");
        return text.isEmpty() ? null : text;
    }

    public void setTodayIntention(String text) {
        prefs.edit()
                .putString(KEY_INTENTION_TEXT, text == null ? "" : text.trim())
                .putString(KEY_INTENTION_DATE, dateKey(0))
                .apply();
    }

    // "I don't need it" -----------------------------------------------------

    public void recordSaidNo() {
        String key = PREFIX_SAID_NO + dateKey(0);
        prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply();
    }

    public int getSaidNo(int daysAgo) {
        return prefs.getInt(PREFIX_SAID_NO + dateKey(daysAgo), 0);
    }

    // Mindful pause apps ------------------------------------------------------

    public boolean isPauseApp(String packageName) {
        return prefs.getStringSet(KEY_PAUSE_APPS, new HashSet<>()).contains(packageName);
    }

    public void setPauseApp(String packageName, boolean pause) {
        Set<String> apps = new HashSet<>(prefs.getStringSet(KEY_PAUSE_APPS, new HashSet<>()));
        if (pause) {
            apps.add(packageName);
        } else {
            apps.remove(packageName);
        }
        prefs.edit().putStringSet(KEY_PAUSE_APPS, apps).apply();
    }
}
