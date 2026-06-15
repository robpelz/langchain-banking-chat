package com.banking.pdf_chat_spring.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfTextExtractor {

    private static final Logger log = LoggerFactory.getLogger(PdfTextExtractor.class);

    /**
     * Extrahiert reinen Text aus der gesamten PDF-Datei (Ganzes Dokument)
     */
    public String extractText(File pdfFile) throws IOException {
        log.info("Extrahiere Text aus gesamter PDF: {}", pdfFile.getName());

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            String text = stripper.getText(document);
            log.info("Extrahiert {} Zeichen aus PDF", text.length());
            return text;
        }
    }

    /**
     * Extrahiert den Text aus der PDF-Datei seitenweise (Chunking für das LLM)
     * Jedes Element der Liste entspricht genau einer Seite im PDF.
     */
    public List<String> extractTextPageByPage(File pdfFile) throws IOException {
        log.info("Extrahiere Text seitenweise aus PDF: {}", pdfFile.getName());
        List<String> pages = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            int totalPages = document.getNumberOfPages();
            log.info("Die Datei hat insgesamt {} Seiten", totalPages);

            // WICHTIG: Stripper NUR EINMAL erstellen, NICHT in der Schleife
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            for (int page = 1; page <= totalPages; page++) {
                // Setze Seitenbereich für die aktuelle Seite
                stripper.setStartPage(page);
                stripper.setEndPage(page);

                String pageText = stripper.getText(document);

                if (pageText != null && !pageText.isBlank()) {
                    pages.add(pageText);
                    log.debug("Seite {} extrahiert ({} Zeichen)", page, pageText.length());
                } else {
                    log.warn("Seite {} ist leer", page);
                    pages.add(""); // Leere Seite trotzdem hinzufügen, um Seitenzahl-Konsistenz zu wahren
                }
            }

            log.info("Erfolgreich {} Seiten extrahiert", pages.size());
        }

        return pages;
    }
}