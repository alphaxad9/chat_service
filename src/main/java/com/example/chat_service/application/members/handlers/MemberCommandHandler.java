// chat_service/src/main/java/com/example/chat_service/application/members/handlers/MemberCommandHandler.java
package com.example.chat_service.application.members.handlers;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.chat_service.application.members.handlers.dtos.MemberResponseDTO;
import com.example.chat_service.application.members.services.MemberCommandServiceInterface;
import com.example.chat_service.domain.members.Member;
import com.example.chat_service.domain.members.MemberAggregate;
import com.example.chat_service.external.users.dtos.UserView;
import com.example.chat_service.external.users.services.UserApiClient;

/**
 * Application-layer orchestrator for member commands.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Create/load aggregates using domain factories</li>
 *   <li>Delegate persistence to command service</li>
 *   <li>Fetch external user data via UserApiClient</li>
 *   <li>Build enriched API DTO responses via reusable helper</li>
 *   <li>Coordinate ownership verification for user-initiated operations</li>
 * </ul>
 * </p>
 *
 * <p><strong>DOES NOT:</strong>
 * <ul>
 *   <li>Contain domain business rules (delegated to MemberAggregate)</li>
 *   <li>Directly access database (delegated to Repository via Service)</li>
 *   <li>Handle HTTP concerns like request parsing (handled at controller boundary)</li>
 * </ul>
 * </p>
 *
 * <p><strong>Architecture note:</strong> This handler receives primitive parameters
 * from the controller, delegates to the domain aggregate for validation and state
 * transitions, then enriches the response with external user data. This maintains
 * clean separation between application orchestration and domain logic.</p>
 *
 * <p><strong>Reusable DTO builder:</strong> The private method {@link #buildMemberResponseDTO(MemberAggregate, UUID)}
 * centralizes the pattern of fetching UserView and constructing MemberResponseDTO,
 * ensuring consistency across all command methods that return member data.</p>
 */
