package com.banking.pdf_chat_spring.controller;

import com.banking.pdf_chat_spring.dto.ChatDto;
import com.banking.pdf_chat_spring.model.Category;
import com.banking.pdf_chat_spring.service.FinanceAnalysisService;
import com.banking.pdf_chat_spring.service.PdfParsingService;
import com.banking.pdf_chat_spring.service.TransactionStorageService;
import com.banking.pdf_chat_spring.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final PdfParsingService pdfParsingService;
    private final FinanceAnalysisService analysisService;
    private final TransactionStorageService storage;

    public ChatController(PdfParsingService pdfParsingService,
                          FinanceAnalysisService analysisService,
                          TransactionStorageService storage) {
        this.pdfParsingService = pdfParsingService;
        this.analysisService = analysisService;
        this.storage = storage;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("totalTransactions", storage.count());
        return "index";
    }

    @PostMapping("/api/upload")
    @ResponseBody
    public ResponseEntity<?> uploadPdf(@RequestParam("file") MultipartFile file) {
        File tempFile = null;
        try {
            log.info("📁 Upload gestartet: {} (Größe: {} bytes)", file.getOriginalFilename(), file.getSize());

            // Temporäre Datei erstellen (wird nach Verarbeitung gelöscht)
            tempFile = File.createTempFile("upload_", ".pdf");
            file.transferTo(tempFile);

            log.info("💾 Temporäre Datei erstellt: {}", tempFile.getAbsolutePath());

            // PDF verarbeiten
            int transactionCount = pdfParsingService.parseAndStore(tempFile);

            log.info("✅ {} Transaktionen extrahiert", transactionCount);

            return ResponseEntity.ok().body(String.format(
                    "✅ PDF verarbeitet: %d Transaktionen extrahiert", transactionCount));

        } catch (IOException e) {
            log.error("Upload fehlgeschlagen", e);
            return ResponseEntity.status(500).body("Fehler beim Speichern: " + e.getMessage());
        } catch (Exception e) {
            log.error("Verarbeitung fehlgeschlagen", e);
            return ResponseEntity.status(500).body("Fehler: " + e.getMessage());
        } finally {
            // Temporäre Datei löschen
            if (tempFile != null && tempFile.exists()) {
                try {
                    Files.delete(tempFile.toPath());
                    log.info("🗑️ Temporäre Datei gelöscht: {}", tempFile.getName());
                } catch (IOException e) {
                    log.warn("Konnte temporäre Datei nicht löschen: {}", tempFile.getName());
                }
            }
        }
    }

    @PostMapping("/api/chat")
    @ResponseBody
    public ChatDto.ChatResponse chat(@RequestBody ChatDto.ChatRequest request) {
        String answer = processQuestion(request.question());
        return new ChatDto.ChatResponse(answer, List.of());
    }

    private String processQuestion(String question) {
        String lower = question.toLowerCase();
        int currentYear = getCurrentYear();

        if (lower.contains("gesamtausgaben") || lower.contains("summe ausgaben")) {
            int month = extractMonth(lower);
            if (month > 0) {
                double expenses = analysisService.getTotalExpenses(month, currentYear);
                return String.format("📊 Gesamtausgaben im %s %d: %.2f €",
                        DateUtil.getMonthName(month), currentYear, expenses);
            }
            double total = storage.getAllTransactions().stream()
                    .filter(tx -> tx.isExpense())
                    .mapToDouble(tx -> tx.getAbsoluteAmount())
                    .sum();
            return String.format("📊 Gesamtausgaben über alle Monate: %.2f €", total);
        }

        if (lower.contains("miete")) {
            return String.format("🏠 Mietkosten %d: %.2f €", currentYear,
                    analysisService.getCategoryTotal(Category.RENT, currentYear));
        }
        if (lower.contains("lebensmittel")) {
            return String.format("🛒 Lebensmittel-Ausgaben %d: %.2f €", currentYear,
                    analysisService.getCategoryTotal(Category.GROCERIES, currentYear));
        }
        if (lower.contains("strom")) {
            return String.format("⚡ Stromkosten %d: %.2f €", currentYear,
                    analysisService.getCategoryTotal(Category.UTILITIES, currentYear));
        }

        if (lower.contains("sparquote")) {
            int month = extractMonth(lower);
            if (month > 0) {
                double rate = analysisService.getSavingsRate(month, currentYear);
                return String.format("💰 Sparquote im %s %d: %.1f%%",
                        DateUtil.getMonthName(month), currentYear, rate);
            }
        }

        if (lower.contains("vergleich") && lower.contains("januar") && lower.contains("februar")) {
            return analysisService.compareMonths(1, currentYear, 2, currentYear);
        }

        if (lower.contains("alle buchungen")) {
            if (storage.isEmpty()) {
                return "Keine Buchungen vorhanden. Bitte lade zuerst PDFs hoch.";
            }
            return storage.getAllTransactions().stream()
                    .limit(20)
                    .map(tx -> String.format("%s | %s | %.2f € | %s",
                            tx.getDate(), tx.getDescription(), tx.getAmount(), tx.getCategory()))
                    .collect(Collectors.joining("\n", "📋 Buchungen (max. 20):\n", ""));
        }

        if (lower.contains("status")) {
            return String.format("📊 Status: %d Buchungen importiert", storage.count());
        }

        return helpText();
    }

    private int getCurrentYear() {
        return storage.getAllTransactions().stream()
                .map(tx -> tx.getYear())
                .max(Integer::compareTo)
                .orElse(java.time.LocalDate.now().getYear());
    }

    @GetMapping("/api/status")
    @ResponseBody
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("chunkCount", storage.count());
        status.put("currentYear", getCurrentYear());
        return status;
    }

    private int extractMonth(String question) {
        if (question.contains("januar")) return 1;
        if (question.contains("februar")) return 2;
        if (question.contains("märz") || question.contains("maerz")) return 3;
        if (question.contains("april")) return 4;
        if (question.contains("mai")) return 5;
        if (question.contains("juni")) return 6;
        if (question.contains("juli")) return 7;
        if (question.contains("august")) return 8;
        if (question.contains("september")) return 9;
        if (question.contains("oktober")) return 10;
        if (question.contains("november")) return 11;
        if (question.contains("dezember")) return 12;
        return 0;
    }

    private String helpText() {
        return """
            ❓ Ich verstehe die Frage nicht.
            
            📌 Beispiele:
            • 'Gesamtausgaben März'
            • 'Miete'
            • 'Lebensmittel'
            • 'Sparquote Januar'
            • 'Vergleich Januar mit Februar'
            • 'Alle Buchungen'
            • 'Status'
            """;
    }

    @GetMapping("/api/files")
    @ResponseBody
    public List<String> getUploadedFiles() {
        // Keine dauerhaft gespeicherten Dateien mehr
        return List.of();
    }
}