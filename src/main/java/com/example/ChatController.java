package com.example;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private final VectorStore vectorStore;

    private ChatClient chatClient;

    private static final String SYSTEM_PROMPT = """
        Your are helpful AI assistant who responds to queries primarily based on the documents section below.
        
        Documents:
        
        {documents}
        
        """;

    public ChatController(VectorStore vectorStore, ChatClient.Builder chatClientBuilder) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    @PostMapping("/chat")
    public String postMethodName(@RequestBody String message) {
        List<Document> relatedDocuments = vectorStore.similaritySearch(message);
        String documents = relatedDocuments.stream().map(Document::getContent)
                .collect(Collectors.joining(System.lineSeparator()));

        return this.chatClient
                .prompt()
                .system(s -> s.text(SYSTEM_PROMPT).params(Map.of("documents", documents)))
                .user(message)
                .call()
                .content();
    }

}
