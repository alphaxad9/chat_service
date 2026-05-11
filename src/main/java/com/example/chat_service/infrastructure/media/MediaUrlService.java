// src/main/java/com/example/chat_service/infrastructure/media/MediaUrlService.java

package com.example.chat_service.infrastructure.media;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;

/**
 * Service for building absolute media URLs from relative paths.
 *
 * <p>This service converts database-stored relative paths like
 * {@code /uploads/posts/abc.jpg} into frontend-ready absolute URLs
 * like {@code http://127.0.0.1:8005/uploads/posts/abc.jpg}.</p>
 *
 * <p><strong>Architecture benefit:</strong> The database stores environment-agnostic
 * relative paths, while the API layer dynamically builds absolute URLs based on
 * the incoming request. This allows seamless migration from localhost to
 * production domains without database changes.</p>
 *
 * <p><strong>Future enhancement:</strong> In production, this can be extended
 * to use a configured CDN base URL from application properties:</p>
 *
 * <pre>{@code
 * # application.yml
 * app:
 *   media:
 *     base-url: https://cdn.myapp.com
 * }</pre>
 *
 * <p>Then {@code buildMediaUrl()} would return:
 * {@code https://cdn.myapp.com/uploads/posts/abc.jpg}</p>
 */
@Service
public class MediaUrlService {

    /**
     * Convert a relative media path into an absolute URL based on the incoming request.
     *
     * <p><strong>Example transformation:</strong></p>
     * <pre>
     * Input relative path:  /uploads/posts/abc123.jpg
     * Request: GET http://127.0.0.1:8005/api/posts
     * Output absolute URL: http://127.0.0.1:8005/uploads/posts/abc123.jpg
     * </pre>
     *
     * @param request the incoming HTTP request (used to extract scheme, host, port)
     * @param relativePath the relative path stored in database, e.g. "/uploads/posts/abc.jpg"
     * @return absolute URL ready for frontend consumption, or null if relativePath is empty
     */
    public String buildMediaUrl(HttpServletRequest request, String relativePath) {

        // ─────────────────────────────────────────────
        // Handle null/empty paths gracefully
        // ─────────────────────────────────────────────
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }

        // ─────────────────────────────────────────────
        // Build base URL from request components
        // ─────────────────────────────────────────────
        String scheme = request.getScheme();              // "http" or "https"
        String serverName = request.getServerName();      // "127.0.0.1" or "api.myapp.com"
        int serverPort = request.getServerPort();         // 8005, 80, 443, etc.

        // ─────────────────────────────────────────────
        // Construct base URL (handle default ports cleanly)
        // ─────────────────────────────────────────────
        StringBuilder baseUrl = new StringBuilder();
        baseUrl.append(scheme).append("://").append(serverName);

        // Only append port if it's not the default for the scheme
        if ((scheme.equals("http") && serverPort != 80) ||
            (scheme.equals("https") && serverPort != 443)) {
            baseUrl.append(":").append(serverPort);
        }

        // ─────────────────────────────────────────────
        // Append relative path and return full URL
        // ─────────────────────────────────────────────
        return baseUrl.append(relativePath).toString();
    }

    /**
     * Optional convenience method: build URL using a configured base URL
     * instead of request-derived values.
     *
     * <p>Useful for background jobs, scheduled tasks, or when request context
     * is not available.</p>
     *
     * @param baseUrl the configured base URL, e.g. "https://cdn.myapp.com"
     * @param relativePath the relative path, e.g. "/uploads/posts/abc.jpg"
     * @return absolute URL, or null if relativePath is empty
     */
    public String buildMediaUrlWithBase(String baseUrl, String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        // Ensure baseUrl doesn't end with slash and relativePath starts with one
        String cleanBase = baseUrl.replaceAll("/+$", "");
        return cleanBase + relativePath;
    }
}