package com.yuliang.ledger.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yuliang.ledger.data.Models;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Small programmatic-UI helpers shared by every screen. */
public final class Ui {

    private static final Locale CN = Locale.CHINA;
    private static final ThreadLocal<DecimalFormat> MONEY = ThreadLocal.withInitial(() -> {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(CN);
        DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
        format.setRoundingMode(java.math.RoundingMode.HALF_UP);
        return format;
    });
    private static final ThreadLocal<DecimalFormat> PLAIN = ThreadLocal.withInitial(() -> {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        DecimalFormat format = new DecimalFormat("0.00", symbols);
        format.setRoundingMode(java.math.RoundingMode.HALF_UP);
        return format;
    });

    private Ui() {
    }

    // ------------------------------------------------------------------ metrics

    public static int dp(Context c, float value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }

    public static float dpFloat(Context c, float value) {
        return value * c.getResources().getDisplayMetrics().density;
    }

    public static int screenWidth(Context c) {
        return c.getResources().getDisplayMetrics().widthPixels;
    }

    public static int screenHeight(Context c) {
        return c.getResources().getDisplayMetrics().heightPixels;
    }

    public static int alpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    // ------------------------------------------------------------------ text

    public static TextView text(Context c, CharSequence value, float sp, int color, Typeface face) {
        TextView tv = new TextView(c);
        tv.setText(value);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        tv.setTextColor(color);
        tv.setTypeface(face);
        tv.setIncludeFontPadding(false);
        tv.setLineSpacing(0f, 1.08f);
        return tv;
    }

    public static TextView body(Context c, AppTheme theme, CharSequence value) {
        return text(c, value, 14, theme.text2, theme.regular);
    }

    public static TextView label(Context c, AppTheme theme, CharSequence value) {
        return text(c, value, 12, theme.text3, theme.regular);
    }

    public static TextView section(Context c, AppTheme theme, CharSequence value) {
        TextView tv = text(c, value, 13, theme.text2, theme.medium);
        tv.setLetterSpacing(0.02f);
        return tv;
    }

    public static void autoSize(TextView tv, float minSp, float maxSp) {
        if (Build.VERSION.SDK_INT >= 26) {
            tv.setAutoSizeTextTypeUniformWithConfiguration(
                    Math.round(minSp), Math.round(maxSp), 1, TypedValue.COMPLEX_UNIT_SP);
        }
    }

    // ------------------------------------------------------------------ layout

    public static LinearLayout vertical(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout horizontal(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static LinearLayout.LayoutParams lp(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    public static LinearLayout.LayoutParams lp(int width, int height, float weight) {
        return new LinearLayout.LayoutParams(width, height, weight);
    }

    public static FrameLayout.LayoutParams flp(int width, int height) {
        return new FrameLayout.LayoutParams(width, height);
    }

    public static void margins(View v, int left, int top, int right, int bottom) {
        ViewGroup.LayoutParams raw = v.getLayoutParams();
        if (raw instanceof ViewGroup.MarginLayoutParams) {
            ((ViewGroup.MarginLayoutParams) raw).setMargins(left, top, right, bottom);
        }
    }

    public static View spacer(Context c, int heightDp) {
        View v = new View(c);
        v.setLayoutParams(lp(ViewGroup.LayoutParams.MATCH_PARENT, dp(c, heightDp)));
        return v;
    }

    public static View divider(Context c, AppTheme theme) {
        View v = new View(c);
        v.setBackgroundColor(theme.line);
        v.setLayoutParams(lp(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(c, 0.8f))));
        return v;
    }

    public static View dividerInset(Context c, AppTheme theme, int insetDp) {
        View v = new View(c);
        v.setBackgroundColor(theme.line);
        LinearLayout.LayoutParams p = lp(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(c, 0.8f)));
        p.setMargins(dp(c, insetDp), 0, 0, 0);
        v.setLayoutParams(p);
        return v;
    }

    // ------------------------------------------------------------------ surfaces

    public static GradientDrawable round(Context c, int color, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dpFloat(c, radiusDp));
        return g;
    }

    public static GradientDrawable roundStroke(Context c, int color, float radiusDp, int strokeColor, float strokeDp) {
        GradientDrawable g = round(c, color, radiusDp);
        g.setStroke(Math.max(1, dp(c, strokeDp)), strokeColor);
        return g;
    }

