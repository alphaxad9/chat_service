// src/main/java/com/example/chat_service/infrastructure/security/JWTVerifier.java
package com.example.chat_service.infrastructure.security;

import com.example.chat_service.config.AuthConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Component
public class JWTVerifier {

    private static final Logger logger = LoggerFactory.getLogger(JWTVerifier.class);

    private final HttpClient httpClient;
    private final long cacheTtlSeconds;
    
    private PublicKey cachedKey;
    private Instant lastFetchTime;

    public JWTVerifier(AuthConfig authConfig) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.cacheTtlSeconds = authConfig.getAuthPublicKeyTTL();
    }

    public synchronized PublicKey getPublicKey(String publicKeyUrl) throws Exception {
        Instant now = Instant.now();
        
        if (cachedKey != null && lastFetchTime != null) {
            long elapsed = Duration.between(lastFetchTime, now).getSeconds();
            if (elapsed < cacheTtlSeconds) {
                return cachedKey;
            }
            logger.debug("Public key cache expired, refreshing from {}", publicKeyUrl);
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(publicKeyUrl))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to fetch public key: HTTP " + response.statusCode() + 
                                         " from " + publicKeyUrl);
            }

            String pem = response.body().trim();
            if (pem.isEmpty()) {
                throw new IllegalArgumentException("Empty public key response from " + publicKeyUrl);
            }
            
            String keyContent = pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");

            byte[] decoded = Base64.getDecoder().decode(keyContent);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
            KeyFactory factory = KeyFactory.getInstance("RSA");
            
            cachedKey = factory.generatePublic(spec);
            lastFetchTime = now;
            
            logger.debug("Public key refreshed successfully");
            return cachedKey;
            
        } catch (IOException e) {
            logger.error("Network error fetching public key from {}: {}", publicKeyUrl, e.getMessage());
            throw new RuntimeException("Network error fetching public key", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while fetching public key from {}", publicKeyUrl);
            throw new RuntimeException("Interrupted while fetching public key", e);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid public key format from {}: {}", publicKeyUrl, e.getMessage());
            throw new RuntimeException("Invalid public key format", e);
        } catch (Exception e) {
            logger.error("Unexpected error fetching public key from {}: {}", publicKeyUrl, e.getMessage(), e);
            throw e;
        }
    }

    public Claims verify(String token, String publicKeyUrl) throws Exception {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token cannot be empty");
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT format: expected 3 parts, got " + parts.length);
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