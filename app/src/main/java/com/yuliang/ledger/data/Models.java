package com.yuliang.ledger.data;

import java.time.YearMonth;

/** Plain data objects shared by the store, the budget engine and the UI. */
public final class Models {

    public static final String EXPENSE = "expense";
    public static final String INCOME = "income";

    private Models() {
    }

    public static final class Tx {
        public long id;
        public String date = "";      // yyyy-MM-dd
        public long createdAt;
        public String type = EXPENSE;
        public double amount;
        public String category = "其他";
        public String note = "";

        public String month() {
            return date.length() >= 7 ? date.substring(0, 7) : "";
        }

        public boolean isIncome() {
            return INCOME.equals(type);
        }

        public Tx copy() {
            Tx t = new Tx();
            t.id = id;
            t.date = date;
            t.createdAt = createdAt;
            t.type = type;
            t.amount = amount;
            t.category = category;
            t.note = note;
            return t;
        }
    }

    public static final class MonthConfig {
        public String month = "";     // yyyy-MM
        public double total;
        public double ratio = 10;     // percent, used when !dailyMode
        public boolean dailyMode;     // daily override has priority over ratio
        public double dailyAmount;
        public double carryIn;        // last month's remaining, added to free funds
        public int startDay = 1;      // budget period starts on this day of the month
        public boolean configured;

        public int daysInMonth() {
            try {
                return YearMonth.parse(month).lengthOfMonth();
            } catch (Exception e) {
                return 30;
            }
        }

        public MonthConfig copy() {
            MonthConfig c = new MonthConfig();
            c.month = month;
            c.total = total;
            c.ratio = ratio;
            c.dailyMode = dailyMode;
            c.dailyAmount = dailyAmount;
            c.carryIn = carryIn;
            c.startDay = startDay;
            c.configured = configured;
            return c;
        }
    }

    /** Everything the home / records screens need for one month. */
    public static final class Summary {
        public String month = "";
        public boolean configured;
        public boolean currentMonth;

        public double total;
        public double carryIn;
        public double ratio;
        public boolean dailyMode;

        public double baseFree;        // free funds from this month's budget (不含结转)
        public double initialFree;     // baseFree + carryIn
        public double baseDaily;       // initial per-day allowance
        public double dailyAllowance;  // today's allowance (may have been rebalanced)
        public double dailyFuture;     // allowance for tomorrow if rebalanced
        public double free;            // current free funds
        public double remaining;       // total + carry + income - expense
        public double income;
        public double expense;
        public double spentRatio;
        public double avgDaily;

        public int daysInMonth;
        public int daysLeft;
        public int dayOfMonth;
        public int startDay;
        public int effectiveDays;

        public double todaySpent;
        public double todayIncome;
        public double todayAllowance;
        public double todayRemaining;
        public double todayFromFree;
        public double todayFromRemaining;

        public boolean rebalanced;
        public boolean overBudget;
        public int txCount;

        public boolean isCurrentMonth() {
            return currentMonth;
        }
    }
}
