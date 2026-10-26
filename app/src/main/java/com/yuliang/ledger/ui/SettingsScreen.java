package com.yuliang.ledger.ui;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import com.yuliang.ledger.MainActivity;
import com.yuliang.ledger.R;
import com.yuliang.ledger.data.LedgerStore;
import com.yuliang.ledger.data.Models;

import java.time.YearMonth;

/** Budget rules, category labels, local data and about. */
public class SettingsScreen {

    private final MainActivity main;
    private final AppTheme t;
    private final LedgerStore store;
    private final YearMonth ym;
    private final String key;
    private final Models.Summary s;

    public SettingsScreen(MainActivity main) {
        this.main = main;
        this.t = main.colors();
        this.store = main.store();
        this.ym = main.currentMonth();
        this.key = ym.toString();
        this.s = store.summary(key);
    }

    public View build() {
        LinearLayout root = Ui.vertical(main);
        root.setBackgroundColor(t.bg);
        root.addView(buildHeader());

        ScrollView scroll = new ScrollView(main);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout content = Ui.vertical(main);
        content.setPadding(Ui.dp(main, 18), Ui.dp(main, 2), Ui.dp(main, 18), Ui.dp(main, 28));
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        content.addView(section("预算 · " + Ui.month(ym)));
        content.addView(buildBudgetCard());
        content.addView(section("分类标签"));
        content.addView(buildCategoryCard());
        content.addView(section("数据"));
        content.addView(buildDataCard());
        content.addView(section("关于"));
        content.addView(buildAboutCard());
        content.addView(footer());
        root.addView(scroll);
        return root;
    }

    private View buildHeader() {
        LinearLayout header = Ui.vertical(main);
        header.setPadding(Ui.dp(main, 20), Ui.dp(main, 10), Ui.dp(main, 20), Ui.dp(main, 4));
        LinearLayout row = Ui.horizontal(main);
        TextView title = Ui.text(main, "设置", 27, t.text, t.medium);
        row.addView(title);
        View sp = new View(main);
        row.addView(sp, new LinearLayout.LayoutParams(0, 1, 1f));
        TextView month = Ui.text(main, Ui.month(ym), 10.5f, t.text3, t.medium);
        month.setPadding(Ui.dp(main, 8), Ui.dp(main, 5), Ui.dp(main, 8), Ui.dp(main, 5));
        month.setBackground(Ui.ripple(main, t.surface2, 10, Ui.alpha(t.accent, 25)));
        month.setOnClickListener(v -> {
            Ui.haptic(v);
            main.showMonthPicker();
        });
        row.addView(month);
        header.addView(row);
        TextView sub = Ui.text(main, "预算、标签与本地数据", 11.5f, t.text3, t.regular);
        sub.setPadding(0, Ui.dp(main, 3), 0, 0);
        header.addView(sub);
        return header;
    }

