// src/main/java/com/example/chat_service/infrastructure/security/UserContext.java
package com.example.chat_service.infrastructure.security;

import java.util.Optional;
import java.util.UUID;

/**
 * Thread-local holder for the authenticated user ID.
 * Replaces Go's context.WithValue / Django's request.user_id pattern.
 * 
 * Usage:
 *   - Set in filter/middleware: UserContext.setUserId("uuid-string")
 *   - Access anywhere: String id = UserContext.getUserId()
 *   - Always clear in finally block to prevent memory leaks
 */
public class UserContext {

    private static final ThreadLocal<String> currentUser = new ThreadLocal<>();

    private UserContext() {
        // Prevent instantiation - utility class only
    }

    /**
     * Store the authenticated user ID for the current request thread.
     * @param userId the user ID as a string (UUID format recommended)
     */
    public static void setUserId(String userId) {
        currentUser.set(userId);
    }

    /**
     * Retrieve the authenticated user ID for the current request thread.
     * @return the user ID string, or null if not set
     */
    public static String getUserId() {
        return currentUser.get();
    }

    /**
     * Retrieve the authenticated user ID as a UUID, if valid.
     * @return Optional containing the UUID, or empty if not set/invalid
     */
    public static Optional<UUID> getUserIdAsUuid() {
        String id = currentUser.get();
        if (id == null || id.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Check if a user ID is set for the current thread.
     * @return true if authenticated, false otherwise
     */
    public static boolean isAuthenticated() {
        String id = currentUser.get();
        return id != null && !id.isEmpty();
    }

    /**
     * Clear the user ID for the current thread.
     * MUST be called after request processing to prevent memory leaks
     * in thread-pooled environments (Tomcat, Undertow, etc.).
     */
    public static void clear() {
        currentUser.remove();
    }
}