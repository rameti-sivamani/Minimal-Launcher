package com.minimalist.launcher.utils;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

/**
 * Detects deliberate swipes in four directions using density-independent thresholds,
 * so gestures feel the same on every screen size.
 * Swipes that start at the far left/right edge are ignored: Android reserves those
 * for the system Back gesture.
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

    public SwipeDetector(Context context, Listener listener) {
        float density = context.getResources().getDisplayMetrics().density;
        final float minDistance = MIN_DISTANCE_DP * density;
        final float edge = EDGE_DP * density;
        final float minVelocity = ViewConfiguration.get(context).getScaledMinimumFlingVelocity() * 2f;
        final int screenWidth = context.getResources().getDisplayMetrics().widthPixels;

        detector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null) {
                    return false;
                }
                float dx = e2.getX() - e1.getX();
                float dy = e2.getY() - e1.getY();

                if (Math.abs(dx) > Math.abs(dy) * 1.2f) {
                    boolean fromEdge = e1.getX() < edge || e1.getX() > screenWidth - edge;
                    if (fromEdge || Math.abs(dx) < minDistance || Math.abs(velocityX) < minVelocity) {
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

    /**
     * Feed every touch event (from Activity.dispatchTouchEvent).
     *
     * @return true if a swipe was recognised and handled
     */
    public boolean onTouchEvent(MotionEvent event) {
        return detector.onTouchEvent(event);
    }
}
