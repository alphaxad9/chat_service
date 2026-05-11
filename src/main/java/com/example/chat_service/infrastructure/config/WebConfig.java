// src/main/java/com/example/chat_service/infrastructure/config/WebConfig.java

package com.example.chat_service.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC configuration for static resource handling.
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