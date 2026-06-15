package com.banking.pdf_chat_spring.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class Transaction {
    private final String id;
    private final LocalDate date;
    private final String description;
    private final double amount;
    private final Category category;

    private Transaction(Builder builder) {
        this.id = builder.id;
        this.date = builder.date;
        this.description = builder.description;
        this.amount = builder.amount;
        this.category = builder.category;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getter
    public String getId() { return id; }
    public LocalDate getDate() { return date; }
    public String getDescription() { return description; }
    public double getAmount() { return amount; }
    public Category getCategory() { return category; }

    public boolean isIncome() { return amount > 0; }
    public boolean isExpense() { return amount < 0; }
    public double getAbsoluteAmount() { return Math.abs(amount); }
    public int getMonth() { return date.getMonthValue(); }
    public int getYear() { return date.getYear(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static class Builder {
        private String id = UUID.randomUUID().toString();
        private LocalDate date;
        private String description;
        private double amount;
        private Category category = Category.OTHER;

        public Builder withId(String id) { this.id = id; return this; }
        public Builder withDate(LocalDate date) { this.date = date; return this; }
        public Builder withDescription(String description) { this.description = description; return this; }
        public Builder withAmount(double amount) { this.amount = amount; return this; }
        public Builder withCategory(Category category) { this.category = category; return this; }

        public Transaction build() {
            Objects.requireNonNull(date, "Datum darf nicht null sein");
            return new Transaction(this);
        }
    }
}