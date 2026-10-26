package com.yuliang.ledger.ui.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/** Seven little rounded bars for the "monthly rhythm" card. */
public class MiniBarsView extends View {

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private double[] values = new double[7];
    private String[] labels = new String[]{"", "", "", "", "", "", ""};
    private int highlight = 6;
    private int accent = 0xFF0FA396;
    private int accentSoft = 0xFFDCF4EF;
    private int track = 0xFFEDF4F2;
    private int labelColor = 0xFF8A9D97;
    private float shown = 1f;
    private ValueAnimator animator;
    private float density = 3f;

    public MiniBarsView(Context context) {
        super(context);
        setWillNotDraw(false);
        density = context.getResources().getDisplayMetrics().density;
        labelPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setColors(int accent, int accentSoft, int track, int labelColor) {
        this.accent = accent;
        this.accentSoft = accentSoft;
        this.track = track;
        this.labelColor = labelColor;
        invalidate();
    }

    public void setData(double[] values, String[] labels, int highlight, boolean animate) {
        this.values = values == null ? new double[7] : values;
        this.labels = labels == null ? new String[]{""} : labels;
        this.highlight = highlight;
        if (animator != null) animator.cancel();
        if (!animate || !isAttachedToWindow()) {
            shown = 1f;
            invalidate();
            return;
        }
        shown = 0f;
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(680);
        animator.setInterpolator(new DecelerateInterpolator(2.2f));
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

        float labelH = 18f * density;
        float plotTop = 6f * density;
        float plotBottom = h - labelH;
        float plotH = Math.max(1f, plotBottom - plotTop);
        int n = Math.max(1, values.length);
        float slot = w / n;
        float barW = Math.min(16f * density, slot * 0.42f);

        double max = 0;
        for (double v : values) max = Math.max(max, Math.max(0, v));
        if (max <= 0) max = 1;

        rect.setEmpty();
        for (int i = 0; i < n; i++) {
            float cx = slot * i + slot / 2f;
            float left = cx - barW / 2f;
            float right = cx + barW / 2f;

            trackPaint.setColor(track);
            rect.set(left, plotTop, right, plotBottom);
            canvas.drawRoundRect(rect, barW / 2f, barW / 2f, trackPaint);

            double v = Math.max(0, values[i]);
            float frac = (float) (v / max);
            float barH = Math.max(v > 0 ? 3f * density : 0f, plotH * frac * shown);
            if (barH > 0.5f) {
                barPaint.setColor(i == highlight ? accent : accentSoft);
                rect.set(left, plotBottom - barH, right, plotBottom);
                canvas.drawRoundRect(rect, barW / 2f, barW / 2f, barPaint);
            }

            if (i < labels.length && labels[i] != null) {
                labelPaint.setColor(i == highlight ? accent : labelColor);
                labelPaint.setTextSize(9.5f * density);
                labelPaint.setTypeface(i == highlight
                        ? android.graphics.Typeface.create("sans-serif-medium", 0)
                        : android.graphics.Typeface.create("sans-serif", 0));
                canvas.drawText(labels[i], cx, h - 3f * density, labelPaint);
            }
        }
    }
}
