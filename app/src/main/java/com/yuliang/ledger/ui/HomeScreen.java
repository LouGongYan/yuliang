package com.yuliang.ledger.ui;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.yuliang.ledger.MainActivity;
import com.yuliang.ledger.R;
import com.yuliang.ledger.data.LedgerStore;
import com.yuliang.ledger.data.Models;
import com.yuliang.ledger.ui.view.HeroCard;
import com.yuliang.ledger.ui.view.LineProgressView;
import com.yuliang.ledger.ui.view.MiniBarsView;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The overview screen. */
public class HomeScreen {

    private final MainActivity main;
    private final AppTheme t;
    private final LedgerStore store;
    private final YearMonth ym;
    private final String key;
    private final Models.Summary s;
    private final List<Models.Tx> txs;

    public HomeScreen(MainActivity main) {
        this.main = main;
        this.t = main.colors();
        this.store = main.store();
        this.ym = main.currentMonth();
        this.key = ym.toString();
        this.s = store.summary(key);
        this.txs = store.transactionsForMonth(key);
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
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        scroll.setLayoutParams(scrollLp);

        LinearLayout content = Ui.vertical(main);
        content.setPadding(Ui.dp(main, 18), Ui.dp(main, 4), Ui.dp(main, 18), Ui.dp(main, 22));
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        content.addView(buildHero());
        content.addView(Ui.spacer(main, 14));
        if (!s.configured) {
            content.addView(buildSetupCard());
            content.addView(Ui.spacer(main, 12));
        }
        content.addView(buildMetrics());
        content.addView(Ui.spacer(main, 12));
        content.addView(buildRhythm());
        content.addView(Ui.spacer(main, 12));
        content.addView(buildTip());
        content.addView(Ui.spacer(main, 20));
        content.addView(buildRecentHeader());
        content.addView(Ui.spacer(main, 8));
        content.addView(buildRecentCard());
        content.addView(Ui.spacer(main, 8));
        root.addView(scroll);
        return root;
    }

    // ------------------------------------------------------------------ header

