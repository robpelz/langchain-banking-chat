package com.banking.pdf_chat_spring.util;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.Locale;

public final class DateUtil {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private DateUtil() {} // Verhindert Instanziierung

    public static LocalDate parseGermanDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            throw new IllegalArgumentException("Datum darf nicht leer sein");
        }

        String trimmed = dateStr.trim();

        // Korrigiere 2-stellige Jahreszahlen
        String[] parts = trimmed.split("\\.");
        if (parts.length == 3 && parts[2].length() == 2) {
            int year = Integer.parseInt(parts[2]);
            year = year < 100 ? 2000 + year : year;
            trimmed = String.format("%s.%s.%04d", parts[0], parts[1], year);
        }

        try {
            return LocalDate.parse(trimmed, FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Ungültiges Datumsformat: " + dateStr, e);
        }
    }

    public static String getMonthName(int month) {
        if (month < 1 || month > 12) {
            return "Unbekannter Monat";
        }
        return Month.of(month).getDisplayName(TextStyle.FULL, Locale.GERMAN);
    }
}