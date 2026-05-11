package com.example.chat_service.application.post.handlers.dtos;

import com.example.chat_service.domain.post.Post;
import com.example.chat_service.domain.post.PostAggregate;
import com.example.chat_service.external.users.dtos.UserView;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object representing a Post for API/UI consumption.
 * 
 * Combines:
 * - PostAggregate (domain logic + state)
 * - UserView (enriched author data from external Auth Service)
 * 
 * This decouples the internal domain model from external representation
 * and allows attaching resolved user data without leaking domain internals.
 * 
 * Usage:
 *   PostResponseDTO dto = PostResponseDTO.fromAggregate(postAggregate, authorUserView);
 *   return ResponseEntity.ok(dto); // Jackson auto-serializes to JSON
 */
public record PostResponseDTO(

        @JsonProperty("post_id")
        UUID postId,

        @JsonProperty("author")
        UserView author,

        String content,

        @JsonProperty("image_url")
        String imageUrl,

        @JsonProperty("created_at")
        LocalDateTime createdAt,

        @JsonProperty("updated_at")
        LocalDateTime updatedAt,

        @JsonProperty("is_deleted")
        boolean isDeleted,

        @JsonProperty("has_image")
        boolean hasImage,

        @JsonProperty("is_active")
        boolean isActive

) {

    /**
     * Factory method to create a PostResponseDTO by combining:
     * - PostAggregate (domain state)
     * - UserView (resolved author data from external service)
     * 
     * @param aggregate the PostAggregate containing domain state
     * @param author the resolved UserView from Auth Service
     * @return enriched PostResponseDTO ready for API response
     */
    public static PostResponseDTO fromAggregate(PostAggregate aggregate, UserView author) {
        Post post = aggregate.post();
        
        return new PostResponseDTO(
                post.id(),
                author,
                post.content(),
                post.imageUrl(),
                post.createdAt(),
                post.updatedAt(),
                post.isDeleted(),
                post.hasImage(),
                post.isActive()
        );
    }

    /**
     * Convenience factory for testing or when author data is not yet resolved.
     * Creates a placeholder author with minimal data.
     * 
     * ⚠️ Use only for internal/testing scenarios — prefer fromAggregate() in production.
     */
    public static PostResponseDTO fromAggregateWithPlaceholderAuthor(PostAggregate aggregate) {
        Post post = aggregate.post();
        
        UserView placeholderAuthor = new UserView(
                post.authorId(),
                "user_" + post.authorId().toString().substring(0, 8),
                null,   // email
                null,   // firstName
                null,   // lastName
                null    // profilePicture
        );
        
        return fromAggregate(aggregate, placeholderAuthor);
    }

    /**
     * Convert to Map for logging, testing, or manual serialization.
     * Not required for Spring MVC responses (Jackson handles records automatically),
     * but useful for debugging or non-JSON use cases.
     */
    public java.util.Map<String, Object> toMap() {
        return java.util.Map.of(
                "post_id", postId.toString(),
                "author", userViewToMap(author),  // ✅ FIXED: use helper method
                "content", content,
                "image_url", imageUrl,
                "created_at", createdAt.toString(),
                "updated_at", updatedAt.toString(),
                "is_deleted", isDeleted,
                "has_image", hasImage,
                "is_active", isActive
        );
    }

    /**
     * Nested helper to convert UserView to Map (since UserView is a record).
     * Delegates to UserView's own toMap() if it exists, otherwise builds manually.
     */
    private static java.util.Map<String, Object> userViewToMap(UserView user) {
        if (user == null) return null;
        
        // If UserView has a toMap() method, use it; else build manually
        try {
            var method = UserView.class.getMethod("toMap");
            return (java.util.Map<String, Object>) method.invoke(user);
        } catch (Exception e) {
            // Fallback manual mapping
            return java.util.Map.of(
                    "user_id", user.userId().toString(),
                    "username", user.username(),
                    "email", user.email(),
                    "first_name", user.firstName(),
                    "last_name", user.lastName(),
                    "profile_picture", user.profilePicture()
            );
        }
    }
}