@Component
public class MemberCommandHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(MemberCommandHandler.class);

    private final MemberCommandServiceInterface commandService;
    private final UserApiClient userApiClient;

    /**
     * Constructor injection — Spring will auto-wire all dependencies
     * because they're annotated with @Component or @Service.
     *
     * @param commandService handles persistence of MemberAggregate
     * @param userApiClient fetches user data from external Auth Service
     */
    public MemberCommandHandler(
            MemberCommandServiceInterface commandService,
            UserApiClient userApiClient
    ) {
        this.commandService = commandService;
        this.userApiClient = userApiClient;
    }

    // ─────────────────────────────────────────────────────────────────
    // REUSABLE HELPER: Build enriched DTO from aggregate + user ID
    // ─────────────────────────────────────────────────────────────────

    /**
     * Centralized helper to construct a MemberResponseDTO from a MemberAggregate.
     *
     * <p>This method encapsulates the common pattern of:
     * <ol>
     *   <li>Extracting the userId from the aggregate</li>
     *   <li>Fetching enriched UserView from external Auth Service</li>
     *   <li>Combining domain state + external data into API-ready DTO</li>
     * </ol>
     * </p>
     *
     * <p>Used by all command methods that return member data to ensure:
     * <ul>
     *   <li>Consistent enrichment logic across endpoints</li>
     *   <li>Single point of change if UserView fields evolve</li>
     *   <li>Clear separation: handler orchestrates, domain validates, DTO represents</li>
     * </ul>
     * </p>
     *
     * @param aggregate the MemberAggregate containing domain state
     * @param userId the ID of the user to fetch (typically aggregate.userId())
     * @return enriched MemberResponseDTO ready for HTTP response
     */
    private MemberResponseDTO buildMemberResponseDTO(MemberAggregate aggregate, UUID userId) {
        UserView user = userApiClient.getUserById(userId);
        return MemberResponseDTO.fromAggregate(aggregate, user);
    }

    // ─────────────────────────────────────────────────────────────────
    // CORE LIFECYCLE COMMANDS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Create a new member and return enriched response DTO.
     *
     * <p>Flow:
     * <ol>
     *   <li>Build MemberAggregate using domain factory (validation inside)</li>
     *   <li>Persist via command service (transactional boundary)</li>
     *   <li>Fetch user profile from external Auth Service</li>
     *   <li>Compose MemberResponseDTO with enriched user data</li>
     * </ol>
     * </p>
     *
     * @param memberId unique ID for the new membership record
     * @param userId the user being added to the room
     * @param roomId the room/group the user is joining
     * @param initialStatus starting role (ADMIN for creator, USER for invitees)
     * @return MemberResponseDTO ready for HTTP response
     */
    public MemberResponseDTO createMember(
            UUID memberId,
            UUID userId,
            UUID roomId,
            Member.Status initialStatus
    ) {
        logger.info(
                "Creating member: member_id={}, user_id={}, room_id={}, status={}",
                memberId, userId, roomId, initialStatus
        );

        // Create aggregate using domain factory — validation happens inside
        MemberAggregate aggregate = MemberAggregate.createNew(
                memberId,
                userId,
                roomId,
                initialStatus
        );

        // Persist via command service (transactional boundary)
        MemberAggregate savedAggregate = commandService.createMember(aggregate);

        // Build enriched DTO using reusable helper
        MemberResponseDTO response = buildMemberResponseDTO(
                savedAggregate,
                savedAggregate.userId()
        );

        logger.info(
                "Member successfully created: member_id={}, user_id={}, room_id={}",
                response.memberId(), response.user().userId(), response.roomId()
        );

        return response;
    }

    /**
     * Member voluntarily leaves the room.
     *
     * <p>Requires ownership verification: requesterId must match member's userId.
     * Service layer handles authorization checks via domain aggregate.</p>
     *
     * @param memberId the membership record to update
     * @param requesterId ID of the user attempting to leave (must match member's userId)
     * @return updated MemberResponseDTO with role status
     */
    public MemberResponseDTO leaveRoom(UUID memberId, UUID requesterId) {
        logger.info(
                "Member leaving room: member_id={}, requester_id={}",
                memberId, requesterId
        );

        MemberAggregate updatedAggregate = commandService.leaveRoom(memberId, requesterId);

        MemberResponseDTO response = buildMemberResponseDTO(
                updatedAggregate,
                updatedAggregate.userId()
        );

        logger.info(
                "Member successfully left: member_id={}, user_id={}",
                response.memberId(), response.user().userId()
        );

        return response;
    }

    /**
     * Remove a member from the room (admin/system-initiated).
     *
     * <p>Authorization checks (caller is admin) should be performed by controller
     * or application service before calling this method.</p>
     *
     * @param memberId the membership record to update
     * @return updated MemberResponseDTO with role status
     */
    public MemberResponseDTO removeMember(UUID memberId) {
        logger.info("Removing member: member_id={}", memberId);

        MemberAggregate updatedAggregate = commandService.removeMember(memberId);

        MemberResponseDTO response = buildMemberResponseDTO(
                updatedAggregate,
                updatedAggregate.userId()
        );

        logger.info(
                "Member successfully removed: member_id={}, user_id={}",
                response.memberId(), response.user().userId()
        );

        return response;
    }

    // ─────────────────────────────────────────────────────────────────
    // ROLE MANAGEMENT COMMANDS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Promote a member to ADMIN status.
     *
     * <p>Authorization checks (caller is admin) should be performed before calling.</p>
     *
     * @param memberId the membership record to update
     * @return updated MemberResponseDTO with new status and isAdmin=true
     */
    public MemberResponseDTO promoteToAdmin(UUID memberId) {
        logger.info("Promoting member to admin: member_id={}", memberId);

        MemberAggregate updatedAggregate = commandService.promoteToAdmin(memberId);

        MemberResponseDTO response = buildMemberResponseDTO(
                updatedAggregate,
                updatedAggregate.userId()
        );

        logger.info(
                "Member promoted to admin: member_id={}, user_id={}",
                response.memberId(), response.user().userId()
        );

        return response;
    }

    /**
     * Demote a member to USER status.
     *
     * <p>Authorization checks (caller is admin) should be performed before calling.</p>
     *
     * @param memberId the membership record to update
     * @return updated MemberResponseDTO with new status and isAdmin=false
     */
    public MemberResponseDTO demoteToUser(UUID memberId) {
        logger.info("Demoting member to user: member_id={}", memberId);

        MemberAggregate updatedAggregate = commandService.demoteToUser(memberId);

        MemberResponseDTO response = buildMemberResponseDTO(
                updatedAggregate,
                updatedAggregate.userId()
        );

        logger.info(
                "Member demoted to user: member_id={}, user_id={}",
                response.memberId(), response.user().userId()
        );

        return response;
    }

    // ─────────────────────────────────────────────────────────────────
    // UNREAD MESSAGES COMMANDS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Increment unread messages count (system operation).
     *
     * <p>Typically called by message delivery service. Does not require
     * ownership verification — only validates that member is active.</p>
     *
     * @param memberId the membership record to update
     * @param amount positive value to add to unread count
     * @return updated MemberResponseDTO (note: unread count not exposed in DTO)
     */
    public MemberResponseDTO addUnreadMessages(UUID memberId, int amount) {
        logger.info(
                "Adding unread messages: member_id={}, amount={}",
                memberId, amount
        );

        MemberAggregate updatedAggregate = commandService.addUnreadMessages(memberId, amount);

        MemberResponseDTO response = buildMemberResponseDTO(
                updatedAggregate,
                updatedAggregate.userId()
        );

        logger.debug(
                "Unread messages updated: member_id={}, new_count={}",
                response.memberId(), updatedAggregate.unreadMessages()
        );

        return response;
    }

    /**
     * Mark all messages as read for a member.
     *
     * <p>Requires ownership: requesterId must match member's userId.</p>
     *
     * @param memberId the membership record to update
     * @param requesterId ID of the user marking messages as read
     * @return updated MemberResponseDTO
     */
    public MemberResponseDTO markAllRead(UUID memberId, UUID requesterId) {
        logger.info(
                "Marking all messages read: member_id={}, requester_id={}",
                memberId, requesterId
        );

        MemberAggregate updatedAggregate = commandService.markAllRead(memberId, requesterId);

        MemberResponseDTO response = buildMemberResponseDTO(
                updatedAggregate,
                updatedAggregate.userId()
        );

        logger.info(
                "Messages marked as read: member_id={}, user_id={}",
                response.memberId(), response.user().userId()
        );

        return response;
    }

    // ─────────────────────────────────────────────────────────────────
    // UTILITY COMMANDS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Touch the aggregate to refresh updatedAt timestamp.
     *
     * <p>Requires ownership for audit trails. Useful for presence tracking
     * or cache invalidation without changing business-relevant state.</p>
     *
     * @param memberId the membership record to touch
     * @param requesterId ID of the user performing the touch
     * @return updated MemberResponseDTO
     */
    public MemberResponseDTO touch(UUID memberId, UUID requesterId) {
        logger.debug(
                "Touching member aggregate: member_id={}, requester_id={}",
                memberId, requesterId
        );

        MemberAggregate updatedAggregate = commandService.touch(memberId, requesterId);

        return buildMemberResponseDTO(
                updatedAggregate,
                updatedAggregate.userId()
        );
    }

    /**
     * Internal touch for system use (no ownership check).
     *
     * <p>Use sparingly — prefer explicit requesterId version for audit trails.
     * Typically used by background jobs or system maintenance tasks.</p>
     *
     * @param memberId the membership record to touch
     * @return updated MemberResponseDTO
     */
    public MemberResponseDTO touchInternal(UUID memberId) {
        logger.debug("Internal touch: member_id={}", memberId);

        MemberAggregate updatedAggregate = commandService.touchInternal(memberId);

        return buildMemberResponseDTO(
                updatedAggregate,
                updatedAggregate.userId()
        );
    }

    // ─────────────────────────────────────────────────────────────────
    // BULK READ OPERATIONS (for admin UI, room management, etc.)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Load all active members of a room with enriched user data.
     *
     * @param roomId the room to query
     * @return list of MemberResponseDTO for active members
     */
    public List<MemberResponseDTO> getActiveRoomMembers(UUID roomId) {
        logger.debug("Fetching active members for room: room_id={}", roomId);

        List<MemberAggregate> aggregates = commandService.bulkLoadActiveByRoomId(roomId);

        return aggregates.stream()
                .map(agg -> buildMemberResponseDTO(agg, agg.userId()))
                .toList();
    }

    /**
     * Load all memberships for a user across all rooms.
     *
     * @param userId the user to query
     * @return list of MemberResponseDTO for all user memberships
     */
    public List<MemberResponseDTO> getUserMemberships(UUID userId) {
        logger.debug("Fetching memberships for user: user_id={}", userId);

        List<MemberAggregate> aggregates = commandService.bulkLoadByUserId(userId);

        return aggregates.stream()
                .map(agg -> buildMemberResponseDTO(agg, agg.userId()))
                .toList();
    }

    /**
     * Check if a user is already a member of a specific room.
     *
     * @param userId the user to check
     * @param roomId the room to check
     * @return true if membership exists and is active
     */
    public boolean isUserActiveMemberOfRoom(UUID userId, UUID roomId) {
        return commandService.aggregateExistsByUserAndRoom(userId, roomId);
    }

    /**
     * Load a specific member by ID with enriched user data.
     *
     * @param memberId the membership record to load
     * @return MemberResponseDTO or null if not found
     */
    public MemberResponseDTO getMemberById(UUID memberId) {
        logger.debug("Fetching member by ID: member_id={}", memberId);

        if (!commandService.aggregateExists(memberId)) {
            return null;
        }

        MemberAggregate aggregate = commandService.loadAggregate(memberId);
        return buildMemberResponseDTO(aggregate, aggregate.userId());
    }

    /**
     * Load a member by user+room relationship with enriched user data.
     *
     * @param userId the user to look up
     * @param roomId the room to look up
     * @return MemberResponseDTO or null if not found
     */
    public MemberResponseDTO getMemberByUserAndRoom(UUID userId, UUID roomId) {
        logger.debug(
                "Fetching member by user+room: user_id={}, room_id={}",
                userId, roomId
        );

        return commandService.loadAggregateByUserAndRoomOptional(userId, roomId)
                .map(agg -> buildMemberResponseDTO(agg, agg.userId()))
                .orElse(null);
    }
}