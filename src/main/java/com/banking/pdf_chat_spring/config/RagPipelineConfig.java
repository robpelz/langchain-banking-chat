package com.banking.pdf_chat_spring.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagPipelineConfig {

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        // WARUM: In-Memory reicht für kleine PDF-Mengen (< 100 PDFs)
        // Wenn du mehr PDFs hast, tausche gegen PostgreSQL/pgvector
        return new InMemoryEmbeddingStore<>();
    }
}