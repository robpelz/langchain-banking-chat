package com.banking.pdf_chat_spring.service;

import com.banking.pdf_chat_spring.model.Category;
import com.banking.pdf_chat_spring.model.Transaction;
import com.banking.pdf_chat_spring.util.DateUtil;
import com.banking.pdf_chat_spring.util.InputSanitizer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class PdfParsingService {

    private static final Logger log = LoggerFactory.getLogger(PdfParsingService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    // Konstanten für bessere Wartbarkeit statt Magic Numbers
    private static final int MAX_PAGE_TEXT_LENGTH = 5500;
    private static final int MIN_VALID_YEAR = 2000;
    private static final int MAX_VALID_YEAR = 2100;

    private final PdfTextExtractor textExtractor;
    private final TransactionStorageService storage;
    private final GroqClient groqClient;  // ← GroqClient
    private final ObjectMapper objectMapper;

    // Konstruktor mit GroqClient
    public PdfParsingService(PdfTextExtractor textExtractor,
                             TransactionStorageService storage,
                             GroqClient groqClient,
                             ObjectMapper objectMapper) {
        this.textExtractor = textExtractor;
        this.storage = storage;
        this.groqClient = groqClient;
        this.objectMapper = objectMapper;
    }

    public int parseAndStore(File pdfFile) {
        long startTime = System.currentTimeMillis();
        log.info("========== START PDF VERARBEITUNG ==========");
        log.info("Datei: {}", pdfFile.getName());
        log.info("Dateigröße: {} bytes", pdfFile.length());

        try {
            // 1. Text seitenweise auslesen
            long extractStart = System.currentTimeMillis();
            List<String> pdfPages = textExtractor.extractTextPageByPage(pdfFile);
            long extractEnd = System.currentTimeMillis();
            log.info("📄 Textextraktion: {} Seiten in {} ms", pdfPages.size(), (extractEnd - extractStart));

            if (pdfPages.isEmpty()) {
                log.warn("⚠️ Kein Text in PDF gefunden: {}", pdfFile.getName());
                return 0;
            }

            List<Transaction> allTransactions = new ArrayList<>();
            int pageNumber = 1;
            int totalPages = pdfPages.size();

            // Variablen für LLM-Zeit-Messung
            long totalLlmTime = 0;

            for (String pageText : pdfPages) {
                if (pageText.isBlank()) {
                    log.debug("Seite {} ist leer, überspringe", pageNumber);
                    pageNumber++;
                    continue;
                }

                log.info("🔄 Verarbeite Seite {}/{} für Datei: {}", pageNumber, totalPages, pdfFile.getName());
                log.debug("Seitentext Länge: {} Zeichen", pageText.length());

                long llmStart = System.currentTimeMillis();
                String prompt = buildPrompt(pageText);
                String llmResponse = groqClient.generate(prompt);  // ← GroqClient
                long llmEnd = System.currentTimeMillis();
                long llmTime = llmEnd - llmStart;
                totalLlmTime += llmTime;

                log.info("🤖 LLM Verarbeitung Seite {}: {} ms", pageNumber, llmTime);

                List<Transaction> pageTransactions = parseResponse(llmResponse);  // ← llmResponse
                log.info("✅ Seite {}: {} Transaktionen extrahiert", pageNumber, pageTransactions.size());

                allTransactions.addAll(pageTransactions);
                pageNumber++;
            }

            // 2. Speichern
            long storageStart = System.currentTimeMillis();
            storage.addAllTransactions(allTransactions);
            long storageEnd = System.currentTimeMillis();

            long totalTime = System.currentTimeMillis() - startTime;

            log.info("========== PDF VERARBEITUNG ABGESCHLOSSEN ==========");
            log.info("📊 Datei: {} | Seiten: {} | Transaktionen: {} | Gesamtzeit: {} ms",
                    pdfFile.getName(), totalPages, allTransactions.size(), totalTime);
            log.info("⏱️  Details: Textextraktion: {} ms | LLM gesamt: {} ms | Speichern: {} ms",
                    (extractEnd - extractStart),
                    totalLlmTime,
                    (storageEnd - storageStart));

            return allTransactions.size();

        } catch (Exception e) {
            log.error("❌ FEHLER bei PDF-Verarbeitung: {}", pdfFile.getName(), e);
            throw new RuntimeException("Fehler beim Parsen der PDF: " + e.getMessage(), e);
        }
    }

    private String buildPrompt(String pageText) {
        String limitedText = pageText.length() > MAX_PAGE_TEXT_LENGTH
                ? pageText.substring(0, MAX_PAGE_TEXT_LENGTH)
                : pageText;

        return """
        [SYSTEM]
        Du bist ein präziser Parser für deutsche Kontoauszüge.
        Deine einzige Aufgabe ist es, strukturierte Buchungsdaten als JSON-Array zu extrahieren.
        
        REGELN:
        1. Spalten: Buchungstag | Wertstellung | Umsatzart | Verwendungszweck | Betrag (€)
        2. Nutze BUCHUNGSTAG als Datum (erste Spalte)
        3. Beträge: "+3.400,00" = 3400.00 (positiv), "-890,00" = -890.00 (negativ)
        4. Wandle Kommas in Punkte um und entferne Tausenderpunkte
        5. Verwendungszweck ist die Beschreibung
        6. Ignoriere "Saldo Anfang/Ende"
        
        JSON-STRUKTUR:
        [{"date": "DD.MM.YYYY", "description": "TEXT", "amount": ZAHL}]
        
        ANTWORTE AUSSCHLIESSLICH MIT DEM JSON-ARRAY - KEINE ERKLÄRUNGEN UND KEIN MARKDOWN!
        [/SYSTEM]
        
        [BEISPIEL]
        INPUT:
        Buchungstag | Betrag (€)
        01.03.2026 | +3.400,00
        02.03.25 | -890,00
        
        OUTPUT:
        [{"date":"01.03.2026","description":"Gehalt","amount":3400.00},{"date":"02.03.2025","description":"Miete","amount":-890.00}]
        [/BEISPIEL]
        
        [PDF-INHALT]
        %s
        [/PDF-INHALT]
        """.formatted(limitedText);
    }

    private List<Transaction> parseResponse(String responseBody) {
        List<Transaction> transactions = new ArrayList<>();

        try {
            // Groq liefert direkt den JSON-String, keinen "response" Wrapper
            String content = responseBody.trim();

            log.info("Groq rohe Antwort: {}", content.length() > 500 ? content.substring(0, 500) + "..." : content);

            if (content.startsWith("```")) {
                content = content.replaceAll("^```json\\s*", "")
                        .replaceAll("^```\\s*", "")
                        .replaceAll("```$", "")
                        .trim();
            }

            JsonNode array = objectMapper.readTree(content);
            if (!array.isArray()) {
                log.warn("Kein JSON-Array von Groq erhalten.");
                return transactions;
            }

            if (array.isEmpty()) {
                log.warn("Groq hat ein leeres JSON-Array zurückgegeben.");
            }

            for (JsonNode node : array) {
                String dateStr = getJsonField(node, "date", "datum");
                String description = getJsonField(node, "description", "verwendungszweck");

                double amount = node.has("amount")
                        ? node.get("amount").asDouble()
                        : (node.has("betrag") ? parseGermanNumber(node.get("betrag").asText()) : 0.0);

                if (dateStr.isEmpty() || description.isEmpty()) {
                    continue;
                }

                try {
                    LocalDate date = parseDate(dateStr);

                    Transaction tx = Transaction.builder()
                            .withId(UUID.randomUUID().toString())
                            .withDate(date)
                            .withDescription(InputSanitizer.sanitizeString(description))
                            .withAmount(amount)
                            .withCategory(Category.fromDescription(description))
                            .build();

                    transactions.add(tx);
                } catch (Exception parseEx) {
                    log.warn("Zeile auf Seite übersprungen wegen parsing Fehler: {} | {}", node, parseEx.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Fehler beim Parsen der Groq-Antwort", e);
        }

        return transactions;
    }

    private LocalDate parseDate(String dateStr) {
        try {
            String[] parts = dateStr.split("\\.");
            if (parts.length == 3) {
                int day = Integer.parseInt(parts[0].trim());
                int month = Integer.parseInt(parts[1].trim());
                int year = Integer.parseInt(parts[2].trim());

                if (year < 100) {
                    year = 2000 + year;
                }

                if (day >= 1 && day <= 31 && month >= 1 && month <= 12 && year >= MIN_VALID_YEAR && year <= MAX_VALID_YEAR) {
                    String normalizedDate = String.format("%02d.%02d.%04d", day, month, year);
                    return LocalDate.parse(normalizedDate, DATE_FORMATTER);
                }
            }
        } catch (Exception e) {
            log.debug("Lokales Extra-Parsing fehlgeschlagen für '{}', nutze Standard-Fallback.", dateStr);
        }
        return DateUtil.parseGermanDate(dateStr.trim());
    }

    private double parseGermanNumber(String numberStr) {
        if (numberStr == null || numberStr.isBlank()) {
            return 0.0;
        }
        try {
            String cleaned = numberStr.replace("€", "").replace("+", "").trim();
            boolean negative = cleaned.startsWith("-");
            if (negative) {
                cleaned = cleaned.substring(1);
            }
            cleaned = cleaned.replace(".", "").replace(",", ".");
            double val = Double.parseDouble(cleaned);
            return negative ? -val : val;
        } catch (Exception e) {
            log.warn("Konnte Betrag '{}' nicht parsen", numberStr);
            return 0.0;
        }
    }

    private String getJsonField(JsonNode node, String primaryKey, String secondaryKey) {
        if (node.has(primaryKey)) {
            return node.get(primaryKey).asText().trim();
        } else if (node.has(secondaryKey)) {
            return node.get(secondaryKey).asText().trim();
        }
        return "";
    }
}