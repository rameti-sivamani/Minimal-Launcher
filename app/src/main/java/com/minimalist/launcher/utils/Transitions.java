package com.minimalist.launcher.utils;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Build;

import com.minimalist.launcher.R;

/**
 * Slide animations that match the swipe that opened or closed a screen
 */
public final class Transitions {

    public enum Slide { FROM_BOTTOM, FROM_LEFT }

    private Transitions() {
    }

    public static void start(Activity activity, Intent intent, Slide slide) {
        int enter = slide == Slide.FROM_BOTTOM ? R.anim.slide_in_bottom : R.anim.slide_in_left;
        ActivityOptions options = ActivityOptions.makeCustomAnimation(activity, enter, R.anim.hold);
        activity.startActivity(intent, options.toBundle());
    }

    /** Finish with the reverse animation */
    @SuppressWarnings("deprecation") // overridePendingTransition is the only option below API 34
    public static void finish(Activity activity, Slide slide) {
        int exit = slide == Slide.FROM_BOTTOM ? R.anim.slide_out_bottom : R.anim.slide_out_left;
        activity.finish();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, R.anim.hold, exit);
        } else {
            activity.overridePendingTransition(R.anim.hold, exit);
        }
    }
}
