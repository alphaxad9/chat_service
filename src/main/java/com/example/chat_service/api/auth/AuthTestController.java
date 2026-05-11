// src/main/java/com/example/chat_service/api/auth/AuthTestController.java
package com.example.chat_service.api.auth;

import com.example.chat_service.infrastructure.security.UserContext;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthTestController {

    @GetMapping("/test")
    public Map<String, Object> testAuth() {
        return Map.of("user_id_from_jwt", UserContext.getUserId());
    }

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("status", "ok", "service", "chat_service");
    }
}