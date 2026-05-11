// chat_service/src/main/java/com/example/chat_service/api/chat/MemberQueryController.java
package com.example.chat_service.api.chat;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.chat_service.application.members.handlers.MemberQueryHandler;
import com.example.chat_service.application.members.handlers.dtos.MemberQueryResponseDTO;
import com.example.chat_service.domain.members.Member;
import com.example.chat_service.infrastructure.security.UserContext;

/**
 * REST controller for member query operations (CQRS read-side).
 *
 * <p>Handles HTTP GET requests to retrieve group/room membership data via {@code application/json}.
 * All endpoints are prefixed with {@code /api/query/} to avoid path conflicts with the command
 * controller ({@link MemberController}) which handles mutations and returns minimal DTOs.</p>
 *
 * <p><strong>CQRS Path Separation:</strong>
 * <ul>
 *   <li>{@code /api/...} → Command operations (POST/PUT/PATCH/DELETE) returning {@code MemberResponseDTO}</li>
 *   <li>{@code /api/query/...} → Query operations (GET) returning enriched {@code MemberQueryResponseDTO}</li>
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
 * <p><strong>Request/Response Format:</strong>
 * <p>All requests use {@code application/json}. Responses return enriched {@code MemberQueryResponseDTO}
 * with embedded {@code UserView} containing username, profile picture, and membership metadata.</p>
 * <pre>{@code
 * // Response example:
 * {
 *   "member_id": "550e8400-e29b-41d4-a716-446655440000",
 *   "user": {
 *     "user_id": "123e4567-e89b-12d3-a456-426614174000",
 *     "username": "alice",
 *     "email": "alice@example.com",
 *     "first_name": "Alice",
 *     "last_name": "Smith",
 *     "profile_picture": "/uploads/avatars/alice.jpg"
 *   },
 *   "room_id": "789e4567-e89b-12d3-a456-426614174999",
 *   "status": "ADMIN",
 *   "is_admin": true,
 *   "unread_messages": 5,
 *   "joined_at": "2024-01-15T10:30:00",
 *   "updated_at": "2024-01-20T14:22:00",
 *   "is_active": true
 * }
 * }</pre>
 * </p>
 */
@RestController
@RequestMapping("/api/query")
public class MemberQueryController {

    private static final Logger logger = LoggerFactory.getLogger(MemberQueryController.class);

    private final MemberQueryHandler memberQueryHandler;

    /**
     * Constructor injection — Spring will auto-wire MemberQueryHandler
     * because it's annotated with @Component.
     */
    public MemberQueryController(MemberQueryHandler memberQueryHandler) {
        this.memberQueryHandler = memberQueryHandler;
    }

    // ─────────────────────────────────────────────────────────────────
    // SINGLE ENTITY QUERIES
    // ─────────────────────────────────────────────────────────────────

