// chat_service/src/main/java/com/example/chat_service/api/chat/MessageQueryController.java

package com.example.chat_service.api.chat;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.chat_service.application.messages.handlers.MessageQueryHandler;
import com.example.chat_service.application.messages.handlers.dtos.MessageQueryResponseDTO;
import com.example.chat_service.infrastructure.media.MediaUrlService;
import com.example.chat_service.infrastructure.security.UserContext;

import jakarta.servlet.http.HttpServletRequest;

/**
 * REST controller for message query (read) operations.
 *
 * <p>Handles HTTP requests to fetch message history and chat threads. Authentication is handled
 * by {@code JWTAuthenticationFilter} which populates {@code UserContext} with the authenticated
 * user ID. This controller extracts that ID as {@code requester_id} for personalization and
 * delegates to the application layer for business logic orchestration.</p>
 *
 * <p><strong>Security principles:</strong>
 * <ul>
 *   <li>Never trust client-submitted {@code requester_id} — always extract from {@code UserContext}</li>
 *   <li>Domain entities enforce read permissions; controller passes verified requester ID</li>
 *   <li>Image URLs returned to frontend are absolute; domain/database stores relative paths</li>
 * </ul>
 * </p>
 *
 * <p><strong>Image URL handling:</strong>
 * <p>The handler returns DTOs with RELATIVE image paths (e.g., {@code /uploads/messages/abc.jpg}).
 * This controller converts them to ABSOLUTE URLs using {@code MediaUrlService} before sending
 * the HTTP response, ensuring frontend-ready URLs without polluting the domain layer.</p>
 *
 * <pre>{@code
 * // Response example for message list:
 * [
 *   {
 *     "id": "550e8400-e29b-41d4-a716-446655440000",
 *     "room_id": "660e8400-e29b-41d4-a716-446655440001",
 *     "content": "Hello, world!",
 *     "image_url": "http://127.0.0.1:8005/uploads/messages/abc123.jpg",
 *     "is_reply": false,
 *     "parent_preview": null,
 *     "created_at": "2024-01-15T10:30:00Z",
 *     "is_mine": true,
 *     "status": "SEEN",
 *     "sender_username": "You",
 *     "sender_profile_image": "http://127.0.0.1:8005/uploads/users/profile/xyz.jpg",
 *     "has_image": true,
 *     "is_deleted": false,
 *     "updated_at": "2024-01-15T10:30:00Z",
 *     "seen_at": "2024-01-15T10:31:00Z"
 *   }
 * ]
 * }</pre>
 * </p>
 */
@RestController
@RequestMapping("/api/messages")
public class MessageQueryController {

    private static final Logger logger = LoggerFactory.getLogger(MessageQueryController.class);

    private final MessageQueryHandler messageQueryHandler;
    private final MediaUrlService mediaUrlService;

    /**
     * Constructor injection — Spring will auto-wire dependencies
     * because they're annotated with @Component or @Service.
     */
    public MessageQueryController(
            MessageQueryHandler messageQueryHandler,
            MediaUrlService mediaUrlService
    ) {
        this.messageQueryHandler = messageQueryHandler;
        this.mediaUrlService = mediaUrlService;
    }

    // ─────────────────────────────────────────────────────────────────
    // QUERY MESSAGES BY ROOM
    // ─────────────────────────────────────────────────────────────────

