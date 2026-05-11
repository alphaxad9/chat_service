// chat_service/src/main/java/com/example/chat_service/application/members/services/MemberCommandServiceInterface.java

package com.example.chat_service.application.members.services;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.chat_service.domain.members.MemberAggregate;

/**
 * Application-layer interface for member command (write) operations.
 *
 * <p>Orchestrates business logic and coordinates domain aggregates with infrastructure
 * repositories. All methods operate on {@link MemberAggregate} to preserve domain invariants.</p>
 *
 * <p><strong>Design principles:</strong>
 * <ul>
 *   <li>Command methods accept IDs — service loads aggregate, applies business logic, persists result</li>
 *   <li>Only {@code createMember} accepts a pre-built aggregate (for initial construction)</li>
 *   <li>Validation and business rules live in the domain (aggregate), not here</li>
 *   <li>Infrastructure concerns (persistence, caching) are delegated to repositories</li>
 *   <li>Transaction boundaries should be applied at the implementation level</li>
 * </ul></p>
 */
public interface MemberCommandServiceInterface {

    // ── Core Lifecycle Commands ────────────────────────────────────────

    /**
     * Create a new member aggregate and persist it.
     *
     * @param aggregate the validated member aggregate to create
     * @return the persisted aggregate (with assigned timestamps, etc.)
     * @throws com.example.chat_service.domain.members.exceptions.InvalidMemberEntityError
     *         if required fields (id, userId, roomId, status) are null or invalid
     */
    MemberAggregate createMember(MemberAggregate aggregate);

    /**
     * Mark a member as having voluntarily left the room.
     *
     * <p>Service loads aggregate by ID, verifies ownership via requesterId, applies domain logic,
     * and persists the updated state.</p>
     *
     * @param memberId the UUID of the membership record to update
     * @param requesterId ID of the user attempting to leave (must match member's userId)
     * @return the updated aggregate with {@code isLeft = true}
     * @throws com.example.chat_service.domain.members.exceptions.MemberNotFoundError
     *         if no member exists with the given ID
     * @throws com.example.chat_service.domain.members.exceptions.MemberUnauthorizedError
     *         if requesterId does not match the member's userId
     * @throws com.example.chat_service.domain.members.exceptions.MemberOperationNotAllowedError
     *         if the member is already inactive or has left
     */
    MemberAggregate leaveRoom(UUID memberId, UUID requesterId);

    /**
     * Remove a member from the room (admin/system-initiated action).
     *
     * <p>Service loads aggregate by ID, applies domain logic, and persists the updated state.
     * Authorization checks (e.g., caller is admin) should be performed before calling.</p>
     *
     * @param memberId the UUID of the membership record to update
     * @return the updated aggregate with {@code isLeft = true}
     * @throws com.example.chat_service.domain.members.exceptions.MemberNotFoundError
     *         if no member exists with the given ID
     * @throws com.example.chat_service.domain.members.exceptions.MemberOperationNotAllowedError
     *         if the member is already inactive or has left
     */
    MemberAggregate removeMember(UUID memberId);

    // ── Role Management Commands ───────────────────────────────────────

    /**
     * Promote a member to ADMIN status.
     *
     * <p>Service loads aggregate by ID, applies domain logic, and persists the updated state.
     * Authorization checks (e.g., caller is admin) should be performed before calling.</p>
     *
     * @param memberId the UUID of the membership record to update
     * @return the updated aggregate with {@code status = ADMIN} and refreshed {@code updatedAt}
     * @throws com.example.chat_service.domain.members.exceptions.MemberNotFoundError
     *         if no member exists with the given ID
     * @throws com.example.chat_service.domain.members.exceptions.MemberStateTransitionError
     *         if the member is already an admin
     * @throws com.example.chat_service.domain.members.exceptions.MemberOperationNotAllowedError
     *         if the member is inactive or has left
     */
    MemberAggregate promoteToAdmin(UUID memberId);

    /**
     * Demote a member to USER status.
     *
     * <p>Service loads aggregate by ID, applies domain logic, and persists the updated state.
     * Authorization checks (e.g., caller is admin) should be performed before calling.</p>
     *
     * @param memberId the UUID of the membership record to update
     * @return the updated aggregate with {@code status = USER} and refreshed {@code updatedAt}
     * @throws com.example.chat_service.domain.members.exceptions.MemberNotFoundError
     *         if no member exists with the given ID
     * @throws com.example.chat_service.domain.members.exceptions.MemberStateTransitionError
     *         if the member is already a regular user
     * @throws com.example.chat_service.domain.members.exceptions.MemberOperationNotAllowedError
     *         if the member is inactive or has left
     */
    MemberAggregate demoteToUser(UUID memberId);