    /**
     * Get a specific member by ID with enriched user data and membership metadata.
     *
     * @param memberId the membership record to load
     * @return ResponseEntity with MemberQueryResponseDTO and HTTP 200, or 404 if not found
     */
    @GetMapping("/members/{memberId}")
    public ResponseEntity<MemberQueryResponseDTO> getMemberById(

            @PathVariable("memberId")
            UUID memberId

    ) {
        logger.debug("Handling GET /api/query/members/{} request", memberId);

        MemberQueryResponseDTO member = memberQueryHandler.getMemberById(memberId);

        if (member == null) {
            logger.debug("Member not found: member_id={}", memberId);
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(member);
    }

    /**
     * Get membership details for a specific user in a specific room.
     *
     * @param userId the user to look up
     * @param roomId the room to look up
     * @return ResponseEntity with MemberQueryResponseDTO and HTTP 200, or 404 if not found
     */
    @GetMapping("/rooms/{roomId}/users/{userId}")
    public ResponseEntity<MemberQueryResponseDTO> getMemberByUserAndRoom(

            @PathVariable("roomId")
            UUID roomId,

            @PathVariable("userId")
            UUID userId

    ) {
        logger.debug("Handling GET /api/query/rooms/{}/users/{} request", roomId, userId);

        MemberQueryResponseDTO member = memberQueryHandler.getMemberByUserAndRoom(userId, roomId);

        if (member == null) {
            logger.debug("Member not found: user_id={}, room_id={}", userId, roomId);
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(member);
    }

    /**
     * Get membership details for the authenticated user in a specific room.
     *
     * <p>Convenience endpoint for UI to check "am I a member of this room?" and
     * retrieve full membership metadata. Uses the authenticated user from JWT token.</p>
     *
     * @param roomId the room to check
     * @return ResponseEntity with MemberQueryResponseDTO and HTTP 200, or 404 if not a member
     */
    @GetMapping("/rooms/{roomId}/me")
    public ResponseEntity<MemberQueryResponseDTO> getMyMembership(

            @PathVariable("roomId")
            UUID roomId

    ) {
        UUID userId = UserContext
                .getUserIdAsUuid()
                .orElseThrow(() -> {
                    logger.error("Unauthorized: No authenticated user found in UserContext for getMyMembership. Room ID: {}", roomId);
                    return new RuntimeException("Unauthorized: No authenticated user found in context");
                });

        logger.debug("Handling GET /api/query/rooms/{}/me request for user_id={}", roomId, userId);

        MemberQueryResponseDTO member = memberQueryHandler.getMemberByUserAndRoom(userId, roomId);

        if (member == null) {
            logger.debug("Membership not found for user: user_id={}, room_id={}", userId, roomId);
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(member);
    }

    /**
     * Check if a user is an active member of a specific room.
     *
     * <p>Lightweight existence check — returns 204 No Content if member exists,
     * 404 if not. Useful for conditional UI rendering or pre-flight checks.</p>
     *
     * <p><strong>Note:</strong> Spring automatically handles HEAD requests for GET mappings,
     * so we use @GetMapping here. A HEAD request to this endpoint will return the same
     * headers as GET but without the response body.</p>
     *
     * @param userId the user to check
     * @param roomId the room to check
     * @return ResponseEntity with HTTP 204 if member exists, 404 otherwise
     */
    @GetMapping("/rooms/{roomId}/members/{userId}")
    public ResponseEntity<Void> checkMembership(

            @PathVariable("roomId")
            UUID roomId,

            @PathVariable("userId")
            UUID userId

    ) {
        logger.debug("Handling membership check: user_id={}, room_id={}", userId, roomId);

        boolean exists = memberQueryHandler.isUserActiveMember(userId, roomId);

        return exists
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    /**
     * Get the role status (ADMIN/USER) of a member in a specific room.
     *
     * @param userId the user to check
     * @param roomId the room to check
     * @return ResponseEntity with status string and HTTP 200, or 404 if not found
     */
    @GetMapping("/rooms/{roomId}/users/{userId}/status")
    public ResponseEntity<String> getMemberStatusInRoom(

            @PathVariable("roomId")
            UUID roomId,

            @PathVariable("userId")
            UUID userId

    ) {
        logger.debug("Fetching member status: user_id={}, room_id={}", userId, roomId);

        Member.Status status = memberQueryHandler.getMemberStatusInRoom(userId, roomId);

        if (status == null) {
            logger.debug("Member not found for status check: user_id={}, room_id={}", userId, roomId);
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(status.name());
    }

    /**
     * Get the unread message count for the authenticated user in a specific room.
     *
     * <p>Convenience endpoint for UI badge counters. Uses authenticated user from JWT.</p>
     *
     * @param roomId the room to check
     * @return ResponseEntity with unread count and HTTP 200
     */
    @GetMapping("/rooms/{roomId}/me/unread")
    public ResponseEntity<Integer> getMyUnreadCount(

            @PathVariable("roomId")
            UUID roomId

    ) {
        UUID userId = UserContext
                .getUserIdAsUuid()
                .orElseThrow(() -> {
                    logger.error("Unauthorized: No authenticated user found in UserContext for getMyUnreadCount. Room ID: {}", roomId);
                    return new RuntimeException("Unauthorized: No authenticated user found in context");
                });

        logger.debug("Fetching unread count for user: user_id={}, room_id={}", userId, roomId);

        int unreadCount = memberQueryHandler.getUnreadMessageCount(userId, roomId);

        return ResponseEntity.ok(unreadCount);
    }

    // ─────────────────────────────────────────────────────────────────
    // BULK QUERIES BY ROOM (Active Members Only)
    // ─────────────────────────────────────────────────────────────────

    /**
     * List all active members of a room with enriched user data and membership metadata.
     *
     * @param roomId the room to query
     * @return ResponseEntity with list of MemberQueryResponseDTO and HTTP 200
     */
    @GetMapping("/rooms/{roomId}/members")
    public ResponseEntity<List<MemberQueryResponseDTO>> getActiveRoomMembers(

            @PathVariable("roomId")
            UUID roomId

    ) {
        logger.debug("Handling GET /api/query/rooms/{}/members request", roomId);

        List<MemberQueryResponseDTO> members = memberQueryHandler.getActiveRoomMembers(roomId);

        return ResponseEntity.ok(members);
    }

    /**
     * List all active admins of a room with enriched user data.
     *
     * @param roomId the room to query
     * @return ResponseEntity with list of MemberQueryResponseDTO for admins and HTTP 200
     */
    @GetMapping("/rooms/{roomId}/members/admins")
    public ResponseEntity<List<MemberQueryResponseDTO>> getActiveRoomAdmins(

            @PathVariable("roomId")
            UUID roomId

    ) {
        logger.debug("Handling GET /api/query/rooms/{}/members/admins request", roomId);

        List<MemberQueryResponseDTO> admins = memberQueryHandler.getActiveRoomAdmins(roomId);

        return ResponseEntity.ok(admins);
    }

    /**
     * List all active regular users of a room with enriched user data.
     *
     * @param roomId the room to query
     * @return ResponseEntity with list of MemberQueryResponseDTO for users and HTTP 200
     */
    @GetMapping("/rooms/{roomId}/members/users")
    public ResponseEntity<List<MemberQueryResponseDTO>> getActiveRoomUsers(

            @PathVariable("roomId")
            UUID roomId

    ) {
        logger.debug("Handling GET /api/query/rooms/{}/members/users request", roomId);

        List<MemberQueryResponseDTO> users = memberQueryHandler.getActiveRoomUsers(roomId);

        return ResponseEntity.ok(users);
    }

    /**
     * Get count of active members in a room.
     *
     * @param roomId the room to count
     * @return ResponseEntity with count and HTTP 200
     */
    @GetMapping("/rooms/{roomId}/members/count")
    public ResponseEntity<Long> countActiveRoomMembers(

            @PathVariable("roomId")
            UUID roomId

    ) {
        logger.debug("Handling GET /api/query/rooms/{}/members/count request", roomId);

        long count = memberQueryHandler.countActiveRoomMembers(roomId);

        return ResponseEntity.ok(count);
    }

    /**
     * Fetch lightweight member summaries for a room (no external user profile data).
     *
     * <p>Use this endpoint when you only need basic membership info without enriched
     * user profiles — reduces external API calls and improves response time.</p>
     *
     * @param roomId the room to query
     * @return ResponseEntity with list of MemberQueryResponseDTO (placeholder user data) and HTTP 200
     */
    @GetMapping("/rooms/{roomId}/members/summaries")
    public ResponseEntity<List<MemberQueryResponseDTO>> getActiveRoomMemberSummaries(

            @PathVariable("roomId")
            UUID roomId

    ) {
        logger.debug("Handling GET /api/query/rooms/{}/members/summaries request", roomId);

        List<MemberQueryResponseDTO> summaries = memberQueryHandler.getActiveRoomMemberSummaries(roomId);

        return ResponseEntity.ok(summaries);
    }

    // ─────────────────────────────────────────────────────────────────
    // BULK QUERIES BY USER (Active Memberships Only)
    // ─────────────────────────────────────────────────────────────────

    /**
     * List all active memberships for the authenticated user across all rooms.
     *
     * <p>Convenience endpoint for "My Rooms" or "My Groups" views. Uses authenticated
     * user from JWT token — no userId parameter needed.</p>
     *
     * @return ResponseEntity with list of MemberQueryResponseDTO and HTTP 200
     */
    @GetMapping("/users/me/memberships")
    public ResponseEntity<List<MemberQueryResponseDTO>> getMyActiveMemberships() {
        UUID userId = UserContext
                .getUserIdAsUuid()
                .orElseThrow(() -> {
                    logger.error("Unauthorized: No authenticated user found in UserContext for getMyActiveMemberships");
                    return new RuntimeException("Unauthorized: No authenticated user found in context");
                });

        logger.debug("Handling GET /api/query/users/me/memberships request for user_id={}", userId);

        List<MemberQueryResponseDTO> memberships = memberQueryHandler.getUserActiveMemberships(userId);

        return ResponseEntity.ok(memberships);
    }

    /**
     * List all active memberships for a specific user across all rooms.
     *
     * <p>Typically used by admin panels or user profile views. May require
     * authorization checks depending on privacy requirements.</p>
     *
     * @param userId the user to query
     * @return ResponseEntity with list of MemberQueryResponseDTO and HTTP 200
     */
    @GetMapping("/users/{userId}/memberships")
    public ResponseEntity<List<MemberQueryResponseDTO>> getUserActiveMemberships(

            @PathVariable("userId")
            UUID userId

    ) {
        // TODO: Add authorization check if userId != authenticated user
        // Example: require admin role or same-user access

        logger.debug("Handling GET /api/query/users/{}/memberships request", userId);

        List<MemberQueryResponseDTO> memberships = memberQueryHandler.getUserActiveMemberships(userId);

        return ResponseEntity.ok(memberships);
    }

    /**
     * List all active admin memberships for the authenticated user.
     *
     * <p>Useful for "Rooms I Administer" views. Uses authenticated user from JWT.</p>
     *
     * @return ResponseEntity with list of MemberQueryResponseDTO and HTTP 200
     */
    @GetMapping("/users/me/memberships/admin")
    public ResponseEntity<List<MemberQueryResponseDTO>> getMyActiveAdminMemberships() {
        UUID userId = UserContext
                .getUserIdAsUuid()
                .orElseThrow(() -> {
                    logger.error("Unauthorized: No authenticated user found in UserContext for getMyActiveAdminMemberships");
                    return new RuntimeException("Unauthorized: No authenticated user found in context");
                });

        logger.debug("Handling GET /api/query/users/me/memberships/admin request for user_id={}", userId);

        List<MemberQueryResponseDTO> adminMemberships = memberQueryHandler.getUserActiveAdminMemberships(userId);

        return ResponseEntity.ok(adminMemberships);
    }

    /**
     * Get count of active memberships for the authenticated user.
     *
     * @return ResponseEntity with count and HTTP 200
     */
    @GetMapping("/users/me/memberships/count")
    public ResponseEntity<Long> countMyActiveMemberships() {
        UUID userId = UserContext
                .getUserIdAsUuid()
                .orElseThrow(() -> {
                    logger.error("Unauthorized: No authenticated user found in UserContext for countMyActiveMemberships");
                    return new RuntimeException("Unauthorized: No authenticated user found in context");
                });

        logger.debug("Handling GET /api/query/users/me/memberships/count request for user_id={}", userId);

        long count = memberQueryHandler.countUserActiveMemberships(userId);

        return ResponseEntity.ok(count);
    }

    /**
     * Fetch lightweight membership summaries for the authenticated user.
     *
     * <p>Use this when listing a user's rooms without needing full profile data —
     * reduces external API calls and improves performance.</p>
     *
     * @return ResponseEntity with list of MemberQueryResponseDTO (placeholder user data) and HTTP 200
     */
    @GetMapping("/users/me/memberships/summaries")
    public ResponseEntity<List<MemberQueryResponseDTO>> getMyMembershipSummaries() {
        UUID userId = UserContext
                .getUserIdAsUuid()
                .orElseThrow(() -> {
                    logger.error("Unauthorized: No authenticated user found in UserContext for getMyMembershipSummaries");
                    return new RuntimeException("Unauthorized: No authenticated user found in context");
                });

        logger.debug("Handling GET /api/query/users/me/memberships/summaries request for user_id={}", userId);

        List<MemberQueryResponseDTO> summaries = memberQueryHandler.getUserMembershipSummaries(userId);

        return ResponseEntity.ok(summaries);
    }

    // ─────────────────────────────────────────────────────────────────
    // BATCH LOOKUP QUERIES
    // ─────────────────────────────────────────────────────────────────

    /**
     * Bulk fetch active members by user IDs within a specific room.
     *
     * <p>Useful for rendering participant lists when you have a known set of user IDs.
     * Request body should contain JSON array of UUID strings.</p>
     *
     * @param roomId the room to filter by
     * @param userIds collection of user IDs to look up (from request body)
     * @return ResponseEntity with list of MemberQueryResponseDTO for found members and HTTP 200
     */
    @PostMapping("/rooms/{roomId}/members/bulk")
    public ResponseEntity<List<MemberQueryResponseDTO>> getActiveMembersByUserIdsInRoom(

            @PathVariable("roomId")
            UUID roomId,

            @RequestBody
            Collection<UUID> userIds

    ) {
        logger.debug(
                "Handling POST /api/query/rooms/{}/members/bulk request for {} user IDs",
                roomId, userIds.size()
        );

        List<MemberQueryResponseDTO> members = memberQueryHandler.getActiveMembersByUserIdsInRoom(
                userIds,
                roomId
        );

        return ResponseEntity.ok(members);
    }

    /**
     * Bulk fetch active members by member IDs.
     *
     * <p>Useful for batch loading membership details when you have a known set
     * of member record IDs. Request body should contain JSON array of UUID strings.</p>
     *
     * @param memberIds collection of member IDs to look up (from request body)
     * @return ResponseEntity with list of MemberQueryResponseDTO for found members and HTTP 200
     */
    @PostMapping("/members/bulk")
    public ResponseEntity<List<MemberQueryResponseDTO>> getActiveMembersByIds(

            @RequestBody
            Collection<UUID> memberIds

    ) {
        logger.debug(
                "Handling POST /api/query/members/bulk request for {} member IDs",
                memberIds.size()
        );

        List<MemberQueryResponseDTO> members = memberQueryHandler.getActiveMembersByIds(memberIds);

        return ResponseEntity.ok(members);
    }

    // ─────────────────────────────────────────────────────────────────
    // EXISTENCE CHECKS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Check if an active member exists by member ID.
     *
     * <p>Lightweight existence check — returns 204 No Content if member exists,
     * 404 if not. Useful for pre-flight validation.</p>
     *
     * @param memberId the membership record to check
     * @return ResponseEntity with HTTP 204 if exists, 404 otherwise
     */
    @GetMapping("/members/{memberId}/exists")
    public ResponseEntity<Void> checkMemberExists(

            @PathVariable("memberId")
            UUID memberId

    ) {
        logger.debug("Checking member existence: member_id={}", memberId);

        boolean exists = memberQueryHandler.activeMemberExists(memberId);

        return exists
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    /**
     * Check if the authenticated user is an active member of a specific room.
     *
     * <p>Convenience endpoint using authenticated user from JWT. Returns 204 if
     * membership exists, 404 otherwise. Useful for conditional UI rendering.</p>
     *
     * @param roomId the room to check
     * @return ResponseEntity with HTTP 204 if member exists, 404 otherwise
     */
    @GetMapping("/rooms/{roomId}/me/exists")
    public ResponseEntity<Void> checkMyMembershipExists(

            @PathVariable("roomId")
            UUID roomId

    ) {
        UUID userId = UserContext
                .getUserIdAsUuid()
                .orElseThrow(() -> {
                    logger.error("Unauthorized: No authenticated user found in UserContext for checkMyMembershipExists. Room ID: {}", roomId);
                    return new RuntimeException("Unauthorized: No authenticated user found in context");
                });

        logger.debug("Checking my membership existence: user_id={}, room_id={}", userId, roomId);

        boolean exists = memberQueryHandler.activeMemberExistsByUserAndRoom(userId, roomId);

        return exists
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}