    private View buildHeader() {
        LinearLayout header = Ui.vertical(main);
        header.setPadding(Ui.dp(main, 20), Ui.dp(main, 8), Ui.dp(main, 20), Ui.dp(main, 2));

        LinearLayout brandRow = Ui.horizontal(main);
        View dot = new View(main);
        dot.setBackground(Ui.round(main, t.accent, 50));
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(Ui.dp(main, 8), Ui.dp(main, 8));
        dotLp.setMargins(0, 0, Ui.dp(main, 7), 0);
        dot.setLayoutParams(dotLp);
        brandRow.addView(dot);

        TextView brand = Ui.text(main, "余量", 18.5f, t.text, t.medium);
        brand.setLetterSpacing(0.02f);
        brandRow.addView(brand);

        TextView tagline = Ui.text(main, "轻预算", 10, t.accentDeep, t.medium);
        tagline.setBackground(Ui.round(main, t.accentSoft, 8));
        tagline.setPadding(Ui.dp(main, 7), Ui.dp(main, 3), Ui.dp(main, 7), Ui.dp(main, 3));
        LinearLayout.LayoutParams tagLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tagLp.setMargins(Ui.dp(main, 8), 0, 0, 0);
        tagline.setLayoutParams(tagLp);
        brandRow.addView(tagline);

        View spacer = new View(main);
        brandRow.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));

        String dateText = s.currentMonth
                ? "今天 · " + Ui.dateLine(LocalDate.now())
                : "历史账本 · 回顾";
        TextView date = Ui.text(main, dateText, 10.5f, t.text3, t.regular);
        date.setGravity(Gravity.END);
        brandRow.addView(date);
        header.addView(brandRow);

        LinearLayout monthRow = Ui.horizontal(main);
        LinearLayout.LayoutParams monthRowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        monthRowLp.setMargins(0, Ui.dp(main, 10), 0, 0);
        monthRow.setLayoutParams(monthRowLp);

        LinearLayout monthGroup = Ui.horizontal(main);
        monthGroup.setClickable(true);
        monthGroup.setFocusable(true);
        monthGroup.setBackground(Ui.ripple(main, Color.TRANSPARENT, 12, Ui.alpha(t.accent, 24)));
        monthGroup.setPadding(Ui.dp(main, 2), Ui.dp(main, 2), Ui.dp(main, 6), Ui.dp(main, 2));
        monthGroup.setOnClickListener(v -> {
            Ui.haptic(v);
            main.showMonthPicker();
        });

        TextView monthNumber = Ui.text(main, String.valueOf(ym.getMonthValue()) + "月",
                27, t.text, t.medium);
        monthGroup.addView(monthNumber);
        TextView year = Ui.text(main, String.valueOf(ym.getYear()), 12, t.text3, t.medium);
        LinearLayout.LayoutParams yearLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        yearLp.setMargins(Ui.dp(main, 5), Ui.dp(main, 9), 0, 0);
        year.setLayoutParams(yearLp);
        monthGroup.addView(year);

        TextView monthTag = pill(s.currentMonth ? "本月" : "历史",
                s.currentMonth ? t.accentSoft : t.amberSoft,
                s.currentMonth ? t.accentDeep : t.amber);
        LinearLayout.LayoutParams mtLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mtLp.setMargins(Ui.dp(main, 7), Ui.dp(main, 7), 0, 0);
        monthTag.setLayoutParams(mtLp);
        monthGroup.addView(monthTag);
        monthRow.addView(monthGroup);

        View spacer2 = new View(main);
        monthRow.addView(spacer2, new LinearLayout.LayoutParams(0, 1, 1f));

        FrameLayout prev = Ui.iconButton(main, t, R.drawable.ic_chevron_left, t.text2);
        prev.setContentDescription("上个月");
        prev.setOnClickListener(v -> {
            Ui.haptic(v);
            if (ym.isAfter(YearMonth.of(2020, 1))) main.setMonth(ym.minusMonths(1));
        });
        monthRow.addView(prev);

        FrameLayout next = Ui.iconButton(main, t, R.drawable.ic_chevron_right, t.text2);
        next.setContentDescription("下个月");
        LinearLayout.LayoutParams nextLp = new LinearLayout.LayoutParams(Ui.dp(main, 40), Ui.dp(main, 40));
        nextLp.setMargins(Ui.dp(main, 4), 0, 0, 0);
        next.setLayoutParams(nextLp);
        if (s.currentMonth) {
            next.setAlpha(0.28f);
            next.setEnabled(false);
        } else {
            next.setOnClickListener(v -> {
                Ui.haptic(v);
                main.setMonth(ym.plusMonths(1));
            });
        }
        monthRow.addView(next);
        header.addView(monthRow);
        return header;
    }

    // ------------------------------------------------------------------ hero

    private View buildHero() {
        HeroCard hero = new HeroCard(main, t);
        hero.setElevation(Ui.dp(main, 7));
        hero.setClickable(true);
        hero.setFocusable(true);
        hero.setContentDescription("调整本月总资金");
        Ui.press(hero);
        hero.setOnClickListener(v -> {
            Ui.haptic(v);
            main.showBudgetSheet("total");
        });

        LinearLayout content = Ui.vertical(main);
        content.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        hero.addView(content);

        LinearLayout top = Ui.horizontal(main);
        TextView caption = Ui.text(main, "本月总资金", 12.5f, t.heroText2, t.medium);
        top.addView(caption);
        View spacer = new View(main);
        top.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));
        TextView tag;
        if (s.overBudget) {
            tag = heroPill("已超支", 0x3DFF9B8F, 0xFFFFD7D2);
        } else if (!s.currentMonth) {
            tag = heroPill("历史", 0x1FFFFFFF, t.heroText);
        } else {
            tag = heroPill("剩 " + Math.max(0, s.daysLeft) + " 天", 0x1FFFFFFF, t.heroText);
        }
        top.addView(tag);
        content.addView(top);

        TextView amount = Ui.text(main,
                s.configured ? Ui.money(s.total) : "未设置",
                40, t.heroText, t.medium);
        Ui.autoSize(amount, 24, 40);
        amount.setPadding(0, Ui.dp(main, 7), 0, 0);
        amount.setSingleLine(true);
        content.addView(amount);

        if (s.carryIn > 0.004) {
            TextView carry = Ui.text(main, "含上月结转 " + Ui.money(s.carryIn),
                    10.5f, t.heroText2, t.regular);
            carry.setPadding(0, Ui.dp(main, 4), 0, 0);
            content.addView(carry);
        } else if (!s.configured) {
            TextView setup = Ui.text(main, "点击设置本月可用总资金",
                    10.5f, t.heroText2, t.regular);
            setup.setPadding(0, Ui.dp(main, 4), 0, 0);
            content.addView(setup);
        }

        if (s.configured && s.total > 0) {
            LineProgressView progress = new LineProgressView(main,
                    0x33FFFFFF, 0xFFFFFFFF, 0xFFFFB3A8);
            LinearLayout.LayoutParams progressLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(main, 6));
            progressLp.setMargins(0, Ui.dp(main, 13), 0, 0);
            progress.setLayoutParams(progressLp);
            progress.setProgress((float) s.spentRatio, true);
            content.addView(progress);
        }

        LinearLayout bottom = Ui.horizontal(main);
        LinearLayout.LayoutParams bottomLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bottomLp.setMargins(0, Ui.dp(main, 11), 0, 0);
        bottom.setLayoutParams(bottomLp);

        TextView spent = Ui.text(main,
                "已支出 " + Ui.money(s.expense) + "   ·   收入 " + Ui.money(s.income),
                11.5f, t.heroText2, t.regular);
        bottom.addView(spent);
        View sp2 = new View(main);
        bottom.addView(sp2, new LinearLayout.LayoutParams(0, 1, 1f));
        TextView used = Ui.text(main,
                s.configured ? "已用 " + Ui.percent(s.spentRatio) : "待设置",
                10.5f, t.heroText, t.medium);
        bottom.addView(used);
        content.addView(bottom);

        if (s.rebalanced && s.currentMonth) {
            TextView note = Ui.text(main, "今日已按剩余资金重新分配每日预算",
                    10, t.heroText2, t.regular);
            note.setPadding(0, Ui.dp(main, 8), 0, 0);
            content.addView(note);
        }
        return hero;
    }

    private TextView heroPill(String text, int bg, int fg) {
        TextView tv = Ui.text(main, text, 9.5f, fg, t.medium);
        tv.setBackground(Ui.round(main, bg, 8));
        tv.setPadding(Ui.dp(main, 8), Ui.dp(main, 4), Ui.dp(main, 8), Ui.dp(main, 4));
        return tv;
    }

    private TextView pill(String text, int bg, int fg) {
        TextView tv = Ui.text(main, text, 9.5f, fg, t.medium);
        tv.setBackground(Ui.round(main, bg, 8));
        tv.setPadding(Ui.dp(main, 7), Ui.dp(main, 3), Ui.dp(main, 7), Ui.dp(main, 3));
        return tv;
    }

    // ------------------------------------------------------------------ setup

    private View buildSetupCard() {
        LinearLayout card = Ui.card(main, t);
        card.setBackground(Ui.roundStroke(main, t.surface, 22,
                Ui.alpha(t.accent, 95), 1));
        LinearLayout top = Ui.horizontal(main);
        FrameLayout iconHolder = new FrameLayout(main);
        iconHolder.setBackground(Ui.round(main, t.accentSoft, 50));
        iconHolder.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(main, 42), Ui.dp(main, 42)));
        android.widget.ImageView wallet = Ui.icon(main, R.drawable.ic_wallet, 21, t.accent);
        FrameLayout.LayoutParams wlp = new FrameLayout.LayoutParams(Ui.dp(main, 21), Ui.dp(main, 21));
        wlp.gravity = Gravity.CENTER;
        wallet.setLayoutParams(wlp);
        iconHolder.addView(wallet);
        top.addView(iconHolder);

        LinearLayout texts = Ui.vertical(main);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tl.setMargins(Ui.dp(main, 12), 0, 0, 0);
        texts.setLayoutParams(tl);
        TextView title = Ui.text(main, "先定下这个月能花多少", 14.5f, t.text, t.medium);
        TextView sub = Ui.text(main, "自由资金与每日预资金会自动算好", 11, t.text3, t.regular);
        sub.setPadding(0, Ui.dp(main, 3), 0, 0);
        texts.addView(title);
        texts.addView(sub);
        top.addView(texts);
        card.addView(top);

        TextView button = Ui.text(main, "设置本月预算", 13.5f, t.accentOn, t.medium);
        button.setGravity(Gravity.CENTER);
        button.setBackground(Ui.ripple(main, t.accent, 14, Ui.alpha(Color.WHITE, 45)));
        button.setClickable(true);
        button.setFocusable(true);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(main, 44));
        blp.setMargins(0, Ui.dp(main, 14), 0, 0);
        button.setLayoutParams(blp);
        button.setOnClickListener(v -> {
            Ui.haptic(v);
            main.showBudgetSheet("total");
        });
        card.addView(button);
        return card;
    }

    // ------------------------------------------------------------------ metrics

    private View buildMetrics() {
        LinearLayout wrapper = Ui.vertical(main);
        LinearLayout row1 = Ui.horizontal(main);
        row1.addView(metricFree(), weightCard(0, 5));
        row1.addView(metricDaily(), weightCard(5, 0));
        wrapper.addView(row1);

        LinearLayout row2 = Ui.horizontal(main);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, Ui.dp(main, 10), 0, 0);
        row2.setLayoutParams(rowLp);
        row2.addView(metricRemaining(), weightCard(0, 5));
        row2.addView(metricIncome(), weightCard(5, 0));
        wrapper.addView(row2);
        return wrapper;
    }

    private LinearLayout.LayoutParams weightCard(int left, int right) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(main, 150), 1f);
        lp.setMargins(Ui.dp(main, left), 0, Ui.dp(main, right), 0);
        return lp;
    }

    private LinearLayout metricBase(String badge, String label, int color, String amountText,
                                    int amountColor, String subText, int subColor,
                                    Runnable action, float progress) {
        LinearLayout card = Ui.vertical(main);
        card.setLayoutParams(new LinearLayout.LayoutParams(0, Ui.dp(main, 150), 1f));
        card.setBackground(Ui.rippleStroke(main, t.surface, 20, t.line, 1,
                Ui.alpha(t.accent, 28)));
        card.setPadding(Ui.dp(main, 14), Ui.dp(main, 13), Ui.dp(main, 14), Ui.dp(main, 12));
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> {
            Ui.haptic(v);
            action.run();
        });
        Ui.press(card);

        LinearLayout top = Ui.horizontal(main);
        FrameLayout badgeView = new FrameLayout(main);
        badgeView.setBackground(Ui.round(main, Ui.alpha(color, t.dark ? 48 : 28), 50));
        badgeView.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(main, 28), Ui.dp(main, 28)));
        TextView ch = Ui.text(main, badge, 11.5f, color, t.medium);
        ch.setGravity(Gravity.CENTER);
        ch.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        badgeView.addView(ch);
        top.addView(badgeView);

        TextView labelView = Ui.text(main, label, 11.5f, t.text2, t.medium);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.setMargins(Ui.dp(main, 7), 0, 0, 0);
        labelView.setLayoutParams(llp);
        top.addView(labelView);
        View sp = new View(main);
        top.addView(sp, new LinearLayout.LayoutParams(0, 1, 1f));
        TextView edit = Ui.text(main, "›", 14, t.text3, t.regular);
        top.addView(edit);
        card.addView(top);

        TextView amount = Ui.text(main, amountText, 22, amountColor, t.medium);
        Ui.autoSize(amount, 16, 22);
        amount.setSingleLine(true);
        amount.setPadding(0, Ui.dp(main, 9), 0, 0);
        card.addView(amount);

        TextView sub = Ui.text(main, subText, 10.3f, subColor, t.regular);
        sub.setMaxLines(2);
        sub.setEllipsize(android.text.TextUtils.TruncateAt.END);
        sub.setPadding(0, Ui.dp(main, 4), 0, 0);
        card.addView(sub);

        if (progress >= 0) {
            View spacer = new View(main);
            card.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1f));
            LineProgressView line = new LineProgressView(main, t.line, t.accent, t.red);
            LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(main, 4));
            line.setLayoutParams(lineLp);
            line.setProgress(progress, true);
            card.addView(line);
        }
        return card;
    }

    private View metricFree() {
        String sub;
        if (!s.configured) {
            sub = "设置总资金后自动生成";
        } else if (s.dailyMode) {
            sub = "每日预算模式" + (s.carryIn > 0.004 ? " · 含结转 " + Ui.money(s.carryIn) : "");
        } else {
            sub = "总资金的 " + Math.round(s.ratio) + "%"
                    + (s.carryIn > 0.004 ? " · 含结转 " + Ui.money(s.carryIn) : "");
        }
        return metricBase("自", "自由资金", t.accent,
                s.configured ? Ui.money(s.free) : "—",
                t.text, sub, t.text3,
                () -> main.showBudgetSheet("free"), -1);
    }

    private View metricDaily() {
        String sub;
        int subColor = t.text3;
        float progress = -1;
        double value = s.currentMonth ? s.todayAllowance : s.baseDaily;
        if (!s.configured) {
            sub = "设置后按本月天数平均";
        } else if (s.currentMonth) {
            sub = "今日剩余 " + Ui.money(s.todayRemaining);
            if (s.todaySpent > s.todayAllowance + 0.005) {
                sub = "今日已超支 " + Ui.money(s.todaySpent - s.todayAllowance)
                        + "，已由自由资金补足";
                subColor = t.amber;
            } else if (s.rebalanced) {
                sub += " · 已重算";
            }
            progress = s.todayAllowance > 0 ? (float) (s.todaySpent / s.todayAllowance) : 0;
        } else {
            sub = "本月日均支出 " + Ui.money(s.avgDaily);
        }
        return metricBase("日", "每日预资金", t.accent,
                s.configured ? Ui.money(value) : "—", t.text,
                sub, subColor, () -> main.showBudgetSheet("daily"), progress);
    }

    private View metricRemaining() {
        String sub;
        if (s.carryIn > 0.004) {
            sub = "含上月结转 " + Ui.money(s.carryIn);
        } else {
            sub = "总资金 + 收入 - 支出";
        }
        int color = s.remaining < -0.005 ? t.red : t.text;
        return metricBase("剩", "剩余资金", s.remaining < -0.005 ? t.red : t.green,
                Ui.money(s.remaining), color, sub, t.text3,
                () -> InfoSheet.showFunds(main), -1);
    }

    private View metricIncome() {
        int incomeCount = 0;
        for (Models.Tx tx : txs) if (tx.isIncome()) incomeCount++;
        String sub = incomeCount > 0 ? "共 " + incomeCount + " 笔收入" : "计入剩余与自由资金";
        return metricBase("收", "本月收入", t.green, Ui.money(s.income), t.green,
                sub, t.text3, () -> {
                    main.setRecordsFilter(2);
                    main.openTab(MainActivity.TAB_RECORDS);
                }, -1);
    }

    // ------------------------------------------------------------------ rhythm

    private View buildRhythm() {
        LinearLayout card = Ui.card(main, t);
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> {
            Ui.haptic(v);
            InfoSheet.showRules(main);
        });
        Ui.press(card);

        LinearLayout titleRow = Ui.horizontal(main);
        TextView title = Ui.text(main, "本月节奏", 14, t.text, t.medium);
        titleRow.addView(title);
        View sp = new View(main);
        titleRow.addView(sp, new LinearLayout.LayoutParams(0, 1, 1f));
        TextView avg = Ui.text(main, "日均支出 " + Ui.money(s.avgDaily), 10.5f, t.text3, t.regular);
        titleRow.addView(avg);
        card.addView(titleRow);

        MiniBarsView chart = new MiniBarsView(main);
        LinearLayout.LayoutParams chartLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(main, 112));
        chartLp.setMargins(0, Ui.dp(main, 10), 0, 0);
        chart.setLayoutParams(chartLp);
        chart.setColors(t.accent, Ui.alpha(t.accent, t.dark ? 90 : 55), t.surface2, t.text3);

        LocalDate end = s.currentMonth ? LocalDate.now() : ym.atEndOfMonth();
        Map<LocalDate, Double> byDate = new HashMap<>();
        for (Models.Tx tx : txs) {
            if (tx.isIncome()) continue;
            try {
                LocalDate d = LocalDate.parse(tx.date);
                byDate.put(d, byDate.getOrDefault(d, 0d) + tx.amount);
            } catch (Exception ignored) {
            }
        }
        double[] values = new double[7];
        String[] labels = new String[7];
        for (int i = 0; i < 7; i++) {
            LocalDate d = end.minusDays(6 - i);
            if (YearMonth.from(d).equals(ym)) {
                values[i] = byDate.getOrDefault(d, 0d);
                labels[i] = String.valueOf(d.getDayOfMonth());
            } else {
                values[i] = 0;
                labels[i] = "";
            }
        }
        chart.setData(values, labels, 6, true);
        card.addView(chart);

        View div = Ui.divider(main, t);
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, Ui.dp(main, 0.8f)));
        divLp.setMargins(0, Ui.dp(main, 8), 0, Ui.dp(main, 9));
        div.setLayoutParams(divLp);
        card.addView(div);

        LinearLayout footer = Ui.horizontal(main);
        TextView left = Ui.text(main,
                s.currentMonth ? "今日剩余 " + Ui.money(s.todayRemaining)
                        : "月末结余 " + Ui.money(s.free),
                11, s.currentMonth ? t.accentDeep : t.text2, t.medium);
        footer.addView(left);
        View sp2 = new View(main);
        footer.addView(sp2, new LinearLayout.LayoutParams(0, 1, 1f));
        TextView right = Ui.text(main,
                s.currentMonth ? "本月还剩 " + s.daysLeft + " 天" : "账本已归档",
                10.5f, t.text3, t.regular);
        footer.addView(right);
        card.addView(footer);
        return card;
    }

    private View buildTip() {
        LinearLayout tip = Ui.horizontal(main);
        tip.setBackground(Ui.ripple(main, t.accentSoft, 16, Ui.alpha(t.accent, 30)));
        tip.setPadding(Ui.dp(main, 13), Ui.dp(main, 11), Ui.dp(main, 12), Ui.dp(main, 11));
        tip.setClickable(true);
        tip.setFocusable(true);
        tip.setOnClickListener(v -> {
            Ui.haptic(v);
            InfoSheet.showRules(main);
        });
        android.widget.ImageView icon = Ui.icon(main, R.drawable.ic_info, 16, t.accent);
        tip.addView(icon);
        TextView text = Ui.text(main,
                "每天没用完的预算会自动转入自由资金", 11, t.accentDeep, t.medium);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tlp.setMargins(Ui.dp(main, 8), 0, Ui.dp(main, 6), 0);
        text.setLayoutParams(tlp);
        tip.addView(text);
        android.widget.ImageView chevron = Ui.icon(main, R.drawable.ic_chevron_right, 15, t.accent);
        tip.addView(chevron);
        return tip;
    }

    // ------------------------------------------------------------------ recent

    private View buildRecentHeader() {
        LinearLayout row = Ui.horizontal(main);
        TextView title = Ui.text(main, "最近记录", 14, t.text, t.medium);
        row.addView(title);
        View sp = new View(main);
        row.addView(sp, new LinearLayout.LayoutParams(0, 1, 1f));
        TextView all = Ui.text(main, "全部 " + txs.size() + " 笔", 11, t.accentDeep, t.medium);
        all.setOnClickListener(v -> {
            Ui.haptic(v);
            main.openTab(MainActivity.TAB_RECORDS);
        });
        all.setClickable(true);
        all.setPadding(Ui.dp(main, 6), Ui.dp(main, 4), 0, Ui.dp(main, 4));
        row.addView(all);
        android.widget.ImageView chevron = Ui.icon(main, R.drawable.ic_chevron_right, 15, t.accent);
        row.addView(chevron);
        return row;
    }

    private View buildRecentCard() {
        LinearLayout card = Ui.vertical(main);
        card.setBackground(Ui.roundStroke(main, t.surface, 20, t.line, 1));
        card.setPadding(Ui.dp(main, 4), Ui.dp(main, 4), Ui.dp(main, 4), Ui.dp(main, 4));

        if (txs.isEmpty()) {
            LinearLayout empty = Ui.vertical(main);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(Ui.dp(main, 18), Ui.dp(main, 22), Ui.dp(main, 18), Ui.dp(main, 22));
            FrameLayout iconHolder = new FrameLayout(main);
            iconHolder.setBackground(Ui.round(main, t.accentSoft, 50));
            iconHolder.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(main, 44), Ui.dp(main, 44)));
            android.widget.ImageView wallet = Ui.icon(main, R.drawable.ic_wallet, 21, t.accent);
            FrameLayout.LayoutParams wlp = new FrameLayout.LayoutParams(Ui.dp(main, 21), Ui.dp(main, 21));
            wlp.gravity = Gravity.CENTER;
            wallet.setLayoutParams(wlp);
            iconHolder.addView(wallet);
            empty.addView(iconHolder);
            TextView title = Ui.text(main, "还没有记录", 14, t.text, t.medium);
            title.setPadding(0, Ui.dp(main, 9), 0, 0);
            empty.addView(title);
            TextView sub = Ui.text(main, "从下面的输入框记下第一笔吧", 11, t.text3, t.regular);
            sub.setPadding(0, Ui.dp(main, 3), 0, 0);
            empty.addView(sub);
            card.addView(empty);
            return card;
        }

        int start = Math.max(0, txs.size() - 3);
        for (int i = txs.size() - 1; i >= start; i--) {
            Models.Tx tx = txs.get(i);
            View row = TxRowView.build(main, t, tx, v -> {
                Ui.haptic(v);
                main.openAddSheet(tx);
            });
            card.addView(row);
            if (i > start) {
                View div = Ui.dividerInset(main, t, 61);
                card.addView(div);
            }
        }
        return card;
    }
}
