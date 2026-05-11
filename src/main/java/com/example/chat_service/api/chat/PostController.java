// src/main/java/com/example/chat_service/api/chat/PostController.java

package com.example.chat_service.api.chat;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.chat_service.api.chat.dtos.CreatePostRequest;
import com.example.chat_service.application.post.handlers.PostCommandHandler;
import com.example.chat_service.application.post.handlers.dtos.PostResponseDTO;
import com.example.chat_service.infrastructure.security.UserContext;

import jakarta.servlet.http.HttpServletRequest;

/**
 * REST controller for post command operations.
 *
 * <p>Handles HTTP POST requests to create posts via {@code multipart/form-data}.
 * Authentication is handled by {@code JWTAuthenticationFilter} which populates
 * {@code UserContext} with the authenticated user ID. This controller extracts
 * that ID and delegates to the application layer for business logic orchestration.</p>
 *
 * <p><strong>Request Format:</strong>
 * <pre>{@code
 * POST /api/posts
 * Content-Type: multipart/form-data
 *
 * --boundary
 * Content-Disposition: form-data; name="content"
 *
 * Hello world!
 * --boundary
 * Content-Disposition: form-data; name="image"; filename="cat.jpg"
 * Content-Type: image/jpeg
 *
 * [binary image data]
 * --boundary--
 * }</pre>
 * </p>
 *
 * <p><strong>Response Format:</strong>
 * <p>The {@code image_url} field in the response contains an <strong>absolute URL</strong>
 * ready for frontend consumption, e.g.:</p>
 * <pre>{@code
 * "image_url": "http://127.0.0.1:8005/uploads/posts/abc123.jpg"
 * }</pre>
 *
 * <p><strong>Security note:</strong> Never trust client-provided author_id.
 * Always extract the authenticated user from the security context ({@code UserContext})
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
     * Create a new post with optional image upload.
     *
     * <p>Flow:
     * <ol>
     *   <li>JWTAuthenticationFilter validates token & sets UserContext</li>
     *   <li>Controller extracts authorId from UserContext</li>
     *   <li>Delegate to PostCommandHandler for aggregate creation & persistence</li>
     *   <li>Handler returns DTO with relative image path from domain</li>
     *   <li>Controller converts relative path → absolute URL for frontend</li>
     *   <li>Returns enriched PostResponseDTO with 201 Created</li>
     * </ol>
     * </p>
     *
     * @param content the text content of the post (from form field "content")
     * @param image optional image file (from form field "image"); can be null/empty
     * @param request the incoming HTTP request (used to build absolute media URLs)
     * @return ResponseEntity with PostResponseDTO and HTTP 201
     * @throws RuntimeException if user is not authenticated
     */
    @PostMapping(
            consumes = {"multipart/form-data"}
    )
    public ResponseEntity<PostResponseDTO> createPost(

            @RequestPart("content")
            String content,

            @RequestPart(
                    value = "image",
                    required = false
            )
            MultipartFile image,

            HttpServletRequest request

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
        // Handler orchestrates:
        //   - Save image (if provided) → get RELATIVE URL path
        //   - Create PostAggregate with content + relative imageUrl
        //   - Persist via command service (DB stores relative path)
        //   - Fetch user profile from Auth Service
        //   - Build PostResponseDTO with relative imageUrl from domain
        // ─────────────────────────────────────────────

        PostResponseDTO response =
                postCommandHandler.createPost(
                        authorId,
                        content,
                        image
                );

        // ─────────────────────────────────────────────
        // Convert relative image path → absolute URL for frontend
        // This keeps domain/DB portable while giving frontend ready-to-use URLs
        // ─────────────────────────────────────────────

        if (response.hasImage() && response.imageUrl() != null) {
            String absoluteImageUrl =
                    postCommandHandler.buildAbsoluteImageUrl(request, response.imageUrl());
            
            // Create new DTO with absolute URL (records are immutable, so we rebuild)
            response = response.withImageUrl(absoluteImageUrl);
        }

        // ─────────────────────────────────────────────
        // Return HTTP 201 Created with enriched response
        // Jackson auto-serializes PostResponseDTO to JSON
        // ─────────────────────────────────────────────

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}