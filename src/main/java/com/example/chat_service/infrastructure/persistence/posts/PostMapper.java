// com/example/chat_service/infrastructure/persistence/posts/PostMapper.java
package com.example.chat_service.infrastructure.persistence.posts;

import com.example.chat_service.domain.post.Post;
import com.example.chat_service.domain.post.PostAggregate;

/**
 * Handles mapping between domain aggregates, JPA entities, and domain value objects.
 *
 * <ul>
 *   <li>{@link #aggregateToEntity(PostAggregate)}: PostAggregate → PostEntity (for persistence)</li>
 *   <li>{@link #entityToAggregate(PostEntity)}: PostEntity → PostAggregate (for command loading)</li>
 *   <li>{@link #entityToDomain(PostEntity)}: PostEntity → Post (for query responses)</li>
 * </ul>
 *
 * <p>Keeps domain logic pure by isolating persistence concerns in infrastructure layer.
 * All methods are static utilities — no state, no dependencies.</p>
 */
public final class PostMapper {

    // Prevent instantiation
    private PostMapper() {
        throw new AssertionError("Utility class - do not instantiate");
    }

    /**
     * Convert a write-side PostAggregate into a JPA-persistable entity.
     * Used by command repositories to save aggregate state after business operations.
     *
     * @param aggregate the domain aggregate containing current state
     * @return JPA entity ready for persistence
     */
    public static PostEntity aggregateToEntity(PostAggregate aggregate) {
        Post post = aggregate.post();
        
        return new PostEntity(
            post.id(),
            post.authorId(),
            post.content(),
            post.imageUrl()
        );
        // Note: createdAt/updatedAt/isDeleted are managed by:
        // - @CreationTimestamp / @UpdateTimestamp annotations
        // - @SQLDelete for soft-delete
        // If loading existing entity, use entityToAggregate then save (JPA merge pattern)
    }

    /**
     * Reconstruct a write-side PostAggregate from a JPA entity.
     * Used by command repository's load() method to hydrate aggregate for mutation.
     *
     * @param entity the persisted JPA entity
     * @return PostAggregate ready for business operations
     */
    public static PostAggregate entityToAggregate(PostEntity entity) {
        Post domain = entityToDomain(entity);
        return PostAggregate.fromEntity(domain);
    }

    /**
     * Convert a JPA entity into the immutable domain value object.
     * Used exclusively by query services to return clean, serializable data.
     *
     * @param entity the persisted JPA entity
     * @return immutable Post domain object
     */
    public static Post entityToDomain(PostEntity entity) {
        // Use Post constructor directly since we're mapping from trusted persistence layer
        // Validation already enforced at domain creation time
        return new Post(
            entity.getId(),
            entity.getAuthorId(),
            entity.getContent(),
            entity.getImageUrl(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.isDeleted()
        );
    }

    /**
     * Convenience: Convert domain Post directly to entity (bypassing aggregate).
     * Useful for read-model sync or event-sourcing projections.
     *
     * @param domain the immutable Post value object
     * @return JPA entity ready for persistence
     */
    public static PostEntity domainToEntity(Post domain) {
        return new PostEntity(
            domain.id(),
            domain.authorId(),
            domain.content(),
            domain.imageUrl()
        );
    }
}