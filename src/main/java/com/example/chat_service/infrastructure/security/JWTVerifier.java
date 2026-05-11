// src/main/java/com/example/chat_service/infrastructure/security/JWTVerifier.java
package com.example.chat_service.infrastructure.security;

import com.example.chat_service.config.AuthConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;

@Component
public class JWTVerifier {

    private final java.net.http.HttpClient httpClient;
    private final long cacheTtlSeconds;
    
    private PublicKey cachedKey;
    private Instant lastFetchTime;

    public JWTVerifier(AuthConfig authConfig) {
        this.httpClient = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(5))
                .build();
        this.cacheTtlSeconds = authConfig.getAuthPublicKeyTTL();
    }

    public synchronized PublicKey getPublicKey(String publicKeyUrl) throws Exception {
        Instant now = Instant.now();
        
        if (cachedKey != null && lastFetchTime != null) {
            long elapsed = java.time.Duration.between(lastFetchTime, now).getSeconds();
            if (elapsed < cacheTtlSeconds) {
                return cachedKey;
            }
        }

        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(publicKeyUrl))
                .timeout(java.time.Duration.ofSeconds(5))
                .GET()
                .build();

        java.net.http.HttpResponse<String> response = httpClient.send(request, 
                java.net.http.HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to fetch public key: HTTP " + response.statusCode());
        }

        String pem = response.body().trim();
        String keyContent = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] decoded = Base64.getDecoder().decode(keyContent);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        
        cachedKey = factory.generatePublic(spec);
        lastFetchTime = now;
        
        return cachedKey;
    }

    public Claims verify(String token, String publicKeyUrl) throws Exception {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token cannot be empty");
        }

        if (token.split("\\.").length != 3) {
            throw new IllegalArgumentException("Invalid JWT format");
        }

        PublicKey key = getPublicKey(publicKeyUrl);

        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        if (!claims.containsKey("user_id")) {
            throw new IllegalArgumentException("Token missing required claim: user_id");
        }

        return claims;
    }
}