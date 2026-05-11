package com.example.chat_service.application.post.handlers;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.chat_service.application.post.handlers.dtos.PostResponseDTO;
import com.example.chat_service.application.post.services.PostCommandServiceInterface;
import com.example.chat_service.domain.post.PostAggregate;
import com.example.chat_service.external.users.dtos.UserView;
import com.example.chat_service.external.users.services.UserApiClient;

/**
 * Application-layer orchestrator for post commands.
 *
 * Responsibilities:
 * - Create/load aggregates using domain factories
 * - Delegate persistence to command service
 * - Fetch external author data via UserApiClient
 * - Build enriched API DTO responses
 *
 * DOES NOT:
 * - Contain domain business rules (delegated to Aggregate)
 * - Directly access database (delegated to Repository via Service)
 * - Know HTTP concerns (delegated to Controller)
 */
@Component
public class PostCommandHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(PostCommandHandler.class);

    private final PostCommandServiceInterface commandService;
    private final UserApiClient userApiClient;

    public PostCommandHandler(
            PostCommandServiceInterface commandService,
            UserApiClient userApiClient
    ) {
        this.commandService = commandService;
        this.userApiClient = userApiClient;
    }

    /**
     * Create a new post and return enriched response DTO.
     *
     * Flow:
     * 1. Build PostAggregate using domain factory (validation inside)
     * 2. Persist via command service (transactional boundary)
     * 3. Fetch author profile from external Auth Service
     * 4. Compose PostResponseDTO with enriched author data
     *
     * @param authorId authenticated user ID (from JWT/UserContext)
     * @param content post text content (validated by domain)
     * @param imageUrl optional image URL/path
     * @return PostResponseDTO ready for HTTP response
     */
    public PostResponseDTO createPost(
            UUID authorId,
            String content,
            String imageUrl
    ) {

        logger.info(
                "Creating post for author_id={}",
                authorId
        );

        // ─────────────────────────────────────────────
        // 1. Create aggregate using domain factory
        //    - Validation happens inside createNew()
        //    - Throws InvalidPostEntityError / InvalidPostContentError if invalid
        // ─────────────────────────────────────────────

        PostAggregate aggregate = PostAggregate.createNew(
                UUID.randomUUID(),  // generate new post ID
                authorId,
                content,
                imageUrl
                // createdAt defaults to now inside factory
        );

        // ─────────────────────────────────────────────
        // 2. Persist via command service
        //    - @Transactional boundary ensures atomicity
        //    - Returns saved aggregate (may have DB-generated fields)
        // ─────────────────────────────────────────────

        PostAggregate savedAggregate =
                commandService.createPost(aggregate);

        // ─────────────────────────────────────────────
        // 3. Fetch author from Auth Service
        //    - Enrich response with username, profile picture, etc.
        //    - This is separate from authentication (JWT only gives user_id)
        // ─────────────────────────────────────────────

        UserView author =
                userApiClient.getUserById(authorId);

        // ─────────────────────────────────────────────
        // 4. Build enriched DTO for API response
        //    - Combines domain state + external user data
        //    - Decouples internal model from external representation
        // ─────────────────────────────────────────────

        PostResponseDTO response =
                PostResponseDTO.fromAggregate(
                        savedAggregate,
                        author
                );

        logger.info(
                "Post successfully created post_id={}",
                response.postId()
        );

        return response;
    }
}