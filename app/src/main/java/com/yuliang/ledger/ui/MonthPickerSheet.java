package com.yuliang.ledger.ui;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yuliang.ledger.MainActivity;
import com.yuliang.ledger.R;

import java.time.YearMonth;

/** Year + 12 month grid. Future months are disabled. */
public final class MonthPickerSheet {

    private MonthPickerSheet() {
    }

    public static void show(MainActivity main) {
        AppTheme t = main.colors();
        YearMonth now = YearMonth.now();
        YearMonth selected = main.currentMonth();
        int[] year = {Math.min(now.getYear(), Math.max(2020, selected.getYear()))};

        LinearLayout content = Ui.vertical(main);
        content.setPadding(Ui.dp(main, 20), 0, Ui.dp(main, 20), Ui.dp(main, 16));

        LinearLayout titleRow = Ui.horizontal(main);
        TextView title = Ui.text(main, "选择月份", 21, t.text, t.medium);
        titleRow.addView(title);
        View sp = new View(main);
        titleRow.addView(sp, new LinearLayout.LayoutParams(0, 1, 1f));
        TextView hint = Ui.text(main, "有记录的月份带小圆点", 10, t.text3, t.regular);
        titleRow.addView(hint);
        content.addView(titleRow);

        LinearLayout yearRow = Ui.horizontal(main);
        LinearLayout.LayoutParams yrlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        yrlp.setMargins(0, Ui.dp(main, 12), 0, Ui.dp(main, 6));
        yearRow.setLayoutParams(yrlp);

        FrameLayout prev = Ui.iconButton(main, t, R.drawable.ic_chevron_left, t.text2);
        prev.setContentDescription("上一年");
        yearRow.addView(prev);

        TextView yearText = Ui.text(main, String.valueOf(year[0]), 18, t.text, t.medium);
        yearText.setGravity(Gravity.CENTER);
        yearText.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        yearRow.addView(yearText);

        FrameLayout next = Ui.iconButton(main, t, R.drawable.ic_chevron_right, t.text2);
        next.setContentDescription("下一年");
        yearRow.addView(next);
        content.addView(yearRow);

        FrameLayout gridHost = new FrameLayout(main);
        gridHost.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        content.addView(gridHost);

        final Runnable[] rebuild = new Runnable[1];
        rebuild[0] = () -> {
            yearText.setText(String.valueOf(year[0]));
            prev.setAlpha(year[0] > 2020 ? 1f : 0.28f);
            prev.setEnabled(year[0] > 2020);
            next.setAlpha(year[0] < now.getYear() ? 1f : 0.28f);
            next.setEnabled(year[0] < now.getYear());

            GridLayout grid = new GridLayout(main);
            grid.setColumnCount(3);
            grid.setRowCount(4);
            grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
            grid.setUseDefaultMargins(false);
            gridHost.removeAllViews();
            gridHost.addView(grid, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            for (int m = 1; m <= 12; m++) {
                final int monthValue = m;
                YearMonth value = YearMonth.of(year[0], m);
                boolean future = value.isAfter(now);
                boolean on = value.equals(selected);

                FrameLayout cell = new FrameLayout(main);
                cell.setBackground(on
                        ? Ui.ripple(main, t.accent, 14, Ui.alpha(Color.WHITE, 45))
                        : Ui.ripple(main, t.surface2, 14, Ui.alpha(t.accent, 25)));
                cell.setAlpha(future ? 0.3f : 1f);
                cell.setClickable(!future);
                cell.setFocusable(!future);

                TextView label = Ui.text(main, m + "月", 13, on ? t.accentOn : t.text, t.medium);
                label.setGravity(Gravity.CENTER);
                label.setLayoutParams(new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
                cell.addView(label);

                boolean hasData = main.store().hasData(value.toString());
                if (hasData && !on) {
                    View dot = new View(main);
                    dot.setBackground(Ui.round(main, t.accent, 50));
                    FrameLayout.LayoutParams dotLp = new FrameLayout.LayoutParams(
                            Ui.dp(main, 4), Ui.dp(main, 4));
                    dotLp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                    dotLp.bottomMargin = Ui.dp(main, 7);
                    dot.setLayoutParams(dotLp);
                    cell.addView(dot);
                }

                if (!future) {
                    cell.setOnClickListener(v -> {
                        Ui.haptic(v);
                        main.setMonth(value);
                        main.dismissSheet();
                    });
                }

                GridLayout.LayoutParams glp = new GridLayout.LayoutParams(
                        GridLayout.spec((m - 1) / 3, 1, 1f),
                        GridLayout.spec((m - 1) % 3, 1, 1f));
                glp.width = 0;
                glp.height = Ui.dp(main, 54);
                glp.setMargins(Ui.dp(main, 4), Ui.dp(main, 4), Ui.dp(main, 4), Ui.dp(main, 4));
                grid.addView(cell, glp);
            }
        };

        prev.setOnClickListener(v -> {
            if (year[0] <= 2020) return;
            Ui.haptic(v);
            year[0]--;
            rebuild[0].run();
        });
        next.setOnClickListener(v -> {
            if (year[0] >= now.getYear()) return;
            Ui.haptic(v);
            year[0]++;
            rebuild[0].run();
        });
        rebuild[0].run();

        if (!selected.equals(now)) {
            TextView back = Ui.text(main, "回到本月 · " + Ui.month(now), 12.5f, t.accentDeep, t.medium);
            back.setGravity(Gravity.CENTER);
            back.setBackground(Ui.ripple(main, t.accentSoft, 13, Ui.alpha(t.accent, 30)));
            back.setClickable(true);
            back.setFocusable(true);
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(main, 44));
            blp.setMargins(0, Ui.dp(main, 12), 0, 0);
            back.setLayoutParams(blp);
            back.setOnClickListener(v -> {
                Ui.haptic(v);
                main.goToCurrentMonth();
                main.dismissSheet();
            });
            content.addView(back);
        }

        main.openSheet(content, 0.8f, null);
    }
}
