package com.banking.pdf_chat_spring.util;

public class InputSanitizer {

    private static final int MAX_STRING_LENGTH = 500;

    public static String sanitizeString(String input) {
        if (input == null) return "";

        // Maximale Länge kappen, um Speicherüberlastung zu vermeiden
        String trimmed = input.length() > MAX_STRING_LENGTH ?
                input.substring(0, MAX_STRING_LENGTH) : input;

        // 1. Entferne XML/HTML-Tags (Schutz vor XSS im Frontend)
        String clean = trimmed.replaceAll("<[^>]*>", "");

        // 2. Ersetze potenziell gefährliche Zeichen durch harmlose Varianten
        clean = clean.replace("<", "").replace(">", "");

        // 3. Bereinige unsaubere Zeilenumbrüche oder Tabs, die aus der PDF kommen
        clean = clean.replaceAll("[\\r\\n\\t]+", " ");

        return clean.trim();
    }
}
