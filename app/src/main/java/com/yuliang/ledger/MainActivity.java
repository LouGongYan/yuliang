package com.yuliang.ledger;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yuliang.ledger.data.LedgerStore;
import com.yuliang.ledger.data.Models;
import com.yuliang.ledger.ui.AddSheet;
import com.yuliang.ledger.ui.AppTheme;
import com.yuliang.ledger.ui.BudgetSheet;
import com.yuliang.ledger.ui.HomeScreen;
import com.yuliang.ledger.ui.MonthPickerSheet;
import com.yuliang.ledger.ui.RecordsScreen;
import com.yuliang.ledger.ui.SettingsScreen;
import com.yuliang.ledger.ui.SheetHost;
import com.yuliang.ledger.ui.Ui;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;

/** The single Activity: three tabs, month navigation and bottom-sheet tools. */
public class MainActivity extends Activity {

    public static final int TAB_HOME = 0;
    public static final int TAB_RECORDS = 1;
    public static final int TAB_SETTINGS = 2;

    private AppTheme theme;
    private LedgerStore store;
    private SheetHost sheetHost;

    private YearMonth month;
    private int tab = TAB_HOME;
    private int recordFilter = 0;

    private FrameLayout contentHost;
    private FrameLayout overlayLayer;
    private LinearLayout column;
    private View quickBar;
    private LinearLayout bottomNav;
    private View[] navItems;
    private ImageView[] navIcons;
    private TextView[] navLabels;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private YearMonth nowMonthAtResume;
    private LocalDate lastRenderDay;
    private boolean setupAutoShown;

    private final Runnable dayTick = new Runnable() {
        @Override
        public void run() {
            if (isFinishing()) return;
            LocalDate now = LocalDate.now();
            if (lastRenderDay == null || !now.equals(lastRenderDay)) {
                syncMonthToTodayIfNeeded();
                render();
            }
            scheduleDayTick();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        theme = new AppTheme(this);
        theme.applyToWindow();
        store = new LedgerStore(this);

        YearMonth now = YearMonth.now();
        nowMonthAtResume = now;
        month = now;
        if (savedInstanceState != null) {
            try {
                String saved = savedInstanceState.getString("month");
                if (saved != null) month = YearMonth.parse(saved);
            } catch (Exception ignored) {
            }
            tab = savedInstanceState.getInt("tab", TAB_HOME);
            recordFilter = savedInstanceState.getInt("filter", 0);
        }
        if (month.isAfter(now)) month = now;
        store.ensureMonth(month.toString());

        setContentView(buildRoot());
        theme.applySystemBarIcons();
        render();

        if (!store.isOnboarded() && !store.getMonth(month.toString()).configured && !setupAutoShown) {
            setupAutoShown = true;
            store.setOnboarded(true);
            contentHost.postDelayed(() -> {
                if (!isFinishing() && !sheetHost.isShowing()) {
                    showBudgetSheet("total");
                }
            }, 420);
        }
    }

    private View buildRoot() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(theme.bg);
        root.setFitsSystemWindows(false);

        column = Ui.vertical(this);
        column.setBackgroundColor(theme.bg);
        root.addView(column, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        contentHost = new FrameLayout(this);
        contentHost.setClipChildren(false);
        contentHost.setClipToPadding(false);
        column.addView(contentHost, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        quickBar = buildQuickBar();
        column.addView(quickBar);

        bottomNav = buildBottomNav();
        column.addView(bottomNav);

        overlayLayer = new FrameLayout(this);
        root.addView(overlayLayer, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        sheetHost = new SheetHost(this, theme, overlayLayer);

        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int top;
            int bottom;
            int left;
            int right;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                top = bars.top;
                bottom = bars.bottom;
                left = bars.left;
                right = bars.right;
            } else {
                top = insets.getSystemWindowInsetTop();
                bottom = insets.getSystemWindowInsetBottom();
                left = insets.getSystemWindowInsetLeft();
                right = insets.getSystemWindowInsetRight();
            }
            column.setPadding(left, top, right, bottom);
            return insets;
        });
        root.requestApplyInsets();
        return root;
    }

