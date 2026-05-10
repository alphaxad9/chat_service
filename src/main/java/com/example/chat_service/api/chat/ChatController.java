package com.example.chat_service.api.chat;

import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    
    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "chat_service");
        response.put("java_version", System.getProperty("java.version"));
        return response;
    }
    
    @PostMapping("/message")
    public Map<String, String> sendMessage(@RequestBody Map<String, String> message) {
        Map<String, String> response = new HashMap<>();
        response.put("received", message.get("content"));
        response.put("status", "Message sent successfully");
        return response;
    }
}
