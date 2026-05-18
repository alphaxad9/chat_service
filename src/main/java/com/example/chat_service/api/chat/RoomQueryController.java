package com.example.chat_service.api.chat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.chat_service.application.rooms.handlers.RoomQueryHandler;
import com.example.chat_service.application.rooms.handlers.dtos.GetRoomByIdDTO;
import com.example.chat_service.application.rooms.handlers.dtos.MyRoomsHomePageListDto;
import com.example.chat_service.external.users.dtos.UserView;
import com.example.chat_service.infrastructure.media.MediaUrlService;
import com.example.chat_service.infrastructure.security.UserContext;

import jakarta.servlet.http.HttpServletRequest;

/**
 * REST controller for room query operations (CQRS read-side).
 *
 * <p>Handles HTTP GET requests to retrieve room list data for user home pages,
 * detailed room information, and users available for starting new conversations
 * via {@code application/json}. All endpoints are prefixed with {@code /api/query/}
 * to avoid path conflicts with the command controller ({@link RoomCommandController})
 * which handles mutations and returns minimal DTOs.</p>
 *
 * <p><strong>CQRS Path Separation:</strong>
 * <ul>
 *   <li>{@code /api/rooms/...} → Command operations (POST/PUT/PATCH/DELETE) returning creation/update DTOs</li>
 *   <li>{@code /api/query/rooms/...} → Query operations (GET) returning enriched DTOs</li>
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
 *   <li>Authorization checks: users can only query rooms they are active members of</li>
 * </ul>
 * </p>
 *
 * <p><strong>Image URL handling:</strong>
 * <p>The handler returns DTOs with image paths that may be:
 * <ul>
 *   <li><strong>Relative paths</strong> (e.g., {@code /uploads/rooms/abc.jpg}) for GROUP rooms from Chat Service → convert to absolute using Chat Service base URL (port 8005)</li>
 *   <li><strong>Absolute URLs</strong> (e.g., {@code http://127.0.0.1:8000/media/...}) for PRIVATE/DIRECT rooms from Auth Service (port 8000) → leave unchanged</li>
 * </ul>
 * This controller uses {@code makeAbsoluteUrl()} to intelligently handle both cases before sending the HTTP response.</p>
 *
 * <pre>{@code
 * // Response example for room detail (GetRoomByIdDTO):
 * {
 *   "room_id": "550e8400-e29b-41d4-a716-446655440000",
 *   "name": "Project Team",
 *   "profile_image_url": "http://127.0.0.1:8005/uploads/groups/profile/abc123.jpg",
 *   "cover_image_url": "http://127.0.0.1:8005/uploads/groups/cover/xyz789.jpg",
 *   "has_profile_image": true,
 *   "has_cover_image": true,
 *   "is_group": true,
 *   "type": "GROUP",
 *   "description": "Collaboration space for Project Alpha",
 *   "creator_id": "71885bbe-1f48-42b6-90e7-f988af5231dd",
 *   "is_admin": false,
 *   "is_owner": false,
 *   "last_activity_at": "2024-01-20T14:22:00Z",
 *   "created_at": "2024-01-15T10:00:00Z",
 *   "updated_at": "2024-01-20T14:22:00Z",
 *   "is_deleted": false
 * }
 * }</pre>
 * </p>
 *
 * <p><strong>Backend invariant:</strong> Only rooms that have at least one active message
 * are included in the home page list. Rooms without messages are filtered out during processing.</p>
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
     *   <li>Image URLs use smart conversion: absolute URLs from Auth Service are preserved, relative paths from Chat Service are converted</li>
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
     * @return List of MyRoomsHomePageListDto with properly formatted image URLs, ordered by last activity
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
        //    - Handler returns DTOs with image URLs that may be relative or absolute
        // ─────────────────────────────────────────────
        List<MyRoomsHomePageListDto> dtos = roomQueryHandler.getRoomsForUserHomePage(userId);

        // ─────────────────────────────────────────────
        // 2. Convert image URLs: preserve absolute (Auth Service), convert relative (Chat Service)
        // ─────────────────────────────────────────────
        List<MyRoomsHomePageListDto> enrichedDtos = convertImageUrlsToAbsolute(dtos, request);

        logger.info(
                "Successfully returned {} room DTOs for user home page: user_id={}",
                enrichedDtos.size(), userId
        );

        return ResponseEntity.ok(enrichedDtos);
    }

    // ─────────────────────────────────────────────────────────────────
    // NEW: USERS FOR STARTING NEW CONVERSATION (Authenticated User)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Retrieve a list of users available to start a new conversation with.
     *
     * <p><strong>What this returns:</strong>
     * <ul>
     *   <li>Users from the external Auth Service (paginated via limit/offset)</li>
     *   <li>Friends from the user's empty DIRECT rooms (rooms created but no messages yet)</li>
     *   <li>Deduplicated list excluding the current user</li>
     * </ul>
     * </p>
     *
     * <p><strong>Why include friends from empty rooms?</strong>
     * <ul>
     *   <li>Home page ({@code /api/query/rooms/home}) only shows rooms WITH messages</li>
     *   <li>But users may have created DIRECT rooms that have no messages yet</li>
     *   <li>These "empty rooms" should still appear in the "start conversation" UI</li>
     *   <li>We fetch the OTHER participant from each empty DIRECT room and include them</li>
     * </ul>
     * </p>
     *
     * <p><strong>Authentication:</strong> User ID extracted from {@code UserContext} (JWT token).</p>
     *
     * <p><strong>Response notes:</strong>
     * <ul>
     *   <li>Returns List&lt;UserView&gt; with minimal user data for display</li>
     *   <li>Each UserView includes: user_id, username, email, first_name, last_name, profile_picture</li>
     *   <li>profile_picture contains ABSOLUTE URL from Auth Service → no conversion needed</li>
     *   <li>Results are deduplicated by user_id and exclude the current user</li>
     * </ul>
     * </p>
     *
     * <p><strong>Query parameters:</strong>
     * <ul>
     *   <li>{@code limit} (default: 20): Maximum number of users to return from Auth Service</li>
     *   <li>{@code offset} (default: 0): Offset for pagination from Auth Service</li>
     *   <li>{@code include_deleted} (default: false): Whether to include deleted users from Auth Service</li>
     * </ul>
     * </p>
     *
     * <p><strong>Example request:</strong>
     * <pre>
     * GET /api/query/rooms/users-for-conversation?limit=10&offset=0&include_deleted=false
     * Authorization: Bearer &lt;jwt_token&gt;
     * </pre>
     * </p>
     *
     * @param request the HTTP request (for potential URL conversion if needed)
     * @param limit maximum number of users to return from Auth Service (default: 20)
     * @param offset offset for pagination from Auth Service (default: 0)
     * @param includeDeleted whether to include deleted users from Auth Service (default: false)
     * @return List of UserView ready for "start conversation" UI display
     */
    @GetMapping(
            path = "/users-for-conversation",
            produces = {"application/json"}
    )
    public ResponseEntity<List<UserView>> getUsersForNewConversation(
            HttpServletRequest request,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "include_deleted", defaultValue = "false") boolean includeDeleted
    ) {
        UUID userId = UserContext
                .getUserIdAsUuid()
                .orElseThrow(() -> {
                    logger.error("Unauthorized: No authenticated user found in UserContext for getUsersForNewConversation");
                    return new RuntimeException("Unauthorized: No authenticated user found in context");
                });

        logger.info(
                "Fetching users for new conversation: user_id={}, limit={}, offset={}, include_deleted={}",
                userId, limit, offset, includeDeleted
        );

        // ─────────────────────────────────────────────
        // 1. Delegate to handler for query + enrichment
        //    - Handler combines Auth Service users + empty-room friends
        //    - Returns deduplicated list excluding current user
        // ─────────────────────────────────────────────
        List<UserView> users = roomQueryHandler.getUsersForNewConversation(
                userId,
                limit,
                offset,
                includeDeleted
        );

        // Note: UserView.profilePicture already contains absolute URLs from Auth Service (port 8000)
        // No conversion needed here.

        logger.info(
                "Successfully returned {} users for conversation starters: user_id={}",
                users.size(), userId
        );

        return ResponseEntity.ok(users);
    }

    // ─────────────────────────────────────────────────────────────────
    // NEW: USERS AVAILABLE TO ADD TO A GROUP ROOM (Authenticated User)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Retrieve a list of users available to add to a specific group room.
     *
     * <p><strong>What this returns:</strong>
     * <ul>
     *   <li>Users from the external Auth Service (paginated via limit/offset)</li>
     *   <li>Filtered to exclude users who are already members of the specified room</li>
     *   <li>Excludes the requester (current user) since they are already the admin</li>
     *   <li>Deduplicated list by user_id</li>
     * </ul>
     * </p>
     *
     * <p><strong>Authorization:</strong> User ID extracted from {@code UserContext} (JWT token).
     * The handler does not enforce room membership checks here — that should be done at the
     * service/command layer when actually adding members. This endpoint is for UI population only.</p>
     *
     * <p><strong>Response notes:</strong>
     * <ul>
     *   <li>Returns List&lt;UserView&gt; with minimal user data for display in "Add Members" UI</li>
     *   <li>Each UserView includes: user_id, username, email, first_name, last_name, profile_picture</li>
     *   <li>profile_picture contains ABSOLUTE URL from Auth Service → no conversion needed</li>
     *   <li>Results are deduplicated by user_id and exclude existing members + requester</li>
     * </ul>
     * </p>
     *
     * <p><strong>Query parameters:</strong>
     * <ul>
     *   <li>{@code limit} (default: 20): Maximum number of users to return from Auth Service</li>
     *   <li>{@code offset} (default: 0): Offset for pagination from Auth Service</li>
     *   <li>{@code include_deleted} (default: false): Whether to include deleted users from Auth Service</li>
     * </ul>
     * </p>
     *
     * <p><strong>Example request:</strong>
     * <pre>
     * GET /api/query/rooms/550e8400-e29b-41d4-a716-446655440000/users-to-add?limit=10&offset=0&include_deleted=false
     * Authorization: Bearer &lt;jwt_token&gt;
     * </pre>
     * </p>
     *
     * @param roomId the UUID of the group room to add users to (path variable)
     * @param request the HTTP request (for potential URL conversion if needed)
     * @param limit maximum number of users to return from Auth Service (default: 20)
     * @param offset offset for pagination from Auth Service (default: 0)
     * @param includeDeleted whether to include deleted users from Auth Service (default: false)
     * @return List of UserView ready for "add to group" UI display, excluding existing members and requester
     */
    @GetMapping(
            path = "/{room_id}/users-to-add",
            produces = {"application/json"}
    )
    public ResponseEntity<List<UserView>> getUsersToAddInGroup(
            @PathVariable("room_id") UUID roomId,
            HttpServletRequest request,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "include_deleted", defaultValue = "false") boolean includeDeleted
    ) {
        UUID requesterId = UserContext
                .getUserIdAsUuid()
                .orElseThrow(() -> {
                    logger.error("Unauthorized: No authenticated user found in UserContext for getUsersToAddInGroup");
                    return new RuntimeException("Unauthorized: No authenticated user found in context");
                });

        logger.info(
                "Fetching users to add to group: room_id={}, requester_id={}, limit={}, offset={}, include_deleted={}",
                roomId, requesterId, limit, offset, includeDeleted
        );

        // ─────────────────────────────────────────────
        // 1. Delegate to handler for query + filtering
        //    - Handler fetches Auth Service users and filters out existing members + requester
        //    - Returns deduplicated list of available users
        // ─────────────────────────────────────────────
        List<UserView> users = roomQueryHandler.getUsersToAddInGroup(
                roomId,
                requesterId,
                limit,
                offset,
                includeDeleted
        );

        // Note: UserView.profilePicture already contains absolute URLs from Auth Service (port 8000)
        // No conversion needed here.

        logger.info(
                "Successfully returned {} users available to add to group: room_id={}, requester_id={}",
                users.size(), roomId, requesterId
        );

        return ResponseEntity.ok(users);
    }

    // ─────────────────────────────────────────────────────────────────
    // SINGLE ROOM DETAIL (Authenticated User - Room Settings/Detail View)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Retrieve detailed information for a specific room (settings, member management view).
     *
     * <p><strong>Authentication & Authorization:</strong>
     * <ul>
     *   <li>User ID extracted from {@code UserContext} (JWT token)</li>
     *   <li>Returns 404 Not Found if: room doesn't exist, is deleted, or user is not an active member</li>
     *   <li>Ensures users can only access rooms they participate in</li>
     * </ul>
     * </p>
     *
     * <p><strong>Response notes:</strong>
     * <ul>
     *   <li>For GROUP rooms: includes {@code description}, {@code cover_image_url}, {@code type="GROUP"}</li>
     *   <li>For DIRECT rooms: {@code name} = other participant's username, {@code profile_image_url} = their picture (absolute URL from Auth Service), {@code description=null}, {@code cover_image_url=null}, {@code type="DIRECT"}</li>
     *   <li>{@code is_admin} reflects the requesting user's membership status (ADMIN/USER)</li>
     *   <li>{@code is_owner} is true only if the requesting user created the room</li>
     *   <li>All timestamps are ISO-8601 formatted strings</li>
     *   <li>Image URLs use smart conversion: absolute URLs preserved, relative paths converted</li>
     * </ul>
     * </p>
     *
     * <p><strong>Example request:</strong>
     * <pre>
     * GET /api/query/rooms/550e8400-e29b-41d4-a716-446655440000
     * Authorization: Bearer &lt;jwt_token&gt;
     * </pre>
     * </p>
     *
     * @param roomId the UUID of the room to fetch (path variable)
     * @return ResponseEntity with GetRoomByIdDTO and HTTP 200, or 404 if not found/not authorized
     */
    @GetMapping(
            path = "/{room_id}",
            produces = {"application/json"}
    )
    public ResponseEntity<GetRoomByIdDTO> getRoomById(
            @PathVariable("room_id") UUID roomId,
            HttpServletRequest request
    ) {
        UUID userId = UserContext
                .getUserIdAsUuid()
                .orElseThrow(() -> {
                    logger.error("Unauthorized: No authenticated user found in UserContext for getRoomById");
                    return new RuntimeException("Unauthorized: No authenticated user found in context");
                });

        logger.info(
                "Fetching room details: room_id={}, requester_id={}",
                roomId, userId
        );

        // ─────────────────────────────────────────────
        // 1. Delegate to handler for query + authorization
        //    - Handler returns Optional.empty() if not found or not authorized
        //    - DTO contains image URLs that may be relative or absolute
        // ─────────────────────────────────────────────
        Optional<GetRoomByIdDTO> dtoOpt = roomQueryHandler.getRoomById(roomId, userId);

        if (dtoOpt.isEmpty()) {
            logger.debug(
                    "Room not found or access denied: room_id={}, requester_id={}",
                    roomId, userId
            );
            return ResponseEntity.notFound().build();
        }

        // ─────────────────────────────────────────────
        // 2. Convert image URLs: preserve absolute (Auth Service), convert relative (Chat Service)
        // ─────────────────────────────────────────────
        GetRoomByIdDTO dto = dtoOpt.get();
        GetRoomByIdDTO enrichedDto = convertGetRoomDtoUrls(dto, request);

        logger.info(
                "Successfully returned room detail DTO: room_id={}, name='{}', type={}",
                enrichedDto.roomId(), enrichedDto.name(), enrichedDto.type()
        );

        return ResponseEntity.ok(enrichedDto);
    }

    // ─────────────────────────────────────────────────────────────────
    // HELPER METHODS: URL Conversion for MyRoomsHomePageListDto
    // ─────────────────────────────────────────────────────────────────

    /**
     * Convert all relative image URLs in a list of MyRoomsHomePageListDto to absolute URLs
     * using the MediaUrlService and the current HTTP request.
     * Absolute URLs (from Auth Service) are preserved as-is.
     *
     * @param dtos the list of DTOs with image URLs (relative or absolute)
     * @param request the current HttpServletRequest for building base URL
     * @return new list with DTO instances having properly formatted image URLs
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
     * Convert image URLs in a single MyRoomsHomePageListDto:
     * - If URL is already absolute (http/https), preserve it (PRIVATE/DIRECT rooms from Auth Service)
     * - If URL is relative, convert to absolute using MediaUrlService (GROUP rooms from Chat Service)
     *
     * @param dto the DTO with image URLs (relative or absolute)
     * @param request the current HttpServletRequest for building base URL
     * @return new DTO instance with properly formatted image URLs
     */
    private MyRoomsHomePageListDto convertSingleDtoUrls(
            MyRoomsHomePageListDto dto,
            HttpServletRequest request
    ) {
        MyRoomsHomePageListDto updated = dto;

        // === PROFILE IMAGE (GROUP or PRIVATE) ===
        if (dto.hasProfileImage() && dto.profileImageUrl() != null && !dto.profileImageUrl().isBlank()) {
            String absoluteUrl = makeAbsoluteUrl(dto.profileImageUrl(), request);
            updated = updated.withProfileImageUrl(absoluteUrl);
            logger.debug("Converted profile image URL: {} → {}", dto.profileImageUrl(), absoluteUrl);
        }

        // === LAST MESSAGE IMAGE (always from Chat Service - relative path) ===
        if (dto.lastMessage() != null && dto.lastMessage().hasImage()
                && dto.lastMessage().imageUrl() != null && !dto.lastMessage().imageUrl().isBlank()) {
            String absoluteUrl = makeAbsoluteUrl(dto.lastMessage().imageUrl(), request);
            updated = updated.withLastMessageImageUrl(absoluteUrl);
            logger.debug("Converted last message image URL: {} → {}", dto.lastMessage().imageUrl(), absoluteUrl);
        }

        return updated;
    }

    // ─────────────────────────────────────────────────────────────────
    // HELPER METHODS: URL Conversion for GetRoomByIdDTO
    // ─────────────────────────────────────────────────────────────────

    /**
     * Convert image URLs in a GetRoomByIdDTO:
     * - If URL is already absolute (http/https), preserve it (PRIVATE/DIRECT rooms from Auth Service)
     * - If URL is relative, convert to absolute using MediaUrlService (GROUP rooms from Chat Service)
     *
     * @param dto the DTO with image URLs (relative or absolute)
     * @param request the current HttpServletRequest for building base URL
     * @return new DTO instance with properly formatted image URLs
     */
    private GetRoomByIdDTO convertGetRoomDtoUrls(
            GetRoomByIdDTO dto,
            HttpServletRequest request
    ) {
        GetRoomByIdDTO updated = dto;

        // Convert profile image URL (GROUP: relative path, DIRECT: absolute URL from Auth Service)
        if (dto.hasProfileImage() && dto.profileImageUrl() != null && !dto.profileImageUrl().isBlank()) {
            String absoluteUrl = makeAbsoluteUrl(dto.profileImageUrl(), request);
            updated = updated.withProfileImageUrl(absoluteUrl);
            logger.debug("Converted profile image URL: {} → {}", dto.profileImageUrl(), absoluteUrl);
        }

        // Convert cover image URL (GROUP rooms only - always relative path from Chat Service)
        if (dto.hasCoverImage() && dto.coverImageUrl() != null && !dto.coverImageUrl().isBlank()) {
            String absoluteUrl = makeAbsoluteUrl(dto.coverImageUrl(), request);
            updated = updated.withCoverImageUrl(absoluteUrl);
            logger.debug("Converted cover image URL: {} → {}", dto.coverImageUrl(), absoluteUrl);
        }

        return updated;
    }

    // ─────────────────────────────────────────────────────────────────
    // SMART URL HELPER: Fixes the double-URL bug for private rooms
    // ─────────────────────────────────────────────────────────────────

    /**
     * SMART URL BUILDER — This fixes the double URL bug.
     *
     * <p>Behavior:
     * <ul>
     *   <li>If the URL is already absolute (starts with http:// or https://), return it unchanged.
     *       This handles PRIVATE/DIRECT room images that come from Auth Service (port 8000).</li>
     *   <li>If the URL is a relative path (starts with /), prepend the Chat Service base URL
     *       using MediaUrlService. This handles GROUP room images from Chat Service (port 8005).</li>
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

        // Relative path (e.g. /uploads/groups/profile/xxx.jpg) → convert using Chat Service base
        String converted = mediaUrlService.buildMediaUrl(request, trimmed);
        logger.debug("Converted relative path to absolute URL: {} → {}", trimmed, converted);
        return converted;
    }

    // ─────────────────────────────────────────────────────────────────
    // HELPER METHODS: URL Conversion for UserView (Optional - Not Needed)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Convert all relative profile_picture URLs in a list of UserView to absolute URLs.
     *
     * <p><strong>NOTE:</strong> This helper is NOT needed for this application because
     * {@code UserView.profilePicture} already returns absolute URLs from the Auth Service (port 8000).
     * Calling {@code makeAbsoluteUrl()} on these would incorrectly prepend the Chat Service base URL.
     * This method is kept for reference only.</p>
     *
     * @param users the list of UserView with profile_picture URLs
     * @param request the current HttpServletRequest (unused - URLs already absolute)
     * @return the same list (no conversion needed)
     */
    @SuppressWarnings("unused")
    private List<UserView> convertUserViewProfileUrls(
            List<UserView> users,
            HttpServletRequest request
    ) {
        // UserView.profilePicture already contains absolute URLs from Auth Service (port 8000)
        // No conversion needed - return as-is to avoid double-URL bug
        return users;
    }
}