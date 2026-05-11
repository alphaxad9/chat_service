package com.example.chat_service.api.chat.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for creating a new post via HTTP API.
 *
 * <p>Maps JSON request body to Java record. Jackson automatically deserializes
 * snake_case JSON fields to camelCase Java fields via @JsonProperty.</p>
 *
 * <p>Example incoming JSON:
 * <pre>{@code
 * {
 *   "content": "Hello world!",
 *   "image_url": "https://example.com/image.jpg"
 * }
 * }</pre>
 * </p>
 *
 * <p><strong>Validation note:</strong> Field validation (non-null, length, etc.)
 * is enforced in the domain layer (PostAggregate), not here. This DTO is purely
 * for transport. The handler will catch domain exceptions and map to HTTP errors.</p>
 */
public record CreatePostRequest(

        @JsonProperty("content")
        String content,

        @JsonProperty("image_url")
        String imageUrl

) {
    // Records are immutable by design — no need for setters or builders.
    // Jackson will use the canonical constructor for deserialization.
}