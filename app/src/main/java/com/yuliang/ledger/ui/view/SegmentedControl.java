package com.yuliang.ledger.ui.view;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yuliang.ledger.ui.AppTheme;
import com.yuliang.ledger.ui.Ui;

/** A compact segmented switch with an animated selected thumb. */
public class SegmentedControl extends FrameLayout {

    public interface OnChange {
        void onChange(int index);
    }

    private final AppTheme theme;
    private final String[] options;
    private final TextView[] labels;
    private final View thumb;
    private final int pad;
    private int selected;
    private OnChange onChange;

    public SegmentedControl(Context context, AppTheme theme, String[] options, int initial) {
        super(context);
        this.theme = theme;
        this.options = options;
        this.selected = Math.max(0, Math.min(options.length - 1, initial));
        this.pad = Ui.dp(context, 4);

        setBackground(Ui.round(context, theme.surface2, 14));
        setPadding(pad, pad, pad, pad);
        setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(context, 42)));

        thumb = new View(context);
        thumb.setBackground(Ui.round(context, theme.accent, 11));
        FrameLayout.LayoutParams thumbLp = new FrameLayout.LayoutParams(0,
                FrameLayout.LayoutParams.MATCH_PARENT);
        thumbLp.gravity = Gravity.START | Gravity.TOP;
        thumb.setLayoutParams(thumbLp);
        addView(thumb);

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        labels = new TextView[options.length];
        for (int i = 0; i < options.length; i++) {
            final int index = i;
            TextView tv = new TextView(context);
            tv.setText(options[i]);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            tv.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            labelLp.setMargins(Ui.dp(context, 2), 0, Ui.dp(context, 2), 0);
            tv.setLayoutParams(labelLp);
            tv.setOnClickListener(v -> {
                Ui.haptic(v);
                setSelected(index, true);
                if (this.onChange != null) this.onChange.onChange(index);
            });
            labels[i] = tv;
            row.addView(tv);
        }
        addView(row);
        updateColors();
    }

    public void setOnChangeListener(OnChange listener) {
        onChange = listener;
    }

    public int getSelected() {
        return selected;
    }

    public void setSelected(int index, boolean animate) {
        int next = Math.max(0, Math.min(options.length - 1, index));
        if (next == selected && animate) {
            positionThumb(false);
            return;
        }
        selected = next;
        updateColors();
        positionThumb(animate);
    }

    private void updateColors() {
        for (int i = 0; i < labels.length; i++) {
            boolean on = i == selected;
            labels[i].setTextColor(on ? theme.accentOn : theme.text2);
            labels[i].setAlpha(on ? 1f : 0.86f);
            // The moving thumb is an enhancement only; give the label its own
            // pill so the selected option stays legible even if the thumb has
            // not been laid out yet on a slow first frame.
            labels[i].setBackground(on
                    ? Ui.round(getContext(), theme.accent, 10)
                    : null);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        positionThumb(false);
    }

    private void positionThumb(boolean animate) {
        int inner = getWidth() - pad * 2;
        if (inner <= 0) return;
        int seg = inner / Math.max(1, options.length);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) thumb.getLayoutParams();
        lp.width = seg;
        lp.height = getHeight() - pad * 2;
        lp.leftMargin = pad;
        lp.topMargin = pad;
        thumb.setLayoutParams(lp);
        float target = selected * (float) seg;
        if (animate) {
            thumb.animate().translationX(target).setDuration(220)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
        } else {
            thumb.setTranslationX(target);
        }
    }
}
