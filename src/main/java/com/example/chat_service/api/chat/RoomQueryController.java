package com.example.chat_service.api.chat;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.chat_service.application.rooms.handlers.RoomQueryHandler;
import com.example.chat_service.application.rooms.handlers.dtos.MyRoomsHomePageListDto;
import com.example.chat_service.infrastructure.media.MediaUrlService;
import com.example.chat_service.infrastructure.security.UserContext;

import jakarta.servlet.http.HttpServletRequest;

/**
 * REST controller for room query operations (CQRS read-side).
 *
 * <p>Handles HTTP GET requests to retrieve room list data for user home pages via
 * {@code application/json}. All endpoints are prefixed with {@code /api/query/} to avoid
 * path conflicts with the command controller ({@link RoomCommandController}) which
 * handles mutations and returns minimal DTOs.</p>
 *
 * <p><strong>CQRS Path Separation:</strong>
 * <ul>
 *   <li>{@code /api/rooms/...} → Command operations (POST/PUT/PATCH/DELETE) returning creation/update DTOs</li>
 *   <li>{@code /api/query/rooms/...} → Query operations (GET) returning enriched {@code MyRoomsHomePageListDto}</li>
 * </ul>
 * </p>
 *
 * <p>Authentication is handled by {@code JWTAuthenticationFilter} which populates
 * {@code UserContext} with the authenticated user ID. This controller extracts
 * that ID for user-specific queries and delegates to the application layer
 * for data retrieval orchestration.</p>
 *
 * <p><strong>Security principles:</strong>
 * <ul>
 *   <li>Never trust client-submitted {@code user_id} for user-specific queries — use {@code UserContext}</li>
 *   <li>Always extract the authenticated requester from {@code UserContext} (JWT token)</li>
 *   <li>Query operations are read-only — no state mutations occur in this layer</li>
 *   <li>Authorization checks for sensitive data should be added as needed</li>
 * </ul>
 * </p>
 *
 * <p><strong>Image URL handling:</strong>
 * <p>The handler returns DTOs with RELATIVE image paths (e.g., {@code /uploads/rooms/abc.jpg}
 * or {@code /uploads/users/xyz.jpg}). This controller converts them to ABSOLUTE URLs using
 * {@code MediaUrlService} before sending the HTTP response, ensuring frontend-ready URLs
 * without polluting the domain layer.</p>
 *
 * <pre>{@code
 * // Response example for room list:
 * [
 *   {
 *     "room_id": "550e8400-e29b-41d4-a716-446655440000",
 *     "name": "Project Team",
 *     "profile_image_url": "http://127.0.0.1:8005/uploads/groups/profile/abc123.jpg",
 *     "has_profile_image": true,
 *     "is_group": true,
 *     "last_activity_at": "2024-01-20T14:22:00Z",
 *     "is_deleted": false,
 *     "last_message": {
 *       "id": "660e8400-e29b-41d4-a716-446655440001",
 *       "room_id": "550e8400-e29b-41d4-a716-446655440000",
 *       "content": "Let's finalize the spec",
 *       "image_url": null,
 *       "created_at": "2024-01-20T14:22:00Z",
 *       "is_mine": false,
 *       "status": "SEEN",
 *       "sender_username": "alice",
 *       "has_image": false
 *     }
 *   }
 * ]
 * }</pre>
 * </p>
 *
 * <p><strong>Backend invariant:</strong> Only rooms that have at least one active message
 * are included in the returned list. Rooms without messages are filtered out during processing.</p>
 */
@RestController
@RequestMapping("/api/query/rooms")
public class RoomQueryController {

    private static final Logger logger = LoggerFactory.getLogger(RoomQueryController.class);

    private final RoomQueryHandler roomQueryHandler;
    private final MediaUrlService mediaUrlService;

    /**
     * Constructor injection — Spring will auto-wire dependencies
     * because they're annotated with @Component or @Service.
     *
     * @param roomQueryHandler handles read-side room query orchestration
     * @param mediaUrlService converts relative image paths to absolute URLs for frontend
     */
    public RoomQueryController(
            RoomQueryHandler roomQueryHandler,
            MediaUrlService mediaUrlService
    ) {
        this.roomQueryHandler = roomQueryHandler;
        this.mediaUrlService = mediaUrlService;
    }

