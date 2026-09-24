package com.minimalist.launcher.utils;

import android.content.Context;

/**
 * Text sizes (sp) for each Font Size setting: 0 = Small, 1 = Medium, 2 = Large, 3 = Extra large
 */
public final class FontScale {

    private static final float[] CLOCK = { 52f, 64f, 76f, 88f };
    private static final float[] DATE = { 14f, 16f, 19f, 22f };
    private static final float[] APP_NAME = { 16f, 19f, 23f, 27f };
    private static final float[] SECONDARY = { 12f, 14f, 16f, 18f };

    private FontScale() {
    }

    private static int level(Context context) {
        int level = Prefs.fontSize(context);
        return Math.max(0, Math.min(level, CLOCK.length - 1));
    }

    public static float clock(Context context) {
        return CLOCK[level(context)];
    }

    public static float date(Context context) {
        return DATE[level(context)];
    }

    /** App names on the home screen and in the app list */
    public static float appName(Context context) {
        return APP_NAME[level(context)];
    }

    /** Screen time, counters, search hint and other secondary text */
    public static float secondary(Context context) {
        return SECONDARY[level(context)];
    }
}
