package com.banking.pdf_chat_spring.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final ChatLanguageModel chatModel;

    public ChatService(ChatLanguageModel chatModel) {
        this.chatModel = chatModel;
    }

    public String chat(String userMessage, String context) {
        String prompt = """
                Du bist ein hilfreicher Banking-Assistent für Kontoauszüge.
                
                Kontext aus den PDFs:
                %s
                
                Frage: %s
                
                Regeln:
                1. Nutze NUR den gegebenen Kontext.
                2. Wenn die Antwort nicht im Kontext steht, sage: "Diese Information konnte ich in deinen Kontoauszügen nicht finden."
                3. Bei Buchungen nenne immer Datum, Betrag und Verwendungszweck.
                4. Sei präzise und zahlenbasiert.
                """.formatted(context, userMessage);

        return chatModel.generate(prompt);
    }
}