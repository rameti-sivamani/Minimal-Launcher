package com.minimalist.launcher.utils;

import android.app.Activity;
import android.view.View;
import android.view.Window;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * Edge-to-edge and system bar handling shared by all screens.
 * Android 15+ always draws apps edge-to-edge, so screens pad themselves with insets
 * instead of relying on status bar colors.
 */
public final class SystemBars {

    private SystemBars() {
    }

    /**
     * Draw edge-to-edge, hide the status bar (swipe down to reveal) and keep the
     * navigation bar so gesture/button navigation stays usable.
     */
    public static void apply(Activity activity, boolean darkTheme) {
        Window window = activity.getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);

        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        controller.hide(WindowInsetsCompat.Type.statusBars());
        controller.setAppearanceLightStatusBars(!darkTheme);
        controller.setAppearanceLightNavigationBars(!darkTheme);
    }

    /**
     * Add system bar and display cutout insets on top of the view's existing padding
     */
    public static void padForInsets(View view, boolean top, boolean bottom) {
        final int left = view.getPaddingLeft();
        final int topPad = view.getPaddingTop();
        final int right = view.getPaddingRight();
        final int bottomPad = view.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(
                    left + insets.left,
                    topPad + (top ? insets.top : 0),
                    right + insets.right,
                    bottomPad + (bottom ? insets.bottom : 0));
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(view);
    }
}