    private View section(String text) {
        TextView tv = Ui.text(main, text, 11.5f, t.text3, t.medium);
        tv.setLetterSpacing(0.04f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(Ui.dp(main, 4), Ui.dp(main, 17), 0, Ui.dp(main, 7));
        tv.setLayoutParams(lp);
        return tv;
    }

    private LinearLayout card() {
        LinearLayout card = Ui.vertical(main);
        card.setBackground(Ui.roundStroke(main, t.surface, 20, t.line, 1));
        card.setPadding(Ui.dp(main, 4), Ui.dp(main, 4), Ui.dp(main, 4), Ui.dp(main, 4));
        return card;
    }

    private View buildBudgetCard() {
        LinearLayout card = card();
        String totalValue = s.configured ? Ui.money(s.total) : "未设置";
        card.addView(settingRow("总", t.accent, "本月总资金", totalValue,
                s.configured ? "点击调整本月可用总额" : "先设置一个总资金",
                v -> main.showBudgetSheet("total")));
        card.addView(Ui.dividerInset(main, t, 61));
        card.addView(settingRow("自", t.accent, "自由资金",
                s.configured ? Ui.money(s.free) : "—",
                s.configured
                        ? (s.dailyMode ? "每日预算模式" : "总资金的 " + Math.round(s.ratio) + "%")
                        + (s.carryIn > 0.004 ? " · 含结转 " + Ui.money(s.carryIn) : "")
                        : "由总资金与分配方式自动计算",
                v -> main.showBudgetSheet("free")));
        card.addView(Ui.dividerInset(main, t, 61));
        String dailySub = s.dailyMode
                ? "由每日预支出反推 · 优先于比例"
                : "按总资金与自由资金计算";
        if (s.configured && s.startDay > 1) {
            dailySub += " · 从 " + s.startDay + " 日起算 " + s.effectiveDays + " 天";
        }
        card.addView(settingRow("日", t.accent, "每日预资金",
                s.configured ? Ui.money(s.currentMonth ? s.todayAllowance : s.baseDaily) : "—",
                dailySub,
                v -> main.showBudgetSheet("daily")));
        card.addView(Ui.dividerInset(main, t, 61));

        LinearLayout switchRow = Ui.horizontal(main);
        switchRow.setPadding(Ui.dp(main, 12), Ui.dp(main, 10), Ui.dp(main, 8), Ui.dp(main, 10));
        switchRow.setMinimumHeight(Ui.dp(main, 60));
        LinearLayout switchTexts = Ui.vertical(main);
        switchTexts.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView switchTitle = Ui.text(main, "上月结余自动结转", 13, t.text, t.medium);
        TextView switchSub = Ui.text(main, "之后的新月份把上月剩余加入自由资金", 10.5f, t.text3, t.regular);
        switchSub.setPadding(0, Ui.dp(main, 3), 0, 0);
        switchTexts.addView(switchTitle);
        switchTexts.addView(switchSub);
        switchRow.addView(switchTexts);

        Switch toggle = new Switch(main);
        toggle.setChecked(store.isCarryEnabled());
        toggle.setShowText(false);
        toggle.setThumbTintList(new ColorStateList(new int[][]{
                new int[]{android.R.attr.state_checked}, new int[]{}
        }, new int[]{t.accent, t.text3}));
        toggle.setTrackTintList(new ColorStateList(new int[][]{
                new int[]{android.R.attr.state_checked}, new int[]{}
        }, new int[]{Ui.alpha(t.accent, 120), t.surface3}));
        toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            store.setCarryEnabled(isChecked);
            main.snackbar(isChecked ? "之后的新月份将自动结转上月剩余" : "新月份不再自动结转上月剩余");
        });
        switchRow.addView(toggle);
        card.addView(switchRow);

