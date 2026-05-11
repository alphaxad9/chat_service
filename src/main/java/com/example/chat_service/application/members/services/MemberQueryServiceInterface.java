// chat_service/src/main/java/com/example/chat_service/application/members/services/MemberQueryServiceInterface.java

package com.example.chat_service.application.members.services;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.chat_service.domain.members.Member;

/**
 * Application-layer interface for member query (read) operations.
 *
 * <p>Orchestrates read-side business logic and coordinates domain entities with infrastructure
 * query repositories. All methods operate on {@link Member} entities (not aggregates) optimized
 * for read operations and CQRS patterns.</p>
 *
 * <p><strong>Design principles:</strong>
 * <ul>
 *   <li>Query methods accept IDs or filters — service delegates to repository, applies read-side logic</li>
 *   <li>All queries automatically exclude members where {@code isLeft = true} unless explicitly documented</li>
 *   <li>Read-side projections and DTOs are defined here or delegated to repository</li>
 *   <li>No state mutations — this interface is strictly for read operations</li>
 *   <li>Transaction boundaries (if any) should be read-only and applied at implementation level</li>
 * </ul></p>
 */
public interface MemberQueryServiceInterface {

    // ── Single Entity Queries (Active Members Only) ──────────────────

    /**
     * Retrieve an active member by its unique membership ID.
     *
     * @param memberId the unique identifier of the membership record
     * @return the member if found and active, otherwise {@link Optional#empty()}
     *
     * <p>Automatically filters out members where {@code isLeft = true}.
     * Use {@code Optional} to handle "not found or inactive" without exceptions.
     * Suitable for: loading member details, permission checks, UI profile displays.</p>
     */
    Optional<Member> getMemberById(UUID memberId);

    /**
     * Retrieve an active member by the user+room relationship.
     *
     * @param userId the unique identifier of the user
     * @param roomId the unique identifier of the room/group
     * @return the member if found and active, otherwise {@link Optional#empty()}
     *
     * <p>Primary lookup for membership checks when room context is known.
     * Returns empty if user is not a member OR has left the room.
     * Suitable for: checking if user can access room, loading membership context.</p>
     */
    Optional<Member> getMemberByUserIdAndRoomId(UUID userId, UUID roomId);

    /**
     * Check if a user is an active member of a room.
     *
     * @param userId the unique identifier of the user
     * @param roomId the unique identifier of the room/group
     * @return {@code true} if user is an active member, {@code false} otherwise
     *
     * <p>Optimized boolean check — does not load full entity.
     * Returns {@code false} if user never joined OR has left.
     * Suitable for: quick permission gates, UI conditional rendering.</p>
     */
    boolean isUserActiveMember(UUID userId, UUID roomId);

    // ── Bulk Queries by Room (Active Members Only) ───────────────────

    /**
     * Retrieve all active members in a room.
     *
     * @param roomId the unique identifier of the room/group
     * @return list of active {@link Member} entities in the room
     *
     * <p>Excludes all members where {@code isLeft = true}.
     * Use for: message broadcasting participant lists, room stats, admin panels.
     * Consider pagination overloads for large rooms in production.</p>
     */
    List<Member> getAllActiveMembersByRoomId(UUID roomId);

    /**
     * Retrieve all active ADMIN members in a room.
     *
     * @param roomId the unique identifier of the room/group
     * @return list of active admin {@link Member} entities
     *
     * <p>Useful for: admin action authorization, escalation flows,
     * displaying room moderators in UI, permission differentiation.</p>
     */
    List<Member> getActiveAdminsByRoomId(UUID roomId);

    /**
     * Retrieve all active USER (non-admin) members in a room.
     *
     * @param roomId the unique identifier of the room/group
     * @return list of active regular-user {@link Member} entities
     *
     * <p>Useful for: participant counts, non-admin broadcast targeting,
     * permission differentiation in UI, regular user lists.</p>
     */
    List<Member> getActiveUsersByRoomId(UUID roomId);

    /**
     * Count active members in a room.
     *
     * @param roomId the unique identifier of the room/group
     * @return count of active members (excludes left members)
     *
     * <p>Efficient count query — does not load full entities.
     * Use for: room stats, capacity checks, UI badges, analytics.</p>
     */
    long countActiveMembersByRoomId(UUID roomId);

    // ── Bulk Queries by User (Active Memberships Only) ───────────────

    /**
     * Retrieve all active memberships for a user across all rooms.
     *
     * @param userId the unique identifier of the user
     * @return list of active {@link Member} entities for this user
     *
     * <p>Excludes memberships where {@code isLeft = true}.
     * Use for: user's conversation list, dashboard, engagement metrics,
     * account overview, navigation menus.</p>
     */
    List<Member> getAllActiveMembershipsByUserId(UUID userId);

    /**
     * Retrieve all active ADMIN memberships for a user.
     *
     * @param userId the unique identifier of the user
     * @return list of rooms where user is an active admin
     *
     * <p>Useful for: "rooms I manage" UI, admin dashboard,
     * permission-based feature gating, admin navigation.</p>
     */
    List<Member> getActiveAdminMembershipsByUserId(UUID userId);

