package com.banking.pdf_chat_spring.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    @Value("${pdf.directory:./pdf-documents}")
    private String pdfDirectory;

    @PostConstruct
    public void validatePaths() {
        Path normalizedPath = Paths.get(pdfDirectory).normalize().toAbsolutePath();
        log.info("PDF-Verzeichnis: {}", normalizedPath);

        // Pfad-Traversal Schutz
        if (normalizedPath.toString().contains("..")) {
            throw new IllegalStateException("Ungültiger Pfad: " + pdfDirectory);
        }
    }
}