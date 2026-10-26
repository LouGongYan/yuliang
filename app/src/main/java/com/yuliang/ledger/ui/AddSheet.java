package com.yuliang.ledger.ui;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yuliang.ledger.MainActivity;
import com.yuliang.ledger.R;
import com.yuliang.ledger.data.LedgerStore;
import com.yuliang.ledger.data.Models;
import com.yuliang.ledger.ui.view.SegmentedControl;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/** The big add/edit transaction sheet with a custom numeric keypad. */
public final class AddSheet {

    private AddSheet() {
    }

    public static void show(MainActivity main, String monthKey, Models.Tx editing) {
        AppTheme t = main.colors();
        LedgerStore store = main.store();
        YearMonth parsedMonth;
        try {
            parsedMonth = YearMonth.parse(monthKey);
        } catch (Exception e) {
            parsedMonth = YearMonth.now();
        }
        final YearMonth ym = parsedMonth;
        final LocalDate[] selectedDate = {
                editing != null ? safeDate(editing.date, ym) : defaultDate(ym)
        };

        final String[] type = {editing != null ? editing.type : Models.EXPENSE};
        final ArrayList<String> cats = new ArrayList<>(store.categories(type[0]));
        final String[] selectedCat = {store.sanitizeCategory(type[0],
                editing != null ? editing.category : (cats.isEmpty() ? "其他" : cats.get(0)))};
        final String[] raw = {editing != null ? Ui.plain(editing.amount) : "0.00"};
        final boolean[] fresh = {true};

        LinearLayout content = Ui.vertical(main);
        content.setPadding(Ui.dp(main, 20), 0, Ui.dp(main, 20), Ui.dp(main, 14));

        LinearLayout titleRow = Ui.horizontal(main);
        TextView title = Ui.text(main, editing == null ? "记一笔" : "编辑记录", 21, t.text, t.medium);
        titleRow.addView(title);
        View sp = new View(main);
        titleRow.addView(sp, new LinearLayout.LayoutParams(0, 1, 1f));
        if (editing != null) {
            FrameLayout delete = Ui.iconButton(main, t, R.drawable.ic_trash, t.red);
            delete.setContentDescription("删除记录");
            delete.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(main, 36), Ui.dp(main, 36)));
            delete.setOnClickListener(v -> {
                Ui.haptic(v);
                confirmDelete(main, editing);
            });
            titleRow.addView(delete);
        }
        FrameLayout close = Ui.iconButton(main, t, R.drawable.ic_close, t.text2);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(Ui.dp(main, 36), Ui.dp(main, 36));
        clp.setMargins(Ui.dp(main, 4), 0, 0, 0);
        close.setLayoutParams(clp);
        close.setContentDescription("关闭");
        close.setOnClickListener(v -> {
            Ui.haptic(v);
            main.dismissSheet();
        });
        titleRow.addView(close);
        content.addView(titleRow);

        SegmentedControl typeSwitch = new SegmentedControl(main, t,
                new String[]{"支出", "收入"}, Models.INCOME.equals(type[0]) ? 1 : 0);
        LinearLayout.LayoutParams tslp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(main, 42));
        tslp.setMargins(0, Ui.dp(main, 12), 0, 0);
        typeSwitch.setLayoutParams(tslp);
        content.addView(typeSwitch);

        // Big amount box.
        LinearLayout amountBox = Ui.vertical(main);
        amountBox.setGravity(Gravity.CENTER);
        amountBox.setPadding(Ui.dp(main, 14), Ui.dp(main, 10), Ui.dp(main, 14), Ui.dp(main, 10));
        LinearLayout.LayoutParams abl = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(main, 88));
        abl.setMargins(0, Ui.dp(main, 12), 0, 0);
        amountBox.setLayoutParams(abl);
        amountBox.setBackground(Ui.roundStroke(main, t.surface2, 18, t.line, 1));

        TextView amountCaption = Ui.text(main,
                Models.INCOME.equals(type[0]) ? "收入金额" : "支出金额",
                10, t.text3, t.medium);
        amountCaption.setGravity(Gravity.CENTER);
        amountBox.addView(amountCaption);

        LinearLayout amountLine = Ui.horizontal(main);
        amountLine.setGravity(Gravity.CENTER);
        TextView currency = Ui.text(main, "¥", 22, t.text3, t.medium);
        amountLine.addView(currency);
        TextView amountDisplay = Ui.text(main, Ui.formatInput(raw[0]),
                40, Models.INCOME.equals(type[0]) ? t.green : t.text, t.medium);
        Ui.autoSize(amountDisplay, 24, 40);
        LinearLayout.LayoutParams adLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        adLp.setMargins(Ui.dp(main, 8), 0, 0, 0);
        amountDisplay.setLayoutParams(adLp);
        amountDisplay.setSingleLine(true);
        amountLine.addView(amountDisplay);
        amountBox.addView(amountLine);
        content.addView(amountBox);

        // Category chips.
        TextView catLabel = Ui.text(main, "标签", 10, t.text3, t.medium);
        catLabel.setPadding(0, Ui.dp(main, 12), 0, Ui.dp(main, 5));
        content.addView(catLabel);

        HorizontalScrollView catScroll = new HorizontalScrollView(main);
        catScroll.setHorizontalScrollBarEnabled(false);
        catScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        catScroll.setClipToPadding(false);
        LinearLayout catRow = Ui.horizontal(main);
        catRow.setPadding(0, 0, Ui.dp(main, 4), 0);
        catScroll.addView(catRow, new HorizontalScrollView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        content.addView(catScroll);

        EditText note = new EditText(main);
        note.setText(editing != null && editing.note != null ? editing.note : "");
        note.setHint("用途 / 备注（可选）");
        note.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13);
        note.setTypeface(t.regular);
        note.setTextColor(t.text);
        note.setHintTextColor(t.text3);
        note.setSingleLine(true);
        note.setBackground(Ui.roundStroke(main, t.surface2, 14, t.line, 1));
        note.setPadding(Ui.dp(main, 14), Ui.dp(main, 11), Ui.dp(main, 14), Ui.dp(main, 11));
        note.setFilters(new InputFilter[]{new InputFilter.LengthFilter(40)});
        note.setImeOptions(EditorInfo.IME_ACTION_DONE);
        note.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                Ui.hideKeyboard(main);
                return true;
            }
            return false;
        });
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nlp.setMargins(0, Ui.dp(main, 10), 0, 0);
        note.setLayoutParams(nlp);
        content.addView(note);

        // Date row.
        LinearLayout dateRow = Ui.horizontal(main);
        dateRow.setBackground(Ui.ripple(main, t.surface2, 14, Ui.alpha(t.accent, 25)));
        dateRow.setPadding(Ui.dp(main, 12), Ui.dp(main, 10), Ui.dp(main, 12), Ui.dp(main, 10));
        LinearLayout.LayoutParams drlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        drlp.setMargins(0, Ui.dp(main, 10), 0, 0);
        dateRow.setLayoutParams(drlp);
        dateRow.setClickable(true);
        dateRow.setFocusable(true);
        android.widget.ImageView calIcon = Ui.icon(main, R.drawable.ic_calendar, 18, t.text2);
        dateRow.addView(calIcon);
        final TextView dateText = Ui.text(main, "", 12, t.text, t.medium);
        LinearLayout.LayoutParams dtlp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        dtlp.setMargins(Ui.dp(main, 9), 0, 0, 0);
        dateText.setLayoutParams(dtlp);
        dateRow.addView(dateText);
        TextView dateHint = Ui.text(main, "可改", 10, t.text3, t.regular);
        dateRow.addView(dateHint);
        android.widget.ImageView dateChevron = Ui.icon(main, R.drawable.ic_chevron_right, 15, t.text3);
        LinearLayout.LayoutParams dclp = new LinearLayout.LayoutParams(Ui.dp(main, 15), Ui.dp(main, 15));
        dclp.setMargins(Ui.dp(main, 4), 0, 0, 0);
        dateChevron.setLayoutParams(dclp);
        dateRow.addView(dateChevron);
        content.addView(dateRow);

        TextView error = Ui.text(main, "", 11, t.red, t.medium);
        error.setVisibility(View.GONE);
        error.setPadding(0, Ui.dp(main, 8), 0, 0);
        content.addView(error);

        // Keypad.
        LinearLayout keypad = Ui.vertical(main);
        LinearLayout.LayoutParams klp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        klp.setMargins(0, Ui.dp(main, 10), 0, 0);
        keypad.setLayoutParams(klp);
        String[][] keys = {{"1", "2", "3"}, {"4", "5", "6"}, {"7", "8", "9"}, {".", "0", "⌫"}};
        for (int r = 0; r < keys.length; r++) {
            LinearLayout row = Ui.horizontal(main);
            if (r > 0) {
                LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rlp.setMargins(0, Ui.dp(main, 7), 0, 0);
                row.setLayoutParams(rlp);
            }
            for (int c = 0; c < 3; c++) {
                String key = keys[r][c];
                LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(0, Ui.dp(main, 48), 1f);
                blp.setMargins(c == 0 ? 0 : Ui.dp(main, 7), 0, c == 2 ? 0 : Ui.dp(main, 7), 0);
                View button;
                if ("⌫".equals(key)) {
                    FrameLayout holder = new FrameLayout(main);
                    holder.setLayoutParams(blp);
                    holder.setBackground(Ui.ripple(main, t.surface2, 14, Ui.alpha(t.accent, 30)));
                    holder.setClickable(true);
                    holder.setFocusable(true);
                    android.widget.ImageView back = Ui.icon(main, R.drawable.ic_backspace, 21, t.text2);
                    FrameLayout.LayoutParams blp2 = new FrameLayout.LayoutParams(
                            Ui.dp(main, 21), Ui.dp(main, 21));
                    blp2.gravity = Gravity.CENTER;
                    back.setLayoutParams(blp2);
                    holder.addView(back);
                    holder.setOnClickListener(v -> {
                        Ui.haptic(v);
                        backspace(raw, fresh);
                        refreshAmount(amountDisplay, raw);
                        error.setVisibility(View.GONE);
                    });
                    holder.setOnLongClickListener(v -> {
                        Ui.haptic(v);
                        raw[0] = "0.00";
                        fresh[0] = true;
                        refreshAmount(amountDisplay, raw);
                        Ui.snack(main, "已清空金额");
                        return true;
                    });
                    button = holder;
                } else {
                    TextView tv = Ui.text(main, key, 21, t.text, t.medium);
                    tv.setGravity(Gravity.CENTER);
                    tv.setLayoutParams(blp);
                    tv.setBackground(Ui.ripple(main, t.surface2, 14, Ui.alpha(t.accent, 30)));
                    tv.setClickable(true);
                    tv.setFocusable(true);
                    tv.setOnClickListener(v -> {
                        Ui.haptic(v);
                        if (".".equals(key)) appendDot(raw, fresh);
                        else appendDigit(raw, fresh, key);
                        refreshAmount(amountDisplay, raw);
                        error.setVisibility(View.GONE);
                    });
                    button = tv;
                }
                row.addView(button);
            }
            keypad.addView(row);
        }
        content.addView(keypad);

        TextView keyHint = Ui.text(main, "长按退格可清空金额", 9.5f, t.text3, t.regular);
        keyHint.setGravity(Gravity.END);
        keyHint.setPadding(0, Ui.dp(main, 5), 0, 0);
        content.addView(keyHint);

        TextView save = Ui.text(main, "保存支出", 13.5f, t.accentOn, t.medium);
        save.setGravity(Gravity.CENTER);
        save.setBackground(Ui.ripple(main, t.accent, 14, Ui.alpha(Color.WHITE, 45)));
        save.setClickable(true);
        save.setFocusable(true);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(main, 50));
        slp.setMargins(0, Ui.dp(main, 6), 0, 0);
        save.setLayoutParams(slp);
        content.addView(save);

        // Rebuild category chips for the current type / selected category.
        final Runnable[] rebuildChips = new Runnable[1];
        rebuildChips[0] = () -> {
            catRow.removeAllViews();
            for (int i = 0; i < cats.size(); i++) {
                String name = cats.get(i);
                final String value = name;
                TextView chip = Ui.chip(main, t, name, name.equals(selectedCat[0]));
                LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                chipLp.setMargins(i == 0 ? 0 : Ui.dp(main, 6), 0, 0, 0);
                chip.setLayoutParams(chipLp);
                chip.setOnClickListener(v -> {
                    Ui.haptic(v);
                    selectedCat[0] = value;
                    rebuildChips[0].run();
                    error.setVisibility(View.GONE);
                });
                catRow.addView(chip);
            }
            TextView add = Ui.text(main, "＋ 新标签", 12, t.accentDeep, t.medium);
            add.setPadding(Ui.dp(main, 12), Ui.dp(main, 8), Ui.dp(main, 12), Ui.dp(main, 8));
            add.setBackground(Ui.rippleStroke(main, t.accentSoft, 13,
                    Ui.alpha(t.accent, 150), 1, Ui.alpha(t.accent, 35)));
            add.setClickable(true);
            add.setFocusable(true);
            LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            alp.setMargins(Ui.dp(main, 6), 0, 0, 0);
            add.setLayoutParams(alp);
            add.setOnClickListener(v -> {
                Ui.haptic(v);
                showAddCategory(main, type[0], cats, selectedCat, rebuildChips[0], error);
            });
            catRow.addView(add);
        };
        rebuildChips[0].run();

        final Runnable refreshTypeUi = () -> {
            boolean income = Models.INCOME.equals(type[0]);
            amountCaption.setText(income ? "收入金额" : "支出金额");
            amountDisplay.setTextColor(income ? t.green : t.text);
            currency.setTextColor(income ? t.green : t.text3);
            amountBox.setBackground(Ui.roundStroke(main,
                    income ? t.greenSoft : t.surface2, 18,
                    income ? Ui.alpha(t.green, 70) : t.line, 1));
            save.setText(editing == null ? (income ? "保存收入" : "保存支出") : "保存修改");
        };
        refreshTypeUi.run();

        final Runnable updateDate = () -> {
            LocalDate d = selectedDate[0];
            String text = d.equals(LocalDate.now())
                    ? "今天 · " + Ui.dateLine(d)
                    : Ui.dateLine(d);
            dateText.setText(text);
        };
        updateDate.run();

        dateRow.setOnClickListener(v -> {
            Ui.haptic(v);
            openDatePicker(main, ym, selectedDate, updateDate);
        });

        typeSwitch.setOnChangeListener(index -> {
            type[0] = index == 1 ? Models.INCOME : Models.EXPENSE;
            List<String> next = store.categories(type[0]);
            cats.clear();
            cats.addAll(next);
            if (!cats.contains(selectedCat[0])) {
                selectedCat[0] = cats.isEmpty() ? "其他" : cats.get(0);
            }
            rebuildChips[0].run();
            refreshTypeUi.run();
            error.setVisibility(View.GONE);
        });

        save.setOnClickListener(v -> {
            Ui.hapticConfirm(v);
            double amount = Ui.parseMoney(raw[0]);
            if (amount < 0.01) {
                showError(error, "请输入大于 0 的金额");
                return;
            }
            if (amount > 999_999_999d) {
                showError(error, "金额太大了");
                return;
            }
            if (selectedCat[0] == null || selectedCat[0].isEmpty()) {
                showError(error, "请选择一个标签");
                return;
            }
            LocalDate date = selectedDate[0];
            if (date.isAfter(maxDate(ym))) {
                showError(error, "不能记录未来的日期");
                return;
            }

            Models.Tx tx = editing != null ? editing.copy() : new Models.Tx();
            tx.type = type[0];
            tx.amount = Math.round(amount * 100.0) / 100.0;
            tx.category = selectedCat[0];
            tx.note = note.getText().toString().trim();
            tx.date = date.toString();
            if (tx.createdAt <= 0) tx.createdAt = System.currentTimeMillis();

            if (editing != null) store.updateTransaction(tx);
            else store.addTransaction(tx);

            main.dismissSheet();
            main.onDataChanged();
            main.snackbar((editing == null ? "已记录 " : "已更新 ")
                    + (tx.isIncome() ? "收入 " : "支出 ")
                    + tx.category + " " + Ui.money(tx.amount));
        });

        main.openSheet(content, 0.96f, null);
    }

    private static void refreshAmount(TextView display, String[] raw) {
        display.setText(Ui.formatInput(raw[0]));
        display.animate().cancel();
        display.setScaleX(1.035f);
        display.setScaleY(1.035f);
        display.animate().scaleX(1f).scaleY(1f).setDuration(110).start();
    }

    private static void appendDigit(String[] raw, boolean[] fresh, String digit) {
        if (fresh[0]) {
            raw[0] = "0".equals(digit) ? "0" : digit;
            fresh[0] = false;
            return;
        }
        String s = raw[0];
        if (s.indexOf('.') >= 0) {
            int dot = s.indexOf('.');
            if (s.length() - dot - 1 >= 2) return;
            raw[0] = s + digit;
        } else {
            String intPart = s.replace(",", "");
            if (intPart.length() >= 8) return;
            if ("0".equals(intPart)) raw[0] = digit;
            else raw[0] = s + digit;
        }
    }

    private static void appendDot(String[] raw, boolean[] fresh) {
        if (fresh[0]) {
            raw[0] = "0.";
            fresh[0] = false;
            return;
        }
        if (raw[0].indexOf('.') < 0) raw[0] = raw[0] + ".";
    }

    private static void backspace(String[] raw, boolean[] fresh) {
        if (fresh[0]) {
            raw[0] = "0";
            fresh[0] = false;
            return;
        }
        String s = raw[0];
        if (s.length() > 1) raw[0] = s.substring(0, s.length() - 1);
        else raw[0] = "0";
        if (raw[0].isEmpty()) raw[0] = "0";
    }

    private static void showError(TextView error, String text) {
        error.setText(text);
        error.setVisibility(View.VISIBLE);
    }

    private static LocalDate maxDate(YearMonth ym) {
        LocalDate now = LocalDate.now();
        YearMonth current = YearMonth.from(now);
        if (ym.equals(current)) return now;
        if (ym.isAfter(current)) return now;
        return ym.atEndOfMonth();
    }

    private static LocalDate defaultDate(YearMonth ym) {
        LocalDate now = LocalDate.now();
        if (YearMonth.from(now).equals(ym)) return now;
        return ym.atEndOfMonth();
    }

    private static LocalDate safeDate(String raw, YearMonth ym) {
        try {
            LocalDate d = LocalDate.parse(raw);
            if (YearMonth.from(d).equals(ym)) return d;
        } catch (Exception ignored) {
        }
        return defaultDate(ym);
    }

    private static void openDatePicker(MainActivity main, YearMonth ym,
                                       LocalDate[] selectedDate, Runnable onChange) {
        LocalDate value = selectedDate[0];
        DatePickerDialog dialog = new DatePickerDialog(main, (view, year, month, day) -> {
            selectedDate[0] = LocalDate.of(year, month + 1, day);
            onChange.run();
        }, value.getYear(), value.getMonthValue() - 1, value.getDayOfMonth());
        ZoneId zone = ZoneId.systemDefault();
        dialog.getDatePicker().setMinDate(ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli());
        dialog.getDatePicker().setMaxDate(maxDate(ym).atStartOfDay(zone).toInstant().toEpochMilli());
        dialog.show();
    }

    private static void showAddCategory(MainActivity main, String type,
                                        ArrayList<String> cats, String[] selectedCat,
                                        Runnable onChanged, TextView error) {
        AppTheme t = main.colors();
        EditText input = new EditText(main);
        input.setHint("例如：咖啡、宠物、房租");
        input.setSingleLine(true);
        input.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15);
        input.setTextColor(t.text);
        input.setHintTextColor(t.text3);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(12)});
        input.setBackground(Ui.roundStroke(main, t.surface2, 12, t.line, 1));
        input.setPadding(Ui.dp(main, 12), Ui.dp(main, 9), Ui.dp(main, 12), Ui.dp(main, 9));
        LinearLayout wrap = Ui.vertical(main);
        wrap.setPadding(Ui.dp(main, 20), Ui.dp(main, 8), Ui.dp(main, 20), 0);
        wrap.addView(input);

        AlertDialog dialog = new AlertDialog.Builder(main)
                .setTitle(Models.INCOME.equals(type) ? "新增收入标签" : "新增支出标签")
                .setView(wrap)
                .setNegativeButton("取消", null)
                .setPositiveButton("添加", null)
                .create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = input.getText().toString().trim();
                if (name.isEmpty()) {
                    input.setError("请输入标签名");
                    return;
                }
                String before = null;
                if (!main.store().addCategory(type, name)) {
                    input.setError("标签已存在或数量已满");
                    return;
                }
                selectedCat[0] = name;
                cats.clear();
                cats.addAll(main.store().categories(type));
                onChanged.run();
                error.setVisibility(View.GONE);
                dialog.dismiss();
            });
            Ui.showKeyboard(input);
        });
        dialog.show();
    }

    private static void confirmDelete(MainActivity main, Models.Tx editing) {
        new AlertDialog.Builder(main)
                .setTitle("删除这笔记录？")
                .setMessage(editing.category + " · " + Ui.money(editing.amount) + "，删除后无法恢复。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (d, w) -> {
                    main.store().deleteTransaction(editing.id);
                    main.dismissSheet();
                    main.onDataChanged();
                    main.snackbar("记录已删除");
                })
                .show();
    }
}
