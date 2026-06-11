package com.banking.pdf_chat_spring.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);

    @Value("${pdf.directory}")
    private String pdfDirectory;

    @Value("${pdf.chunk.size:500}")
    private int chunkSize;

    @Value("${pdf.chunk.overlap:50}")
    private int chunkOverlap;

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public DocumentIngestionService(EmbeddingModel embeddingModel,
                                    EmbeddingStore<TextSegment> embeddingStore) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    @PostConstruct
    public void ingestDocuments() {
        log.info("Starte PDF-Import aus: {}", pdfDirectory);

        List<Document> documents = loadPdfs();
        if (documents.isEmpty()) {
            log.warn("Keine PDFs in '{}' gefunden.", pdfDirectory);
            return;
        }

        List<TextSegment> segments = splitDocuments(documents);
        log.info("{} Dokumente → {} Textsegmente", documents.size(), segments.size());

        createAndStoreEmbeddings(segments);
        // FIXED: count() existiert nicht in 0.35.0, daher segments.size() verwenden
        log.info("✅ Import abgeschlossen! {} Chunks verarbeitet", segments.size());
    }

    private List<Document> loadPdfs() {
        List<Document> documents = new ArrayList<>();
        File dir = new File(pdfDirectory);

        if (!dir.exists()) {
            log.error("Verzeichnis existiert nicht: {}", pdfDirectory);
            return documents;
        }

        File[] pdfFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".pdf"));
        if (pdfFiles == null || pdfFiles.length == 0) {
            return documents;
        }

        for (File pdfFile : pdfFiles) {
            try {
                Document document = FileSystemDocumentLoader.loadDocument(
                        pdfFile.toPath(),
                        new ApachePdfBoxDocumentParser()
                );
                documents.add(document);
                log.debug("Geladen: {}", pdfFile.getName());
            } catch (Exception e) {
                log.error("Fehler bei {}: {}", pdfFile.getName(), e.getMessage());
            }
        }
        return documents;
    }

    private List<TextSegment> splitDocuments(List<Document> documents) {
        List<TextSegment> allSegments = new ArrayList<>();
        for (Document doc : documents) {
            List<TextSegment> segments = DocumentSplitters.recursive(chunkSize, chunkOverlap)
                    .split(doc);
            allSegments.addAll(segments);
        }
        return allSegments;
    }

    private void createAndStoreEmbeddings(List<TextSegment> segments) {
        log.info("Generiere Embeddings für {} Segmente...", segments.size());

        for (int i = 0; i < segments.size(); i++) {
            TextSegment segment = segments.get(i);
            Embedding embedding = embeddingModel.embed(segment).content();
            embeddingStore.add(embedding, segment);

            if ((i + 1) % 50 == 0 || i == segments.size() - 1) {
                log.info("Fortschritt: {}/{}", i + 1, segments.size());
            }
        }
    }
}