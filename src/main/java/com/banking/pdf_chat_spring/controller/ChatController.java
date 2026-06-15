package com.banking.pdf_chat_spring.controller;

import com.banking.pdf_chat_spring.dto.ChatDto;
import com.banking.pdf_chat_spring.model.Category;
import com.banking.pdf_chat_spring.service.FinanceAnalysisService;
import com.banking.pdf_chat_spring.service.PdfParsingService;
import com.banking.pdf_chat_spring.service.TransactionStorageService;
import com.banking.pdf_chat_spring.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
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
        long uploadStart = System.currentTimeMillis();
        log.info("📁 UPLOAD START: {} (Größe: {} bytes)", file.getOriginalFilename(), file.getSize());

        try {
            String userDir = System.getProperty("user.dir");
            File directory = new File(userDir, "pdf-documents");

            if (!directory.exists() && !directory.mkdirs()) {
                log.error("❌ Konnte Verzeichnis nicht erstellen: {}", directory.getAbsolutePath());
                return ResponseEntity.status(500).body("Konnte Verzeichnis nicht erstellen");
            }

            File targetFile = new File(directory, sanitizeFilename(file.getOriginalFilename()));
            file.transferTo(targetFile);
            log.info("💾 PDF gespeichert: {}", targetFile.getAbsolutePath());

            long parseStart = System.currentTimeMillis();
            int transactionCount = pdfParsingService.parseAndStore(targetFile);
            long parseTime = System.currentTimeMillis() - parseStart;

            long totalTime = System.currentTimeMillis() - uploadStart;

            log.info("✅ UPLOAD ABGESCHLOSSEN: {} | {} Transaktionen | Gesamtzeit: {} ms | Parse-Zeit: {} ms",
                    file.getOriginalFilename(), transactionCount, totalTime, parseTime);

            return ResponseEntity.ok().body(String.format(
                    "✅ PDF verarbeitet: %d Transaktionen extrahiert (%.1f Sekunden)",
                    transactionCount, totalTime / 1000.0));

        } catch (IOException e) {
            log.error("❌ UPLOAD FEHLGESCHLAGEN (IO): {}", file.getOriginalFilename(), e);
            return ResponseEntity.status(500).body("Fehler beim Speichern: " + e.getMessage());
        } catch (Exception e) {
            log.error("❌ UPLOAD FEHLGESCHLAGEN (Verarbeitung): {}", file.getOriginalFilename(), e);
            return ResponseEntity.status(500).body("Fehler: " + e.getMessage());
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
        int currentYear = 2026;

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

        if (lower.contains("vergleich")) {
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

    @GetMapping("/api/status")
    @ResponseBody
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("chunkCount", storage.count());
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
            • 'Gesamtausgaben Januar 2026'
            • 'Miete 2026'
            • 'Lebensmittel'
            • 'Sparquote Januar'
            • 'Vergleich Januar mit Februar'
            • 'Alle Buchungen'
            • 'Status'
            """;
    }

    /**
     * Entfernt gefährliche Zeichen aus Dateinamen
     */
    private String sanitizeFilename(String filename) {
        if (filename == null) return "unnamed.pdf";
        return filename.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    @GetMapping("/api/files")
    @ResponseBody
    public List<String> getUploadedFiles() {
        String userDir = System.getProperty("user.dir");
        File dir = new File(userDir, "pdf-documents");
        if (!dir.exists()) return List.of();

        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".pdf"));
        if (files == null) return List.of();

        return Arrays.stream(files)
                .map(File::getName)
                .sorted()
                .collect(Collectors.toList());
    }
}