package com.yuliang.ledger.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yuliang.ledger.data.Models;

/** One common transaction row, used on the home screen and in the records list. */
public final class TxRowView {

    private TxRowView() {
    }

    public static View build(Context c, AppTheme t, Models.Tx tx, View.OnClickListener click) {
        LinearLayout row = Ui.horizontal(c);
        row.setPadding(Ui.dp(c, 14), Ui.dp(c, 10), Ui.dp(c, 14), Ui.dp(c, 10));
        row.setMinimumHeight(Ui.dp(c, 62));
        row.setBackground(Ui.ripple(c, Color.TRANSPARENT, 12, Ui.alpha(t.accent, 22)));
        row.setClickable(true);
        row.setFocusable(true);
        if (click != null) row.setOnClickListener(click);

        int color = tx.isIncome() ? t.green : Ui.categoryColor(t, tx.category);
        FrameLayout badge = new FrameLayout(c);
        badge.setBackground(Ui.round(c, Ui.alpha(color, t.dark ? 48 : 30), 50));
        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(Ui.dp(c, 36), Ui.dp(c, 36));
        badge.setLayoutParams(badgeLp);
        TextView ch = Ui.text(c, Ui.categoryChar(tx.category), 14, color, t.medium);
        ch.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams chLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        ch.setLayoutParams(chLp);
        badge.addView(ch);
        row.addView(badge);

        LinearLayout texts = Ui.vertical(c);
        LinearLayout.LayoutParams textsLp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textsLp.setMargins(Ui.dp(c, 11), 0, Ui.dp(c, 10), 0);
        texts.setLayoutParams(textsLp);

        TextView title = Ui.text(c, tx.category == null || tx.category.isEmpty() ? "其他" : tx.category,
                13.5f, t.text, t.medium);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        texts.addView(title);

        StringBuilder meta = new StringBuilder();
        String time = Ui.time(tx.createdAt);
        if (!time.isEmpty()) meta.append(time);
        if (tx.note != null && !tx.note.isEmpty()) {
            if (meta.length() > 0) meta.append(" · ");
            meta.append(tx.note);
        }
        if (meta.length() == 0) meta.append(tx.isIncome() ? "收入" : "支出");
        TextView sub = Ui.text(c, meta.toString(), 11, t.text3, t.regular);
        sub.setSingleLine(true);
        sub.setEllipsize(android.text.TextUtils.TruncateAt.END);
        sub.setPadding(0, Ui.dp(c, 2), 0, 0);
        texts.addView(sub);
        row.addView(texts);

        TextView amount = Ui.text(c, Ui.signedMoney(tx), 14, tx.isIncome() ? t.green : t.text, t.medium);
        amount.setGravity(Gravity.END);
        amount.setSingleLine(true);
        row.addView(amount);
        return row;
    }
}
