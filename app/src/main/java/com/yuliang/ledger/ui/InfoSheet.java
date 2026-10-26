package com.yuliang.ledger.ui;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yuliang.ledger.MainActivity;
import com.yuliang.ledger.data.Models;

/** Rule explainers and the money breakdown, shown in a bottom sheet. */
public final class InfoSheet {

    private InfoSheet() {
    }

    public static void showRules(MainActivity main) {
        AppTheme t = main.colors();
        LinearLayout content = Ui.vertical(main);
        content.setPadding(Ui.dp(main, 22), 0, Ui.dp(main, 22), Ui.dp(main, 16));

        TextView title = Ui.text(main, "钱是怎么算的", 21, t.text, t.medium);
        content.addView(title);
        TextView sub = Ui.text(main, "每个数字都由下面这几条规则推出来", 11.5f, t.text3, t.regular);
        sub.setPadding(0, Ui.dp(main, 4), 0, 0);
        content.addView(sub);

        LinearLayout formula = Ui.vertical(main);
        formula.setBackground(Ui.round(main, t.accentSoft, 18));
        formula.setPadding(Ui.dp(main, 16), Ui.dp(main, 14), Ui.dp(main, 16), Ui.dp(main, 14));
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        flp.setMargins(0, Ui.dp(main, 16), 0, Ui.dp(main, 6));
        formula.setLayoutParams(flp);
        TextView f1 = Ui.text(main, "自由资金 = 总资金 × 比例 + 上月结转", 12, t.accentDeep, t.medium);
        TextView f2 = Ui.text(main, "每日预资金 = (总资金 - 自由资金) ÷ 生效天数", 12, t.accentDeep, t.medium);
        f2.setPadding(0, Ui.dp(main, 6), 0, 0);
        TextView f3 = Ui.text(main, "整月设置时，生效天数就是本月天数；月中设置则从当天算到月底。",
                10.5f, t.accentDeep, t.regular);
        f3.setLineSpacing(0, 1.2f);
        f3.setPadding(0, Ui.dp(main, 6), 0, 0);
        formula.addView(f1);
        formula.addView(f2);
        formula.addView(f3);
        content.addView(formula);

        addRule(main, content, "1", "每天先用当日预资金",
                "每笔支出先扣当天的每日预资金，首页会实时显示今天还剩多少。");
        addRule(main, content, "2", "超支由自由资金兜底",
                "当天花超了，超出的部分自动从自由资金扣；自由资金也会随收入增加。");
        addRule(main, content, "3", "没花完的进入自由资金",
                "当天结束时，每日预资金没用完的部分会自动转入自由资金，可以留着以后花。");
        addRule(main, content, "4", "两边都空了，再从剩余资金扣",
                "自由资金和当日预资金都扣完时，会从剩余资金里扣，并把剩下的钱重新平均到本月剩下的天。");
        addRule(main, content, "5", "收入同时进入两个口袋",
                "收入会同时增加剩余资金和自由资金，所以月中收到一笔钱，后面会更从容。");
        addRule(main, content, "6", "上月剩余可以继承",
                "新月份会把上一月的正剩余结转成本月自由资金的一部分；在设置里可以关闭或重新结转。");
        addRule(main, content, "7", "每日预支出优先于比例",
                "如果你设定了每日预支出，自由资金 = 总资金 - 生效天数 × 每日预支出，这个设置优先于比例；算出来小于 0 时不能保存。");

        TextView note = Ui.text(main,
                "提醒：总资金是你为这个月设定的预算上限，结转和收入会让实际可用的剩余资金更多。",
                10.5f, t.text3, t.regular);
        note.setLineSpacing(0, 1.25f);
        note.setPadding(0, Ui.dp(main, 14), 0, Ui.dp(main, 14));
        content.addView(note);

        TextView done = Ui.text(main, "知道了", 13.5f, t.accentOn, t.medium);
        done.setGravity(Gravity.CENTER);
        done.setBackground(Ui.ripple(main, t.accent, 14, Ui.alpha(Color.WHITE, 45)));
        done.setClickable(true);
        done.setFocusable(true);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(main, 48));
        done.setLayoutParams(dlp);
        done.setOnClickListener(v -> {
            Ui.haptic(v);
            main.dismissSheet();
        });
        content.addView(done);

