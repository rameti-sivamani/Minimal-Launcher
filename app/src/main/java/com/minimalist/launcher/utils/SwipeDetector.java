package com.minimalist.launcher.utils;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Detects deliberate swipes in four directions using density-independent thresholds,
 * so gestures feel the same on every screen size.
 * Swipes that start inside Android's system gesture zones (the Back edges and the
 * bottom Home/Recents bar) are ignored, so the launcher never fights the system
 * gestures and the Recents animation stays smooth.
 */
public class SwipeDetector {

    public enum Direction { LEFT, RIGHT, UP, DOWN }

    public interface Listener {
        /** @return true if the swipe was handled */
        boolean onSwipe(Direction direction);
    }

    private static final float MIN_DISTANCE_DP = 60f;
    private static final float EDGE_DP = 24f;

    private final GestureDetector detector;

    /**
     * @param root the full-screen view whose touches are fed in; used for its size and
     *             for the system gesture insets
     */
    public SwipeDetector(Context context, View root, Listener listener) {
        float density = context.getResources().getDisplayMetrics().density;
        final float minDistance = MIN_DISTANCE_DP * density;
        final float minEdge = EDGE_DP * density;
        final float minVelocity = ViewConfiguration.get(context).getScaledMinimumFlingVelocity() * 2f;

        detector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || startsInSystemGestureZone(e1, root, minEdge)) {
                    return false;
                }
                float dx = e2.getX() - e1.getX();
                float dy = e2.getY() - e1.getY();

                if (Math.abs(dx) > Math.abs(dy) * 1.2f) {
                    if (Math.abs(dx) < minDistance || Math.abs(velocityX) < minVelocity) {
                        return false;
                    }
                    return listener.onSwipe(dx > 0 ? Direction.RIGHT : Direction.LEFT);
                }
                if (Math.abs(dy) > Math.abs(dx) * 1.2f) {
                    if (Math.abs(dy) < minDistance || Math.abs(velocityY) < minVelocity) {
                        return false;
                    }
                    return listener.onSwipe(dy > 0 ? Direction.DOWN : Direction.UP);
                }
                return false;
            }
        });
    }

    private static boolean startsInSystemGestureZone(MotionEvent down, View root, float minEdge) {
        int width = root.getWidth();
        int height = root.getHeight();
        if (width == 0 || height == 0) {
            return false;
        }
        Insets gestures = Insets.NONE;
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(root);
        if (insets != null) {
            gestures = insets.getInsets(WindowInsetsCompat.Type.systemGestures());
        }
        float left = Math.max(gestures.left, minEdge);
        float right = Math.max(gestures.right, minEdge);
        float x = down.getX();
        float y = down.getY();
        return x < left || x > width - right
                || y > height - gestures.bottom
                || y < gestures.top;
    }

    /**
     * Feed every touch event (from Activity.dispatchTouchEvent).
     *
     * @return true if a swipe was recognised and handled
     */
    public boolean onTouchEvent(MotionEvent event) {
        return detector.onTouchEvent(event);
    }
}
