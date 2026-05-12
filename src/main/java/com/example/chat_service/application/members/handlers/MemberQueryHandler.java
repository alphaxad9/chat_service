// chat_service/src/main/java/com/example/chat_service/application/members/handlers/MemberQueryHandler.java
package com.example.chat_service.application.members.handlers;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.chat_service.application.members.handlers.dtos.MemberQueryResponseDTO;
import com.example.chat_service.application.members.services.MemberQueryServiceInterface;
import com.example.chat_service.domain.members.Member;
import com.example.chat_service.external.users.dtos.UserView;
import com.example.chat_service.external.users.dtos.users.services.UserApiClient;

/**
 * Application-layer orchestrator for member queries.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Delegate read operations to query service</li>
 *   <li>Fetch external user data via UserApiClient</li>
 *   <li>Build enriched API DTO responses via reusable helper</li>
 *   <li>Transform domain {@link Member} entities into {@link MemberQueryResponseDTO}</li>
 * </ul>
 * </p>
 *
 * <p><strong>DOES NOT:</strong>
 * <ul>
 *   <li>Contain domain business rules (delegated to Member domain model)</li>
 *   <li>Directly access database (delegated to Repository via Query Service)</li>
 *   <li>Handle HTTP concerns like request parsing (handled at controller boundary)</li>
 *   <li>Mutate state — this handler is strictly for read operations</li>
 * </ul>
 * </p>
 *
 * <p><strong>Architecture note:</strong> This handler receives primitive parameters
 * from the controller, delegates to the query service for data retrieval, then enriches
 * results with external user data. This maintains clean separation between application
 * orchestration and domain logic while following CQRS read-side patterns.</p>
 *
 * <p><strong>Reusable DTO builder:</strong> The private method {@link #buildMemberQueryResponseDTO(Member, UUID)}
 * centralizes the pattern of fetching UserView and constructing MemberQueryResponseDTO,
 * ensuring consistency across all query methods that return member data.</p>
 */
