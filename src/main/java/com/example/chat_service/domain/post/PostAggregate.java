// chat_service/src/main/java/com/example/chat_service/domain/post/PostAggregate.java
package com.example.chat_service.domain.post;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

// ← Imports for exceptions in sub-package
import com.example.chat_service.domain.post.exceptions.InvalidPostContentError;
import com.example.chat_service.domain.post.exceptions.InvalidPostEntityError;
import com.example.chat_service.domain.post.exceptions.PostOperationNotAllowedError;
import com.example.chat_service.domain.post.exceptions.PostStateTransitionError;

import static java.util.Objects.requireNonNull;

/**
 * Aggregate root for managing the lifecycle and state of a Post.
 * Enforces business rules, coordinates state transitions, and guards operations.
 */
public final class PostAggregate {

    private Post post; // Mutable reference to current state; Post itself is immutable

    private PostAggregate(Post post) {
        this.post = requireNonNull(post, "post cannot be null");
    }

    // ── Accessors ─────────────────────────────────────────────────────
    public Post post() {
        return post;
    }

    // ── Factory Methods ──────────────────────────────────────────────

    /**
     * Create an aggregate from an existing Post entity (e.g., loaded from repository).
     */
    public static PostAggregate fromEntity(Post post) {
        return new PostAggregate(post);
    }

    /**
     * Create a new post aggregate with validation.
     * Fails fast on invalid input before entity creation.
     */
    public static PostAggregate createNew(
            UUID id,
            UUID authorId,
            String content,
            String imageUrl,
            LocalDateTime createdAt
    ) {
        // Validate IDs
        if (id == null) {
            throw new InvalidPostEntityError(null, authorId, "Post ID cannot be null");
        }
        if (authorId == null) {
            throw new InvalidPostEntityError(id, null, "Author ID cannot be null");
        }

        // Validate content before entity creation
        validateContent(content);

        // imageUrl is optional - no validation needed beyond null-safety in Post constructor
        Post newPost = Post.create(id, authorId, content, imageUrl);
        return new PostAggregate(newPost);
    }

    /**
     * Convenience overload using current timestamp.
     */
    public static PostAggregate createNew(
            UUID id,
            UUID authorId,
            String content,
            String imageUrl
    ) {
        return createNew(id, authorId, content, imageUrl, null);
    }

    // ── Business Operations ──────────────────────────────────────────

    /**
     * Soft-delete the post (mark as inactive).
     */
    public PostAggregate delete() {
        ensureActive("delete");
        
        if (post.isDeleted()) {
            throw new PostStateTransitionError(
                post.isDeleted(),
                true,
                "Post is already deleted"
            );
        }
        
        this.post = post.toggleDeletion();
        return this;
    }

    /**
     * Restore a soft-deleted post (mark as active).
     */
    public PostAggregate restore() {
        // Do NOT call ensureActive - we expect post to be deleted
        if (!post.isDeleted()) {
            throw new PostStateTransitionError(
                post.isDeleted(),
                false,
                "Post is already active"
            );
        }
        
        this.post = post.toggleDeletion();
        return this;
    }

    /**
     * Update the post content with validation.
     */
    public PostAggregate updateContent(String newContent) {
        ensureActive("update_content");
        validateContent(newContent);
        
        this.post = post.withContent(newContent);
        return this;
    }

    /**
     * Update or remove the post image.
     * @param newImageUrl URL/path to image, or null to remove image
     */
    public PostAggregate updateImage(String newImageUrl) {
        ensureActive("update_image");
        // imageUrl can be null (removing image) - no additional validation
        this.post = post.withImage(newImageUrl);
        return this;
    }

    /**
     * Update the updated_at timestamp (e.g., for cache invalidation).
     */
    public PostAggregate touch() {
        ensureActive("touch");
        this.post = post.touch();
        return this;
    }

    /**
     * Permanently mark aggregate as deleted (for hard-delete workflows).
     * Use with caution - typically handled at repository level.
     */
    public PostAggregate markForPermanentDeletion() {
        ensureActive("markForPermanentDeletion");
        this.post = post.toggleDeletion(); // Soft delete first
        return this;
    }

    // ── State Queries (delegated to Post) ────────────────────────────

    public boolean isValid() {
        return post.isValid();
    }

    public boolean isActive() {
        return post.isActive();
    }

    public boolean hasImage() {
        return post.hasImage();
    }

    // ── Helper Methods ───────────────────────────────────────────────

    private void ensureActive(String operation) {
        if (!post.isActive()) {
            throw new PostOperationNotAllowedError(
                post.id(),
                operation,
                "Post is inactive or deleted"
            );
        }
    }

    private static void validateContent(String content) {
        if (content == null) {
            throw new InvalidPostContentError(null, "Content cannot be null", 5000);
        }
        if (content.isBlank()) {
            throw new InvalidPostContentError(content, "Content cannot be empty or whitespace only", 5000);
        }
        if (content.length() > 5000) {
            throw new InvalidPostContentError(content, "Content exceeds maximum length", 5000);
        }
    }

    // ── Standard Object Methods ──────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PostAggregate that)) return false;
        return Objects.equals(post.id(), that.post.id());
    }

    @Override
    public int hashCode() {
        return Objects.hash(post.id());
    }

    @Override
    public String toString() {
        return "PostAggregate{" +
                "id=" + post.id() +
                ", authorId=" + post.authorId() +
                ", hasImage=" + hasImage() +
                ", isActive=" + isActive() +
                ", createdAt=" + post.createdAt() +
                '}';
    }
}