package com.yuliang.ledger.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Local, offline storage for budgets, transactions and category labels.
 *
 * Everything lives in one SharedPreferences file. The payload is small (a few
 * hundred transactions a year), it is fully backed up by Android's Auto Backup,
 * and it keeps the app dependency-free and instant to open.
 */
public class LedgerStore {

    private static final String PREFS = "yuliang_ledger_v1";
    private static final String KEY_MONTHS = "months";
    private static final String KEY_TXS = "txs";
    private static final String KEY_CATS = "categories";
    private static final String KEY_SETTINGS = "settings";

    private static final String KEY_CARRY = "carryEnabled";
    private static final String KEY_ONBOARDED = "onboarded";

    private static final List<String> DEFAULT_EXPENSE_CATS = Arrays.asList(
            "餐饮", "日用", "交通", "购物", "娱乐", "住房", "医疗", "学习", "其他");
    private static final List<String> DEFAULT_INCOME_CATS = Arrays.asList(
            "工资", "兼职", "红包", "理财", "其他");

    private final SharedPreferences prefs;
    private final Map<String, Models.MonthConfig> months = new LinkedHashMap<>();
    private final List<Models.Tx> txs = new ArrayList<>();
    private final List<String> expenseCategories = new ArrayList<>();
    private final List<String> incomeCategories = new ArrayList<>();

    private boolean carryEnabled = true;
    private boolean onboarded = false;
    private long nextId = 1;

