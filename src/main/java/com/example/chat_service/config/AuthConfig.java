// src/main/java/com/example/chat_service/config/AuthConfig.java
package com.example.chat_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthConfig {

    @Value("${auth.public-key-url}")
    private String authPublicKeyUrl;

    @Value("${auth.public-key-ttl:300}")
    private long authPublicKeyTTL;

    @Value("${auth.internal-api-key}")
    private String internalApiKey;

    @Value("${auth.service-url}")
    private String authServiceUrl;

    public String getAuthPublicKeyUrl() {
        return authPublicKeyUrl;
    }

    public long getAuthPublicKeyTTL() {
        return authPublicKeyTTL;
    }

    public String getInternalApiKey() {
        return internalApiKey;
    }

    public String getAuthServiceUrl() {
        return authServiceUrl;
    }
}