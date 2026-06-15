package com.banking.pdf_chat_spring.model;

import com.banking.pdf_chat_spring.util.DateUtil;
import java.util.HashMap;
import java.util.Map;

/**
 * Monatszusammenfassung für Analysen und Dashboard.
 * Immutable nach der Erstellung.
 */
public class MonthlySummary {
    private final int month;
    private final int year;
    private final double totalIncome;
    private final double totalExpenses;
    private final double savingsRate;
    private final Map<String, Double> expensesByCategory;

    private MonthlySummary(Builder builder) {
        this.month = builder.month;
        this.year = builder.year;
        this.totalIncome = builder.totalIncome;
        this.totalExpenses = builder.totalExpenses;
        this.savingsRate = builder.savingsRate;
        this.expensesByCategory = new HashMap<>(builder.expensesByCategory);
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getter
    public int getMonth() { return month; }
    public int getYear() { return year; }
    public double getTotalIncome() { return totalIncome; }
    public double getTotalExpenses() { return totalExpenses; }
    public double getSavingsRate() { return savingsRate; }
    public Map<String, Double> getExpensesByCategory() { return new HashMap<>(expensesByCategory); }

    // Nutzt jetzt die sichere und zentrale DateUtil-Klasse
    public String getMonthName() {
        return DateUtil.getMonthName(month);
    }

    public double getExpenseForCategory(String category) {
        if (category == null) return 0.0;
        return expensesByCategory.getOrDefault(category, 0.0);
    }

    @Override
    public String toString() {
        return String.format("%s %d: Einnahmen %.2f €, Ausgaben %.2f €, Sparquote %.1f%%",
                getMonthName(), year, totalIncome, totalExpenses, savingsRate);
    }

    public static class Builder {
        private int month;
        private int year;
        private double totalIncome;
        private double totalExpenses;
        private double savingsRate;
        private final Map<String, Double> expensesByCategory = new HashMap<>();

        public Builder withMonth(int month) { this.month = month; return this; }
        public Builder withYear(int year) { this.year = year; return this; }
        public Builder withTotalIncome(double totalIncome) { this.totalIncome = totalIncome; return this; }
        public Builder withTotalExpenses(double totalExpenses) { this.totalExpenses = totalExpenses; return this; }
        public Builder withSavingsRate(double savingsRate) { this.savingsRate = savingsRate; return this; }

        public Builder addExpenseCategory(String category, double amount) {
            if (category != null) {
                this.expensesByCategory.put(category, amount);
            }
            return this;
        }

        public MonthlySummary build() {
            if (month < 1 || month > 12) {
                throw new IllegalArgumentException("Monat muss zwischen 1 und 12 liegen: " + month);
            }
            // Aktualisiert auf das aktuelle Kalenderjahr 2026 als Obergrenze
            if (year < 2000 || year > 2026) {
                throw new IllegalArgumentException("Jahr ungültig: " + year);
            }
            return new MonthlySummary(this);
        }
    }
}
