// chat_service/src/main/java/com/example/chat_service/application/post/services/impl/PostCommandServiceImpl.java

package com.example.chat_service.application.post.services.impl;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.chat_service.application.post.services.PostCommandServiceInterface;
import com.example.chat_service.domain.post.PostAggregate;
import com.example.chat_service.domain.post.exceptions.InvalidPostContentError;
import com.example.chat_service.domain.post.exceptions.InvalidPostEntityError;
import com.example.chat_service.domain.post.exceptions.PostAlreadyExistsError;
import com.example.chat_service.domain.post.exceptions.PostDomainError;
import com.example.chat_service.domain.post.exceptions.PostNotFoundError;
import com.example.chat_service.domain.post.exceptions.PostOperationNotAllowedError;
import com.example.chat_service.domain.post.exceptions.PostStateTransitionError;
import com.example.chat_service.domain.post.repositories.PostCommandRepository;

/**
 * Application-layer implementation of {@link PostCommandServiceInterface}.
 *
 * <p>Orchestrates post command (write) operations by coordinating domain aggregates
 * with infrastructure repositories. All methods run within a transaction boundary
 * to ensure consistency.</p>
 *
 * <p><strong>No event publishing:</strong> This implementation focuses purely on
 * command orchestration. Event emission (outbox, Kafka, etc.) should be added
 * in a separate layer or via domain events when the infrastructure is ready.</p>
 *
 * <p><strong>Design principles:</strong>
 * <ul>
 *   <li>Methods accept/return {@link PostAggregate} — never expose entities or DTOs</li>
 *   <li>Business rules and validation live in the domain (aggregate), not here</li>
 *   <li>Infrastructure concerns (persistence) are delegated to {@link PostCommandRepository}</li>
 *   <li>All public methods are {@code @Transactional} for atomicity</li>
 * </ul></p>
 */
@Service
@Transactional
public class PostCommandServiceImpl implements PostCommandServiceInterface {

    private static final Logger logger = LoggerFactory.getLogger(PostCommandServiceImpl.class);

    private final PostCommandRepository postCommandRepository;

    public PostCommandServiceImpl(PostCommandRepository postCommandRepository) {
        this.postCommandRepository = postCommandRepository;
    }

    // ── Core Lifecycle Commands ────────────────────────────────────────

    @Override
    public PostAggregate createPost(PostAggregate aggregate) {
        try {
            // Domain validation already enforced in aggregate factory/constructor
            // Repository handles persistence (INSERT if new ID)
            postCommandRepository.save(aggregate);

            logger.info(
                "Successfully created post (post_id={}) by author_id={}",
                aggregate.post().id(),
                aggregate.post().authorId()
            );
            return aggregate;

        } catch (PostAlreadyExistsError e) {
            logger.warn(
                "Post creation failed: post already exists (post_id={}, author_id={})",
                e.getPostId(),
                e.getAuthorId()
            );
            throw e;

        } catch (InvalidPostEntityError | InvalidPostContentError e) {
            logger.warn(
                "Post creation failed: invalid data (author_id={}, reason={})",
                aggregate.post().authorId(),
                e.getMessage()
            );
            throw e;

        } catch (PostDomainError e) {
            logger.warn(
                "Post creation domain error (post_id={}, author_id={}, reason={})",
                aggregate.post().id(),
                aggregate.post().authorId(),
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error creating post (post_id={}, author_id={})",
                aggregate.post().id(),
                aggregate.post().authorId(),
                e
            );
            throw e;
        }
    }

    @Override
    public PostAggregate deletePost(PostAggregate aggregate) {
        try {
            // Extract state BEFORE mutation for consistent logging
            UUID postId = aggregate.post().id();
            UUID authorId = aggregate.post().authorId();

            // Domain enforces: post must be active to delete
            aggregate.delete();

            // Persist the state change
            postCommandRepository.save(aggregate);

            logger.info("Successfully deleted post (post_id={}) by author_id={}", postId, authorId);
            return aggregate;

        } catch (PostStateTransitionError e) {
            logger.warn(
                "Post deletion failed: invalid state transition (post_id={}, current={}, target={}, reason={})",
                aggregate.post().id(),
                e.getCurrentState(),
                e.getTargetState(),
                e.getReason()
            );
            throw e;

        } catch (PostOperationNotAllowedError e) {
            logger.warn(
                "Post deletion failed: operation not allowed (post_id={}, operation={}, reason={})",
                e.getPostId(),
                e.getOperation(),
                e.getReason()
            );
            throw e;

        } catch (PostDomainError e) {
            logger.warn(
                "Post deletion domain error (post_id={}, reason={})",
                aggregate.post().id(),
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error deleting post (post_id={})",
                aggregate.post().id(),
                e
            );
            throw e;
        }
    }

    @Override
    public PostAggregate restorePost(PostAggregate aggregate) {
        try {
            UUID postId = aggregate.post().id();
            UUID authorId = aggregate.post().authorId();

            // Domain enforces: post must be deleted to restore
            aggregate.restore();

            postCommandRepository.save(aggregate);

            logger.info("Successfully restored post (post_id={}) by author_id={}", postId, authorId);
            return aggregate;

        } catch (PostStateTransitionError e) {
            logger.warn(
                "Post restore failed: invalid state transition (post_id={}, current={}, target={}, reason={})",
                aggregate.post().id(),
                e.getCurrentState(),
                e.getTargetState(),
                e.getReason()
            );
            throw e;

        } catch (PostOperationNotAllowedError e) {
            logger.warn(
                "Post restore failed: operation not allowed (post_id={}, operation={}, reason={})",
                e.getPostId(),
                e.getOperation(),
                e.getReason()
            );
            throw e;

        } catch (PostDomainError e) {
            logger.warn(
                "Post restore domain error (post_id={}, reason={})",
                aggregate.post().id(),
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error restoring post (post_id={})",
                aggregate.post().id(),
                e
            );
            throw e;
        }
    }

