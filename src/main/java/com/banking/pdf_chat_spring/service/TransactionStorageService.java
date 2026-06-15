package com.banking.pdf_chat_spring.service;

import com.banking.pdf_chat_spring.model.Transaction;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class TransactionStorageService {

    private final List<Transaction> transactions = new CopyOnWriteArrayList<>();

    public synchronized void addTransaction(Transaction transaction) {
        if (!isDuplicate(transaction)) {
            transactions.add(transaction);
        }
    }

    public synchronized void addAllTransactions(List<Transaction> newTransactions) {
        for (Transaction tx : newTransactions) {
            addTransaction(tx);
        }
    }

    public List<Transaction> getAllTransactions() {
        return new ArrayList<>(transactions);
    }

    public void clear() {
        transactions.clear();
    }

    public int count() {
        return transactions.size();
    }

    public boolean isEmpty() {
        return transactions.isEmpty();
    }

    // Prüft, ob exakt diese Buchung (Inhaltlich) bereits existiert
    private boolean isDuplicate(Transaction tx) {
        return transactions.stream().anyMatch(existing ->
                existing.getDate().equals(tx.getDate()) &&
                        existing.getAmount() == tx.getAmount() &&
                        existing.getDescription().equalsIgnoreCase(tx.getDescription())
        );
    }
}