    public LedgerStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        load();
    }

    // ------------------------------------------------------------------ load

    private void load() {
        loadMonths();
        loadTransactions();
        loadCategories();
        loadSettings();

        long max = 0;
        for (Models.Tx tx : txs) {
            if (tx.id > max) max = tx.id;
        }
        nextId = Math.max(1, max + 1);
    }

    private void loadMonths() {
        String raw = prefs.getString(KEY_MONTHS, "");
        if (raw == null || raw.isEmpty()) return;
        try {
            JSONObject root = new JSONObject(raw);
            for (java.util.Iterator<String> it = root.keys(); it.hasNext(); ) {
                String key = it.next();
                JSONObject o = root.optJSONObject(key);
                if (o == null) continue;
                Models.MonthConfig c = new Models.MonthConfig();
                c.month = key;
                c.total = o.optDouble("total", 0);
                c.ratio = o.optDouble("ratio", 10);
                c.dailyMode = o.optBoolean("dailyMode", false);
                c.dailyAmount = o.optDouble("dailyAmount", 0);
                c.carryIn = o.optDouble("carryIn", 0);
                c.startDay = o.optInt("startDay", 1);
                c.configured = o.optBoolean("configured", false);
                months.put(key, c);
            }
        } catch (Exception ignored) {
        }
    }

    private void loadTransactions() {
        String raw = prefs.getString(KEY_TXS, "");
        if (raw == null || raw.isEmpty()) return;
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                Models.Tx tx = new Models.Tx();
                tx.id = o.optLong("id", 0);
                tx.date = o.optString("date", "");
                tx.createdAt = o.optLong("createdAt", 0);
                tx.type = o.optString("type", Models.EXPENSE);
                tx.amount = o.optDouble("amount", 0);
                tx.category = o.optString("category", "其他");
                tx.note = o.optString("note", "");
                if (tx.date.isEmpty()) continue;
                txs.add(tx);
            }
            sortTransactions();
        } catch (Exception ignored) {
        }
    }

    private void loadCategories() {
        String raw = prefs.getString(KEY_CATS, "");
        if (raw != null && !raw.isEmpty()) {
            try {
                JSONObject root = new JSONObject(raw);
                readCategories(root.optJSONArray("expense"), expenseCategories);
                readCategories(root.optJSONArray("income"), incomeCategories);
            } catch (Exception ignored) {
            }
        }
        if (expenseCategories.isEmpty()) expenseCategories.addAll(DEFAULT_EXPENSE_CATS);
        if (incomeCategories.isEmpty()) incomeCategories.addAll(DEFAULT_INCOME_CATS);
    }

    private static void readCategories(JSONArray arr, List<String> out) {
        if (arr == null) return;
        for (int i = 0; i < arr.length(); i++) {
            String name = arr.optString(i, "").trim();
            if (!name.isEmpty() && !out.contains(name)) out.add(name);
        }
    }

    private void loadSettings() {
        String raw = prefs.getString(KEY_SETTINGS, "");
        if (raw == null || raw.isEmpty()) return;
        try {
            JSONObject o = new JSONObject(raw);
            carryEnabled = o.optBoolean(KEY_CARRY, true);
            onboarded = o.optBoolean(KEY_ONBOARDED, false);
        } catch (Exception ignored) {
        }
    }

    // ------------------------------------------------------------------ save

    private void saveMonths() {
        try {
            JSONObject root = new JSONObject();
            for (Models.MonthConfig c : months.values()) {
                JSONObject o = new JSONObject();
                o.put("total", c.total);
                o.put("ratio", c.ratio);
                o.put("dailyMode", c.dailyMode);
                o.put("dailyAmount", c.dailyAmount);
                o.put("carryIn", c.carryIn);
                o.put("startDay", c.startDay);
                o.put("configured", c.configured);
                root.put(c.month, o);
            }
            prefs.edit().putString(KEY_MONTHS, root.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    private void saveTransactions() {
        try {
            JSONArray arr = new JSONArray();
            for (Models.Tx tx : txs) {
                JSONObject o = new JSONObject();
                o.put("id", tx.id);
                o.put("date", tx.date);
                o.put("createdAt", tx.createdAt);
                o.put("type", tx.type);
                o.put("amount", tx.amount);
                o.put("category", tx.category);
                o.put("note", tx.note == null ? "" : tx.note);
                arr.put(o);
            }
            prefs.edit().putString(KEY_TXS, arr.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    private void saveCategories() {
        try {
            JSONObject root = new JSONObject();
            root.put("expense", new JSONArray(expenseCategories));
            root.put("income", new JSONArray(incomeCategories));
            prefs.edit().putString(KEY_CATS, root.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    private void saveSettings() {
        try {
            JSONObject o = new JSONObject();
            o.put(KEY_CARRY, carryEnabled);
            o.put(KEY_ONBOARDED, onboarded);
            prefs.edit().putString(KEY_SETTINGS, o.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    // ------------------------------------------------------------------ months

    public Models.MonthConfig getMonth(String key) {
        return months.get(key);
    }

    public Models.MonthConfig ensureMonth(String key) {
        Models.MonthConfig existing = months.get(key);
        if (existing != null) return existing;

        Models.MonthConfig c = new Models.MonthConfig();
        c.month = key;
        c.ratio = 10;
        c.total = 0;
        c.configured = false;
        c.dailyMode = false;
        c.carryIn = 0;
        c.startDay = 1;

        Models.MonthConfig previous = months.get(previousMonthKey(key));
        if (previous != null) {
            c.ratio = previous.ratio;
            if (carryEnabled) {
                c.carryIn = carryFromPrevious(key);
            }
        } else if (carryEnabled) {
            c.carryIn = carryFromPrevious(key);
        }
        months.put(key, c);
        saveMonths();
        return c;
    }

    public Models.MonthConfig ensureCurrentMonth() {
        return ensureMonth(YearMonth.now().toString());
    }

    private static String previousMonthKey(String key) {
        try {
            return YearMonth.parse(key).minusMonths(1).toString();
        } catch (Exception e) {
            return "";
        }
    }

    /** Remaining funds of the previous month, clamped to >= 0. */
    public double carryFromPrevious(String key) {
        String prevKey = previousMonthKey(key);
        if (prevKey.isEmpty()) return 0;
        Models.MonthConfig prev = months.get(prevKey);
        if (prev == null) {
            // No saved budget, but imported/legacy transactions may still exist.
            boolean hasTx = !transactionsForMonth(prevKey).isEmpty();
            if (!hasTx) return 0;
            prev = new Models.MonthConfig();
            prev.month = prevKey;
            prev.total = 0;
            prev.ratio = 10;
            prev.configured = false;
            prev.carryIn = 0;
        }
        Models.Summary summary = BudgetEngine.compute(prev, transactionsForMonth(prevKey), LocalDate.now());
        return Math.max(0, summary.remaining);
    }

    /** Recalculates this month's carry-in snapshot from the previous month. */
    public double recalculateCarry(String key) {
        Models.MonthConfig c = months.get(key);
        if (c == null) return 0;
        double carry = carryEnabled ? carryFromPrevious(key) : 0;
        c.carryIn = Math.max(0, carry);
        saveMonths();
        return c.carryIn;
    }

    public void saveMonth(Models.MonthConfig c) {
        if (c == null || c.month == null || c.month.isEmpty()) return;
        months.put(c.month, c);
        saveMonths();
    }

    public Models.Summary summary(String key) {
        Models.MonthConfig c = months.get(key);
        if (c == null) c = ensureMonth(key);
        return BudgetEngine.compute(c, transactionsForMonth(key), LocalDate.now());
    }

    public Models.Summary currentSummary() {
        return summary(YearMonth.now().toString());
    }

    public List<String> monthKeys() {
        return new ArrayList<>(months.keySet());
    }

    public boolean hasData(String key) {
        Models.MonthConfig c = months.get(key);
        return (c != null && c.configured) || !transactionsForMonth(key).isEmpty();
    }

    // ------------------------------------------------------------------ transactions

    public List<Models.Tx> transactionsForMonth(String key) {
        List<Models.Tx> out = new ArrayList<>();
        for (Models.Tx tx : txs) {
            if (key.equals(tx.month())) out.add(tx);
        }
        out.sort(Comparator.comparing((Models.Tx t) -> t.date)
                .thenComparingLong(t -> t.createdAt)
                .thenComparingLong(t -> t.id));
        return out;
    }

    public List<Models.Tx> allTransactions() {
        return new ArrayList<>(txs);
    }

    public Models.Tx transactionById(long id) {
        for (Models.Tx tx : txs) {
            if (tx.id == id) return tx;
        }
        return null;
    }

    public void addTransaction(Models.Tx tx) {
        if (tx == null) return;
        if (tx.id <= 0) tx.id = nextId++;
        if (tx.createdAt <= 0) tx.createdAt = System.currentTimeMillis();
        txs.add(tx);
        sortTransactions();
        saveTransactions();
    }

    public void updateTransaction(Models.Tx tx) {
        if (tx == null) return;
        for (int i = 0; i < txs.size(); i++) {
            if (txs.get(i).id == tx.id) {
                txs.set(i, tx);
                sortTransactions();
                saveTransactions();
                return;
            }
        }
        addTransaction(tx);
    }

    public void deleteTransaction(long id) {
        for (int i = 0; i < txs.size(); i++) {
            if (txs.get(i).id == id) {
                txs.remove(i);
                saveTransactions();
                return;
            }
        }
    }

    public int clearMonthTransactions(String key) {
        int removed = 0;
        for (int i = txs.size() - 1; i >= 0; i--) {
            if (key.equals(txs.get(i).month())) {
                txs.remove(i);
                removed++;
            }
        }
        if (removed > 0) saveTransactions();
        return removed;
    }

    public int totalTransactionCount() {
        return txs.size();
    }

    public int monthsWithDataCount() {
        int n = 0;
        for (String key : months.keySet()) {
            if (hasData(key)) n++;
        }
        // Transactions in a month with no config still count as a recorded month.
        java.util.HashSet<String> keys = new java.util.HashSet<>();
        for (Models.Tx tx : txs) keys.add(tx.month());
        for (String key : keys) {
            if (!months.containsKey(key) && !key.isEmpty()) n++;
        }
        return n;
    }

    private void sortTransactions() {
        txs.sort(Comparator.comparing((Models.Tx t) -> t.date)
                .thenComparingLong(t -> t.createdAt)
                .thenComparingLong(t -> t.id));
    }

    // ------------------------------------------------------------------ categories

    public List<String> categories(String type) {
        return new ArrayList<>(isIncome(type) ? incomeCategories : expenseCategories);
    }

    public boolean addCategory(String type, String rawName) {
        String name = cleanCategory(rawName);
        if (name.isEmpty()) return false;
        List<String> list = isIncome(type) ? incomeCategories : expenseCategories;
        if (list.size() >= 24) return false;
        if (list.contains(name)) return false;
        list.add(name);
        saveCategories();
        return true;
    }

    public boolean renameCategory(String type, String oldName, String rawNewName) {
        String name = cleanCategory(rawNewName);
        if (name.isEmpty() || oldName == null) return false;
        List<String> list = isIncome(type) ? incomeCategories : expenseCategories;
        int index = list.indexOf(oldName);
        if (index < 0) return false;
        if (!name.equals(oldName) && list.contains(name)) return false;
        list.set(index, name);
        for (Models.Tx tx : txs) {
            if (type.equals(tx.type) && oldName.equals(tx.category)) {
                tx.category = name;
            }
        }
        saveCategories();
        saveTransactions();
        return true;
    }

    public boolean deleteCategory(String type, String name) {
        List<String> list = isIncome(type) ? incomeCategories : expenseCategories;
        if (name == null || "其他".equals(name) || list.size() <= 1) return false;
        if (!list.remove(name)) return false;
        if (!list.contains("其他")) list.add("其他");
        for (Models.Tx tx : txs) {
            if (type.equals(tx.type) && name.equals(tx.category)) {
                tx.category = "其他";
            }
        }
        saveCategories();
        saveTransactions();
        return true;
    }

    public int categoryUsage(String type, String name) {
        int n = 0;
        for (Models.Tx tx : txs) {
            if (type.equals(tx.type) && name.equals(tx.category)) n++;
        }
        return n;
    }

    public String sanitizeCategory(String type, String name) {
        List<String> list = isIncome(type) ? incomeCategories : expenseCategories;
        if (name != null && list.contains(name)) return name;
        return list.isEmpty() ? "其他" : list.get(0);
    }

    private static boolean isIncome(String type) {
        return Models.INCOME.equals(type);
    }

    private static String cleanCategory(String raw) {
        if (raw == null) return "";
        String name = raw.trim().replace('\n', ' ').replace('\r', ' ');
        while (name.contains("  ")) name = name.replace("  ", " ");
        if (name.length() > 12) name = name.substring(0, 12);
        return name;
    }

    // ------------------------------------------------------------------ settings

    public boolean isCarryEnabled() {
        return carryEnabled;
    }

    public void setCarryEnabled(boolean enabled) {
        carryEnabled = enabled;
        saveSettings();
    }

    public boolean isOnboarded() {
        return onboarded;
    }

    public void setOnboarded(boolean value) {
        onboarded = value;
        saveSettings();
    }

    public void resetAll() {
        months.clear();
        txs.clear();
        expenseCategories.clear();
        expenseCategories.addAll(DEFAULT_EXPENSE_CATS);
        incomeCategories.clear();
        incomeCategories.addAll(DEFAULT_INCOME_CATS);
        carryEnabled = true;
        onboarded = false;
        nextId = 1;
        prefs.edit().clear().apply();
    }

    /** Convenience for the UI: copies a list sorted by amount, keeping its label. */
    public static List<Map.Entry<String, Double>> sortedTotals(Map<String, Double> totals) {
        List<Map.Entry<String, Double>> list = new ArrayList<>(totals.entrySet());
        list.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return list;
    }

    public static List<String> unmodifiable(List<String> list) {
        return Collections.unmodifiableList(list);
    }
}