    public static RippleDrawable ripple(Context c, int color, float radiusDp, int rippleColor) {
        GradientDrawable content = round(c, color, radiusDp);
        GradientDrawable mask = round(c, Color.WHITE, radiusDp);
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), content, mask);
    }

    public static RippleDrawable rippleStroke(Context c, int color, float radiusDp,
                                             int strokeColor, float strokeDp, int rippleColor) {
        GradientDrawable content = roundStroke(c, color, radiusDp, strokeColor, strokeDp);
        GradientDrawable mask = round(c, Color.WHITE, radiusDp);
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), content, mask);
    }

    public static LinearLayout card(Context c, AppTheme theme) {
        LinearLayout card = vertical(c);
        card.setBackground(roundStroke(c, theme.surface, 22, Ui.alpha(theme.line, theme.dark ? 150 : 255), 1));
        card.setElevation(dp(c, 1.5f));
        card.setPadding(dp(c, 18), dp(c, 18), dp(c, 18), dp(c, 18));
        return card;
    }

    public static void press(View v) {
        v.setOnTouchListener((view, event) -> {
            switch (event.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    view.animate().scaleX(0.973f).scaleY(0.973f)
                            .setDuration(120).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    view.animate().scaleX(1f).scaleY(1f)
                            .setDuration(170).setInterpolator(new android.view.animation.OvershootInterpolator(1.4f)).start();
                    break;
            }
            return false;
        });
    }

    public static void haptic(View v) {
        if (v == null) return;
        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
    }

    public static void hapticConfirm(View v) {
        if (v == null) return;
        if (Build.VERSION.SDK_INT >= 30) {
            v.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
        } else {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }
    }

    // ------------------------------------------------------------------ icons

    public static ImageView icon(Context c, int res, int sizeDp, int tint) {
        ImageView iv = new ImageView(c);
        iv.setImageResource(res);
        iv.setColorFilter(tint, PorterDuff.Mode.SRC_IN);
        iv.setLayoutParams(new LinearLayout.LayoutParams(dp(c, sizeDp), dp(c, sizeDp)));
        return iv;
    }

    public static FrameLayout iconButton(Context c, AppTheme theme, int res, int tint) {
        FrameLayout holder = new FrameLayout(c);
        holder.setBackground(ripple(c, theme.surface2, 50, Ui.alpha(theme.accent, 40)));
        holder.setLayoutParams(new LinearLayout.LayoutParams(dp(c, 40), dp(c, 40)));
        ImageView iv = icon(c, res, 20, tint);
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(dp(c, 20), dp(c, 20));
        p.gravity = Gravity.CENTER;
        iv.setLayoutParams(p);
        holder.addView(iv);
        holder.setClickable(true);
        holder.setFocusable(true);
        Ui.press(holder);
        return holder;
    }

    public static TextView chip(Context c, AppTheme theme, String label, boolean selected) {
        TextView tv = text(c, label, 12.5f, selected ? theme.accentDeep : theme.text2, theme.medium);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(c, 13), dp(c, 8), dp(c, 13), dp(c, 8));
        tv.setBackground(rippleStroke(c,
                selected ? theme.accentSoft : theme.surface2,
                13,
                selected ? Ui.alpha(theme.accent, 150) : Color.TRANSPARENT,
                1,
                Ui.alpha(theme.accent, 35)));
        tv.setClickable(true);
        tv.setFocusable(true);
        return tv;
    }

    public static void styleChip(TextView tv, AppTheme theme, boolean selected) {
        tv.setTextColor(selected ? theme.accentDeep : theme.text2);
        tv.setBackground(rippleStroke(tv.getContext(),
                selected ? theme.accentSoft : theme.surface2,
                13,
                selected ? Ui.alpha(theme.accent, 150) : Color.TRANSPARENT,
                1,
                Ui.alpha(theme.accent, 35)));
    }

    // ------------------------------------------------------------------ formatting

    public static String money(double value) {
        return "¥" + MONEY.get().format(value);
    }

    public static String moneyNoSymbol(double value) {
        return MONEY.get().format(value);
    }

    public static String plain(double value) {
        return PLAIN.get().format(value);
    }

    public static String signedMoney(Models.Tx tx) {
        return (tx.isIncome() ? "+" : "-") + money(tx.amount);
    }

    public static String month(YearMonth ym) {
        return ym.getYear() + "年" + ym.getMonthValue() + "月";
    }

    public static String monthShort(YearMonth ym) {
        return ym.getMonthValue() + "月";
    }

    public static String day(LocalDate date) {
        return (date.getMonthValue()) + "月" + date.getDayOfMonth() + "日";
    }

    public static String weekday(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> "周一";
            case TUESDAY -> "周二";
            case WEDNESDAY -> "周三";
            case THURSDAY -> "周四";
            case FRIDAY -> "周五";
            case SATURDAY -> "周六";
            case SUNDAY -> "周日";
        };
    }

    public static String dateLine(LocalDate date) {
        return day(date) + " · " + weekday(date);
    }

    public static String relativeDate(LocalDate date) {
        LocalDate today = LocalDate.now();
        if (date.equals(today)) return "今天";
        if (date.equals(today.minusDays(1))) return "昨天";
        if (date.equals(today.plusDays(1))) return "明天";
        return day(date);
    }

    public static String time(long ts) {
        if (ts <= 0) return "";
        return java.time.Instant.ofEpochMilli(ts)
                .atZone(java.time.ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    public static String percent(double value) {
        return Math.round(value * 100) + "%";
    }

    public static String formatInput(String raw) {
        if (raw == null || raw.isEmpty()) return "0";
        String s = raw;
        int dot = s.indexOf('.');
        String intPart = dot >= 0 ? s.substring(0, dot) : s;
        String decPart = dot >= 0 ? s.substring(dot + 1) : null;
        if (intPart.isEmpty()) intPart = "0";
        try {
            intPart = new java.math.BigInteger(intPart).toString();
        } catch (Exception ignored) {
        }
        StringBuilder grouped = new StringBuilder();
        int count = 0;
        for (int i = intPart.length() - 1; i >= 0; i--) {
            grouped.append(intPart.charAt(i));
            count++;
            if (count % 3 == 0 && i > 0) grouped.append(',');
        }
        grouped.reverse();
        if (decPart != null) grouped.append('.').append(decPart);
        return grouped.toString();
    }

    public static double parseMoney(String value) {
        if (value == null) return 0;
        String s = value.trim().replace("¥", "").replace(",", "").replace(" ", "");
        if (TextUtils.isEmpty(s)) return 0;
        try {
            double d = Double.parseDouble(s);
            if (Double.isNaN(d) || Double.isInfinite(d)) return 0;
            return Math.max(0, Math.min(999_999_999d, d));
        } catch (Exception e) {
            return 0;
        }
    }

    public static int parseColor(String value, int fallback) {
        try {
            return Color.parseColor(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    // ------------------------------------------------------------------ insets

    public static int statusBarInset(Activity activity) {
        WindowInsets insets = activity.getWindow().getDecorView().getRootWindowInsets();
        if (insets == null) return dp(activity, 24);
        if (Build.VERSION.SDK_INT >= 30) {
            android.graphics.Insets i = insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            return i.top;
        }
        return insets.getSystemWindowInsetTop();
    }

    public static int navigationBarInset(Activity activity) {
        WindowInsets insets = activity.getWindow().getDecorView().getRootWindowInsets();
        if (insets == null) return dp(activity, 16);
        if (Build.VERSION.SDK_INT >= 30) {
            android.graphics.Insets i = insets.getInsets(WindowInsets.Type.systemBars());
            return i.bottom;
        }
        return insets.getSystemWindowInsetBottom();
    }

    public static int[] systemBarInsets(Activity activity) {
        WindowInsets insets = activity.getWindow().getDecorView().getRootWindowInsets();
        if (insets == null) {
            int d = dp(activity, 24);
            return new int[]{d, d, 0, 0};
        }
        if (Build.VERSION.SDK_INT >= 30) {
            android.graphics.Insets i = insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            return new int[]{i.top, i.bottom, i.left, i.right};
        }
        return new int[]{
                insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetBottom(),
                insets.getSystemWindowInsetLeft(),
                insets.getSystemWindowInsetRight()
        };
    }

    // ------------------------------------------------------------------ keyboard

    public static void showKeyboard(View view) {
        if (view == null) return;
        view.postDelayed(() -> {
            view.requestFocus();
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager)
                            view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(view, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        }, 180);
    }

    public static void hideKeyboard(Activity activity) {
        if (activity == null) return;
        View focus = activity.getCurrentFocus();
        android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager)
                        activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            if (focus != null) {
                imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
            } else {
                imm.hideSoftInputFromWindow(activity.getWindow().getDecorView().getWindowToken(), 0);
            }
        }
        if (focus != null) focus.clearFocus();
    }

    // ------------------------------------------------------------------ snackbar

    public static void snack(Activity activity, String message) {
        if (activity == null || activity.isFinishing()) return;
        View root = activity.findViewById(android.R.id.content);
        if (!(root instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) root;
        View old = group.findViewWithTag("yuliang-snack");
        if (old != null) group.removeView(old);

        TextView tv = new TextView(activity);
        tv.setTag("yuliang-snack");
        tv.setText(message);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setTextColor(Color.WHITE);
        tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(activity, 18), dp(activity, 12), dp(activity, 18), dp(activity, 12));
        tv.setBackground(round(activity, 0xF21A2824, 15));
        tv.setElevation(dp(activity, 10));
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        p.bottomMargin = dp(activity, 100);
        group.addView(tv, p);
        tv.setAlpha(0f);
        tv.setTranslationY(dp(activity, 18));
        tv.animate().alpha(1f).translationY(0).setDuration(180).start();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (tv.getParent() == null) return;
            tv.animate().alpha(0f).translationY(dp(activity, 12)).setDuration(180)
                    .withEndAction(() -> {
                        if (tv.getParent() != null) group.removeView(tv);
                    }).start();
        }, 2200);
    }

    public static void hideSnack(Activity activity) {
        View root = activity.findViewById(android.R.id.content);
        if (!(root instanceof ViewGroup)) return;
        View old = ((ViewGroup) root).findViewWithTag("yuliang-snack");
        if (old != null) ((ViewGroup) root).removeView(old);
    }

    // ------------------------------------------------------------------ misc

    public static int categoryColor(AppTheme theme, String name) {
        if (name == null) return theme.categoryColors[0];
        int hash = 0;
        for (int i = 0; i < name.length(); i++) hash = hash * 31 + name.charAt(i);
        return theme.categoryColors[(hash & 0x7fffffff) % theme.categoryColors.length];
    }

    public static String categoryChar(String name) {
        if (name == null || name.isEmpty()) return "其";
        return name.substring(0, 1);
    }
}
