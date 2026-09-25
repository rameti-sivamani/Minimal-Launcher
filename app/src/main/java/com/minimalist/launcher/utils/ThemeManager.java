package com.minimalist.launcher.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Typeface;

import com.minimalist.launcher.R;

/**
 * Visual styles and accent colours.
 *
 * Styles: Aura (bold, dark, one bright accent), Mono (black, serif clock, typewriter
 * names), Paper (warm off-white, serif) and Auto (Aura at night, Paper by day).
 * Every screen reads its colours and typefaces from here and re-applies them in onResume.
 */
public class ThemeManager {

    public static final int STYLE_AURA = 0;
    public static final int STYLE_MONO = 1;
    public static final int STYLE_PAPER = 2;
    public static final int STYLE_AUTO = 3;

    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_STYLE = "style";
    private static final String KEY_ACCENT = "accent_index";
    // v1 key: 0 OLED black, 1 dark gray, 2 light, 3 auto
    private static final String KEY_LEGACY_THEME = "selected_theme";

    /** Accents for the dark styles (bright) and for Paper (deep, readable on cream) */
    private static final int[] DARK_ACCENTS = { 0xFFC6FF3D, 0xFFFF7A45, 0xFF8FB8FF, 0xFFF7A8FF };
    private static final int[] PAPER_ACCENTS = { 0xFF8A4B2A, 0xFF2F5D50, 0xFF2B4C7E, 0xFF8E3B5F };
    private static final int MONO_DEFAULT_ACCENT = 0xFFF2F2EE;

    private final SharedPreferences prefs;
    private final Context context;

    public ThemeManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ---------------------------------------------------------------------
    // Settings
    // ---------------------------------------------------------------------

    /** The chosen style (may be STYLE_AUTO) */
    public int getCurrentTheme() {
        if (!prefs.contains(KEY_STYLE) && prefs.contains(KEY_LEGACY_THEME)) {
            // Keep the look people already chose in v1
            int legacy = prefs.getInt(KEY_LEGACY_THEME, 0);
            int style = legacy == 2 ? STYLE_PAPER : legacy == 3 ? STYLE_AUTO : STYLE_MONO;
            prefs.edit().putInt(KEY_STYLE, style).apply();
            return style;
        }
        return prefs.getInt(KEY_STYLE, STYLE_AURA);
    }

    public void setTheme(int style) {
        prefs.edit().putInt(KEY_STYLE, style).apply();
    }

    public int getAccentIndex() {
        return prefs.getInt(KEY_ACCENT, 0);
    }

    public void setAccentIndex(int index) {
        prefs.edit().putInt(KEY_ACCENT, index).apply();
    }

    /** The four accent choices for the style in effect now */
    public int[] getAccentOptions() {
        return getEffectiveTheme() == STYLE_PAPER ? PAPER_ACCENTS.clone() : DARK_ACCENTS.clone();
    }

    /** The style in effect right now (resolves Auto) */
    public int getEffectiveTheme() {
        int style = getCurrentTheme();
        if (style == STYLE_AUTO) {
            int nightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            return nightMode == Configuration.UI_MODE_NIGHT_YES ? STYLE_AURA : STYLE_PAPER;
        }
        return style;
    }

    /** Changes whenever anything visual changes; screens compare it to skip re-styling */
    public String getSignature() {
        return getEffectiveTheme() + ":" + getAccentIndex();
    }

    // ---------------------------------------------------------------------
    // Colours
    // ---------------------------------------------------------------------

    public boolean isDarkTheme() {
        return getEffectiveTheme() != STYLE_PAPER;
    }

    public int getBackgroundColor() {
        switch (getEffectiveTheme()) {
            case STYLE_PAPER:
                return 0xFFEFE9DF;
            case STYLE_MONO:
                return 0xFF000000;
            default:
                return 0xFF0B0B0C;
        }
    }

    /** Cards and pills */
    public int getSurfaceColor() {
        switch (getEffectiveTheme()) {
            case STYLE_PAPER:
                return 0xFFE4DCCE;
            case STYLE_MONO:
                return 0xFF111111;
            default:
                return 0xFF18181A;
        }
    }

    public int getTextColor() {
        return getEffectiveTheme() == STYLE_PAPER ? 0xFF27241F : 0xFFF4F4F0;
    }

    public int getSecondaryTextColor() {
        switch (getEffectiveTheme()) {
            case STYLE_PAPER:
                return 0xFF5E574D;
            case STYLE_MONO:
                return 0xFF9A9A94;
            default:
                return 0xFFA3A39E;
        }
    }

    public int getAccentColor() {
        int style = getEffectiveTheme();
        int index = Math.max(0, Math.min(getAccentIndex(), 3));
        if (style == STYLE_PAPER) {
            return PAPER_ACCENTS[index];
        }
        if (style == STYLE_MONO && index == 0) {
            return MONO_DEFAULT_ACCENT; // Mono stays monochrome unless a colour is picked
        }
        return DARK_ACCENTS[index];
    }

    /** Text drawn on top of the accent colour */
    public int getOnAccentColor() {
        return getEffectiveTheme() == STYLE_PAPER ? 0xFFFFFFFF : 0xFF0B0B0C;
    }

    // ---------------------------------------------------------------------
    // Type (system fonts only: nothing is downloaded)
    // ---------------------------------------------------------------------

    public Typeface getClockTypeface() {
        switch (getEffectiveTheme()) {
            case STYLE_PAPER:
            case STYLE_MONO:
                return Typeface.create("serif", Typeface.NORMAL);
            default:
                return Typeface.create("sans-serif-light", Typeface.NORMAL);
        }
    }

    public Typeface getBodyTypeface() {
        switch (getEffectiveTheme()) {
            case STYLE_MONO:
                return Typeface.MONOSPACE;
            case STYLE_PAPER:
                return Typeface.create("sans-serif", Typeface.NORMAL);
            default:
                return Typeface.create("sans-serif-medium", Typeface.NORMAL);
        }
    }

    /** Clock letter spacing in em */
    public float getClockLetterSpacing() {
        return getEffectiveTheme() == STYLE_AURA ? -0.05f : -0.02f;
    }

    /** Mono shows app names in lower case */
    public boolean useLowercaseNames() {
        return getEffectiveTheme() == STYLE_MONO;
    }

    public String getThemeName(int style) {
        String[] names = context.getResources().getStringArray(R.array.themes);
        return style >= 0 && style < names.length ? names[style] : names[STYLE_AURA];
    }

    public String getAccentName(int index) {
        String[] names = context.getResources().getStringArray(
                getEffectiveTheme() == STYLE_PAPER ? R.array.accents_paper : R.array.accents_dark);
        return index >= 0 && index < names.length ? names[index] : names[0];
    }
}
