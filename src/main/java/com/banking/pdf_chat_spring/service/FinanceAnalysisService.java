package com.banking.pdf_chat_spring.service;

import com.banking.pdf_chat_spring.model.Category;
import com.banking.pdf_chat_spring.model.Transaction;
import com.banking.pdf_chat_spring.util.DateUtil;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FinanceAnalysisService {

    private final TransactionStorageService storage;

    public FinanceAnalysisService(TransactionStorageService storage) {
        this.storage = storage;
    }

    public double getTotalExpenses(int month, int year) {
        return getAllTransactions().stream()
                .filter(Transaction::isExpense)
                .filter(tx -> tx.getMonth() == month && tx.getYear() == year)
                .mapToDouble(Transaction::getAbsoluteAmount)
                .sum();
    }

    public double getTotalIncome(int month, int year) {
        return getAllTransactions().stream()
                .filter(Transaction::isIncome)
                .filter(tx -> tx.getMonth() == month && tx.getYear() == year)
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    public double getSavingsRate(int month, int year) {
        double income = getTotalIncome(month, year);
        if (income <= 0) return 0.0; // Verhindert NaN oder Division durch Null

        double expenses = getTotalExpenses(month, year);
        return ((income - expenses) / income) * 100;
    }

    public Map<Category, Double> getExpensesByCategory(int month, int year) {
        return getAllTransactions().stream()
                .filter(Transaction::isExpense)
                .filter(tx -> tx.getMonth() == month && tx.getYear() == year)
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.summingDouble(Transaction::getAbsoluteAmount)
                ));
    }

    public double getCategoryTotal(Category category, int year) {
        return getAllTransactions().stream()
                .filter(Transaction::isExpense)
                .filter(tx -> tx.getYear() == year)
                .filter(tx -> tx.getCategory() == category)
                .mapToDouble(Transaction::getAbsoluteAmount)
                .sum();
    }

    public String compareMonths(int month1, int year1, int month2, int year2) {
        double expenses1 = getTotalExpenses(month1, year1);
        double expenses2 = getTotalExpenses(month2, year2);
        double rate1 = getSavingsRate(month1, year1);
        double rate2 = getSavingsRate(month2, year2);

        double change = expenses1 > 0 ? ((expenses2 - expenses1) / expenses1) * 100 : 0;

        // Nutzt jetzt das korrigierte und sichere DateUtil statt des alten Arrays
        return String.format("""
            📊 Vergleich %s %d vs. %s %d:
            Ausgaben: %.2f € vs. %.2f € (%.1f%% %s)
            Sparquote: %.1f%% vs. %.1f%%
            """,
                DateUtil.getMonthName(month1), year1, DateUtil.getMonthName(month2), year2,
                expenses1, expenses2, Math.abs(change), change > 0 ? "mehr" : "weniger",
                rate1, rate2);
    }

    private List<Transaction> getAllTransactions() {
        return storage.getAllTransactions();
    }
}
