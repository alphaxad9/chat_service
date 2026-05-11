package com.example.chat_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * HTTP client configuration for service-to-service communication.
 * 
 * Provides a singleton RestClient bean that can be injected
 * into any service needing to make external HTTP calls.
 * 
 * Future enhancements:
 * - Add timeout configuration
 * - Add retry logic via ClientHttpRequestInterceptor
 * - Add logging interceptor for debugging
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public RestClient restClient() {
        return RestClient.builder().build();
    }
}