    private View buildQuickBar() {
        LinearLayout wrap = Ui.vertical(this);
        wrap.setPadding(Ui.dp(this, 18), Ui.dp(this, 8), Ui.dp(this, 18), 0);

        LinearLayout bar = Ui.horizontal(this);
        bar.setBackground(Ui.rippleStroke(this, theme.surface, 19, theme.line, 1, Ui.alpha(theme.accent, 35)));
        bar.setElevation(Ui.dp(this, 2));
        bar.setPadding(Ui.dp(this, 9), Ui.dp(this, 9), Ui.dp(this, 15), Ui.dp(this, 9));
        bar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 58)));
        bar.setClickable(true);
        bar.setFocusable(true);
        Ui.press(bar);
        bar.setOnClickListener(v -> {
            Ui.haptic(v);
            openAddSheet(null);
        });

        FrameLayout plus = new FrameLayout(this);
        plus.setBackground(Ui.round(this, theme.accentSoft, 50));
        plus.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 40)));
        ImageView plusIcon = Ui.icon(this, R.drawable.ic_plus, 21, theme.accent);
        FrameLayout.LayoutParams pi = new FrameLayout.LayoutParams(Ui.dp(this, 21), Ui.dp(this, 21));
        pi.gravity = Gravity.CENTER;
        plusIcon.setLayoutParams(pi);
        plus.addView(plusIcon);
        bar.addView(plus);

        LinearLayout texts = Ui.vertical(this);
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textLp.setMargins(Ui.dp(this, 12), 0, Ui.dp(this, 8), 0);
        texts.setLayoutParams(textLp);

        TextView title = Ui.text(this, "记一笔", 14.5f, theme.text, theme.medium);
        TextView hint = Ui.text(this, "今天花了多少？点这里开始", 11.5f, theme.text3, theme.regular);
        hint.setPadding(0, Ui.dp(this, 2), 0, 0);
        texts.addView(title);
        texts.addView(hint);
        bar.addView(texts);

        LinearLayout right = Ui.horizontal(this);
        TextView dual = Ui.text(this, "支出 / 收入", 11.5f, theme.text2, theme.medium);
        right.addView(dual);
        ImageView chevron = Ui.icon(this, R.drawable.ic_chevron_right, 17, theme.text3);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(Ui.dp(this, 17), Ui.dp(this, 17));
        clp.setMargins(Ui.dp(this, 2), 0, 0, 0);
        chevron.setLayoutParams(clp);
        right.addView(chevron);
        bar.addView(right);

        wrap.addView(bar);
        return wrap;
    }

    private LinearLayout buildBottomNav() {
        LinearLayout holder = Ui.horizontal(this);
        LinearLayout.LayoutParams holderLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 66));
        holderLp.setMargins(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 10));
        holder.setLayoutParams(holderLp);
        holder.setBackground(Ui.roundStroke(this, theme.surface, 22, theme.line, 1));
        holder.setElevation(Ui.dp(this, 10));
        holder.setPadding(Ui.dp(this, 6), Ui.dp(this, 6), Ui.dp(this, 6), Ui.dp(this, 6));

        navItems = new View[3];
        navIcons = new ImageView[3];
        navLabels = new TextView[3];
        int[] icons = {R.drawable.ic_home, R.drawable.ic_receipt, R.drawable.ic_tune};
        String[] labels = {"概览", "明细", "设置"};

        for (int i = 0; i < 3; i++) {
            final int index = i;
            LinearLayout item = Ui.vertical(this);
            item.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            lp.setMargins(Ui.dp(this, 2), 0, Ui.dp(this, 2), 0);
            item.setLayoutParams(lp);
            item.setClickable(true);
            item.setFocusable(true);
            item.setOnClickListener(v -> {
                Ui.haptic(v);
                selectTab(index);
            });

            ImageView icon = Ui.icon(this, icons[i], 21, theme.text3);
            LinearLayout.LayoutParams iconLp = (LinearLayout.LayoutParams) icon.getLayoutParams();
            iconLp.setMargins(0, Ui.dp(this, 2), 0, Ui.dp(this, 2));
            item.addView(icon);

            TextView label = Ui.text(this, labels[i], 10.5f, theme.text3, theme.medium);
            label.setGravity(Gravity.CENTER);
            item.addView(label);

            navItems[i] = item;
            navIcons[i] = icon;
            navLabels[i] = label;
            holder.addView(item);
        }
        return holder;
    }

    private void selectTab(int index) {
        if (tab == index) return;
        tab = index;
        render();
    }

    public void setRecordsFilter(int filter) {
        if (recordFilter == filter) return;
        recordFilter = filter;
        render();
    }

    public void openTab(int index) {
        tab = Math.max(TAB_HOME, Math.min(TAB_SETTINGS, index));
        render();
    }

    public int getRecordsFilter() {
        return recordFilter;
    }

    public void render() {
        if (month.isAfter(YearMonth.now())) month = YearMonth.now();
        store.ensureMonth(month.toString());

        contentHost.removeAllViews();
        View screen;
        if (tab == TAB_RECORDS) {
            screen = new RecordsScreen(this).build();
        } else if (tab == TAB_SETTINGS) {
            screen = new SettingsScreen(this).build();
        } else {
            screen = new HomeScreen(this).build();
        }
        contentHost.addView(screen, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        screen.setAlpha(0f);
        screen.setTranslationY(Ui.dp(this, 8));
        screen.animate().alpha(1f).translationY(0).setDuration(240).start();

        boolean showQuick = tab == TAB_HOME && month.equals(YearMonth.now());
        quickBar.setVisibility(showQuick ? View.VISIBLE : View.GONE);
        updateBottomNav();
        lastRenderDay = LocalDate.now();
    }

    private void updateBottomNav() {
        for (int i = 0; i < navItems.length; i++) {
            boolean on = i == tab;
            navItems[i].setBackground(on
                    ? Ui.ripple(this, theme.accentSoft, 16, Ui.alpha(theme.accent, 30))
                    : Ui.ripple(this, Color.TRANSPARENT, 16, Ui.alpha(theme.accent, 20)));
            navIcons[i].setColorFilter(on ? theme.accent : theme.text3, android.graphics.PorterDuff.Mode.SRC_IN);
            navLabels[i].setTextColor(on ? theme.accentDeep : theme.text3);
            navItems[i].animate().scaleX(on ? 1f : 0.96f).scaleY(on ? 1f : 0.96f)
                    .setDuration(180).start();
        }
    }

    // ------------------------------------------------------------------ actions

    public void openAddSheet(Models.Tx edit) {
        AddSheet.show(this, month.toString(), edit);
    }

    public void showBudgetSheet(String focus) {
        BudgetSheet.show(this, month.toString(), focus);
    }

    public void showMonthPicker() {
        MonthPickerSheet.show(this);
    }

    public void openSheet(View content, float maxHeightRatio, Runnable onDismiss) {
        sheetHost.show(content, maxHeightRatio, onDismiss);
    }

    public void dismissSheet() {
        sheetHost.dismiss();
    }

    public boolean isSheetShowing() {
        return sheetHost != null && sheetHost.isShowing();
    }

    public void setMonth(YearMonth target) {
        YearMonth now = YearMonth.now();
        if (target.isAfter(now)) target = now;
        month = target;
        store.ensureMonth(month.toString());
        render();
    }

    public void goToCurrentMonth() {
        setMonth(YearMonth.now());
    }

    public void showMonthPickerForRecords() {
        showMonthPicker();
    }

    public void onDataChanged() {
        render();
    }

    public void snackbar(String message) {
        Ui.snack(this, message);
    }

    public void afterSheetClosed(Runnable action) {
        if (isSheetShowing()) {
            sheetHost.dismiss();
            contentHost.postDelayed(action, 240);
        } else {
            action.run();
        }
    }

    public LedgerStore store() {
        return store;
    }

    public AppTheme colors() {
        return theme;
    }

    public YearMonth currentMonth() {
        return month;
    }

    public String currentMonthKey() {
        return month.toString();
    }

    public boolean isCurrentMonth() {
        return month.equals(YearMonth.now());
    }

    public int currentTab() {
        return tab;
    }

    public Models.Summary summary() {
        return store.summary(month.toString());
    }

    // ------------------------------------------------------------------ lifecycle

    private void syncMonthToTodayIfNeeded() {
        YearMonth now = YearMonth.now();
        if (month.equals(nowMonthAtResume) && !now.equals(nowMonthAtResume)) {
            month = now;
            store.ensureMonth(month.toString());
        }
        nowMonthAtResume = now;
    }

    private void scheduleDayTick() {
        handler.removeCallbacks(dayTick);
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime next = LocalDate.now().plusDays(1).atStartOfDay().plusSeconds(1);
        long delay = Math.max(30_000L, next.atZone(zone).toInstant().toEpochMilli()
                - System.currentTimeMillis());
        handler.postDelayed(dayTick, delay);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (store == null) return;
        if (theme != null) theme.applySystemBarIcons();
        LocalDate now = LocalDate.now();
        boolean needsRender = lastRenderDay == null || !now.equals(lastRenderDay);
        syncMonthToTodayIfNeeded();
        if (needsRender) render();
        scheduleDayTick();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(dayTick);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("month", month.toString());
        outState.putInt("tab", tab);
        outState.putInt("filter", recordFilter);
    }

    @Override
    public void onBackPressed() {
        if (isSheetShowing()) {
            dismissSheet();
            return;
        }
        if (tab != TAB_HOME) {
            tab = TAB_HOME;
            render();
            return;
        }
        if (!month.equals(YearMonth.now())) {
            goToCurrentMonth();
            return;
        }
        super.onBackPressed();
    }
}
