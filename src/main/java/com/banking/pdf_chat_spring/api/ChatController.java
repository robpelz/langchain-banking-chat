package com.banking.pdf_chat_spring.api;

import com.banking.pdf_chat_spring.dto.ChatDto;
import com.banking.pdf_chat_spring.service.ChatService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class ChatController {

    private final ChatService chatService;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    @Value("${rag.max-results:5}")
    private int maxResults;

    @Value("${rag.min-score:0.7}")
    private double minScore;

    public ChatController(ChatService chatService,
                          EmbeddingModel embeddingModel,
                          EmbeddingStore<TextSegment> embeddingStore) {
        this.chatService = chatService;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("chunkCount", "?");
        return "index";
    }

    @PostMapping("/api/chat")
    @ResponseBody
    public ChatDto.ChatResponse chat(@RequestBody ChatDto.ChatRequest request) {
        String userInput = request.question();

        Embedding queryEmbedding = embeddingModel.embed(userInput).content();
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(
                queryEmbedding, maxResults, minScore
        );

        if (matches.isEmpty()) {
            return new ChatDto.ChatResponse("Keine relevanten Informationen gefunden.", List.of());
        }

        String context = matches.stream()
                .map(m -> m.embedded().text())
                .collect(Collectors.joining("\n---\n"));

        String answer = chatService.chat(userInput, context);

        List<String> sources = matches.stream()
                .limit(3)
                .map(m -> "PDF-Auszug")
                .collect(Collectors.toList());

        return new ChatDto.ChatResponse(answer, sources);
    }

    @GetMapping("/api/status")
    @ResponseBody
    public ChatDto.StatusResponse status() {
        return new ChatDto.StatusResponse(-1);
    }
}