package com.yuliang.ledger.ui;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.yuliang.ledger.ui.view.MaxHeightScrollView;

/** Bottom sheet presentation built on top of the Activity, without AndroidX. */
public class SheetHost {

    private final Activity activity;
    private final AppTheme theme;
    private final FrameLayout layer;

    private FrameLayout overlay;
    private View scrim;
    private LinearLayout panel;
    private boolean showing;
    private Runnable dismissCallback;

    public SheetHost(Activity activity, AppTheme theme, FrameLayout layer) {
        this.activity = activity;
        this.theme = theme;
        this.layer = layer;
    }

    public boolean isShowing() {
        return showing;
    }

    public void show(View content, float maxHeightRatio, Runnable onDismiss) {
        if (showing) {
            removeNow(false);
        }
        showing = true;
        dismissCallback = onDismiss;

        overlay = new FrameLayout(activity);
        overlay.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        overlay.setClickable(true);

        scrim = new View(activity);
        scrim.setBackgroundColor(theme.scrim);
        scrim.setAlpha(0f);
        scrim.setOnClickListener(v -> dismiss());
        overlay.addView(scrim, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(topRounded(theme.surface, 28));
        panel.setElevation(Ui.dp(activity, 24));
        panel.setClickable(true);
        panel.setFocusable(true);
        int bottomInset = navigationInset();
        FrameLayout.LayoutParams panelLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        panelLp.gravity = Gravity.BOTTOM;
        panelLp.bottomMargin = bottomInset;
        overlay.addView(panel, panelLp);

        // Drag handle.
        FrameLayout handle = new FrameLayout(activity);
        LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(activity, 24));
        handle.setLayoutParams(handleLp);
        View pill = new View(activity);
        pill.setBackground(Ui.round(activity, theme.line, 3));
        FrameLayout.LayoutParams pillLp = new FrameLayout.LayoutParams(
                Ui.dp(activity, 38), Ui.dp(activity, 4));
        pillLp.gravity = Gravity.CENTER;
        handle.addView(pill, pillLp);
        handle.setOnTouchListener(new View.OnTouchListener() {
            float downRaw;
            boolean dragging;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downRaw = event.getRawY();
                        dragging = false;
                        panel.animate().cancel();
                        return true;
                    case MotionEvent.ACTION_MOVE: {
                        float dy = event.getRawY() - downRaw;
                        if (dy > 6) dragging = true;
                        float shift = Math.max(0, dy);
                        panel.setTranslationY(shift);
                        scrim.setAlpha(Math.max(0f, 1f - shift / (Ui.dp(activity, 320))));
                        return true;
                    }
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL: {
                        float dy = panel.getTranslationY();
                        if (dy > Ui.dp(activity, 90)) {
                            dismiss();
                        } else {
                            panel.animate().translationY(0).setDuration(200)
                                    .setInterpolator(new DecelerateInterpolator()).start();
                            scrim.animate().alpha(1f).setDuration(200).start();
                        }
                        return true;
                    }
                }
                return false;
            }
        });
        panel.addView(handle);

        int maxHeight = (int) (Ui.screenHeight(activity) * maxHeightRatio)
                - Ui.dp(activity, 24) - bottomInset;
        MaxHeightScrollView scroll = new MaxHeightScrollView(activity);
        scroll.setMaxHeight(Math.max(Ui.dp(activity, 240), maxHeight));
        scroll.setClipToPadding(false);
        scroll.setPadding(0, 0, 0, Ui.dp(activity, 8));
        scroll.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        panel.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        panel.setTranslationY(Ui.screenHeight(activity));
        layer.addView(overlay);
        layer.setVisibility(View.VISIBLE);

        panel.animate().translationY(0).setDuration(300)
                .setInterpolator(new DecelerateInterpolator(2.2f)).start();
        scrim.animate().alpha(1f).setDuration(240).start();
    }

    public void dismiss() {
        if (!showing || overlay == null) return;
        showing = false;
        Ui.hideKeyboard(activity);
        final FrameLayout oldOverlay = overlay;
        final Runnable callback = dismissCallback;
        dismissCallback = null;
        panel.animate().translationY(panel.getHeight()).setDuration(230)
                .setInterpolator(new DecelerateInterpolator(2f))
                .withEndAction(() -> {
                    if (oldOverlay.getParent() != null) layer.removeView(oldOverlay);
                    overlay = null;
                    scrim = null;
                    panel = null;
                    if (callback != null) callback.run();
                }).start();
        scrim.animate().alpha(0f).setDuration(190).start();
    }

    private void removeNow(boolean callCallback) {
        Runnable callback = dismissCallback;
        if (overlay != null && overlay.getParent() != null) layer.removeView(overlay);
        overlay = null;
        scrim = null;
        panel = null;
        showing = false;
        dismissCallback = null;
        if (callCallback && callback != null) callback.run();
    }

    private int navigationInset() {
        int inset = Ui.navigationBarInset(activity);
        if (inset <= 0) {
            int id = activity.getResources().getIdentifier(
                    "navigation_bar_height", "dimen", "android");
            if (id > 0) {
                inset = activity.getResources().getDimensionPixelSize(id);
            }
        }
        return Math.max(inset, Ui.dp(activity, 6));
    }

    private GradientDrawable topRounded(int color, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        float r = Ui.dpFloat(activity, radiusDp);
        g.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        return g;
    }
}
