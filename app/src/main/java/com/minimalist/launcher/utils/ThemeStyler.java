package com.minimalist.launcher.utils;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;

/**
 * Applies the current style (Aura, Mono, Paper) to common pieces of any screen, so every
 * screen shares the same colours, type and button shapes.
 */
public final class ThemeStyler {

    private ThemeStyler() {
    }

    /** Window/background, system bars and (optionally) the toolbar */
    public static void applyScreen(Activity activity, ThemeManager theme, View root, MaterialToolbar toolbar) {
        int bg = theme.getBackgroundColor();
        SystemBars.apply(activity, theme.isDarkTheme());
        activity.getWindow().getDecorView().setBackgroundColor(bg);
        if (root != null) {
            root.setBackgroundColor(bg);
        }
        if (toolbar != null) {
            toolbar.setBackgroundColor(bg);
            toolbar.setTitleTextColor(theme.getTextColor());
            if (toolbar.getNavigationIcon() != null) {
                toolbar.getNavigationIcon().setTint(theme.getTextColor());
            }
            // Toolbar title uses the style's clock face for a consistent headline look
            for (int i = 0; i < toolbar.getChildCount(); i++) {
                if (toolbar.getChildAt(i) instanceof TextView) {
                    ((TextView) toolbar.getChildAt(i)).setTypeface(theme.getClockTypeface());
                }
            }
        }
    }

    /** Set the style's body typeface on every TextView under {@code view} */
    public static void applyBodyTypeface(View view, ThemeManager theme) {
        if (view instanceof TextView) {
            ((TextView) view).setTypeface(theme.getBodyTypeface());
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyBodyTypeface(group.getChildAt(i), theme);
            }
        }
    }

    public static GradientDrawable rounded(int color, float radiusPx) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(radiusPx);
        return shape;
    }

    /** Filled accent pill button (primary action) */
    public static void accentButton(TextView button, ThemeManager theme) {
        float density = button.getResources().getDisplayMetrics().density;
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33000000),
                rounded(theme.getAccentColor(), 28 * density), null));
        button.setTextColor(theme.getOnAccentColor());
        button.setTypeface(theme.getBodyTypeface());
    }

    /** Outlined pill button (secondary action) */
    public static void outlineButton(TextView button, ThemeManager theme) {
        float density = button.getResources().getDisplayMetrics().density;
        GradientDrawable shape = rounded(theme.getBackgroundColor(), 28 * density);
        shape.setStroke(Math.round(density), theme.getSecondaryTextColor());
        button.setBackground(new RippleDrawable(
                ColorStateList.valueOf(theme.getSecondaryTextColor() & 0x33FFFFFF), shape, null));
        button.setTextColor(theme.getTextColor());
        button.setTypeface(theme.getBodyTypeface());
    }

    /** Rounded surface card */
    public static void card(View view, ThemeManager theme, float radiusDp) {
        float density = view.getResources().getDisplayMetrics().density;
        view.setBackground(rounded(theme.getSurfaceColor(), radiusDp * density));
    }
}
