// chat_service/src/main/java/com/example/chat_service/api/chat/RoomCommandController.java

package com.example.chat_service.api.chat;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.chat_service.application.rooms.handlers.RoomCommandHandler;
import com.example.chat_service.application.rooms.handlers.dtos.GroupCreationResponse;
import com.example.chat_service.application.rooms.handlers.dtos.PrivateRoomCreationResponse;
import com.example.chat_service.infrastructure.media.MediaUrlService;
import com.example.chat_service.infrastructure.security.UserContext;

import jakarta.servlet.http.HttpServletRequest;

/**
 * REST controller for room command operations.
 *
 * <p>Handles HTTP requests to create rooms (GROUP and DIRECT) via {@code multipart/form-data}
 * or {@code application/json}. Authentication is handled by {@code JWTAuthenticationFilter}
 * which populates {@code UserContext} with the authenticated user ID. This controller extracts
 * that ID for ownership and delegates to the application layer for business logic orchestration.</p>
 *
 * <p><strong>Security principles:</strong>
 * <ul>
 *   <li>Never trust client-submitted {@code creator_id} — always extract from {@code UserContext}</li>
 *   <li>Domain aggregates enforce ownership checks; controller passes verified requester ID</li>
 *   <li>Image URLs returned to frontend are absolute; domain/database stores relative paths</li>
 * </ul>
 * </p>
 *
 * <p><strong>Image URL handling:</strong>
 * <p>The handler returns DTOs with RELATIVE image paths (e.g., {@code /uploads/groups/profile/abc.jpg}).
 * This controller converts them to ABSOLUTE URLs using {@code MediaUrlService} before sending
 * the HTTP response, ensuring frontend-ready URLs without polluting the domain layer.</p>
 *
 * <pre>{@code
 * // Response example for GROUP room:
 * {
 *   "room_id": "550e8400-e29b-41d4-a716-446655440000",
 *   "name": "Project Team",
 *   "profile_image_url": "http://127.0.0.1:8005/uploads/groups/profile/abc123.jpg",
 *   "members": [
 *     {"username": "You"},
 *     {"username": "alice"},
 *     {"username": "bob"}
 *   ],
 *   "admin": true,
 *   "is_group": true,
 *   "has_profile_image": true
 * }
 * }</pre>
 * </p>
 */
@RestController
@RequestMapping("/api/rooms")
public class RoomCommandController {

    private static final Logger logger = LoggerFactory.getLogger(RoomCommandController.class);

    private final RoomCommandHandler roomCommandHandler;
    private final MediaUrlService mediaUrlService;

    /**
     * Constructor injection — Spring will auto-wire dependencies
     * because they're annotated with @Component or @Service.
     */
    public RoomCommandController(
            RoomCommandHandler roomCommandHandler,
            MediaUrlService mediaUrlService
    ) {
        this.roomCommandHandler = roomCommandHandler;
        this.mediaUrlService = mediaUrlService;
    }

    // ─────────────────────────────────────────────────────────────────
    // GROUP ROOM CREATION
    // ─────────────────────────────────────────────────────────────────

