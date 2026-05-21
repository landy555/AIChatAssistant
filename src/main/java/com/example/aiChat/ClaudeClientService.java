package com.example.aiChat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ClaudeClientService {

    private static final String API_URL = "https://api.siliconflow.cn/v1/chat/completions";
    private static final String MODEL = "Qwen/Qwen2.5-7B-Instruct";

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public ClaudeClientService(@Value("${silicon.api.key:}") String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
    }

    public String chat(List<Map<String, String>> messages, String systemPrompt) throws Exception {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("Missing API key. Set silicon.api.key in application.properties");
        }

        List<Map<String, String>> fullMessages = new ArrayList<>();

        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        fullMessages.add(systemMsg);
        fullMessages.addAll(messages);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", MODEL);
        requestBody.put("max_tokens", 1024);
        requestBody.put("messages", fullMessages);

        String jsonBody = mapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

        HttpResponse<String> response = httpClient.send(
            request,
            HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            String responseBody = response.body();
            if (responseBody.length() > 500) {
                responseBody = responseBody.substring(0, 500) + "...";
            }
            throw new RuntimeException(
                "API request failed with status " + response.statusCode()
            );
        }

        return mapper.readTree(response.body())
            .path("choices").get(0)
            .path("message").path("content").asText();
    }
}