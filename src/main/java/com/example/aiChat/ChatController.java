package com.example.aiChat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private static final String SYSTEM_PROMPT =
        "You are a friendly, professional AI assistant. Please answer in clear, concise Chinese."
            + " If the question is about programming, include practical examples when helpful.";

    private final ConcurrentHashMap<String, List<Map<String, String>>> sessionHistories = new ConcurrentHashMap<>();

    @Autowired
    private ClaudeClientService claudeClient;

    @PostMapping("/send")
    public Map<String, Object> sendMessage(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();

        String sessionId = request.getOrDefault("sessionId", "default");
        String userMessage = request.get("message");

        if (userMessage == null || userMessage.trim().isEmpty()) {
            response.put("success", false);
            response.put("error", "Message cannot be empty");
            return response;
        }

        String cmd = userMessage.toLowerCase().trim();
        if (cmd.equals("quit") || cmd.equals("exit")) {
            response.put("success", true);
            response.put("type", "exit");
            response.put("message", "Goodbye!");
            sessionHistories.remove(sessionId);
            return response;
        }

        if (cmd.equals("clear")) {
            sessionHistories.remove(sessionId);
            response.put("success", true);
            response.put("type", "clear");
            response.put("message", "History cleared");
            return response;
        }

        List<Map<String, String>> history = sessionHistories.computeIfAbsent(sessionId, k -> new ArrayList<>());

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        history.add(userMsg);

        try {
            String reply = claudeClient.chat(history, SYSTEM_PROMPT);

            Map<String, String> aiMsg = new HashMap<>();
            aiMsg.put("role", "assistant");
            aiMsg.put("content", reply);
            history.add(aiMsg);

            response.put("success", true);
            response.put("message", reply);
            response.put("type", "message");
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Request failed: " + e.getMessage());
            synchronized (history) {
                if (!history.isEmpty()) {
                    history.remove(history.size() - 1);
                }
            }
        }

        return response;
    }

    @GetMapping("/history/{sessionId}")
    public Map<String, Object> getHistory(@PathVariable String sessionId) {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, String>> history = sessionHistories.getOrDefault(sessionId, new ArrayList<>());

        response.put("success", true);
        response.put("history", history);
        return response;
    }

    @DeleteMapping("/history/{sessionId}")
    public Map<String, Object> clearHistory(@PathVariable String sessionId) {
        Map<String, Object> response = new HashMap<>();
        sessionHistories.remove(sessionId);

        response.put("success", true);
        response.put("message", "History cleared");
        return response;
    }
}