        card.addView(Ui.dividerInset(main, t, 16));
        card.addView(settingRow("转", t.amber, "重新结转本月",
                s.carryIn > 0.004 ? Ui.money(s.carryIn) : "¥0.00",
                "从上一月重新计算，并更新本月自由资金",
                v -> confirmRecalculateCarry()));
        return card;
    }

    private void confirmRecalculateCarry() {
        new AlertDialog.Builder(main)
                .setTitle("重新结转上月剩余？")
                .setMessage("会按上一月的最终剩余重新计算本月的自由资金与剩余资金。")
                .setNegativeButton("取消", null)
                .setPositiveButton("重新计算", (d, w) -> {
                    double carry = store.recalculateCarry(key);
                    main.onDataChanged();
                    main.snackbar("已重新结转 " + Ui.money(carry));
                })
                .show();
    }

    private View buildCategoryCard() {
        LinearLayout card = card();
        card.addView(settingRow("支", t.accent, "支出标签",
                store.categories(Models.EXPENSE).size() + " 个",
                "餐饮、日用、交通……可自行新增和改名",
                v -> CategorySheet.show(main, Models.EXPENSE)));
        card.addView(Ui.dividerInset(main, t, 61));
        card.addView(settingRow("收", t.green, "收入标签",
                store.categories(Models.INCOME).size() + " 个",
                "工资、兼职、红包……收入计入剩余与自由资金",
                v -> CategorySheet.show(main, Models.INCOME)));
        return card;
    }

    private View buildDataCard() {
        LinearLayout card = card();
        card.addView(settingRow("记", t.accent, "本月记录", s.txCount + " 笔",
                "点击回顾这个月的每一笔", v -> main.openTab(MainActivity.TAB_RECORDS)));
        card.addView(Ui.dividerInset(main, t, 61));
        card.addView(settingRow("全", t.text2, "全部账本",
                store.totalTransactionCount() + " 笔",
                "已记录 " + store.monthsWithDataCount() + " 个月 · 仅保存在本机", null));
        card.addView(Ui.dividerInset(main, t, 61));
        card.addView(settingRow("清", t.red, "清空本月记录", "",
                "只删除 " + Ui.month(ym) + " 的流水，预算保留", v -> confirmClearMonth()));
        return card;
    }

    private void confirmClearMonth() {
        if (s.txCount == 0) {
            main.snackbar("本月还没有记录");
            return;
        }
        new AlertDialog.Builder(main)
                .setTitle("清空本月记录？")
                .setMessage("会删除 " + Ui.month(ym) + " 的 " + s.txCount + " 笔流水，无法恢复。预算设置会保留。")
                .setNegativeButton("取消", null)
                .setPositiveButton("清空", (d, w) -> {
                    int removed = store.clearMonthTransactions(key);
                    main.onDataChanged();
                    main.snackbar("已清空 " + removed + " 笔记录");
                })
                .show();
    }

    private View buildAboutCard() {
        LinearLayout card = card();
        card.addView(settingRow("规", t.accent, "钱是怎么算的",
                "", "总资金、自由资金、每日预资金的完整规则",
                v -> InfoSheet.showRules(main)));
        card.addView(Ui.dividerInset(main, t, 61));
        card.addView(settingRow("色", t.accent, "外观",
                t.dark ? "深色" : "浅色", "跟随系统自动切换",
                null));
        card.addView(Ui.dividerInset(main, t, 61));
        card.addView(settingRow("版", t.text2, "版本",
                "1.0.0", "余量 · 为 Xiaomi HyperOS 与 Android 8+ 优化", null));
        return card;
    }

    private View settingRow(String badge, int color, String title, String value,
                            String sub, View.OnClickListener click) {
        LinearLayout row = Ui.horizontal(main);
        row.setPadding(Ui.dp(main, 12), Ui.dp(main, 10), Ui.dp(main, 10), Ui.dp(main, 10));
        row.setMinimumHeight(Ui.dp(main, 60));
        row.setBackground(Ui.ripple(main, Color.TRANSPARENT, 14, Ui.alpha(t.accent, 22)));
        if (click != null) {
            row.setClickable(true);
            row.setFocusable(true);
            row.setOnClickListener(v -> {
                Ui.haptic(v);
                click.onClick(v);
            });
        }

        FrameLayout badgeView = new FrameLayout(main);
        badgeView.setBackground(Ui.round(main, Ui.alpha(color, t.dark ? 46 : 26), 50));
        badgeView.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(main, 34), Ui.dp(main, 34)));
        TextView ch = Ui.text(main, badge, 12, color, t.medium);
        ch.setGravity(Gravity.CENTER);
        ch.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        badgeView.addView(ch);
        row.addView(badgeView);

        LinearLayout texts = Ui.vertical(main);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tlp.setMargins(Ui.dp(main, 12), 0, Ui.dp(main, 8), 0);
        texts.setLayoutParams(tlp);
        TextView titleView = Ui.text(main, title, 13,
                title.contains("清空") ? t.red : t.text, t.medium);
        texts.addView(titleView);
        if (sub != null && !sub.isEmpty()) {
            TextView subView = Ui.text(main, sub, 10.5f, t.text3, t.regular);
            subView.setPadding(0, Ui.dp(main, 3), 0, 0);
            subView.setMaxLines(2);
            subView.setEllipsize(android.text.TextUtils.TruncateAt.END);
            texts.addView(subView);
        }
        row.addView(texts);

        if (value != null && !value.isEmpty()) {
            TextView valueView = Ui.text(main, value, 12.5f,
                    title.contains("清空") ? t.red : t.text2, t.medium);
            valueView.setGravity(Gravity.END);
            row.addView(valueView);
        }
        if (click != null) {
            android.widget.ImageView chevron = Ui.icon(main, R.drawable.ic_chevron_right, 16, t.text3);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(Ui.dp(main, 16), Ui.dp(main, 16));
            clp.setMargins(Ui.dp(main, 5), 0, 0, 0);
            chevron.setLayoutParams(clp);
            row.addView(chevron);
        }
        return row;
    }

    private View footer() {
        LinearLayout footer = Ui.vertical(main);
        footer.setGravity(Gravity.CENTER);
        TextView dot = Ui.text(main, "余量", 12, t.accentDeep, t.medium);
        dot.setGravity(Gravity.CENTER);
        TextView sub = Ui.text(main, "把每一笔花得明白 · 数据只在本机", 10, t.text3, t.regular);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, Ui.dp(main, 4), 0, 0);
        footer.addView(dot);
        footer.addView(sub);
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        flp.setMargins(0, Ui.dp(main, 24), 0, 0);
        footer.setLayoutParams(flp);
        return footer;
    }
}