    /**
     * Count active rooms a user participates in.
     *
     * @param userId the unique identifier of the user
     * @return count of active memberships (excludes left rooms)
     *
     * <p>Efficient count — does not load entities.
     * Use for: user stats, onboarding progress, UI badges, analytics.</p>
     */
    long countActiveMembershipsByUserId(UUID userId);

    // ── Batch Lookup Queries (Active Members Only) ───────────────────

    /**
     * Bulk lookup active members for multiple users in a specific room.
     *
     * @param userIds collection of user identifiers to check
     * @param roomId the unique identifier of the room/group
     * @return list of active {@link Member} entities for matching users
     *
     * <p>Returns only users who are active members of the room.
     * Does not throw if some users are not members or have left.
     * Use for: batch invitation validation, permission checks,
     * presence updates for multiple users, bulk operations.</p>
     */
    List<Member> getActiveMembersByUserIdsInRoom(Collection<UUID> userIds, UUID roomId);

    /**
     * Bulk lookup active members by their membership IDs.
     *
     * @param memberIds collection of membership record IDs
     * @return list of active {@link Member} entities for matching IDs
     *
     * <p>Filters out any IDs that don't exist or belong to left members.
     * Order of results is not guaranteed — use a map if order matters.
     * Use for: batch operations on known membership IDs, caching layers.</p>
     */
    List<Member> getActiveMembersByIds(Collection<UUID> memberIds);

    // ── Projection Queries (Lightweight Read Models) ─────────────────

    /**
     * Fetch minimal member info for active participants in a room.
     *
     * @param roomId the unique identifier of the room/group
     * @return list of {@link MemberSummary} records (lightweight DTO)
     *
     * <p>Optimized for UI lists: only fetches {@code userId, status, joinedAt}.
     * Avoids loading full entity state when only display data is needed.
     * Implementations may use database projections or cached read models.
     * Suitable for: participant lists, mention suggestions, quick displays.</p>
     */
    List<MemberSummary> getActiveMemberSummariesByRoomId(UUID roomId);

    /**
     * Fetch minimal info for a user's active room memberships.
     *
     * @param userId the unique identifier of the user
     * @return list of {@link MemberSummary} records for active memberships
     *
     * <p>Same optimization as {@link #getActiveMemberSummariesByRoomId(UUID)}
     * but scoped to a single user's active conversations.
     * Suitable for: conversation list sidebar, quick room previews.</p>
     */
    List<MemberSummary> getActiveMemberSummariesByUserId(UUID userId);

    // ── Read-Side Utility Queries ────────────────────────────────────

    /**
     * Check if a member with the given ID exists and is active.
     *
     * @param memberId the UUID of the membership record
     * @return {@code true} if member exists and {@code isLeft = false}
     *
     * <p>Fast existence check without loading full entity.
     * Use for: precondition checks, cache key validation.</p>
     */
    boolean activeMemberExists(UUID memberId);

    /**
     * Check if a user+room membership exists and is active.
     *
     * @param userId the UUID of the user
     * @param roomId the UUID of the room/group
     * @return {@code true} if membership exists and {@code isLeft = false}
     *
     * <p>Fast existence check without loading full entity.
     * Use for: precondition checks before command operations.</p>
     */
    boolean activeMemberExistsByUserAndRoom(UUID userId, UUID roomId);

    /**
     * Get the role/status of a user in a specific room.
     *
     * @param userId the UUID of the user
     * @param roomId the UUID of the room/group
     * @return the member's {@link Member.Status} if active member, otherwise {@code null}
     *
     * <p>Convenience method for quick role checks without loading full entity.
     * Returns {@code null} if user is not a member or has left.
     * Suitable for: permission gates, UI role badges, conditional logic.</p>
     */
    Member.Status getMemberStatusInRoom(UUID userId, UUID roomId);

    /**
     * Get unread message count for a user in a specific room.
     *
     * @param userId the UUID of the user
     * @param roomId the UUID of the room/group
     * @return the unread count if active member, otherwise {@code 0}
     *
     * <p>Convenience method for UI badge displays.
     * Returns {@code 0} if user is not a member or has left.
     * Does not mark messages as read — use command service for that.</p>
     */
    int getUnreadMessageCount(UUID userId, UUID roomId);

    // ── Nested DTO for Lightweight Projections ───────────────────────

    /**
     * Lightweight read model for member list displays.
     * Contains only fields needed for UI rendering or quick checks.
     *
     * <p>This record mirrors {@link com.example.chat_service.domain.members.repositories.MemberQueryRepository.MemberSummary}
     * to keep application-layer contracts explicit and decoupled from infrastructure.</p>
     */
    record MemberSummary(
        UUID memberId,
        UUID userId,
        UUID roomId,
        Member.Status status,
        LocalDateTime joinedAt
    ) {}
}