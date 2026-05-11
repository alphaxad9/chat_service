// chat_service/src/main/java/com/example/chat_service/config/AuthProperties.java

package com.example.chat_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed configuration properties for external service communication.
 * 
 * Binds to application.yml properties under 'auth.' prefix.
 * Used specifically by UserApiClient and other service-to-service clients.
 * 
 * Example usage:
 *   String url = authProperties.serviceUrl();
 */
@ConfigurationProperties(prefix = "auth")
public record AuthProperties(

        String serviceUrl,
        String internalApiKey,
        String publicKeyUrl,
        Long publicKeyTtl

) {}