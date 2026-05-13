// src/main/java/com/example/chat_service/infrastructure/config/WebConfig.java

package com.example.chat_service.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Spring MVC configuration for static resource handling and CORS.
 *
 * <p>This configuration exposes the local file upload directory
 * as a public HTTP endpoint, allowing uploaded images to be
 * served directly by the Spring Boot application.</p>
 *
 * <p><strong>URL Mapping:</strong>
 * <ul>
 *   <li>Request: {@code GET /uploads/posts/abc123.jpg}</li>
 *   <li>Resolved to: {@code file:uploads/posts/abc123.jpg}</li>
 * </ul>
 * </p>
 *
 * <p><strong>Security note:</strong> This exposes files in {@code uploads/}
 * publicly. Ensure:
 * <ul>
 *   <li>Uploaded files are validated (MIME type, size, extension)</li>
 *   <li>Filenames are sanitized (we use UUIDs to prevent path traversal)</li>
 *   <li>Consider adding authentication for sensitive media in production</li>
 * </ul>
 * </p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Register resource handlers to serve uploaded media files.
     *
     * @param registry the ResourceHandlerRegistry to configure
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // ─────────────────────────────────────────────
        // Map HTTP requests to local filesystem
        // ─────────────────────────────────────────────

        registry
            // URL pattern that clients will request
            .addResourceHandler("/uploads/**")
            // Physical location on filesystem (note the "file:" prefix)
            // "file:uploads/" means: look in ./uploads/ relative to app root
            .addResourceLocations("file:uploads/")
            // Optional: set cache period (in seconds) for browser caching
            // .setCachePeriod(3600) // 1 hour
            ;

        // ─────────────────────────────────────────────
        // Keep default static resource handlers for /static/, /public/, etc.
        // Spring Boot auto-configures these, so we don't override them.
        // ─────────────────────────────────────────────
    }

    /**
     * CORS filter configuration to allow frontend requests from localhost:3000.
     *
     * <p>This enables:
     * <ul>
     *   <li>Cross-origin requests from the React frontend</li>
     *   <li>Credentials (cookies/JWT tokens) to be sent with requests</li>
     *   <li>Authorization headers for authenticated endpoints</li>
     * </ul>
     * </p>
     *
     * @return configured CorsFilter bean
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // FRONTEND URL — update if your frontend runs on a different origin
        config.setAllowedOrigins(List.of(
                "http://localhost:3000"
        ));

        // ALLOW COOKIES / JWT TOKENS TO BE SENT
        config.setAllowCredentials(true);

        // ALLOW HEADERS — include Authorization for JWT
        config.setAllowedHeaders(List.of(
                "Origin",
                "Content-Type",
                "Accept",
                "Authorization",
                "X-Requested-With",
                "X-CSRF-TOKEN"
        ));

        // ALLOW HTTP METHODS
        config.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        // EXPOSE HEADERS — let frontend read these from responses
        config.setExposedHeaders(List.of(
                "Authorization",
                "Set-Cookie"
        ));

        // MAX AGE — cache preflight responses (optional)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }

    /**
     * Optional: Add more resource handlers here if needed.
     *
     * Example: Serve avatars from a separate directory
     *
     * <pre>{@code
     * registry
     *     .addResourceHandler("/avatars/**")
     *     .addResourceLocations("file:uploads/avatars/");
     * }</pre>
     */
}