// chat_service/src/main/java/com/example/chat_service/application/post/services/PostCommandServiceInterface.java

package com.example.chat_service.application.post.services;

import java.util.Optional;
import java.util.UUID;

import com.example.chat_service.domain.post.PostAggregate;

/**
 * Application-layer interface for post command (write) operations.
 *
 * <p>Orchestrates business logic and coordinates domain aggregates with infrastructure
 * repositories. All methods operate on {@link PostAggregate} to preserve domain invariants.</p>
 *
 * <p><strong>Design principles:</strong>
 * <ul>
 *   <li>Methods accept/return aggregates — never expose entities or DTOs at this layer</li>
 *   <li>Validation and business rules live in the domain (aggregate), not here</li>
 *   <li>Infrastructure concerns (persistence, caching) are delegated to repositories</li>
 *   <li>Transaction boundaries should be applied at the implementation level</li>
 * </ul></p>
 */
public interface PostCommandServiceInterface {

    // ── Core Lifecycle Commands ────────────────────────────────────────

    /**
     * Create a new post aggregate and persist it.
     *
     * @param aggregate the validated post aggregate to create
     * @return the persisted aggregate (with assigned timestamps, etc.)
     * @throws com.example.chat_service.domain.post.exceptions.PostAlreadyExistsError
     *         if a post with the same ID already exists
     */
    PostAggregate createPost(PostAggregate aggregate);

    /**
     * Soft-delete an existing post aggregate.
     *
     * @param aggregate the loaded aggregate to delete
     * @return the updated aggregate with {@code isDeleted = true}
     * @throws com.example.chat_service.domain.post.exceptions.PostOperationNotAllowedError
     *         if the post is already deleted or inactive
     */
    PostAggregate deletePost(PostAggregate aggregate);

    /**
     * Restore a soft-deleted post aggregate.
     *
     * @param aggregate the loaded aggregate to restore
     * @return the updated aggregate with {@code isDeleted = false}
     * @throws com.example.chat_service.domain.post.exceptions.PostOperationNotAllowedError
     *         if the post is already active
     */
    PostAggregate restorePost(PostAggregate aggregate);

    // ── Content & Media Commands ───────────────────────────────────────

    /**
     * Update the content of an existing post.
     *
     * @param aggregate the loaded aggregate to update
     * @param newContent the new content string (validated by domain)
     * @return the updated aggregate with new content and refreshed {@code updatedAt}
     * @throws com.example.chat_service.domain.post.exceptions.InvalidPostContentError
     *         if content violates domain rules (null, empty, too long)
     */
    PostAggregate updateContent(PostAggregate aggregate, String newContent);

    /**
     * Update or remove the image URL of an existing post.
     *
     * @param aggregate the loaded aggregate to update
     * @param newImageUrl the new image URL/path, or {@code null} to remove image
     * @return the updated aggregate with new image reference and refreshed {@code updatedAt}
     */
    PostAggregate updateImage(PostAggregate aggregate, String newImageUrl);

    /**
     * Touch the aggregate to refresh its {@code updatedAt} timestamp.
     *
     * <p>Useful for cache invalidation, activity tracking, or forcing persistence
     * without changing business-relevant state.</p>
     *
     * @param aggregate the loaded aggregate to touch
     * @return the updated aggregate with refreshed {@code updatedAt}
     */
    PostAggregate touch(PostAggregate aggregate);

    // ── Query Support Methods (for command orchestration) ──────────────

    /**
     * Load a post aggregate by its unique ID for mutation.
     *
     * @param postId the UUID of the post to load
     * @return the loaded aggregate ready for business operations
     * @throws com.example.chat_service.domain.post.exceptions.PostNotFoundError
     *         if no active post exists with the given ID
     */
    PostAggregate loadAggregate(UUID postId);

    /**
     * Load a post aggregate by author relationship (most recent).
     *
     * <p><strong>Note:</strong> Users may have multiple posts. This returns the
     * most recent active post for the author, or empty if none found.</p>
     *
     * @param authorId the UUID of the author
     * @return {@link Optional} containing the aggregate if found, empty otherwise
     */
    Optional<PostAggregate> loadAggregateByAuthor(UUID authorId);

    /**
     * Check if a post aggregate exists by ID (fast existence check).
     *
     * @param postId the UUID of the post
     * @return {@code true} if an active post exists with the given ID
     */
    boolean aggregateExists(UUID postId);

    /**
     * Check if any post aggregate exists for the given author.
     *
     * @param authorId the UUID of the author
     * @return {@code true} if author has at least one active post
     */
    boolean aggregateExistsByAuthor(UUID authorId);
}