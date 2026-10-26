import com.yuliang.ledger.data.BudgetEngine;
import com.yuliang.ledger.data.Models;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** JVM sanity checks for the money rules (not shipped with the APK). */
public class TestEngine {
    private static int checks = 0;

    public static void main(String[] args) {
        ratioNoSpend();
        dailyRollover();
        incomeAndCarry();
        dailyOverride();
        rebalanceFromRemaining();
        todayOverspendUsesFree();
        midMonthStart();
        System.out.println("All " + checks + " checks passed.");
    }

    private static Models.MonthConfig config(String month, double total, double ratio) {
        Models.MonthConfig c = new Models.MonthConfig();
        c.month = month;
        c.total = total;
        c.ratio = ratio;
        c.configured = true;
        return c;
    }

    private static Models.Tx tx(String date, double amount, String type) {
        Models.Tx t = new Models.Tx();
        t.date = date;
        t.amount = amount;
        t.type = type;
        t.category = "餐饮";
        t.createdAt = 1;
        return t;
    }

    private static void ratioNoSpend() {
        Models.MonthConfig c = config("2026-10", 3100, 10);
        Models.Summary s = BudgetEngine.compute(c, new ArrayList<>(), LocalDate.of(2026, 10, 26));
        eq(310, s.baseFree, "base free 10%");
        eq(90, s.baseDaily, "base daily");
        eq(2560, s.free, "25 completed days roll over");
        eq(90, s.todayAllowance, "today allowance");
        eq(3100, s.remaining, "remaining");
        eq(6, s.daysLeft, "days left");
        eq(0, s.todaySpent, "today spent");
    }

    private static void dailyRollover() {
        Models.MonthConfig c = config("2026-10", 3100, 10);
        List<Models.Tx> txs = new ArrayList<>();
        txs.add(tx("2026-10-01", 200, Models.EXPENSE));
        txs.add(tx("2026-10-02", 30, Models.EXPENSE));
        Models.Summary s = BudgetEngine.compute(c, txs, LocalDate.of(2026, 10, 3));
        // Day 1: 90 daily + 110 from free => free 200. Day 2: 30 used, 60 rolls in => free 260.
        eq(260, s.free, "daily rollover and overspend from free");
        eq(90, s.todayAllowance, "today allowance remains base");
        eq(230, s.expense, "expenses");
    }

    private static void incomeAndCarry() {
        Models.MonthConfig c = config("2026-10", 3100, 10);
        c.carryIn = 500;
        List<Models.Tx> txs = new ArrayList<>();
        txs.add(tx("2026-10-01", 100, Models.EXPENSE));
        txs.add(tx("2026-10-02", 200, Models.INCOME));
        Models.Summary s = BudgetEngine.compute(c, txs, LocalDate.of(2026, 10, 3));
        // initial 810; day1 daily 90 covers 100 (overflow 10 from free), unused 0 => 800
        // day2 income +200, no expense, unused 90 => 1090
        eq(1090, s.free, "income and carry reach free funds");
        eq(3700, s.remaining, "remaining includes carry and income");
        eq(200, s.income, "income total");
    }

    private static void dailyOverride() {
        Models.MonthConfig c = new Models.MonthConfig();
        c.month = "2026-10";
        c.total = 3000;
        c.dailyMode = true;
        c.dailyAmount = 80;
        c.configured = true;
        Models.Summary s = BudgetEngine.compute(c, new ArrayList<>(), LocalDate.of(2026, 10, 26));
        eq(520, s.baseFree, "daily override free = total - days*daily");
        eq(80, s.baseDaily, "daily override amount");
        eq(2520, s.free, "daily override rollover");
    }

    private static void rebalanceFromRemaining() {
        Models.MonthConfig c = config("2026-10", 3000, 10);
        List<Models.Tx> txs = new ArrayList<>();
        txs.add(tx("2026-10-01", 400, Models.EXPENSE));
        Models.Summary s = BudgetEngine.compute(c, txs, LocalDate.of(2026, 10, 2));
        // Day 1: 90 daily + 300 free + 10 remaining. Re-spread 2600 over 30 days.
        eq(0, s.free, "free exhausted");
        eq(2600, s.remaining, "remaining after rule 4");
        check(Math.abs(s.todayAllowance - 2600.0 / 30.0) < 0.01,
                "daily rebalanced to remaining days: " + s.todayAllowance);
        check(s.rebalanced, "rebalance flag");
    }

    private static void todayOverspendUsesFree() {
        Models.MonthConfig c = config("2026-10", 3000, 10);
        List<Models.Tx> txs = new ArrayList<>();
        txs.add(tx("2026-10-01", 200, Models.EXPENSE));
        Models.Summary s = BudgetEngine.compute(c, txs, LocalDate.of(2026, 10, 1));
        // Today, daily 87.0968 is used, the rest comes from free (300 -> 187.0968).
        eq(300 - (200 - 2700.0 / 31.0), s.free, "today overflow paid from free");
        eq(0, s.todayRemaining, "no daily left today");
        eq(200, s.todaySpent, "today spent");
        eq(2800, s.remaining, "remaining after overspend");
    }

    private static void midMonthStart() {
        Models.MonthConfig c = config("2026-10", 5000, 10);
        c.startDay = 26;
        Models.Summary s = BudgetEngine.compute(c, new ArrayList<>(), LocalDate.of(2026, 10, 26));
        eq(500, s.free, "mid-month start does not retroactively roll unused days");
        eq(750, s.baseDaily, "mid-month daily spreads over the six remaining days");
        eq(750, s.todayAllowance, "mid-month today allowance");
        eq(6, s.effectiveDays, "effective days");
        eq(5000, s.remaining, "mid-month remaining");

        Models.Tx past = tx("2026-10-20", 100, Models.EXPENSE);
        List<Models.Tx> txs = new ArrayList<>();
        txs.add(past);
        Models.Summary before = BudgetEngine.compute(c, txs, LocalDate.of(2026, 10, 26));
        check(before.free <= 400.005, "pre-budget expense is paid from free funds: " + before.free);
        check(before.remaining == 4900, "pre-budget expense still reduces remaining");
    }

    private static void eq(double expected, double actual, String what) {
        check(Math.abs(expected - actual) < 0.005,
                what + " expected=" + expected + " actual=" + actual);
    }

    private static void check(boolean condition, String what) {
        checks++;
        if (!condition) throw new AssertionError(what);
        System.out.println("ok " + checks + " - " + what);
    }
}
