package com.banking.pdf_chat_spring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GroqClient {

    private static final Logger log = LoggerFactory.getLogger(GroqClient.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;

    public GroqClient(@Value("${groq.api-key}") String apiKey) {
        this.apiKey = apiKey;
        this.baseUrl = "https://api.groq.com/openai/v1";
        this.objectMapper = new ObjectMapper();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);
        factory.setReadTimeout(60000);
        this.restTemplate = new RestTemplate(factory);
    }

    public String generate(String prompt) {
        try {
            log.info("🟢 Groq Request gestartet");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "llama-3.3-70b-versatile"); // oder "mixtral-8x7b-32768"
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", "Du bist ein präziser Parser für Banktransaktionen. Antworte NUR mit JSON."),
                    Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("temperature", 0.1);
            requestBody.put("max_tokens", 4096);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/chat/completions",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            // Extrahiere die Antwort aus dem OpenAI-Format
            Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");

            log.info("✅ Groq Antwort erhalten ({} Zeichen)", content.length());
            return content;

        } catch (Exception e) {
            log.error("❌ Groq Fehler", e);
            throw new RuntimeException("Groq API Fehler: " + e.getMessage(), e);
        }
    }
}