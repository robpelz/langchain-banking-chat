package com.banking.pdf_chat_spring.dto;

import java.util.List;

public class ChatDto {

    public record ChatRequest(String question) {}

    public record ChatResponse(String answer, List<String> sources) {}

    public record StatusResponse(long chunkCount) {}
}