@Component
public class MemberQueryHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(MemberQueryHandler.class);

    private final MemberQueryServiceInterface queryService;
    private final UserApiClient userApiClient;

    /**
     * Constructor injection — Spring will auto-wire all dependencies
     * because they're annotated with @Component or @Service.
     *
     * @param queryService handles read operations for Member entities
     * @param userApiClient fetches user data from external Auth Service
     */
    public MemberQueryHandler(
            MemberQueryServiceInterface queryService,
            UserApiClient userApiClient
    ) {
        this.queryService = queryService;
        this.userApiClient = userApiClient;
    }

    // ─────────────────────────────────────────────────────────────────
    // REUSABLE HELPER: Build enriched DTO from Member + user ID
    // ─────────────────────────────────────────────────────────────────

    /**
     * Centralized helper to construct a MemberQueryResponseDTO from a Member.
     *
     * <p>This method encapsulates the common pattern of:
     * <ol>
     *   <li>Extracting the userId from the Member domain object</li>
     *   <li>Fetching enriched UserView from external Auth Service</li>
     *   <li>Combining domain state + external data into API-ready DTO</li>
     * </ol>
     * </p>
     *
     * <p>Used by all query methods that return member data to ensure:
     * <ul>
     *   <li>Consistent enrichment logic across endpoints</li>
     *   <li>Single point of change if UserView fields evolve</li>
     *   <li>Clear separation: handler orchestrates, domain holds state, DTO represents</li>
     * </ul>
     * </p>
     *
     * @param member the Member domain object containing membership state
     * @param userId the ID of the user to fetch (typically member.userId())
     * @return enriched MemberQueryResponseDTO ready for HTTP response
     */
    private MemberQueryResponseDTO buildMemberQueryResponseDTO(Member member, UUID userId) {
        UserView user = userApiClient.getUserById(userId);
        return MemberQueryResponseDTO.fromMember(member, user);
    }

    // ─────────────────────────────────────────────────────────────────
    // SINGLE ENTITY QUERIES
    // ─────────────────────────────────────────────────────────────────

    /**
     * Load a specific member by ID with enriched user data.
     *
     * @param memberId the membership record to load
     * @return MemberQueryResponseDTO or null if not found
     */
    public MemberQueryResponseDTO getMemberById(UUID memberId) {
        logger.debug("Fetching member by ID: member_id={}", memberId);

        return queryService.getMemberById(memberId)
                .map(member -> buildMemberQueryResponseDTO(member, member.userId()))
                .orElse(null);
    }

    /**
     * Load a member by user+room relationship with enriched user data.
     *
     * @param userId the user to look up
     * @param roomId the room to look up
     * @return MemberQueryResponseDTO or null if not found
     */
    public MemberQueryResponseDTO getMemberByUserAndRoom(UUID userId, UUID roomId) {
        logger.debug(
                "Fetching member by user+room: user_id={}, room_id={}",
                userId, roomId
        );

        return queryService.getMemberByUserIdAndRoomId(userId, roomId)
                .map(member -> buildMemberQueryResponseDTO(member, member.userId()))
                .orElse(null);
    }

    /**
     * Check if a user is an active member of a specific room.
     *
     * @param userId the user to check
     * @param roomId the room to check
     * @return true if membership exists and is active
     */
    public boolean isUserActiveMember(UUID userId, UUID roomId) {
        logger.debug(
                "Checking active membership: user_id={}, room_id={}",
                userId, roomId
        );
        return queryService.isUserActiveMember(userId, roomId);
    }

    /**
     * Get the status (ADMIN/USER) of a member in a specific room.
     *
     * @param userId the user to check
     * @param roomId the room to check
     * @return Member.Status or null if not found
     */
    public Member.Status getMemberStatusInRoom(UUID userId, UUID roomId) {
        logger.debug(
                "Fetching member status: user_id={}, room_id={}",
                userId, roomId
        );
        return queryService.getMemberStatusInRoom(userId, roomId);
    }

    /**
     * Get the unread message count for a member in a specific room.
     *
     * @param userId the user to check
     * @param roomId the room to check
     * @return unread message count, or 0 if member not found
     */
    public int getUnreadMessageCount(UUID userId, UUID roomId) {
        logger.debug(
                "Fetching unread count: user_id={}, room_id={}",
                userId, roomId
        );
        return queryService.getUnreadMessageCount(userId, roomId);
    }

    // ─────────────────────────────────────────────────────────────────
    // BULK QUERIES BY ROOM (Active Members Only)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Load all active members of a room with enriched user data.
     *
     * @param roomId the room to query
     * @return list of MemberQueryResponseDTO for active members
     */
    public List<MemberQueryResponseDTO> getActiveRoomMembers(UUID roomId) {
        logger.debug("Fetching active members for room: room_id={}", roomId);

        List<Member> members = queryService.getAllActiveMembersByRoomId(roomId);

        return members.stream()
                .map(member -> buildMemberQueryResponseDTO(member, member.userId()))
                .toList();
    }

    /**
     * Load all active admins of a room with enriched user data.
     *
     * @param roomId the room to query
     * @return list of MemberQueryResponseDTO for active admin members
     */
    public List<MemberQueryResponseDTO> getActiveRoomAdmins(UUID roomId) {
        logger.debug("Fetching active admins for room: room_id={}", roomId);

        List<Member> admins = queryService.getActiveAdminsByRoomId(roomId);

        return admins.stream()
                .map(member -> buildMemberQueryResponseDTO(member, member.userId()))
                .toList();
    }

    /**
     * Load all active regular users of a room with enriched user data.
     *
     * @param roomId the room to query
     * @return list of MemberQueryResponseDTO for active user members
     */
    public List<MemberQueryResponseDTO> getActiveRoomUsers(UUID roomId) {
        logger.debug("Fetching active users for room: room_id={}", roomId);

        List<Member> users = queryService.getActiveUsersByRoomId(roomId);

        return users.stream()
                .map(member -> buildMemberQueryResponseDTO(member, member.userId()))
                .toList();
    }

    /**
     * Count active members in a room.
     *
     * @param roomId the room to count
     * @return count of active members
     */
    public long countActiveRoomMembers(UUID roomId) {
        logger.debug("Counting active members for room: room_id={}", roomId);
        return queryService.countActiveMembersByRoomId(roomId);
    }

    // ─────────────────────────────────────────────────────────────────
    // BULK QUERIES BY USER (Active Memberships Only)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Load all active memberships for a user across all rooms.
     *
     * @param userId the user to query
     * @return list of MemberQueryResponseDTO for all user memberships
     */
    public List<MemberQueryResponseDTO> getUserActiveMemberships(UUID userId) {
        logger.debug("Fetching active memberships for user: user_id={}", userId);

        List<Member> memberships = queryService.getAllActiveMembershipsByUserId(userId);

        return memberships.stream()
                .map(member -> buildMemberQueryResponseDTO(member, member.userId()))
                .toList();
    }

    /**
     * Load all active admin memberships for a user.
     *
     * @param userId the user to query
     * @return list of MemberQueryResponseDTO for admin memberships
     */
    public List<MemberQueryResponseDTO> getUserActiveAdminMemberships(UUID userId) {
        logger.debug("Fetching active admin memberships for user: user_id={}", userId);

        List<Member> adminMemberships = queryService.getActiveAdminMembershipsByUserId(userId);

        return adminMemberships.stream()
                .map(member -> buildMemberQueryResponseDTO(member, member.userId()))
                .toList();
    }

    /**
     * Count active memberships for a user.
     *
     * @param userId the user to count
     * @return count of active memberships
     */
    public long countUserActiveMemberships(UUID userId) {
        logger.debug("Counting active memberships for user: user_id={}", userId);
        return queryService.countActiveMembershipsByUserId(userId);
    }

    // ─────────────────────────────────────────────────────────────────
    // BATCH LOOKUP QUERIES
    // ─────────────────────────────────────────────────────────────────

    /**
     * Bulk fetch active members by user IDs within a specific room.
     *
     * @param userIds collection of user IDs to look up
     * @param roomId the room to filter by
     * @return list of MemberQueryResponseDTO for found members
     */
    public List<MemberQueryResponseDTO> getActiveMembersByUserIdsInRoom(
            Collection<UUID> userIds,
            UUID roomId
    ) {
        logger.debug(
                "Bulk fetching members by user IDs in room: room_id={}, user_count={}",
                roomId, userIds.size()
        );

        List<Member> members = queryService.getActiveMembersByUserIdsInRoom(userIds, roomId);

        return members.stream()
                .map(member -> buildMemberQueryResponseDTO(member, member.userId()))
                .toList();
    }

    /**
     * Bulk fetch active members by member IDs.
     *
     * @param memberIds collection of member IDs to look up
     * @return list of MemberQueryResponseDTO for found members
     */
    public List<MemberQueryResponseDTO> getActiveMembersByIds(Collection<UUID> memberIds) {
        logger.debug(
                "Bulk fetching members by IDs: requested_count={}",
                memberIds.size()
        );

        List<Member> members = queryService.getActiveMembersByIds(memberIds);

        return members.stream()
                .map(member -> buildMemberQueryResponseDTO(member, member.userId()))
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────
    // PROJECTION QUERIES (Lightweight Read Models)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Fetch lightweight member summaries for a room (no external user data).
     *
     * <p>Use this when you only need basic membership info without enriched
     * user profiles — reduces external API calls and improves performance.</p>
     *
     * @param roomId the room to query
     * @return list of MemberQueryResponseDTO with placeholder user data
     */
    public List<MemberQueryResponseDTO> getActiveRoomMemberSummaries(UUID roomId) {
        logger.debug("Fetching member summaries for room: room_id={}", roomId);

        List<MemberQueryServiceInterface.MemberSummary> summaries =
                queryService.getActiveMemberSummariesByRoomId(roomId);

        // Convert summaries to full Member objects for DTO compatibility
        // Note: This is a trade-off — if summaries are frequently used,
        // consider creating a separate lightweight DTO type
        return summaries.stream()
                .map(summary -> {
                    // Reconstruct minimal Member from summary for DTO factory
                    Member member = new Member(
                            summary.memberId(),
                            summary.userId(),
                            summary.roomId(),
                            summary.status(),
                            0, // unreadMessages not in summary
                            summary.joinedAt(),
                            summary.joinedAt(), // updatedAt approximated
                            false // isLeft assumed false for active summaries
                    );
                    return MemberQueryResponseDTO.fromMemberWithPlaceholderUser(member);
                })
                .toList();
    }

    /**
     * Fetch lightweight membership summaries for a user (no external user data).
     *
     * <p>Use this when listing a user's rooms without needing full profile data.</p>
     *
     * @param userId the user to query
     * @return list of MemberQueryResponseDTO with placeholder user data
     */
    public List<MemberQueryResponseDTO> getUserMembershipSummaries(UUID userId) {
        logger.debug("Fetching membership summaries for user: user_id={}", userId);

        List<MemberQueryServiceInterface.MemberSummary> summaries =
                queryService.getActiveMemberSummariesByUserId(userId);

        return summaries.stream()
                .map(summary -> {
                    Member member = new Member(
                            summary.memberId(),
                            summary.userId(),
                            summary.roomId(),
                            summary.status(),
                            0,
                            summary.joinedAt(),
                            summary.joinedAt(),
                            false
                    );
                    return MemberQueryResponseDTO.fromMemberWithPlaceholderUser(member);
                })
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────
    // EXISTENCE CHECKS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Check if an active member exists by member ID.
     *
     * @param memberId the membership record to check
     * @return true if active member exists
     */
    public boolean activeMemberExists(UUID memberId) {
        logger.debug("Checking active member existence: member_id={}", memberId);
        return queryService.activeMemberExists(memberId);
    }

    /**
     * Check if an active member exists by user+room.
     *
     * @param userId the user to check
     * @param roomId the room to check
     * @return true if active membership exists
     */
    public boolean activeMemberExistsByUserAndRoom(UUID userId, UUID roomId) {
        logger.debug(
                "Checking active member existence by user+room: user_id={}, room_id={}",
                userId, roomId
        );
        return queryService.activeMemberExistsByUserAndRoom(userId, roomId);
    }
}