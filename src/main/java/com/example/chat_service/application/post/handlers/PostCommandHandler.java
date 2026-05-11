// src/main/java/com/example/chat_service/application/post/handlers/PostCommandHandler.java

package com.example.chat_service.application.post.handlers;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.example.chat_service.application.post.handlers.dtos.PostResponseDTO;
import com.example.chat_service.application.post.services.PostCommandServiceInterface;
import com.example.chat_service.domain.post.PostAggregate;
import com.example.chat_service.external.users.dtos.UserView;
import com.example.chat_service.external.users.services.UserApiClient;
import com.example.chat_service.infrastructure.media.LocalMediaStorageService;
import com.example.chat_service.infrastructure.media.MediaUrlService;

/**
 * Application-layer orchestrator for post commands.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Create/load aggregates using domain factories</li>
 *   <li>Delegate persistence to command service</li>
 *   <li>Handle media uploads via LocalMediaStorageService</li>
 *   <li>Build absolute media URLs via MediaUrlService</li>
 *   <li>Fetch external author data via UserApiClient</li>
 *   <li>Build enriched API DTO responses</li>
 * </ul>
 * </p>
 *
 * <p><strong>DOES NOT:</strong>
 * <ul>
 *   <li>Contain domain business rules (delegated to Aggregate)</li>
 *   <li>Directly access database (delegated to Repository via Service)</li>
 *   <li>Know HTTP concerns like MultipartFile (handled at controller boundary)</li>
 * </ul>
 * </p>
 *
 * <p><strong>Architecture note:</strong> This handler receives {@code MultipartFile}
 * from the controller, but immediately converts it to a {@code String} URL/path
 * via {@code LocalMediaStorageService}. The domain layer only ever sees the
 * final {@code imageUrl} string, maintaining clean separation of concerns.</p>
 *
 * <p><strong>URL building note:</strong> The handler receives {@code HttpServletRequest}
 * to build absolute URLs for frontend consumption. The database still stores
 * relative paths for portability across environments.</p>
 */
@Component
public class PostCommandHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(PostCommandHandler.class);

    private final PostCommandServiceInterface commandService;
    private final UserApiClient userApiClient;
    private final LocalMediaStorageService mediaStorageService;
    private final MediaUrlService mediaUrlService;

    /**
     * Constructor injection — Spring will auto-wire all dependencies
     * because they're annotated with @Component or @Service.
     *
     * @param commandService handles persistence of PostAggregate
     * @param userApiClient fetches author data from external Auth Service
     * @param mediaStorageService handles local file storage for uploaded images
     * @param mediaUrlService builds absolute URLs from relative paths for API responses
     */
    public PostCommandHandler(
            PostCommandServiceInterface commandService,
            UserApiClient userApiClient,
            LocalMediaStorageService mediaStorageService,
            MediaUrlService mediaUrlService
    ) {
        this.commandService = commandService;
        this.userApiClient = userApiClient;
        this.mediaStorageService = mediaStorageService;
        this.mediaUrlService = mediaUrlService;
    }

    /**
     * Create a new post with optional image upload and return enriched response DTO.
     *
     * <p>Flow:
     * <ol>
     *   <li>If image file provided: save via LocalMediaStorageService → get relative path</li>
     *   <li>Build absolute URL via MediaUrlService for API response</li>
     *   <li>Build PostAggregate using domain factory with RELATIVE path (validation inside)</li>
     *   <li>Persist via command service (transactional boundary)</li>
     *   <li>Fetch author profile from external Auth Service</li>
     *   <li>Compose PostResponseDTO with enriched author data + ABSOLUTE image URL</li>
     * </ol>
     * </p>
     *
     * @param authorId authenticated user ID (from JWT/UserContext)
     * @param content post text content (validated by domain)
     * @param image optional image file from multipart request; can be null or empty
     * @param request the incoming HTTP request (used to build absolute media URLs)
     * @return PostResponseDTO ready for HTTP response with absolute image_url
     */
    public PostResponseDTO createPost(
            UUID authorId,
            String content,
            MultipartFile image
    ) {

        logger.info(
                "Creating post for author_id={}",
                authorId
        );

        // ─────────────────────────────────────────────
        // 1. Handle image upload (if provided)
        //    - Convert MultipartFile → local file → RELATIVE URL path
        //    - This relative path is what gets stored in the database
        //    - The domain only sees the relative path, keeping it environment-agnostic
        // ─────────────────────────────────────────────

        String relativeImagePath = null;

        if (image != null && !image.isEmpty()) {
            relativeImagePath = mediaStorageService.savePostImage(image);
            logger.debug(
                    "Image saved for post (relative path): {}",
                    relativeImagePath
            );
        }

        // ─────────────────────────────────────────────
        // 2. Create aggregate using domain factory
        //    - Validation happens inside createNew()
        //    - Throws InvalidPostEntityError / InvalidPostContentError if invalid
        //    - Domain only sees the RELATIVE imageUrl string, not the upload mechanics
        //    - This keeps the domain portable across environments
        // ─────────────────────────────────────────────

        PostAggregate aggregate = PostAggregate.createNew(
                UUID.randomUUID(),  // generate new post ID
                authorId,
                content,
                relativeImagePath   // ← Store RELATIVE path in domain/DB
                // createdAt defaults to now inside factory
        );

        // ─────────────────────────────────────────────
        // 3. Persist via command service
        //    - @Transactional boundary ensures atomicity
        //    - Returns saved aggregate (may have DB-generated fields)
        // ─────────────────────────────────────────────

        PostAggregate savedAggregate =
                commandService.createPost(aggregate);

        // ─────────────────────────────────────────────
        // 4. Fetch author from Auth Service
        //    - Enrich response with username, profile picture, etc.
        //    - This is separate from authentication (JWT only gives user_id)
        // ─────────────────────────────────────────────

        UserView author =
                userApiClient.getUserById(authorId);

        // ─────────────────────────────────────────────
        // 5. Build enriched DTO for API response
        //    - Combines domain state + external user data
        //    - Decouples internal model from external representation
        //    - NOTE: imageUrl in DTO will be set from aggregate (relative path)
        //          The controller or a separate mapping layer can convert to absolute
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

    /**
     * Helper method to build absolute image URL for API responses.
     *
     * <p>This method is called by the controller after the handler returns,
     * or can be used internally if the DTO needs absolute URLs.</p>
     *
     * @param request the incoming HTTP request
     * @param relativePath the relative path from domain/DB
     * @return absolute URL for frontend consumption, or null if no image
     */
    public String buildAbsoluteImageUrl(jakarta.servlet.http.HttpServletRequest request, String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        return mediaUrlService.buildMediaUrl(request, relativePath);
    }
}