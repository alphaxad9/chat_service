// chat_service/src/main/java/com/example/chat_service/domain/members/repositories/MemberQueryRepository.java
package com.example.chat_service.domain.members.repositories;

import java.time.LocalDateTime;  // ← ADD THIS IMPORT
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.chat_service.domain.members.Member;

/**
 * Interface for read-only query operations on member entities.
 *
 * <p>All methods return {@link Member} entities (not aggregates) optimized for read operations.
 * <strong>Important:</strong> All query methods automatically exclude members where {@code isLeft = true},
 * ensuring only active participants are returned. For historical/audit queries including left members,
 * use a separate audit repository or the command repository.</p>
 *
 * <p>This is a domain-layer interface — infrastructure implementations (e.g., JPA, JDBC, Redis)
 * should reside in {@code infrastructure.persistence.members.repositories}.</p>
 *
 * <p>Designed for CQRS read-side: fast, denormalized, projection-friendly queries.</p>
 */
public interface MemberQueryRepository {

    // ── Single Entity Queries (Active Members Only) ──────────────────

    /**
     * Load an active member by its unique ID.
     *
     * @param memberId the unique identifier of the membership record
     * @return the member if found and active, otherwise {@link Optional#empty()}
     *
     * <p>Automatically filters out members where {@code isLeft = true}.
     * Use {@code Optional} to handle "not found or inactive" without exceptions.</p>
     */
    Optional<Member> findById(UUID memberId);

    /**
     * Load an active member by the user+room relationship.
     *
     * @param userId the unique identifier of the user
     * @param roomId the unique identifier of the room/group
     * @return the member if found and active, otherwise {@link Optional#empty()}
     *
     * <p>Primary lookup for membership checks when room context is known.
     * Returns empty if user is not a member OR has left the room.</p>
     */
    Optional<Member> findByUserIdAndRoomId(UUID userId, UUID roomId);

    /**
     * Check if a user is an active member of a room.
     *
     * @param userId the unique identifier of the user
     * @param roomId the unique identifier of the room/group
     * @return {@code true} if user is an active member, {@code false} otherwise
     *
     * <p>Optimized boolean check — does not load full entity.
     * Returns {@code false} if user never joined OR has left.</p>
     */
    boolean isActiveMember(UUID userId, UUID roomId);

    // ── Bulk Queries by Room (Active Members Only) ───────────────────

    /**
     * Load all active members in a room.
     *
     * @param roomId the unique identifier of the room/group
     * @return list of active {@link Member} entities in the room
     *
     * <p>Excludes all members where {@code isLeft = true}.
     * Use for: message broadcasting, participant lists, room stats.</p>
     *
     * <p>Consider pagination overloads for large rooms in production.</p>
     */
    List<Member> findAllActiveByRoomId(UUID roomId);

    /**
     * Load all active ADMIN members in a room.
     *
     * @param roomId the unique identifier of the room/group
     * @return list of active admin {@link Member} entities
     *
     * <p>Useful for: admin action authorization, escalation flows,
     * displaying room moderators in UI.</p>
     */
    List<Member> findActiveAdminsByRoomId(UUID roomId);

    /**
     * Load all active USER (non-admin) members in a room.
     *
     * @param roomId the unique identifier of the room/group
     * @return list of active regular-user {@link Member} entities
     *
     * <p>Useful for: participant counts, non-admin broadcast targeting,
     * permission differentiation in UI.</p>
     */
    List<Member> findActiveUsersByRoomId(UUID roomId);

    /**
     * Count active members in a room.
     *
     * @param roomId the unique identifier of the room/group
     * @return count of active members (excludes left members)
     *
     * <p>Efficient count query — does not load full entities.
     * Use for: room stats, capacity checks, UI badges.</p>
     */
    long countActiveByRoomId(UUID roomId);

    // ── Bulk Queries by User (Active Memberships Only) ───────────────

    /**
     * Load all active memberships for a user across all rooms.
     *
     * @param userId the unique identifier of the user
     * @return list of active {@link Member} entities for this user
     *
     * <p>Excludes memberships where {@code isLeft = true}.
     * Use for: user's conversation list, dashboard, engagement metrics.</p>
     */
    List<Member> findAllActiveByUserId(UUID userId);

    /**
     * Load all active ADMIN memberships for a user.
     *
     * @param userId the unique identifier of the user
     * @return list of rooms where user is an active admin
     *
     * <p>Useful for: "rooms I manage" UI, admin dashboard,
     * permission-based feature gating.</p>
     */
    List<Member> findActiveAdminMembershipsByUserId(UUID userId);

    /**
     * Count active rooms a user participates in.
     *
     * @param userId the unique identifier of the user
     * @return count of active memberships (excludes left rooms)
     *
     * <p>Efficient count — does not load entities.
     * Use for: user stats, onboarding progress, UI badges.</p>
     */
    long countActiveByUserId(UUID userId);

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
     * presence updates for multiple users.</p>
     */
    List<Member> findActiveByUserIdsInRoom(Collection<UUID> userIds, UUID roomId);

    /**
     * Bulk lookup active members by their membership IDs.
     *
     * @param memberIds collection of membership record IDs
     * @return list of active {@link Member} entities for matching IDs
     *
     * <p>Filters out any IDs that don't exist or belong to left members.
     * Order of results is not guaranteed — use a map if order matters.
     * Use for: batch operations on known membership IDs.</p>
     */
    List<Member> findActiveByIds(Collection<UUID> memberIds);

    // ── Projection Queries (Lightweight Read Models) ─────────────────

    /**
     * Fetch minimal member info for active participants in a room.
     *
     * @param roomId the unique identifier of the room/group
     * @return list of {@link MemberSummary} records (lightweight DTO)
     *
     * <p>Optimized for UI lists: only fetches {@code userId, status, joinedAt}.
     * Avoids loading full entity state when only display data is needed.
     * Implementations may use database projections or cached read models.</p>
     */
    List<MemberSummary> findActiveSummariesByRoomId(UUID roomId);

    /**
     * Fetch minimal info for a user's active room memberships.
     *
     * @param userId the unique identifier of the user
     * @return list of {@link MemberSummary} records for active memberships
     *
     * <p>Same optimization as {@link #findActiveSummariesByRoomId(UUID)}
     * but scoped to a single user's active conversations.</p>
     */
    List<MemberSummary> findActiveSummariesByUserId(UUID userId);

    // ── Nested DTO for Lightweight Projections ───────────────────────

    /**
     * Lightweight read model for member list displays.
     * Contains only fields needed for UI rendering or quick checks.
     */
    record MemberSummary(
        UUID memberId,
        UUID userId,
        UUID roomId,
        Member.Status status,
        LocalDateTime joinedAt  // ← Now resolves correctly with the import above
    ) {}
}