    /**
     * Create a new GROUP room with participants and optional images.
     *
     * <p><strong>Request Format (multipart/form-data):</strong></p>
     * <pre>{@code
     * POST /api/rooms/groups
     * Content-Type: multipart/form-data; boundary=----WebKitFormBoundary
     * Authorization: Bearer <jwt_token>
     *
     * ------WebKitFormBoundary
     * Content-Disposition: form-data; name="group_name"
     *
     * Project Team
     * ------WebKitFormBoundary
     * Content-Disposition: form-data; name="description"
     *
     * Team collaboration space
     * ------WebKitFormBoundary
     * Content-Disposition: form-data; name="participant_ids"
     *
     * ["uuid-1","uuid-2","uuid-3"]
     * ------WebKitFormBoundary
     * Content-Disposition: form-data; name="profile_image"; filename="avatar.jpg"
     * Content-Type: image/jpeg
     *
     * [binary image data]
     * ------WebKitFormBoundary
     * Content-Disposition: form-data; name="cover_image"; filename="cover.jpg"
     * Content-Type: image/jpeg
     *
     * [binary image data]
     * ------WebKitFormBoundary--
     * }</pre>
     *
     * <p><strong>Security:</strong> The authenticated user ID is extracted from the JWT token
     * via {@code UserContext}. This user becomes the room creator and ADMIN. Client cannot
     * impersonate another user as creator.</p>
     *
     * @param groupName the name for the new group (1-100 chars)
     * @param description optional description (max 500 chars)
     * @param participantIdsJson JSON array string of participant user IDs (must have >= 2 IDs)
     * @param profileImage optional profile/avatar image file
     * @param coverImage optional cover/background image file
     * @param request the incoming HTTP request (used to build absolute media URLs)
     * @return ResponseEntity with GroupCreationResponse and HTTP 201
     * @throws RuntimeException if user is not authenticated or participant count is invalid
     */
    @PostMapping(
            path = "/groups",
            consumes = {"multipart/form-data"}
    )
    public ResponseEntity<GroupCreationResponse> createGroupRoom(

            @RequestPart("group_name")
            String groupName,

            @RequestPart(value = "description", required = false)
            String description,

            @RequestPart("participant_ids")
            String participantIdsJson,

            @RequestPart(value = "profile_image", required = false)
            MultipartFile profileImage,

            @RequestPart(value = "cover_image", required = false)
            MultipartFile coverImage,

            HttpServletRequest request

    ) {
        // ─────────────────────────────────────────────
        // Extract authenticated user from JWT filter
        // UserContext is thread-local, populated by JWTAuthenticationFilter
        // This is the ONLY source of truth for "who is creating this room"
        // ─────────────────────────────────────────────
        UUID creatorId = UserContext
                .getUserIdAsUuid()
                .orElseThrow(() -> {
                    logger.error("Unauthorized: No authenticated user found in UserContext for createGroupRoom");
                    return new RuntimeException("Unauthorized: No authenticated user found in context");
                });

        // ─────────────────────────────────────────────
        // Parse participant IDs from JSON array string
        // Expected format: ["uuid-1","uuid-2","uuid-3"]
        // ─────────────────────────────────────────────
        List<UUID> participantIds = parseUuidList(participantIdsJson);

        logger.info(
                "Processing GROUP room creation: creator_id={}, group_name='{}', participant_count={}",
                creatorId, groupName, participantIds.size()
        );

        // ─────────────────────────────────────────────
        // Delegate to application layer handler
        // Handler orchestrates:
        //   - Validate participant count (>= 2 excluding creator)
        //   - Save images (if provided) → get RELATIVE URL paths
        //   - Create RoomAggregate with relative image paths
        //   - Persist room + create member records (transactional)
        //   - Fetch usernames from Auth Service
        //   - Build GroupCreationResponse with relative profile_image_url
        // ─────────────────────────────────────────────
        GroupCreationResponse response = roomCommandHandler.createGroupRoom(
                creatorId,
                groupName,
                description,
                participantIds,
                profileImage,
                coverImage
        );

        // ─────────────────────────────────────────────
        // Convert relative profile image path → absolute URL for frontend
        // This keeps domain/DB portable while giving frontend ready-to-use URLs
        // ─────────────────────────────────────────────
        if (response.hasProfileImage() && response.profileImageUrl() != null) {
            String absoluteProfileUrl = mediaUrlService.buildMediaUrl(request, response.profileImageUrl());
            response = response.withProfileImageUrl(absoluteProfileUrl);
            logger.debug("Converted profile image to absolute URL: {}", absoluteProfileUrl);
        }

        logger.info(
                "GROUP room successfully created: room_id={}, group_name='{}'",
                response.roomId(), response.name()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // ─────────────────────────────────────────────────────────────────
    // DIRECT ROOM CREATION
    // ─────────────────────────────────────────────────────────────────

    /**
     * Create or retrieve a DIRECT message room between two users.
     *
     * <p><strong>Request Format (application/json):</strong></p>
     * <pre>{@code
     * POST /api/rooms/direct
     * Content-Type: application/json
     * Authorization: Bearer <jwt_token>
     *
     * {
     *   "friend_id": "uuid-of-other-user"
     * }
     * }</pre>
     *
     * <p><strong>Security:</strong> The authenticated user ID is extracted from the JWT token.
     * This user becomes one participant; the {@code friend_id} in the body is the other participant.
     * Bidirectional deduplication ensures no duplicate conversations between the same two users.</p>
     *
     * <p><strong>Note:</strong> DIRECT rooms have no profile/cover images. Frontend should
     * fetch participant profile images separately via the user service.</p>
     *
     * @param requestDto body containing {@code friend_id}
     * @return ResponseEntity with PrivateRoomCreationResponse and HTTP 201 (or 200 if room existed)
     * @throws RuntimeException if user is not authenticated
     */
    @PostMapping(
            path = "/direct",
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public ResponseEntity<PrivateRoomCreationResponse> createDirectRoom(

            @RequestBody
            CreateDirectRoomRequest requestDto

    ) {
        // ─────────────────────────────────────────────
        // Extract authenticated user from JWT filter
        // This user is the conversation initiator
        // ─────────────────────────────────────────────
        UUID creatorId = UserContext
                .getUserIdAsUuid()
                .orElseThrow(() -> {
                    logger.error("Unauthorized: No authenticated user found in UserContext for createDirectRoom");
                    return new RuntimeException("Unauthorized: No authenticated user found in context");
                });

        UUID friendId = requestDto.friendId();

        logger.info(
                "Processing DIRECT room creation/retrieval: creator_id={}, friend_id={}",
                creatorId, friendId
        );

        // ─────────────────────────────────────────────
        // Delegate to application layer handler
        // Handler orchestrates:
        //   - Bidirectional lookup: (creator, friend) then (friend, creator)
        //   - If found: return existing room (prevents duplicates)
        //   - If not found: create new DIRECT room + member records
        //   - Fetch usernames from Auth Service
        //   - Build PrivateRoomCreationResponse (no image fields for DIRECT)
        // ─────────────────────────────────────────────
        PrivateRoomCreationResponse response = roomCommandHandler.createDirectRoom(
                creatorId,
                friendId
        );

        // Determine HTTP status: 201 if new room, 200 if existing room returned
        // (Handler doesn't expose this detail, so we default to 201 for simplicity)
        HttpStatus status = HttpStatus.CREATED;

        logger.info(
                "DIRECT room response: room_id={}, member_count={}, status={}",
                response.roomId(), response.members().size(), status
        );

        return ResponseEntity
                .status(status)
                .body(response);
    }

    // ─────────────────────────────────────────────────────────────────
    // HELPER METHODS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Parse a JSON array string of UUIDs into a List<UUID>.
     *
     * <p>Expected input format: {@code ["uuid-1","uuid-2","uuid-3"]}</p>
     *
     * @param jsonUuidArray the JSON array string
     * @return list of parsed UUIDs
     * @throws IllegalArgumentException if parsing fails or format is invalid
     */
    private List<UUID> parseUuidList(String jsonUuidArray) {
        if (jsonUuidArray == null || jsonUuidArray.isBlank()) {
            throw new IllegalArgumentException("participant_ids cannot be empty");
        }

        // Simple parsing: remove brackets, split by comma, trim quotes
        // For production, consider using a proper JSON library like Jackson
        String cleaned = jsonUuidArray
                .trim()
                .replaceAll("^\\[|\\]$", "")  // Remove [ and ]
                .replaceAll("\"", "");          // Remove quotes

        if (cleaned.isBlank()) {
            return List.of();
        }

        String[] parts = cleaned.split(",");
        List<UUID> uuids = new java.util.ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isBlank()) {
                try {
                    uuids.add(UUID.fromString(trimmed));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                            "Invalid UUID format in participant_ids: " + trimmed, e
                    );
                }
            }
        }
        return uuids;
    }

    // ─────────────────────────────────────────────────────────────────
    // NESTED DTO FOR REQUEST PARSING
    // ─────────────────────────────────────────────────────────────────

    /**
     * Request DTO for creating a DIRECT room.
     * Contains only the friend_id parameter.
     *
     * <p>Using a record for immutability and clean JSON binding.</p>
     */
    public record CreateDirectRoomRequest(
            @com.fasterxml.jackson.annotation.JsonProperty("friend_id")
            UUID friendId
    ) {}
}