package com.yuliang.ledger.ui;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import com.yuliang.ledger.R;

/** Colours, typefaces and window treatment for one Activity instance. */
public final class AppTheme {

    public final boolean dark;
    public final int bg;
    public final int surface;
    public final int surface2;
    public final int surface3;
    public final int text;
    public final int text2;
    public final int text3;
    public final int line;
    public final int accent;
    public final int accentDeep;
    public final int accentSoft;
    public final int accentOn;
    public final int heroStart;
    public final int heroEnd;
    public final int heroText;
    public final int heroText2;
    public final int green;
    public final int greenSoft;
    public final int red;
    public final int redSoft;
    public final int amber;
    public final int amberSoft;
    public final int scrim;
    public final int iconBg;
    public final int splashBg;
    public final int[] categoryColors;

    public final Typeface regular;
    public final Typeface medium;
    public final Typeface light;

    private final Activity activity;

    public AppTheme(Activity activity) {
        this.activity = activity;
        Configuration cfg = activity.getResources().getConfiguration();
        dark = (cfg.uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        bg = activity.getColor(R.color.bg);
        surface = activity.getColor(R.color.surface);
        surface2 = activity.getColor(R.color.surface2);
        surface3 = activity.getColor(R.color.surface3);
        text = activity.getColor(R.color.text);
        text2 = activity.getColor(R.color.text2);
        text3 = activity.getColor(R.color.text3);
        line = activity.getColor(R.color.line);
        accent = activity.getColor(R.color.accent);
        accentDeep = activity.getColor(R.color.accent_deep);
        accentSoft = activity.getColor(R.color.accent_soft);
        accentOn = activity.getColor(R.color.accent_on);
        heroStart = activity.getColor(R.color.hero_start);
        heroEnd = activity.getColor(R.color.hero_end);
        heroText = activity.getColor(R.color.hero_text);
        heroText2 = activity.getColor(R.color.hero_text2);
        green = activity.getColor(R.color.green);
        greenSoft = activity.getColor(R.color.green_soft);
        red = activity.getColor(R.color.red);
        redSoft = activity.getColor(R.color.red_soft);
        amber = activity.getColor(R.color.amber);
        amberSoft = activity.getColor(R.color.amber_soft);
        scrim = activity.getColor(R.color.scrim);
        iconBg = activity.getColor(R.color.icon_bg);
        splashBg = activity.getColor(R.color.splash_bg);
        categoryColors = new int[]{
                activity.getColor(R.color.cat1),
                activity.getColor(R.color.cat2),
                activity.getColor(R.color.cat3),
                activity.getColor(R.color.cat4),
                activity.getColor(R.color.cat5),
                activity.getColor(R.color.cat6)
        };
        regular = Typeface.create("sans-serif", Typeface.NORMAL);
        medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
        light = Typeface.create("sans-serif-light", Typeface.NORMAL);
    }

    public GradientDrawable heroDrawable() {
        GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{heroStart, heroEnd});
        g.setCornerRadius(dp(26));
        return g;
    }

    public int categoryColor(String name) {
        if (name == null) return categoryColors[0];
        int hash = 0;
        for (int i = 0; i < name.length(); i++) hash = hash * 31 + name.charAt(i);
        return categoryColors[(hash & 0x7fffffff) % categoryColors.length];
    }

    public int colorForType(boolean income) {
        return income ? green : text;
    }

    public void applyToWindow() {
        Window window = activity.getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        if (Build.VERSION.SDK_INT >= 29) {
            window.setNavigationBarContrastEnforced(false);
            window.setStatusBarContrastEnforced(false);
        }
        if (Build.VERSION.SDK_INT >= 28) {
            window.setNavigationBarDividerColor(Color.TRANSPARENT);
        }
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false);
        } else {
            int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            if (!dark) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                if (Build.VERSION.SDK_INT >= 26) {
                    flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
            }
            window.getDecorView().setSystemUiVisibility(flags);
        }
    }

    /**
     * Must be called after setContentView: on several Android 11 builds
     * Window.getInsetsController() NPEs while the decor view is still being
     * created, so the icon colour is applied once the content exists.
     */
    public void applySystemBarIcons() {
        Window window = activity.getWindow();
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                WindowInsetsController controller = window.getInsetsController();
                if (controller != null) {
                    int mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                            | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                    controller.setSystemBarsAppearance(dark ? 0 : mask, mask);
                    return;
                }
            } catch (Exception ignored) {
            }
        }
        int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        if (!dark) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 26) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
        }
        window.getDecorView().setSystemUiVisibility(flags);
    }

    private int dp(float value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
