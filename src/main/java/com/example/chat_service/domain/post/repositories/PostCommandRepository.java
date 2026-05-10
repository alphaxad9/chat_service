// chat_service/src/main/java/com/example/chat_service/domain/post/repositories/PostCommandRepository.java

package com.example.chat_service.domain.post.repositories;

import java.util.Optional;
import java.util.UUID;

import com.example.chat_service.domain.post.PostAggregate;
import com.example.chat_service.domain.post.exceptions.PostNotFoundError;

/**
 * Interface for write operations on post aggregates.
 *
 * <p>All methods operate on full {@link PostAggregate} instances to preserve domain invariants.
 * Implementations are responsible for persistence, optimistic concurrency control (if used),
 * and ensuring aggregate state (including soft-delete flags) is durably stored.</p>
 *
 * <p>This is a domain-layer interface — infrastructure implementations (e.g., JPA, JDBC)
 * should reside in {@code infrastructure.persistence.posts.repositories}.</p>
 */
public interface PostCommandRepository {

    /**
     * Persist a post aggregate.
     *
     * <p>This method handles both creation and updates:
     * <ul>
     *   <li>If the post does not exist (new ID), it performs an INSERT.</li>
     *   <li>If it exists, it performs an UPDATE based on the post ID.</li>
     * </ul>
     *
     * <p>The aggregate must be fully validated before calling this method.
     * This includes ensuring media types are normalized, content is valid,
     * and metric constraints (e.g., non-negative counts) are met.</p>
     *
     * @param aggregate the post aggregate to persist
     */
    void save(PostAggregate aggregate);

    /**
     * Load an existing post aggregate by its unique ID.
     *
     * @param postId the unique identifier of the post
     * @return the loaded post aggregate
     * @throws PostNotFoundError if no post exists with the given ID
     *
     * <p>Used before applying any update command (e.g., delete, restore, update content).</p>
     */
    PostAggregate load(UUID postId);

    /**
     * Load a post aggregate by the author relationship.
     *
     * @param authorId the unique identifier of the author
     * @return the aggregate if found, otherwise {@link Optional#empty()}
     *
     * <p><strong>Note:</strong> Users may have multiple posts.
     * This method should return a specific matching post (e.g., the most recent)
     * or be used in contexts where only one relevant post is expected.
     * For fetching all posts by an author, use a query repository instead.</p>
     */
    Optional<PostAggregate> loadByAuthor(UUID authorId);

    /**
     * Check whether a post record exists for the given ID.
     *
     * @param postId the unique identifier of the post
     * @return {@code true} if a post with the given ID exists, {@code false} otherwise
     *
     * <p>Useful for fast validation before attempting to load or update.
     * Avoids loading full aggregate state when only existence matters.</p>
     */
    boolean exists(UUID postId);

    /**
     * Check specifically if a post by this author already exists.
     *
     * @param authorId the unique identifier of the author
     * @return {@code true} if any post matches the author, {@code false} otherwise
     *
     * <p>This is a specialized query. Since multiple posts are allowed per user,
     * this returns {@code true} if <em>any</em> post matches the criteria.
     * Useful for activity checks, onboarding flows, or rate-limiting without
     * loading full aggregate state.</p>
     */
    boolean existsByAuthor(UUID authorId);
}