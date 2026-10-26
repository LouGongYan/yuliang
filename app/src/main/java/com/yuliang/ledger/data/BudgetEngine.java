package com.yuliang.ledger.data;

import com.yuliang.ledger.data.Models.MonthConfig;
import com.yuliang.ledger.data.Models.Summary;
import com.yuliang.ledger.data.Models.Tx;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Replays a month's transactions against its budget rules.
 *
 * The engine is intentionally pure Java (no Android classes) so the money rules
 * can be unit tested on a normal JVM.
 *
 * Rules implemented:
 *  - free funds = total * ratio (default 10%), or total - days * dailyOverride
 *    when the user prefers a fixed daily allowance;
 *  - daily allowance = (total - base free) / days in month;
 *  - every finished day: unused daily allowance rolls into free funds, an
 *    overspend beyond the daily allowance is paid from free funds;
 *  - if free funds and the daily allowance are both exhausted, the expense is
 *    paid from the remaining funds and the rest is spread over the remaining
 *    days;
 *  - income increases both remaining funds and free funds;
 *  - last month's positive remaining can be carried into this month's free funds.
 */
public final class BudgetEngine {

    private static final double EPS = 0.005;

    private BudgetEngine() {
    }

    private static final class Day {
        double income;
        double expense;
    }

    public static Summary compute(MonthConfig config, List<Tx> txs, LocalDate today) {
        Summary s = new Summary();
        if (config == null) {
            return s;
        }
        YearMonth ym;
        try {
            ym = YearMonth.parse(config.month);
        } catch (Exception e) {
            return s;
        }

        int days = ym.lengthOfMonth();
        LocalDate first = ym.atDay(1);
        LocalDate last = ym.atEndOfMonth();
        YearMonth nowMonth = YearMonth.from(today);
        boolean current = ym.equals(nowMonth);
        boolean future = ym.isAfter(nowMonth);

        s.month = config.month;
        s.configured = config.configured;
        s.currentMonth = current;
        s.total = Math.max(0, config.total);
        s.carryIn = Math.max(0, config.carryIn);
        s.ratio = clamp(config.ratio, 0, 100);
        s.dailyMode = config.dailyMode;
        int startDay = Math.max(1, Math.min(days, config.startDay));
        if (current) startDay = Math.min(startDay, today.getDayOfMonth());
        s.startDay = startDay;
        s.effectiveDays = Math.max(1, days - startDay + 1);
        s.daysInMonth = days;
        s.dayOfMonth = current ? today.getDayOfMonth() : days;
        s.daysLeft = current ? Math.max(1, days - today.getDayOfMonth() + 1) : 0;

        Map<LocalDate, Day> byDay = new HashMap<>();
        double incomeTotal = 0;
        double expenseTotal = 0;
        int count = 0;
        if (txs != null) {
            for (Tx tx : txs) {
                if (tx == null || tx.date == null || !tx.date.startsWith(config.month)) {
                    continue;
                }
                LocalDate date;
                try {
                    date = LocalDate.parse(tx.date);
                } catch (Exception e) {
                    continue;
                }
                double amount = Math.max(0, tx.amount);
                Day day = byDay.get(date);
                if (day == null) {
                    day = new Day();
                    byDay.put(date, day);
                }
                if (tx.isIncome()) {
                    day.income += amount;
                    incomeTotal += amount;
                } else {
                    day.expense += amount;
                    expenseTotal += amount;
                }
                count++;
            }
        }
        s.income = incomeTotal;
        s.expense = expenseTotal;
        s.txCount = count;
        s.overBudget = false;

        double baseFree;
        double baseDaily;
        if (config.dailyMode) {
            baseFree = s.total - s.effectiveDays * Math.max(0, config.dailyAmount);
            baseDaily = Math.max(0, config.dailyAmount);
            if (baseFree < 0) {
                // Defensive fallback for an invalid/old configuration.
                baseFree = 0;
                baseDaily = s.total / s.effectiveDays;
            }
        } else {
            baseFree = s.total * s.ratio / 100.0;
            if (baseFree < 0) baseFree = 0;
            if (baseFree > s.total) baseFree = s.total;
            baseDaily = (s.total - baseFree) / s.effectiveDays;
        }
        s.baseFree = baseFree;
        s.initialFree = baseFree + s.carryIn;
        s.baseDaily = Math.max(0, baseDaily);

        double free = s.initialFree;
        double daily = s.baseDaily;
        boolean rebalanced = false;

        LocalDate settleEnd;
        if (current) {
            settleEnd = today.minusDays(1);
        } else if (future) {
            settleEnd = first.minusDays(1);
        } else {
            settleEnd = last;
        }

        double incomeSoFar = 0;
        double expenseSoFar = 0;
        LocalDate budgetStart = first.plusDays(startDay - 1L);

        // Transactions dated before the budget period still count against the
        // real remaining money; they are paid from free funds (no daily credit
        // for days on which the budget was not active).
        for (LocalDate d = first; d.isBefore(budgetStart); d = d.plusDays(1)) {
            Day day = byDay.get(d);
            double income = day == null ? 0 : day.income;
            double expense = day == null ? 0 : day.expense;
            incomeSoFar += income;
            expenseSoFar += expense;
            free += income;
            if (expense > 0) {
                free -= Math.min(expense, Math.max(0, free));
                if (free < 0) free = 0;
            }
        }
        if (startDay > 1) {
            double remainingAtStart = s.total + s.carryIn + incomeSoFar - expenseSoFar;
            free = Math.min(free, Math.max(0, remainingAtStart));
            daily = Math.max(0, (remainingAtStart - free) / s.effectiveDays);
        }

        for (LocalDate d = budgetStart; !d.isAfter(settleEnd); d = d.plusDays(1)) {
            Day day = byDay.get(d);
            double income = day == null ? 0 : day.income;
            double expense = day == null ? 0 : day.expense;

            incomeSoFar += income;
            expenseSoFar += expense;
            free += income;

            if (expense <= daily + EPS) {
                // What was not used today becomes available as free funds.
                free += daily - expense;
            } else {
                double overflow = expense - daily;
                double fromFree = Math.min(overflow, Math.max(0, free));
                free -= fromFree;
                if (free <= EPS) {
                    free = 0;
                    rebalanced = true;
                    int daysAfter = days - d.getDayOfMonth();
                    double remainingAfter = s.total + s.carryIn + incomeSoFar - expenseSoFar;
                    daily = daysAfter > 0 ? Math.max(0, remainingAfter / daysAfter) : 0;
                }
            }
            if (free < 0) free = 0;
        }

        s.dailyAllowance = Math.max(0, daily);
        s.dailyFuture = Math.max(0, daily);

        if (current) {
            Day day = byDay.get(today);
            s.todayIncome = day == null ? 0 : day.income;
            s.todaySpent = day == null ? 0 : day.expense;
            free += s.todayIncome;

            s.todayAllowance = Math.max(0, daily);
            if (s.todaySpent <= daily + EPS) {
                s.todayRemaining = Math.max(0, daily - s.todaySpent);
            } else {
                s.todayRemaining = 0;
                double overflow = s.todaySpent - daily;
                double fromFree = Math.min(overflow, Math.max(0, free));
                free -= fromFree;
                s.todayFromFree = fromFree;
                overflow -= fromFree;
                if (overflow > EPS) {
                    s.todayFromRemaining = overflow;
                }
                if (free <= EPS && s.todaySpent >= daily - EPS) {
                    free = 0;
                    rebalanced = true;
                    int daysAfter = days - today.getDayOfMonth();
                    double incomeUpToToday = incomeSoFar + s.todayIncome;
                    double expenseUpToToday = expenseSoFar + s.todaySpent;
                    double remainingAfter = s.total + s.carryIn + incomeUpToToday - expenseUpToToday;
                    s.dailyFuture = daysAfter > 0 ? Math.max(0, remainingAfter / daysAfter) : 0;
                }
            }
        } else {
            s.todaySpent = 0;
            s.todayIncome = 0;
            s.todayAllowance = 0;
            s.todayRemaining = 0;
        }

        if (free < 0) free = 0;
        s.free = free;
        s.rebalanced = rebalanced;
        s.remaining = s.total + s.carryIn + incomeTotal - expenseTotal;
        s.overBudget = s.remaining < -EPS;
        s.spentRatio = s.total > 0 ? expenseTotal / s.total : (expenseTotal > 0 ? 1 : 0);
        int elapsed = current
                ? Math.max(1, today.getDayOfMonth() - startDay + 1)
                : s.effectiveDays;
        s.avgDaily = elapsed > 0 ? expenseTotal / elapsed : 0;
        return s;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
