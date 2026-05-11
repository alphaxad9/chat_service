package com.example.chat_service.api.chat;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.chat_service.api.chat.dtos.CreatePostRequest;
import com.example.chat_service.application.post.handlers.PostCommandHandler;
import com.example.chat_service.application.post.handlers.dtos.PostResponseDTO;
import com.example.chat_service.infrastructure.security.UserContext;

/**
 * REST controller for post command operations.
 *
 * <p>Handles HTTP POST requests to create posts. Authentication is handled
 * by JWTAuthenticationFilter which populates UserContext with the authenticated
 * user ID. This controller extracts that ID and delegates to the application
 * layer for business logic orchestration.</p>
 *
 * <p><strong>Security note:</strong> Never trust client-provided author_id.
 * Always extract the authenticated user from the security context (UserContext)
 * which is populated by the JWT filter after token validation.</p>
 */
@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostCommandHandler postCommandHandler;

    /**
     * Constructor injection — Spring will auto-wire PostCommandHandler
     * because it's annotated with @Component.
     */
    public PostController(PostCommandHandler postCommandHandler) {
        this.postCommandHandler = postCommandHandler;
    }

    /**
     * Create a new post.
     *
     * <p>Flow:
     * <ol>
     *   <li>JWTAuthenticationFilter validates token & sets UserContext</li>
     *   <li>Controller extracts authorId from UserContext</li>
     *   <li>Delegates to PostCommandHandler for orchestration</li>
     *   <li>Returns enriched PostResponseDTO with 201 Created</li>
     * </ol>
     * </p>
     *
     * @param request JSON body with content and optional image_url
     * @return ResponseEntity with PostResponseDTO and HTTP 201
     * @throws RuntimeException if user is not authenticated
     */
    @PostMapping
    public ResponseEntity<PostResponseDTO> createPost(
            @RequestBody CreatePostRequest request
    ) {

        // ─────────────────────────────────────────────
        // Extract authenticated user from JWT filter
        // UserContext is thread-local, populated by JWTAuthenticationFilter
        // ─────────────────────────────────────────────

        UUID authorId = UserContext
                .getUserIdAsUuid()
                .orElseThrow(() ->
                        new RuntimeException("Unauthorized: No authenticated user found in context")
                );

        // ─────────────────────────────────────────────
        // Delegate to application layer handler
        // Handler orchestrates: aggregate creation → persistence → user fetch → DTO build
        // ─────────────────────────────────────────────

        PostResponseDTO response =
                postCommandHandler.createPost(
                        authorId,
                        request.content(),
                        request.imageUrl()
                );

        // ─────────────────────────────────────────────
        // Return HTTP 201 Created with enriched response
        // Jackson auto-serializes PostResponseDTO to JSON
        // ─────────────────────────────────────────────

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}