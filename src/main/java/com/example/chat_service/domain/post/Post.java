// chat_service/src/main/java/com/example/chat_service/domain/post/Post.java
package com.example.chat_service.domain.post;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable domain model representing a Post aggregate.
 * Simple design focused on content + image with core metadata.
 * 
 * <p>Constructor is public to allow infrastructure mapping from persistence layer.
 * Validation is enforced in constructor, so instantiation is always safe.</p>
 */
public final class Post {

    private final UUID id;
    private final UUID authorId;
    
    // Core content fields (as requested)
    private final String content;        // Max 5000 chars
    private final String imageUrl;       // URL/path to image file (nullable)
    
    // Metadata
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final boolean isDeleted;

    // ── Constructor with validation ──────────────────────────────────
    /**
     * Public constructor for domain creation and infrastructure mapping.
     * All arguments are validated to ensure domain invariants.
     */
    public Post(UUID id, UUID authorId, String content, String imageUrl,
                LocalDateTime createdAt, LocalDateTime updatedAt, boolean isDeleted) {
        
        // Validate required fields
        if (id == null) throw new IllegalArgumentException("id cannot be null");
        if (authorId == null) throw new IllegalArgumentException("authorId cannot be null");
        if (content == null) throw new IllegalArgumentException("content cannot be null");
        if (content.length() > 5000) {
            throw new IllegalArgumentException("content cannot exceed 5000 characters");
        }
        if (content.isBlank()) {
            throw new IllegalArgumentException("content cannot be empty");
        }
        if (createdAt == null) throw new IllegalArgumentException("createdAt cannot be null");
        if (updatedAt == null) throw new IllegalArgumentException("updatedAt cannot be null");

        this.id = id;
        this.authorId = authorId;
        this.content = content;
        this.imageUrl = imageUrl; // nullable - no image is valid
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isDeleted = isDeleted;
    }

    // ── Factory Method ─────────────────────────────────────────────
    /**
     * Create a new Post with auto-generated timestamps and default state.
     * Use this for new posts created via application commands.
     */
    public static Post create(UUID id, UUID authorId, String content, String imageUrl) {
        LocalDateTime now = LocalDateTime.now();
        return new Post(id, authorId, content, imageUrl, now, now, false);
    }

    // ── Getters (no setters - immutable) ───────────────────────────
    public UUID id() { return id; }
    public UUID authorId() { return authorId; }
    public String content() { return content; }
    public String imageUrl() { return imageUrl; }
    public LocalDateTime createdAt() { return createdAt; }
    public LocalDateTime updatedAt() { return updatedAt; }
    public boolean isDeleted() { return isDeleted; }

    // ── State Queries ──────────────────────────────────────────────
    public boolean isValid() {
        return !isDeleted && content != null && !content.trim().isEmpty();
    }

    public boolean isActive() {
        return !isDeleted;
    }

    public boolean hasImage() {
        return imageUrl != null && !imageUrl.isBlank();
    }

    // ── State Transformers (return new instance) ───────────────────
    public Post withContent(String newContent) {
        if (newContent == null || newContent.isBlank()) {
            throw new IllegalArgumentException("content cannot be empty");
        }
        if (newContent.length() > 5000) {
            throw new IllegalArgumentException("content cannot exceed 5000 characters");
        }
        return new Post(this.id, this.authorId, newContent, this.imageUrl,
                       this.createdAt, LocalDateTime.now(), this.isDeleted);
    }

    public Post withImage(String newImageUrl) {
        // imageUrl can be null (removing image) or a valid path/URL
        return new Post(this.id, this.authorId, this.content, newImageUrl,
                       this.createdAt, LocalDateTime.now(), this.isDeleted);
    }

    public Post toggleDeletion() {
        return new Post(this.id, this.authorId, this.content, this.imageUrl,
                       this.createdAt, LocalDateTime.now(), !this.isDeleted);
    }

    public Post touch() {
        return new Post(this.id, this.authorId, this.content, this.imageUrl,
                       this.createdAt, LocalDateTime.now(), this.isDeleted);
    }

    // ── Standard Object Methods ────────────────────────────────────
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Post post)) return false;
        return Objects.equals(id, post.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Post{" +
                "id=" + id +
                ", authorId=" + authorId +
                ", contentLength=" + content.length() +
                ", hasImage=" + hasImage() +
                ", isDeleted=" + isDeleted +
                ", createdAt=" + createdAt +
                '}';
    }
}