        main.openSheet(content, 0.9f, null);
    }

    private static void addRule(MainActivity main, LinearLayout parent, String number,
                                String title, String body) {
        AppTheme t = main.colors();
        LinearLayout row = Ui.horizontal(main);
        row.setGravity(Gravity.TOP);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.setMargins(0, Ui.dp(main, 14), 0, 0);
        row.setLayoutParams(rlp);

        TextView num = Ui.text(main, number, 11, t.accent, t.medium);
        num.setGravity(Gravity.CENTER);
        num.setBackground(Ui.round(main, t.accentSoft, 50));
        num.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(main, 24), Ui.dp(main, 24)));
        row.addView(num);

        LinearLayout texts = Ui.vertical(main);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tlp.setMargins(Ui.dp(main, 11), 0, 0, 0);
        texts.setLayoutParams(tlp);
        TextView titleView = Ui.text(main, title, 13, t.text, t.medium);
        TextView bodyView = Ui.text(main, body, 11, t.text2, t.regular);
        bodyView.setLineSpacing(0, 1.22f);
        bodyView.setPadding(0, Ui.dp(main, 3), 0, 0);
        texts.addView(titleView);
        texts.addView(bodyView);
        row.addView(texts);
        parent.addView(row);
    }

    public static void showFunds(MainActivity main) {
        AppTheme t = main.colors();
        Models.Summary s = main.summary();

        LinearLayout content = Ui.vertical(main);
        content.setPadding(Ui.dp(main, 22), 0, Ui.dp(main, 22), Ui.dp(main, 18));

        TextView title = Ui.text(main, "资金构成", 21, t.text, t.medium);
        content.addView(title);
        TextView sub = Ui.text(main, Ui.month(main.currentMonth()) + " · 每一块钱都在这里", 11.5f, t.text3, t.regular);
        sub.setPadding(0, Ui.dp(main, 4), 0, 0);
        content.addView(sub);

        LinearLayout card = Ui.card(main, t);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.setMargins(0, Ui.dp(main, 16), 0, 0);
        card.setLayoutParams(clp);
        card.addView(breakdownRow(main, "本月总资金", Ui.money(s.total), t.text, t.text2, false));
        if (s.carryIn > 0.004) {
            card.addView(breakdownRow(main, "上月结转", "+" + Ui.money(s.carryIn), t.text, t.text2, false));
        }
        card.addView(breakdownRow(main, "本月收入", "+" + Ui.money(s.income), t.green, t.text2, false));
        card.addView(breakdownRow(main, "本月支出", "-" + Ui.money(s.expense), t.text, t.text2, false));
        View div = Ui.divider(main, t);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, Ui.dp(main, 0.8f)));
        dlp.setMargins(0, Ui.dp(main, 6), 0, Ui.dp(main, 6));
        div.setLayoutParams(dlp);
        card.addView(div);
        card.addView(breakdownRow(main, s.remaining < 0 ? "超出资金" : "剩余资金",
                (s.remaining >= 0 ? "" : "-") + Ui.money(Math.abs(s.remaining)),
                s.remaining < 0 ? t.red : t.accentDeep, t.text, true));
        card.addView(breakdownRow(main, "其中自由资金", Ui.money(s.free),
                t.text, t.text3, false));
        content.addView(card);

        LinearLayout explain = Ui.vertical(main);
        explain.setBackground(Ui.round(main, t.surface2, 16));
        explain.setPadding(Ui.dp(main, 14), Ui.dp(main, 12), Ui.dp(main, 14), Ui.dp(main, 12));
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        elp.setMargins(0, Ui.dp(main, 12), 0, 0);
        explain.setLayoutParams(elp);
        TextView e1 = Ui.text(main,
                "剩余资金 = 总资金 + 上月结转 + 收入 - 支出，是这个月真正还能用的钱。",
                11, t.text2, t.regular);
        e1.setLineSpacing(0, 1.25f);
        TextView e2 = Ui.text(main,
                "自由资金是剩余资金里可以灵活支配的一部分，用来兜住每日预资金的超支。",
                11, t.text2, t.regular);
        e2.setLineSpacing(0, 1.25f);
        e2.setPadding(0, Ui.dp(main, 7), 0, 0);
        explain.addView(e1);
        explain.addView(e2);
        content.addView(explain);

        TextView close = Ui.text(main, "知道了", 13.5f, t.accentOn, t.medium);
        close.setGravity(Gravity.CENTER);
        close.setBackground(Ui.ripple(main, t.accent, 14, Ui.alpha(Color.WHITE, 45)));
        close.setClickable(true);
        close.setFocusable(true);
        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(main, 48));
        closeLp.setMargins(0, Ui.dp(main, 16), 0, 0);
        close.setLayoutParams(closeLp);
        close.setOnClickListener(v -> {
            Ui.haptic(v);
            main.dismissSheet();
        });
        content.addView(close);

        main.openSheet(content, 0.85f, null);
    }

    private static View breakdownRow(MainActivity main, String label, String value,
                                     int valueColor, int labelColor, boolean strong) {
        AppTheme t = main.colors();
        LinearLayout row = Ui.horizontal(main);
        row.setPadding(0, Ui.dp(main, strong ? 5 : 4), 0, Ui.dp(main, strong ? 5 : 4));
        TextView l = Ui.text(main, label, strong ? 12.5f : 12, labelColor,
                strong ? t.medium : t.regular);
        row.addView(l);
        View sp = new View(main);
        row.addView(sp, new LinearLayout.LayoutParams(0, 1, 1f));
        TextView v = Ui.text(main, value, strong ? 18 : 12.5f, valueColor,
                strong ? t.medium : t.regular);
        v.setGravity(Gravity.END);
        row.addView(v);
        return row;
    }
}
