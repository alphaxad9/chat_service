// chat_service/src/main/java/com/example/chat_service/infrastructure/persistence/posts/repositories/PostCommandOrmRepository.java

package com.example.chat_service.infrastructure.persistence.posts.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.chat_service.domain.post.PostAggregate;
import com.example.chat_service.domain.post.exceptions.InvalidPostContentError;
import com.example.chat_service.domain.post.exceptions.InvalidPostEntityError;
import com.example.chat_service.domain.post.exceptions.PostNotFoundError;
import com.example.chat_service.domain.post.repositories.PostCommandRepository;
import com.example.chat_service.infrastructure.persistence.posts.PostEntity;
import com.example.chat_service.infrastructure.persistence.posts.PostMapper;
import com.example.chat_service.infrastructure.persistence.posts.jpa.PostCommandJpaRepository;

/**
 * JPA/Hibernate implementation of {@link PostCommandRepository}.
 *
 * <p>Handles write-side operations for Post aggregates using Spring Data JPA.
 * Leverages {@link PostCommandJpaRepository} for persistence and {@link PostMapper}
 * for domain ↔ entity conversion.</p>
 *
 * <p><strong>Soft-delete handling:</strong> The underlying {@link PostEntity}
 * uses {@code @SQLRestriction("is_deleted = false")}, so all queries automatically
 * exclude deleted posts. For operations on deleted posts (e.g., restore),
 * use repository methods that bypass this filter when needed.</p>
 *
 * <p><strong>Transaction management:</strong> All methods run within a transaction
 * via class-level {@code @Transactional}. Rollback occurs automatically on
 * unchecked exceptions, preserving aggregate consistency.</p>
 */
@Repository
@Transactional
public class PostCommandOrmRepository implements PostCommandRepository {

    private final PostCommandJpaRepository postJpaRepository;

    public PostCommandOrmRepository(PostCommandJpaRepository postJpaRepository) {
        this.postJpaRepository = postJpaRepository;
    }

    @Override
    public void save(PostAggregate aggregate) {
        PostEntity entity = PostMapper.aggregateToEntity(aggregate);
        
        try {
            // JPA merge pattern: save handles both insert and update
            // If entity with ID exists → UPDATE; otherwise → INSERT
            postJpaRepository.save(entity);
            
        } catch (DataIntegrityViolationException e) {
            // Map database constraint violations to domain exceptions
            String errorMsg = e.getRootCause() != null 
                ? e.getRootCause().getMessage().toLowerCase() 
                : e.getMessage().toLowerCase();
            
            // Check for NOT NULL constraint on author_id
            if (errorMsg.contains("author_id") && errorMsg.contains("null")) {
                throw new InvalidPostEntityError(
                    aggregate.post().id(),
                    aggregate.post().authorId(),
                    "Database constraint violated: author_id cannot be null"
                );
            }
            
            // Check for content length or NOT NULL constraints
            if (errorMsg.contains("content") && 
                (errorMsg.contains("null") || errorMsg.contains("length") || errorMsg.contains("5000"))) {
                throw new InvalidPostContentError(
                    aggregate.post().content(),
                    "Database constraint violated: content validation failed",
                    5000
                );
            }
            
            // Re-throw as generic integrity error if no specific mapping
            throw new DataIntegrityViolationException(
                "Failed to persist post " + aggregate.post().id() + ": " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public PostAggregate load(UUID postId) {
        try {
            // @SQLRestriction automatically filters out is_deleted=true
            PostEntity entity = postJpaRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundError(
                    postId,
                    null,
                    "Post not found or already deleted"
                ));
            
            return PostMapper.entityToAggregate(entity);
            
        } catch (EmptyResultDataAccessException e) {
            // Fallback for JPA query methods that throw this
            // PostNotFoundError does not accept a cause exception, so we chain via initCause()
            PostNotFoundError notFound = new PostNotFoundError(
                postId,
                null,
                "Post not found or already deleted"
            );
            notFound.initCause(e);
            throw notFound;
        }
    }

    @Override
    public Optional<PostAggregate> loadByAuthor(UUID authorId) {
        // Find most recent post by author (excluding deleted via @SQLRestriction)
        return postJpaRepository.findFirstByAuthorIdOrderByCreatedAtDesc(authorId)
            .map(PostMapper::entityToAggregate);
    }

    @Override
    public boolean exists(UUID postId) {
        // @SQLRestriction ensures we only see non-deleted posts
        return postJpaRepository.existsById(postId);
    }

    @Override
    public boolean existsByAuthor(UUID authorId) {
        // Returns true if author has ANY non-deleted post
        return postJpaRepository.existsByAuthorId(authorId);
    }
}