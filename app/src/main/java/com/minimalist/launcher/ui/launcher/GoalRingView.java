package com.minimalist.launcher.ui.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

/**
 * Circular progress ring for today's screen time against the daily goal,
 * with the percentage in the middle.
 */
public class GoalRingView extends View {

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();
    private float progress = 0f;
    private String label = "";

    public GoalRingView(Context context) {
        this(context, null);
    }

    public GoalRingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        float density = getResources().getDisplayMetrics().density;
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(6 * density);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(6 * density);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(13 * getResources().getDisplayMetrics().scaledDensity);
        textPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        setColors(0xFF2A2A2D, 0xFFC6FF3D, 0xFFF4F4F0);
    }

    /** Size of the centre label in sp and ring thickness in dp */
    public void setLabelStyle(float textSizeSp, float strokeDp, Typeface typeface) {
        float density = getResources().getDisplayMetrics().density;
        textPaint.setTextSize(textSizeSp * getResources().getDisplayMetrics().scaledDensity);
        textPaint.setTypeface(typeface);
        trackPaint.setStrokeWidth(strokeDp * density);
        progressPaint.setStrokeWidth(strokeDp * density);
        invalidate();
    }

    public void setColors(int track, int progressColor, int text) {
        trackPaint.setColor(track);
        progressPaint.setColor(progressColor);
        textPaint.setColor(text);
        invalidate();
    }

    /**
     * @param progress 0..1 share of the goal used
     * @param label    text in the middle, e.g. "60%"
     */
    public void setProgress(float progress, String label) {
        this.progress = Math.max(0f, Math.min(1f, progress));
        this.label = label != null ? label : "";
        setContentDescription(this.label);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float inset = trackPaint.getStrokeWidth() / 2f;
        bounds.set(inset, inset, getWidth() - inset, getHeight() - inset);
        canvas.drawArc(bounds, 0, 360, false, trackPaint);
        if (progress > 0f) {
            canvas.drawArc(bounds, -90, 360 * progress, false, progressPaint);
        }
        float y = getHeight() / 2f - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(label, getWidth() / 2f, y, textPaint);
    }
}