    // ─────────────────────────────────────────────────────────────────
    // HOME PAGE ROOM LIST (Authenticated User)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Retrieve all rooms for the authenticated user's home page display.
     *
     * <p><strong>Authentication:</strong> User ID is extracted from {@code UserContext}
     * (populated by {@code JWTAuthenticationFilter}). This ID is used to:
     * <ul>
     *   <li>Fetch the user's active memberships</li>
     *   <li>Calculate {@code is_mine} flag for last message previews</li>
     *   <li>Personalize {@code sender_username} as "You" when user is the message sender</li>
     * </ul>
     * </p>
     *
     * <p><strong>Response notes:</strong>
     * <ul>
     *   <li>Rooms are ordered by {@code last_activity_at} descending (most recent first)</li>
     *   <li>Only rooms with at least one active message are included (backend invariant)</li>
     *   <li>For GROUP rooms: {@code name} = group name, {@code profile_image_url} = room's profile image</li>
     *   <li>For DIRECT rooms: {@code name} = friend's username, {@code profile_image_url} = friend's profile picture</li>
     *   <li>Last message preview follows "image-over-text" priority: if message has image, {@code content} is {@code null}</li>
     *   <li>All image URLs are absolute (converted from relative paths stored in domain)</li>
     * </ul>
     * </p>
     *
     * <p><strong>Example request:</strong>
     * <pre>
     * GET /api/query/rooms/home
     * Authorization: Bearer &lt;jwt_token&gt;
     * </pre>
     * </p>
     *
     * @return List of MyRoomsHomePageListDto with absolute image URLs, ordered by last activity
     */
    @GetMapping(
            path = "/home",
            produces = {"application/json"}
    )
    public ResponseEntity<List<MyRoomsHomePageListDto>> getRoomsForUserHomePage(
            HttpServletRequest request
    ) {
        UUID userId = UserContext
                .getUserIdAsUuid()
                .orElseThrow(() -> {
                    logger.error("Unauthorized: No authenticated user found in UserContext for getRoomsForUserHomePage");
                    return new RuntimeException("Unauthorized: No authenticated user found in context");
                });

        logger.info("Fetching rooms for user home page: user_id={}", userId);

        // ─────────────────────────────────────────────
        // 1. Delegate to handler for query + enrichment
        //    - Handler returns DTOs with RELATIVE image paths
        // ─────────────────────────────────────────────
        List<MyRoomsHomePageListDto> dtos = roomQueryHandler.getRoomsForUserHomePage(userId);

        // ─────────────────────────────────────────────
        // 2. Convert all relative image URLs to absolute URLs
        //    - Room profile images and last message images
        // ─────────────────────────────────────────────
        List<MyRoomsHomePageListDto> enrichedDtos = convertImageUrlsToAbsolute(dtos, request);

        logger.info(
                "Successfully returned {} room DTOs for user home page: user_id={}",
                enrichedDtos.size(), userId
        );

        return ResponseEntity.ok(enrichedDtos);
    }

    // ─────────────────────────────────────────────────────────────────
    // HELPER METHODS: URL Conversion
    // ─────────────────────────────────────────────────────────────────

    /**
     * Convert all relative image URLs in a list of MyRoomsHomePageListDto to absolute URLs
     * using the MediaUrlService and the current HTTP request.
     *
     * @param dtos the list of DTOs with relative image paths
     * @param request the current HttpServletRequest for building base URL
     * @return new list with DTO instances having absolute image URLs
     */
    private List<MyRoomsHomePageListDto> convertImageUrlsToAbsolute(
            List<MyRoomsHomePageListDto> dtos,
            HttpServletRequest request
    ) {
        return dtos.stream()
                .map(dto -> convertSingleDtoUrls(dto, request))
                .toList();
    }

    /**
     * Convert all relative image URLs in a single MyRoomsHomePageListDto to absolute URLs.
     *
     * @param dto the DTO with relative image paths
     * @param request the current HttpServletRequest for building base URL
     * @return new DTO instance with absolute image URLs
     */
    private MyRoomsHomePageListDto convertSingleDtoUrls(
            MyRoomsHomePageListDto dto,
            HttpServletRequest request
    ) {
        MyRoomsHomePageListDto updated = dto;

        // Convert room profile image URL
        if (dto.hasProfileImage() && dto.profileImageUrl() != null && !dto.profileImageUrl().isBlank()) {
            String absoluteProfileUrl = mediaUrlService.buildMediaUrl(request, dto.profileImageUrl());
            updated = updated.withProfileImageUrl(absoluteProfileUrl);
            logger.debug("Converted room profile image to absolute URL: {}", absoluteProfileUrl);
        }

        // Convert last message image URL (if present and has image)
        if (dto.lastMessage() != null && dto.lastMessage().hasImage() 
                && dto.lastMessage().imageUrl() != null && !dto.lastMessage().imageUrl().isBlank()) {
            String absoluteMessageImageUrl = mediaUrlService.buildMediaUrl(request, dto.lastMessage().imageUrl());
            updated = updated.withLastMessageImageUrl(absoluteMessageImageUrl);
            logger.debug("Converted last message image to absolute URL: {}", absoluteMessageImageUrl);
        }

        return updated;
    }
}