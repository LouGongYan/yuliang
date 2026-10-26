package com.yuliang.ledger.ui;

import android.graphics.Color;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.yuliang.ledger.MainActivity;
import com.yuliang.ledger.R;
import com.yuliang.ledger.data.LedgerStore;
import com.yuliang.ledger.data.Models;
import com.yuliang.ledger.ui.view.SegmentedControl;

import java.time.LocalDate;
import java.time.YearMonth;

/** Sheet for total funds, free-fund share and the daily-budget override. */
public final class BudgetSheet {

    private BudgetSheet() {
    }

    public static void show(MainActivity main, String monthKey, String focus) {
        AppTheme t = main.colors();
        LedgerStore store = main.store();
        Models.MonthConfig config = store.ensureMonth(monthKey);
        Models.Summary summary = store.summary(monthKey);
        YearMonth ym;
        try {
            ym = YearMonth.parse(monthKey);
        } catch (Exception e) {
            ym = YearMonth.now();
        }
        int days = ym.lengthOfMonth();
        boolean firstSetup = !config.configured;
        int startDay;
        if (config.configured) {
            startDay = Math.max(1, Math.min(days, config.startDay));
        } else if (ym.equals(YearMonth.now())) {
            startDay = LocalDate.now().getDayOfMonth();
        } else {
            startDay = 1;
        }
        int effectiveDays = Math.max(1, days - startDay + 1);

        int initialMode = config.dailyMode ? 1 : 0;
        if ("daily".equals(focus)) initialMode = 1;

        LinearLayout content = Ui.vertical(main);
        content.setPadding(Ui.dp(main, 22), 0, Ui.dp(main, 22), Ui.dp(main, 16));

        LinearLayout titleRow = Ui.horizontal(main);
        TextView title = Ui.text(main,
                firstSetup ? "设置本月预算" : "调整本月预算",
                21, t.text, t.medium);
        titleRow.addView(title);
        View sp = new View(main);
        titleRow.addView(sp, new LinearLayout.LayoutParams(0, 1, 1f));
        FrameLayout close = Ui.iconButton(main, t, R.drawable.ic_close, t.text2);
        close.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(main, 36), Ui.dp(main, 36)));
        close.setOnClickListener(v -> {
            Ui.haptic(v);
            main.dismissSheet();
        });
        titleRow.addView(close);
        content.addView(titleRow);

        TextView sub = Ui.text(main,
                firstSetup ? "先定下这个月可以花多少，剩下的交给规则"
                        : "修改后会按本月已记录的流水重新计算",
                11.5f, t.text3, t.regular);
        sub.setPadding(0, Ui.dp(main, 4), 0, 0);
        content.addView(sub);

        // Total funds field.
        TextView totalLabel = fieldLabel(main, "总资金 · " + ym.getYear() + "年" + ym.getMonthValue() + "月");
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        flp.setMargins(0, Ui.dp(main, 16), 0, Ui.dp(main, 5));
        totalLabel.setLayoutParams(flp);
        content.addView(totalLabel);

        EditText totalEdit = moneyEdit(main, t, "0.00");
        totalEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(12)});
        if (config.total > 0) totalEdit.setText(Ui.plain(config.total));
        content.addView(moneyField(main, t, totalEdit, "¥", null));

        TextView modeLabel = fieldLabel(main, "自由资金怎么分");
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mlp.setMargins(0, Ui.dp(main, 16), 0, Ui.dp(main, 6));
        modeLabel.setLayoutParams(mlp);
        content.addView(modeLabel);

        SegmentedControl mode = new SegmentedControl(main, t,
                new String[]{"按比例", "按每日预算"}, initialMode);
        content.addView(mode);

        // Ratio panel.
        LinearLayout ratioPanel = Ui.vertical(main);
        ratioPanel.setPadding(0, Ui.dp(main, 12), 0, 0);

        LinearLayout ratioHead = Ui.horizontal(main);
        TextView ratioLabel = Ui.text(main, "自由资金比例", 12, t.text2, t.medium);
        ratioHead.addView(ratioLabel);
        View rsp = new View(main);
        ratioHead.addView(rsp, new LinearLayout.LayoutParams(0, 1, 1f));
        TextView ratioValue = Ui.text(main, Math.round(config.ratio) + "%", 16, t.accentDeep, t.medium);
        ratioHead.addView(ratioValue);
        ratioPanel.addView(ratioHead);

        SeekBar seek = new SeekBar(main);
        seek.setMax(90);
        int initialRatio = (int) Math.max(0, Math.min(90, Math.round(config.ratio)));
        seek.setProgress(initialRatio);
        seek.setProgressTintList(android.content.res.ColorStateList.valueOf(t.accent));
        seek.setThumbTintList(android.content.res.ColorStateList.valueOf(t.accent));
        seek.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(t.line));
        seek.setPadding(Ui.dp(main, 2), Ui.dp(main, 6), Ui.dp(main, 2), Ui.dp(main, 2));
        ratioPanel.addView(seek);

        LinearLayout ratioChips = Ui.horizontal(main);
        int[] presets = {5, 10, 15, 20, 30};
        TextView[] chipViews = new TextView[presets.length];
        for (int i = 0; i < presets.length; i++) {
            final int value = presets[i];
            TextView chip = Ui.chip(main, t, value + "%", initialRatio == value);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            clp.setMargins(i == 0 ? 0 : Ui.dp(main, 6), Ui.dp(main, 4), 0, 0);
            chip.setLayoutParams(clp);
            chip.setOnClickListener(v -> {
                Ui.haptic(v);
                seek.setProgress(value);
            });
            chipViews[i] = chip;
            ratioChips.addView(chip);
        }
        ratioPanel.addView(ratioChips);
        content.addView(ratioPanel);

        // Daily override panel.
        LinearLayout dailyPanel = Ui.vertical(main);
        dailyPanel.setPadding(0, Ui.dp(main, 12), 0, 0);
        String hintText = "总资金 - 生效天数 × 每日预支出 = 自由资金，优先于比例";
        if (startDay > 1) {
            hintText += "\n本月从 " + startDay + " 日起算，共 " + effectiveDays + " 天";
        }
        TextView dailyHint = Ui.text(main, hintText, 10.5f, t.text3, t.regular);
        dailyHint.setLineSpacing(0, 1.2f);
        dailyPanel.addView(dailyHint);
        TextView dailyLabel = fieldLabel(main, "每日预支出");
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dlp.setMargins(0, Ui.dp(main, 10), 0, Ui.dp(main, 5));
        dailyLabel.setLayoutParams(dlp);
        dailyPanel.addView(dailyLabel);

        EditText dailyEdit = moneyEdit(main, t, "0.00");
        dailyEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});
        double initialDaily;
        if (config.configured) {
            initialDaily = config.dailyMode ? config.dailyAmount : summary.baseDaily;
        } else {
            initialDaily = config.total > 0
                    ? config.total * (1 - config.ratio / 100.0) / effectiveDays
                    : 0;
        }
        if (initialDaily > 0.004) dailyEdit.setText(Ui.plain(initialDaily));
        dailyPanel.addView(moneyField(main, t, dailyEdit, "¥", "/ 天"));
        content.addView(dailyPanel);

        // Carry info.
        if (config.carryIn > 0.004) {
            LinearLayout carry = Ui.horizontal(main);
            carry.setPadding(0, Ui.dp(main, 10), 0, 0);
            TextView carryText = Ui.text(main, "含上月结转 " + Ui.money(config.carryIn),
                    10.5f, t.accentDeep, t.medium);
            carry.addView(carryText);
            content.addView(carry);
        }

        // Live preview.
        LinearLayout preview = Ui.vertical(main);
        preview.setBackground(Ui.round(main, t.surface2, 16));
        preview.setPadding(Ui.dp(main, 14), Ui.dp(main, 12), Ui.dp(main, 14), Ui.dp(main, 12));
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        plp.setMargins(0, Ui.dp(main, 14), 0, 0);
        preview.setLayoutParams(plp);

        TextView previewTitle = Ui.text(main, "这样分配 · 按 " + effectiveDays + " 天",
                10.5f, t.text3, t.medium);
        preview.addView(previewTitle);
        TextView freePreview = previewRow(main, "自由资金", "¥0.00", t.accentDeep);
        TextView dailyPreview = previewRow(main, "每日预资金", "¥0.00", t.text);
        preview.addView(freePreview);
        preview.addView(dailyPreview);
        TextView error = Ui.text(main, "", 10.5f, t.red, t.medium);
        error.setVisibility(View.GONE);
        error.setPadding(0, Ui.dp(main, 7), 0, 0);
        preview.addView(error);
        content.addView(preview);

        TextView save = Ui.text(main, firstSetup ? "保存并开始" : "保存预算",
                13.5f, t.accentOn, t.medium);
        save.setGravity(Gravity.CENTER);
        save.setBackground(Ui.ripple(main, t.accent, 14, Ui.alpha(Color.WHITE, 45)));
        save.setClickable(true);
        save.setFocusable(true);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(main, 50));
        slp.setMargins(0, Ui.dp(main, 16), 0, 0);
        save.setLayoutParams(slp);
        content.addView(save);

        final int[] currentRatio = {initialRatio};
        final int[] currentMode = {initialMode};

        Runnable refreshMode = () -> {
            boolean daily = currentMode[0] == 1;
            ratioPanel.setVisibility(daily ? View.GONE : View.VISIBLE);
            dailyPanel.setVisibility(daily ? View.VISIBLE : View.GONE);
        };
        refreshMode.run();

        Runnable updatePreview = () -> {
            double total = Ui.parseMoney(totalEdit.getText().toString());
            double carry = config.carryIn;
            double freeBase;
            double daily;
            boolean dailyMode = currentMode[0] == 1;
            String err = null;
            if (dailyMode) {
                boolean blank = TextUtils.isEmpty(dailyEdit.getText().toString().trim());
                double inputDaily = Ui.parseMoney(dailyEdit.getText().toString());
                freeBase = total - effectiveDays * inputDaily;
                daily = inputDaily;
                if (blank) {
                    err = "请输入每日预支出";
                } else if (freeBase < -0.005) {
                    err = "自由资金会变成负数，请调低每日预支出或提高总资金";
                }
            } else {
                freeBase = total * currentRatio[0] / 100.0;
                daily = (total - freeBase) / effectiveDays;
            }
            if (total <= 0 && err == null) err = "总资金需要大于 0";
            double free = freeBase + carry;
            freePreview.setText("自由资金  " + Ui.money(Math.max(0, free)));
            dailyPreview.setText("每日预资金  " + Ui.money(Math.max(0, daily)));
            if (err != null) {
                error.setText(err);
                error.setVisibility(View.VISIBLE);
                preview.setBackground(Ui.round(main, t.redSoft, 16));
            } else {
                error.setVisibility(View.GONE);
                preview.setBackground(Ui.round(main, t.surface2, 16));
            }
            save.setAlpha(err == null ? 1f : 0.72f);
            save.setTag(err);
        };

        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                updatePreview.run();
            }
        };
        totalEdit.addTextChangedListener(watcher);
        dailyEdit.addTextChangedListener(watcher);

        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentRatio[0] = progress;
                ratioValue.setText(progress + "%");
                for (int i = 0; i < chipViews.length; i++) {
                    Ui.styleChip(chipViews[i], t, presets[i] == progress);
                }
                updatePreview.run();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        mode.setOnChangeListener(index -> {
            currentMode[0] = index;
            refreshMode.run();
            if (index == 1 && TextUtils.isEmpty(dailyEdit.getText().toString().trim())) {
                double base = summary.baseDaily;
                if (base <= 0.004) {
                    double total = Ui.parseMoney(totalEdit.getText().toString());
                    base = total > 0 ? total / effectiveDays : 0;
                }
                dailyEdit.setText(Ui.plain(base));
            }
            updatePreview.run();
        });

        save.setOnClickListener(v -> {
            Ui.hapticConfirm(v);
            String err = (String) save.getTag();
            if (err != null && !err.isEmpty()) {
                Ui.snack(main, err);
                return;
            }
            double total = Ui.parseMoney(totalEdit.getText().toString());
            if (total <= 0) {
                Ui.snack(main, "总资金需要大于 0");
                return;
            }
            boolean dailyMode = currentMode[0] == 1;
            double dailyAmount = 0;
            if (dailyMode) {
                if (TextUtils.isEmpty(dailyEdit.getText().toString().trim())) {
                    Ui.snack(main, "请输入每日预支出");
                    return;
                }
                dailyAmount = Ui.parseMoney(dailyEdit.getText().toString());
                if (total - effectiveDays * dailyAmount < -0.005) {
                    Ui.snack(main, "自由资金不能小于 0，请调低每日预支出");
                    return;
                }
            }
            config.total = total;
            config.ratio = currentRatio[0];
            config.dailyMode = dailyMode;
            config.dailyAmount = dailyMode ? dailyAmount : 0;
            if (!config.configured) config.startDay = startDay;
            config.configured = true;
            store.saveMonth(config);
            main.dismissSheet();
            main.onDataChanged();
            main.snackbar(firstSetup ? "预算设置好了，开始记账吧" : "预算已更新");
        });

        updatePreview.run();
        main.openSheet(content, 0.92f, null);
        // Deliberately do not force the soft keyboard open: the sheet already
        // contains the live preview and the save button, and auto-opening the
        // keyboard would push them under it on shorter phones.
    }

    private static TextView fieldLabel(MainActivity main, String text) {
        AppTheme t = main.colors();
        return Ui.text(main, text, 11, t.text3, t.medium);
    }

    private static EditText moneyEdit(MainActivity main, AppTheme t, String hint) {
        EditText edit = new EditText(main);
        edit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        edit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(12)});
        edit.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 21);
        edit.setTypeface(t.medium);
        edit.setTextColor(t.text);
        edit.setHintTextColor(t.text3);
        edit.setHint(hint);
        edit.setSingleLine(true);
        edit.setBackground(null);
        edit.setPadding(0, Ui.dp(main, 10), 0, Ui.dp(main, 10));
        edit.setIncludeFontPadding(false);
        edit.setImeOptions(EditorInfo.IME_ACTION_DONE);
        edit.setSelectAllOnFocus(true);
        return edit;
    }

    private static LinearLayout moneyField(MainActivity main, AppTheme t, EditText edit,
                                           String prefix, String suffix) {
        LinearLayout wrap = Ui.horizontal(main);
        wrap.setBackground(Ui.roundStroke(main, t.surface2, 14, t.line, 1));
        wrap.setPadding(Ui.dp(main, 14), 0, Ui.dp(main, 14), 0);
        TextView pre = Ui.text(main, prefix, 18, t.text3, t.medium);
        wrap.addView(pre);
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        elp.setMargins(Ui.dp(main, 8), 0, 0, 0);
        edit.setLayoutParams(elp);
        wrap.addView(edit);
        if (suffix != null) {
            TextView suf = Ui.text(main, suffix, 11.5f, t.text3, t.regular);
            wrap.addView(suf);
        }
        LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wrap.setLayoutParams(wlp);
        return wrap;
    }

    private static TextView previewRow(MainActivity main, String label, String value, int color) {
        AppTheme t = main.colors();
        TextView rowText = Ui.text(main, label + "  " + value, 12, color, t.medium);
        rowText.setPadding(0, Ui.dp(main, 6), 0, 0);
        return rowText;
    }
}
