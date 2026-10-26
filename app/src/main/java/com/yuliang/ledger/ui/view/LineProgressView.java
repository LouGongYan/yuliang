package com.yuliang.ledger.ui.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/** A thin, rounded progress line that animates to its new value. */
public class LineProgressView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final int trackColor;
    private final int fillColor;
    private final int overColor;
    private float progress;
    private float shown;
    private ValueAnimator animator;

    public LineProgressView(Context context, int trackColor, int fillColor, int overColor) {
        super(context);
        this.trackColor = trackColor;
        this.fillColor = fillColor;
        this.overColor = overColor;
        setWillNotDraw(false);
    }

    public void setProgress(float value, boolean animate) {
        float target = Math.max(0, Math.min(2f, value));
        if (animator != null) animator.cancel();
        if (!animate || !isAttachedToWindow()) {
            progress = target;
            shown = target;
            invalidate();
            return;
        }
        progress = target;
        animator = ValueAnimator.ofFloat(shown, target);
        animator.setDuration(620);
        animator.setInterpolator(new DecelerateInterpolator(2f));
        animator.addUpdateListener(a -> {
            shown = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float h = getHeight();
        float w = getWidth();
        if (h <= 0 || w <= 0) return;
        float r = h / 2f;
        paint.setColor(trackColor);
        rect.set(0, 0, w, h);
        canvas.drawRoundRect(rect, r, r, paint);

        float fill = Math.min(1f, Math.max(0f, shown));
        paint.setColor(progress > 1f ? overColor : fillColor);
        rect.set(0, 0, Math.max(h, w * fill), h);
        canvas.drawRoundRect(rect, r, r, paint);
    }
}
