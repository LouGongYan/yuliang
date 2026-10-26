package com.yuliang.ledger.ui.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/** A rounded, segmented bar used for the category breakdown. */
public class StackedBarView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private double[] values = new double[0];
    private int[] colors = new int[0];
    private int trackColor = 0xFFE3EEEA;
    private float shown = 1f;
    private ValueAnimator animator;

    public StackedBarView(Context context) {
        super(context);
        setWillNotDraw(false);
    }

    public void setTrackColor(int color) {
        trackColor = color;
        invalidate();
    }

    public void setData(double[] values, int[] colors, boolean animate) {
        this.values = values == null ? new double[0] : values;
        this.colors = colors == null ? new int[0] : colors;
        if (animator != null) animator.cancel();
        if (!animate || !isAttachedToWindow()) {
            shown = 1f;
            invalidate();
            return;
        }
        shown = 0f;
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(600);
        animator.setInterpolator(new DecelerateInterpolator(2f));
        animator.addUpdateListener(a -> {
            shown = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;
        float r = h / 2f;
        paint.setColor(trackColor);
        rect.set(0, 0, w, h);
        canvas.drawRoundRect(rect, r, r, paint);

        double total = 0;
        for (double v : values) total += Math.max(0, v);
        if (total <= 0 || values.length == 0) return;

        float gap = Math.min(h * 0.6f, w * 0.006f);
        float x = 0;
        float available = Math.max(0, w - gap * Math.max(0, values.length - 1));
        for (int i = 0; i < values.length; i++) {
            double v = Math.max(0, values[i]);
            if (v <= 0) continue;
            float seg = (float) (available * v / total) * shown;
            float left = x;
            float right = x + seg;
            if (right - left < h * 0.65f) right = left + h * 0.65f;
            if (right > w) right = w;
            paint.setColor(i < colors.length ? colors[i] : trackColor);
            rect.set(left, 0, right, h);
            canvas.drawRoundRect(rect, r, r, paint);
            x = right + gap;
            if (x >= w) break;
        }
    }
}