    // ── Unread Messages Commands ───────────────────────────────────────

    /**
     * Increment the unread messages count for a member.
     *
     * <p>System operation typically called by message delivery service.
     * Does not require ownership verification. Service loads aggregate by ID,
     * applies domain logic, and persists the updated state.</p>
     *
     * @param memberId the UUID of the membership record to update
     * @param amount the positive increment value
     * @return the updated aggregate with increased unread count and refreshed {@code updatedAt}
     * @throws com.example.chat_service.domain.members.exceptions.MemberNotFoundError
     *         if no member exists with the given ID
     * @throws com.example.chat_service.domain.members.exceptions.InvalidUnreadMessagesError
     *         if amount is negative
     * @throws com.example.chat_service.domain.members.exceptions.MemberOperationNotAllowedError
     *         if the member is inactive or has left
     */
    MemberAggregate addUnreadMessages(UUID memberId, int amount);

    /**
     * Mark all messages as read for a member.
     *
     * <p>Requires ownership — only the member themselves can clear their unread count.
     * Service loads aggregate by ID, verifies ownership via requesterId, applies domain logic,
     * and persists the updated state.</p>
     *
     * @param memberId the UUID of the membership record to update
     * @param requesterId ID of the user attempting this operation (must match member's userId)
     * @return the updated aggregate with {@code unreadMessages = 0} and refreshed {@code updatedAt}
     * @throws com.example.chat_service.domain.members.exceptions.MemberNotFoundError
     *         if no member exists with the given ID
     * @throws com.example.chat_service.domain.members.exceptions.MemberUnauthorizedError
     *         if requesterId does not match the member's userId
     * @throws com.example.chat_service.domain.members.exceptions.MemberOperationNotAllowedError
     *         if the member is inactive or has left
     */
    MemberAggregate markAllRead(UUID memberId, UUID requesterId);

    // ── Utility Commands ───────────────────────────────────────────────

    /**
     * Touch the aggregate to refresh its {@code updatedAt} timestamp.
     *
     * <p>Useful for cache invalidation, presence tracking, or forcing persistence
     * without changing business-relevant state. Requires ownership for audit trails.
     * Service loads aggregate by ID, verifies ownership via requesterId, applies domain logic,
     * and persists the updated state.</p>
     *
     * @param memberId the UUID of the membership record to touch
     * @param requesterId ID of the user performing the touch (must match member's userId)
     * @return the updated aggregate with refreshed {@code updatedAt}
     * @throws com.example.chat_service.domain.members.exceptions.MemberNotFoundError
     *         if no member exists with the given ID
     * @throws com.example.chat_service.domain.members.exceptions.MemberUnauthorizedError
     *         if requesterId does not match the member's userId
     * @throws com.example.chat_service.domain.members.exceptions.MemberOperationNotAllowedError
     *         if the member is inactive or has left
     */
    MemberAggregate touch(UUID memberId, UUID requesterId);

    /**
     * Internal touch for system use (no ownership check).
     *
     * <p>Use sparingly — prefer explicit requesterId version for audit trails.
     * Typically used by background jobs or system maintenance tasks.
     * Service loads aggregate by ID, applies domain logic, and persists the updated state.</p>
     *
     * @param memberId the UUID of the membership record to touch
     * @return the updated aggregate with refreshed {@code updatedAt}
     * @throws com.example.chat_service.domain.members.exceptions.MemberNotFoundError
     *         if no member exists with the given ID
     * @throws com.example.chat_service.domain.members.exceptions.MemberOperationNotAllowedError
     *         if the member is inactive or has left
     */
    MemberAggregate touchInternal(UUID memberId);

    // ── Query Support Methods (for command orchestration) ──────────────

    /**
     * Load a member aggregate by its unique ID for mutation.
     *
     * @param memberId the UUID of the membership record to load
     * @return the loaded aggregate ready for business operations
     * @throws com.example.chat_service.domain.members.exceptions.MemberNotFoundError
     *         if no member exists with the given ID
     */
    MemberAggregate loadAggregate(UUID memberId);

    /**
     * Load a member aggregate by user+room relationship for mutation.
     *
     * <p>This is the primary lookup for membership operations since a user can only
     * have one membership record per room. Use this when you know the room context.</p>
     *
     * @param userId the UUID of the user
     * @param roomId the UUID of the room/group
     * @return the loaded aggregate ready for business operations
     * @throws com.example.chat_service.domain.members.exceptions.MemberNotFoundError
     *         if no membership exists for this user+room pair
     */
    MemberAggregate loadAggregateByUserAndRoom(UUID userId, UUID roomId);

