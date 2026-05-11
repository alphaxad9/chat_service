// chat_service/src/main/java/com/example/chat_service/api/chat/MemberController.java
package com.example.chat_service.api.chat;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.chat_service.api.chat.dtos.CreateMemberRequest;
import com.example.chat_service.application.members.handlers.MemberCommandHandler;
import com.example.chat_service.application.members.handlers.dtos.MemberResponseDTO;
import com.example.chat_service.domain.members.Member;
import com.example.chat_service.infrastructure.security.UserContext;

/**
 * REST controller for member command operations.
 *
 * <p>Handles HTTP requests to manage group/room membership via {@code application/json}.
 * Authentication is handled by {@code JWTAuthenticationFilter} which populates
 * {@code UserContext} with the authenticated user ID. This controller extracts
 * that ID for ownership verification and delegates to the application layer
 * for business logic orchestration.</p>
 *
 * <p><strong>Security principles:</strong>
 * <ul>
 *   <li>Never trust client-submitted {@code user_id} for ownership-sensitive operations</li>
 *   <li>Always extract the authenticated requester from {@code UserContext} (JWT token)</li>
 *   <li>Domain aggregates enforce ownership checks; controller passes verified requester ID</li>
 *   <li>Admin-only operations should be guarded by role checks before calling handler</li>
 * </ul>
 * </p>
 *
 * <p><strong>Request/Response Format:</strong>
 * <p>All requests use {@code application/json}. Responses return enriched {@code MemberResponseDTO}
 * with embedded {@code UserView} containing username, profile picture, etc.</p>
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
 *   "is_admin": true
 * }
 * }</pre>
 * </p>
 */
@RestController
@RequestMapping("/api")
public class MemberController {

    private static final Logger logger = LoggerFactory.getLogger(MemberController.class);

    private final MemberCommandHandler memberCommandHandler;

    /**
     * Constructor injection — Spring will auto-wire MemberCommandHandler
     * because it's annotated with @Component.
     */
    public MemberController(MemberCommandHandler memberCommandHandler) {
        this.memberCommandHandler = memberCommandHandler;
    }

    // ─────────────────────────────────────────────────────────────────
    // MEMBER CREATION
    // ─────────────────────────────────────────────────────────────────

