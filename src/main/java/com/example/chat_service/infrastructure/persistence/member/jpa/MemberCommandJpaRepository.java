// chat_service/src/main/java/com/example/chat_service/infrastructure/persistence/members/jpa/MemberCommandJpaRepository.java

package com.example.chat_service.infrastructure.persistence.member.jpa;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.chat_service.infrastructure.persistence.member.MemberEntity;

/**
 * Spring Data JPA repository for command-side (write) operations on {@link MemberEntity}.
 *
 * <p><strong>Purpose:</strong> Supports {@code MemberCommandOrmRepository} by providing
 * type-safe, derived-query methods for aggregate persistence and retrieval.
 * No custom JPQL/SQL — all methods use Spring Data JPA's method-name derivation.</p>
 *
 * <p><strong>Soft-delete handling:</strong> Filtering by {@code isLeft} is handled
 * explicitly via method names (e.g., {@code ...AndIsLeftFalse()}) rather than
 * {@code @SQLRestriction}. This allows the same repository to support both
 * active-only and all-members queries using pure ORM derivation.</p>
 *
 * <p><strong>Unique constraint:</strong> The {@code (user_id, room_id)} unique
 * constraint is enforced at the database level. Application logic should check
 * existence before creation to avoid {@code DataIntegrityViolationException}.</p>
 *
 * <p><strong>Not for read-side queries:</strong> This repository is optimized for
 * loading full aggregates for mutation. For read-only views, lists, or projections,
 * use a separate query-side repository when implemented.</p>
 */
@Repository
public interface MemberCommandJpaRepository extends JpaRepository<MemberEntity, UUID> {

    // ── Inherited Methods from JpaRepository<MemberEntity, UUID> ─────────
    // Basic CRUD operations (no isLeft filtering — caller decides):
    //
    // • Optional<MemberEntity> findById(UUID id)
    //   → Loads entity by ID regardless of isLeft status
    //
    // • <S extends MemberEntity> S save(S entity)
    //   → INSERT if new ID, UPDATE if ID exists (JPA merge pattern)
    //
    // • boolean existsById(UUID id)
    //   → Fast existence check regardless of isLeft status

    // ── Derived Query Methods: Active Members Only (isLeft = false) ─────

    /**
     * Find an active member by user+room relationship.
     *
     * <p>Filters to {@code isLeft = false} via method name derivation.
     * Used by {@code loadByUserAndRoom()} in command repository.</p>
     *
     * @param userId the UUID of the user
     * @param roomId the UUID of the room
     * @return {@link Optional} containing the active member, or empty
     */
    Optional<MemberEntity> findByUserIdAndRoomIdAndIsLeftFalse(UUID userId, UUID roomId);

    /**
     * Check if an active membership exists for the given user+room pair.
     *
     * <p>Efficient existence check with {@code isLeft = false} filter.
     * Used by {@code existsByUserAndRoom()} in command repository.</p>
     *
     * @param userId the UUID of the user
     * @param roomId the UUID of the room
     * @return {@code true} if an active membership exists
     */
    boolean existsByUserIdAndRoomIdAndIsLeftFalse(UUID userId, UUID roomId);

    /**
     * Load all active members in a room.
     *
     * <p>Filters to {@code isLeft = false} via method name derivation.
     * Used by {@code bulkLoadActiveByRoomId()}.</p>
     *
     * @param roomId the UUID of the room
     * @return list of active member entities only
     */
    List<MemberEntity> findAllByRoomIdAndIsLeftFalse(UUID roomId);

    /**
     * Load all active memberships for a user across all rooms.
     *
     * <p>Filters to {@code isLeft = false} via method name derivation.
     * Used by {@code bulkLoadActiveByUserId()}.</p>
     *
     * @param userId the UUID of the user
     * @return list of active member entities only
     */
    List<MemberEntity> findAllByUserIdAndIsLeftFalse(UUID userId);

    /**
     * Bulk lookup: find active members for multiple users in a specific room.
     *
     * <p>Filters to {@code isLeft = false} via method name derivation.
     * Used by {@code bulkLoadActiveByUserIdsInRoom()}.</p>
     *
     * @param userIds collection of user UUIDs to lookup
     * @param roomId the UUID of the room
     * @return list of matching active member entities only
     */
    List<MemberEntity> findAllByRoomIdAndUserIdInAndIsLeftFalse(
            UUID roomId,
            Collection<UUID> userIds
    );

    // ── Derived Query Methods: All Members (including left) ─────────────

    /**
     * Load all members in a room, including those who have left.
     *
     * <p>No {@code isLeft} filter — returns all membership states.
     * Used by {@code bulkLoadByRoomId()} for admin/audit operations.</p>
     *
     * @param roomId the UUID of the room
     * @return list of all member entities (active + left)
     */
    List<MemberEntity> findAllByRoomId(UUID roomId);

    /**
     * Load all memberships for a user across all rooms, including left ones.
     *
     * <p>No {@code isLeft} filter — returns all membership states.
     * Used by {@code bulkLoadByUserId()} for user history/export operations.</p>
     *
     * @param userId the UUID of the user
     * @return list of all member entities (active + left)
     */
    List<MemberEntity> findAllByUserId(UUID userId);

    /**
     * Bulk lookup: find members for multiple users in a specific room (all states).
     *
     * <p>No {@code isLeft} filter — returns all matching memberships.
     * Used by {@code bulkLoadByUserIdsInRoom()} for batch validation.</p>
     *
     * @param userIds collection of user UUIDs to lookup
     * @param roomId the UUID of the room
     * @return list of matching member entities (active + left)
     */
    List<MemberEntity> findAllByRoomIdAndUserIdIn(
            UUID roomId,
            Collection<UUID> userIds
    );
}