    // ── Content & Media Commands ───────────────────────────────────────

    @Override
    public PostAggregate updateContent(PostAggregate aggregate, String newContent) {
        try {
            UUID postId = aggregate.post().id();
            UUID authorId = aggregate.post().authorId();

            // Domain validates: content non-null, non-empty, <= 5000 chars
            aggregate.updateContent(newContent);

            postCommandRepository.save(aggregate);

            logger.info(
                "Successfully updated post content (post_id={}) by author_id={}",
                postId,
                authorId
            );
            return aggregate;

        } catch (InvalidPostContentError e) {
            logger.warn(
                "Post content update failed: invalid content (post_id={}, reason={})",
                aggregate.post().id(),
                e.getMessage()
            );
            throw e;

        } catch (PostOperationNotAllowedError e) {
            logger.warn(
                "Post content update failed: operation not allowed (post_id={}, operation={}, reason={})",
                e.getPostId(),
                e.getOperation(),
                e.getReason()
            );
            throw e;

        } catch (PostStateTransitionError e) {
            logger.warn(
                "Post content update failed: invalid state transition (post_id={}, reason={})",
                aggregate.post().id(),
                e.getReason()
            );
            throw e;

        } catch (PostDomainError e) {
            logger.warn(
                "Post content update domain error (post_id={}, reason={})",
                aggregate.post().id(),
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error updating post content (post_id={})",
                aggregate.post().id(),
                e
            );
            throw e;
        }
    }

    @Override
    public PostAggregate updateImage(PostAggregate aggregate, String newImageUrl) {
        try {
            UUID postId = aggregate.post().id();
            UUID authorId = aggregate.post().authorId();

            // Domain allows: imageUrl can be null (removing image) or valid URL/path
            aggregate.updateImage(newImageUrl);

            postCommandRepository.save(aggregate);

            logger.info(
                "Successfully updated post image (post_id={}) by author_id={}, hasImage={}",
                postId,
                authorId,
                aggregate.post().hasImage()
            );
            return aggregate;

        } catch (PostOperationNotAllowedError e) {
            logger.warn(
                "Post image update failed: operation not allowed (post_id={}, operation={}, reason={})",
                e.getPostId(),
                e.getOperation(),
                e.getReason()
            );
            throw e;

        } catch (PostDomainError e) {
            logger.warn(
                "Post image update domain error (post_id={}, reason={})",
                aggregate.post().id(),
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error updating post image (post_id={})",
                aggregate.post().id(),
                e
            );
            throw e;
        }
    }

    @Override
    public PostAggregate touch(PostAggregate aggregate) {
        try {
            UUID postId = aggregate.post().id();

            // Refreshes updatedAt timestamp without changing business state
            aggregate.touch();

            postCommandRepository.save(aggregate);

            logger.debug(
                "Successfully touched post (post_id={}), updated_at={}",
                postId,
                aggregate.post().updatedAt()
            );
            return aggregate;

        } catch (PostOperationNotAllowedError e) {
            logger.warn(
                "Post touch failed: operation not allowed (post_id={}, operation={}, reason={})",
                e.getPostId(),
                e.getOperation(),
                e.getReason()
            );
            throw e;

        } catch (PostDomainError e) {
            logger.warn(
                "Post touch domain error (post_id={}, reason={})",
                aggregate.post().id(),
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error touching post (post_id={})",
                aggregate.post().id(),
                e
            );
            throw e;
        }
    }

    // ── Query Support Methods (for command orchestration) ──────────────

    @Override
    public PostAggregate loadAggregate(UUID postId) {
        try {
            PostAggregate aggregate = postCommandRepository.load(postId);
            logger.debug("Loaded post aggregate: post_id={}", postId);
            return aggregate;

        } catch (PostNotFoundError e) {
            logger.warn("Post aggregate not found: post_id={}", postId);
            throw e;

        } catch (PostDomainError e) {
            logger.warn(
                "Domain error loading post aggregate: post_id={}, reason={}",
                postId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error loading post aggregate: post_id={}", postId, e);
            throw e;
        }
    }

    @Override
    public Optional<PostAggregate> loadAggregateByAuthor(UUID authorId) {
        try {
            Optional<PostAggregate> result = postCommandRepository.loadByAuthor(authorId);
            
            if (result.isPresent()) {
                logger.debug(
                    "Loaded post aggregate by author: author_id={}, post_id={}",
                    authorId,
                    result.get().post().id()
                );
            } else {
                logger.debug("No post aggregate found for author: author_id={}", authorId);
            }
            
            return result;

        } catch (PostDomainError e) {
            logger.warn(
                "Domain error loading post by author: author_id={}, reason={}",
                authorId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error loading post by author: author_id={}", authorId, e);
            throw e;
        }
    }

    @Override
    public boolean aggregateExists(UUID postId) {
        try {
            boolean exists = postCommandRepository.exists(postId);
            logger.debug("Existence check: post_id={}, exists={}", postId, exists);
            return exists;

        } catch (Exception e) {
            logger.error("Unexpected error checking post existence: post_id={}", postId, e);
            throw e;
        }
    }

    @Override
    public boolean aggregateExistsByAuthor(UUID authorId) {
        try {
            boolean exists = postCommandRepository.existsByAuthor(authorId);
            logger.debug("Existence check by author: author_id={}, exists={}", authorId, exists);
            return exists;

        } catch (Exception e) {
            logger.error("Unexpected error checking post existence by author: author_id={}", authorId, e);
            throw e;
        }
    }
}