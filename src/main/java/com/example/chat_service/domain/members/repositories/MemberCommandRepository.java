// chat_service/src/main/java/com/example/chat_service/domain/members/repositories/MemberCommandRepository.java
package com.example.chat_service.domain.members.repositories;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.chat_service.domain.members.MemberAggregate;
import com.example.chat_service.domain.members.exceptions.MemberNotFoundError;

/**
 * Interface for write operations on member aggregates.
 *
 * <p>All methods operate on full {@link MemberAggregate} instances to preserve domain invariants.
 * Implementations are responsible for persistence, optimistic concurrency control (if used),
 * and ensuring aggregate state (including soft-delete flags) is durably stored.</p>
 *
 * <p>This is a domain-layer interface — infrastructure implementations (e.g., JPA, JDBC)
 * should reside in {@code infrastructure.persistence.members.repositories}.</p>
 */
public interface MemberCommandRepository {

    /**
     * Persist a member aggregate.
     *
     * <p>This method handles both creation and updates:
     * <ul>
     *   <li>If the member does not exist (new ID), it performs an INSERT.</li>
     *   <li>If it exists, it performs an UPDATE based on the member ID.</li>
     * </ul>
     *
     * <p>The aggregate must be fully validated before calling this method.
     * This includes ensuring status transitions are valid, unread counts are non-negative,
     * and membership invariants (e.g., one record per user+room) are preserved.</p>
     *
     * @param aggregate the member aggregate to persist
     */
    void save(MemberAggregate aggregate);

    /**
     * Load an existing member aggregate by its unique ID.
     *
     * @param memberId the unique identifier of the membership record
     * @return the loaded member aggregate
     * @throws MemberNotFoundError if no member exists with the given ID
     *
     * <p>Used before applying any update command (e.g., promote, leave, markAllRead).</p>
     */
    MemberAggregate load(UUID memberId);

    /**
     * Load a member aggregate by the user+room relationship.
     *
     * @param userId the unique identifier of the user
     * @param roomId the unique identifier of the room/group
     * @return the loaded member aggregate if found
     * @throws MemberNotFoundError if no membership exists for this user+room pair
     *
     * <p>This is the primary lookup for membership operations since a user can only
     * have one membership record per room. Use this when you know the room context.</p>
     */
    MemberAggregate loadByUserAndRoom(UUID userId, UUID roomId);

    /**
     * Load a member aggregate by the user+room relationship, returning optional.
     *
     * @param userId the unique identifier of the user
     * @param roomId the unique identifier of the room/group
     * @return the member aggregate if found, otherwise {@link Optional#empty()}
     *
     * <p>Use this when membership existence is uncertain and you want to avoid
     * exception handling for the "not found" case.</p>
     */
    Optional<MemberAggregate> loadByUserAndRoomOptional(UUID userId, UUID roomId);

    /**
     * Check whether a membership record exists for the given member ID.
     *
     * @param memberId the unique identifier of the membership record
     * @return {@code true} if a member with the given ID exists, {@code false} otherwise
     *
     * <p>Useful for fast validation before attempting to load or update.
     * Avoids loading full aggregate state when only existence matters.</p>
     */
    boolean exists(UUID memberId);

    /**
     * Check whether a user is already a member of a specific room.
     *
     * @param userId the unique identifier of the user
     * @param roomId the unique identifier of the room/group
     * @return {@code true} if membership exists, {@code false} otherwise
     *
     * <p>Useful for preventing duplicate invitations or validating join requests
     * without loading full aggregate state.</p>
     */
    boolean existsByUserAndRoom(UUID userId, UUID roomId);

    // ── Bulk Load Operations ───────────────────────────────────────────

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