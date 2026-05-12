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
import com.example.chat_service.application.rooms.handlers.dtos.GroupUpdateActionsResponse;
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
    // GROUP ROOM CREATION (unchanged - working correctly)
    // ─────────────────────────────────────────────────────────────────

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
        UUID creatorId = UserContext
                .getUserIdAsUuid()
                .orElseThrow(() -> {
                    logger.error("Unauthorized: No authenticated user found in UserContext for createGroupRoom");
                    return new RuntimeException("Unauthorized: No authenticated user found in context");
                });

        List<UUID> participantIds = parseUuidList(participantIdsJson);

        logger.info(
                "Processing GROUP room creation: creator_id={}, group_name='{}', participant_count={}",
                creatorId, groupName, participantIds.size()
        );

        GroupCreationResponse response = roomCommandHandler.createGroupRoom(
                creatorId,
                groupName,
                description,
                participantIds,
                profileImage,
                coverImage
        );

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
    // DIRECT ROOM CREATION (unchanged - working correctly)
    // ─────────────────────────────────────────────────────────────────

    @PostMapping(
            path = "/direct",
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public ResponseEntity<PrivateRoomCreationResponse> createDirectRoom(

            @RequestBody
            CreateDirectRoomRequest requestDto

    ) {
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

        PrivateRoomCreationResponse response = roomCommandHandler.createDirectRoom(
                creatorId,
                friendId
        );

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
    // GROUP ROOM UPDATE ACTIONS (return GroupUpdateActionsResponse)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Delete a GROUP room.
     */
    @DeleteMapping("/groups/{room_id}")
    public ResponseEntity<GroupUpdateActionsResponse> deleteRoom(
            @PathVariable("room_id") UUID roomId,
            HttpServletRequest request
    ) {
        UUID requesterId = UserContext
                .getUserIdAsUuid()
                .orElseThrow(() -> {
                    logger.error("Unauthorized: No authenticated user found for deleteRoom");
                    return new RuntimeException("Unauthorized: No authenticated user found in context");
                });

        logger.info("Processing delete room: room_id={}, requester_id={}", roomId, requesterId);

        GroupUpdateActionsResponse response = roomCommandHandler.deleteRoom(roomId, requesterId);

        response = convertImageUrlsToAbsolute(response, request);

        logger.info("Room deleted successfully: room_id={}", roomId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update the group name for a GROUP room.
     */
    @PatchMapping("/groups/{room_id}/name")
    public ResponseEntity<GroupUpdateActionsResponse> updateGroupName(
            @PathVariable("room_id") UUID roomId,
            @RequestBody UpdateGroupNameRequest requestDto,
            HttpServletRequest request
    ) {
        UUID requesterId = UserContext
                .getUserIdAsUuid()
                .orElseThrow(() -> {
                    logger.error("Unauthorized: No authenticated user found for updateGroupName");
                    return new RuntimeException("Unauthorized: No authenticated user found in context");
                });

        logger.info("Processing update group name: room_id={}, new_name='{}'", roomId, requestDto.newName());

        GroupUpdateActionsResponse response = roomCommandHandler.updateGroupName(
                roomId,
                requestDto.newName(),
                requesterId
        );

        response = convertImageUrlsToAbsolute(response, request);

        logger.info("Group name updated: room_id={}, new_name='{}'", roomId, requestDto.newName());
        return ResponseEntity.ok(response);
    }

    /**
     * Update the description for a GROUP room.
     */
    @PatchMapping("/groups/{room_id}/description")
    public ResponseEntity<GroupUpdateActionsResponse> updateDescription(
            @PathVariable("room_id") UUID roomId,
            @RequestBody UpdateDescriptionRequest requestDto,
            HttpServletRequest request
    ) {
        UUID requesterId = UserContext
                .getUserIdAsUuid()
                .orElseThrow(() -> {
                    logger.error("Unauthorized: No authenticated user found for updateDescription");
                    return new RuntimeException("Unauthorized: No authenticated user found in context");
                });

        logger.info("Processing update description: room_id={}", roomId);

        GroupUpdateActionsResponse response = roomCommandHandler.updateDescription(
                roomId,
                requestDto.newDescription(),
                requesterId
        );

        response = convertImageUrlsToAbsolute(response, request);

        logger.info("Description updated: room_id={}", roomId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update the cover image for a GROUP room.
     *
     * <p><strong>Important:</strong> Image saving is handled by {@code RoomCommandHandler}
     * via {@code LocalMediaStorageService}. This controller only passes the {@code MultipartFile}
     * to the handler and converts the returned relative URL to absolute for the response.</p>
     */
    @PatchMapping(
            path = "/groups/{room_id}/cover-image",
            consumes = {"multipart/form-data"}
    )
    public ResponseEntity<GroupUpdateActionsResponse> updateCoverImage(
            @PathVariable("room_id") UUID roomId,
            @RequestPart(value = "cover_image", required = false) MultipartFile coverImage,
            @RequestParam(value = "remove", required = false) Boolean remove,
            HttpServletRequest request
    ) {
        UUID requesterId = UserContext
                .getUserIdAsUuid()
                .orElseThrow(() -> {
                    logger.error("Unauthorized: No authenticated user found for updateCoverImage");
                    return new RuntimeException("Unauthorized: No authenticated user found in context");
                });

        logger.info("Processing update cover image: room_id={}, remove={}", roomId, remove);

        // Delegate to handler - handler handles image saving via LocalMediaStorageService
        GroupUpdateActionsResponse response = roomCommandHandler.updateCoverImage(
                roomId,
                coverImage,
                Boolean.TRUE.equals(remove),
                requesterId
        );

        response = convertImageUrlsToAbsolute(response, request);

        logger.info("Cover image updated: room_id={}", roomId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update the profile image for a GROUP room.
     *
     * <p><strong>Important:</strong> Image saving is handled by {@code RoomCommandHandler}
     * via {@code LocalMediaStorageService}. This controller only passes the {@code MultipartFile}
     * to the handler and converts the returned relative URL to absolute for the response.</p>
     */
    @PatchMapping(
            path = "/groups/{room_id}/profile-image",
            consumes = {"multipart/form-data"}
    )
    public ResponseEntity<GroupUpdateActionsResponse> updateProfileImage(
            @PathVariable("room_id") UUID roomId,
            @RequestPart(value = "profile_image", required = false) MultipartFile profileImage,
            @RequestParam(value = "remove", required = false) Boolean remove,
            HttpServletRequest request
    ) {
        UUID requesterId = UserContext
                .getUserIdAsUuid()
                .orElseThrow(() -> {
                    logger.error("Unauthorized: No authenticated user found for updateProfileImage");
                    return new RuntimeException("Unauthorized: No authenticated user found in context");
                });

        logger.info("Processing update profile image: room_id={}, remove={}", roomId, remove);

        // Delegate to handler - handler handles image saving via LocalMediaStorageService
        GroupUpdateActionsResponse response = roomCommandHandler.updateProfileImage(
                roomId,
                profileImage,
                Boolean.TRUE.equals(remove),
                requesterId
        );

        response = convertImageUrlsToAbsolute(response, request);

        logger.info("Profile image updated: room_id={}", roomId);
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────
    // HELPER: Convert relative image URLs to absolute for response
    // ─────────────────────────────────────────────────────────────────

    private GroupUpdateActionsResponse convertImageUrlsToAbsolute(
            GroupUpdateActionsResponse response,
            HttpServletRequest request
    ) {
        GroupUpdateActionsResponse updated = response;

        if (response.hasProfileImage() && response.profileImageUrl() != null && !response.profileImageUrl().isBlank()) {
            String absoluteProfileUrl = mediaUrlService.buildMediaUrl(request, response.profileImageUrl());
            updated = updated.withProfileImageUrl(absoluteProfileUrl);
            logger.debug("Converted profile image to absolute URL: {}", absoluteProfileUrl);
        }

        if (response.hasCoverImage() && response.coverImageUrl() != null && !response.coverImageUrl().isBlank()) {
            String absoluteCoverUrl = mediaUrlService.buildMediaUrl(request, response.coverImageUrl());
            updated = updated.withCoverImageUrl(absoluteCoverUrl);
            logger.debug("Converted cover image to absolute URL: {}", absoluteCoverUrl);
        }

        return updated;
    }

    // ─────────────────────────────────────────────────────────────────
    // HELPER: Parse UUID list from JSON string
    // ─────────────────────────────────────────────────────────────────

    private List<UUID> parseUuidList(String jsonUuidArray) {
        if (jsonUuidArray == null || jsonUuidArray.isBlank()) {
            throw new IllegalArgumentException("participant_ids cannot be empty");
        }

        String cleaned = jsonUuidArray
                .trim()
                .replaceAll("^\\[|\\]$", "")
                .replaceAll("\"", "");

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
    // NESTED REQUEST DTOs
    // ─────────────────────────────────────────────────────────────────

    public record CreateDirectRoomRequest(
            @com.fasterxml.jackson.annotation.JsonProperty("friend_id")
            UUID friendId
    ) {}

    public record UpdateGroupNameRequest(
            @com.fasterxml.jackson.annotation.JsonProperty("new_name")
            String newName
    ) {}

    public record UpdateDescriptionRequest(
            @com.fasterxml.jackson.annotation.JsonProperty("new_description")
            String newDescription
    ) {}

}