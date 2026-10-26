package com.yuliang.ledger.ui;

import android.app.AlertDialog;
import android.graphics.Color;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yuliang.ledger.MainActivity;
import com.yuliang.ledger.R;
import com.yuliang.ledger.data.LedgerStore;
import com.yuliang.ledger.data.Models;

import java.util.List;

/** Add, rename and delete the user's own category labels. */
public final class CategorySheet {

    private CategorySheet() {
    }

    public static void show(MainActivity main, String type) {
        AppTheme t = main.colors();
        LedgerStore store = main.store();
        boolean income = Models.INCOME.equals(type);
        String titleText = income ? "收入标签" : "支出标签";

        LinearLayout content = Ui.vertical(main);
        content.setPadding(Ui.dp(main, 22), 0, Ui.dp(main, 22), Ui.dp(main, 16));

        LinearLayout titleRow = Ui.horizontal(main);
        TextView title = Ui.text(main, titleText, 21, t.text, t.medium);
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
                income ? "收入会计入剩余资金与自由资金" : "标签可以自己新增、改名，删除后原记录归到「其他」",
                11, t.text3, t.regular);
        sub.setLineSpacing(0, 1.2f);
        sub.setPadding(0, Ui.dp(main, 4), 0, Ui.dp(main, 14));
        content.addView(sub);

        LinearLayout listCard = Ui.vertical(main);
        listCard.setBackground(Ui.roundStroke(main, t.surface, 20, t.line, 1));
        listCard.setPadding(Ui.dp(main, 4), Ui.dp(main, 4), Ui.dp(main, 4), Ui.dp(main, 4));
        content.addView(listCard);

        final Runnable[] rebuild = new Runnable[1];
        rebuild[0] = () -> {
            listCard.removeAllViews();
            List<String> cats = store.categories(type);
            for (int i = 0; i < cats.size(); i++) {
                String name = cats.get(i);
                int usage = store.categoryUsage(type, name);
                listCard.addView(categoryRow(main, t, type, name, usage, rebuild[0]));
                if (i < cats.size() - 1) listCard.addView(Ui.dividerInset(main, t, 58));
            }
        };
        rebuild[0].run();

