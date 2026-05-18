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
 * <p>The handler returns DTOs with image URLs that may be:
 * <ul>
 *   <li><strong>Relative paths</strong> (e.g., {@code /uploads/messages/abc.jpg}) for message images from Chat Service → convert to absolute using Chat Service base URL (port 8005)</li>
 *   <li><strong>Absolute URLs</strong> (e.g., {@code http://127.0.0.1:8000/media/...}) for sender profile images from Auth Service (port 8000) → leave unchanged</li>
 * </ul>
 * This controller uses {@code makeAbsoluteUrl()} to intelligently handle both cases before sending the HTTP response.</p>
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
 *     "sender_profile_image": "http://127.0.0.1:8000/media/users/profile/xyz.jpg",
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
     *   <li>Message images use Chat Service base URL (port 8005)</li>
     *   <li>Sender profile images use Auth Service base URL (port 8000) - preserved as-is</li>
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
     * @return List of MessageQueryResponseDTO with properly formatted image URLs, ordered by creation time
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
        //    - Handler returns DTOs with image URLs (relative or absolute)
        // ─────────────────────────────────────────────
        List<MessageQueryResponseDTO> responses = messageQueryHandler.getAllActiveMessagesByRoomId(
                roomId,
                requesterId
        );

        // ─────────────────────────────────────────────
        // 2. Convert image URLs: preserve absolute (Auth Service), convert relative (Chat Service)
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
     * Convert all relative image URLs in a list of MessageQueryResponseDTO to absolute URLs.
     * Absolute URLs (from Auth Service) are preserved as-is.
     *
     * @param responses the list of DTOs with image URLs (relative or absolute)
     * @param request the current HttpServletRequest for building base URL
     * @return new list with DTO instances having properly formatted image URLs
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
     * Convert image URLs in a single MessageQueryResponseDTO:
     * - Message images: relative paths from Chat Service → convert to absolute
     * - Sender profile images: absolute URLs from Auth Service → preserve as-is
     * - Parent preview images: relative paths from Chat Service → convert to absolute
     *
     * @param response the DTO with image URLs (relative or absolute)
     * @param request the current HttpServletRequest for building base URL
     * @return new DTO instance with properly formatted image URLs
     */
    private MessageQueryResponseDTO convertSingleResponseUrls(
            MessageQueryResponseDTO response,
            HttpServletRequest request
    ) {
        MessageQueryResponseDTO updated = response;

        // === MESSAGE IMAGE (always from Chat Service - relative path) ===
        if (response.hasImage() && response.imageUrl() != null && !response.imageUrl().isBlank()) {
            String absoluteImageUrl = makeAbsoluteUrl(response.imageUrl(), request);
            updated = updated.withImageUrl(absoluteImageUrl);
            logger.debug("Converted message image URL: {} → {}", response.imageUrl(), absoluteImageUrl);
        }

        // === SENDER PROFILE IMAGE (from Auth Service - already absolute URL) ===
        if (response.senderProfileImage() != null && !response.senderProfileImage().isBlank()) {
            String absoluteSenderImage = makeAbsoluteUrl(response.senderProfileImage(), request);
            updated = updated.withSenderProfileImage(absoluteSenderImage);
            logger.debug("Converted sender profile image URL: {} → {}", response.senderProfileImage(), absoluteSenderImage);
        }

        // === PARENT PREVIEW IMAGE (always from Chat Service - relative path) ===
        if (response.parentPreview() != null && response.parentPreview().imageUrl() != null && !response.parentPreview().imageUrl().isBlank()) {
            String absoluteParentImage = makeAbsoluteUrl(response.parentPreview().imageUrl(), request);
            updated = updated.withParentImageUrl(absoluteParentImage);
            logger.debug("Converted parent preview image URL: {} → {}", response.parentPreview().imageUrl(), absoluteParentImage);
        }

        return updated;
    }

    // ─────────────────────────────────────────────────────────────────
    // SMART URL HELPER: Fixes the double-URL bug for sender profile images
    // ─────────────────────────────────────────────────────────────────

    /**
     * SMART URL BUILDER — This fixes the double URL bug.
     *
     * <p>Behavior:
     * <ul>
     *   <li>If the URL is already absolute (starts with http:// or https://), return it unchanged.
     *       This handles sender profile images from Auth Service (port 8000).</li>
     *   <li>If the URL is a relative path (starts with /), prepend the Chat Service base URL
     *       using MediaUrlService. This handles message images from Chat Service (port 8005).</li>
     * </ul>
     * </p>
     *
     * @param url the image URL (may be relative or absolute)
     * @param request the current HttpServletRequest for building base URL when needed
     * @return properly formatted absolute URL, or null if input was null/blank
     */
    private String makeAbsoluteUrl(String url, HttpServletRequest request) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String trimmed = url.trim();

        // Already a full URL (e.g. from Auth Service port 8000) → do NOT prepend anything
        // This prevents the double-URL bug: http://127.0.0.1:8005http://127.0.0.1:8000/...
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            logger.debug("Preserving absolute URL from Auth Service: {}", trimmed);
            return trimmed;
        }

        // Relative path (e.g. /uploads/messages/xxx.jpg) → convert using Chat Service base
        String converted = mediaUrlService.buildMediaUrl(request, trimmed);
        logger.debug("Converted relative path to absolute URL: {} → {}", trimmed, converted);
        return converted;
    }
}