    /**
     * Add a new member to a room.
     *
     * <p>Typically called by room admin or system service. The {@code userId}
     * in the request body is the user being added (not the requester).
     * Authorization checks (caller is admin) should be added as needed.</p>
     *
     * @param roomId the room the user is joining
     * @param request body containing {@code userId} and optional {@code status}
     * @return ResponseEntity with created MemberResponseDTO and HTTP 201
     */
    @PostMapping("/rooms/{roomId}/members")
    public ResponseEntity<MemberResponseDTO> createMember(

            @PathVariable("roomId")
            UUID roomId,

            @RequestBody
            CreateMemberRequest request

    ) {
        UUID memberId = UUID.randomUUID();
        Member.Status initialStatus = request.status() != null
                ? Member.Status.valueOf(request.status().toUpperCase())
                : Member.Status.USER;

        MemberResponseDTO response = memberCommandHandler.createMember(
                memberId,
                request.userId(),
                roomId,
                initialStatus
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // ─────────────────────────────────────────────────────────────────
    // MEMBERSHIP LIFECYCLE (ownership-sensitive operations)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Member voluntarily leaves a room.
     *
     * <p><strong>Security:</strong> The {@code requesterId} is extracted from the JWT token
     * via {@code UserContext}, NOT from the request body or path. This prevents users
     * from leaving other members' memberships. The domain aggregate verifies that
     * {@code requesterId} matches the member's {@code userId} before allowing the operation.</p>
     *
     * @param memberId the membership record to update
     * @return ResponseEntity with updated MemberResponseDTO and HTTP 200
     * @throws RuntimeException if user is not authenticated or lacks ownership
     */
    @DeleteMapping("/members/{memberId}/leave")
    public ResponseEntity<MemberResponseDTO> leaveRoom(

            @PathVariable("memberId")
            UUID memberId

    ) {
        // Extract authenticated user from JWT filter via UserContext
        // This is the ONLY source of truth for "who is making this request"
        UUID requesterId = UserContext
                .getUserIdAsUuid()
                .orElseThrow(() -> {
                    logger.error("Unauthorized: No authenticated user found in UserContext for leaveRoom. Member ID: {}", memberId);
                    return new RuntimeException("Unauthorized: No authenticated user found in context");
                });

        logger.debug("Processing leaveRoom: member_id={}, requester_id={}", memberId, requesterId);

        MemberResponseDTO response = memberCommandHandler.leaveRoom(
                memberId,
                requesterId
        );

        logger.info("Member successfully left: member_id={}, user_id={}", memberId, requesterId);
        return ResponseEntity.ok(response);
    }

    /**
     * Remove a member from a room (admin/system-initiated).
     *
     * <p>Authorization checks (caller is admin of the room) should be performed
     * before calling this endpoint. This operation does NOT require ownership
     * verification — admins can remove any member.</p>
     *
     * @param memberId the membership record to update
     * @return ResponseEntity with updated MemberResponseDTO and HTTP 200
     */
    @DeleteMapping("/members/{memberId}")
    public ResponseEntity<MemberResponseDTO> removeMember(

            @PathVariable("memberId")
            UUID memberId

    ) {
        // TODO: Add admin authorization check here before proceeding
        // Example: verify caller is admin of the room containing this member

        MemberResponseDTO response = memberCommandHandler.removeMember(memberId);

        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────
    // ROLE MANAGEMENT (admin-only operations)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Promote a member to ADMIN status.
     *
     * <p>Authorization checks (caller is admin) should be performed before calling.</p>
     *
     * @param memberId the membership record to update
     * @return ResponseEntity with updated MemberResponseDTO and HTTP 200
     */
    @PatchMapping("/members/{memberId}/promote")
    public ResponseEntity<MemberResponseDTO> promoteToAdmin(

            @PathVariable("memberId")
            UUID memberId

    ) {
        // TODO: Add admin authorization check here before proceeding

        MemberResponseDTO response = memberCommandHandler.promoteToAdmin(memberId);

        return ResponseEntity.ok(response);
    }

    /**
     * Demote a member to USER status.
     *
     * <p>Authorization checks (caller is admin) should be performed before calling.</p>
     *
     * @param memberId the membership record to update
     * @return ResponseEntity with updated MemberResponseDTO and HTTP 200
     */
    @PatchMapping("/members/{memberId}/demote")
    public ResponseEntity<MemberResponseDTO> demoteToUser(

            @PathVariable("memberId")
            UUID memberId

    ) {
        // TODO: Add admin authorization check here before proceeding

        MemberResponseDTO response = memberCommandHandler.demoteToUser(memberId);

        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────
    // UNREAD MESSAGES (ownership-sensitive operations)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Mark all messages as read for the authenticated member.
     *
     * <p><strong>Security:</strong> The {@code requesterId} is extracted from the JWT token
     * via {@code UserContext}. The domain aggregate verifies ownership before allowing
     * the unread count to be reset. This prevents users from marking other members'
     * messages as read.</p>
     *
     * @param memberId the membership record to update
     * @return ResponseEntity with updated MemberResponseDTO and HTTP 200
     */
    @PatchMapping("/members/{memberId}/read")
    public ResponseEntity<MemberResponseDTO> markAllRead(

            @PathVariable("memberId")
            UUID memberId

    ) {
        // Extract authenticated user from JWT filter via UserContext
        UUID requesterId = UserContext
                .getUserIdAsUuid()
                .orElseThrow(() -> {
                    logger.error("Unauthorized: No authenticated user found in UserContext for markAllRead. Member ID: {}", memberId);
                    return new RuntimeException("Unauthorized: No authenticated user found in context");
                });

        logger.debug("Processing markAllRead: member_id={}, requester_id={}", memberId, requesterId);

        MemberResponseDTO response = memberCommandHandler.markAllRead(
                memberId,
                requesterId
        );

        logger.info("Messages marked as read: member_id={}, user_id={}", memberId, requesterId);
        return ResponseEntity.ok(response);
    }

    /**
     * System endpoint to increment unread messages (called by message delivery service).
     *
     * <p>Does not require authentication — typically called internally by the
     * message service after successfully delivering a message to a room member.</p>
     *
     * @param memberId the membership record to update
     * @param amount positive value to add to unread count
     * @return ResponseEntity with updated MemberResponseDTO and HTTP 200
     */
    @PatchMapping("/members/{memberId}/unread")
    public ResponseEntity<MemberResponseDTO> addUnreadMessages(

            @PathVariable("memberId")
            UUID memberId,

            @RequestParam("amount")
            int amount

    ) {
        MemberResponseDTO response = memberCommandHandler.addUnreadMessages(
                memberId,
                amount
        );

        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────
    // BULK READ OPERATIONS (for UI, admin panels, etc.)
    // ─────────────────────────────────────────────────────────────────

    /**
     * List all active members of a room with enriched user data.
     *
     * @param roomId the room to query
     * @return ResponseEntity with list of MemberResponseDTO and HTTP 200
     */
    @GetMapping("/rooms/{roomId}/members")
    public ResponseEntity<List<MemberResponseDTO>> getActiveRoomMembers(

            @PathVariable("roomId")
            UUID roomId

    ) {
        List<MemberResponseDTO> members = memberCommandHandler.getActiveRoomMembers(roomId);

        return ResponseEntity.ok(members);
    }

    /**
     * Get a specific member by ID with enriched user data.
     *
     * @param memberId the membership record to load
     * @return ResponseEntity with MemberResponseDTO and HTTP 200, or 404 if not found
     */
    @GetMapping("/members/{memberId}")
    public ResponseEntity<MemberResponseDTO> getMemberById(

            @PathVariable("memberId")
            UUID memberId

    ) {
        MemberResponseDTO member = memberCommandHandler.getMemberById(memberId);

        if (member == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(member);
    }

    /**
     * Get membership details for the authenticated user in a specific room.
     *
     * <p>Convenience endpoint for UI to check "am I a member of this room?".
     * Uses the authenticated user from JWT token — no userId parameter needed.</p>
     *
     * @param roomId the room to check
     * @return ResponseEntity with MemberResponseDTO and HTTP 200, or 404 if not a member
     */
    @GetMapping("/rooms/{roomId}/me")
    public ResponseEntity<MemberResponseDTO> getMyMembership(

            @PathVariable("roomId")
            UUID roomId

    ) {
        UUID userId = UserContext
                .getUserIdAsUuid()
                .orElseThrow(() ->
                        new RuntimeException("Unauthorized: No authenticated user found in context")
                );

        MemberResponseDTO member = memberCommandHandler.getMemberByUserAndRoom(
                userId,
                roomId
        );

        if (member == null) {
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
        boolean exists = memberCommandHandler.isUserActiveMemberOfRoom(userId, roomId);

        return exists
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}