        TextView add = Ui.text(main, "＋ 新增标签", 12.5f, t.accentDeep, t.medium);
        add.setGravity(Gravity.CENTER);
        add.setBackground(Ui.rippleStroke(main, t.accentSoft, 14,
                Ui.alpha(t.accent, 110), 1, Ui.alpha(t.accent, 30)));
        add.setClickable(true);
        add.setFocusable(true);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(main, 46));
        alp.setMargins(0, Ui.dp(main, 12), 0, 0);
        add.setLayoutParams(alp);
        add.setOnClickListener(v -> {
            Ui.haptic(v);
            showInput(main, type, null, rebuild[0]);
        });
        content.addView(add);

        main.openSheet(content, 0.85f, null);
    }

    private static View categoryRow(MainActivity main, AppTheme t, String type,
                                    String name, int usage, Runnable onChanged) {
        LinearLayout row = Ui.horizontal(main);
        row.setPadding(Ui.dp(main, 10), Ui.dp(main, 9), Ui.dp(main, 8), Ui.dp(main, 9));
        row.setMinimumHeight(Ui.dp(main, 54));
        row.setBackground(Ui.ripple(main, Color.TRANSPARENT, 14, Ui.alpha(t.accent, 22)));
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(v -> {
            Ui.haptic(v);
            showInput(main, type, name, onChanged);
        });

        int color = Models.INCOME.equals(type) ? t.green : Ui.categoryColor(t, name);
        FrameLayout badge = new FrameLayout(main);
        badge.setBackground(Ui.round(main, Ui.alpha(color, t.dark ? 48 : 30), 50));
        badge.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(main, 34), Ui.dp(main, 34)));
        TextView ch = Ui.text(main, Ui.categoryChar(name), 12.5f, color, t.medium);
        ch.setGravity(Gravity.CENTER);
        ch.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        badge.addView(ch);
        row.addView(badge);

        LinearLayout texts = Ui.vertical(main);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tlp.setMargins(Ui.dp(main, 11), 0, 0, 0);
        texts.setLayoutParams(tlp);
        TextView nameView = Ui.text(main, name, 13, t.text, t.medium);
        TextView count = Ui.text(main, usage > 0 ? usage + " 笔记录" : "还没有使用",
                10, t.text3, t.regular);
        count.setPadding(0, Ui.dp(main, 2), 0, 0);
        texts.addView(nameView);
        texts.addView(count);
        row.addView(texts);

        FrameLayout edit = smallIconButton(main, t, R.drawable.ic_edit, t.text3);
        edit.setContentDescription("重命名 " + name);
        edit.setOnClickListener(v -> {
            Ui.haptic(v);
            showInput(main, type, name, onChanged);
        });
        row.addView(edit);

        if (!"其他".equals(name)) {
            FrameLayout del = smallIconButton(main, t, R.drawable.ic_trash, t.red);
            del.setContentDescription("删除 " + name);
            del.setOnClickListener(v -> {
                Ui.haptic(v);
                confirmDelete(main, type, name, usage, onChanged);
            });
            row.addView(del);
        }
        return row;
    }

    private static FrameLayout smallIconButton(MainActivity main, AppTheme t, int res, int tint) {
        FrameLayout holder = new FrameLayout(main);
        holder.setBackground(Ui.ripple(main, Color.TRANSPARENT, 50, Ui.alpha(t.accent, 30)));
        holder.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(main, 34), Ui.dp(main, 34)));
        holder.setClickable(true);
        holder.setFocusable(true);
        android.widget.ImageView icon = Ui.icon(main, res, 17, tint);
        FrameLayout.LayoutParams ilp = new FrameLayout.LayoutParams(Ui.dp(main, 17), Ui.dp(main, 17));
        ilp.gravity = Gravity.CENTER;
        icon.setLayoutParams(ilp);
        holder.addView(icon);
        return holder;
    }

    private static void showInput(MainActivity main, String type, String oldName, Runnable onChanged) {
        AppTheme t = main.colors();
        EditText input = new EditText(main);
        input.setSingleLine(true);
        input.setText(oldName == null ? "" : oldName);
        input.setHint("标签名，最多 12 个字");
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
                .setTitle(oldName == null
                        ? (Models.INCOME.equals(type) ? "新增收入标签" : "新增支出标签")
                        : "重命名标签")
                .setView(wrap)
                .setNegativeButton("取消", null)
                .setPositiveButton(oldName == null ? "添加" : "保存", null)
                .create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = input.getText().toString().trim();
                if (name.isEmpty()) {
                    input.setError("请输入标签名");
                    return;
                }
                boolean ok;
                if (oldName == null) {
                    ok = main.store().addCategory(type, name);
                } else {
                    ok = main.store().renameCategory(type, oldName, name);
                }
                if (!ok) {
                    input.setError("标签已存在或数量已满");
                    return;
                }
                onChanged.run();
                main.onDataChanged();
                dialog.dismiss();
            });
            Ui.showKeyboard(input);
            if (oldName != null) input.setSelection(input.getText().length());
        });
        dialog.show();
    }

    private static void confirmDelete(MainActivity main, String type, String name,
                                      int usage, Runnable onChanged) {
        String message = usage > 0
                ? "这个标签下有 " + usage + " 笔记录，删除后会归到「其他」。"
                : "删除后不能再选这个标签。";
        new AlertDialog.Builder(main)
                .setTitle("删除「" + name + "」？")
                .setMessage(message)
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (d, w) -> {
                    if (main.store().deleteCategory(type, name)) {
                        onChanged.run();
                        main.onDataChanged();
                    }
                })
                .show();
    }
}
