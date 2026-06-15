package com.banking.pdf_chat_spring.model;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum Category {
    RENT("Miete", "Wohnung", "Hausverwaltung", "Vermieter"),
    GROCERIES("Lebensmittel", "REWE", "Aldi", "Edeka", "Lidl", "Kaufland"),
    UTILITIES("Strom", "Energie", "Vattenfall", "EON", "Gas"),
    SALARY("Gehalt", "Lohn", "Überweisung von", "Firma"),
    RESTAURANT("Restaurant", "Lieferando", "Pizza", "Sushi", "Café"),
    FUEL("Tankstelle", "Shell", "Aral", "BP"),
    INSURANCE("Versicherung", "Haftpflicht", "Rechtsschutz"),
    OTHER("Sonstiges");

    private final Set<String> keywords;

    Category(String... keywords) {
        this.keywords = Arrays.stream(keywords)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    public static Category fromDescription(String description) {
        if (description == null || description.isBlank()) {
            return OTHER;
        }

        String lowerDesc = description.toLowerCase();
        Category bestMatch = OTHER;
        int longestKeywordLength = 0;

        for (Category category : values()) {
            for (String keyword : category.keywords) {
                // Prüft, ob das Keyword exakt so im Text vorkommt
                if (lowerDesc.contains(keyword)) {
                    // Das längere/spezifischere Keyword gewinnt (z.B. "Vattenfall" > "Strom")
                    if (keyword.length() > longestKeywordLength) {
                        longestKeywordLength = keyword.length();
                        bestMatch = category;
                    }
                }
            }
        }
        return bestMatch;
    }

}