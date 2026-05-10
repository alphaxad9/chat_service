// chat_service/src/main/java/com/example/chat_service/infrastructure/persistence/posts/PostEntity.java
package com.example.chat_service.infrastructure.persistence.posts;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.SQLRestriction;  // Hibernate 6+ replacement for @Where

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity for Post persistence.
 * Maps the simple domain Post model to database schema.
 * Uses soft-delete pattern with @SQLRestriction filter for automatic query filtering.
 * 
 * Note: Soft-delete is handled via repository methods, not @SQLDelete, for Hibernate 7.x compatibility.
 */
@Entity
@Table(
    name = "posts",
    indexes = {
        @Index(name = "idx_posts_author", columnList = "author_id"),
        @Index(name = "idx_posts_deleted", columnList = "is_deleted"),
        @Index(name = "idx_posts_created", columnList = "created_at"),
        @Index(name = "idx_posts_updated", columnList = "updated_at"),
        // Composite: active posts by author (common query pattern)
        @Index(name = "idx_posts_author_active", columnList = "author_id, is_deleted"),
        // Composite: trending active posts
        @Index(name = "idx_posts_active_created", columnList = "is_deleted, created_at")
    }
)
// Hibernate 6+: Auto-filter deleted rows in queries (replaces @Where)
@SQLRestriction("is_deleted = false")
public class PostEntity {

    @Id
    @Column(columnDefinition = "UUID", updatable = false)
    private UUID id;

    @Column(name = "author_id", nullable = false, columnDefinition = "UUID")
    private UUID authorId;

    @Column(nullable = false, length = 5000)
    private String content;

    // Optional image field - stored as URL/path string
    @Column(name = "image_url", length = 2048)
    private String imageUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    // ── JPA Required Default Constructor ─────────────────────────────
    protected PostEntity() {
        // For JPA/Hibernate only
    }

    // ── Constructor for Domain Mapping ───────────────────────────────
    public PostEntity(UUID id, UUID authorId, String content, String imageUrl) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.authorId = Objects.requireNonNull(authorId, "authorId cannot be null");
        this.content = Objects.requireNonNull(content, "content cannot be null");
        validateContent(content);
        this.imageUrl = imageUrl; // nullable
        // createdAt/updatedAt set by @CreationTimestamp/@UpdateTimestamp
        this.isDeleted = false;
    }

    // ── Getters (JPA uses field access, but getters useful for mapping) ─
    public UUID getId() { return id; }
    public UUID getAuthorId() { return authorId; }
    public String getContent() { return content; }
    public String getImageUrl() { return imageUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public boolean isDeleted() { return isDeleted; }

    // ── Setters for JPA/Hibernate (package-private for controlled access) ─
    void setContent(String content) {
        validateContent(content);
        this.content = content;
    }

    void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * Mark entity as deleted (soft-delete).
     * Call this in repository before save() to persist the deletion.
     */
    void markDeleted() {
        this.isDeleted = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Restore entity from soft-delete state.
     */
    void restore() {
        this.isDeleted = false;
        this.updatedAt = LocalDateTime.now();
    }

    // ── Validation Helpers ───────────────────────────────────────────
    private static void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content cannot be empty");
        }
        if (content.length() > 5000) {
            throw new IllegalArgumentException("content cannot exceed 5000 characters");
        }
    }

    // ── Domain Conversion Methods ────────────────────────────────────

    /**
     * Convert domain Post → JPA Entity
     */
    public static PostEntity fromDomain(com.example.chat_service.domain.post.Post domain) {
        PostEntity entity = new PostEntity();
        entity.id = domain.id();
        entity.authorId = domain.authorId();
        entity.content = domain.content();
        entity.imageUrl = domain.imageUrl();
        entity.createdAt = domain.createdAt();
        entity.updatedAt = domain.updatedAt();
        entity.isDeleted = domain.isDeleted();
        return entity;
    }

    /**
     * Convert JPA Entity → domain Post
     * Package-private to restrict access to infrastructure layer only.
     */
    com.example.chat_service.domain.post.Post toDomain() {
        return new com.example.chat_service.domain.post.Post(
            this.id,
            this.authorId,
            this.content,
            this.imageUrl,
            this.createdAt,
            this.updatedAt,
            this.isDeleted
        );
    }

    // ── Standard Object Methods ──────────────────────────────────────
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PostEntity that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PostEntity{" +
                "id=" + id +
                ", authorId=" + authorId +
                ", contentLength=" + (content != null ? content.length() : 0) +
                ", hasImage=" + (imageUrl != null && !imageUrl.isBlank()) +
                ", isDeleted=" + isDeleted +
                ", createdAt=" + createdAt +
                '}';
    }
}