    /**
     * Retrieve all active messages in a specific room for chat history.
     *
     * <p><strong>Authentication:</strong> Requester ID is extracted from {@code UserContext}
     * (populated by {@code JWTAuthenticationFilter}). This ID is used for:
     * <ul>
     *   <li>Calculating {@code is_mine} flag for each message</li>
     *   <li>Personalizing {@code sender_username} as "You" when requester is the sender</li>
     * </ul>
     * </p>
     *
     * <p><strong>Response notes:</strong>
     * <ul>
     *   <li>Messages are ordered by {@code created_at} ascending (oldest first) for chat history</li>
     *   <li>Deleted messages ({@code is_deleted=true}) are excluded from results</li>
     *   <li>Reply messages include {@code parent_preview} with image-over-text priority</li>
     *   <li>All image URLs are absolute (converted from relative paths stored in domain)</li>
     * </ul>
     * </p>
     *
     * <p><strong>Example request:</strong>
     * <pre>
     * GET /api/messages/room/660e8400-e29b-41d4-a716-446655440001
     * Authorization: Bearer &lt;jwt_token&gt;
     * </pre>
     * </p>
     *
     * @param roomId the UUID of the room to query messages from (path variable)
     * @return List of MessageQueryResponseDTO with absolute image URLs, ordered by creation time
     */
    @GetMapping(
            path = "/room/{room_id}",
            produces = {"application/json"}
    )
    public ResponseEntity<List<MessageQueryResponseDTO>> getAllActiveMessagesByRoomId(
            @PathVariable("room_id") UUID roomId,
            HttpServletRequest request
    ) {
        UUID requesterId = UserContext
                .getUserIdAsUuid()
                .orElseThrow(() -> {
                    logger.error("Unauthorized: No authenticated user found in UserContext for getAllActiveMessagesByRoomId");
                    return new RuntimeException("Unauthorized: No authenticated user found in context");
                });

        logger.info(
                "Querying active messages for room: room_id={}, requester_id={}",
                roomId, requesterId
        );

        // ─────────────────────────────────────────────
        // 1. Delegate to handler for query + enrichment
        //    - Handler returns DTOs with RELATIVE image paths
        // ─────────────────────────────────────────────
        List<MessageQueryResponseDTO> responses = messageQueryHandler.getAllActiveMessagesByRoomId(
                roomId,
                requesterId
        );

        // ─────────────────────────────────────────────
        // 2. Convert all relative image URLs to absolute URLs
        //    - Message images, sender profile images, parent preview images
        // ─────────────────────────────────────────────
        List<MessageQueryResponseDTO> enrichedResponses = convertImageUrlsToAbsolute(responses, request);

        logger.info(
                "Successfully returned {} message DTOs for room: room_id={}, requester_id={}",
                enrichedResponses.size(), roomId, requesterId
        );

        return ResponseEntity.ok(enrichedResponses);
    }

    // ─────────────────────────────────────────────────────────────────
    // HELPER METHODS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Convert all relative image URLs in a list of MessageQueryResponseDTO to absolute URLs
     * using the MediaUrlService and the current HTTP request.
     *
     * @param responses the list of DTOs with relative image paths
     * @param request the current HttpServletRequest for building base URL
     * @return new list with DTO instances having absolute image URLs
     */
    private List<MessageQueryResponseDTO> convertImageUrlsToAbsolute(
            List<MessageQueryResponseDTO> responses,
            HttpServletRequest request
    ) {
        return responses.stream()
                .map(response -> convertSingleResponseUrls(response, request))
                .toList();
    }

    /**
     * Convert all relative image URLs in a single MessageQueryResponseDTO to absolute URLs.
     *
     * @param response the DTO with relative image paths
     * @param request the current HttpServletRequest for building base URL
     * @return new DTO instance with absolute image URLs
     */
    private MessageQueryResponseDTO convertSingleResponseUrls(
            MessageQueryResponseDTO response,
            HttpServletRequest request
    ) {
        MessageQueryResponseDTO updated = response;

        // Convert message image URL
        if (response.hasImage() && response.imageUrl() != null && !response.imageUrl().isBlank()) {
            String absoluteImageUrl = mediaUrlService.buildMediaUrl(request, response.imageUrl());
            updated = updated.withImageUrl(absoluteImageUrl);
            logger.debug("Converted message image to absolute URL: {}", absoluteImageUrl);
        }

        // Convert sender profile image URL
        if (response.senderProfileImage() != null && !response.senderProfileImage().isBlank()) {
            String absoluteSenderImage = mediaUrlService.buildMediaUrl(request, response.senderProfileImage());
            updated = updated.withSenderProfileImage(absoluteSenderImage);
            logger.debug("Converted sender profile image to absolute URL: {}", absoluteSenderImage);
        }

        // Convert parent preview image URL (if present)
        if (response.parentPreview() != null && response.parentPreview().imageUrl() != null && !response.parentPreview().imageUrl().isBlank()) {
            String absoluteParentImage = mediaUrlService.buildMediaUrl(request, response.parentPreview().imageUrl());
            updated = updated.withParentImageUrl(absoluteParentImage);
            logger.debug("Converted parent preview image to absolute URL: {}", absoluteParentImage);
        }

        return updated;
    }
}