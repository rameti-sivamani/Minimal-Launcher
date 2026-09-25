package com.minimalist.launcher.data.wellbeing;

import java.util.List;

/**
 * Streak = consecutive finished days (yesterday backwards) whose screen time stayed at
 * or under the daily goal. A day without data (before install, or without usage
 * access) ends the streak. Pure Java so it can be unit tested.
 */
public final class StreakCalculator {

    private StreakCalculator() {
    }

    /**
     * @param totalsNewestFirst screen time of past days, index 0 = yesterday; null = no data
     * @param goalMillis        daily goal
     */
    public static int currentStreak(List<Long> totalsNewestFirst, long goalMillis) {
        int streak = 0;
        for (Long total : totalsNewestFirst) {
            if (total == null || total > goalMillis) {
                break;
            }
            streak++;
        }
        return streak;
    }

    /**
     * The streak shown today: past days, plus today if today is still under the goal
     * (today can only extend the streak, never break it, until it is over).
     */
    public static int displayStreak(int pastStreak, long todayMillis, long goalMillis) {
        return todayMillis >= 0 && todayMillis <= goalMillis ? pastStreak + 1 : pastStreak;
    }

    /** 0..1 share of the goal used today */
    public static float goalProgress(long todayMillis, long goalMillis) {
        if (goalMillis <= 0 || todayMillis <= 0) {
            return 0f;
        }
        return Math.min(1f, (float) todayMillis / goalMillis);
    }
}
