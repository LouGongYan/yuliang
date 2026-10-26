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
import com.yuliang.ledger.ui.view.StackedBarView;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Transactions grouped by day, with a category breakdown. */
public class RecordsScreen {

    private final MainActivity main;
    private final AppTheme t;
    private final LedgerStore store;
    private final YearMonth ym;
    private final Models.Summary s;
    private final int filter;

    public RecordsScreen(MainActivity main) {
        this.main = main;
        this.t = main.colors();
        this.store = main.store();
        this.ym = main.currentMonth();
        this.s = store.summary(ym.toString());
        this.filter = main.getRecordsFilter();
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
        content.setPadding(Ui.dp(main, 18), Ui.dp(main, 2), Ui.dp(main, 18), Ui.dp(main, 24));
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        content.addView(buildSummary());
        content.addView(Ui.spacer(main, 14));
        content.addView(buildFilter());
        content.addView(Ui.spacer(main, 14));
        if (s.expense > 0) {
            content.addView(buildCategories());
            content.addView(Ui.spacer(main, 18));
        }
        content.addView(buildList());
        root.addView(scroll);
        return root;
    }

    private View buildHeader() {
        LinearLayout header = Ui.vertical(main);
        header.setPadding(Ui.dp(main, 20), Ui.dp(main, 10), Ui.dp(main, 20), Ui.dp(main, 4));

        LinearLayout titleRow = Ui.horizontal(main);
        TextView title = Ui.text(main, "明细", 27, t.text, t.medium);
        titleRow.addView(title);
        View spacer = new View(main);
        titleRow.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));
        if (s.currentMonth) {
            FrameLayout add = Ui.iconButton(main, t, R.drawable.ic_plus, t.accent);
            add.setContentDescription("记一笔");
            add.setOnClickListener(v -> {
                Ui.haptic(v);
                main.openAddSheet(null);
            });
            titleRow.addView(add);
        }
        header.addView(titleRow);

        LinearLayout monthRow = Ui.horizontal(main);
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mlp.setMargins(0, Ui.dp(main, 8), 0, 0);
        monthRow.setLayoutParams(mlp);

        LinearLayout monthGroup = Ui.horizontal(main);
        monthGroup.setClickable(true);
        monthGroup.setFocusable(true);
        monthGroup.setPadding(Ui.dp(main, 2), Ui.dp(main, 2), Ui.dp(main, 6), Ui.dp(main, 2));
        monthGroup.setBackground(Ui.ripple(main, Color.TRANSPARENT, 10, Ui.alpha(t.accent, 22)));
        monthGroup.setOnClickListener(v -> {
            Ui.haptic(v);
            main.showMonthPicker();
        });
        TextView month = Ui.text(main, Ui.month(ym), 14, t.text, t.medium);
        monthGroup.addView(month);
        TextView tag = Ui.text(main, s.currentMonth ? "本月" : "历史", 9.5f,
                s.currentMonth ? t.accentDeep : t.amber, t.medium);
        tag.setBackground(Ui.round(main, s.currentMonth ? t.accentSoft : t.amberSoft, 8));
        tag.setPadding(Ui.dp(main, 6), Ui.dp(main, 3), Ui.dp(main, 6), Ui.dp(main, 3));
        LinearLayout.LayoutParams tagLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tagLp.setMargins(Ui.dp(main, 7), 0, 0, 0);
        tag.setLayoutParams(tagLp);
        monthGroup.addView(tag);
        monthRow.addView(monthGroup);

        View sp2 = new View(main);
        monthRow.addView(sp2, new LinearLayout.LayoutParams(0, 1, 1f));

        FrameLayout prev = Ui.iconButton(main, t, R.drawable.ic_chevron_left, t.text2);
        prev.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(main, 36), Ui.dp(main, 36)));
        prev.setOnClickListener(v -> {
            Ui.haptic(v);
            if (ym.isAfter(YearMonth.of(2020, 1))) main.setMonth(ym.minusMonths(1));
        });
        monthRow.addView(prev);
        FrameLayout next = Ui.iconButton(main, t, R.drawable.ic_chevron_right, t.text2);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(Ui.dp(main, 36), Ui.dp(main, 36));
        nlp.setMargins(Ui.dp(main, 4), 0, 0, 0);
        next.setLayoutParams(nlp);
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

    private View buildSummary() {
        LinearLayout card = Ui.card(main, t);
        LinearLayout row = Ui.horizontal(main);
        LinearLayout left = Ui.vertical(main);
        left.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView l1 = Ui.text(main, "本月支出", 11, t.text3, t.regular);
        TextView l2 = Ui.text(main, Ui.money(s.expense), 27, t.text, t.medium);
        Ui.autoSize(l2, 20, 27);
        l2.setPadding(0, Ui.dp(main, 3), 0, 0);
        left.addView(l1);
        left.addView(l2);
        row.addView(left);

        LinearLayout right = Ui.vertical(main);
        right.setGravity(Gravity.END);
        right.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView r1 = Ui.text(main, "本月收入", 11, t.text3, t.regular);
        r1.setGravity(Gravity.END);
        TextView r2 = Ui.text(main, Ui.money(s.income), 17, t.green, t.medium);
        r2.setGravity(Gravity.END);
        r2.setPadding(0, Ui.dp(main, 6), 0, 0);
        right.addView(r1);
        right.addView(r2);
        row.addView(right);
        card.addView(row);

        View div = Ui.divider(main, t);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, Ui.dp(main, 0.8f)));
        dlp.setMargins(0, Ui.dp(main, 13), 0, Ui.dp(main, 10));
        div.setLayoutParams(dlp);
        card.addView(div);

        LinearLayout bottom = Ui.horizontal(main);
        TextView netLabel = Ui.text(main, "收支差", 11, t.text3, t.regular);
        bottom.addView(netLabel);
        double netValue = s.income - s.expense;
        TextView net = Ui.text(main, (netValue >= 0 ? "+" : "-") + Ui.money(Math.abs(netValue)),
                12, netValue >= 0 ? t.green : t.red, t.medium);
        LinearLayout.LayoutParams netLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        netLp.setMargins(Ui.dp(main, 6), 0, 0, 0);
        net.setLayoutParams(netLp);
        bottom.addView(net);
        View sp = new View(main);
        bottom.addView(sp, new LinearLayout.LayoutParams(0, 1, 1f));
        TextView count = Ui.text(main, "共 " + s.txCount + " 笔 · 日均 " + Ui.money(s.avgDaily),
                10.5f, t.text3, t.regular);
        bottom.addView(count);
        card.addView(bottom);
        return card;
    }

    private View buildFilter() {
        String[] options = {"全部", "支出", "收入"};
        com.yuliang.ledger.ui.view.SegmentedControl control =
                new com.yuliang.ledger.ui.view.SegmentedControl(main, t, options, filter);
        control.setOnChangeListener(main::setRecordsFilter);
        return control;
    }

    private View buildCategories() {
        Map<String, Double> totals = new LinkedHashMap<>();
        for (Models.Tx tx : store.transactionsForMonth(ym.toString())) {
            if (tx.isIncome()) continue;
            totals.put(tx.category, totals.getOrDefault(tx.category, 0d) + tx.amount);
        }
        List<Map.Entry<String, Double>> sorted = LedgerStore.sortedTotals(totals);
        if (sorted.isEmpty()) return new View(main);

        LinearLayout card = Ui.card(main, t);
        LinearLayout titleRow = Ui.horizontal(main);
        TextView title = Ui.text(main, "花在哪里", 14, t.text, t.medium);
        titleRow.addView(title);
        View sp = new View(main);
        titleRow.addView(sp, new LinearLayout.LayoutParams(0, 1, 1f));
        TextView total = Ui.text(main, "合计 " + Ui.money(s.expense), 10.5f, t.text3, t.regular);
        titleRow.addView(total);
        card.addView(titleRow);

        int max = Math.min(4, sorted.size());
        boolean hasMore = sorted.size() > max;
        double[] values = new double[max + (hasMore ? 1 : 0)];
        int[] colors = new int[values.length];
        double all = 0;
        for (Map.Entry<String, Double> e : sorted) all += e.getValue();
        for (int i = 0; i < max; i++) {
            values[i] = sorted.get(i).getValue();
            colors[i] = Ui.categoryColor(t, sorted.get(i).getKey());
        }
        double otherTotal = 0;
        for (int i = max; i < sorted.size(); i++) otherTotal += sorted.get(i).getValue();
        if (hasMore) {
            values[max] = otherTotal;
            colors[max] = t.text3;
        }
        StackedBarView bar = new StackedBarView(main);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(main, 10));
        blp.setMargins(0, Ui.dp(main, 12), 0, Ui.dp(main, 12));
        bar.setLayoutParams(blp);
        bar.setTrackColor(t.surface2);
        bar.setData(values, colors, true);
        card.addView(bar);

        for (int i = 0; i < values.length; i++) {
            String nameText = i < max ? sorted.get(i).getKey() : "其他";
            double amountValue = values[i];
            LinearLayout row = Ui.horizontal(main);
            View dot = new View(main);
            dot.setBackground(Ui.round(main, colors[i], 50));
            dot.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(main, 7), Ui.dp(main, 7)));
            row.addView(dot);
            TextView name = Ui.text(main, nameText, 11.5f, t.text2, t.medium);
            LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            nlp.setMargins(Ui.dp(main, 8), 0, 0, 0);
            name.setLayoutParams(nlp);
            row.addView(name);
            TextView percent = Ui.text(main, Math.round(amountValue / all * 100) + "%",
                    10.5f, t.text3, t.regular);
            row.addView(percent);
            TextView amount = Ui.text(main, Ui.money(amountValue), 11.5f, t.text, t.medium);
            LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            alp.setMargins(Ui.dp(main, 10), 0, 0, 0);
            amount.setLayoutParams(alp);
            row.addView(amount);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowLp.setMargins(0, i == 0 ? 0 : Ui.dp(main, 7), 0, 0);
            row.setLayoutParams(rowLp);
            card.addView(row);
        }
        return card;
    }

    private View buildList() {
        List<Models.Tx> filtered = new ArrayList<>();
        for (Models.Tx tx : store.transactionsForMonth(ym.toString())) {
            if (filter == 1 && tx.isIncome()) continue;
            if (filter == 2 && !tx.isIncome()) continue;
            filtered.add(tx);
        }
        filtered.sort(Comparator.comparing((Models.Tx tx) -> tx.date)
                .thenComparingLong(tx -> tx.createdAt)
                .thenComparingLong(tx -> tx.id)
                .reversed());

        LinearLayout wrapper = Ui.vertical(main);
        if (filtered.isEmpty()) {
            wrapper.addView(buildEmpty());
            return wrapper;
        }

        Map<LocalDate, List<Models.Tx>> days = new LinkedHashMap<>();
        for (Models.Tx tx : filtered) {
            try {
                LocalDate d = LocalDate.parse(tx.date);
                days.computeIfAbsent(d, k -> new ArrayList<>()).add(tx);
            } catch (Exception ignored) {
            }
        }

        for (Map.Entry<LocalDate, List<Models.Tx>> entry : days.entrySet()) {
            LocalDate date = entry.getKey();
            List<Models.Tx> list = entry.getValue();
            double expense = 0;
            double income = 0;
            for (Models.Tx tx : list) {
                if (tx.isIncome()) income += tx.amount;
                else expense += tx.amount;
            }

            LinearLayout head = Ui.horizontal(main);
            LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            hlp.setMargins(Ui.dp(main, 3), 0, Ui.dp(main, 3), Ui.dp(main, 7));
            head.setLayoutParams(hlp);
            String label = date.equals(LocalDate.now()) ? "今天 · " + Ui.dateLine(date) : Ui.dateLine(date);
            TextView left = Ui.text(main, label, 11, t.text2, t.medium);
            head.addView(left);
            View sp = new View(main);
            head.addView(sp, new LinearLayout.LayoutParams(0, 1, 1f));
            StringBuilder totals = new StringBuilder();
            if (expense > 0) totals.append("支出 ").append(Ui.money(expense));
            if (income > 0) {
                if (totals.length() > 0) totals.append(" · ");
                totals.append("收入 ").append(Ui.money(income));
            }
            TextView right = Ui.text(main, totals.toString(), 10.5f, t.text3, t.regular);
            head.addView(right);
            wrapper.addView(head);

            LinearLayout card = Ui.vertical(main);
            card.setBackground(Ui.roundStroke(main, t.surface, 18, t.line, 1));
            card.setPadding(Ui.dp(main, 4), Ui.dp(main, 4), Ui.dp(main, 4), Ui.dp(main, 4));
            for (int i = 0; i < list.size(); i++) {
                Models.Tx tx = list.get(i);
                View row = TxRowView.build(main, t, tx, v -> {
                    Ui.haptic(v);
                    main.openAddSheet(tx);
                });
                card.addView(row);
                if (i < list.size() - 1) card.addView(Ui.dividerInset(main, t, 61));
            }
            wrapper.addView(card);
            wrapper.addView(Ui.spacer(main, 14));
        }
        return wrapper;
    }

    private View buildEmpty() {
        LinearLayout card = Ui.card(main, t);
        card.setGravity(Gravity.CENTER);
        card.setPadding(Ui.dp(main, 18), Ui.dp(main, 26), Ui.dp(main, 18), Ui.dp(main, 26));
        FrameLayout iconHolder = new FrameLayout(main);
        iconHolder.setBackground(Ui.round(main, t.accentSoft, 50));
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(Ui.dp(main, 46), Ui.dp(main, 46));
        ilp.gravity = Gravity.CENTER_HORIZONTAL;
        iconHolder.setLayoutParams(ilp);
        android.widget.ImageView icon = Ui.icon(main, R.drawable.ic_receipt, 22, t.accent);
        FrameLayout.LayoutParams icLp = new FrameLayout.LayoutParams(Ui.dp(main, 22), Ui.dp(main, 22));
        icLp.gravity = Gravity.CENTER;
        icon.setLayoutParams(icLp);
        iconHolder.addView(icon);
        card.addView(iconHolder);
        TextView title = Ui.text(main, filter == 0 ? "这个月还没有记录" : "没有符合条件的记录",
                14, t.text, t.medium);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, Ui.dp(main, 10), 0, 0);
        card.addView(title);
        TextView sub = Ui.text(main, filter == 0 ? "记下第一笔，让每个数字都有来处" : "换个筛选看看",
                11, t.text3, t.regular);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, Ui.dp(main, 4), 0, 0);
        card.addView(sub);
        if (s.currentMonth && filter == 0) {
            TextView add = Ui.text(main, "记一笔", 12.5f, t.accentOn, t.medium);
            add.setGravity(Gravity.CENTER);
            add.setBackground(Ui.ripple(main, t.accent, 13, Ui.alpha(Color.WHITE, 45)));
            add.setClickable(true);
            add.setFocusable(true);
            LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                    Ui.dp(main, 132), Ui.dp(main, 42));
            alp.gravity = Gravity.CENTER_HORIZONTAL;
            alp.setMargins(0, Ui.dp(main, 16), 0, 0);
            add.setLayoutParams(alp);
            add.setOnClickListener(v -> {
                Ui.haptic(v);
                main.openAddSheet(null);
            });
            card.addView(add);
        } else if (filter != 0) {
            TextView all = Ui.text(main, "查看全部", 12, t.accentDeep, t.medium);
            all.setGravity(Gravity.CENTER);
            all.setPadding(Ui.dp(main, 12), Ui.dp(main, 8), Ui.dp(main, 12), Ui.dp(main, 8));
            all.setClickable(true);
            all.setOnClickListener(v -> main.setRecordsFilter(0));
            card.addView(all);
        }
        return card;
    }
}