    /**
     * Load a member aggregate by user+room relationship, returning optional.
     *
     * <p>Use this when membership existence is uncertain and you want to avoid
     * exception handling for the "not found" case.</p>
     *
     * @param userId the UUID of the user
     * @param roomId the UUID of the room/group
     * @return {@link Optional} containing the aggregate if found, empty otherwise
     */
    Optional<MemberAggregate> loadAggregateByUserAndRoomOptional(UUID userId, UUID roomId);

    /**
     * Check if a member aggregate exists by ID (fast existence check).
     *
     * @param memberId the UUID of the membership record
     * @return {@code true} if a member exists with the given ID
     */
    boolean aggregateExists(UUID memberId);

    /**
     * Check if a user is already a member of a specific room.
     *
     * @param userId the UUID of the user
     * @param roomId the UUID of the room/group
     * @return {@code true} if membership exists for this user+room pair
     */
    boolean aggregateExistsByUserAndRoom(UUID userId, UUID roomId);

    // ── Bulk Read Operations (for command orchestration & read models) ─

    /**
     * Bulk load all member aggregates for a given room.
     *
     * @param roomId the unique identifier of the room/group
     * @return list of all member aggregates in the room (including inactive/left members)
     *
     * <p>Use this when you need to iterate over all members for operations like:
     * <ul>
     *   <li>Sending broadcast messages</li>
     *   <li>Computing room-level statistics</li>
     *   <li>Admin member management UI</li>
     * </ul>
     *
     * <p>Consider pagination for large rooms in production implementations.</p>
     */
    List<MemberAggregate> bulkLoadByRoomId(UUID roomId);

    /**
     * Bulk load all active member aggregates for a given room.
     *
     * @param roomId the unique identifier of the room/group
     * @return list of active member aggregates (excludes members who have left)
     *
     * <p>Optimized for common read patterns where only current participants matter.
     * More efficient than filtering {@link #bulkLoadByRoomId(UUID)} results in memory.</p>
     */
    List<MemberAggregate> bulkLoadActiveByRoomId(UUID roomId);

    /**
     * Bulk load all member aggregates for a given user across all rooms.
     *
     * @param userId the unique identifier of the user
     * @return list of all member aggregates for this user (including inactive/left memberships)
     *
     * <p>Use this when you need to:
     * <ul>
     *   <li>Display a user's room list / conversation history</li>
     *   <li>Compute user-level engagement metrics</li>
     *   <li>Handle account deletion or data export</li>
     * </ul></p>
     */
    List<MemberAggregate> bulkLoadByUserId(UUID userId);

    /**
     * Bulk load all active member aggregates for a given user across all rooms.
     *
     * @param userId the unique identifier of the user
     * @return list of active member aggregates (excludes memberships where user has left)
     *
     * <p>Optimized for UI displays showing only current conversations/rooms.
     * More efficient than filtering {@link #bulkLoadByUserId(UUID)} results in memory.</p>
     */
    List<MemberAggregate> bulkLoadActiveByUserId(UUID userId);

    /**
     * Bulk load member aggregates for multiple users within a specific room.
     *
     * @param userIds collection of user identifiers to look up
     * @param roomId the unique identifier of the room/group
     * @return list of member aggregates for matching user+room pairs
     *
     * <p>Useful for batch operations like:
     * <ul>
     *   <li>Checking which of several users are already in a room</li>
     *   <li>Bulk invitation validation</li>
     *   <li>Permission checks for multiple users at once</li>
     * </ul>
     *
     * <p>Returns only found members — does not throw if some users are not members.</p>
     */
    List<MemberAggregate> bulkLoadByUserIdsInRoom(Collection<UUID> userIds, UUID roomId);

    /**
     * Bulk load active member aggregates for multiple users within a specific room.
     *
     * @param userIds collection of user identifiers to look up
     * @param roomId the unique identifier of the room/group
     * @return list of active member aggregates for matching user+room pairs
     *
     * <p>Same as {@link #bulkLoadByUserIdsInRoom(Collection, UUID)} but filters
     * out members who have left the room. Useful for messaging or presence features.</p>
     */
    List<MemberAggregate> bulkLoadActiveByUserIdsInRoom(Collection<UUID> userIds, UUID roomId);
}